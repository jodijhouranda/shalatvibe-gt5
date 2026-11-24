package com.jadwalshalat.gt5.learning

import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class LearningTrackType {
    DAILY_MODULES,
    SERIES_COURSE;

    companion object {
        fun fromRaw(raw: String?): LearningTrackType {
            return when (raw?.uppercase(Locale.US)) {
                "SERIES_COURSE" -> SERIES_COURSE
                else -> DAILY_MODULES
            }
        }
    }
}

data class LearningTrackEntry(
    val id: String,
    val type: LearningTrackType,
    val title: String,
    val subtitle: String,
    val badge: String,
    val accentHex: String,
    val iconName: String,
    val manifestUrl: String,
    val assetPath: String = ""
)

data class GlossaryTerm(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val term: String,
    val meaning: String,
    val fiqhNote: String,
    val exampleInVideo: String,
    val episodeNumber: Int,
    val blockId: String,
    val exampleTimeSec: Int
)

data class TranscriptSegment(
    val id: String,
    val startSec: Int,
    val endSec: Int,
    val arabic: String,
    val indonesian: String,
    val termIds: List<String>
) {
    val timeRangeLabel: String
        get() = if (endSec > startSec) {
            "${ArabicCatalogParser.formatTimestamp(startSec)} – ${ArabicCatalogParser.formatTimestamp(endSec)}"
        } else {
            ArabicCatalogParser.formatTimestamp(startSec)
        }
}

data class TopicBlock(
    val id: String,
    val order: Int,
    val title: String,
    val startSec: Int,
    val endSec: Int,
    val pdfPageHint: Int,
    val summary: String,
    val keyTermIds: List<String>,
    val segments: List<TranscriptSegment>
) {
    val timeRangeLabel: String
        get() = if (endSec > startSec) {
            "${ArabicCatalogParser.formatTimestamp(startSec)} – ${ArabicCatalogParser.formatTimestamp(endSec)}"
        } else {
            ArabicCatalogParser.formatTimestamp(startSec)
        }

    val durationMinutes: Int
        get() = ((endSec - startSec).coerceAtLeast(0) / 60).coerceAtLeast(1)
}

data class CourseEpisode(
    val episodeNumber: Int,
    val id: String,
    val title: String,
    val arabicTitle: String,
    val youtubeVideoId: String,
    val durationSec: Int,
    val pdfStartPage: Int,
    val pdfEndPage: Int,
    val summary: String,
    val blocks: List<TopicBlock>
) {
    val durationLabel: String
        get() = ArabicCatalogParser.formatTimestamp(durationSec)

    val pdfRangeLabel: String
        get() = "Hal. $pdfStartPage–$pdfEndPage"
}

data class SeriesCourse(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val instructor: String,
    val subtitle: String,
    val bookTitle: String,
    val bookPdfFileName: String,
    val bookPdfUrl: String,
    val bookTotalPages: Int,
    val glossary: List<GlossaryTerm>,
    val episodes: List<CourseEpisode>
) {
    val allBlocks: List<Pair<CourseEpisode, TopicBlock>>
        get() = episodes.flatMap { episode -> episode.blocks.map { episode to it } }

    val totalBlockCount: Int
        get() = episodes.sumOf { it.blocks.size }

    fun glossaryById(id: String): GlossaryTerm? = glossary.firstOrNull { it.id == id }

    fun findEpisodeAndBlock(blockId: String): Pair<CourseEpisode, TopicBlock>? {
        for (ep in episodes) {
            val found = ep.blocks.firstOrNull { it.id == blockId }
            if (found != null) return ep to found
        }
        return null
    }
}

object CourseCatalogParser {

    fun parseTracksIndexJson(rawJson: String): List<LearningTrackEntry> {
        if (rawJson.isBlank()) return emptyList()
        val root = JSONObject(rawJson)
        val array = root.optJSONArray("courses") ?: return emptyList()
        val list = ArrayList<LearningTrackEntry>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id").trim()
            val title = item.optString("title").trim()
            if (id.isBlank() || title.isBlank()) continue
            list += LearningTrackEntry(
                id = id,
                type = LearningTrackType.fromRaw(item.optString("type")),
                title = title,
                subtitle = item.optString("subtitle").trim(),
                badge = item.optString("badge").trim(),
                accentHex = item.optString("accentHex", "#10B981").trim(),
                iconName = item.optString("iconName", "menu_book").trim(),
                manifestUrl = item.optString("manifestUrl").trim(),
                assetPath = item.optString("assetPath").trim()
            )
        }
        return list
    }

    fun parseSeriesCourseJson(rawJson: String): SeriesCourse? {
        if (rawJson.isBlank()) return null
        val root = JSONObject(rawJson)
        val id = root.optString("id").trim()
        val title = root.optString("title").trim()
        if (id.isBlank() || title.isBlank()) return null

        val glossary = parseGlossary(root.optJSONArray("glossary"))
        val episodes = parseEpisodes(root.optJSONArray("episodes"))
        return SeriesCourse(
            id = id,
            title = title,
            arabicTitle = root.optString("arabicTitle").trim(),
            instructor = root.optString("instructor").trim(),
            subtitle = root.optString("subtitle").trim(),
            bookTitle = root.optString("bookTitle").trim(),
            bookPdfFileName = root.optString("bookPdfFileName").trim(),
            bookPdfUrl = root.optString("bookPdfUrl").trim(),
            bookTotalPages = root.optInt("bookTotalPages", 95),
            glossary = glossary,
            episodes = episodes
        )
    }

    private fun parseGlossary(array: JSONArray?): List<GlossaryTerm> {
        if (array == null) return emptyList()
        val list = ArrayList<GlossaryTerm>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val term = obj.optString("term").trim()
            if (id.isBlank() || term.isBlank()) continue
            list += GlossaryTerm(
                id = id,
                arabic = obj.optString("arabic").trim(),
                transliteration = obj.optString("transliteration").trim(),
                term = term,
                meaning = obj.optString("meaning").trim(),
                fiqhNote = obj.optString("fiqhNote").trim(),
                exampleInVideo = obj.optString("exampleInVideo").trim(),
                episodeNumber = obj.optInt("episodeNumber", 1),
                blockId = obj.optString("blockId").trim(),
                exampleTimeSec = obj.optInt("exampleTimeSec", 0)
            )
        }
        return list
    }

    private fun parseEpisodes(array: JSONArray?): List<CourseEpisode> {
        if (array == null) return emptyList()
        val list = ArrayList<CourseEpisode>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val title = obj.optString("title").trim()
            val videoId = obj.optString("youtubeVideoId").trim()
            if (id.isBlank() || title.isBlank()) continue
            list += CourseEpisode(
                episodeNumber = obj.optInt("episodeNumber", i + 1),
                id = id,
                title = title,
                arabicTitle = obj.optString("arabicTitle").trim(),
                youtubeVideoId = videoId,
                durationSec = obj.optInt("durationSec", 0),
                pdfStartPage = obj.optInt("pdfStartPage", 1),
                pdfEndPage = obj.optInt("pdfEndPage", 1),
                summary = obj.optString("summary").trim(),
                blocks = parseBlocks(obj.optJSONArray("blocks"))
            )
        }
        return list
    }

    private fun parseBlocks(array: JSONArray?): List<TopicBlock> {
        if (array == null) return emptyList()
        val list = ArrayList<TopicBlock>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val title = obj.optString("title").trim()
            if (id.isBlank() || title.isBlank()) continue
            list += TopicBlock(
                id = id,
                order = obj.optInt("order", i + 1),
                title = title,
                startSec = obj.optInt("startSec", 0),
                endSec = obj.optInt("endSec", 0),
                pdfPageHint = obj.optInt("pdfPageHint", 1),
                summary = obj.optString("summary").trim(),
                keyTermIds = parseStringList(obj.optJSONArray("keyTermIds")),
                segments = parseSegments(obj.optJSONArray("segments"))
            )
        }
        return list
    }

    private fun parseSegments(array: JSONArray?): List<TranscriptSegment> {
        if (array == null) return emptyList()
        val list = ArrayList<TranscriptSegment>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val arabic = obj.optString("arabic").trim()
            if (id.isBlank() || arabic.isBlank()) continue
            list += TranscriptSegment(
                id = id,
                startSec = obj.optInt("startSec", 0),
                endSec = obj.optInt("endSec", 0),
                arabic = arabic,
                indonesian = obj.optString("indonesian").trim(),
                termIds = parseStringList(obj.optJSONArray("termIds"))
            )
        }
        return list
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val v = array.optString(i).trim()
            if (v.isNotBlank()) list += v
        }
        return list
    }
}
