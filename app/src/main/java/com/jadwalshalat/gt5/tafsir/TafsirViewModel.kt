package com.jadwalshalat.gt5.tafsir

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TafsirViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TafsirRepository(application)
    private val prefsStore = TafsirPreferencesStore(application)

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<TafsirUiState> = _uiState.asStateFlow()

    private var activeLoadJob: Job? = null
    private var prefetchJob: Job? = null

    init {
        val initial = _uiState.value
        loadCurrent(
            book = initial.activeBook,
            surah = initial.surahNumber,
            ayah = initial.ayahNumber,
            recordLastRead = false,
            forceRefresh = false
        )
    }

    private fun buildInitialState(): TafsirUiState {
        val savedLastRead = prefsStore.loadLastRead()
        val book = TafsirCatalog.bookById(savedLastRead?.bookId)
        val surah = savedLastRead?.surah ?: 1
        val ayah = savedLastRead?.ayah ?: 1
        return TafsirUiState(
            activeBook = book,
            surahNumber = surah,
            ayahNumber = ayah,
            showHarakat = prefsStore.loadShowHarakat(),
            fontSize = prefsStore.loadFontSize(),
            paperTheme = prefsStore.loadPaperTheme(),
            isAyahCollapsed = prefsStore.loadAyahCollapsed(),
            bookmarks = prefsStore.loadBookmarks(),
            lastRead = savedLastRead
        )
    }

    fun selectBook(book: TafsirBook) {
        val cur = _uiState.value
        if (cur.activeBook.id == book.id && cur.tafsirState is TafsirLoadState.Ready) return
        loadCurrent(book = book, surah = cur.surahNumber, ayah = cur.ayahNumber, recordLastRead = true)
    }

    fun selectSurah(surahNumber: Int, ayahNumber: Int = 1) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val maxAyah = TafsirCatalog.surahByNumber(safeSurah).ayahCount
        val safeAyah = ayahNumber.coerceIn(1, maxAyah)
        val cur = _uiState.value
        loadCurrent(book = cur.activeBook, surah = safeSurah, ayah = safeAyah, recordLastRead = true)
    }

    fun selectAyah(ayahNumber: Int) {
        val cur = _uiState.value
        val maxAyah = cur.activeSurah.ayahCount
        val safeAyah = ayahNumber.coerceIn(1, maxAyah)
        loadCurrent(book = cur.activeBook, surah = cur.surahNumber, ayah = safeAyah, recordLastRead = true)
    }

    fun openReference(bookId: String, surah: Int, ayah: Int) {
        val book = TafsirCatalog.bookById(bookId)
        val safeSurah = surah.coerceIn(1, 114)
        val maxAyah = TafsirCatalog.surahByNumber(safeSurah).ayahCount
        val safeAyah = ayah.coerceIn(1, maxAyah)
        loadCurrent(book = book, surah = safeSurah, ayah = safeAyah, recordLastRead = true)
    }

    fun openPreviousAyah() {
        val cur = _uiState.value
        val prev = repository.computePreviousReference(cur.surahNumber, cur.ayahNumber) ?: return
        loadCurrent(book = cur.activeBook, surah = prev.first, ayah = prev.second, recordLastRead = true)
    }

    fun openNextAyah() {
        val cur = _uiState.value
        val next = repository.computeNextReference(cur.surahNumber, cur.ayahNumber) ?: return
        loadCurrent(book = cur.activeBook, surah = next.first, ayah = next.second, recordLastRead = true)
    }

    fun retryCurrent(forceRefresh: Boolean = true) {
        val cur = _uiState.value
        loadCurrent(
            book = cur.activeBook,
            surah = cur.surahNumber,
            ayah = cur.ayahNumber,
            recordLastRead = false,
            forceRefresh = forceRefresh
        )
    }

    fun toggleHarakat() {
        val next = !_uiState.value.showHarakat
        prefsStore.saveShowHarakat(next)
        _uiState.update { it.copy(showHarakat = next) }
    }

    fun setShowHarakat(show: Boolean) {
        prefsStore.saveShowHarakat(show)
        _uiState.update { it.copy(showHarakat = show) }
    }

    fun setFontSize(size: TafsirFontSize) {
        prefsStore.saveFontSize(size)
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setPaperTheme(theme: TafsirPaperTheme) {
        prefsStore.savePaperTheme(theme)
        _uiState.update { it.copy(paperTheme = theme) }
    }

    fun toggleAyahCollapsed() {
        val next = !_uiState.value.isAyahCollapsed
        prefsStore.saveAyahCollapsed(next)
        _uiState.update { it.copy(isAyahCollapsed = next) }
    }

    fun toggleCurrentBookmark(): Boolean {
        val cur = _uiState.value
        val id = TafsirBookmark.buildId(cur.activeBook.id, cur.surahNumber, cur.ayahNumber)
        val existing = cur.bookmarks.any { it.id == id }
        val updated = if (existing) {
            cur.bookmarks.filterNot { it.id == id }
        } else {
            val surahMeta = cur.activeSurah
            val newItem = TafsirBookmark(
                id = id,
                bookId = cur.activeBook.id,
                bookNameLatin = cur.activeBook.nameLatin,
                surah = cur.surahNumber,
                ayah = cur.ayahNumber,
                surahNameLatin = surahMeta.nameLatin,
                surahNameArabic = surahMeta.nameArabic,
                ayahPreview = cur.ayahText.take(90).trim(),
                createdAtMillis = System.currentTimeMillis()
            )
            listOf(newItem) + cur.bookmarks.filterNot { it.id == id }
        }
        prefsStore.saveBookmarks(updated)
        _uiState.update { it.copy(bookmarks = updated) }
        return !existing
    }

    fun removeBookmark(bookmarkId: String) {
        val updated = _uiState.value.bookmarks.filterNot { it.id == bookmarkId }
        prefsStore.saveBookmarks(updated)
        _uiState.update { it.copy(bookmarks = updated) }
    }

    private fun loadCurrent(
        book: TafsirBook,
        surah: Int,
        ayah: Int,
        recordLastRead: Boolean,
        forceRefresh: Boolean = false
    ) {
        activeLoadJob?.cancel()
        prefetchJob?.cancel()

        val updatedLastRead = if (recordLastRead || _uiState.value.lastRead == null) {
            prefsStore.saveLastRead(book.id, surah, ayah)
        } else {
            _uiState.value.lastRead
        }

        val sameAyahAlreadyLoaded = _uiState.value.surahNumber == surah &&
            _uiState.value.ayahNumber == ayah &&
            _uiState.value.ayahText.isNotBlank() &&
            !forceRefresh

        _uiState.update {
            it.copy(
                activeBook = book,
                surahNumber = surah,
                ayahNumber = ayah,
                isLoadingAyah = !sameAyahAlreadyLoaded,
                ayahErrorMessage = null,
                tafsirState = TafsirLoadState.Loading,
                lastRead = updatedLastRead
            )
        }

        activeLoadJob = viewModelScope.launch {
            val surahDeferred = async { repository.loadSurahVerses(surah) }
            val tafsirDeferred = async { repository.loadTafsir(book, surah, ayah, forceRefresh = forceRefresh) }

            val surahRes = surahDeferred.await()
            val ayahText = surahRes.getOrNull()?.get(ayah).orEmpty()
            val ayahErr = if (surahRes.isFailure) surahRes.exceptionOrNull()?.message else null

            _uiState.update {
                if (it.surahNumber == surah && it.ayahNumber == ayah) {
                    it.copy(
                        ayahText = ayahText.ifBlank { it.ayahText.takeIf { sameAyahAlreadyLoaded }.orEmpty() },
                        isLoadingAyah = false,
                        ayahErrorMessage = ayahErr
                    )
                } else it
            }

            val tafsirResult = tafsirDeferred.await()
            _uiState.update {
                if (it.activeBook.id == book.id && it.surahNumber == surah && it.ayahNumber == ayah) {
                    it.copy(tafsirState = tafsirResult)
                } else it
            }

            // Prefetch diam-diam ayat berikutnya untuk kitab yang aktif
            prefetchJob = viewModelScope.launch {
                repository.prefetchNextAyah(book, surah, ayah)
            }
        }
    }

    companion object {
        fun parseTafsirKamusLessonId(lessonId: String): Triple<String, Int, Int>? {
            if (!lessonId.startsWith("tafsir:")) return null
            val parts = lessonId.split(":")
            if (parts.size < 4) return null
            val bookId = parts[1].ifBlank { return null }
            val surah = parts[2].toIntOrNull()?.coerceIn(1, 114) ?: return null
            val ayah = parts[3].toIntOrNull()?.coerceAtLeast(1) ?: return null
            return Triple(bookId, surah, ayah)
        }
    }
}
