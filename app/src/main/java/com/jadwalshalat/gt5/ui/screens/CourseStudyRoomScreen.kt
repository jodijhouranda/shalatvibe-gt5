package com.jadwalshalat.gt5.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.ArabicWordToken
import com.jadwalshalat.gt5.arabic.KamusAiUiState
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind
import com.jadwalshalat.gt5.arabic.PdfReadingMode
import com.jadwalshalat.gt5.learning.CourseEpisode
import com.jadwalshalat.gt5.learning.CourseKamusBridge
import com.jadwalshalat.gt5.learning.GlossaryTerm
import com.jadwalshalat.gt5.learning.SeriesCourse
import com.jadwalshalat.gt5.learning.TopicBlock
import com.jadwalshalat.gt5.learning.TranscriptSegment
import com.jadwalshalat.gt5.ui.components.ArabicYouTubePlayerView
import com.jadwalshalat.gt5.ui.components.PdfDocumentViewer
import com.jadwalshalat.gt5.ui.components.YouTubePlayerController
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import kotlinx.coroutines.launch
import java.io.File

private enum class StudyPaneTab {
    TRANSCRIPT,
    PDF_BOOK
}

private data class CoursePhraseSelection(
    val tokens: List<ArabicWordToken>,
    val startIndex: Int,
    val endIndex: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseStudyRoomScreen(
    course: SeriesCourse,
    episode: CourseEpisode,
    block: TopicBlock,
    initialTimeSec: Int?,
    pdfFile: File?,
    isLoadingPdf: Boolean,
    pdfError: String?,
    isCompleted: Boolean,
    hasNextBlock: Boolean,
    readingMode: PdfReadingMode,
    onCycleReadingMode: () -> Unit,
    onToggleBlockCompleted: () -> Unit,
    onCompleteAndNext: () -> Unit,
    onSelectOtherBlock: (episodeId: String, blockId: String, seekSec: Int?) -> Unit,
    onBack: () -> Unit,
    showHarakat: Boolean = true,
    onToggleHarakat: () -> Unit = {},
    savedNormalizedKeys: Set<String> = emptySet(),
    kamusEntries: List<KamusEntry> = emptyList(),
    kamusCount: Int = 0,
    kamusAiState: KamusAiUiState = KamusAiUiState.Idle,
    onLookupKamusAi: (arabic: String, contextSentence: String, lessonTitle: String, forceRefresh: Boolean) -> Unit = { _, _, _, _ -> },
    onClearKamusAiState: () -> Unit = {},
    onSaveKamusEntry: (KamusEntry) -> Unit = {},
    onOpenKamus: () -> Unit = {},
    onSpeakArabic: (String) -> String? = { null },
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val palette = LocalShalatVibePalette.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    BackHandler(onBack = onBack)

    // Continuous playback across block segments (loop disabled by default so segments flow naturally)
    val ytController = remember(episode.youtubeVideoId) {
        YouTubePlayerController(
            initialStartSec = block.startSec,
            initialEndSec = block.endSec,
            initialLoopEnabled = false
        )
    }

    LaunchedEffect(episode.youtubeVideoId, block.id, initialTimeSec) {
        ytController.setSegmentWindow(
            startSec = block.startSec,
            endSec = block.endSec,
            seekNow = true
        )
        if (initialTimeSec != null && initialTimeSec >= block.startSec) {
            ytController.seekTo(initialTimeSec, autoPlay = true)
        }
    }

    var activePane by remember(block.id) { mutableStateOf(StudyPaneTab.TRANSCRIPT) }
    var isVideoCollapsed by remember { mutableStateOf(false) }
    var selectedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }

    // Interactive Arabic word/phrase collector state (shared Kamus Saya + AI Kamus)
    var phraseMode by remember(block.id) { mutableStateOf(false) }
    var phraseStartSegIdx by remember(block.id) { mutableIntStateOf(-1) }
    var phraseStartWordIdx by remember(block.id) { mutableIntStateOf(-1) }
    var phraseStartSegment by remember(block.id) { mutableStateOf<TranscriptSegment?>(null) }
    var phraseStartTokens by remember(block.id) { mutableStateOf<List<ArabicWordToken>>(emptyList()) }
    var pendingKamusDraft by remember { mutableStateOf<KamusEntry?>(null) }
    var pendingPhraseSelection by remember { mutableStateOf<CoursePhraseSelection?>(null) }

    fun resetPhraseSelection() {
        phraseMode = false
        phraseStartSegIdx = -1
        phraseStartWordIdx = -1
        phraseStartSegment = null
        phraseStartTokens = emptyList()
    }

    fun openWordCollector(
        arabicPhrase: String,
        seg: TranscriptSegment?,
        overrideKind: KamusEntryKind? = null,
        selectionContext: CoursePhraseSelection? = null
    ) {
        onClearKamusAiState()
        pendingPhraseSelection = selectionContext
        pendingKamusDraft = CourseKamusBridge.buildDraftFromTranscriptWord(
            arabicPhrase = arabicPhrase,
            course = course,
            episode = episode,
            block = block,
            segment = seg,
            existingEntries = kamusEntries,
            overrideKind = overrideKind
        )
    }

    // Live active segment tracking current video seconds
    val currentSec = ytController.currentSeconds
    val activeSegmentIndex by remember(block.id, block.segments, currentSec) {
        derivedStateOf {
            val segs = block.segments
            if (segs.isEmpty()) -1
            else {
                val exact = segs.indexOfFirst { currentSec >= it.startSec && currentSec < it.endSec }
                if (exact >= 0) exact
                else if (currentSec >= segs.last().endSec) segs.lastIndex
                else 0
            }
        }
    }

    val transcriptListState = rememberLazyListState()
    var isAutoFollowEnabled by remember(block.id) { mutableStateOf(true) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Detect manual user scrolling to pause auto-scroll without interrupting reading
    LaunchedEffect(transcriptListState.isScrollInProgress) {
        if (transcriptListState.isScrollInProgress && !isProgrammaticScroll) {
            isAutoFollowEnabled = false
        }
    }

    // Auto-scroll smoothly when the active segment changes while playing/following
    LaunchedEffect(activeSegmentIndex, isAutoFollowEnabled, activePane) {
        if (activePane == StudyPaneTab.TRANSCRIPT && isAutoFollowEnabled && activeSegmentIndex >= 0 && !phraseMode && pendingKamusDraft == null) {
            // Item 0: Kamus quick tools strip, Item 1: block summary strip; segment i is at index i + 2
            val targetItemIndex = activeSegmentIndex + 2
            isProgrammaticScroll = true
            try {
                transcriptListState.animateScrollToItem(targetItemIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    val openExternalYouTube: () -> Unit = {
        val sec = ytController.currentSeconds.takeIf { it > 0 } ?: block.startSec
        val url = "https://www.youtube.com/watch?v=${episode.youtubeVideoId}&t=${sec}s"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    val videoHeight by animateDpAsState(
        targetValue = if (isVideoCollapsed) 1.dp else 198.dp,
        animationSpec = tween(240),
        label = "course_video_height"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
    ) {
        // 1. Sleek Borderless Theatre Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.pillBg)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali ke daftar seri",
                    tint = palette.textPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Seri ${episode.episodeNumber} • ${block.title}",
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${block.timeRangeLabel} • Kitab Hal. ${block.pdfPageHint}",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (activePane == StudyPaneTab.PDF_BOOK) {
                Surface(
                    onClick = onCycleReadingMode,
                    shape = CircleShape,
                    color = palette.cardSurface,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = readingMode.label,
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            IconButton(
                onClick = { isVideoCollapsed = !isVideoCollapsed },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isVideoCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (isVideoCollapsed) "Tampilkan video" else "Ciutkan video",
                    tint = palette.textSecondary
                )
            }

            Surface(
                onClick = onToggleBlockCompleted,
                shape = CircleShape,
                color = if (isCompleted) palette.emeraldSoft else palette.cardSurface,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Tandai blok selesai",
                        tint = if (isCompleted) palette.emeraldPrimary else palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCompleted) "Selesai" else "Tandai",
                        color = if (isCompleted) palette.emeraldPrimary else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Sticky YouTube Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(videoHeight)
                .background(Color.Black)
        ) {
            ArabicYouTubePlayerView(
                videoId = episode.youtubeVideoId,
                controller = ytController,
                onOpenExternalYouTube = openExternalYouTube,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Unified Borderless Playback Controls & Speed Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.pillBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = { ytController.togglePlayPause() },
                shape = CircleShape,
                color = palette.emeraldPrimary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (ytController.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (palette.isDark) Color(0xFF03231D) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                onClick = {
                    isAutoFollowEnabled = true
                    ytController.replayWindow()
                },
                shape = CircleShape,
                color = palette.cardSurfaceElevated,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = "Ulangi blok",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Surface(
                onClick = { ytController.setLoop(!ytController.loopEnabled) },
                shape = CircleShape,
                color = if (ytController.loopEnabled) palette.emeraldSoft else palette.cardSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Loop blok",
                        tint = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Loop",
                        color = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            listOf(0.75f, 1.0f, 1.25f).forEach { speed ->
                val active = kotlin.math.abs(ytController.playbackRate - speed) < 0.05f
                Surface(
                    onClick = { ytController.setSpeed(speed) },
                    shape = CircleShape,
                    color = if (active) palette.goldSoft else palette.cardSurfaceElevated
                ) {
                    Text(
                        text = if (speed == 1.0f) "1x" else "${speed}x",
                        color = if (active) palette.goldAccent else palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${ArabicCatalogParser.formatTimestamp(currentSec.takeIf { it > 0 } ?: block.startSec)} / ${ArabicCatalogParser.formatTimestamp(block.endSec)}",
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                )
            )

            IconButton(
                onClick = openExternalYouTube,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Buka di YouTube",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // 4. Sleek Tonal Segmented Switcher [ Teks & Arti | Kitab PDF ]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.pillBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentedModeButton(
                        label = "Teks & Arti",
                        icon = Icons.Rounded.Translate,
                        selected = activePane == StudyPaneTab.TRANSCRIPT,
                        onClick = { activePane = StudyPaneTab.TRANSCRIPT },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedModeButton(
                        label = "Kitab PDF • Hal. ${block.pdfPageHint}",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        selected = activePane == StudyPaneTab.PDF_BOOK,
                        onClick = { activePane = StudyPaneTab.PDF_BOOK },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Dual-Mode Content Pane
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activePane) {
                StudyPaneTab.TRANSCRIPT -> {
                    LazyColumn(
                        state = transcriptListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Item 0: Compact Kamus & Interactive Reader Tools Strip
                        item(key = "course_kamus_tools") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onToggleHarakat,
                                    shape = CircleShape,
                                    color = if (showHarakat) palette.emeraldSoft else palette.pillBg,
                                    modifier = Modifier.testTag("course_toggle_harakat_pill")
                                ) {
                                    Text(
                                        text = if (showHarakat) "تَشْكِيل" else "Gundul",
                                        color = if (showHarakat) palette.emeraldPrimary else palette.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    onClick = {
                                        if (phraseMode) {
                                            resetPhraseSelection()
                                        } else {
                                            phraseMode = true
                                            phraseStartSegIdx = -1
                                            phraseStartWordIdx = -1
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (phraseMode) palette.goldAccent else palette.goldSoft,
                                    modifier = Modifier.testTag("course_phrase_mode_pill")
                                ) {
                                    Text(
                                        text = if (phraseMode) "Batal Frase" else "+ Frase",
                                        color = if (phraseMode) Color.White else palette.goldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    onClick = {
                                        val currentSeg = block.segments.getOrNull(activeSegmentIndex.coerceAtLeast(0))
                                        onClearKamusAiState()
                                        pendingPhraseSelection = null
                                        pendingKamusDraft = CourseKamusBridge.buildDraftFromTranscriptWord(
                                            arabicPhrase = "",
                                            course = course,
                                            episode = episode,
                                            block = block,
                                            segment = currentSeg,
                                            existingEntries = kamusEntries,
                                            overrideKind = KamusEntryKind.WORD
                                        ).copy(id = "", arabic = "")
                                    },
                                    shape = CircleShape,
                                    color = palette.pillBg,
                                    modifier = Modifier.testTag("course_manual_kamus_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Catat kosakata",
                                            tint = palette.emeraldPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Catat",
                                            color = palette.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Surface(
                                    onClick = onOpenKamus,
                                    shape = CircleShape,
                                    color = palette.emeraldSoft,
                                    modifier = Modifier.testTag("course_open_kamus_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.BookmarkAdd,
                                            contentDescription = "Buka Kamus Saya",
                                            tint = palette.emeraldPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Kamus ($kamusCount)",
                                            color = palette.emeraldPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Item 1: Compact Borderless Summary Strip & Key Glossary Pills
                        item(key = "block_summary") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.pillBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "RINGKASAN BAHASAN • KETUK KATA ARAB UNTUK KAMUS & AI",
                                        color = palette.emeraldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = block.summary,
                                        color = palette.textSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )

                                    val keyTerms = remember(block.keyTermIds, course.glossary) {
                                        block.keyTermIds.mapNotNull { course.glossaryById(it) }
                                    }
                                    if (keyTerms.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(9.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            keyTerms.forEach { term ->
                                                GlossaryChip(
                                                    term = term,
                                                    onClick = { selectedTerm = term }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Continuous Timestamped Transcript Cards (Auto-Highlight + Interactive Arabic Words)
                        itemsIndexed(block.segments, key = { _, seg -> seg.id }) { idx, segment ->
                            val isActive = idx == activeSegmentIndex
                            TranscriptSegmentCard(
                                segment = segment,
                                segmentIndex = idx,
                                course = course,
                                isActive = isActive,
                                showHarakat = showHarakat,
                                savedNormalizedKeys = savedNormalizedKeys,
                                phraseMode = phraseMode,
                                phraseStartSegIdx = phraseStartSegIdx,
                                phraseStartWordIdx = phraseStartWordIdx,
                                onPlaySegment = {
                                    isAutoFollowEnabled = true
                                    ytController.seekTo(segment.startSec, autoPlay = true)
                                },
                                onSpeakSegment = {
                                    onSpeakArabic(segment.arabic)?.let(onShowMessage)
                                },
                                onTapWord = { tok, tokens ->
                                    if (!phraseMode) {
                                        openWordCollector(
                                            arabicPhrase = tok.cleanArabic,
                                            seg = segment,
                                            overrideKind = KamusEntryKind.WORD,
                                            selectionContext = CoursePhraseSelection(tokens, tok.index, tok.index)
                                        )
                                    } else if (phraseStartWordIdx < 0 || phraseStartSegIdx != idx) {
                                        phraseStartSegIdx = idx
                                        phraseStartWordIdx = tok.index
                                        phraseStartSegment = segment
                                        phraseStartTokens = tokens
                                    } else {
                                        val startIdx = minOf(phraseStartWordIdx, tok.index)
                                        val endIdx = maxOf(phraseStartWordIdx, tok.index)
                                        val phrase = ArabicTextUtils.buildPhraseFromRange(tokens, startIdx, endIdx)
                                        resetPhraseSelection()
                                        if (phrase.isNotBlank()) {
                                            openWordCollector(
                                                arabicPhrase = phrase,
                                                seg = segment,
                                                overrideKind = if (endIdx > startIdx) KamusEntryKind.PHRASE else KamusEntryKind.WORD,
                                                selectionContext = CoursePhraseSelection(tokens, startIdx, endIdx)
                                            )
                                        }
                                    }
                                },
                                onLongPressWord = { tok, tokens ->
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    phraseMode = true
                                    phraseStartSegIdx = idx
                                    phraseStartWordIdx = tok.index
                                    phraseStartSegment = segment
                                    phraseStartTokens = tokens
                                },
                                onTapGlossaryTerm = { term -> selectedTerm = term }
                            )
                        }

                        // Sleek Borderless Bottom Actions
                        item(key = "study_footer") {
                            StudyBlockBottomActions(
                                isCompleted = isCompleted,
                                hasNextBlock = hasNextBlock,
                                onToggleCompleted = onToggleBlockCompleted,
                                onCompleteAndNext = onCompleteAndNext
                            )
                        }
                    }

                    // Floating Banner saat Mode Pilih Frase aktif
                    if (phraseMode) {
                        Surface(
                            shape = CircleShape,
                            color = palette.goldAccent,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                            val seg = phraseStartSegment
                                            val tokens = phraseStartTokens
                                            val startIdx = phraseStartWordIdx
                                            val single = ArabicTextUtils.buildPhraseFromRange(tokens, startIdx, startIdx)
                                            resetPhraseSelection()
                                            if (single.isNotBlank()) {
                                                openWordCollector(
                                                    arabicPhrase = single,
                                                    seg = seg,
                                                    overrideKind = KamusEntryKind.WORD,
                                                    selectionContext = CoursePhraseSelection(tokens, startIdx, startIdx)
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                        color = palette.emeraldPrimary
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

                    // Floating "Ikuti Video" pill when user manually scrolled away
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isAutoFollowEnabled && activeSegmentIndex >= 0 && !phraseMode,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 18.dp)
                    ) {
                        Surface(
                            onClick = {
                                isAutoFollowEnabled = true
                                scope.launch {
                                    isProgrammaticScroll = true
                                    try {
                                        transcriptListState.animateScrollToItem(activeSegmentIndex + 2)
                                    } finally {
                                        isProgrammaticScroll = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = palette.emeraldPrimary,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (palette.isDark) Color(0xFF03231D) else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ikuti Video (${ArabicCatalogParser.formatTimestamp(currentSec)})",
                                    color = if (palette.isDark) Color(0xFF03231D) else Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                StudyPaneTab.PDF_BOOK -> {
                    when {
                        isLoadingPdf -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = palette.emeraldPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Memuat Kitab Fiqh Muamalah (95 Hal)...",
                                        color = palette.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        pdfFile != null -> {
                            PdfDocumentViewer(
                                pdfFile = pdfFile,
                                readingMode = readingMode,
                                initialPage = block.pdfPageHint,
                                modifier = Modifier.fillMaxSize(),
                                bottomActionContent = {
                                    StudyBlockBottomActions(
                                        isCompleted = isCompleted,
                                        hasNextBlock = hasNextBlock,
                                        onToggleCompleted = onToggleBlockCompleted,
                                        onCompleteAndNext = onCompleteAndNext
                                    )
                                }
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pdfError ?: "File PDF kitab tidak ditemukan.",
                                    color = palette.textSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTerm?.let { term ->
        val termNorm = remember(term.arabic) { ArabicTextUtils.normalizeForLookup(term.arabic) }
        val isSaved = termNorm.isNotBlank() && savedNormalizedKeys.contains(termNorm)
        GlossaryTermBottomSheet(
            term = term,
            isSavedInKamus = isSaved,
            onDismiss = { selectedTerm = null },
            onSpeakArabic = { txt -> onSpeakArabic(txt)?.let(onShowMessage) },
            onSaveToKamus = { targetTerm ->
                selectedTerm = null
                onClearKamusAiState()
                pendingPhraseSelection = null
                pendingKamusDraft = CourseKamusBridge.buildDraftFromGlossaryTerm(
                    term = targetTerm,
                    course = course,
                    fallbackEpisode = episode,
                    fallbackBlock = block,
                    existingEntries = kamusEntries
                )
            },
            onJumpToVideo = { target ->
                selectedTerm = null
                if (target.blockId == block.id && target.exampleTimeSec > 0) {
                    isAutoFollowEnabled = true
                    ytController.seekTo(target.exampleTimeSec, autoPlay = true)
                } else {
                    val found = course.findEpisodeAndBlock(target.blockId)
                    if (found != null) {
                        onSelectOtherBlock(
                            found.first.id,
                            found.second.id,
                            target.exampleTimeSec.takeIf { it > 0 }
                        )
                    }
                }
            }
        )
    }

    pendingKamusDraft?.let { draft ->
        val selCtx = pendingPhraseSelection
        KamusEntryEditorSheet(
            initialEntry = draft,
            aiState = kamusAiState,
            onRequestAi = onLookupKamusAi,
            onClearAiState = onClearKamusAiState,
            segmentTokens = selCtx?.tokens.orEmpty(),
            initialRangeStart = selCtx?.startIndex ?: -1,
            initialRangeEnd = selCtx?.endIndex ?: -1,
            onDismiss = {
                onClearKamusAiState()
                pendingKamusDraft = null
                pendingPhraseSelection = null
            },
            onSave = { entry ->
                onSaveKamusEntry(entry)
                onClearKamusAiState()
                pendingKamusDraft = null
                pendingPhraseSelection = null
                onShowMessage("Disimpan ke Kamus Saya: ${entry.arabic}")
            },
            onSpeakArabic = { txt -> onSpeakArabic(txt)?.let(onShowMessage) }
        )
    }
}

@Composable
fun CourseFullBookPdfScreen(
    course: SeriesCourse,
    pdfFile: File?,
    isLoadingPdf: Boolean,
    pdfError: String?,
    initialPage: Int,
    readingMode: PdfReadingMode,
    onCycleReadingMode: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.pillBg)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali",
                    tint = palette.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.bookTitle.ifBlank { course.title },
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course.bookTotalPages} Halaman • ${course.instructor}",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onCycleReadingMode,
                shape = CircleShape,
                color = palette.cardSurface,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "Mode: ${readingMode.label}",
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoadingPdf -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.emeraldPrimary)
                    }
                }
                pdfFile != null -> {
                    PdfDocumentViewer(
                        pdfFile = pdfFile,
                        readingMode = readingMode,
                        initialPage = initialPage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pdfError ?: "PDF tidak dapat dimuat.",
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedModeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val bg by animateColorAsState(
        targetValue = if (selected) palette.cardSurface else Color.Transparent,
        animationSpec = tween(180),
        label = "seg_mode_bg"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.emeraldPrimary else palette.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) palette.textPrimary else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TranscriptSegmentCard(
    segment: TranscriptSegment,
    segmentIndex: Int,
    course: SeriesCourse,
    isActive: Boolean,
    showHarakat: Boolean,
    savedNormalizedKeys: Set<String>,
    phraseMode: Boolean,
    phraseStartSegIdx: Int,
    phraseStartWordIdx: Int,
    onPlaySegment: () -> Unit,
    onSpeakSegment: () -> Unit,
    onTapWord: (ArabicWordToken, List<ArabicWordToken>) -> Unit,
    onLongPressWord: (ArabicWordToken, List<ArabicWordToken>) -> Unit,
    onTapGlossaryTerm: (GlossaryTerm) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val tokens = remember(segment.arabic) { ArabicTextUtils.tokenizeSentenceWords(segment.arabic) }
    val containerColor by animateColorAsState(
        targetValue = if (isActive) palette.highlightSurface else palette.cardSurface,
        animationSpec = tween(220),
        label = "transcript_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(palette.emeraldBright)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (isActive) 12.dp else 15.dp,
                        end = 15.dp,
                        top = 14.dp,
                        bottom = 14.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPlaySegment,
                        shape = CircleShape,
                        color = if (isActive) palette.emeraldPrimary else palette.pillBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (palette.isDark) Color(0xFF03231D) else Color.White)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Putar bagian ini",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isActive) {
                                    "${segment.timeRangeLabel} • Diputar"
                                } else {
                                    segment.timeRangeLabel
                                },
                                style = TextStyle(
                                    color = if (isActive) {
                                        if (palette.isDark) Color(0xFF03231D) else Color.White
                                    } else {
                                        palette.textSecondary
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = onSpeakSegment,
                        shape = CircleShape,
                        color = palette.emeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Dengar pengucapan Arab",
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dengar",
                                color = palette.emeraldPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kata-kata Arab interaktif (ketuk untuk Kamus + AI, tekan lama untuk pilih frase)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        tokens.forEach { tok ->
                            val isSaved = tok.hasArabicLetters &&
                                tok.normalizedKey.length >= 2 &&
                                savedNormalizedKeys.contains(tok.normalizedKey)
                            val isPhraseStart = phraseStartSegIdx == segmentIndex && phraseStartWordIdx == tok.index
                            val displayWord = remember(tok.raw, showHarakat) {
                                if (showHarakat) tok.raw else ArabicTextUtils.stripHarakat(tok.raw)
                            }
                            val isSamePhraseSegment = phraseMode && phraseStartSegIdx == segmentIndex
                            val chipBg = when {
                                isPhraseStart -> palette.emeraldPrimary
                                isSaved -> palette.goldSoft
                                isSamePhraseSegment && tok.hasArabicLetters -> palette.emeraldSoft.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }
                            val chipFg = when {
                                isPhraseStart -> Color.White
                                isSaved -> palette.goldAccent
                                else -> palette.textPrimary
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(chipBg)
                                    .then(
                                        if (tok.hasArabicLetters && tok.cleanArabic.isNotBlank()) {
                                            Modifier.combinedClickable(
                                                onClick = { onTapWord(tok, tokens) },
                                                onLongClick = { onLongPressWord(tok, tokens) }
                                            )
                                        } else Modifier
                                    )
                                    .padding(
                                        horizontal = if (isSaved || isPhraseStart || isSamePhraseSegment) 4.dp else 1.dp,
                                        vertical = 0.dp
                                    )
                            ) {
                                Text(
                                    text = displayWord,
                                    color = chipFg,
                                    fontSize = 19.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = if (isSaved || isPhraseStart) FontWeight.Bold else FontWeight.Medium,
                                    textDecoration = if (isSaved) TextDecoration.Underline else TextDecoration.None
                                )
                            }
                        }
                    }
                }

                if (segment.indonesian.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = segment.indonesian,
                        color = if (isActive) palette.textPrimary.copy(alpha = 0.88f) else palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPlaySegment)
                    )
                }

                val terms = remember(segment.termIds, course.glossary) {
                    segment.termIds.mapNotNull { course.glossaryById(it) }
                }
                if (terms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        terms.forEach { term ->
                            GlossaryChip(
                                term = term,
                                onClick = { onTapGlossaryTerm(term) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossaryChip(
    term: GlossaryTerm,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.goldSoft
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = palette.goldAccent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${term.arabic} • ${term.term}",
                color = palette.goldAccent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudyBlockBottomActions(
    isCompleted: Boolean,
    hasNextBlock: Boolean,
    onToggleCompleted: () -> Unit,
    onCompleteAndNext: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onToggleCompleted,
            shape = RoundedCornerShape(16.dp),
            color = if (isCompleted) palette.emeraldSoft else palette.cardSurface,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 13.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (isCompleted) palette.emeraldPrimary else palette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isCompleted) "Selesai ✓" else "Tandai Selesai",
                    color = if (isCompleted) palette.emeraldPrimary else palette.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasNextBlock) {
            Surface(
                onClick = onCompleteAndNext,
                shape = RoundedCornerShape(16.dp),
                color = palette.emeraldPrimary,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 13.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blok Berikutnya",
                        color = if (palette.isDark) Color(0xFF03231D) else Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (palette.isDark) Color(0xFF03231D) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
