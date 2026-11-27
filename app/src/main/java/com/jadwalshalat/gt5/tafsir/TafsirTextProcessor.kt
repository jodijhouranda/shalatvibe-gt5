package com.jadwalshalat.gt5.tafsir

object TafsirTextProcessor {

    private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    // Harakat + Quranic annotation marks + tatwil (saat mode Gundul aktif)
    private val GUNDUL_STRIP_REGEX = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u0640\u06D6-\u06ED\u08D3-\u08FF]")
    private val FOOTNOTE_REGEX = Regex("\\[\\[(.+?)\\]\\]", setOf(RegexOption.DOT_MATCHES_ALL))
    private val BLOCK_BREAK_TAG_REGEX = Regex("(?i)</?(p|div|br|li|h[1-6]|blockquote)[^>]*>")
    private val ANY_HTML_TAG_REGEX = Regex("<[^>]+>")
    private val NUMERIC_HEX_ENTITY_REGEX = Regex("&#x([0-9a-fA-F]+);")
    private val NUMERIC_DEC_ENTITY_REGEX = Regex("&#([0-9]+);")
    private val MULTI_SPACE_REGEX = Regex("[ \\t\\x0B\\f]+")
    private val MULTI_NEWLINE_REGEX = Regex("\\n{2,}")

    /**
     * Mengonversi angka Latin menjadi angka Arab-Timur (mis. 12 -> ١٢).
     */
    fun toArabicDigits(number: Int): String {
        val raw = number.toString()
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') {
                sb.append(ARABIC_INDIC_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Ornamen nomor ayat standar mushaf: ﴿١﴾.
     */
    fun formatAyahOrnament(ayah: Int): String = "﴿${toArabicDigits(ayah)}﴾"

    /**
     * Format tampilan teks Arab untuk mode Harakat atau Gundul.
     * Saat Gundul, Alif Wasla (ٱ) dinormalisasi menjadi Alif biasa (ا) agar "بسم الله" rapi.
     */
    fun formatArabicForDisplay(input: String, showHarakat: Boolean): String {
        if (input.isEmpty()) return ""
        if (showHarakat) return input
        val normalizedWasla = input
            .replace('\u0671', '\u0627') // ٱ -> ا
            .replace("\u06DF", "")       // small high rounded zero
            .replace("\u06E0", "")       // small high upright rectangular zero
            .replace("\u06E1", "")       // small high dotless head of khah
            .replace("\u06E2", "")
            .replace("\u06E3", "")
            .replace("\u06E4", "")
            .replace("\u06E5", "")
            .replace("\u06E6", "")
            .replace("\u06E7", "")
            .replace("\u06E8", "")
            .replace("\u06EA", "")
            .replace("\u06EB", "")
            .replace("\u06EC", "")
            .replace("\u06ED", "")
        return GUNDUL_STRIP_REGEX.replace(normalizedWasla, "")
    }

    /**
     * Membersihkan HTML/entity dari sumber tafsir (khususnya tafsir.app/get.php) menjadi paragraf bersih,
     * serta memisahkan catatan kaki manuskrip `[[...]]` agar tampil halus di bawah paragraf.
     */
    fun parseTafsirRawText(raw: String): List<TafsirParagraph> {
        if (raw.isBlank()) return emptyList()

        val cleaned = cleanHtmlToPlainText(raw)
        if (cleaned.isBlank()) return emptyList()

        val rawParagraphs = cleaned
            .split(Regex("\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var footnoteCounter = 0
        val result = ArrayList<TafsirParagraph>(rawParagraphs.size)

        for (para in rawParagraphs) {
            val footnotes = mutableListOf<TafsirFootnote>()
            val bodyWithMarkers = FOOTNOTE_REGEX.replace(para) { match ->
                val noteText = match.groupValues[1]
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (noteText.isBlank()) {
                    ""
                } else {
                    footnoteCounter += 1
                    footnotes += TafsirFootnote(index = footnoteCounter, text = noteText)
                    " (${toArabicDigits(footnoteCounter)}) "
                }
            }
                .replace(MULTI_SPACE_REGEX, " ")
                .replace(" ،", "،")
                .replace(" .", ".")
                .replace(" :", ":")
                .replace(" ؛", "؛")
                .trim()

            if (bodyWithMarkers.isNotBlank() || footnotes.isNotEmpty()) {
                result += TafsirParagraph(
                    body = bodyWithMarkers,
                    footnotes = footnotes
                )
            }
        }

        return result
    }

    fun cleanHtmlToPlainText(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")

        // Hapus script/style jika ada
        text = text.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), "")
        // Ubah tag blok/baris menjadi newline
        text = BLOCK_BREAK_TAG_REGEX.replace(text, "\n")
        // Hapus sisa tag HTML
        text = ANY_HTML_TAG_REGEX.replace(text, "")
        // Decode HTML entities umum + numerik
        text = decodeHtmlEntities(text)
        // Rapikan spasi per baris dan baris kosong beruntun
        val normalizedLines = text
            .split('\n')
            .map { MULTI_SPACE_REGEX.replace(it, " ").trim() }
            .filter { it.isNotBlank() }
        return MULTI_NEWLINE_REGEX.replace(normalizedLines.joinToString("\n"), "\n").trim()
    }

    fun decodeHtmlEntities(input: String): String {
        var out = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace("&hellip;", "…")

        out = NUMERIC_HEX_ENTITY_REGEX.replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null && Character.isValidCodePoint(code)) {
                String(Character.toChars(code))
            } else m.value
        }
        out = NUMERIC_DEC_ENTITY_REGEX.replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null && Character.isValidCodePoint(code)) {
                String(Character.toChars(code))
            } else m.value
        }
        return out
    }

    /**
     * Mengambil potongan kalimat konteks yang ringkas di sekitar kata yang diketuk
     * untuk dikirim ke Kamus AI agar cepat dan tetap presisi.
     */
    fun extractContextSnippet(fullParagraph: String, tappedWord: String, maxChars: Int = 260): String {
        val clean = fullParagraph.trim()
        if (clean.length <= maxChars) return clean
        val idx = if (tappedWord.isNotBlank()) clean.indexOf(tappedWord) else -1
        if (idx < 0) return clean.take(maxChars).trim()
        val half = maxChars / 2
        val start = (idx - half).coerceAtLeast(0)
        val end = (start + maxChars).coerceAtMost(clean.length)
        var slice = clean.substring(start, end)
        if (start > 0) {
            val firstSpace = slice.indexOf(' ')
            if (firstSpace in 1..30) slice = slice.substring(firstSpace + 1)
        }
        if (end < clean.length) {
            val lastSpace = slice.lastIndexOf(' ')
            if (lastSpace > slice.length - 30 && lastSpace > 0) slice = slice.substring(0, lastSpace)
        }
        return slice.trim()
    }
}
