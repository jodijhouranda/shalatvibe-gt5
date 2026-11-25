package com.jadwalshalat.gt5.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import com.jadwalshalat.gt5.arabic.ArabicLesson
import com.jadwalshalat.gt5.arabic.KamusAiUiState
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.PdfReadingMode
import com.jadwalshalat.gt5.ui.components.ArabicInteractiveReader
import com.jadwalshalat.gt5.ui.components.ArabicYouTubePlayerView
import com.jadwalshalat.gt5.ui.components.PdfDocumentViewer
import com.jadwalshalat.gt5.ui.components.YouTubePlayerController
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import java.io.File

@Composable
fun ArabicStudyRoomScreen(
    lesson: ArabicLesson,
    pdfFile: File?,
    isLoadingPdf: Boolean,
    pdfErrorMessage: String?,
    isCompleted: Boolean,
    readingMode: PdfReadingMode,
    lessonContent: com.jadwalshalat.gt5.arabic.ArabicLessonContent? = null,
    isLoadingContent: Boolean = false,
    initialSeekSec: Int? = null,
    showHarakat: Boolean = true,
    savedNormalizedKeys: Set<String> = emptySet(),
    kamusEntries: List<KamusEntry> = emptyList(),
    kamusCount: Int = 0,
    kamusAiState: KamusAiUiState = KamusAiUiState.Idle,
    onLookupKamusAi: (arabic: String, contextSentence: String, lessonTitle: String, forceRefresh: Boolean) -> Unit = { _, _, _, _ -> },
    onClearKamusAiState: () -> Unit = {},
    onToggleHarakat: () -> Unit = {},
    onSaveKamusEntry: (com.jadwalshalat.gt5.arabic.KamusEntry) -> Unit = {},
    onOpenKamus: () -> Unit = {},
    onSpeakArabic: (String) -> String? = { null },
    onCycleReadingMode: () -> Unit,
    onToggleCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalShalatVibePalette.current

    BackHandler(onBack = onBack)

    val ytController = remember(lesson.id) {
        YouTubePlayerController(
            initialStartSec = initialSeekSec ?: lesson.startSeconds,
            initialEndSec = lesson.endSeconds
        )
    }

    // -1 berarti seluruh segmen utama (mis. 06:08–08:15), >= 0 berarti sub-segmen ke-i
    var activeSegmentIndex by remember(lesson.id) { mutableIntStateOf(-1) }
    var isPdfFocusMode by remember(lesson.id) { mutableStateOf(false) }
    var useInteractiveMode by remember(lesson.id, lessonContent != null) {
        mutableStateOf(lessonContent != null)
    }
    var showManualKamusSheet by remember { mutableStateOf(false) }

    LaunchedEffect(lesson.id, initialSeekSec) {
        val seekSec = initialSeekSec ?: return@LaunchedEffect
        ytController.seekTo(seekSec, autoPlay = true)
        val segIdx = lessonContent?.findSegmentAt(seekSec)
            ?: lesson.segments.indexOfFirst { seekSec in it.startSec..it.endSec }.takeIf { it >= 0 }
        if (segIdx != null) {
            activeSegmentIndex = segIdx
        }
    }

    val openInYouTubeApp: () -> Unit = {
        val targetSec = ytController.currentSeconds.takeIf { it > 0 } ?: ytController.windowStartSec
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lesson.youtubeWatchUrl(targetSec)))
        runCatching { context.startActivity(intent) }
    }

    val videoHeight by animateDpAsState(
        targetValue = if (isPdfFocusMode) 1.dp else 182.dp,
        animationSpec = tween(260),
        label = "video_height"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Sleek Tonal Top Study Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.pillBg)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(lesson.sourceName.ifBlank { lesson.level })
                        if (lesson.hasVideo) {
                            append(" • ")
                            append(lesson.segmentRangeLabel)
                        }
                    },
                    color = palette.textSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Kamus Saya shortcut pill
            Surface(
                onClick = onOpenKamus,
                shape = CircleShape,
                color = palette.goldSoft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Book,
                        contentDescription = "Buka Kamus Saya",
                        tint = palette.goldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$kamusCount",
                        color = palette.goldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Teks Interaktif <-> PDF Asli single compact toggle pill
            if (lessonContent != null) {
                Surface(
                    onClick = { useInteractiveMode = !useInteractiveMode },
                    shape = CircleShape,
                    color = palette.cardSurface
                ) {
                    Text(
                        text = if (useInteractiveMode) "PDF Asli" else "Teks",
                        color = palette.emeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            // Saat mode PDF Asli: kontras + catat kata manual
            if (!useInteractiveMode || lessonContent == null) {
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    onClick = onCycleReadingMode,
                    shape = CircleShape,
                    color = palette.cardSurface,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Contrast,
                            contentDescription = "Mode baca PDF: ${readingMode.label}",
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    onClick = { showManualKamusSheet = true },
                    shape = CircleShape,
                    color = palette.emeraldPrimary,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Catat kata ke Kamus",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (lesson.hasVideo) {
                IconButton(
                    onClick = openInYouTubeApp,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Buka di aplikasi YouTube",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Sticky Collapsible YouTube Player & Controls (hanya untuk modul dengan video)
        if (lesson.hasVideo) {
            // Keep WebView mounted even in PDF Focus Mode (1dp) so audio never interrupts!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(videoHeight)
                    .alpha(if (isPdfFocusMode) 0f else 1f)
            ) {
                ArabicYouTubePlayerView(
                    videoId = lesson.youtubeVideoId,
                    controller = ytController,
                    onOpenExternalYouTube = openInYouTubeApp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isPdfFocusMode) {
                // Compact Mini-Player Bar (Mode Fokus PDF ~85% layar untuk PDF)
                Surface(
                    color = palette.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lesson.sourceName.ifBlank { "Audio YouTube" },
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ArabicCatalogParser.formatTimestamp(ytController.currentSeconds)} / ${ArabicCatalogParser.formatTimestamp(ytController.windowEndSec)}" +
                                    if (ytController.loopEnabled) " • Loop" else "",
                                style = TextStyle(
                                    color = palette.emeraldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        IconButton(
                            onClick = { ytController.replayWindow() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "Ulangi segmen",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Surface(
                            onClick = { ytController.togglePlayPause() },
                            shape = CircleShape,
                            color = palette.emeraldPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (ytController.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Putar/Jeda",
                                    tint = if (palette.isDark) Color(0xFF03231D) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = { isPdfFocusMode = false },
                            shape = CircleShape,
                            color = palette.pillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Tampilkan video",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Video",
                                    color = palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // Full Split-View Controls Strip + Segment Timestamp Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardSurface)
                        .padding(top = 6.dp, bottom = 7.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live timestamp counter
                        Text(
                            text = "${ArabicCatalogParser.formatTimestamp(ytController.currentSeconds)} / ${ArabicCatalogParser.formatTimestamp(ytController.windowEndSec)}",
                            style = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Replay segment start button
                        Surface(
                            onClick = { ytController.replayWindow() },
                            shape = CircleShape,
                            color = palette.pillBg
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "Ulangi dari awal segmen",
                                tint = palette.textPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(15.dp)
                            )
                        }

                        // Speed toggle pill (0.75x -> 1.0x -> 1.25x)
                        Surface(
                            onClick = {
                                val nextRate = when {
                                    ytController.playbackRate < 0.9f -> 1.0f
                                    ytController.playbackRate < 1.15f -> 1.25f
                                    else -> 0.75f
                                }
                                ytController.setSpeed(nextRate)
                            },
                            shape = CircleShape,
                            color = if (ytController.playbackRate != 1.0f) palette.emeraldSoft else palette.pillBg
                        ) {
                            Text(
                                text = "${ytController.playbackRate}x",
                                color = if (ytController.playbackRate != 1.0f) palette.emeraldPrimary else palette.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }

                        // Loop segment toggle pill
                        Surface(
                            onClick = { ytController.setLoop(!ytController.loopEnabled) },
                            shape = CircleShape,
                            color = if (ytController.loopEnabled) palette.emeraldSoft else palette.pillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = "Loop segmen",
                                    tint = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Loop",
                                    color = if (ytController.loopEnabled) palette.emeraldPrimary else palette.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Lipat video agar ruang transkrip / PDF jauh lebih lega
                        Surface(
                            onClick = { isPdfFocusMode = true },
                            shape = CircleShape,
                            color = palette.pillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Lipat video",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Lipat",
                                    color = palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Sub-segment quick seek chips
                    if (lesson.segments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item(key = "seg_all") {
                                SegmentChip(
                                    label = "Semua (${lesson.segmentRangeLabel})",
                                    selected = activeSegmentIndex == -1,
                                    onClick = {
                                        activeSegmentIndex = -1
                                        ytController.setSegmentWindow(
                                            lesson.startSeconds,
                                            lesson.endSeconds,
                                            seekNow = true
                                        )
                                    }
                                )
                            }
                            itemsIndexed(lesson.segments) { idx, seg ->
                                SegmentChip(
                                    label = seg.label,
                                    selected = activeSegmentIndex == idx,
                                    onClick = {
                                        activeSegmentIndex = idx
                                        ytController.setSegmentWindow(
                                            seg.startSec,
                                            seg.endSec,
                                            seekNow = true
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Split: Teks Interaktif atau Native High-Clarity PDF Reader
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (useInteractiveMode && lessonContent != null) {
                ArabicInteractiveReader(
                    lesson = lesson,
                    content = lessonContent,
                    currentVideoSeconds = ytController.currentSeconds,
                    selectedSegmentIdx = activeSegmentIndex.coerceAtLeast(0),
                    showHarakat = showHarakat,
                    savedNormalizedKeys = savedNormalizedKeys,
                    isCompleted = isCompleted,
                    onToggleHarakat = onToggleHarakat,
                    onSeekToSegment = { seg, idx ->
                        activeSegmentIndex = idx
                        ytController.setSegmentWindow(seg.startSec, seg.endSec, seekNow = true)
                    },
                    onSaveKamusEntry = onSaveKamusEntry,
                    onSpeakArabic = onSpeakArabic,
                    onToggleCompleted = onToggleCompleted,
                    onShowMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    kamusEntries = kamusEntries,
                    kamusAiState = kamusAiState,
                    onLookupKamusAi = onLookupKamusAi,
                    onClearKamusAiState = onClearKamusAiState,
                    modifier = Modifier.fillMaxSize()
                )
            } else when {
                isLoadingPdf -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = palette.emeraldPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Memuat modul PDF...",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                pdfFile != null -> {
                    PdfDocumentViewer(
                        pdfFile = pdfFile,
                        readingMode = readingMode,
                        modifier = Modifier.fillMaxSize(),
                        bottomActionContent = {
                            Button(
                                onClick = onToggleCompleted,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) palette.statusOkBg else palette.emeraldPrimary,
                                    contentColor = if (isCompleted) palette.statusOkFg else {
                                        if (palette.isDark) Color(0xFF03231D) else Color.White
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCompleted) "Selesai Dipelajari ✓" else "Tandai Modul Selesai ✓",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                            text = pdfErrorMessage ?: "File PDF belum tersedia.",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showManualKamusSheet) {
        KamusEntryEditorSheet(
            initialEntry = KamusEntry(
                id = "",
                arabic = "",
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                segmentLabel = lesson.segmentRangeLabel,
                segmentStartSec = ytController.currentSeconds.takeIf { it > 0 } ?: lesson.startSeconds
            ),
            aiState = kamusAiState,
            onRequestAi = onLookupKamusAi,
            onClearAiState = onClearKamusAiState,
            onDismiss = {
                onClearKamusAiState()
                showManualKamusSheet = false
            },
            onSave = { entry ->
                onSaveKamusEntry(entry)
                onClearKamusAiState()
                showManualKamusSheet = false
                Toast.makeText(context, "Disimpan ke Kamus Saya: ${entry.arabic}", Toast.LENGTH_SHORT).show()
            },
            onSpeakArabic = { txt ->
                onSpeakArabic(txt)?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            }
        )
    }
}

@Composable
private fun SegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val bg by animateColorAsState(
        targetValue = if (selected) palette.emeraldPrimary else palette.pillBg,
        animationSpec = tween(160),
        label = "seg_chip_bg"
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (selected) {
                    if (palette.isDark) Color(0xFF03231D) else Color.White
                } else {
                    palette.textSecondary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}
