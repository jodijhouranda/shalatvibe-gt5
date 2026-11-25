package com.jadwalshalat.gt5.arabic

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
enum class KamusEntryKind(val label: String, val raw: String) {
    WORD("Kata", "word"),
    PHRASE("Frase", "phrase"),
    IDIOM("Idiom", "idiom");

    companion object {
        fun fromRaw(raw: String?): KamusEntryKind {
            return entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: WORD
        }
    }
}

@Immutable
enum class KamusFilter(val label: String) {
    ALL("Semua"),
    DUE_REVIEW("Siap Review"),
    WORDS("Kata"),
    PHRASES("Frase & Idiom"),
    MASTERED("Dihafal")
}

@Immutable
data class KamusEntry(
    val id: String,
    val arabic: String,
    val transliteration: String = "",
    val meaning: String = "",
    val turatsOrigin: String = "",
    val synonymNote: String = "",
    val kind: KamusEntryKind = KamusEntryKind.WORD,
    val lessonId: String = "",
    val lessonTitle: String = "",
    val segmentLabel: String = "",
    val segmentStartSec: Int = 0,
    val contextSentence: String = "",
    val personalNote: String = "",
    val masteryStage: Int = 0,
    val nextReviewEpochDay: Long = 0L,
    val createdAtMillis: Long = 0L,
    val reviewCount: Int = 0
) {
    val isMastered: Boolean get() = masteryStage >= KamusSrsScheduler.MASTERED_STAGE

    val normalizedKey: String get() = ArabicTextUtils.normalizeForLookup(arabic)
}

object KamusSrsScheduler {
    const val MASTERED_STAGE = 5
    val STAGE_INTERVALS_DAYS = intArrayOf(0, 1, 3, 7, 14, 30)

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun isDue(entry: KamusEntry, todayEpochDay: Long = todayEpochDay()): Boolean {
        if (entry.masteryStage >= MASTERED_STAGE) return false
        return entry.nextReviewEpochDay <= 0L || entry.nextReviewEpochDay <= todayEpochDay
    }

    fun advance(
        entry: KamusEntry,
        remembered: Boolean,
        todayEpochDay: Long = todayEpochDay()
    ): KamusEntry {
        return if (remembered) {
            val nextStage = (entry.masteryStage + 1).coerceAtMost(MASTERED_STAGE)
            val daysWait = STAGE_INTERVALS_DAYS[nextStage]
            entry.copy(
                masteryStage = nextStage,
                nextReviewEpochDay = todayEpochDay + daysWait,
                reviewCount = entry.reviewCount + 1
            )
        } else {
            entry.copy(
                masteryStage = 0,
                nextReviewEpochDay = todayEpochDay,
                reviewCount = entry.reviewCount + 1
            )
        }
    }

    fun intervalLabelForNextRecall(currentStage: Int): String {
        val next = (currentStage + 1).coerceAtMost(MASTERED_STAGE)
        return when (next) {
            1 -> "+1 hari"
            2 -> "+3 hari"
            3 -> "+7 hari"
            4 -> "+14 hari"
            MASTERED_STAGE -> "Hafal ✓"
            else -> "Sekarang"
        }
    }

    fun statusBadge(entry: KamusEntry, todayEpochDay: Long = todayEpochDay()): String {
        if (entry.isMastered) return "Dihafal"
        if (isDue(entry, todayEpochDay)) return "Siap Review"
        val diff = (entry.nextReviewEpochDay - todayEpochDay).coerceAtLeast(1L)
        return "Review +${diff}h"
    }
}

object KamusCodec {
    private val CSV_HEADERS = listOf(
        "Arabic",
        "Arti Indonesia",
        "Transliterasi",
        "Akar Turats",
        "Nuansa Sinonim",
        "Konteks Kalimat",
        "Pelajaran",
        "Timestamp Detik",
        "Jenis",
        "Tahap SRS",
        "Catatan"
    )

    fun createStableId(arabic: String, lessonId: String): String {
        val key = ArabicTextUtils.normalizeForLookup(arabic)
        val raw = "${key}|${lessonId}".lowercase()
        var h = 2166136261L
        for (ch in raw) {
            h = h xor ch.code.toLong()
            h *= 16777619L
        }
        return "km_${java.lang.Long.toUnsignedString(h, 36)}"
    }

    fun toBackupJson(entries: List<KamusEntry>): String {
        val sb = StringBuilder()
        sb.append("{\n  \"version\": 1,\n  \"entries\": [")
        entries.forEachIndexed { idx, e ->
            if (idx > 0) sb.append(',')
            sb.append("\n    {")
            sb.append("\"id\": ").append(MiniJson.quote(e.id)).append(", ")
            sb.append("\"arabic\": ").append(MiniJson.quote(e.arabic)).append(", ")
            sb.append("\"transliteration\": ").append(MiniJson.quote(e.transliteration)).append(", ")
            sb.append("\"meaning\": ").append(MiniJson.quote(e.meaning)).append(", ")
            sb.append("\"turatsOrigin\": ").append(MiniJson.quote(e.turatsOrigin)).append(", ")
            sb.append("\"synonymNote\": ").append(MiniJson.quote(e.synonymNote)).append(", ")
            sb.append("\"kind\": ").append(MiniJson.quote(e.kind.raw)).append(", ")
            sb.append("\"lessonId\": ").append(MiniJson.quote(e.lessonId)).append(", ")
            sb.append("\"lessonTitle\": ").append(MiniJson.quote(e.lessonTitle)).append(", ")
            sb.append("\"segmentLabel\": ").append(MiniJson.quote(e.segmentLabel)).append(", ")
            sb.append("\"segmentStartSec\": ").append(e.segmentStartSec).append(", ")
            sb.append("\"contextSentence\": ").append(MiniJson.quote(e.contextSentence)).append(", ")
            sb.append("\"personalNote\": ").append(MiniJson.quote(e.personalNote)).append(", ")
            sb.append("\"masteryStage\": ").append(e.masteryStage).append(", ")
            sb.append("\"nextReviewEpochDay\": ").append(e.nextReviewEpochDay).append(", ")
            sb.append("\"createdAtMillis\": ").append(e.createdAtMillis).append(", ")
            sb.append("\"reviewCount\": ").append(e.reviewCount)
            sb.append("}")
        }
        sb.append("\n  ]\n}")
        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    fun fromBackupJson(rawJson: String): List<KamusEntry> {
        val parsed = MiniJson.parse(rawJson)
        val rawList: List<Any?> = when (parsed) {
            is Map<*, *> -> (parsed["entries"] as? List<Any?>).orEmpty()
            is List<*> -> parsed as List<Any?>
            else -> return emptyList()
        }
        return rawList.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val arabic = m["arabic"]?.toString().orEmpty().trim()
            if (arabic.isBlank()) return@mapNotNull null
            val lessonId = m["lessonId"]?.toString().orEmpty()
            val id = m["id"]?.toString()?.takeIf { it.isNotBlank() } ?: createStableId(arabic, lessonId)
            KamusEntry(
                id = id,
                arabic = arabic,
                transliteration = m["transliteration"]?.toString().orEmpty(),
                meaning = m["meaning"]?.toString().orEmpty(),
                turatsOrigin = m["turatsOrigin"]?.toString().orEmpty(),
                synonymNote = m["synonymNote"]?.toString().orEmpty(),
                kind = KamusEntryKind.fromRaw(m["kind"]?.toString()),
                lessonId = lessonId,
                lessonTitle = m["lessonTitle"]?.toString().orEmpty(),
                segmentLabel = m["segmentLabel"]?.toString().orEmpty(),
                segmentStartSec = (m["segmentStartSec"] as? Number)?.toInt() ?: 0,
                contextSentence = m["contextSentence"]?.toString().orEmpty(),
                personalNote = m["personalNote"]?.toString().orEmpty(),
                masteryStage = ((m["masteryStage"] as? Number)?.toInt() ?: 0).coerceIn(0, KamusSrsScheduler.MASTERED_STAGE),
                nextReviewEpochDay = (m["nextReviewEpochDay"] as? Number)?.toLong() ?: 0L,
                createdAtMillis = (m["createdAtMillis"] as? Number)?.toLong() ?: 0L,
                reviewCount = ((m["reviewCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
            )
        }
    }

    /**
     * Ekspor format CSV standar RFC-4180 siap diimpor langsung ke Anki Desktop/AnkiDroid atau spreadsheet.
     */
    fun toAnkiCsv(entries: List<KamusEntry>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM agar Excel & Anki membaca huruf Arab berharakat tanpa corrupt
        sb.append(CSV_HEADERS.joinToString(",") { escapeCsv(it) }).append("\r\n")
        for (e in entries) {
            val row = listOf(
                e.arabic,
                e.meaning,
                e.transliteration,
                e.turatsOrigin,
                e.synonymNote,
                e.contextSentence,
                e.lessonTitle,
                e.segmentStartSec.toString(),
                e.kind.raw,
                e.masteryStage.toString(),
                e.personalNote
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(field: String): String {
        val needsQuote = field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')
        if (!needsQuote) return field
        return "\"${field.replace("\"", "\"\"")}\""
    }
}

/**
 * Parser & Quoter JSON ringan murni Kotlin tanpa ketergantungan Android SDK/org.json,
 * sehingga lolos unit test JVM murni secara langsung.
 */
object MiniJson {
    fun quote(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun parse(src: String): Any? {
        val trimmed = src.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (trimmed.isEmpty()) return null
        return try {
            Parser(trimmed).parseValue()
        } catch (_: Exception) {
            null
        }
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWs()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBool()
                'n' -> {
                    pos += 4
                    null
                }
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val map = LinkedHashMap<String, Any?>()
            pos++ // '{'
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                val key = parseString()
                skipWs()
                if (pos < src.length && src[pos] == ':') pos++
                val value = parseValue()
                map[key] = value
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == '}') pos++
                    break
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            val list = ArrayList<Any?>()
            pos++ // '['
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue())
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == ']') pos++
                    break
                }
            }
            return list
        }

        private fun parseString(): String {
            if (pos < src.length && src[pos] == '"') pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') break
                if (c == '\\' && pos < src.length) {
                    when (val e = src[pos++]) {
                        '"', '\\', '/' -> sb.append(e)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 <= src.length) {
                                val code = src.substring(pos, pos + 4).toInt(16)
                                sb.append(code.toChar())
                                pos += 4
                            }
                        }
                        else -> sb.append(e)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseBool(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            var isFloat = false
            while (pos < src.length) {
                val c = src[pos]
                if (c in '0'..'9') {
                    pos++
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    isFloat = true
                    pos++
                } else {
                    break
                }
            }
            val raw = src.substring(start, pos)
            return if (isFloat) raw.toDouble() else raw.toLong()
        }

        private fun skipWs() {
            while (pos < src.length) {
                when (src[pos]) {
                    ' ', '\n', '\r', '\t' -> pos++
                    else -> break
                }
            }
        }
    }
}
