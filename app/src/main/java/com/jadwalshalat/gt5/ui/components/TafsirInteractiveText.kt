package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.ArabicWordToken
import com.jadwalshalat.gt5.tafsir.TafsirFootnote
import com.jadwalshalat.gt5.tafsir.TafsirPaperTheme
import com.jadwalshalat.gt5.tafsir.TafsirTextProcessor
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import com.jadwalshalat.gt5.ui.theme.ShalatVibePalette

@Immutable
data class TafsirPaperColors(
    val isDark: Boolean,
    val background: Color,
    val topBarBg: Color,
    val cardSurface: Color,
    val ayahSurface: Color,
    val footnoteSurface: Color,
    val pillBg: Color,
    val border: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val savedWordInk: Color,
    val savedWordBg: Color,
    val phraseHighlightBg: Color,
    val phraseHighlightInk: Color,
    val ornamentGold: Color
)

@Composable
fun rememberTafsirPaperColors(theme: TafsirPaperTheme): TafsirPaperColors {
    val palette: ShalatVibePalette = LocalShalatVibePalette.current
    return remember(theme, palette) {
        when (theme) {
            TafsirPaperTheme.SEPIA -> TafsirPaperColors(
                isDark = false,
                background = Color(0xFFF7F1E3),
                topBarBg = Color(0xFFF2E9D6),
                cardSurface = Color(0xFFFAF5EA),
                ayahSurface = Color(0xFFEEE4CE),
                footnoteSurface = Color(0xFFEFE6D4),
                pillBg = Color(0xFFE7DCC5),
                border = Color(0xFFDCCFB5),
                inkPrimary = Color(0xFF2B2118),
                inkSecondary = Color(0xFF5D4B3B),
                inkMuted = Color(0xFF8A7560),
                accent = Color(0xFF0B6E62),
                accentSoft = Color(0x200B6E62),
                savedWordInk = Color(0xFF8C5308),
                savedWordBg = Color(0x1EB97316),
                phraseHighlightBg = Color(0xFF0B6E62),
                phraseHighlightInk = Color.White,
                ornamentGold = Color(0xFF9F6310)
            )

            TafsirPaperTheme.DARK -> TafsirPaperColors(
                isDark = true,
                background = Color(0xFF0B1012),
                topBarBg = Color(0xFF10181B),
                cardSurface = Color(0xFF131D20),
                ayahSurface = Color(0xFF122324),
                footnoteSurface = Color(0xFF10181B),
                pillBg = Color(0xFF1A262A),
                border = Color(0xFF223136),
                inkPrimary = Color(0xFFECEFE9),
                inkSecondary = Color(0xFFA6B5B0),
                inkMuted = Color(0xFF6F807B),
                accent = Color(0xFF34D399),
                accentSoft = Color(0x2610B981),
                savedWordInk = Color(0xFFF5B041),
                savedWordBg = Color(0x1FF5B041),
                phraseHighlightBg = Color(0xFF10B981),
                phraseHighlightInk = Color(0xFF042620),
                ornamentGold = Color(0xFFF5B041)
            )

            TafsirPaperTheme.FOLLOW_THEME -> TafsirPaperColors(
                isDark = palette.isDark,
                background = palette.background,
                topBarBg = palette.navBarBg,
                cardSurface = palette.cardSurface,
                ayahSurface = if (palette.isDark) Color(0xFF102221) else Color(0xFFEAF3EF),
                footnoteSurface = if (palette.isDark) Color(0xFF10191C) else Color(0xFFEDF1EE),
                pillBg = palette.pillBg,
                border = palette.cardBorder,
                inkPrimary = palette.textPrimary,
                inkSecondary = palette.textSecondary,
                inkMuted = palette.textMuted,
                accent = palette.emeraldPrimary,
                accentSoft = palette.emeraldSoft,
                savedWordInk = palette.goldAccent,
                savedWordBg = palette.goldSoft,
                phraseHighlightBg = palette.emeraldPrimary,
                phraseHighlightInk = Color.White,
                ornamentGold = palette.goldAccent
            )
        }
    }
}

private data class PositionedArabicToken(
    val token: ArabicWordToken,
    val startOffset: Int,
    val endOffset: Int
)

private data class BuiltInteractiveText(
    val annotated: AnnotatedString,
    val tokens: List<ArabicWordToken>,
    val positioned: List<PositionedArabicToken>
)

/**
 * Paragraf Arab RTL performa tinggi berbasis AnnotatedString + TextLayoutResult.
 * Menjaga ligatur/kerning asli kitab cetak, mendukung ketuk kata & tekan lama (+ Frase),
 * garis bawah halus untuk kata yang sudah tersimpan di Kamus Saya, dan ornamen nomor ayat.
 */
@Composable
fun TafsirInteractiveArabicText(
    rawArabic: String,
    blockKey: String,
    showHarakat: Boolean,
    fontSizeSp: Float,
    lineHeightMultiplier: Float,
    paper: TafsirPaperColors,
    savedNormalizedKeys: Set<String>,
    phraseMode: Boolean,
    activePhraseBlockKey: String?,
    phraseStartWordIndex: Int,
    onTapWord: (token: ArabicWordToken, allTokens: List<ArabicWordToken>) -> Unit,
    onLongPressWord: (token: ArabicWordToken, allTokens: List<ArabicWordToken>) -> Unit,
    modifier: Modifier = Modifier,
    trailingOrnament: String? = null,
    textAlign: TextAlign = TextAlign.Start,
    fontWeight: FontWeight = FontWeight.Normal,
    inkColorOverride: Color? = null
) {
    val tokens = remember(rawArabic) {
        ArabicTextUtils.tokenizeSentenceWords(rawArabic)
    }

    val isActivePhraseBlock = phraseMode && activePhraseBlockKey == blockKey
    val effectiveInk = inkColorOverride ?: paper.inkPrimary

    val built = remember(
        tokens,
        showHarakat,
        savedNormalizedKeys,
        isActivePhraseBlock,
        phraseStartWordIndex,
        paper,
        trailingOrnament,
        effectiveInk
    ) {
        val positioned = ArrayList<PositionedArabicToken>(tokens.size)
        val annotated = buildAnnotatedString {
            tokens.forEachIndexed { idx, tok ->
                if (idx > 0) append(' ')
                val displayWord = TafsirTextProcessor.formatArabicForDisplay(tok.raw, showHarakat)
                val start = length
                append(displayWord)
                val end = length
                positioned += PositionedArabicToken(tok, start, end)

                val isSaved = tok.hasArabicLetters &&
                    tok.normalizedKey.length >= 2 &&
                    savedNormalizedKeys.contains(tok.normalizedKey)
                val isPhraseStart = isActivePhraseBlock && phraseStartWordIndex == tok.index

                when {
                    isPhraseStart -> {
                        addStyle(
                            SpanStyle(
                                color = paper.phraseHighlightInk,
                                background = paper.phraseHighlightBg,
                                fontWeight = FontWeight.Bold
                            ),
                            start,
                            end
                        )
                    }

                    isSaved -> {
                        addStyle(
                            SpanStyle(
                                color = paper.savedWordInk,
                                textDecoration = TextDecoration.Underline
                            ),
                            start,
                            end
                        )
                    }

                    isActivePhraseBlock && tok.hasArabicLetters -> {
                        addStyle(
                            SpanStyle(
                                background = paper.accentSoft.copy(alpha = 0.35f)
                            ),
                            start,
                            end
                        )
                    }
                }
            }

            if (!trailingOrnament.isNullOrBlank()) {
                append("  ")
                val ornStart = length
                append(trailingOrnament)
                val ornEnd = length
                addStyle(
                    SpanStyle(
                        color = paper.ornamentGold,
                        fontWeight = FontWeight.SemiBold
                    ),
                    ornStart,
                    ornEnd
                )
            }
        }
        BuiltInteractiveText(annotated, tokens, positioned)
    }

    var textLayoutResult by remember(blockKey, built.annotated) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    fun resolveTokenAt(offsetX: Float, offsetY: Float): ArabicWordToken? {
        val layout = textLayoutResult ?: return null
        val charIdx = layout.getOffsetForPosition(androidx.compose.ui.geometry.Offset(offsetX, offsetY))
        val exact = built.positioned.firstOrNull { charIdx in it.startOffset..it.endOffset }
            ?: built.positioned.minByOrNull { item ->
                when {
                    charIdx < item.startOffset -> item.startOffset - charIdx
                    charIdx > item.endOffset -> charIdx - item.endOffset
                    else -> 0
                }
            }?.takeIf { item ->
                val dist = when {
                    charIdx < item.startOffset -> item.startOffset - charIdx
                    charIdx > item.endOffset -> charIdx - item.endOffset
                    else -> 0
                }
                dist <= 2
            }
        val tok = exact?.token ?: return null
        return tok.takeIf { it.hasArabicLetters && it.cleanArabic.isNotBlank() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = built.annotated,
            style = TextStyle(
                color = effectiveInk,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineHeightMultiplier).sp,
                fontWeight = fontWeight,
                textAlign = textAlign,
                textDirection = TextDirection.Rtl
            ),
            onTextLayout = { textLayoutResult = it },
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(blockKey, built.positioned, phraseMode, activePhraseBlockKey, phraseStartWordIndex) {
                    detectTapGestures(
                        onTap = { pos ->
                            resolveTokenAt(pos.x, pos.y)?.let { tok ->
                                onTapWord(tok, built.tokens)
                            }
                        },
                        onLongPress = { pos ->
                            resolveTokenAt(pos.x, pos.y)?.let { tok ->
                                onLongPressWord(tok, built.tokens)
                            }
                        }
                    )
                }
        )
    }
}

/**
 * Blok catatan kaki manuskrip `[[...]]` yang ditampilkan secara halus di bawah paragraf tafsir.
 */
@Composable
fun TafsirFootnotesBlock(
    footnotes: List<TafsirFootnote>,
    blockKeyPrefix: String,
    showHarakat: Boolean,
    bodyFontSizeSp: Float,
    paper: TafsirPaperColors,
    savedNormalizedKeys: Set<String>,
    phraseMode: Boolean,
    activePhraseBlockKey: String?,
    phraseStartWordIndex: Int,
    onTapWord: (blockKey: String, fullText: String, token: ArabicWordToken, allTokens: List<ArabicWordToken>) -> Unit,
    onLongPressWord: (blockKey: String, fullText: String, token: ArabicWordToken, allTokens: List<ArabicWordToken>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (footnotes.isEmpty()) return

    val footnoteFontSp = (bodyFontSizeSp * 0.80f).coerceIn(13.5f, 18.5f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(paper.footnoteSurface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        footnotes.forEach { fn ->
            val fnKey = "${blockKeyPrefix}_fn_${fn.index}"
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "(${TafsirTextProcessor.toArabicDigits(fn.index)})",
                        color = paper.ornamentGold,
                        fontSize = (footnoteFontSp - 1f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        TafsirInteractiveArabicText(
                            rawArabic = fn.text,
                            blockKey = fnKey,
                            showHarakat = showHarakat,
                            fontSizeSp = footnoteFontSp,
                            lineHeightMultiplier = 1.75f,
                            paper = paper,
                            savedNormalizedKeys = savedNormalizedKeys,
                            phraseMode = phraseMode,
                            activePhraseBlockKey = activePhraseBlockKey,
                            phraseStartWordIndex = phraseStartWordIndex,
                            inkColorOverride = paper.inkSecondary,
                            onTapWord = { tok, all -> onTapWord(fnKey, fn.text, tok, all) },
                            onLongPressWord = { tok, all -> onLongPressWord(fnKey, fn.text, tok, all) }
                        )
                    }
                }
            }
        }
    }
}
