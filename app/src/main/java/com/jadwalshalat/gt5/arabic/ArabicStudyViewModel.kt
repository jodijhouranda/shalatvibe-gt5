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
    val pdfErrorMessage: String? = null
) {
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

    private val _uiState = MutableStateFlow(ArabicStudyUiState())
    val uiState: StateFlow<ArabicStudyUiState> = _uiState.asStateFlow()

    init {
        reloadFromLocal()
    }

    fun reloadFromLocal() {
        _uiState.update {
            it.copy(
                lessons = repository.loadLessons(),
                completedIds = repository.getCompletedIds(),
                favoriteIds = repository.getFavoriteIds(),
                lastOpenedLessonId = repository.getLastOpenedLessonId(),
                readingMode = repository.getReadingMode(),
                syncManifestUrl = repository.getSyncManifestUrl(),
                lastSyncLabel = repository.getLastSyncLabel()
            )
        }
    }

    fun setFilter(filter: LessonFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openLesson(lesson: ArabicLesson) {
        repository.setLastOpenedLessonId(lesson.id)
        _uiState.update {
            it.copy(
                activeLesson = lesson,
                lastOpenedLessonId = lesson.id,
                activePdfFile = null,
                isLoadingPdf = true,
                pdfErrorMessage = null
            )
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

    fun closeLesson() {
        _uiState.update {
            it.copy(
                activeLesson = null,
                activePdfFile = null,
                isLoadingPdf = false,
                pdfErrorMessage = null
            )
        }
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
