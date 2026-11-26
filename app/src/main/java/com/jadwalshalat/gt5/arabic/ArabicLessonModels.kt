package com.jadwalshalat.gt5.arabic

import org.json.JSONObject
import java.util.Locale

enum class LessonCategory(val label: String) {
    VIDEO_ANALYSIS("Analisis Video"),
    DAILY_LEXICAL("Leksikal Harian");

    companion object {
        fun fromRaw(raw: String?): LessonCategory {
            return when (raw?.uppercase(Locale.US)) {
                "DAILY_LEXICAL" -> DAILY_LEXICAL
                else -> VIDEO_ANALYSIS
            }
        }
    }
}

enum class PdfReadingMode(val label: String) {
    ORIGINAL("Asli"),
    SEPIA("Sepia"),
    DARK_INVERT("Gelap");

    fun next(): PdfReadingMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromName(name: String?): PdfReadingMode {
            return entries.firstOrNull { it.name == name } ?: ORIGINAL
        }
    }
}

enum class LessonFilter(val label: String) {
    ALL("Semua"),
    VIDEO("Analisis Video"),
    INCOMPLETE("Belum Selesai"),
    FAVORITE("Favorit")
}

data class LessonSegment(
    val label: String,
    val startSec: Int,
    val endSec: Int
)

data class ArabicLesson(
    val id: String,
    val category: LessonCategory,
    val dayLabel: String,
    val dateLabel: String,
    val title: String,
    val subtitle: String,
    val sourceName: String,
    val level: String,
    val youtubeVideoId: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val pdfFileName: String,
    val pdfUrl: String,
    val driveFileId: String,
    val segments: List<LessonSegment>,
    val jsonFileName: String = "",
    val jsonUrl: String = ""
) {
    val hasVideo: Boolean
        get() = youtubeVideoId.isNotBlank()

    val hasPdfSource: Boolean
        get() = pdfUrl.isNotBlank() || driveFileId.isNotBlank()

    val segmentRangeLabel: String
        get() = if (endSeconds > startSeconds) {
            "${ArabicCatalogParser.formatTimestamp(startSeconds)} – ${ArabicCatalogParser.formatTimestamp(endSeconds)}"
        } else if (startSeconds > 0) {
            "Mulai ${ArabicCatalogParser.formatTimestamp(startSeconds)}"
        } else {
            "Video Penuh"
        }

    val youtubeThumbnailUrl: String
        get() = if (hasVideo) "https://img.youtube.com/vi/$youtubeVideoId/hqdefault.jpg" else ""

    fun youtubeWatchUrl(atSeconds: Int = startSeconds): String {
        val sec = atSeconds.coerceAtLeast(0)
        return "https://www.youtube.com/watch?v=$youtubeVideoId&t=${sec}s"
    }
}

object ArabicCatalogParser {

    fun formatTimestamp(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Mengubah link berbagi Google Drive menjadi URL unduhan langsung bila diperlukan.
     */
    fun normalizeDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val match = Regex("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)").find(trimmed)
        if (match != null) {
            val fileId = match.groupValues[1]
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }
        return trimmed
    }

    fun parseCatalogJson(jsonString: String): List<ArabicLesson> {
        val root = JSONObject(jsonString)
        val arr = root.optJSONArray("lessons") ?: return emptyList()
        val result = ArrayList<ArabicLesson>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim()
            val title = obj.optString("title", "").trim()
            if (id.isEmpty() || title.isEmpty()) continue

            val segArr = obj.optJSONArray("segments")
            val segments = ArrayList<LessonSegment>()
            if (segArr != null) {
                for (j in 0 until segArr.length()) {
                    val s = segArr.optJSONObject(j) ?: continue
                    val start = s.optInt("startSec", 0).coerceAtLeast(0)
                    val end = s.optInt("endSec", start).coerceAtLeast(start)
                    val label = s.optString("label", "").ifBlank {
                        "${formatTimestamp(start)} – ${formatTimestamp(end)}"
                    }
                    segments.add(LessonSegment(label = label, startSec = start, endSec = end))
                }
            }

            result.add(
                ArabicLesson(
                    id = id,
                    category = LessonCategory.fromRaw(obj.optString("category", "VIDEO_ANALYSIS")),
                    dayLabel = obj.optString("dayLabel", ""),
                    dateLabel = obj.optString("dateLabel", ""),
                    title = title,
                    subtitle = obj.optString("subtitle", ""),
                    sourceName = obj.optString("sourceName", ""),
                    level = obj.optString("level", "CEFR B2–C1"),
                    youtubeVideoId = obj.optString("youtubeVideoId", "").trim(),
                    startSeconds = obj.optInt("startSeconds", 0).coerceAtLeast(0),
                    endSeconds = obj.optInt("endSeconds", 0).coerceAtLeast(0),
                    pdfFileName = obj.optString("pdfFileName", "").trim(),
                    pdfUrl = obj.optString("pdfUrl", "").trim(),
                    driveFileId = obj.optString("driveFileId", "").trim(),
                    segments = segments,
                    jsonFileName = obj.optString("jsonFileName", "").trim(),
                    jsonUrl = obj.optString("jsonUrl", "").trim()
                )
            )
        }
        return result
    }
}
