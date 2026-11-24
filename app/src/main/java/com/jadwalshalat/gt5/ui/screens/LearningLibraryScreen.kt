package com.jadwalshalat.gt5.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Balance
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val query = hubState.searchQuery.trim()
    val isSearching = query.isNotBlank()

    val searchHits = remember(query, hubState.tracks, hubState.seriesCourses, arabicState.lessons) {
        if (query.isBlank()) emptyList()
        else buildGlobalSearchHits(query, hubState, arabicState.lessons)
    }

    // Resolve Continue Learning Card
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

    val trackPairs = remember(hubState.tracks) {
        hubState.tracks.chunked(2)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item(key = "library_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pustaka Belajar",
                        color = palette.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${hubState.tracks.size} Bidang Ilmu • Video, Teks & Kitab PDF",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }

                val syncing = hubState.isSyncing || arabicState.isSyncing
                Surface(
                    onClick = onSyncAll,
                    enabled = !syncing,
                    shape = CircleShape,
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder),
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

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onOpenSyncSettings,
                    shape = CircleShape,
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder),
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

        // Global Search Bar
        item(key = "global_search") {
            OutlinedTextField(
                value = hubState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text(
                        text = "Cari istilah (Riba, Gharar, Mudharabah), topik, atau modul...",
                        color = palette.textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus pencarian",
                                tint = palette.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.cardSurface,
                    unfocusedContainerColor = palette.cardSurface,
                    focusedBorderColor = palette.emeraldPrimary,
                    unfocusedBorderColor = palette.cardBorder,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )
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
            // Continue Learning Hero Card
            if (continueFiqh != null || continueArabic != null) {
                item(key = "continue_card") {
                    if (continueFiqh != null &&
                        (hubState.lastTrackId == continueFiqh.first.id || continueArabic == null)
                    ) {
                        val (course, ep, block) = continueFiqh
                        ContinueLearningBanner(
                            badge = "${course.title} • Seri ${ep.episodeNumber}",
                            title = block.title,
                            subtitle = "${block.timeRangeLabel} • Hal. Kitab ${block.pdfPageHint}",
                            accentColor = palette.goldAccent,
                            onClick = {
                                onOpenTopicBlock(course.id, ep.id, block.id, null)
                            }
                        )
                    } else if (continueArabic != null) {
                        ContinueLearningBanner(
                            badge = "Bahasa Arab • ${continueArabic.dayLabel}",
                            title = continueArabic.title,
                            subtitle = "${continueArabic.sourceName} • ${continueArabic.segmentRangeLabel}",
                            accentColor = palette.emeraldPrimary,
                            onClick = { onOpenArabicLesson(continueArabic) }
                        )
                    }
                }
            }

            // Section Title
            item(key = "grid_section_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Koleksi Materi",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sync terakhir: ${arabicState.lastSyncLabel}",
                        color = palette.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // 2-Column Scalable Grid
            items(trackPairs, key = { pair -> pair.joinToString("_") { it.id } }) { rowTracks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (track in rowTracks) {
                        val (progressText, fraction, detailText) = resolveTrackStats(
                            track = track,
                            hubState = hubState,
                            arabicState = arabicState
                        )
                        CourseGridCard(
                            track = track,
                            progressText = progressText,
                            progressFraction = fraction,
                            detailText = detailText,
                            onClick = { onOpenTrack(track) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowTracks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Future-proof extensibility hint
            item(key = "extensibility_footer") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.emeraldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pustaka Siap Dikembangkan",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Materi baru (Hadis, Aqidah, Tafsir) otomatis muncul sebagai kartu baru cukup lewat JSON manifest + Sync ↻.",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningBanner(
    badge: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.highlightSurface,
        border = BorderStroke(1.dp, palette.highlightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LANJUTKAN TERAKHIR • $badge",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CourseGridCard(
    track: LearningTrackEntry,
    progressText: String,
    progressFraction: Float,
    detailText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val accent = remember(track.accentHex, palette.emeraldPrimary) {
        parseHexColor(track.accentHex, palette.emeraldPrimary)
    }
    val icon = remember(track.iconName) {
        resolveTrackIcon(track.iconName)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (track.badge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = track.badge,
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = palette.pillBg,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = progressText,
                color = palette.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detailText,
                color = palette.textMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        shape = RoundedCornerShape(14.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = hit.badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hit.title,
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hit.subtitle.isNotBlank()) {
                    Text(
                        text = hit.subtitle,
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun resolveTrackStats(
    track: LearningTrackEntry,
    hubState: LearningHubUiState,
    arabicState: ArabicStudyUiState
): Triple<String, Float, String> {
    return when (track.type) {
        LearningTrackType.DAILY_MODULES -> {
            val total = arabicState.lessons.size
            val done = arabicState.completedCount
            val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f
            Triple("$done/$total modul", fraction, "${arabicState.favoriteIds.size} favorit")
        }
        LearningTrackType.SERIES_COURSE -> {
            val course = hubState.seriesCourses[track.id]
            if (course == null) {
                Triple("Tersedia", 0f, "Ketuk untuk memuat")
            } else {
                val totalBlocks = course.totalBlockCount
                val doneBlocks = hubState.completedCountForCourse(course.id)
                val fraction = if (totalBlocks > 0) doneBlocks.toFloat() / totalBlocks.toFloat() else 0f
                Triple(
                    "$doneBlocks/$totalBlocks blok tema",
                    fraction,
                    "${course.episodes.size} seri • ${course.glossary.size} istilah"
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

private fun resolveTrackIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "balance" -> Icons.Rounded.Balance
        "school" -> Icons.Rounded.School
        "bookmark" -> Icons.Rounded.Bookmark
        "check" -> Icons.Rounded.CheckCircle
        else -> Icons.AutoMirrored.Rounded.MenuBook
    }
}
