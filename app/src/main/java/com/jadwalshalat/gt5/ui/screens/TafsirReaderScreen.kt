package com.jadwalshalat.gt5.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.ArabicWordToken
import com.jadwalshalat.gt5.arabic.KamusAiUiState
import com.jadwalshalat.gt5.arabic.KamusCodec
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind
import com.jadwalshalat.gt5.tafsir.TafsirBook
import com.jadwalshalat.gt5.tafsir.TafsirBookmark
import com.jadwalshalat.gt5.tafsir.TafsirFontSize
import com.jadwalshalat.gt5.tafsir.TafsirLoadState
import com.jadwalshalat.gt5.tafsir.TafsirPaperTheme
import com.jadwalshalat.gt5.tafsir.TafsirTextProcessor
import com.jadwalshalat.gt5.tafsir.TafsirUiState
import com.jadwalshalat.gt5.ui.components.TafsirAyahPickerSheet
import com.jadwalshalat.gt5.ui.components.TafsirBookPickerSheet
import com.jadwalshalat.gt5.ui.components.TafsirDisplayAndBookmarksSheet
import com.jadwalshalat.gt5.ui.components.TafsirFootnotesBlock
import com.jadwalshalat.gt5.ui.components.TafsirInteractiveArabicText
import com.jadwalshalat.gt5.ui.components.TafsirPaperColors
import com.jadwalshalat.gt5.ui.components.TafsirSurahPickerSheet
import com.jadwalshalat.gt5.ui.components.rememberTafsirPaperColors

private data class TafsirPhraseDraftSelection(
    val tokens: List<ArabicWordToken>,
    val startIndex: Int,
    val endIndex: Int
)

@Composable
fun TafsirReaderScreen(
    state: TafsirUiState,
    savedNormalizedKeys: Set<String>,
    kamusEntries: List<KamusEntry>,
    kamusCount: Int,
    kamusAiState: KamusAiUiState,
    onSelectBook: (TafsirBook) -> Unit,
    onSelectSurah: (Int) -> Unit,
    onSelectSurahAndAyah: (surah: Int, ayah: Int) -> Unit,
    onSelectAyah: (Int) -> Unit,
    onOpenPreviousAyah: () -> Unit,
    onOpenNextAyah: () -> Unit,
    onRetry: () -> Unit,
    onSetShowHarakat: (Boolean) -> Unit,
    onSetFontSize: (TafsirFontSize) -> Unit,
    onSetPaperTheme: (TafsirPaperTheme) -> Unit,
    onToggleAyahCollapsed: () -> Unit,
    onToggleBookmark: () -> Boolean,
    onOpenBookmark: (TafsirBookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSearchAyah: (String) -> Unit,
    onClearAyahSearch: () -> Unit,
    onLookupKamusAi: (arabic: String, contextSentence: String, lessonTitle: String, forceRefresh: Boolean) -> Unit,
    onClearKamusAiState: () -> Unit,
    onSaveKamusEntry: (KamusEntry) -> Unit,
    onOpenKamus: () -> Unit,
    onSpeakArabic: (String) -> String?,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val paper = rememberTafsirPaperColors(state.paperTheme)
    val listState = rememberLazyListState()

    var showBookSheet by remember { mutableStateOf(false) }
    var showSurahSheet by remember { mutableStateOf(false) }
    var showAyahSheet by remember { mutableStateOf(false) }
    var showDisplaySheet by remember { mutableStateOf(false) }

    // State untuk Mode + Frase
    var phraseMode by remember { mutableStateOf(false) }
    var phraseStartBlockKey by remember { mutableStateOf<String?>(null) }
    var phraseStartWordIdx by remember { mutableIntStateOf(-1) }
    var phraseStartTokens by remember { mutableStateOf<List<ArabicWordToken>>(emptyList()) }
    var phraseStartFullText by remember { mutableStateOf("") }

    // State modal Kamus AI & Kamus Saya
    var pendingDraft by remember { mutableStateOf<KamusEntry?>(null) }
    var pendingPhraseSelection by remember { mutableStateOf<TafsirPhraseDraftSelection?>(null) }

    fun resetPhraseSelection() {
        phraseMode = false
        phraseStartBlockKey = null
        phraseStartWordIdx = -1
        phraseStartTokens = emptyList()
        phraseStartFullText = ""
    }

    // Gulung ke atas & reset mode frase setiap kali pindah surah/ayat/kitab
    LaunchedEffect(state.activeBook.id, state.surahNumber, state.ayahNumber) {
        resetPhraseSelection()
        listState.scrollToItem(0)
    }

    fun openExternalBrowser(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            onShowMessage("Tidak dapat membuka browser untuk $url")
        }
    }

    fun openKamusCollector(
        arabicPhrase: String,
        fullBlockText: String,
        overrideKind: KamusEntryKind? = null,
        selection: TafsirPhraseDraftSelection? = null
    ) {
        onClearKamusAiState()
        val clean = ArabicTextUtils.stripOuterPunctuation(arabicPhrase).ifBlank { arabicPhrase.trim() }
        if (clean.isBlank()) return
        val normKey = ArabicTextUtils.normalizeForLookup(clean)
        val existingSaved = if (normKey.isNotBlank()) {
            kamusEntries.firstOrNull { it.normalizedKey == normKey }
        } else null

        val lessonId = "tafsir:${state.activeBook.id}:${state.surahNumber}:${state.ayahNumber}"
        val surahMeta = state.activeSurah
        val lessonTitle = "${state.activeBook.nameLatin} • ${surahMeta.nameLatin}:${state.ayahNumber}"
        val focusedWindow = if (selection != null && selection.tokens.isNotEmpty()) {
            TafsirTextProcessor.buildFocusedContextWindow(
                allTokens = selection.tokens,
                startIndex = selection.startIndex,
                endIndex = selection.endIndex
            )
        } else {
            null
        }
        val snippet = focusedWindow?.contextSentence?.ifBlank { null }
            ?: TafsirTextProcessor.extractContextSnippet(fullBlockText, clean)
        val inferredKind = overrideKind ?: existingSaved?.kind ?: if (clean.contains(' ')) {
            KamusEntryKind.PHRASE
        } else {
            KamusEntryKind.WORD
        }

        pendingPhraseSelection = focusedWindow?.let {
            TafsirPhraseDraftSelection(
                tokens = it.tokens,
                startIndex = it.startIndex,
                endIndex = it.endIndex
            )
        }
        pendingDraft = KamusEntry(
            id = existingSaved?.id ?: KamusCodec.createStableId(clean, lessonId),
            arabic = existingSaved?.arabic?.ifBlank { clean } ?: clean,
            transliteration = existingSaved?.transliteration.orEmpty(),
            meaning = existingSaved?.meaning.orEmpty(),
            turatsOrigin = existingSaved?.turatsOrigin.orEmpty(),
            synonymNote = existingSaved?.synonymNote.orEmpty(),
            kind = inferredKind,
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            segmentLabel = "QS ${surahMeta.nameLatin} ${state.surahNumber}:${state.ayahNumber}",
            segmentStartSec = 0,
            contextSentence = snippet,
            personalNote = existingSaved?.personalNote.orEmpty(),
            masteryStage = existingSaved?.masteryStage ?: 0,
            nextReviewEpochDay = existingSaved?.nextReviewEpochDay ?: 0L,
            createdAtMillis = existingSaved?.createdAtMillis ?: 0L,
            reviewCount = existingSaved?.reviewCount ?: 0
        )
    }

    fun handleWordTap(
        blockKey: String,
        fullBlockText: String,
        token: ArabicWordToken,
        allTokens: List<ArabicWordToken>
    ) {
        if (!phraseMode) {
            openKamusCollector(
                arabicPhrase = token.cleanArabic.ifBlank { token.raw },
                fullBlockText = fullBlockText,
                overrideKind = KamusEntryKind.WORD,
                selection = TafsirPhraseDraftSelection(allTokens, token.index, token.index)
            )
            return
        }

        if (phraseStartWordIdx < 0 || phraseStartBlockKey != blockKey) {
            phraseStartBlockKey = blockKey
            phraseStartWordIdx = token.index
            phraseStartTokens = allTokens
            phraseStartFullText = fullBlockText
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            val lo = minOf(phraseStartWordIdx, token.index)
            val hi = maxOf(phraseStartWordIdx, token.index)
            val phrase = ArabicTextUtils.buildPhraseFromRange(allTokens, lo, hi)
            resetPhraseSelection()
            if (phrase.isNotBlank()) {
                openKamusCollector(
                    arabicPhrase = phrase,
                    fullBlockText = fullBlockText,
                    overrideKind = if (hi > lo) KamusEntryKind.PHRASE else KamusEntryKind.WORD,
                    selection = TafsirPhraseDraftSelection(allTokens, lo, hi)
                )
            }
        }
    }

    fun handleWordLongPress(
        blockKey: String,
        fullBlockText: String,
        token: ArabicWordToken,
        allTokens: List<ArabicWordToken>
    ) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        phraseMode = true
        phraseStartBlockKey = blockKey
        phraseStartWordIdx = token.index
        phraseStartTokens = allTokens
        phraseStartFullText = fullBlockText
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(paper.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Bilah Atas Ringkas 1 Baris (Kembali | Surah & Cari Ayat | Ayat ◀ n ▶ | Kitab | +Frase | Bookmark | Tampilan)
        TafsirCompactTopBar(
            state = state,
            paper = paper,
            phraseMode = phraseMode,
            onBack = onBack,
            onOpenSurahPicker = { showSurahSheet = true },
            onOpenAyahPicker = { showAyahSheet = true },
            onPreviousAyah = onOpenPreviousAyah,
            onNextAyah = onOpenNextAyah,
            onOpenBookPicker = { showBookSheet = true },
            onTogglePhraseMode = {
                if (phraseMode) {
                    resetPhraseSelection()
                } else {
                    phraseMode = true
                    phraseStartBlockKey = null
                    phraseStartWordIdx = -1
                }
            },
            onToggleBookmark = {
                val added = onToggleBookmark()
                val ref = "${state.activeSurah.nameLatin} ${state.surahNumber}:${state.ayahNumber}"
                onShowMessage(
                    if (added) "Bookmark disimpan: $ref (${state.activeBook.nameLatin})"
                    else "Bookmark dihapus: $ref"
                )
            },
            onOpenDisplaySettings = { showDisplaySheet = true }
        )

        HorizontalDivider(thickness = 0.6.dp, color = paper.border)

        // 2. Area Baca Utama yang Lega
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 740.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (phraseMode) 52.dp else 10.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kartu Ayat Ringkas yang bisa dilipat (Mode Fokus Tafsir)
                item(key = "ayah_card_${state.surahNumber}_${state.ayahNumber}") {
                    TafsirCollapsibleAyahCard(
                        state = state,
                        paper = paper,
                        savedNormalizedKeys = savedNormalizedKeys,
                        phraseMode = phraseMode,
                        activePhraseBlockKey = phraseStartBlockKey,
                        phraseStartWordIdx = phraseStartWordIdx,
                        onToggleCollapsed = onToggleAyahCollapsed,
                        onSpeakAyah = {
                            if (state.ayahText.isNotBlank()) {
                                onSpeakArabic(state.ayahText)?.let(onShowMessage)
                            }
                        },
                        onTapWord = { tok, all ->
                            handleWordTap("ayah", state.ayahText, tok, all)
                        },
                        onLongPressWord = { tok, all ->
                            handleWordLongPress("ayah", state.ayahText, tok, all)
                        },
                        onRetryAyah = onRetry
                    )
                }

                // Header Ringkas Kitab Aktif (1 baris elegan, tanpa menumpuk banyak kartu)
                item(key = "book_strip_${state.activeBook.id}") {
                    ActiveBookMetaStrip(
                        state = state,
                        paper = paper,
                        onOpenBookPicker = { showBookSheet = true }
                    )
                }

                // Isi Kitab Tafsir Aktif
                when (val tafsir = state.tafsirState) {
                    is TafsirLoadState.Loading -> {
                        item(key = "tafsir_loading") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = paper.accent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Memuat ${state.activeBook.nameLatin}...",
                                    color = paper.inkSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    is TafsirLoadState.ScannedOnly -> {
                        item(key = "tafsir_scanned_only") {
                            TafsirSpecialNoticeCard(
                                title = "Format Scan Halaman di tafsir.app",
                                arabicTitle = tafsir.book.nameArabic,
                                description = "Kitab ${tafsir.book.nameLatin} di tafsir.app tersedia dalam format gambar pindaian (scan) halaman kitab tanpa teks digital, sehingga belum bisa ditampilkan langsung sebagai teks interaktif di sini.",
                                paper = paper,
                                primaryActionLabel = "Buka di tafsir.app",
                                onPrimaryAction = { openExternalBrowser(tafsir.externalUrl) },
                                secondaryActionLabel = "Pilih Kitab Lain",
                                onSecondaryAction = { showBookSheet = true }
                            )
                        }
                    }

                    is TafsirLoadState.EmptyForAyah -> {
                        item(key = "tafsir_empty_ayah") {
                            val extraNote = tafsir.book.specialNoteId.takeIf { it.isNotBlank() }
                                ?: "Kitab ini tidak mencantumkan uraian tersendiri untuk QS ${state.activeSurah.nameLatin} ayat ${tafsir.ayah}, atau pembahasannya tergabung pada ayat sebelumnya."
                            TafsirSpecialNoticeCard(
                                title = "Tidak Ada Entri Khusus untuk Ayat Ini",
                                arabicTitle = tafsir.book.nameArabic,
                                description = extraNote,
                                paper = paper,
                                primaryActionLabel = "Pilih Kitab Lain",
                                onPrimaryAction = { showBookSheet = true },
                                secondaryActionLabel = "Cek di tafsir.app",
                                onSecondaryAction = { openExternalBrowser(tafsir.externalUrl) }
                            )
                        }
                    }

                    is TafsirLoadState.Error -> {
                        item(key = "tafsir_error") {
                            TafsirSpecialNoticeCard(
                                title = "Gagal Memuat Teks Tafsir",
                                arabicTitle = state.activeBook.nameArabic,
                                description = tafsir.message,
                                paper = paper,
                                primaryActionLabel = "Coba Lagi",
                                onPrimaryAction = onRetry,
                                secondaryActionLabel = "Buka di tafsir.app",
                                onSecondaryAction = { openExternalBrowser(tafsir.externalUrl) }
                            )
                        }
                    }

                    is TafsirLoadState.Ready -> {
                        itemsIndexed(
                            items = tafsir.content.paragraphs,
                            key = { idx, _ -> "para_${state.activeBook.id}_${state.surahNumber}_${state.ayahNumber}_$idx" }
                        ) { idx, paragraph ->
                            val blockKey = "para_$idx"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                if (paragraph.body.isNotBlank()) {
                                    TafsirInteractiveArabicText(
                                        rawArabic = paragraph.body,
                                        blockKey = blockKey,
                                        showHarakat = state.showHarakat,
                                        fontSizeSp = state.fontSize.bodySp,
                                        lineHeightMultiplier = state.fontSize.lineHeightMultiplier,
                                        paper = paper,
                                        savedNormalizedKeys = savedNormalizedKeys,
                                        phraseMode = phraseMode,
                                        activePhraseBlockKey = phraseStartBlockKey,
                                        phraseStartWordIndex = phraseStartWordIdx,
                                        onTapWord = { tok, all ->
                                            handleWordTap(blockKey, paragraph.body, tok, all)
                                        },
                                        onLongPressWord = { tok, all ->
                                            handleWordLongPress(blockKey, paragraph.body, tok, all)
                                        }
                                    )
                                }
                                if (paragraph.footnotes.isNotEmpty()) {
                                    TafsirFootnotesBlock(
                                        footnotes = paragraph.footnotes,
                                        blockKeyPrefix = blockKey,
                                        showHarakat = state.showHarakat,
                                        bodyFontSizeSp = state.fontSize.bodySp,
                                        paper = paper,
                                        savedNormalizedKeys = savedNormalizedKeys,
                                        phraseMode = phraseMode,
                                        activePhraseBlockKey = phraseStartBlockKey,
                                        phraseStartWordIndex = phraseStartWordIdx,
                                        onTapWord = { fnKey, fnText, tok, all ->
                                            handleWordTap(fnKey, fnText, tok, all)
                                        },
                                        onLongPressWord = { fnKey, fnText, tok, all ->
                                            handleWordLongPress(fnKey, fnText, tok, all)
                                        }
                                    )
                                }
                            }
                        }

                        // Petunjuk halus & navigasi bawah setelah selesai membaca tafsir
                        item(key = "tafsir_footer_nav") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(thickness = 0.6.dp, color = paper.border)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = onOpenPreviousAyah,
                                        enabled = state.hasPreviousAyah,
                                        shape = RoundedCornerShape(12.dp),
                                        color = paper.pillBg,
                                        modifier = Modifier.testTag("tafsir_footer_prev_ayah")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                                contentDescription = null,
                                                tint = if (state.hasPreviousAyah) paper.inkPrimary else paper.inkMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Sebelumnya",
                                                color = if (state.hasPreviousAyah) paper.inkPrimary else paper.inkMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onOpenKamus,
                                        shape = CircleShape,
                                        color = paper.savedWordBg,
                                        modifier = Modifier.testTag("tafsir_open_kamus_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Book,
                                                contentDescription = "Buka Kamus Saya",
                                                tint = paper.ornamentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Kamus ($kamusCount)",
                                                color = paper.ornamentGold,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onOpenNextAyah,
                                        enabled = state.hasNextAyah,
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (state.hasNextAyah) paper.accent else paper.pillBg,
                                        modifier = Modifier.testTag("tafsir_footer_next_ayah")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Berikutnya",
                                                color = if (state.hasNextAyah) Color.White else paper.inkMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = if (state.hasNextAyah) Color.White else paper.inkMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Ketuk kata Arab mana saja untuk bedah Kamus AI & simpan ke Kamus Saya • Tekan lama kata untuk pilih rentang frase",
                                    color = paper.inkMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Banner Melayang Mode + Frase
            if (phraseMode) {
                Surface(
                    shape = CircleShape,
                    color = paper.ornamentGold,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (phraseStartWordIdx < 0) {
                                "Ketuk KATA AWAL frase"
                            } else {
                                "Ketuk KATA AKHIR frase"
                            },
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (phraseStartWordIdx >= 0 && phraseStartTokens.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    val tokens = phraseStartTokens
                                    val startIdx = phraseStartWordIdx
                                    val fullText = phraseStartFullText
                                    val single = ArabicTextUtils.buildPhraseFromRange(tokens, startIdx, startIdx)
                                    resetPhraseSelection()
                                    if (single.isNotBlank()) {
                                        openKamusCollector(
                                            arabicPhrase = single,
                                            fullBlockText = fullText,
                                            overrideKind = KamusEntryKind.WORD,
                                            selection = TafsirPhraseDraftSelection(tokens, startIdx, startIdx)
                                        )
                                    }
                                },
                                shape = CircleShape,
                                color = paper.accent
                            ) {
                                Text(
                                    text = "1 Kata",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { resetPhraseSelection() },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Batal",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (showBookSheet) {
        TafsirBookPickerSheet(
            activeBook = state.activeBook,
            onSelectBook = onSelectBook,
            onDismiss = { showBookSheet = false }
        )
    }

    if (showSurahSheet) {
        TafsirSurahPickerSheet(
            activeSurah = state.surahNumber,
            showHarakat = state.showHarakat,
            ayahSearchResults = state.ayahSearchResults,
            isSearchingAyah = state.isSearchingAyah,
            ayahSearchError = state.ayahSearchError,
            onSearchAyah = onSearchAyah,
            onClearAyahSearch = onClearAyahSearch,
            onSelectSurah = onSelectSurah,
            onSelectSurahAndAyah = onSelectSurahAndAyah,
            onDismiss = { showSurahSheet = false }
        )
    }

    if (showAyahSheet) {
        TafsirAyahPickerSheet(
            surah = state.activeSurah,
            activeAyah = state.ayahNumber,
            onSelectAyah = onSelectAyah,
            onDismiss = { showAyahSheet = false }
        )
    }

    if (showDisplaySheet) {
        TafsirDisplayAndBookmarksSheet(
            showHarakat = state.showHarakat,
            fontSize = state.fontSize,
            paperTheme = state.paperTheme,
            isAyahCollapsed = state.isAyahCollapsed,
            bookmarks = state.bookmarks,
            onSetShowHarakat = onSetShowHarakat,
            onSetFontSize = onSetFontSize,
            onSetPaperTheme = onSetPaperTheme,
            onToggleAyahCollapsed = onToggleAyahCollapsed,
            onOpenBookmark = onOpenBookmark,
            onRemoveBookmark = onRemoveBookmark,
            onDismiss = { showDisplaySheet = false }
        )
    }

    // Modal Kamus AI & Simpan ke Kamus Saya (memakai komponen KamusEntryEditorSheet yang sudah ada)
    val draft = pendingDraft
    if (draft != null) {
        val sel = pendingPhraseSelection
        KamusEntryEditorSheet(
            initialEntry = draft,
            aiState = kamusAiState,
            onRequestAi = onLookupKamusAi,
            onClearAiState = onClearKamusAiState,
            segmentTokens = sel?.tokens.orEmpty(),
            initialRangeStart = sel?.startIndex ?: -1,
            initialRangeEnd = sel?.endIndex ?: -1,
            onDismiss = {
                onClearKamusAiState()
                pendingDraft = null
                pendingPhraseSelection = null
            },
            onSave = { entry ->
                onSaveKamusEntry(entry)
                onClearKamusAiState()
                pendingDraft = null
                pendingPhraseSelection = null
                onShowMessage("Disimpan ke Kamus Saya: ${entry.arabic}")
            },
            onSpeakArabic = { txt -> onSpeakArabic(txt)?.let(onShowMessage) }
        )
    }
}

@Composable
private fun TafsirCompactTopBar(
    state: TafsirUiState,
    paper: TafsirPaperColors,
    phraseMode: Boolean,
    onBack: () -> Unit,
    onOpenSurahPicker: () -> Unit,
    onOpenAyahPicker: () -> Unit,
    onPreviousAyah: () -> Unit,
    onNextAyah: () -> Unit,
    onOpenBookPicker: () -> Unit,
    onTogglePhraseMode: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenDisplaySettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(paper.topBarBg)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // 0. Tombol Kembali ke Pustaka
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(32.dp)
                .testTag("tafsir_back_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali ke Pustaka",
                    tint = paper.inkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // 1. Pemilih Surah & Pencarian Potongan Ayat
        Surface(
            onClick = onOpenSurahPicker,
            shape = RoundedCornerShape(10.dp),
            color = paper.pillBg,
            modifier = Modifier
                .weight(0.96f)
                .height(36.dp)
                .testTag("tafsir_surah_picker_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Cari surah atau potongan ayat",
                    tint = paper.inkMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${state.surahNumber}. ${state.activeSurah.nameLatin}",
                    color = paper.inkPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Pemilih Ayat + Panah Sebelumnya / Berikutnya
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = paper.pillBg,
            modifier = Modifier.height(36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 36.dp)
                        .clickable(enabled = state.hasPreviousAyah, onClick = onPreviousAyah)
                        .testTag("tafsir_prev_ayah_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Ayat sebelumnya",
                        tint = if (state.hasPreviousAyah) paper.inkPrimary else paper.inkMuted.copy(alpha = 0.45f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clickable(onClick = onOpenAyahPicker)
                        .padding(horizontal = 3.dp)
                        .testTag("tafsir_ayah_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = TafsirTextProcessor.toArabicDigits(state.ayahNumber),
                        color = paper.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 36.dp)
                        .clickable(enabled = state.hasNextAyah, onClick = onNextAyah)
                        .testTag("tafsir_next_ayah_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Ayat berikutnya",
                        tint = if (state.hasNextAyah) paper.inkPrimary else paper.inkMuted.copy(alpha = 0.45f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // 3. Pemilih Kitab Aktif
        Surface(
            onClick = onOpenBookPicker,
            shape = RoundedCornerShape(10.dp),
            color = paper.accentSoft,
            modifier = Modifier
                .weight(1.04f)
                .height(36.dp)
                .testTag("tafsir_book_picker_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.activeBook.nameLatin,
                    color = paper.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Ganti Kitab Tafsir",
                    tint = paper.accent,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // 4. Tombol + Frase
        Surface(
            onClick = onTogglePhraseMode,
            shape = RoundedCornerShape(10.dp),
            color = if (phraseMode) paper.ornamentGold else paper.savedWordBg,
            modifier = Modifier
                .height(36.dp)
                .testTag("tafsir_phrase_mode_button")
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+Frase",
                    color = if (phraseMode) Color.White else paper.ornamentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // 5. Tombol Bookmark
        Surface(
            onClick = onToggleBookmark,
            shape = CircleShape,
            color = if (state.isCurrentBookmarked) paper.savedWordBg else Color.Transparent,
            modifier = Modifier
                .size(30.dp)
                .testTag("tafsir_bookmark_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (state.isCurrentBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (state.isCurrentBookmarked) "Hapus bookmark" else "Simpan bookmark",
                    tint = if (state.isCurrentBookmarked) paper.ornamentGold else paper.inkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 6. Tombol Pengaturan Tampilan
        Surface(
            onClick = onOpenDisplaySettings,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(30.dp)
                .testTag("tafsir_display_settings_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Pengaturan tampilan & bookmark",
                    tint = paper.inkSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TafsirCollapsibleAyahCard(
    state: TafsirUiState,
    paper: TafsirPaperColors,
    savedNormalizedKeys: Set<String>,
    phraseMode: Boolean,
    activePhraseBlockKey: String?,
    phraseStartWordIdx: Int,
    onToggleCollapsed: () -> Unit,
    onSpeakAyah: () -> Unit,
    onTapWord: (ArabicWordToken, List<ArabicWordToken>) -> Unit,
    onLongPressWord: (ArabicWordToken, List<ArabicWordToken>) -> Unit,
    onRetryAyah: () -> Unit
) {
    val surah = state.activeSurah
    val ornament = TafsirTextProcessor.formatAyahOrnament(state.ayahNumber)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = paper.ayahSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.7.dp, paper.border, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Baris header ringkas kartu ayat (selalu tampil, menjadi strip tunggal saat Mode Fokus)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleCollapsed)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("tafsir_toggle_ayah_focus"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${surah.nameLatin} ${state.surahNumber}:${state.ayahNumber}",
                        color = paper.accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        color = paper.inkMuted,
                        fontSize = 11.sp
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "${surah.nameArabic} $ornament",
                            color = paper.inkPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!state.isAyahCollapsed && state.ayahText.isNotBlank()) {
                        Surface(
                            onClick = onSpeakAyah,
                            shape = CircleShape,
                            color = paper.accentSoft,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Dengar pelafalan ayat",
                                    tint = paper.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(paper.pillBg.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.isAyahCollapsed) "Tampilkan Ayat" else "Fokus",
                            color = paper.inkSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (state.isAyahCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = if (state.isAyahCollapsed) "Tampilkan ayat" else "Lipat ayat",
                            tint = paper.inkSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !state.isAyahCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    HorizontalDivider(thickness = 0.5.dp, color = paper.border.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(10.dp))

                    when {
                        state.isLoadingAyah && state.ayahText.isBlank() -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = paper.accent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Memuat ayat Utsmani...",
                                    color = paper.inkSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        state.ayahText.isBlank() && state.ayahErrorMessage != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.ayahErrorMessage,
                                    color = paper.inkSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = onRetryAyah,
                                    shape = CircleShape,
                                    color = paper.accentSoft
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Muat ulang ayat",
                                            tint = paper.accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Muat Ulang", color = paper.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        else -> {
                            TafsirInteractiveArabicText(
                                rawArabic = state.ayahText,
                                blockKey = "ayah",
                                showHarakat = state.showHarakat,
                                fontSizeSp = state.fontSize.ayahSp,
                                lineHeightMultiplier = 1.85f,
                                paper = paper,
                                savedNormalizedKeys = savedNormalizedKeys,
                                phraseMode = phraseMode,
                                activePhraseBlockKey = activePhraseBlockKey,
                                phraseStartWordIndex = phraseStartWordIdx,
                                trailingOrnament = ornament,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Medium,
                                onTapWord = onTapWord,
                                onLongPressWord = onLongPressWord
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBookMetaStrip(
    state: TafsirUiState,
    paper: TafsirPaperColors,
    onOpenBookPicker: () -> Unit
) {
    val book = state.activeBook
    val readyMeta = (state.tafsirState as? TafsirLoadState.Ready)?.content

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenBookPicker)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoStories,
                    contentDescription = null,
                    tint = paper.accent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = book.nameLatin,
                    color = paper.inkPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (readyMeta != null) {
                    Surface(
                        shape = CircleShape,
                        color = if (readyMeta.fromCache) paper.accentSoft else paper.pillBg
                    ) {
                        Text(
                            text = if (readyMeta.fromCache) "Cache Lokal" else readyMeta.sourceLabel,
                            color = if (readyMeta.fromCache) paper.accent else paper.inkMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "${book.category.titleId} • ${book.authorArabic} • ${book.volumeNoteArabic}",
                color = paper.inkMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = book.nameArabic,
                color = paper.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TafsirSpecialNoticeCard(
    title: String,
    arabicTitle: String,
    description: String,
    paper: TafsirPaperColors,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = paper.cardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, paper.border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                tint = paper.ornamentGold,
                modifier = Modifier.size(30.dp)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = arabicTitle,
                    color = paper.accent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                color = paper.inkPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                color = paper.inkSecondary,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrimaryAction,
                    shape = CircleShape,
                    color = paper.accent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (primaryActionLabel.contains("tafsir.app", ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = primaryActionLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    onClick = onSecondaryAction,
                    shape = CircleShape,
                    color = paper.pillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (secondaryActionLabel.contains("tafsir.app", ignoreCase = true)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = paper.inkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = secondaryActionLabel,
                            color = paper.inkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
