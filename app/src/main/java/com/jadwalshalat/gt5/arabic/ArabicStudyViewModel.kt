package com.jadwalshalat.gt5.arabic

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jadwalshalat.gt5.cloud.CloudSyncState
import com.jadwalshalat.gt5.cloud.NidaCloudSyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val isExtractingContentFromPdf: Boolean = false,
    val contentExtractionError: String? = null,
    val activeSeekSec: Int? = null,
    val showHarakat: Boolean = true,
    val kamusEntries: List<KamusEntry> = emptyList(),
    val kamusFilter: KamusFilter = KamusFilter.ALL,
    val kamusSearchQuery: String = "",
    val dueKamusCount: Int = 0,
    val kamusAiState: KamusAiUiState = KamusAiUiState.Idle,
    val cloudSyncState: CloudSyncState = CloudSyncState.SignedOut
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
    private val kamusAiRepository = KamusAiRepository(application.applicationContext)
    private val cloudSyncRepository by lazy { NidaCloudSyncRepository(application.applicationContext) }
    private val ttsHelper = ArabicTtsHelper(application.applicationContext)
    private var aiLookupJob: Job? = null
    private var lessonLoadJob: Job? = null
    private var pdfExtractJob: Job? = null
    private var cloudKamusJob: Job? = null
    private var cloudProgressJob: Job? = null
    private var cloudSyncUserId: String? = null
    private var latestCourseBlockIds: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(ArabicStudyUiState())
    val uiState: StateFlow<ArabicStudyUiState> = _uiState.asStateFlow()

    init {
        reloadFromLocal()
        syncCatalogSilently()
    }

    private fun syncCatalogSilently() {
        viewModelScope.launch {
            val result = runCatching { repository.syncFromRemote() }.getOrNull() ?: return@launch
            if (result.success) {
                _uiState.update {
                    it.copy(
                        lessons = repository.loadLessons(),
                        lastSyncLabel = repository.getLastSyncLabel()
                    )
                }
            }
        }
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
        lessonLoadJob?.cancel()
        pdfExtractJob?.cancel()
        repository.setLastOpenedLessonId(lesson.id)
        val shouldAttemptPdf = lesson.hasPdfSource || repository.hasBundledOrCachedPdf(lesson)
        _uiState.update {
            it.copy(
                activeLesson = lesson,
                lastOpenedLessonId = lesson.id,
                activePdfFile = null,
                isLoadingPdf = shouldAttemptPdf,
                pdfErrorMessage = null,
                activeLessonContent = null,
                isLoadingContent = true,
                isExtractingContentFromPdf = false,
                contentExtractionError = null,
                activeSeekSec = initialSeekSec
            )
        }
        cloudSyncUserId?.let { uid ->
            val state = _uiState.value
            pushProgressToCloud(uid, state.completedIds, state.favoriteIds, latestCourseBlockIds, lesson.id)
        }
        lessonLoadJob = viewModelScope.launch {
            val contentDeferred = async { contentRepository.loadLessonContent(lesson) }
            val pdfDeferred = if (shouldAttemptPdf) {
                async { repository.resolvePdfFile(lesson) }
            } else {
                null
            }

            if (pdfDeferred != null) {
                launch {
                    val file = pdfDeferred.await()
                    _uiState.update { state ->
                        if (state.activeLesson?.id != lesson.id) state
                        else state.copy(
                            activePdfFile = file,
                            isLoadingPdf = false,
                            pdfErrorMessage = if (file == null && state.activeLessonContent == null && !state.isLoadingContent) {
                                "Konten modul belum tersedia. Coba tekan Sync saat terhubung ke internet."
                            } else null
                        )
                    }
                }
            }

            val content = contentDeferred.await()
            _uiState.update { state ->
                if (state.activeLesson?.id != lesson.id) state
                else state.copy(
                    activeLessonContent = content,
                    isLoadingContent = false,
                    pdfErrorMessage = if (content == null && pdfDeferred == null) {
                        "Konten modul JSON belum tersedia. Coba tekan Sync saat terhubung ke internet."
                    } else state.pdfErrorMessage
                )
            }

            if (content == null && pdfDeferred != null) {
                val file = pdfDeferred.await()
                if (file != null && file.exists() && file.length() > 100L && _uiState.value.activeLesson?.id == lesson.id) {
                    triggerPdfContentExtraction(lesson, file)
                }
            }
        }
    }

    private fun triggerPdfContentExtraction(lesson: ArabicLesson, pdfFile: File) {
        pdfExtractJob?.cancel()
        _uiState.update { state ->
            if (state.activeLesson?.id != lesson.id) state
            else state.copy(
                isExtractingContentFromPdf = true,
                contentExtractionError = null
            )
        }
        pdfExtractJob = viewModelScope.launch {
            try {
                val extracted = contentRepository.extractAndCacheFromPdf(lesson, pdfFile)
                _uiState.update { state ->
                    if (state.activeLesson?.id != lesson.id) state
                    else state.copy(
                        activeLessonContent = extracted,
                        isExtractingContentFromPdf = false,
                        contentExtractionError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI PDF extraction timed out for '${lesson.pdfFileName}'", e)
                _uiState.update { state ->
                    if (state.activeLesson?.id != lesson.id) state
                    else state.copy(
                        isExtractingContentFromPdf = false,
                        contentExtractionError = "Ekstraksi teks interaktif AI melebihi batas waktu. Ketuk Ekstrak Teks untuk mencoba lagi."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI PDF extraction failed for '${lesson.pdfFileName}'", e)
                _uiState.update { state ->
                    if (state.activeLesson?.id != lesson.id) state
                    else state.copy(
                        isExtractingContentFromPdf = false,
                        contentExtractionError = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Gagal mengekstrak teks interaktif dari PDF. Pastikan koneksi internet aktif."
                    )
                }
            }
        }
    }

    fun retryExtractContentFromPdf() {
        val current = _uiState.value
        val lesson = current.activeLesson ?: return
        val pdfFile = current.activePdfFile ?: return
        if (current.isExtractingContentFromPdf || current.activeLessonContent != null) return
        if (!pdfFile.exists() || pdfFile.length() <= 100L) return
        triggerPdfContentExtraction(lesson, pdfFile)
    }

    fun openLessonClip(lessonId: String, startSec: Int) {
        val lesson = _uiState.value.lessons.firstOrNull { it.id == lessonId } ?: return
        openLesson(lesson = lesson, initialSeekSec = startSec)
    }

    fun closeLesson() {
        lessonLoadJob?.cancel()
        pdfExtractJob?.cancel()
        ttsHelper.stop()
        _uiState.update {
            it.copy(
                activeLesson = null,
                activePdfFile = null,
                isLoadingPdf = false,
                pdfErrorMessage = null,
                activeLessonContent = null,
                isLoadingContent = false,
                isExtractingContentFromPdf = false,
                contentExtractionError = null,
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

    fun startCloudSync(
        userId: String,
        email: String,
        displayName: String,
        localCourseBlockIds: Set<String> = emptySet(),
        onCloudCourseBlocksReceived: (Set<String>) -> Unit = {}
    ) {
        if (userId.isBlank()) return
        if (cloudSyncUserId == userId && cloudKamusJob?.isActive == true) return

        stopCloudSync()
        cloudSyncUserId = userId
        latestCourseBlockIds = localCourseBlockIds
        _uiState.update {
            it.copy(cloudSyncState = CloudSyncState.Syncing(userId, email, displayName))
        }

        cloudKamusJob = viewModelScope.launch {
            // 1. Unggah entri lokal yang belum ada di cloud agar data lama di HP tidak hilang
            val localSnapshot = withContext(Dispatchers.IO) { kamusRepository.loadEntries() }
            if (localSnapshot.isNotEmpty()) {
                val uploadResult = withContext(Dispatchers.IO) {
                    cloudSyncRepository.uploadMissingLocalKamusEntries(userId, localSnapshot)
                }
                uploadResult.onFailure { err ->
                    Log.w(TAG, "Initial local Kamus upload failed", err)
                    if (cloudSyncUserId == userId) {
                        _uiState.update {
                            it.copy(
                                cloudSyncState = CloudSyncState.Error(
                                    userId = userId,
                                    email = email,
                                    displayName = displayName,
                                    message = err.localizedMessage ?: "Gagal mengunggah Kamus lokal ke cloud."
                                )
                            )
                        }
                    }
                }
            }

            // 2. Pantau real-time snapshot Kamus Saya dari Firestore
            cloudSyncRepository.observeKamusEntries(userId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing cloud Kamus", rootError)
                    if (cloudSyncUserId == userId) {
                        _uiState.update {
                            it.copy(
                                cloudSyncState = CloudSyncState.Error(
                                    userId = userId,
                                    email = email,
                                    displayName = displayName,
                                    message = rootError.localizedMessage ?: "Gagal menyinkronkan Kamus."
                                )
                            )
                        }
                    }
                }
                .collect { cloudEntries ->
                    val persisted = withContext(Dispatchers.IO) {
                        kamusRepository.replaceAllFromCloud(cloudEntries)
                    }
                    val today = KamusSrsScheduler.todayEpochDay()
                    _uiState.update {
                        it.copy(
                            kamusEntries = persisted,
                            dueKamusCount = persisted.count { item -> KamusSrsScheduler.isDue(item, today) },
                            cloudSyncState = CloudSyncState.Synced(
                                userId = userId,
                                email = email,
                                displayName = displayName,
                                syncedWordCount = persisted.size
                            )
                        )
                    }
                }
        }

        cloudProgressJob = viewModelScope.launch {
            cloudSyncRepository.observeLearningProgress(userId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing cloud progress", firestoreError ?: error)
                }
                .collect { doc ->
                    val cloudCompleted = doc?.completedArabicIds?.toSet().orEmpty()
                    val cloudFavorites = doc?.favoriteArabicIds?.toSet().orEmpty()
                    val cloudBlocks = doc?.completedCourseBlockIds?.toSet().orEmpty()
                    val cloudLastOpened = doc?.lastOpenedLessonId?.takeIf { it.isNotBlank() }

                    val (mergedCompleted, mergedFavorites) = repository.mergeProgressFromCloud(
                        cloudCompleted = cloudCompleted,
                        cloudFavorites = cloudFavorites
                    )
                    val mergedBlocks = latestCourseBlockIds + cloudBlocks
                    latestCourseBlockIds = mergedBlocks
                    if (cloudBlocks.isNotEmpty()) {
                        onCloudCourseBlocksReceived(cloudBlocks)
                    }

                    val effectiveLastOpened = if (_uiState.value.activeLesson == null && cloudLastOpened != null) {
                        repository.setLastOpenedLessonId(cloudLastOpened)
                        cloudLastOpened
                    } else {
                        _uiState.value.lastOpenedLessonId ?: cloudLastOpened
                    }

                    _uiState.update {
                        it.copy(
                            completedIds = mergedCompleted,
                            favoriteIds = mergedFavorites,
                            lastOpenedLessonId = effectiveLastOpened
                        )
                    }

                    val localHadNewer = doc == null ||
                        mergedCompleted.size > cloudCompleted.size ||
                        mergedFavorites.size > cloudFavorites.size ||
                        mergedBlocks.size > cloudBlocks.size ||
                        (cloudLastOpened == null && effectiveLastOpened != null)
                    if (localHadNewer && (
                            mergedCompleted.isNotEmpty() ||
                                mergedFavorites.isNotEmpty() ||
                                mergedBlocks.isNotEmpty() ||
                                effectiveLastOpened != null
                            )
                    ) {
                        pushProgressToCloud(userId, mergedCompleted, mergedFavorites, mergedBlocks, effectiveLastOpened)
                    }
                }
        }
    }

    fun stopCloudSync() {
        cloudKamusJob?.cancel()
        cloudKamusJob = null
        cloudProgressJob?.cancel()
        cloudProgressJob = null
        cloudSyncUserId = null
        _uiState.update { it.copy(cloudSyncState = CloudSyncState.SignedOut) }
    }

    fun pushCourseBlocksToCloud(completedCourseBlockIds: Set<String>) {
        latestCourseBlockIds = completedCourseBlockIds
        val uid = cloudSyncUserId ?: return
        val state = _uiState.value
        pushProgressToCloud(
            userId = uid,
            completedArabicIds = state.completedIds,
            favoriteArabicIds = state.favoriteIds,
            completedCourseBlockIds = completedCourseBlockIds,
            lastOpenedLessonId = state.lastOpenedLessonId
        )
    }

    private fun pushProgressToCloud(
        userId: String,
        completedArabicIds: Set<String>,
        favoriteArabicIds: Set<String>,
        completedCourseBlockIds: Set<String>,
        lastOpenedLessonId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncRepository.saveLearningProgress(
                userId = userId,
                completedArabicIds = completedArabicIds,
                favoriteArabicIds = favoriteArabicIds,
                completedCourseBlockIds = completedCourseBlockIds,
                lastOpenedLessonId = lastOpenedLessonId
            )
        }
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
        val uid = cloudSyncUserId ?: return
        val persistedEntry = updated.firstOrNull {
            it.id == entry.id || (entry.normalizedKey.isNotBlank() && it.normalizedKey == entry.normalizedKey)
        } ?: updated.firstOrNull() ?: entry
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncRepository.upsertKamusEntry(uid, persistedEntry)
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
        val uid = cloudSyncUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncRepository.deleteKamusEntry(uid, entryId)
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
        val uid = cloudSyncUserId ?: return
        val gradedEntry = updated.firstOrNull { it.id == entryId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncRepository.updateKamusSrs(uid, gradedEntry)
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
        val uid = cloudSyncUserId ?: return
        val toggledEntry = updated.firstOrNull { it.id == entryId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            cloudSyncRepository.updateKamusSrs(uid, toggledEntry)
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
        val uid = cloudSyncUserId
        if (uid != null && addedCount > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                cloudSyncRepository.uploadMissingLocalKamusEntries(uid, updated)
            }
        }
        return addedCount
    }

    fun lookupKamusWithAi(
        arabic: String,
        contextSentence: String,
        lessonTitle: String = "",
        forceRefresh: Boolean = false
    ) {
        val cleanQuery = ArabicTextUtils.stripOuterPunctuation(arabic).ifBlank { arabic.trim() }
        if (cleanQuery.isBlank()) return

        aiLookupJob?.cancel()

        if (!forceRefresh) {
            val cached = kamusAiRepository.getCached(cleanQuery, contextSentence)
            if (cached != null) {
                _uiState.update {
                    it.copy(kamusAiState = KamusAiUiState.Success(cleanQuery, cached))
                }
                return
            }
        }

        _uiState.update { it.copy(kamusAiState = KamusAiUiState.Loading(cleanQuery)) }
        aiLookupJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    kamusAiRepository.analyze(
                        arabic = cleanQuery,
                        contextSentence = contextSentence,
                        lessonTitle = lessonTitle,
                        forceRefresh = forceRefresh
                    )
                }
                _uiState.update {
                    it.copy(kamusAiState = KamusAiUiState.Success(cleanQuery, result))
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Kamus lookup timed out for '$cleanQuery'", e)
                _uiState.update {
                    it.copy(
                        kamusAiState = KamusAiUiState.Error(
                            queryArabic = cleanQuery,
                            message = "Permintaan Gemini AI melebihi batas waktu. Periksa koneksi lalu coba lagi."
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Kamus lookup failed for '$cleanQuery'", e)
                _uiState.update {
                    it.copy(
                        kamusAiState = KamusAiUiState.Error(
                            queryArabic = cleanQuery,
                            message = e.localizedMessage?.takeIf { msg -> msg.isNotBlank() }
                                ?: "Gagal menghubungi Gemini AI. Pastikan koneksi internet aktif."
                        )
                    )
                }
            }
        }
    }

    fun clearKamusAiState() {
        aiLookupJob?.cancel()
        aiLookupJob = null
        _uiState.update { it.copy(kamusAiState = KamusAiUiState.Idle) }
    }

    fun speakArabic(arabicText: String): String? {
        return ttsHelper.speak(arabicText)
    }

    override fun onCleared() {
        super.onCleared()
        stopCloudSync()
        lessonLoadJob?.cancel()
        pdfExtractJob?.cancel()
        aiLookupJob?.cancel()
        ttsHelper.shutdown()
    }

    companion object {
        private const val TAG = "ArabicStudyViewModel"
    }

    fun toggleCompleted(lessonId: String) {
        val updated = repository.toggleCompleted(lessonId)
        _uiState.update { it.copy(completedIds = updated) }
        val uid = cloudSyncUserId ?: return
        val state = _uiState.value
        pushProgressToCloud(uid, updated, state.favoriteIds, latestCourseBlockIds, state.lastOpenedLessonId)
    }

    fun toggleFavorite(lessonId: String) {
        val updated = repository.toggleFavorite(lessonId)
        _uiState.update { it.copy(favoriteIds = updated) }
        val uid = cloudSyncUserId ?: return
        val state = _uiState.value
        pushProgressToCloud(uid, state.completedIds, updated, latestCourseBlockIds, state.lastOpenedLessonId)
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
            val result = repository.syncFromRemote(refreshContentJson = true)
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
