package com.jadwalshalat.gt5.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.learning.CourseEpisode
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

    val totalBlocks = course.totalBlockCount
    val doneBlocks = course.allBlocks.count { (_, block) -> completedBlockIds.contains(block.id) }
    val progressFraction = if (totalBlocks > 0) doneBlocks.toFloat() / totalBlocks.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.instructor,
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Course Hero + Quick Action Pills
            item(key = "course_hero") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (course.arabicTitle.isNotBlank()) {
                            Text(
                                text = course.arabicTitle,
                                color = palette.goldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = course.subtitle,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progres Belajar",
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$doneBlocks / $totalBlocks Blok Tema Selesai",
                                color = palette.emeraldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = palette.emeraldPrimary,
                            trackColor = palette.pillBg,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { onOpenFullBookPdf(1) },
                                shape = RoundedCornerShape(12.dp),
                                color = palette.emeraldSoft,
                                border = BorderStroke(1.dp, palette.emeraldPrimary.copy(alpha = 0.35f)),
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
                                        tint = palette.emeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kitab PDF (${course.bookTotalPages} Hal)",
                                        color = palette.emeraldPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                onClick = { showGlossaryBrowser = true },
                                shape = RoundedCornerShape(12.dp),
                                color = palette.goldAccent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, palette.goldAccent.copy(alpha = 0.35f)),
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
                                        tint = palette.goldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kamus (${course.glossary.size} Istilah)",
                                        color = palette.goldAccent,
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

            // Episodes Accordion List
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
        GlossaryTermBottomSheet(
            term = term,
            onDismiss = { selectedTerm = null },
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

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.cardSurface,
        border = BorderStroke(
            1.dp,
            if (isExpanded) palette.emeraldPrimary.copy(alpha = 0.45f) else palette.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Surface(
                onClick = onToggleExpand,
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (doneInEp == totalInEp && totalInEp > 0) palette.emeraldSoft
                                else palette.goldAccent.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${episode.episodeNumber}",
                            color = if (doneInEp == totalInEp && totalInEp > 0) palette.emeraldPrimary else palette.goldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = episode.title,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${episode.durationLabel} • ${episode.blocks.size} Blok Tema • Kitab ${episode.pdfRangeLabel} • $doneInEp/$totalInEp selesai",
                            color = palette.textSecondary,
                            fontSize = 11.sp
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
                        lineHeight = 17.sp
                    )

                    Surface(
                        onClick = onOpenEpisodePdf,
                        shape = RoundedCornerShape(10.dp),
                        color = palette.pillBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                text = "Buka Bab Majlis ${episode.episodeNumber} di Kitab PDF (${episode.pdfRangeLabel})",
                                color = palette.emeraldPrimary,
                                fontSize = 11.sp,
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
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted) palette.highlightSurface else palette.cardSurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (isCompleted) palette.highlightBorder else palette.cardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.emeraldSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.pillBg
                ) {
                    Text(
                        text = "Hal. ${block.pdfPageHint}",
                        color = palette.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = block.title,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (block.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = block.summary,
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val resolvedTerms = remember(block.keyTermIds, course.glossary) {
                block.keyTermIds.mapNotNull { course.glossaryById(it) }
            }
            if (resolvedTerms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(7.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    resolvedTerms.forEach { term ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.goldAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${term.arabic} • ${term.term}",
                                color = palette.goldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Kamus Istilah Fiqih Muamalah",
                color = palette.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk istilah untuk melihat arti, catatan kaidah & contohnya di video",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Cari istilah Arab atau Indonesia...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.goldAccent,
                    unfocusedBorderColor = palette.cardBorder
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { term ->
                    Surface(
                        onClick = { onSelectTerm(term) },
                        shape = RoundedCornerShape(12.dp),
                        color = palette.cardSurfaceElevated,
                        border = BorderStroke(1.dp, palette.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = term.arabic,
                                    color = palette.goldAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Seri ${term.episodeNumber}",
                                    color = palette.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "${term.term} (${term.transliteration})",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = term.meaning,
                                color = palette.textSecondary,
                                fontSize = 11.sp,
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
    onDismiss: () -> Unit,
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
            Text(
                text = term.arabic,
                color = palette.goldAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Column {
                Text(
                    text = term.term,
                    color = palette.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${term.transliteration} • Seri ${term.episodeNumber}",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cardSurfaceElevated,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Arti & Definisi",
                        color = palette.emeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
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
                    shape = RoundedCornerShape(12.dp),
                    color = palette.highlightSurface,
                    border = BorderStroke(1.dp, palette.highlightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Catatan Kaidah Syaikh",
                            color = palette.emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = term.fiqhNote,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (term.exampleInVideo.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.goldAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, palette.goldAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Contoh Kasus di Kajian",
                            color = palette.goldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = term.exampleInVideo,
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (term.blockId.isNotBlank()) {
                Surface(
                    onClick = { onJumpToVideo(term) },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.emeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = palette.cardSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putar Contoh di Video",
                            color = palette.cardSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
