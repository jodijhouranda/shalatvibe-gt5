package com.jadwalshalat.gt5.learning

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface LearningDestination {
    data object Library : LearningDestination
    data object ArabicCatalog : LearningDestination
    data class CourseOverview(val courseId: String) : LearningDestination
    data class CourseStudyBlock(
        val courseId: String,
        val episodeId: String,
        val blockId: String,
        val initialTimeSec: Int? = null
    ) : LearningDestination
    data class CourseBookPdf(
        val courseId: String,
        val initialPage: Int = 1
    ) : LearningDestination
}

data class LearningSearchHit(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val kind: Kind,
    val courseId: String = "",
    val episodeId: String = "",
    val blockId: String = "",
    val lessonId: String = "",
    val glossaryTerm: GlossaryTerm? = null
) {
    enum class Kind {
        TRACK,
        TOPIC_BLOCK,
        GLOSSARY_TERM,
        ARABIC_LESSON
    }
}

data class LearningHubUiState(
    val tracks: List<LearningTrackEntry> = emptyList(),
    val seriesCourses: Map<String, SeriesCourse> = emptyMap(),
    val completedBlockIds: Set<String> = emptySet(),
    val destination: LearningDestination = LearningDestination.Library,
    val activeCoursePdfFile: File? = null,
    val isLoadingCoursePdf: Boolean = false,
    val coursePdfError: String? = null,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val lastTrackId: String? = null,
    val lastCourseId: String? = null,
    val lastBlockId: String? = null
) {
    fun completedCountForCourse(courseId: String): Int {
        val course = seriesCourses[courseId] ?: return 0
        return course.allBlocks.count { (_, block) -> completedBlockIds.contains(block.id) }
    }

    val activeSeriesCourse: SeriesCourse?
        get() = when (val dest = destination) {
            is LearningDestination.CourseOverview -> seriesCourses[dest.courseId]
            is LearningDestination.CourseStudyBlock -> seriesCourses[dest.courseId]
            is LearningDestination.CourseBookPdf -> seriesCourses[dest.courseId]
            else -> null
        }
}

class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CourseRepository(application)
    private val _uiState = MutableStateFlow(LearningHubUiState())
    val uiState: StateFlow<LearningHubUiState> = _uiState.asStateFlow()

    init {
        refreshLocalData()
    }

    fun refreshLocalData() {
        val tracks = repo.loadTracks()
        val series = repo.loadAllSeriesCourses(tracks)
        _uiState.update {
            it.copy(
                tracks = tracks,
                seriesCourses = series,
                completedBlockIds = repo.getCompletedBlockIds(),
                lastTrackId = repo.getLastTrackId(),
                lastCourseId = repo.getLastCourseId(),
                lastBlockId = repo.getLastBlockId()
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openTrack(track: LearningTrackEntry) {
        repo.recordLastOpened(trackId = track.id, courseId = track.id, blockId = repo.getLastBlockId())
        when (track.type) {
            LearningTrackType.DAILY_MODULES -> {
                _uiState.update {
                    it.copy(
                        destination = LearningDestination.ArabicCatalog,
                        lastTrackId = track.id
                    )
                }
            }
            LearningTrackType.SERIES_COURSE -> {
                _uiState.update {
                    it.copy(
                        destination = LearningDestination.CourseOverview(track.id),
                        lastTrackId = track.id,
                        lastCourseId = track.id
                    )
                }
            }
        }
    }

    fun openCourseOverview(courseId: String) {
        repo.recordLastOpened(trackId = courseId, courseId = courseId, blockId = repo.getLastBlockId())
        _uiState.update {
            it.copy(
                destination = LearningDestination.CourseOverview(courseId),
                lastTrackId = courseId,
                lastCourseId = courseId
            )
        }
    }

    fun openTopicBlock(
        courseId: String,
        episodeId: String,
        blockId: String,
        initialTimeSec: Int? = null
    ) {
        repo.recordLastOpened(trackId = courseId, courseId = courseId, blockId = blockId)
        _uiState.update {
            it.copy(
                destination = LearningDestination.CourseStudyBlock(
                    courseId = courseId,
                    episodeId = episodeId,
                    blockId = blockId,
                    initialTimeSec = initialTimeSec
                ),
                lastTrackId = courseId,
                lastCourseId = courseId,
                lastBlockId = blockId
            )
        }
        ensureCoursePdfLoaded(courseId)
    }

    fun openFullBookPdf(courseId: String, initialPage: Int = 1) {
        repo.recordLastOpened(trackId = courseId, courseId = courseId, blockId = repo.getLastBlockId())
        _uiState.update {
            it.copy(
                destination = LearningDestination.CourseBookPdf(courseId, initialPage),
                lastTrackId = courseId,
                lastCourseId = courseId
            )
        }
        ensureCoursePdfLoaded(courseId)
    }

    fun ensureCoursePdfLoaded(courseId: String) {
        val course = _uiState.value.seriesCourses[courseId] ?: return
        val existing = _uiState.value.activeCoursePdfFile
        if (existing != null && existing.name == course.bookPdfFileName && existing.exists()) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeCoursePdfFile = null,
                    isLoadingCoursePdf = true,
                    coursePdfError = null
                )
            }
            val resolved = repo.resolveCoursePdf(course)
            _uiState.update {
                it.copy(
                    activeCoursePdfFile = resolved,
                    isLoadingCoursePdf = false,
                    coursePdfError = if (resolved == null) {
                        "Kitab PDF belum tersedia offline. Coba tekan Sinkronkan atau periksa internet."
                    } else null
                )
            }
        }
    }

    fun navigateBack(): Boolean {
        return when (val current = _uiState.value.destination) {
            LearningDestination.Library -> false
            LearningDestination.ArabicCatalog -> {
                _uiState.update { it.copy(destination = LearningDestination.Library) }
                true
            }
            is LearningDestination.CourseOverview -> {
                _uiState.update { it.copy(destination = LearningDestination.Library) }
                true
            }
            is LearningDestination.CourseStudyBlock -> {
                _uiState.update { it.copy(destination = LearningDestination.CourseOverview(current.courseId)) }
                true
            }
            is LearningDestination.CourseBookPdf -> {
                _uiState.update { it.copy(destination = LearningDestination.CourseOverview(current.courseId)) }
                true
            }
        }
    }

    fun resetToLibrary() {
        _uiState.update { it.copy(destination = LearningDestination.Library) }
    }

    fun toggleBlockCompleted(blockId: String) {
        val updated = repo.toggleBlockCompleted(blockId)
        _uiState.update { it.copy(completedBlockIds = updated) }
    }

    fun mergeCompletedBlocksFromCloud(cloudBlocks: Set<String>) {
        val merged = repo.mergeCompletedBlocksFromCloud(cloudBlocks)
        _uiState.update { it.copy(completedBlockIds = merged) }
    }

    fun markCompletedAndOpenNext(courseId: String, currentBlockId: String) {
        val updated = repo.markBlockCompleted(currentBlockId)
        val course = _uiState.value.seriesCourses[courseId]
        if (course == null) {
            _uiState.update { it.copy(completedBlockIds = updated) }
            return
        }
        val flat = course.allBlocks
        val currentIdx = flat.indexOfFirst { (_, block) -> block.id == currentBlockId }
        if (currentIdx >= 0 && currentIdx + 1 < flat.size) {
            val (nextEp, nextBlock) = flat[currentIdx + 1]
            repo.recordLastOpened(trackId = courseId, courseId = courseId, blockId = nextBlock.id)
            _uiState.update {
                it.copy(
                    completedBlockIds = updated,
                    lastBlockId = nextBlock.id,
                    destination = LearningDestination.CourseStudyBlock(
                        courseId = courseId,
                        episodeId = nextEp.id,
                        blockId = nextBlock.id
                    )
                )
            }
        } else {
            _uiState.update { it.copy(completedBlockIds = updated) }
        }
    }

    fun syncCourses(onDone: (CourseSyncResult) -> Unit) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = repo.syncCoursesFromRemote()
            val tracks = repo.loadTracks()
            val series = repo.loadAllSeriesCourses(tracks)
            _uiState.update {
                it.copy(
                    tracks = tracks,
                    seriesCourses = series,
                    isSyncing = false
                )
            }
            onDone(result)
        }
    }
}
