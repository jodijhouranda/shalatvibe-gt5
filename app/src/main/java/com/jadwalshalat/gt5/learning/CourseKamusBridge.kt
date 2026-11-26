package com.jadwalshalat.gt5.learning

import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.KamusCodec
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind

data class CourseLessonRef(
    val courseId: String,
    val episodeId: String,
    val blockId: String
)

/**
 * Jembatan antara modul Seri Kursus (mis. Fiqh Muamalah) dan sistem Kamus Saya + AI Kamus.
 * Mengonversi kata/frase transkrip & istilah glosarium menjadi draf KamusEntry lengkap dengan
 * tautan balik ke blok & timestamp video kursus.
 */
object CourseKamusBridge {

    private const val COURSE_ID_PREFIX = "course:"

    fun buildCourseLessonId(courseId: String, episodeId: String, blockId: String): String {
        return "$COURSE_ID_PREFIX$courseId:$episodeId:$blockId"
    }

    fun parseCourseLessonId(lessonId: String): CourseLessonRef? {
        if (!lessonId.startsWith(COURSE_ID_PREFIX)) return null
        val parts = lessonId.removePrefix(COURSE_ID_PREFIX).split(':')
        if (parts.size != 3 || parts.any { it.isBlank() }) return null
        return CourseLessonRef(
            courseId = parts[0],
            episodeId = parts[1],
            blockId = parts[2]
        )
    }

    fun buildDraftFromTranscriptWord(
        arabicPhrase: String,
        course: SeriesCourse,
        episode: CourseEpisode,
        block: TopicBlock,
        segment: TranscriptSegment?,
        existingEntries: List<KamusEntry>,
        overrideKind: KamusEntryKind? = null
    ): KamusEntry {
        val clean = ArabicTextUtils.stripOuterPunctuation(arabicPhrase).ifBlank { arabicPhrase.trim() }
        val normKey = ArabicTextUtils.normalizeForLookup(clean)
        val existingSaved = if (normKey.isNotBlank()) {
            existingEntries.firstOrNull { it.normalizedKey == normKey }
        } else null
        val matchedTerm = findMatchingGlossaryTerm(normKey, course.glossary)

        val lessonId = buildCourseLessonId(course.id, episode.id, block.id)
        val inferredKind = overrideKind
            ?: existingSaved?.kind
            ?: if (clean.contains(' ')) KamusEntryKind.PHRASE else KamusEntryKind.WORD

        val glossaryMeaning = matchedTerm?.let { formatTermMeaning(it) }.orEmpty()

        return KamusEntry(
            id = existingSaved?.id ?: KamusCodec.createStableId(clean, lessonId),
            arabic = existingSaved?.arabic?.ifBlank { clean } ?: matchedTerm?.arabic?.ifBlank { clean } ?: clean,
            transliteration = existingSaved?.transliteration.orEmpty()
                .ifBlank { matchedTerm?.transliteration.orEmpty() },
            meaning = existingSaved?.meaning.orEmpty()
                .ifBlank { glossaryMeaning },
            turatsOrigin = existingSaved?.turatsOrigin.orEmpty()
                .ifBlank { matchedTerm?.fiqhNote.orEmpty() },
            synonymNote = existingSaved?.synonymNote.orEmpty(),
            kind = inferredKind,
            lessonId = lessonId,
            lessonTitle = "${course.title} • Seri ${episode.episodeNumber}: ${block.title}",
            segmentLabel = segment?.timeRangeLabel ?: block.timeRangeLabel,
            segmentStartSec = segment?.startSec ?: block.startSec,
            contextSentence = segment?.arabic.orEmpty().ifBlank { matchedTerm?.exampleInVideo.orEmpty() },
            personalNote = existingSaved?.personalNote.orEmpty(),
            masteryStage = existingSaved?.masteryStage ?: 0,
            nextReviewEpochDay = existingSaved?.nextReviewEpochDay ?: 0L,
            createdAtMillis = existingSaved?.createdAtMillis ?: 0L,
            reviewCount = existingSaved?.reviewCount ?: 0
        )
    }

    fun buildDraftFromGlossaryTerm(
        term: GlossaryTerm,
        course: SeriesCourse,
        fallbackEpisode: CourseEpisode? = null,
        fallbackBlock: TopicBlock? = null,
        existingEntries: List<KamusEntry> = emptyList()
    ): KamusEntry {
        val cleanArabic = ArabicTextUtils.stripOuterPunctuation(term.arabic).ifBlank { term.arabic.trim() }
        val normKey = ArabicTextUtils.normalizeForLookup(cleanArabic)
        val existingSaved = if (normKey.isNotBlank()) {
            existingEntries.firstOrNull { it.normalizedKey == normKey }
        } else null

        val resolvedPair = if (term.blockId.isNotBlank()) {
            course.findEpisodeAndBlock(term.blockId)
        } else null
        val targetEpisode = resolvedPair?.first ?: fallbackEpisode
        val targetBlock = resolvedPair?.second ?: fallbackBlock

        val lessonId = if (targetEpisode != null && targetBlock != null) {
            buildCourseLessonId(course.id, targetEpisode.id, targetBlock.id)
        } else {
            course.id
        }
        val lessonTitle = if (targetEpisode != null && targetBlock != null) {
            "${course.title} • Seri ${targetEpisode.episodeNumber}: ${targetBlock.title}"
        } else {
            "${course.title} • Seri ${term.episodeNumber}"
        }

        val kind = existingSaved?.kind
            ?: if (cleanArabic.contains(' ')) KamusEntryKind.PHRASE else KamusEntryKind.WORD

        return KamusEntry(
            id = existingSaved?.id ?: KamusCodec.createStableId(cleanArabic, lessonId),
            arabic = existingSaved?.arabic?.ifBlank { cleanArabic } ?: cleanArabic,
            transliteration = existingSaved?.transliteration.orEmpty().ifBlank { term.transliteration },
            meaning = existingSaved?.meaning.orEmpty().ifBlank { formatTermMeaning(term) },
            turatsOrigin = existingSaved?.turatsOrigin.orEmpty().ifBlank { term.fiqhNote },
            synonymNote = existingSaved?.synonymNote.orEmpty(),
            kind = kind,
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            segmentLabel = targetBlock?.timeRangeLabel.orEmpty(),
            segmentStartSec = term.exampleTimeSec.takeIf { it > 0 } ?: (targetBlock?.startSec ?: 0),
            contextSentence = term.exampleInVideo,
            personalNote = existingSaved?.personalNote.orEmpty(),
            masteryStage = existingSaved?.masteryStage ?: 0,
            nextReviewEpochDay = existingSaved?.nextReviewEpochDay ?: 0L,
            createdAtMillis = existingSaved?.createdAtMillis ?: 0L,
            reviewCount = existingSaved?.reviewCount ?: 0
        )
    }

    private fun findMatchingGlossaryTerm(normKey: String, glossary: List<GlossaryTerm>): GlossaryTerm? {
        if (normKey.length < 2) return null
        return glossary.firstOrNull { term ->
            val termNorm = ArabicTextUtils.normalizeForLookup(term.arabic)
            termNorm == normKey || (termNorm.length >= 3 && (termNorm.removePrefix("ال") == normKey.removePrefix("ال")))
        }
    }

    private fun formatTermMeaning(term: GlossaryTerm): String {
        val label = term.term.trim()
        val meaning = term.meaning.trim()
        return when {
            label.isBlank() -> meaning
            meaning.isBlank() -> label
            meaning.startsWith(label, ignoreCase = true) -> meaning
            else -> "$label — $meaning"
        }
    }
}
