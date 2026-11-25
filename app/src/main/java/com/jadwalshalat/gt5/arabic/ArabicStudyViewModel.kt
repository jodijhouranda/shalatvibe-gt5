package com.jadwalshalat.gt5.arabic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class ArabicStudyUiState(
    val lessons: List<ArabicLesson> = emptyList(),
    val completedIds: Set<String> = emptySet(),
    val favoriteIds: Set<String> = emptySet(),
    val lastOpenedLessonId: String? = null,
    val selectedFilter: LessonFilter = LessonFilter.ALL,
    val searchQuery: String = "",
    val readingMode: PdfReadingMode = PdfReadingMode.ORIGINAL,
    val syncManifestUrl: String = ArabicLessonRepository.DEFAULT_SYNC_URL,
    val lastSyncLabel: String = "Bundled offline",
    val isSyncing: Boolean = false,
    val activeLesson: ArabicLesson? = null,
    val activePdfFile: File? = null,
    val isLoadingPdf: Boolean = false,
    val pdfErrorMessage: String? = null,
    val activeLessonContent: ArabicLessonContent? = null,
    val isLoadingContent: Boolean = false,
    val activeSeekSec: Int? = null,
    val showHarakat: Boolean = true,
    val kamusEntries: List<KamusEntry> = emptyList(),
    val kamusFilter: KamusFilter = KamusFilter.ALL,
    val kamusSearchQuery: String = "",
    val dueKamusCount: Int = 0
) {
    val savedNormalizedKeys: Set<String>
        get() = kamusEntries.mapNotNull { it.normalizedKey.takeIf(String::isNotBlank) }.toSet()
    val completedCount: Int
        get() = lessons.count { completedIds.contains(it.id) }

    val lastOpenedLesson: ArabicLesson?
        get() = lessons.firstOrNull { it.id == lastOpenedLessonId } ?: lessons.firstOrNull()

    val filteredLessons: List<ArabicLesson>
        get() {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            return lessons.filter { lesson ->
                val matchesFilter = when (selectedFilter) {
                    LessonFilter.ALL -> true
                    LessonFilter.VIDEO -> lesson.category == LessonCategory.VIDEO_ANALYSIS
                    LessonFilter.INCOMPLETE -> !completedIds.contains(lesson.id)
                    LessonFilter.FAVORITE -> favoriteIds.contains(lesson.id)
                }
                val matchesQuery = q.isEmpty() ||
                    lesson.title.lowercase(Locale.getDefault()).contains(q) ||
                    lesson.subtitle.lowercase(Locale.getDefault()).contains(q) ||
                    lesson.sourceName.lowercase(Locale.getDefault()).contains(q) ||
                    lesson.dayLabel.lowercase(Locale.getDefault()).contains(q)
                matchesFilter && matchesQuery
            }
        }
}

class ArabicStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArabicLessonRepository(application.applicationContext)
    private val contentRepository = ArabicContentRepository(application.applicationContext)
    private val kamusRepository = KamusRepository(application.applicationContext)
    private val ttsHelper = ArabicTtsHelper(application.applicationContext)

    private val _uiState = MutableStateFlow(ArabicStudyUiState())
    val uiState: StateFlow<ArabicStudyUiState> = _uiState.asStateFlow()

    init {
        reloadFromLocal()
    }

    fun reloadFromLocal() {
        val kamus = kamusRepository.loadEntries()
        val today = KamusSrsScheduler.todayEpochDay()
        _uiState.update {
            it.copy(
                lessons = repository.loadLessons(),
                completedIds = repository.getCompletedIds(),
                favoriteIds = repository.getFavoriteIds(),
                lastOpenedLessonId = repository.getLastOpenedLessonId(),
                readingMode = repository.getReadingMode(),
                syncManifestUrl = repository.getSyncManifestUrl(),
                lastSyncLabel = repository.getLastSyncLabel(),
                kamusEntries = kamus,
                dueKamusCount = kamus.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
    }

    fun setFilter(filter: LessonFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openLesson(lesson: ArabicLesson, initialSeekSec: Int? = null) {
        repository.setLastOpenedLessonId(lesson.id)
        _uiState.update {
            it.copy(
                activeLesson = lesson,
                lastOpenedLessonId = lesson.id,
                activePdfFile = null,
                isLoadingPdf = true,
                pdfErrorMessage = null,
                activeLessonContent = null,
                isLoadingContent = true,
                activeSeekSec = initialSeekSec
            )
        }
        viewModelScope.launch {
            val content = contentRepository.loadLessonContent(lesson)
            _uiState.update { state ->
                if (state.activeLesson?.id != lesson.id) state
                else state.copy(activeLessonContent = content, isLoadingContent = false)
            }
        }
        viewModelScope.launch {
            val file = repository.resolvePdfFile(lesson)
            _uiState.update { state ->
                if (state.activeLesson?.id != lesson.id) state
                else state.copy(
                    activePdfFile = file,
                    isLoadingPdf = false,
                    pdfErrorMessage = if (file == null) {
                        "File PDF tidak ditemukan. Coba tekan Sync saat terhubung ke internet."
                    } else null
                )
            }
        }
    }

    fun openLessonClip(lessonId: String, startSec: Int) {
        val lesson = _uiState.value.lessons.firstOrNull { it.id == lessonId } ?: return
        openLesson(lesson = lesson, initialSeekSec = startSec)
    }

    fun closeLesson() {
        ttsHelper.stop()
        _uiState.update {
            it.copy(
                activeLesson = null,
                activePdfFile = null,
                isLoadingPdf = false,
                pdfErrorMessage = null,
                activeLessonContent = null,
                isLoadingContent = false,
                activeSeekSec = null
            )
        }
    }

    fun toggleHarakat() {
        _uiState.update { it.copy(showHarakat = !it.showHarakat) }
    }

    fun setKamusFilter(filter: KamusFilter) {
        _uiState.update { it.copy(kamusFilter = filter) }
    }

    fun setKamusSearchQuery(query: String) {
        _uiState.update { it.copy(kamusSearchQuery = query) }
    }

    fun saveKamusEntry(entry: KamusEntry) {
        val updated = kamusRepository.upsertEntry(entry)
        val today = KamusSrsScheduler.todayEpochDay()
        _uiState.update {
            it.copy(
                kamusEntries = updated,
                dueKamusCount = updated.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
    }

    fun deleteKamusEntry(entryId: String) {
        val updated = kamusRepository.deleteEntry(entryId)
        val today = KamusSrsScheduler.todayEpochDay()
        _uiState.update {
            it.copy(
                kamusEntries = updated,
                dueKamusCount = updated.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
    }

    fun gradeKamusSrs(entryId: String, remembered: Boolean) {
        val today = KamusSrsScheduler.todayEpochDay()
        val updated = kamusRepository.gradeSrs(entryId, remembered, today)
        _uiState.update {
            it.copy(
                kamusEntries = updated,
                dueKamusCount = updated.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
    }

    fun toggleKamusMastered(entryId: String) {
        val today = KamusSrsScheduler.todayEpochDay()
        val updated = kamusRepository.toggleMastered(entryId, today)
        _uiState.update {
            it.copy(
                kamusEntries = updated,
                dueKamusCount = updated.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
    }

    fun importKamusBackupJson(rawJson: String): Int {
        val (updated, addedCount) = kamusRepository.importBackupJson(rawJson)
        val today = KamusSrsScheduler.todayEpochDay()
        _uiState.update {
            it.copy(
                kamusEntries = updated,
                dueKamusCount = updated.count { item -> KamusSrsScheduler.isDue(item, today) }
            )
        }
        return addedCount
    }

    fun speakArabic(arabicText: String): String? {
        return ttsHelper.speak(arabicText)
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }

    fun toggleCompleted(lessonId: String) {
        val updated = repository.toggleCompleted(lessonId)
        _uiState.update { it.copy(completedIds = updated) }
    }

    fun toggleFavorite(lessonId: String) {
        val updated = repository.toggleFavorite(lessonId)
        _uiState.update { it.copy(favoriteIds = updated) }
    }

    fun cycleReadingMode() {
        val next = _uiState.value.readingMode.next()
        repository.setReadingMode(next)
        _uiState.update { it.copy(readingMode = next) }
    }

    fun saveSyncManifestUrl(url: String) {
        repository.setSyncManifestUrl(url)
        _uiState.update { it.copy(syncManifestUrl = repository.getSyncManifestUrl()) }
    }

    fun resetSyncManifestUrl() {
        val defaultUrl = repository.resetSyncManifestUrl()
        _uiState.update { it.copy(syncManifestUrl = defaultUrl) }
    }

    fun syncCatalog(onResult: (SyncResult) -> Unit) {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val result = repository.syncFromRemote()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lessons = repository.loadLessons(),
                    lastSyncLabel = repository.getLastSyncLabel()
                )
            }
            onResult(result)
        }
    }
}
