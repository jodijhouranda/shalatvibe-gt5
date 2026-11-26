package com.jadwalshalat.gt5.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.ui.platform.testTag
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.KamusAiUiState
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.learning.CourseEpisode
import com.jadwalshalat.gt5.learning.CourseKamusBridge
import com.jadwalshalat.gt5.learning.GlossaryTerm
import com.jadwalshalat.gt5.learning.SeriesCourse
import com.jadwalshalat.gt5.learning.TopicBlock
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseOverviewScreen(
    course: SeriesCourse,
    completedBlockIds: Set<String>,
    onBack: () -> Unit,
    onOpenBlock: (episodeId: String, blockId: String, initialTimeSec: Int?) -> Unit,
    onOpenFullBookPdf: (initialPage: Int) -> Unit,
    onToggleBlockCompleted: (blockId: String) -> Unit,
    savedNormalizedKeys: Set<String> = emptySet(),
    kamusEntries: List<KamusEntry> = emptyList(),
    kamusAiState: KamusAiUiState = KamusAiUiState.Idle,
    onLookupKamusAi: (arabic: String, contextSentence: String, lessonTitle: String, forceRefresh: Boolean) -> Unit = { _, _, _, _ -> },
    onClearKamusAiState: () -> Unit = {},
    onSaveKamusEntry: (KamusEntry) -> Unit = {},
    onSpeakArabic: (String) -> String? = { null },
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val expandedEpisodes = remember(course.id) {
        mutableStateMapOf<String, Boolean>().apply {
            course.episodes.firstOrNull()?.let { put(it.id, true) }
        }
    }
    var showGlossaryBrowser by remember { mutableStateOf(false) }
    var selectedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }
    var pendingKamusDraft by remember { mutableStateOf<KamusEntry?>(null) }

    val totalBlocks = course.totalBlockCount
    val doneBlocks = course.allBlocks.count { (_, block) -> completedBlockIds.contains(block.id) }
    val progressFraction = if (totalBlocks > 0) doneBlocks.toFloat() / totalBlocks.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Sleek Borderless Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Kembali ke Pustaka",
                    tint = palette.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.instructor,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Characterful Editorial Course Hero (Zero Borders)
            item(key = "course_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF103F3A),
                                    Color(0xFF0A292A),
                                    Color(0xFF1A2218)
                                )
                            )
                        )
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFF5B041).copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(size.width * 0.88f, size.height * 0.18f),
                                radius = 110.dp.toPx()
                            ),
                            radius = 110.dp.toPx(),
                            center = Offset(size.width * 0.88f, size.height * 0.18f)
                        )
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        if (course.arabicTitle.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = course.arabicTitle,
                                    color = Color(0xFFFCD34D),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = course.subtitle,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.82f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progres Daurah",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$doneBlocks / $totalBlocks Blok Tema",
                                color = Color(0xFF6EE7B7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape),
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { onOpenFullBookPdf(1) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kitab (${course.bookTotalPages} Hal)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = { showGlossaryBrowser = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF5B041).copy(alpha = 0.22f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bookmark,
                                        contentDescription = null,
                                        tint = Color(0xFFFCD34D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kamus (${course.glossary.size})",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Borderless Tonal Episodes List
            items(course.episodes, key = { it.id }) { episode ->
                val isExpanded = expandedEpisodes[episode.id] == true
                EpisodeAccordionCard(
                    episode = episode,
                    course = course,
                    isExpanded = isExpanded,
                    completedBlockIds = completedBlockIds,
                    onToggleExpand = { expandedEpisodes[episode.id] = !isExpanded },
                    onOpenBlock = { block -> onOpenBlock(episode.id, block.id, null) },
                    onOpenEpisodePdf = { onOpenFullBookPdf(episode.pdfStartPage) },
                    onToggleBlockCompleted = onToggleBlockCompleted
                )
            }
        }
    }

    if (showGlossaryBrowser) {
        GlossaryBrowserBottomSheet(
            glossary = course.glossary,
            onSelectTerm = { term ->
                showGlossaryBrowser = false
                selectedTerm = term
            },
            onDismiss = { showGlossaryBrowser = false }
        )
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
                pendingKamusDraft = CourseKamusBridge.buildDraftFromGlossaryTerm(
                    term = targetTerm,
                    course = course,
                    existingEntries = kamusEntries
                )
            },
            onJumpToVideo = { targetTerm ->
                selectedTerm = null
                val found = course.findEpisodeAndBlock(targetTerm.blockId)
                if (found != null) {
                    onOpenBlock(
                        found.first.id,
                        found.second.id,
                        targetTerm.exampleTimeSec.takeIf { it > 0 }
                    )
                }
            }
        )
    }

    pendingKamusDraft?.let { draft ->
        KamusEntryEditorSheet(
            initialEntry = draft,
            aiState = kamusAiState,
            onRequestAi = onLookupKamusAi,
            onClearAiState = onClearKamusAiState,
            onDismiss = {
                onClearKamusAiState()
                pendingKamusDraft = null
            },
            onSave = { entry ->
                onSaveKamusEntry(entry)
                onClearKamusAiState()
                pendingKamusDraft = null
                onShowMessage("Disimpan ke Kamus Saya: ${entry.arabic}")
            },
            onSpeakArabic = { txt -> onSpeakArabic(txt)?.let(onShowMessage) }
        )
    }
}

@Composable
private fun EpisodeAccordionCard(
    episode: CourseEpisode,
    course: SeriesCourse,
    isExpanded: Boolean,
    completedBlockIds: Set<String>,
    onToggleExpand: () -> Unit,
    onOpenBlock: (TopicBlock) -> Unit,
    onOpenEpisodePdf: () -> Unit,
    onToggleBlockCompleted: (String) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val doneInEp = episode.blocks.count { completedBlockIds.contains(it.id) }
    val totalInEp = episode.blocks.size
    val allDone = doneInEp == totalInEp && totalInEp > 0

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Surface(
                onClick = onToggleExpand,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (allDone) palette.emeraldSoft else palette.goldSoft
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${episode.episodeNumber}",
                            color = if (allDone) palette.emeraldPrimary else palette.goldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = episode.title,
                            color = palette.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${episode.durationLabel} • ${episode.blocks.size} Blok • Kitab ${episode.pdfRangeLabel} • $doneInEp/$totalInEp",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = episode.summary,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    Surface(
                        onClick = onOpenEpisodePdf,
                        shape = CircleShape,
                        color = palette.pillBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buka Bab Majlis ${episode.episodeNumber} di Kitab (${episode.pdfRangeLabel})",
                                color = palette.emeraldPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    for (block in episode.blocks) {
                        TopicBlockItemCard(
                            block = block,
                            course = course,
                            isCompleted = completedBlockIds.contains(block.id),
                            onClick = { onOpenBlock(block) },
                            onToggleCompleted = { onToggleBlockCompleted(block.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicBlockItemCard(
    block: TopicBlock,
    course: SeriesCourse,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isCompleted) palette.highlightSurface else palette.cardSurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.emeraldSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = block.timeRangeLabel,
                            color = palette.emeraldPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = palette.pillBg
                ) {
                    Text(
                        text = "Hal. ${block.pdfPageHint}",
                        color = palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onToggleCompleted,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Tandai selesai",
                        tint = if (isCompleted) palette.emeraldPrimary else palette.textMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = block.title,
                color = palette.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            if (block.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = block.summary,
                    color = palette.textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val resolvedTerms = remember(block.keyTermIds, course.glossary) {
                block.keyTermIds.mapNotNull { course.glossaryById(it) }
            }
            if (resolvedTerms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    resolvedTerms.forEach { term ->
                        Surface(
                            shape = CircleShape,
                            color = palette.goldSoft
                        ) {
                            Text(
                                text = "${term.arabic} • ${term.term}",
                                color = palette.goldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryBrowserBottomSheet(
    glossary: List<GlossaryTerm>,
    onSelectTerm: (GlossaryTerm) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, glossary) {
        val q = search.trim().lowercase()
        if (q.isBlank()) glossary
        else glossary.filter {
            it.term.lowercase().contains(q) ||
                it.arabic.contains(q) ||
                it.transliteration.lowercase().contains(q) ||
                it.meaning.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Kamus Istilah Fiqih Muamalah",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk istilah untuk melihat arti, catatan kaidah & contohnya di video",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = CircleShape,
                color = palette.pillBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text(
                                text = "Cari istilah Arab atau Indonesia...",
                                color = palette.textMuted,
                                fontSize = 12.5.sp
                            )
                        }
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            singleLine = true,
                            cursorBrush = SolidColor(palette.goldAccent),
                            textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { term ->
                    Surface(
                        onClick = { onSelectTerm(term) },
                        shape = RoundedCornerShape(16.dp),
                        color = palette.cardSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(13.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = term.arabic,
                                    color = palette.goldAccent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Seri ${term.episodeNumber}",
                                    color = palette.textMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                            Text(
                                text = "${term.term} (${term.transliteration})",
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = term.meaning,
                                color = palette.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryTermBottomSheet(
    term: GlossaryTerm,
    isSavedInKamus: Boolean = false,
    onDismiss: () -> Unit,
    onSaveToKamus: ((GlossaryTerm) -> Unit)? = null,
    onSpeakArabic: ((String) -> Unit)? = null,
    onJumpToVideo: (GlossaryTerm) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = term.arabic,
                    color = palette.goldAccent,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onSpeakArabic != null && term.arabic.isNotBlank()) {
                    Surface(
                        onClick = { onSpeakArabic(term.arabic) },
                        shape = CircleShape,
                        color = palette.emeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Dengar pelafalan",
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dengar",
                                color = palette.emeraldPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Column {
                Text(
                    text = term.term,
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${term.transliteration} • Seri ${term.episodeNumber}",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Arti & Definisi",
                        color = palette.emeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = term.meaning,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            if (term.fiqhNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.highlightSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Catatan Kaidah Syaikh",
                            color = palette.emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = term.fiqhNote,
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (term.exampleInVideo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.goldSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Contoh Kasus di Kajian",
                            color = palette.goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = term.exampleInVideo,
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (onSaveToKamus != null && term.arabic.isNotBlank()) {
                Surface(
                    onClick = { onSaveToKamus(term) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSavedInKamus) palette.goldSoft else palette.emeraldSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_save_to_kamus_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSavedInKamus) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            tint = if (isSavedInKamus) palette.goldAccent else palette.emeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSavedInKamus) "Tersimpan di Kamus Saya (Edit)" else "Simpan ke Kamus Saya",
                            color = if (isSavedInKamus) palette.goldAccent else palette.emeraldPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (term.blockId.isNotBlank()) {
                Surface(
                    onClick = { onJumpToVideo(term) },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.emeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = if (palette.isDark) Color(0xFF03231D) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putar Penjelasan di Video",
                            color = if (palette.isDark) Color(0xFF03231D) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
