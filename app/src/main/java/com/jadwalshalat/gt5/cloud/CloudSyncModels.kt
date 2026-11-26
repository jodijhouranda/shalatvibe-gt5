package com.jadwalshalat.gt5.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind
import com.jadwalshalat.gt5.arabic.KamusSrsScheduler

@Keep
@IgnoreExtraProperties
data class KamusEntryDoc(
    @DocumentId val id: String = "",
    val arabic: String = "",
    val transliteration: String = "",
    val meaning: String = "",
    val turatsOrigin: String = "",
    val synonymNote: String = "",
    val kind: String = "word",
    val lessonId: String = "",
    val lessonTitle: String = "",
    val segmentLabel: String = "",
    val segmentStartSec: Long = 0L,
    val contextSentence: String = "",
    val personalNote: String = "",
    val masteryStage: Long = 0L,
    val nextReviewEpochDay: Long = 0L,
    val createdAtMillis: Long = 0L,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toKamusEntry(): KamusEntry {
        val fallbackMillis = createdAtMillis.takeIf { it > 0L }
            ?: createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        return KamusEntry(
            id = id,
            arabic = arabic,
            transliteration = transliteration,
            meaning = meaning,
            turatsOrigin = turatsOrigin,
            synonymNote = synonymNote,
            kind = KamusEntryKind.fromRaw(kind),
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            segmentLabel = segmentLabel,
            segmentStartSec = segmentStartSec.toInt().coerceIn(0, 86400),
            contextSentence = contextSentence,
            personalNote = personalNote,
            masteryStage = masteryStage.toInt().coerceIn(0, KamusSrsScheduler.MASTERED_STAGE),
            nextReviewEpochDay = nextReviewEpochDay.coerceAtLeast(0L),
            createdAtMillis = fallbackMillis,
            reviewCount = reviewCount.toInt().coerceAtLeast(0)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class LearningProgressDoc(
    @DocumentId val id: String = "",
    val completedArabicIds: List<String> = emptyList(),
    val favoriteArabicIds: List<String> = emptyList(),
    val completedCourseBlockIds: List<String> = emptyList(),
    val lastOpenedLessonId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface CloudSyncState {
    data object SignedOut : CloudSyncState
    data class Syncing(val userId: String, val email: String, val displayName: String) : CloudSyncState
    data class Synced(
        val userId: String,
        val email: String,
        val displayName: String,
        val syncedWordCount: Int
    ) : CloudSyncState
    data class Error(
        val userId: String,
        val email: String,
        val displayName: String,
        val message: String
    ) : CloudSyncState
}
