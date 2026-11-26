package com.jadwalshalat.gt5.arabic

import androidx.compose.runtime.Immutable

@Immutable
data class ArabicTranscriptSegment(
    val label: String,
    val startSec: Int,
    val endSec: Int,
    val arabic: String,
    val indonesian: String
)

@Immutable
data class SemanticShiftEntry(
    val arabic: String,
    val transliteration: String = "",
    val turatsMeaning: String = "",
    val modernMeaning: String = ""
)

@Immutable
data class IdiomEntry(
    val arabic: String,
    val transliteration: String = "",
    val literalMeaning: String = "",
    val idiomaticMeaning: String = "",
    val exampleArabic: String = "",
    val exampleIndonesian: String = ""
)

@Immutable
data class SynonymGroupEntry(
    val words: List<String> = emptyList(),
    val distinction: String = "",
    val collocations: List<String> = emptyList()
)

@Immutable
data class ThematicVocabEntry(
    val arabic: String,
    val indonesian: String,
    val clusterTitle: String = ""
)

@Immutable
data class ArabicLessonContent(
    val lessonId: String,
    val pdfFileName: String,
    val transcriptSegments: List<ArabicTranscriptSegment> = emptyList(),
    val semanticShifts: List<SemanticShiftEntry> = emptyList(),
    val idioms: List<IdiomEntry> = emptyList(),
    val synonymGroups: List<SynonymGroupEntry> = emptyList(),
    val thematicVocab: List<ThematicVocabEntry> = emptyList()
) {
    val totalGlossaryItems: Int
        get() = semanticShifts.size + idioms.size + synonymGroups.size + thematicVocab.size

    fun findSegmentAt(seconds: Int): Int {
        if (transcriptSegments.isEmpty()) return 0
        val idx = transcriptSegments.indexOfFirst { seconds in it.startSec..it.endSec }
        if (idx >= 0) return idx
        return transcriptSegments.indices.minByOrNull { i ->
            val seg = transcriptSegments[i]
            when {
                seconds < seg.startSec -> seg.startSec - seconds
                seconds > seg.endSec -> seconds - seg.endSec
                else -> 0
            }
        } ?: 0
    }
}

@Immutable
data class GlossaryMatchResult(
    val suggestedMeaning: String = "",
    val transliteration: String = "",
    val turatsOrigin: String = "",
    val synonymNote: String = "",
    val kind: KamusEntryKind = KamusEntryKind.WORD,
    val matchedDisplayArabic: String = ""
)

object ArabicContentParser {
    fun contentFileNameForPdf(pdfFileName: String): String {
        val base = pdfFileName.substringBeforeLast('.', pdfFileName)
        return "$base.json"
    }

    fun contentFileNameForLesson(lesson: ArabicLesson): String {
        val explicit = lesson.jsonFileName.trim()
        if (explicit.isNotEmpty()) {
            return if (explicit.endsWith(".json", ignoreCase = true)) explicit else "$explicit.json"
        }
        val fromPdf = lesson.pdfFileName.trim()
        if (fromPdf.isNotEmpty()) {
            return contentFileNameForPdf(fromPdf)
        }
        val fromUrl = lesson.jsonUrl.trim().substringAfterLast('/').substringBefore('?')
        if (fromUrl.endsWith(".json", ignoreCase = true)) {
            return fromUrl
        }
        return "${lesson.id.trim().ifEmpty { "lesson" }}.json"
    }

    fun inferContentUrl(lesson: ArabicLesson): String {
        val explicitUrl = lesson.jsonUrl.trim()
        if (explicitUrl.isNotEmpty()) {
            return ArabicCatalogParser.normalizeDownloadUrl(explicitUrl)
        }
        val fn = contentFileNameForLesson(lesson)
        val url = lesson.pdfUrl
        if (url.contains("/lessons/pdfs/")) {
            return url.substringBeforeLast("/lessons/pdfs/") + "/lessons/arabic_content/" + fn
        }
        if (url.contains("raw.githubusercontent.com/") && url.endsWith(".pdf")) {
            return url.substringBeforeLast('/') + "/../arabic_content/" + fn
        }
        return "https://raw.githubusercontent.com/jodijhouranda/shalatvibe-gt5/main/lessons/arabic_content/$fn"
    }

    @Suppress("UNCHECKED_CAST")
    fun parseContentJson(rawJson: String): ArabicLessonContent? {
        val root = MiniJson.parse(rawJson) as? Map<String, Any?> ?: return null
        val lessonId = root["lessonId"]?.toString().orEmpty()
        val pdfFileName = root["pdfFileName"]?.toString().orEmpty()
        if (pdfFileName.isBlank() && lessonId.isBlank()) return null

        val segsRaw = root["transcriptSegments"] as? List<Any?> ?: emptyList()
        val segments = segsRaw.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val arabic = m["arabic"]?.toString().orEmpty().trim()
            if (arabic.isBlank()) return@mapNotNull null
            ArabicTranscriptSegment(
                label = m["label"]?.toString().orEmpty(),
                startSec = (m["startSec"] as? Number)?.toInt() ?: 0,
                endSec = (m["endSec"] as? Number)?.toInt() ?: 0,
                arabic = arabic,
                indonesian = m["indonesian"]?.toString().orEmpty().trim()
            )
        }

        val semRaw = root["semanticShifts"] as? List<Any?> ?: emptyList()
        val shifts = semRaw.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val arabic = m["arabic"]?.toString().orEmpty().trim()
            if (arabic.isBlank()) return@mapNotNull null
            SemanticShiftEntry(
                arabic = arabic,
                transliteration = m["transliteration"]?.toString().orEmpty().trim(),
                turatsMeaning = m["turatsMeaning"]?.toString().orEmpty().trim(),
                modernMeaning = m["modernMeaning"]?.toString().orEmpty().trim()
            )
        }

        val idmRaw = root["idioms"] as? List<Any?> ?: emptyList()
        val idioms = idmRaw.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val arabic = m["arabic"]?.toString().orEmpty().trim()
            if (arabic.isBlank()) return@mapNotNull null
            IdiomEntry(
                arabic = arabic,
                transliteration = m["transliteration"]?.toString().orEmpty().trim(),
                literalMeaning = m["literalMeaning"]?.toString().orEmpty().trim(),
                idiomaticMeaning = m["idiomaticMeaning"]?.toString().orEmpty().trim(),
                exampleArabic = m["exampleArabic"]?.toString().orEmpty().trim(),
                exampleIndonesian = m["exampleIndonesian"]?.toString().orEmpty().trim()
            )
        }

        val synRaw = root["synonymGroups"] as? List<Any?> ?: emptyList()
        val synonyms = synRaw.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val words = (m["words"] as? List<Any?>).orEmpty().mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            val distinction = m["distinction"]?.toString().orEmpty().trim()
            val colls = (m["collocations"] as? List<Any?>).orEmpty().mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            if (words.isEmpty() && distinction.isBlank()) return@mapNotNull null
            SynonymGroupEntry(
                words = words,
                distinction = distinction,
                collocations = colls
            )
        }

        val thmRaw = root["thematicVocab"] as? List<Any?> ?: emptyList()
        val thematic = thmRaw.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val arabic = m["arabic"]?.toString().orEmpty().trim()
            val indo = m["indonesian"]?.toString().orEmpty().trim()
            if (arabic.isBlank() || indo.isBlank()) return@mapNotNull null
            ThematicVocabEntry(
                arabic = arabic,
                indonesian = indo,
                clusterTitle = m["clusterTitle"]?.toString().orEmpty().trim()
            )
        }

        return ArabicLessonContent(
            lessonId = lessonId,
            pdfFileName = pdfFileName,
            transcriptSegments = segments,
            semanticShifts = shifts,
            idioms = idioms,
            synonymGroups = synonyms,
            thematicVocab = thematic
        )
    }

    /**
     * Mencocokkan kata atau frase yang diketuk terhadap glosarium aktif (Pergeseran Semantik, Idiom, Sinonim, Tematik)
     * agar pengisian arti, asal Turats, dan nuansa sinonim otomatis terisi.
     */
    fun matchInGlossary(queryArabic: String, content: ArabicLessonContent?): GlossaryMatchResult {
        if (content == null || queryArabic.isBlank()) return GlossaryMatchResult()
        val qNorm = ArabicTextUtils.normalizeForLookup(queryArabic)
        val qStem = ArabicTextUtils.stripCommonPrefixesForMatch(qNorm)
        if (qNorm.isBlank()) return GlossaryMatchResult()

        fun matches(candidate: String): Boolean {
            if (candidate.isBlank()) return false
            val parts = candidate.split('/', '•', '–', '-').map { it.trim() }.filter { it.isNotBlank() }
            val allForms = if (parts.size > 1) parts + candidate else listOf(candidate)
            return allForms.any { form ->
                val fNorm = ArabicTextUtils.normalizeForLookup(form)
                val fStem = ArabicTextUtils.stripCommonPrefixesForMatch(fNorm)
                fNorm == qNorm ||
                    fStem == qStem ||
                    (qNorm.length >= 3 && (fNorm.contains(qNorm) || qNorm.contains(fNorm))) ||
                    (qStem.length >= 3 && (fStem.contains(qStem) || qStem.contains(fStem)))
            }
        }

        val shift = content.semanticShifts.firstOrNull { matches(it.arabic) }
        val idiom = content.idioms.firstOrNull { matches(it.arabic) }
        val syn = content.synonymGroups.firstOrNull { grp -> grp.words.any { matches(it) } }
        val theme = content.thematicVocab.firstOrNull { matches(it.arabic) }

        val meaning = when {
            shift != null -> shift.modernMeaning
            idiom != null -> idiom.idiomaticMeaning
            theme != null -> theme.indonesian
            syn != null -> syn.distinction
            else -> ""
        }
        val turatsOrigin = shift?.turatsMeaning.orEmpty()
        val synNote = syn?.let { grp ->
            val wordsJoined = grp.words.joinToString(" / ")
            if (wordsJoined.isNotBlank()) "$wordsJoined — ${grp.distinction}" else grp.distinction
        }.orEmpty()
        val translit = shift?.transliteration?.ifBlank { idiom?.transliteration.orEmpty() }
            ?: idiom?.transliteration.orEmpty()
        val kind = when {
            idiom != null -> KamusEntryKind.IDIOM
            queryArabic.trim().contains(' ') -> KamusEntryKind.PHRASE
            else -> KamusEntryKind.WORD
        }
        val displayArabic = shift?.arabic ?: idiom?.arabic ?: theme?.arabic ?: queryArabic.trim()

        return GlossaryMatchResult(
            suggestedMeaning = meaning,
            transliteration = translit,
            turatsOrigin = turatsOrigin,
            synonymNote = synNote,
            kind = kind,
            matchedDisplayArabic = displayArabic
        )
    }
}
