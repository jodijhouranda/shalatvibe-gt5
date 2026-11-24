package com.jadwalshalat.gt5.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import com.jadwalshalat.gt5.arabic.PdfReadingMode
import com.jadwalshalat.gt5.learning.CourseEpisode
import com.jadwalshalat.gt5.learning.GlossaryTerm
import com.jadwalshalat.gt5.learning.SeriesCourse
import com.jadwalshalat.gt5.learning.TopicBlock
import com.jadwalshalat.gt5.learning.TranscriptSegment
import com.jadwalshalat.gt5.ui.components.ArabicYouTubePlayerView
import com.jadwalshalat.gt5.ui.components.PdfDocumentViewer
import com.jadwalshalat.gt5.ui.components.YouTubePlayerController
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import java.io.File

private enum class StudyPaneTab {
    TRANSCRIPT,
    PDF_BOOK
}

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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalShalatVibePalette.current
    BackHandler(onBack = onBack)

    // Pertahankan YouTubePlayerController per youtubeVideoId agar pindah blok di seri yang sama instan
    val ytController = remember(episode.youtubeVideoId) {
        YouTubePlayerController(
            initialStartSec = block.startSec,
            initialEndSec = block.endSec
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
    var activeSegmentId by remember(block.id) { mutableStateOf<String?>(null) }
    var selectedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }

    val openExternalYouTube: () -> Unit = {
        val sec = ytController.currentSeconds.takeIf { it > 0 } ?: block.startSec
        val url = "https://www.youtube.com/watch?v=${episode.youtubeVideoId}&t=${sec}s"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    val videoHeight by animateDpAsState(
        targetValue = if (isVideoCollapsed) 1.dp else 204.dp,
        animationSpec = tween(240),
        label = "course_video_height"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
    ) {
        // 1. Compact Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.cardSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    fontSize = 13.sp,
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
                    shape = RoundedCornerShape(10.dp),
                    color = palette.pillBg,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = readingMode.label,
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            IconButton(onClick = { isVideoCollapsed = !isVideoCollapsed }) {
                Icon(
                    imageVector = if (isVideoCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    contentDescription = if (isVideoCollapsed) "Tampilkan video" else "Ciutkan video",
                    tint = palette.textSecondary
                )
            }

            IconButton(onClick = onToggleBlockCompleted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Tandai blok selesai",
                    tint = if (isCompleted) palette.emeraldPrimary else palette.textMuted
                )
            }
        }

        // 2. Sticky YouTube Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(videoHeight)
                .background(palette.cardSurface)
        ) {
            ArabicYouTubePlayerView(
                videoId = episode.youtubeVideoId,
                controller = ytController,
                onOpenExternalYouTube = openExternalYouTube,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Playback Controls & Speed Toolbar
        Surface(
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            tint = palette.cardSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    onClick = { ytController.replayWindow() },
                    shape = CircleShape,
                    color = palette.pillBg,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = "Ulangi blok",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    onClick = { ytController.setLoop(!ytController.loopEnabled) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (ytController.loopEnabled) palette.emeraldSoft else palette.pillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Loop blok",
                            tint = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Loop",
                            color = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                listOf(0.75f, 1.0f, 1.25f).forEach { speed ->
                    val active = kotlin.math.abs(ytController.playbackRate - speed) < 0.05f
                    Surface(
                        onClick = { ytController.setSpeed(speed) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) palette.goldAccent.copy(alpha = 0.16f) else palette.pillBg
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1x" else "${speed}x",
                            color = if (active) palette.goldAccent else palette.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = ArabicCatalogParser.formatTimestamp(
                        ytController.currentSeconds.takeIf { it > 0 } ?: block.startSec
                    ),
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = openExternalYouTube,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Buka di YouTube",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 4. Dual-Mode Toggle [ Teks & Arti | Buku PDF ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.background)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudyModeTabButton(
                label = "Teks & Arti",
                icon = Icons.Rounded.Translate,
                selected = activePane == StudyPaneTab.TRANSCRIPT,
                onClick = { activePane = StudyPaneTab.TRANSCRIPT },
                modifier = Modifier.weight(1f)
            )
            StudyModeTabButton(
                label = "Kitab PDF (Hal. ${block.pdfPageHint})",
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                selected = activePane == StudyPaneTab.PDF_BOOK,
                onClick = { activePane = StudyPaneTab.PDF_BOOK },
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Dual-Mode Content Pane
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activePane) {
                StudyPaneTab.TRANSCRIPT -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Block Summary & Glossary Pills
                        item(key = "block_summary") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardSurface,
                                border = BorderStroke(1.dp, palette.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Ringkasan Blok",
                                        color = palette.emeraldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = block.summary,
                                        color = palette.textPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )

                                    val keyTerms = remember(block.keyTermIds, course.glossary) {
                                        block.keyTermIds.mapNotNull { course.glossaryById(it) }
                                    }
                                    if (keyTerms.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Ketuk Istilah untuk Penjelasan & Contoh:",
                                            color = palette.textSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
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

                        // Timestamped Transcript Cards
                        items(block.segments, key = { it.id }) { segment ->
                            TranscriptSegmentCard(
                                segment = segment,
                                course = course,
                                isActive = activeSegmentId == segment.id,
                                onPlaySegment = {
                                    activeSegmentId = segment.id
                                    ytController.setSegmentWindow(
                                        startSec = segment.startSec,
                                        endSec = segment.endSec,
                                        seekNow = true
                                    )
                                },
                                onTapGlossaryTerm = { term -> selectedTerm = term }
                            )
                        }

                        // Completion Footer
                        item(key = "study_footer") {
                            StudyBlockBottomActions(
                                isCompleted = isCompleted,
                                hasNextBlock = hasNextBlock,
                                onToggleCompleted = onToggleBlockCompleted,
                                onCompleteAndNext = onCompleteAndNext
                            )
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
        GlossaryTermBottomSheet(
            term = term,
            onDismiss = { selectedTerm = null },
            onJumpToVideo = { target ->
                selectedTerm = null
                if (target.blockId == block.id && target.exampleTimeSec > 0) {
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
                .background(palette.cardSurface)
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
                shape = RoundedCornerShape(10.dp),
                color = palette.pillBg,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "Mode: ${readingMode.label}",
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
private fun StudyModeTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.emeraldPrimary else palette.cardSurface,
        border = BorderStroke(
            1.dp,
            if (selected) palette.emeraldPrimary else palette.cardBorder
        ),
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
                tint = if (selected) palette.cardSurface else palette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) palette.cardSurface else palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptSegmentCard(
    segment: TranscriptSegment,
    course: SeriesCourse,
    isActive: Boolean,
    onPlaySegment: () -> Unit,
    onTapGlossaryTerm: (GlossaryTerm) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) palette.highlightSurface else palette.cardSurface,
        border = BorderStroke(
            1.dp,
            if (isActive) palette.highlightBorder else palette.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPlaySegment,
                    shape = RoundedCornerShape(8.dp),
                    color = palette.emeraldSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar bagian ini",
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Putar ${segment.timeRangeLabel}",
                            color = palette.emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = segment.arabic,
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            if (segment.indonesian.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = segment.indonesian,
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
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

@Composable
private fun GlossaryChip(
    term: GlossaryTerm,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = palette.goldAccent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, palette.goldAccent.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
                fontSize = 11.sp,
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
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onToggleCompleted,
            shape = RoundedCornerShape(12.dp),
            color = if (isCompleted) palette.emeraldSoft else palette.cardSurface,
            border = BorderStroke(
                1.dp,
                if (isCompleted) palette.emeraldPrimary else palette.cardBorder
            ),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp),
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (hasNextBlock) {
            Surface(
                onClick = onCompleteAndNext,
                shape = RoundedCornerShape(12.dp),
                color = palette.emeraldPrimary,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lanjut Blok Berikutnya",
                        color = palette.cardSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = palette.cardSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
