package com.jadwalshalat.gt5.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicLesson
import com.jadwalshalat.gt5.arabic.ArabicStudyUiState
import com.jadwalshalat.gt5.learning.GlossaryTerm
import com.jadwalshalat.gt5.learning.LearningHubUiState
import com.jadwalshalat.gt5.learning.LearningSearchHit
import com.jadwalshalat.gt5.learning.LearningTrackEntry
import com.jadwalshalat.gt5.learning.LearningTrackType
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@Composable
fun LearningLibraryScreen(
    hubState: LearningHubUiState,
    arabicState: ArabicStudyUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenTrack: (LearningTrackEntry) -> Unit,
    onOpenArabicLesson: (ArabicLesson) -> Unit,
    onOpenTopicBlock: (courseId: String, episodeId: String, blockId: String, initialTimeSec: Int?) -> Unit,
    onSyncAll: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenArabicKamus: () -> Unit = {}
) {
    val palette = LocalShalatVibePalette.current
    val query = hubState.searchQuery.trim()
    val isSearching = query.isNotBlank()

    val searchHits = remember(query, hubState.tracks, hubState.seriesCourses, arabicState.lessons) {
        if (query.isBlank()) emptyList()
        else buildGlobalSearchHits(query, hubState, arabicState.lessons)
    }

    val continueFiqh = remember(hubState.lastCourseId, hubState.lastBlockId, hubState.seriesCourses) {
        val cid = hubState.lastCourseId
        val bid = hubState.lastBlockId
        if (!cid.isNullOrBlank() && !bid.isNullOrBlank()) {
            hubState.seriesCourses[cid]?.let { course ->
                course.findEpisodeAndBlock(bid)?.let { (ep, block) ->
                    Triple(course, ep, block)
                }
            }
        } else null
    }
    val continueArabic = arabicState.lastOpenedLesson

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Editorial Spacious Header
        item(key = "library_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PUSTAKA ILMU",
                        color = palette.emeraldPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pustaka Belajar",
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kajian berseri, transkrip tersinkron & kitab PDF",
                        color = palette.textSecondary,
                        fontSize = 12.5.sp
                    )
                }

                val syncing = hubState.isSyncing || arabicState.isSyncing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(
                        onClick = onSyncAll,
                        enabled = !syncing,
                        shape = CircleShape,
                        color = palette.pillBg,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (syncing) {
                                CircularProgressIndicator(
                                    color = palette.emeraldPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(17.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = "Sinkronkan semua materi",
                                    tint = palette.emeraldPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onOpenSyncSettings,
                        shape = CircleShape,
                        color = palette.pillBg,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Pengaturan sinkronisasi",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Borderless Pill Search Bar
        item(key = "global_search") {
            TonalSearchPill(
                value = hubState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Cari istilah (Riba, Gharar), topik, atau modul..."
            )
        }

        if (isSearching) {
            item(key = "search_count") {
                Text(
                    text = if (searchHits.isEmpty()) "Tidak ada hasil untuk \"$query\""
                    else "${searchHits.size} hasil ditemukan di seluruh pustaka",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            items(searchHits, key = { it.id }) { hit ->
                SearchHitCard(
                    hit = hit,
                    onClick = {
                        when (hit.kind) {
                            LearningSearchHit.Kind.TRACK -> {
                                hubState.tracks.firstOrNull { it.id == hit.courseId }?.let(onOpenTrack)
                            }
                            LearningSearchHit.Kind.TOPIC_BLOCK -> {
                                onOpenTopicBlock(hit.courseId, hit.episodeId, hit.blockId, null)
                            }
                            LearningSearchHit.Kind.GLOSSARY_TERM -> {
                                val term = hit.glossaryTerm
                                if (term != null && term.blockId.isNotBlank()) {
                                    val ep = hubState.seriesCourses[hit.courseId]
                                        ?.findEpisodeAndBlock(term.blockId)?.first
                                    if (ep != null) {
                                        onOpenTopicBlock(
                                            hit.courseId,
                                            ep.id,
                                            term.blockId,
                                            term.exampleTimeSec.takeIf { it > 0 }
                                        )
                                    }
                                }
                            }
                            LearningSearchHit.Kind.ARABIC_LESSON -> {
                                arabicState.lessons.firstOrNull { it.id == hit.lessonId }
                                    ?.let(onOpenArabicLesson)
                            }
                        }
                    }
                )
            }
        } else {
            // 3. Characterful "Lanjutkan Terakhir" Hero Card
            if (continueFiqh != null || continueArabic != null) {
                item(key = "continue_card") {
                    if (continueFiqh != null &&
                        (hubState.lastTrackId == continueFiqh.first.id || continueArabic == null)
                    ) {
                        val (course, ep, block) = continueFiqh
                        val total = course.totalBlockCount.coerceAtLeast(1)
                        val done = hubState.completedCountForCourse(course.id)
                        ContinueLearningHeroCard(
                            eyebrow = "LANJUTKAN TERAKHIR • SERI ${ep.episodeNumber}",
                            title = block.title,
                            subtitle = "${block.timeRangeLabel} • Kitab Hal. ${block.pdfPageHint}",
                            progressFraction = done.toFloat() / total.toFloat(),
                            onClick = {
                                onOpenTopicBlock(course.id, ep.id, block.id, null)
                            }
                        )
                    } else if (continueArabic != null) {
                        val total = arabicState.lessons.size.coerceAtLeast(1)
                        val done = arabicState.completedCount
                        ContinueLearningHeroCard(
                            eyebrow = "LANJUTKAN TERAKHIR • ${continueArabic.dayLabel.uppercase()}",
                            title = continueArabic.title,
                            subtitle = "${continueArabic.sourceName} • ${continueArabic.segmentRangeLabel}",
                            progressFraction = done.toFloat() / total.toFloat(),
                            onClick = { onOpenArabicLesson(continueArabic) }
                        )
                    }
                }
            }

            // 3b. Kamus Saya & Flashcard SRS Shortcut Card
            item(key = "kamus_saya_shortcut") {
                Surface(
                    onClick = onOpenArabicKamus,
                    shape = RoundedCornerShape(18.dp),
                    color = palette.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.goldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Book,
                                contentDescription = "Kamus Saya",
                                tint = palette.goldAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kamus Saya (${arabicState.kamusEntries.size} kata & frase)",
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    arabicState.dueKamusCount > 0 ->
                                        "${arabicState.dueKamusCount} siap review flashcard SRS hari ini"
                                    arabicState.kamusEntries.isNotEmpty() ->
                                        "Flashcard, Kuis Kalimat Konteks & Ekspor Anki"
                                    else ->
                                        "Ketuk kata saat belajar Bahasa Arab untuk menyimpan"
                                },
                                color = palette.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (arabicState.dueKamusCount > 0) palette.goldAccent else palette.emeraldSoft
                        ) {
                            Text(
                                text = if (arabicState.dueKamusCount > 0) "Review (${arabicState.dueKamusCount})" else "Buka",
                                color = if (arabicState.dueKamusCount > 0) Color.White else palette.emeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4. Section Title
            item(key = "grid_section_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Koleksi Materi",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "${hubState.tracks.size} Bidang • ${arabicState.lastSyncLabel}",
                        color = palette.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // 5. Full-Width Editorial Course Showcase Cards (Zero Borders & Zero Placeholder Boxes)
            items(hubState.tracks, key = { it.id }) { track ->
                val stats = resolveTrackEditorialStats(
                    track = track,
                    hubState = hubState,
                    arabicState = arabicState
                )
                EditorialCourseCard(
                    track = track,
                    arabicSubtitle = stats.arabicCallout,
                    progressText = stats.progressText,
                    progressFraction = stats.fraction,
                    detailText = stats.detailText,
                    onClick = { onOpenTrack(track) }
                )
            }
        }
    }
}

@Composable
private fun TonalSearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        shape = CircleShape,
        color = palette.pillBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = palette.textMuted,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(palette.emeraldPrimary),
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    onClick = { onValueChange("") },
                    shape = CircleShape,
                    color = palette.cardSurface,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus pencarian",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    progressFraction: Float,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF103F3A),
                            Color(0xFF0A292A),
                            Color(0xFF1C2318)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF5B041).copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * 0.90f, size.height * 0.85f),
                        radius = 100.dp.toPx()
                    ),
                    radius = 100.dp.toPx(),
                    center = Offset(size.width * 0.90f, size.height * 0.85f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = eyebrow,
                        color = Color(0xFFFDE68A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.78f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Lanjut putar",
                            tint = Color(0xFF0A292A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0.04f, 1f) },
                    color = Color(0xFFFCD34D),
                    trackColor = Color.White.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun EditorialCourseCard(
    track: LearningTrackEntry,
    arabicSubtitle: String,
    progressText: String,
    progressFraction: Float,
    detailText: String,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val accent = remember(track.accentHex, palette.emeraldPrimary) {
        parseHexColor(track.accentHex, palette.emeraldPrimary)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.badge.isNotBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = if (palette.isDark) 0.14f else 0.11f)
                    ) {
                        Text(
                            text = track.badge.uppercase(),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (arabicSubtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = arabicSubtitle,
                            color = accent,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.title,
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (track.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = track.subtitle,
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressText,
                            color = palette.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = detailText,
                            color = palette.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = accent,
                        trackColor = palette.pillBg,
                        strokeCap = StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.pillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHitCard(
    hit: LearningSearchHit,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val badgeColor = when (hit.kind) {
        LearningSearchHit.Kind.GLOSSARY_TERM -> palette.goldAccent
        LearningSearchHit.Kind.TOPIC_BLOCK -> palette.emeraldPrimary
        LearningSearchHit.Kind.ARABIC_LESSON -> palette.emeraldPrimary
        LearningSearchHit.Kind.TRACK -> palette.textSecondary
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = hit.badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hit.title,
                    color = palette.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hit.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hit.subtitle,
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.pillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private data class TrackEditorialStats(
    val progressText: String,
    val fraction: Float,
    val detailText: String,
    val arabicCallout: String
)

private fun resolveTrackEditorialStats(
    track: LearningTrackEntry,
    hubState: LearningHubUiState,
    arabicState: ArabicStudyUiState
): TrackEditorialStats {
    return when (track.type) {
        LearningTrackType.DAILY_MODULES -> {
            val total = arabicState.lessons.size
            val done = arabicState.completedCount
            val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f
            TrackEditorialStats(
                progressText = "$done / $total modul selesai",
                fraction = fraction,
                detailText = "${arabicState.favoriteIds.size} favorit",
                arabicCallout = "العربية للناطقين بغيرها"
            )
        }
        LearningTrackType.SERIES_COURSE -> {
            val course = hubState.seriesCourses[track.id]
            if (course == null) {
                TrackEditorialStats(
                    progressText = "Siap dipelajari",
                    fraction = 0f,
                    detailText = "Ketuk untuk memuat",
                    arabicCallout = ""
                )
            } else {
                val totalBlocks = course.totalBlockCount
                val doneBlocks = hubState.completedCountForCourse(course.id)
                val fraction = if (totalBlocks > 0) doneBlocks.toFloat() / totalBlocks.toFloat() else 0f
                TrackEditorialStats(
                    progressText = "$doneBlocks / $totalBlocks blok tema",
                    fraction = fraction,
                    detailText = "${course.glossary.size} istilah",
                    arabicCallout = course.arabicTitle
                )
            }
        }
    }
}

private fun buildGlobalSearchHits(
    query: String,
    hubState: LearningHubUiState,
    arabicLessons: List<ArabicLesson>
): List<LearningSearchHit> {
    val q = query.lowercase()
    val hits = ArrayList<LearningSearchHit>()

    for ((courseId, course) in hubState.seriesCourses) {
        for (term: GlossaryTerm in course.glossary) {
            if (term.term.lowercase().contains(q) ||
                term.arabic.contains(q) ||
                term.transliteration.lowercase().contains(q) ||
                term.meaning.lowercase().contains(q) ||
                term.fiqhNote.lowercase().contains(q)
            ) {
                hits += LearningSearchHit(
                    id = "term_${courseId}_${term.id}",
                    title = "${term.term} (${term.arabic})",
                    subtitle = term.meaning,
                    badge = "Istilah • Seri ${term.episodeNumber}",
                    kind = LearningSearchHit.Kind.GLOSSARY_TERM,
                    courseId = courseId,
                    blockId = term.blockId,
                    glossaryTerm = term
                )
            }
        }

        for (ep in course.episodes) {
            for (block in ep.blocks) {
                if (block.title.lowercase().contains(q) ||
                    block.summary.lowercase().contains(q)
                ) {
                    hits += LearningSearchHit(
                        id = "block_${courseId}_${block.id}",
                        title = block.title,
                        subtitle = "${course.title} • Seri ${ep.episodeNumber} • ${block.timeRangeLabel}",
                        badge = "Blok Tema • Hal. ${block.pdfPageHint}",
                        kind = LearningSearchHit.Kind.TOPIC_BLOCK,
                        courseId = courseId,
                        episodeId = ep.id,
                        blockId = block.id
                    )
                }
            }
        }
    }

    for (lesson in arabicLessons) {
        if (lesson.title.lowercase().contains(q) ||
            lesson.subtitle.lowercase().contains(q) ||
            lesson.dayLabel.lowercase().contains(q) ||
            lesson.sourceName.lowercase().contains(q)
        ) {
            hits += LearningSearchHit(
                id = "arabic_${lesson.id}",
                title = lesson.title,
                subtitle = "${lesson.dayLabel} • ${lesson.sourceName}",
                badge = "Bahasa Arab (TAFL)",
                kind = LearningSearchHit.Kind.ARABIC_LESSON,
                lessonId = lesson.id
            )
        }
    }

    return hits.take(30)
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}

@Suppress("unused")
private fun resolveTrackIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "balance" -> Icons.Rounded.Balance
        "school" -> Icons.Rounded.School
        "bookmark" -> Icons.Rounded.Bookmark
        "check" -> Icons.Rounded.CheckCircle
        else -> Icons.AutoMirrored.Rounded.MenuBook
    }
}
