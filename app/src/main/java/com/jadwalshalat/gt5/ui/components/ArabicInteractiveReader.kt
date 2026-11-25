package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicContentParser
import com.jadwalshalat.gt5.arabic.ArabicLesson
import com.jadwalshalat.gt5.arabic.ArabicLessonContent
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.ArabicTranscriptSegment
import com.jadwalshalat.gt5.arabic.IdiomEntry
import com.jadwalshalat.gt5.arabic.KamusCodec
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind
import com.jadwalshalat.gt5.arabic.SemanticShiftEntry
import com.jadwalshalat.gt5.arabic.SynonymGroupEntry
import com.jadwalshalat.gt5.arabic.ThematicVocabEntry
import com.jadwalshalat.gt5.ui.screens.KamusEntryEditorSheet
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import kotlinx.coroutines.launch

@Composable
fun ArabicInteractiveReader(
    lesson: ArabicLesson,
    content: ArabicLessonContent,
    currentVideoSeconds: Int,
    selectedSegmentIdx: Int,
    showHarakat: Boolean,
    savedNormalizedKeys: Set<String>,
    isCompleted: Boolean,
    onToggleHarakat: () -> Unit,
    onSeekToSegment: (ArabicTranscriptSegment, Int) -> Unit,
    onSaveKamusEntry: (KamusEntry) -> Unit,
    onSpeakArabic: (String) -> String?,
    onToggleCompleted: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var phraseMode by remember(lesson.id) { mutableStateOf(false) }
    var phraseStartSegIdx by remember(lesson.id) { mutableIntStateOf(-1) }
    var phraseStartWordIdx by remember(lesson.id) { mutableIntStateOf(-1) }

    var pendingDraft by remember { mutableStateOf<KamusEntry?>(null) }

    fun openCollectorForPhrase(
        arabicPhrase: String,
        seg: ArabicTranscriptSegment?,
        overrideMeaning: String = "",
        overrideTurats: String = "",
        overrideSyn: String = "",
        overrideKind: KamusEntryKind? = null,
        overrideTranslit: String = ""
    ) {
        val clean = ArabicTextUtils.stripOuterPunctuation(arabicPhrase).ifBlank { arabicPhrase.trim() }
        val match = ArabicContentParser.matchInGlossary(clean, content)
        val segCtx = seg?.arabic.orEmpty()
        pendingDraft = KamusEntry(
            id = KamusCodec.createStableId(clean, lesson.id),
            arabic = clean,
            transliteration = overrideTranslit.ifBlank { match.transliteration },
            meaning = overrideMeaning.ifBlank { match.suggestedMeaning },
            turatsOrigin = overrideTurats.ifBlank { match.turatsOrigin },
            synonymNote = overrideSyn.ifBlank { match.synonymNote },
            kind = overrideKind ?: match.kind,
            lessonId = lesson.id,
            lessonTitle = lesson.title,
            segmentLabel = seg?.label.orEmpty(),
            segmentStartSec = seg?.startSec ?: lesson.startSeconds,
            contextSentence = segCtx
        )
    }

    // Hitung indeks item untuk tombol lompat cepat antar bagian 1..5
    val sec1HeaderIndex = 1
    val sec2HeaderIndex = sec1HeaderIndex + 1 + content.transcriptSegments.size
    val sec3HeaderIndex = sec2HeaderIndex + if (content.semanticShifts.isEmpty()) 0 else (1 + content.semanticShifts.size)
    val sec4HeaderIndex = sec3HeaderIndex + if (content.idioms.isEmpty()) 0 else (1 + content.idioms.size)
    val sec5HeaderIndex = sec4HeaderIndex + if (content.synonymGroups.isEmpty()) 0 else (1 + content.synonymGroups.size)

    // Auto-scroll mengikuti segmen video aktif saat berjalan
    val activeSegIdxByTime = remember(currentVideoSeconds, content) {
        content.findSegmentAt(currentVideoSeconds)
    }
    LaunchedEffect(activeSegIdxByTime) {
        if (content.transcriptSegments.isNotEmpty() && !phraseMode && pendingDraft == null) {
            val targetIdx = sec1HeaderIndex + 1 + activeSegIdxByTime
            val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (visible.isNotEmpty() && targetIdx !in visible && activeSegIdxByTime < content.transcriptSegments.size) {
                if (visible.first() <= sec2HeaderIndex) {
                    runCatching { listState.animateScrollToItem(targetIdx) }
                }
            }
        }
    }

    val openManualDraft = {
        val seg = content.transcriptSegments.getOrNull(selectedSegmentIdx)
        pendingDraft = KamusEntry(
            id = "",
            arabic = "",
            kind = KamusEntryKind.WORD,
            lessonId = lesson.id,
            lessonTitle = lesson.title,
            segmentLabel = seg?.label.orEmpty(),
            segmentStartSec = seg?.startSec ?: lesson.startSeconds,
            contextSentence = seg?.arabic.orEmpty()
        )
    }

    val isScrolledPastTools by remember {
        androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex >= 1 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Scrollable Interactive Reader Body (bebas tumpukan toolbar lengket!)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = if (isScrolledPastTools) 64.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Item 0: Compact scrollable action & section-jump strip (ikut tergulung agar ruang baca 100% lega)
            item(key = "reader_tools") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            onClick = onToggleHarakat,
                            shape = CircleShape,
                            color = if (showHarakat) palette.emeraldSoft else palette.pillBg
                        ) {
                            Text(
                                text = if (showHarakat) "تَشْكِيل ✓" else "Gundul",
                                color = if (showHarakat) palette.emeraldPrimary else palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    item {
                        Surface(
                            onClick = {
                                phraseMode = !phraseMode
                                phraseStartSegIdx = -1
                                phraseStartWordIdx = -1
                            },
                            shape = CircleShape,
                            color = if (phraseMode) palette.goldAccent else palette.goldSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (phraseMode) Icons.Rounded.Close else Icons.Rounded.BookmarkAdd,
                                    contentDescription = null,
                                    tint = if (phraseMode) Color.White else palette.goldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (phraseMode) "Batal Frase" else "+ Frase",
                                    color = if (phraseMode) Color.White else palette.goldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            onClick = openManualDraft,
                            shape = CircleShape,
                            color = palette.pillBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Edit, null, tint = palette.textSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Catat", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (content.semanticShifts.isNotEmpty()) {
                        item {
                            SectionJumpPill("Turats (${content.semanticShifts.size})") {
                                scope.launch { listState.animateScrollToItem(sec2HeaderIndex) }
                            }
                        }
                    }
                    if (content.idioms.isNotEmpty()) {
                        item {
                            SectionJumpPill("Idiom (${content.idioms.size})") {
                                scope.launch { listState.animateScrollToItem(sec3HeaderIndex) }
                            }
                        }
                    }
                    if (content.synonymGroups.isNotEmpty()) {
                        item {
                            SectionJumpPill("Sinonim (${content.synonymGroups.size})") {
                                scope.launch { listState.animateScrollToItem(sec4HeaderIndex) }
                            }
                        }
                    }
                    if (content.thematicVocab.isNotEmpty()) {
                        item {
                            SectionJumpPill("Tematik (${content.thematicVocab.size})") {
                                scope.launch { listState.animateScrollToItem(sec5HeaderIndex) }
                            }
                        }
                    }
                }
            }

            // Item 1: Ringkas Section 1 Header (tip hanya tampil bila kamus masih kosong)
            item(key = "sec1_hdr") {
                SectionHeaderBlock(
                    number = "1",
                    title = "Transkrip Segmen (${content.transcriptSegments.size})",
                    subtitle = if (savedNormalizedKeys.isEmpty())
                        "Ketuk kata Arab mana saja untuk simpan ke Kamus"
                    else
                        "Kata bergaris emas sudah ada di Kamus Saya"
                )
            }

            // Section 1 Transcript Segments
            itemsIndexed(
                items = content.transcriptSegments,
                key = { idx, seg -> "seg_${idx}_${seg.label}" }
            ) { idx, seg ->
                val isActive = idx == activeSegIdxByTime || idx == selectedSegmentIdx
                InteractiveTranscriptSegmentCard(
                    segment = seg,
                    segmentIndex = idx,
                    isActive = isActive,
                    showHarakat = showHarakat,
                    savedNormalizedKeys = savedNormalizedKeys,
                    phraseStartSegIdx = phraseStartSegIdx,
                    phraseStartWordIdx = phraseStartWordIdx,
                    onPlaySegment = { onSeekToSegment(seg, idx) },
                    onSpeakSegment = {
                        onSpeakArabic(seg.arabic)?.let(onShowMessage)
                    },
                    onTapWord = { token, tokens ->
                        if (phraseMode) {
                            if (phraseStartSegIdx != idx || phraseStartWordIdx < 0) {
                                phraseStartSegIdx = idx
                                phraseStartWordIdx = token.index
                            } else {
                                val phrase = ArabicTextUtils.buildPhraseFromRange(tokens, phraseStartWordIdx, token.index)
                                phraseMode = false
                                phraseStartSegIdx = -1
                                phraseStartWordIdx = -1
                                openCollectorForPhrase(
                                    arabicPhrase = phrase,
                                    seg = seg,
                                    overrideKind = if (phrase.contains(' ')) KamusEntryKind.PHRASE else KamusEntryKind.WORD
                                )
                            }
                        } else {
                            openCollectorForPhrase(arabicPhrase = token.cleanArabic, seg = seg)
                        }
                    }
                )
            }

            // Section 2: Semantic Shifts (Turats -> Modern)
            if (content.semanticShifts.isNotEmpty()) {
                item(key = "sec2_hdr") {
                    SectionHeaderBlock(
                        number = "2",
                        title = "Pergeseran Semantik (Turats → Modern)",
                        subtitle = "Akar kata Al-Qur'an / klasik vs konteks wacana kontemporer"
                    )
                }
                itemsIndexed(
                    items = content.semanticShifts,
                    key = { idx, s -> "sem_${idx}_${s.arabic.take(12)}" }
                ) { _, shift ->
                    SemanticShiftCard(
                        shift = shift,
                        showHarakat = showHarakat,
                        isSaved = savedNormalizedKeys.contains(ArabicTextUtils.normalizeForLookup(shift.arabic)),
                        onSpeak = { onSpeakArabic(shift.arabic)?.let(onShowMessage) },
                        onCollect = {
                            openCollectorForPhrase(
                                arabicPhrase = shift.arabic,
                                seg = content.transcriptSegments.firstOrNull { seg ->
                                    ArabicTextUtils.normalizeForLookup(seg.arabic).contains(
                                        ArabicTextUtils.stripCommonPrefixesForMatch(ArabicTextUtils.normalizeForLookup(shift.arabic))
                                    )
                                } ?: content.transcriptSegments.firstOrNull(),
                                overrideMeaning = shift.modernMeaning,
                                overrideTurats = shift.turatsMeaning,
                                overrideTranslit = shift.transliteration,
                                overrideKind = KamusEntryKind.WORD
                            )
                        }
                    )
                }
            }

            // Section 3: Idioms
            if (content.idioms.isNotEmpty()) {
                item(key = "sec3_hdr") {
                    SectionHeaderBlock(
                        number = "3",
                        title = "Ungkapan Idiomatik & Kolokasi",
                        subtitle = "Frase khas penutur asli (B2–C1)"
                    )
                }
                itemsIndexed(
                    items = content.idioms,
                    key = { idx, idm -> "idm_${idx}_${idm.arabic.take(12)}" }
                ) { _, idiom ->
                    IdiomCard(
                        idiom = idiom,
                        showHarakat = showHarakat,
                        isSaved = savedNormalizedKeys.contains(ArabicTextUtils.normalizeForLookup(idiom.arabic)),
                        onSpeak = { onSpeakArabic(idiom.arabic)?.let(onShowMessage) },
                        onCollect = {
                            val combinedMeaning = buildString {
                                if (idiom.idiomaticMeaning.isNotBlank()) append(idiom.idiomaticMeaning)
                                if (idiom.literalMeaning.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Harfiah: ").append(idiom.literalMeaning)
                                }
                            }
                            val fakeSeg = content.transcriptSegments.firstOrNull { seg ->
                                ArabicTextUtils.normalizeForLookup(seg.arabic).contains(
                                    ArabicTextUtils.normalizeForLookup(idiom.arabic).take(8)
                                )
                            } ?: content.transcriptSegments.firstOrNull()
                            pendingDraft = KamusEntry(
                                id = KamusCodec.createStableId(idiom.arabic, lesson.id),
                                arabic = idiom.arabic,
                                transliteration = idiom.transliteration,
                                meaning = combinedMeaning,
                                kind = KamusEntryKind.IDIOM,
                                lessonId = lesson.id,
                                lessonTitle = lesson.title,
                                segmentLabel = fakeSeg?.label.orEmpty(),
                                segmentStartSec = fakeSeg?.startSec ?: lesson.startSeconds,
                                contextSentence = idiom.exampleArabic.ifBlank { fakeSeg?.arabic.orEmpty() }
                            )
                        }
                    )
                }
            }

            // Section 4: Al-Furuq al-Lughawiyyah (Synonyms)
            if (content.synonymGroups.isNotEmpty()) {
                item(key = "sec4_hdr") {
                    SectionHeaderBlock(
                        number = "4",
                        title = "Pembeda Nuansa Sinonim (Al-Furūq)",
                        subtitle = "Ketuk kata untuk simpan beserta catatan pembeda maknanya"
                    )
                }
                itemsIndexed(
                    items = content.synonymGroups,
                    key = { idx, _ -> "syn_$idx" }
                ) { _, group ->
                    SynonymGroupCard(
                        group = group,
                        showHarakat = showHarakat,
                        savedNormalizedKeys = savedNormalizedKeys,
                        onSpeak = { w -> onSpeakArabic(w)?.let(onShowMessage) },
                        onCollectWord = { w ->
                            openCollectorForPhrase(
                                arabicPhrase = w,
                                seg = content.transcriptSegments.firstOrNull(),
                                overrideMeaning = group.distinction,
                                overrideSyn = "${group.words.joinToString(" / ")} — ${group.distinction}",
                                overrideKind = KamusEntryKind.WORD
                            )
                        }
                    )
                }
            }

            // Section 5: Thematic Vocab Cluster
            if (content.thematicVocab.isNotEmpty()) {
                val clusterTitle = content.thematicVocab.firstOrNull()?.clusterTitle.orEmpty()
                item(key = "sec5_hdr") {
                    SectionHeaderBlock(
                        number = "5",
                        title = if (clusterTitle.isNotBlank()) "Klaster Tematik: $clusterTitle" else "Klaster Kosakata Tematik",
                        subtitle = "Kosakata inti topik modul"
                    )
                }
                itemsIndexed(
                    items = content.thematicVocab,
                    key = { idx, t -> "thm_${idx}_${t.arabic.take(10)}" }
                ) { _, item ->
                    ThematicVocabCard(
                        vocab = item,
                        showHarakat = showHarakat,
                        isSaved = savedNormalizedKeys.contains(ArabicTextUtils.normalizeForLookup(item.arabic)),
                        onSpeak = { onSpeakArabic(item.arabic)?.let(onShowMessage) },
                        onCollect = {
                            openCollectorForPhrase(
                                arabicPhrase = item.arabic,
                                seg = content.transcriptSegments.firstOrNull(),
                                overrideMeaning = item.indonesian,
                                overrideKind = if (item.arabic.trim().contains(' ')) KamusEntryKind.PHRASE else KamusEntryKind.WORD
                            )
                        }
                    )
                }
            }

            // Completion Button
            item(key = "complete_footer") {
                Surface(
                    onClick = onToggleCompleted,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCompleted) palette.statusOkBg else palette.emeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (isCompleted) palette.statusOkFg else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompleted) "Modul Sudah Selesai Dipelajari" else "Tandai Modul Ini Selesai",
                            color = if (isCompleted) palette.statusOkFg else Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Banner mengambang hanya muncul saat Mode Pilih Frase aktif
        if (phraseMode) {
            Surface(
                shape = CircleShape,
                color = palette.goldAccent,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (phraseStartWordIdx < 0)
                            "Ketuk KATA AWAL frase"
                        else
                            "Ketuk KATA AKHIR frase",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            phraseMode = false
                            phraseStartSegIdx = -1
                            phraseStartWordIdx = -1
                        },
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

        // Pill cepat mungil saat pengguna sudah scroll ke bawah
        if (isScrolledPastTools && !phraseMode) {
            Surface(
                shape = CircleShape,
                color = palette.cardSurfaceElevated.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleHarakat,
                        shape = CircleShape,
                        color = if (showHarakat) palette.emeraldSoft else palette.pillBg
                    ) {
                        Text(
                            text = if (showHarakat) "تَشْكِيل" else "Gundul",
                            color = if (showHarakat) palette.emeraldPrimary else palette.textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        onClick = {
                            phraseMode = true
                            phraseStartSegIdx = -1
                            phraseStartWordIdx = -1
                        },
                        shape = CircleShape,
                        color = palette.goldSoft
                    ) {
                        Text(
                            text = "+ Frase",
                            color = palette.goldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        onClick = openManualDraft,
                        shape = CircleShape,
                        color = palette.emeraldPrimary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Edit, "Catat", tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }

    val draft = pendingDraft
    if (draft != null) {
        KamusEntryEditorSheet(
            initialEntry = draft,
            onDismiss = { pendingDraft = null },
            onSave = { entry ->
                onSaveKamusEntry(entry)
                pendingDraft = null
                onShowMessage("Disimpan ke Kamus Saya: ${entry.arabic}")
            },
            onSpeakArabic = { txt -> onSpeakArabic(txt)?.let(onShowMessage) }
        )
    }
}

@Composable
private fun SectionJumpPill(label: String, onClick: () -> Unit) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.pillBg
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SectionHeaderBlock(number: String, title: String, subtitle: String) {
    val palette = LocalShalatVibePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = palette.emeraldPrimary,
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = palette.textMuted, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveTranscriptSegmentCard(
    segment: ArabicTranscriptSegment,
    segmentIndex: Int,
    isActive: Boolean,
    showHarakat: Boolean,
    savedNormalizedKeys: Set<String>,
    phraseStartSegIdx: Int,
    phraseStartWordIdx: Int,
    onPlaySegment: () -> Unit,
    onSpeakSegment: () -> Unit,
    onTapWord: (com.jadwalshalat.gt5.arabic.ArabicWordToken, List<com.jadwalshalat.gt5.arabic.ArabicWordToken>) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val tokens = remember(segment.arabic) { ArabicTextUtils.tokenizeSentenceWords(segment.arabic) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) palette.highlightSurface else palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar klip",
                            tint = if (isActive) Color.White else palette.emeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = segment.label,
                            color = if (isActive) Color.White else palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
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
                            contentDescription = "Dengar pengucapan segmen",
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dengar",
                            color = palette.emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kata-kata Arab yang dapat diketuk dalam aliran RTL natural yang rapat & nyaman dibaca
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

                        val chipBg = when {
                            isPhraseStart -> palette.emeraldPrimary
                            isSaved -> palette.goldSoft
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
                                        Modifier.clickable { onTapWord(tok, tokens) }
                                    } else Modifier
                                )
                                .padding(horizontal = if (isSaved || isPhraseStart) 4.dp else 1.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = displayWord,
                                color = chipFg,
                                fontSize = 18.5.sp,
                                fontWeight = if (isSaved || isPhraseStart) FontWeight.Bold else FontWeight.Medium,
                                lineHeight = 27.sp,
                                textDecoration = if (isSaved) TextDecoration.Underline else TextDecoration.None
                            )
                        }
                    }
                }
            }

            if (segment.indonesian.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.hairline)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = segment.indonesian,
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.5.sp
                )
            }
        }
    }
}

@Composable
private fun SemanticShiftCard(
    shift: SemanticShiftEntry,
    showHarakat: Boolean,
    isSaved: Boolean,
    onSpeak: () -> Unit,
    onCollect: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val displayArabic = if (showHarakat) shift.arabic else ArabicTextUtils.stripHarakat(shift.arabic)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayArabic,
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (shift.transliteration.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${shift.transliteration})",
                            color = palette.emeraldPrimary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallSpeakIcon(onSpeak)
                    SaveKamusChip(isSaved = isSaved, onClick = onCollect)
                }
            }

            if (shift.turatsMeaning.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = palette.goldSoft, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Rounded.HistoryEdu, null, tint = palette.goldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Asal Turats: ${shift.turatsMeaning}",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            if (shift.modernMeaning.isNotBlank()) {
                Text(
                    text = "Makna Modern: ${shift.modernMeaning}",
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun IdiomCard(
    idiom: IdiomEntry,
    showHarakat: Boolean,
    isSaved: Boolean,
    onSpeak: () -> Unit,
    onCollect: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val displayArabic = if (showHarakat) idiom.arabic else ArabicTextUtils.stripHarakat(idiom.arabic)
    val displayExample = if (showHarakat) idiom.exampleArabic else ArabicTextUtils.stripHarakat(idiom.exampleArabic)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayArabic,
                    color = palette.textPrimary,
                    fontSize = 18.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallSpeakIcon(onSpeak)
                    SaveKamusChip(isSaved = isSaved, onClick = onCollect)
                }
            }

            if (idiom.idiomaticMeaning.isNotBlank()) {
                Text(
                    text = idiom.idiomaticMeaning,
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
            }
            if (idiom.literalMeaning.isNotBlank()) {
                Text(
                    text = "Harfiah: ${idiom.literalMeaning}",
                    color = palette.textMuted,
                    fontSize = 11.5.sp
                )
            }
            if (displayExample.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = palette.cardSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = displayExample,
                                color = palette.textSecondary,
                                fontSize = 14.5.sp,
                                lineHeight = 23.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (idiom.exampleIndonesian.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = idiom.exampleIndonesian,
                                color = palette.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SynonymGroupCard(
    group: SynonymGroupEntry,
    showHarakat: Boolean,
    savedNormalizedKeys: Set<String>,
    onSpeak: (String) -> Unit,
    onCollectWord: (String) -> Unit
) {
    val palette = LocalShalatVibePalette.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (group.words.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        group.words.forEach { w ->
                            val saved = savedNormalizedKeys.contains(ArabicTextUtils.normalizeForLookup(w))
                            val disp = if (showHarakat) w else ArabicTextUtils.stripHarakat(w)
                            Surface(
                                onClick = { onCollectWord(w) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (saved) palette.goldSoft else palette.emeraldSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = disp,
                                        color = if (saved) palette.goldAccent else palette.emeraldPrimary,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.Add,
                                        contentDescription = "Simpan ke Kamus",
                                        tint = if (saved) palette.goldAccent else palette.emeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (group.distinction.isNotBlank()) {
                Text(
                    text = group.distinction,
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }

            if (group.collocations.isNotEmpty()) {
                group.collocations.forEach { col ->
                    val dispCol = if (showHarakat) col else ArabicTextUtils.stripHarakat(col)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.pillBg)
                            .clickable { onSpeak(col) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(dispCol, color = palette.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = palette.emeraldPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThematicVocabCard(
    vocab: ThematicVocabEntry,
    showHarakat: Boolean,
    isSaved: Boolean,
    onSpeak: () -> Unit,
    onCollect: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val dispAr = if (showHarakat) vocab.arabic else ArabicTextUtils.stripHarakat(vocab.arabic)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dispAr,
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vocab.indonesian,
                    color = palette.textSecondary,
                    fontSize = 12.5.sp
                )
            }
            SmallSpeakIcon(onSpeak)
            SaveKamusChip(isSaved = isSaved, onClick = onCollect)
        }
    }
}

@Composable
private fun SmallSpeakIcon(onClick: () -> Unit) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.emeraldSoft,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Dengar pelafalan Arab",
                tint = palette.emeraldPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SaveKamusChip(isSaved: Boolean, onClick: () -> Unit) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSaved) palette.goldSoft else palette.emeraldPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Rounded.BookmarkAdded else Icons.Rounded.Add,
                contentDescription = null,
                tint = if (isSaved) palette.goldAccent else Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isSaved) "Tersimpan" else "Kamus",
                color = if (isSaved) palette.goldAccent else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
