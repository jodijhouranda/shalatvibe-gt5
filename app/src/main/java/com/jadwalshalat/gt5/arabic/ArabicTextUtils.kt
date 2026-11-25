package com.jadwalshalat.gt5.arabic

import androidx.compose.runtime.Immutable

@Immutable
data class ArabicWordToken(
    val raw: String,
    val cleanArabic: String,
    val normalizedKey: String,
    val hasArabicLetters: Boolean,
    val index: Int
)

@Immutable
data class ClozeQuestion(
    val targetEntry: KamusEntry,
    val maskedSentence: String,
    val correctAnswer: String,
    val options: List<String>,
    val indonesianHint: String
)

object ArabicTextUtils {
    private val HARAKAT_REGEX = Regex("[\u064B-\u065F\u0670\u0640\u200E\u200F]")
    private val OUTER_PUNCT_REGEX = Regex("^[\\s،؟.,;:()«»\"'\\[\\]/…—–\\-]+|[\\s،؟.,;:()«»\"'\\[\\]/…—–\\-]+$")
    private val ARABIC_LETTER_REGEX = Regex("[\u0621-\u064A]")

    fun stripHarakat(input: String): String {
        if (input.isEmpty()) return ""
        return HARAKAT_REGEX.replace(input, "")
    }

    fun stripOuterPunctuation(token: String): String {
        return OUTER_PUNCT_REGEX.replace(token, "").trim()
    }

    fun hasArabicLetters(text: String): Boolean {
        return ARABIC_LETTER_REGEX.containsMatchIn(text)
    }

    /**
     * Kunci konsonan normal untuk pengelompokan/pencarian kamus (menghapus harakat, tatwil, tanda baca,
     * dan menyeragamkan alif hamzah -> ا, alif maqshurah -> ي, ta marbuthah -> ه).
     */
    fun normalizeForLookup(input: String): String {
        val bare = stripHarakat(stripOuterPunctuation(input))
            .replace(Regex("[،؟.,;:()«»\"'\\[\\]/…—–\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (bare.isEmpty()) return ""
        val sb = StringBuilder(bare.length)
        for (ch in bare) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                'ة' -> sb.append('ه')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun stripCommonPrefixesForMatch(normalized: String): String {
        var cur = normalized.trim()
        // Lepas awalan kata sambung tunggal (و / ف / ب / ل / ك) lalu sandang (ال) secara hati-hati
        if (cur.length >= 4 && (cur.startsWith("و") || cur.startsWith("ف") || cur.startsWith("ب") || cur.startsWith("ل") || cur.startsWith("ك"))) {
            val withoutSingle = cur.substring(1)
            if (withoutSingle.startsWith("ال") || cur.length >= 5) {
                cur = withoutSingle
            }
        }
        if (cur.length >= 4 && cur.startsWith("ال")) {
            cur = cur.substring(2)
        }
        return cur
    }

    fun tokenizeSentenceWords(sentence: String): List<ArabicWordToken> {
        val rawTokens = sentence.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return rawTokens.mapIndexed { idx, raw ->
            val cleaned = stripOuterPunctuation(raw)
            ArabicWordToken(
                raw = raw,
                cleanArabic = cleaned,
                normalizedKey = normalizeForLookup(cleaned),
                hasArabicLetters = hasArabicLetters(cleaned),
                index = idx
            )
        }
    }

    fun buildPhraseFromRange(tokens: List<ArabicWordToken>, startIndex: Int, endIndex: Int): String {
        if (tokens.isEmpty()) return ""
        val lo = minOf(startIndex, endIndex).coerceIn(0, tokens.lastIndex)
        val hi = maxOf(startIndex, endIndex).coerceIn(0, tokens.lastIndex)
        return tokens.subList(lo, hi + 1)
            .joinToString(" ") { it.cleanArabic.ifBlank { it.raw } }
            .trim()
    }

    fun maskTargetInSentence(sentence: String, targetArabic: String): Pair<String, String>? {
        val sentenceTrim = sentence.trim()
        val targetClean = stripOuterPunctuation(targetArabic)
        if (sentenceTrim.isBlank() || targetClean.isBlank()) return null

        // 1. Exact harakat match first
        val exactIdx = sentenceTrim.indexOf(targetClean)
        if (exactIdx >= 0) {
            val found = sentenceTrim.substring(exactIdx, exactIdx + targetClean.length)
            val masked = sentenceTrim.replaceRange(exactIdx, exactIdx + targetClean.length, "________")
            return masked to found
        }

        // 2. Consonant-skeleton match (handles harakat/alif/prefix differences cleanly across word tokens)
        val sentenceTokens = tokenizeSentenceWords(sentenceTrim)
        val targetWordCount = targetClean.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
        val targetNorm = normalizeForLookup(targetClean)
        val targetStem = stripCommonPrefixesForMatch(targetNorm)
        if (targetNorm.length < 2) return null

        for (i in 0..(sentenceTokens.size - targetWordCount).coerceAtLeast(0)) {
            if (i + targetWordCount > sentenceTokens.size) break
            val windowSlice = sentenceTokens.subList(i, i + targetWordCount)
            val joinedRaw = windowSlice.joinToString(" ") { it.raw }
            val joinedClean = windowSlice.joinToString(" ") { it.cleanArabic }
            val wNorm = normalizeForLookup(joinedClean)
            val wStem = stripCommonPrefixesForMatch(wNorm)
            val matched = wNorm == targetNorm ||
                wStem == targetStem ||
                (targetWordCount == 1 && targetNorm.length >= 3 && (wNorm.contains(targetNorm) || targetNorm.contains(wNorm)))
            if (matched && joinedClean.isNotBlank()) {
                val maskedTokens = sentenceTokens.toMutableList()
                val firstTok = windowSlice.first()
                val lastTok = windowSlice.last()
                val lead = firstTok.raw.substringBefore(firstTok.cleanArabic, "")
                val trail = lastTok.raw.substringAfterLast(lastTok.cleanArabic, "")
                val newWords = mutableListOf<String>()
                for (k in sentenceTokens.indices) {
                    when {
                        k < i -> newWords.add(sentenceTokens[k].raw)
                        k == i -> newWords.add("${lead}________$trail")
                        k >= i + targetWordCount -> newWords.add(sentenceTokens[k].raw)
                    }
                }
                return newWords.joinToString(" ") to joinedClean
            }
        }
        return null
    }

    fun buildClozeQuestion(
        targetEntry: KamusEntry,
        allEntries: List<KamusEntry>,
        extraPoolArabic: List<String> = emptyList(),
        showHarakat: Boolean = true,
        seed: Int = 0
    ): ClozeQuestion? {
        val ctx = targetEntry.contextSentence.ifBlank { return null }
        val (rawMasked, matchedDisplay) = maskTargetInSentence(ctx, targetEntry.arabic) ?: return null
        val displaySentence = if (showHarakat) rawMasked else stripHarakat(rawMasked)
        val displayAnswer = if (showHarakat) matchedDisplay else stripHarakat(matchedDisplay)
        val answerKey = normalizeForLookup(displayAnswer)

        val candidates = mutableListOf<String>()
        fun tryAddCandidate(raw: String) {
            val clean = stripOuterPunctuation(raw)
            if (clean.isBlank() || !hasArabicLetters(clean)) return
            val formatted = if (showHarakat) clean else stripHarakat(clean)
            val key = normalizeForLookup(formatted)
            if (key.isNotBlank() && key != answerKey && candidates.none { normalizeForLookup(it) == key }) {
                candidates.add(formatted)
            }
        }

        // 1. Prioritize entries of same kind from user's own Kamus
        allEntries.filter { it.id != targetEntry.id && it.kind == targetEntry.kind }.forEach { tryAddCandidate(it.arabic) }
        // 2. Other user entries
        allEntries.filter { it.id != targetEntry.id }.forEach { tryAddCandidate(it.arabic) }
        // 3. Extra pool from active module / fallback standard Arabic academic words
        extraPoolArabic.forEach { tryAddCandidate(it) }
        DEFAULT_CLOZE_DISTRACTORS.forEach { tryAddCandidate(it) }

        val distractors = candidates.take(3)
        if (distractors.size < 2) return null

        val combined = (listOf(displayAnswer) + distractors).toMutableList()
        // Deterministic rotation/shuffle so choices don't flicker on Compose recomposition
        val shift = ((seed + targetEntry.id.hashCode()).let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % combined.size
        val ordered = combined.indices.map { i -> combined[(i + shift) % combined.size] }

        return ClozeQuestion(
            targetEntry = targetEntry,
            maskedSentence = displaySentence,
            correctAnswer = displayAnswer,
            options = ordered,
            indonesianHint = targetEntry.meaning.ifBlank { targetEntry.turatsOrigin }
        )
    }

    private val DEFAULT_CLOZE_DISTRACTORS = listOf(
        "الْحَضَارَةُ",
        "السَّرْدِيَّةُ",
        "اَلْمَنْهَجُ الْعِلْمِيُّ",
        "تَدَهْوُرٌ",
        "اِزْدِهَارٌ",
        "اَلْهَيْمَنَةُ",
        "مُسْتَهْجَنٌ",
        "تَبَادُلٌ ثَقَافِيٌّ"
    )
}
