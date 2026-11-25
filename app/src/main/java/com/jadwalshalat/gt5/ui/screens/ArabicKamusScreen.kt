package com.jadwalshalat.gt5.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import com.jadwalshalat.gt5.arabic.ArabicTextUtils
import com.jadwalshalat.gt5.arabic.ClozeQuestion
import com.jadwalshalat.gt5.arabic.KamusCodec
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusEntryKind
import com.jadwalshalat.gt5.arabic.KamusFilter
import com.jadwalshalat.gt5.arabic.KamusSrsScheduler
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

enum class KamusModeTab(val label: String) {
    LIST("Daftar"),
    FLASHCARD("Flashcard"),
    CLOZE_QUIZ("Kuis Kalimat")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArabicKamusScreen(
    entries: List<KamusEntry>,
    selectedFilter: KamusFilter,
    searchQuery: String,
    showHarakat: Boolean,
    onToggleHarakat: () -> Unit,
    onSelectFilter: (KamusFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveEntry: (KamusEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onGradeSrs: (String, Boolean) -> Unit,
    onToggleMastered: (String) -> Unit,
    onImportBackupJson: (String) -> Int,
    onOpenLessonClip: (lessonId: String, startSec: Int) -> Unit,
    onSpeakArabic: (String) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val context = LocalContext.current
    val today = remember(entries.size) { KamusSrsScheduler.todayEpochDay() }
    val dueEntries = remember(entries, today) { entries.filter { KamusSrsScheduler.isDue(it, today) } }
    val masteredCount = remember(entries) { entries.count { it.isMastered } }

    var activeTab by remember { mutableStateOf(KamusModeTab.LIST) }
    var showMenu by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<KamusEntry?>(null) }
    var showEditorSheet by remember { mutableStateOf(false) }

    // SAF launchers untuk Ekspor Anki CSV, Backup JSON, dan Impor JSON
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(KamusCodec.toAnkiCsv(entries).toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "CSV Anki (${entries.size} entri) berhasil disimpan.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal menyimpan CSV: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(KamusCodec.toBackupJson(entries).toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Backup Kamus (${entries.size} entri) tersimpan.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal backup JSON: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.bufferedReader(Charsets.UTF_8).readText()
                }
            }.onSuccess { raw ->
                if (!raw.isNullOrBlank()) {
                    val added = onImportBackupJson(raw)
                    Toast.makeText(
                        context,
                        if (added > 0) "$added entri baru diimpor ke Kamus Saya." else "Semua entri dari file sudah ada.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(context, "Gagal membaca file backup.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 1. Sleek Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = palette.pillBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KOLEKSI & MUROJAAH",
                    color = palette.goldAccent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Kamus Saya",
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            // Harakat ON/OFF Toggle Chip
            Surface(
                onClick = onToggleHarakat,
                shape = CircleShape,
                color = if (showHarakat) palette.emeraldSoft else palette.pillBg
            ) {
                Text(
                    text = if (showHarakat) "تَشْكِيل ✓" else "Gundul",
                    color = if (showHarakat) palette.emeraldPrimary else palette.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Add manual entry circle button
            Surface(
                onClick = {
                    editingEntry = null
                    showEditorSheet = true
                },
                shape = CircleShape,
                color = palette.emeraldPrimary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Catat kata manual",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Export / Import / Share menu
            Box {
                Surface(
                    onClick = { showMenu = true },
                    shape = CircleShape,
                    color = palette.pillBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Ekspor & Backup",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = palette.cardSurfaceElevated
                ) {
                    DropdownMenuItem(
                        text = { Text("Ekspor CSV (Siap Anki)", color = palette.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null, tint = palette.emeraldPrimary) },
                        enabled = entries.isNotEmpty(),
                        onClick = {
                            showMenu = false
                            exportCsvLauncher.launch("kamus_arab_anki.csv")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bagikan Daftar Kosakata", color = palette.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, tint = palette.emeraldPrimary) },
                        enabled = entries.isNotEmpty(),
                        onClick = {
                            showMenu = false
                            val summary = entries.take(60).joinToString("\n") { e ->
                                "• ${e.arabic} = ${e.meaning.ifBlank { e.turatsOrigin }}"
                            }
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Kamus Saya — Jadwal App")
                                putExtra(Intent.EXTRA_TEXT, "Kamus Bahasa Arab (${entries.size} koleksi):\n\n$summary")
                            }
                            context.startActivity(Intent.createChooser(share, "Bagikan Kamus Saya"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Backup File JSON", color = palette.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Book, null, tint = palette.goldAccent) },
                        enabled = entries.isNotEmpty(),
                        onClick = {
                            showMenu = false
                            exportJsonLauncher.launch("kamus_saya_backup.json")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Impor / Pulihkan JSON", color = palette.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.UploadFile, null, tint = palette.textSecondary) },
                        onClick = {
                            showMenu = false
                            importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                        }
                    )
                }
            }
        }

        // 2. Segmented Mode Switcher (Daftar | Flashcard SRS | Kuis Kalimat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.pillBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KamusModeTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val icon = when (tab) {
                    KamusModeTab.LIST -> Icons.Rounded.Book
                    KamusModeTab.FLASHCARD -> Icons.Rounded.Style
                    KamusModeTab.CLOZE_QUIZ -> Icons.Rounded.Quiz
                }
                val labelText = if (tab == KamusModeTab.FLASHCARD && dueEntries.isNotEmpty()) {
                    "${tab.label} (${dueEntries.size})"
                } else {
                    tab.label
                }
                Surface(
                    onClick = { activeTab = tab },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) palette.cardSurfaceElevated else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) palette.emeraldPrimary else palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = labelText,
                            color = if (selected) palette.textPrimary else palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Body by Selected Mode
        when (activeTab) {
            KamusModeTab.LIST -> {
                KamusListMode(
                    entries = entries,
                    dueCount = dueEntries.size,
                    masteredCount = masteredCount,
                    today = today,
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    showHarakat = showHarakat,
                    onSelectFilter = onSelectFilter,
                    onSearchQueryChange = onSearchQueryChange,
                    onStartFlashcard = { activeTab = KamusModeTab.FLASHCARD },
                    onStartClozeQuiz = { activeTab = KamusModeTab.CLOZE_QUIZ },
                    onEditEntry = { entry ->
                        editingEntry = entry
                        showEditorSheet = true
                    },
                    onDeleteEntry = onDeleteEntry,
                    onToggleMastered = onToggleMastered,
                    onOpenLessonClip = onOpenLessonClip,
                    onSpeakArabic = onSpeakArabic
                )
            }
            KamusModeTab.FLASHCARD -> {
                KamusFlashcardMode(
                    entries = entries,
                    dueEntries = dueEntries,
                    showHarakat = showHarakat,
                    onToggleHarakat = onToggleHarakat,
                    onGradeSrs = onGradeSrs,
                    onOpenLessonClip = onOpenLessonClip,
                    onSpeakArabic = onSpeakArabic,
                    onBackToList = { activeTab = KamusModeTab.LIST }
                )
            }
            KamusModeTab.CLOZE_QUIZ -> {
                KamusClozeQuizMode(
                    entries = entries,
                    showHarakat = showHarakat,
                    onToggleHarakat = onToggleHarakat,
                    onGradeSrs = onGradeSrs,
                    onOpenLessonClip = onOpenLessonClip,
                    onSpeakArabic = onSpeakArabic,
                    onBackToList = { activeTab = KamusModeTab.LIST }
                )
            }
        }
    }

    if (showEditorSheet) {
        KamusEntryEditorSheet(
            initialEntry = editingEntry,
            onDismiss = { showEditorSheet = false },
            onSave = { updated ->
                onSaveEntry(updated)
                showEditorSheet = false
            },
            onSpeakArabic = onSpeakArabic
        )
    }
}

@Composable
private fun KamusListMode(
    entries: List<KamusEntry>,
    dueCount: Int,
    masteredCount: Int,
    today: Long,
    selectedFilter: KamusFilter,
    searchQuery: String,
    showHarakat: Boolean,
    onSelectFilter: (KamusFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStartFlashcard: () -> Unit,
    onStartClozeQuiz: () -> Unit,
    onEditEntry: (KamusEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onToggleMastered: (String) -> Unit,
    onOpenLessonClip: (String, Int) -> Unit,
    onSpeakArabic: (String) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val filteredEntries = remember(entries, selectedFilter, searchQuery, today) {
        val q = searchQuery.trim().lowercase()
        val qNorm = ArabicTextUtils.normalizeForLookup(searchQuery)
        entries.filter { e ->
            val matchFilter = when (selectedFilter) {
                KamusFilter.ALL -> true
                KamusFilter.DUE_REVIEW -> KamusSrsScheduler.isDue(e, today)
                KamusFilter.WORDS -> e.kind == KamusEntryKind.WORD
                KamusFilter.PHRASES -> e.kind == KamusEntryKind.PHRASE || e.kind == KamusEntryKind.IDIOM
                KamusFilter.MASTERED -> e.isMastered
            }
            if (!matchFilter) return@filter false
            if (q.isEmpty()) return@filter true
            e.arabic.lowercase().contains(q) ||
                (qNorm.isNotBlank() && e.normalizedKey.contains(qNorm)) ||
                e.meaning.lowercase().contains(q) ||
                e.turatsOrigin.lowercase().contains(q) ||
                e.lessonTitle.lowercase().contains(q) ||
                e.contextSentence.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Summary Card
        item(key = "kamus_hero") {
            KamusSrsHeroCard(
                totalCount = entries.size,
                dueCount = dueCount,
                masteredCount = masteredCount,
                onStartFlashcard = onStartFlashcard,
                onStartClozeQuiz = onStartClozeQuiz
            )
        }

        // Borderless Search Pill
        item(key = "kamus_search") {
            Surface(
                shape = CircleShape,
                color = palette.pillBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari kata Arab, arti Indonesia, atau turats…",
                                color = palette.textMuted,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            cursorBrush = SolidColor(palette.emeraldPrimary),
                            textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Surface(
                            onClick = { onSearchQueryChange("") },
                            shape = CircleShape,
                            color = palette.cardSurface,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, "Hapus", tint = palette.textSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips
        item(key = "kamus_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(KamusFilter.entries) { filter ->
                    val selected = filter == selectedFilter
                    val label = when (filter) {
                        KamusFilter.DUE_REVIEW -> "Siap Review ($dueCount)"
                        KamusFilter.MASTERED -> "Dihafal ($masteredCount)"
                        else -> filter.label
                    }
                    Surface(
                        onClick = { onSelectFilter(filter) },
                        shape = CircleShape,
                        color = if (selected) palette.emeraldPrimary else palette.pillBg
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            item(key = "kamus_empty") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = palette.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Book,
                            contentDescription = null,
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (entries.isEmpty()) "Kamus Saya Masih Kosong" else "Tidak Ada Hasil yang Cocok",
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (entries.isEmpty())
                                "Ketuk kata Arab mana saja di mode Teks Interaktif Ruang Belajar, pakai '+ Pilih Frase', atau klik tombol + di kanan atas."
                            else "Ubah filter atau kata kunci pencarian.",
                            color = palette.textSecondary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                KamusEntryCard(
                    entry = entry,
                    today = today,
                    showHarakat = showHarakat,
                    onEdit = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry.id) },
                    onToggleMastered = { onToggleMastered(entry.id) },
                    onOpenClip = {
                        if (entry.lessonId.isNotBlank()) {
                            onOpenLessonClip(entry.lessonId, entry.segmentStartSec)
                        }
                    },
                    onSpeak = { onSpeakArabic(entry.arabic) }
                )
            }
        }
    }
}

@Composable
private fun KamusSrsHeroCard(
    totalCount: Int,
    dueCount: Int,
    masteredCount: Int,
    onStartFlashcard: () -> Unit,
    onStartClozeQuiz: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0E3D37), Color(0xFF0B292A), Color(0xFF1F2417))
                )
            )
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (dueCount > 0) "SPACED REPETITION • $dueCount SIAP REVIEW HARI INI"
                    else if (totalCount > 0) "SEMUA HAFALAN HARI INI TERJAGA ✓"
                    else "KOLEKSI KOSAKATA & IDIOM PRIBADI",
                    color = Color(0xFFFDE68A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$totalCount Kosakata",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$masteredCount dihafal mantap • ${totalCount - masteredCount} sedang dilatih",
                        color = Color(0xFFD1FAE5).copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onStartFlashcard,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Style, null, tint = Color(0xFF0A292A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (dueCount > 0) "Review ($dueCount)" else "Flashcard",
                                color = Color(0xFF0A292A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = onStartClozeQuiz,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Quiz, null, tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kuis Kalimat",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KamusEntryCard(
    entry: KamusEntry,
    today: Long,
    showHarakat: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleMastered: () -> Unit,
    onOpenClip: () -> Unit,
    onSpeak: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val displayArabic = remember(entry.arabic, showHarakat) {
        if (showHarakat) entry.arabic else ArabicTextUtils.stripHarakat(entry.arabic)
    }
    val displayContext = remember(entry.contextSentence, showHarakat) {
        if (showHarakat) entry.contextSentence else ArabicTextUtils.stripHarakat(entry.contextSentence)
    }
    val statusText = remember(entry.masteryStage, entry.nextReviewEpochDay, today) {
        KamusSrsScheduler.statusBadge(entry, today)
    }
    val isDue = KamusSrsScheduler.isDue(entry, today)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top badge row + Audio + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = palette.pillBg) {
                    Text(
                        text = entry.kind.label,
                        color = palette.textSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = when {
                        entry.isMastered -> palette.statusOkBg
                        isDue -> palette.goldSoft
                        else -> palette.pillBg
                    }
                ) {
                    Text(
                        text = statusText,
                        color = when {
                            entry.isMastered -> palette.statusOkFg
                            isDue -> palette.goldAccent
                            else -> palette.textSecondary
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    onClick = onSpeak,
                    shape = CircleShape,
                    color = palette.emeraldSoft,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Dengar pelafalan Arab",
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    onClick = onEdit,
                    shape = CircleShape,
                    color = palette.pillBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Ubah catatan",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    onClick = onDelete,
                    shape = CircleShape,
                    color = palette.pillBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Hapus dari Kamus",
                            tint = palette.statusWarnFg,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic Word / Phrase + Transliteration
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = displayArabic,
                    color = palette.textPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (entry.transliteration.isNotBlank()) {
                Text(
                    text = entry.transliteration,
                    color = palette.emeraldPrimary,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            if (entry.meaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.meaning,
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp
                )
            }

            // Nuansa Turats -> Modern callout
            if (entry.turatsOrigin.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.goldSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.HistoryEdu,
                            contentDescription = null,
                            tint = palette.goldAccent,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Akar Turats: ${entry.turatsOrigin}",
                            color = palette.textPrimary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Al-Furuq Sinonim note
            if (entry.synonymNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.emeraldSoft.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nuansa Sinonim: ${entry.synonymNote}",
                        color = palette.textSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // Real Context Sentence from Module
            if (displayContext.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.cardSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = displayContext,
                            color = palette.textSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (entry.personalNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${entry.personalNote}",
                    color = palette.textMuted,
                    fontSize = 11.5.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Video Clip Jump Pill + Mastered Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entry.lessonId.isNotBlank()) {
                    val clipTime = ArabicCatalogParser.formatTimestamp(entry.segmentStartSec)
                    Surface(
                        onClick = onOpenClip,
                        shape = CircleShape,
                        color = palette.pillBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = palette.emeraldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Klip $clipTime • ${entry.lessonTitle.ifBlank { "Video Modul" }}",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Surface(
                    onClick = onToggleMastered,
                    shape = CircleShape,
                    color = if (entry.isMastered) palette.statusOkBg else palette.emeraldSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (entry.isMastered) palette.statusOkFg else palette.emeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (entry.isMastered) "Hafal ✓" else "Tandai Hafal",
                            color = if (entry.isMastered) palette.statusOkFg else palette.emeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KamusFlashcardMode(
    entries: List<KamusEntry>,
    dueEntries: List<KamusEntry>,
    showHarakat: Boolean,
    onToggleHarakat: () -> Unit,
    onGradeSrs: (String, Boolean) -> Unit,
    onOpenLessonClip: (String, Int) -> Unit,
    onSpeakArabic: (String) -> Unit,
    onBackToList: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    var practiceAll by remember { mutableStateOf(false) }
    val deck = remember(entries, dueEntries, practiceAll) {
        if (!practiceAll && dueEntries.isNotEmpty()) dueEntries else entries
    }
    var cardIdx by remember(deck.size, practiceAll) { mutableIntStateOf(0) }
    var revealed by remember(cardIdx, practiceAll) { mutableStateOf(false) }
    var sessionCompleted by remember { mutableStateOf(false) }

    if (entries.isEmpty()) {
        KamusDrillEmptyState(
            title = "Belum Ada Kartu Flashcard",
            subtitle = "Tambahkan minimal 1 kosakata dari Teks Interaktif atau lewat tombol + untuk berlatih flashcard SRS.",
            onAction = onBackToList,
            actionLabel = "Kembali ke Daftar"
        )
        return
    }

    if (dueEntries.isEmpty() && !practiceAll) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = palette.statusOkBg, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = palette.statusOkFg, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Target Review Hari Ini Selesai!",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Seluruh ${entries.size} kosakata kamu sudah dijadwalkan dengan interval SRS optimal.",
                color = palette.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { practiceAll = true; cardIdx = 0; revealed = false; sessionCompleted = false },
                colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Latihan Semua (${entries.size} Kartu)")
            }
        }
        return
    }

    if (sessionCompleted || deck.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sesi Flashcard Selesai!", color = palette.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Jadwal murojaah berikutnya telah diperbarui otomatis.", color = palette.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cardIdx = 0; revealed = false; sessionCompleted = false },
                colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary)
            ) {
                Text("Mulai Ulang")
            }
        }
        return
    }

    val current = deck[cardIdx.coerceIn(0, deck.lastIndex)]
    val displayArabic = if (showHarakat) current.arabic else ArabicTextUtils.stripHarakat(current.arabic)
    val displayContext = if (showHarakat) current.contextSentence else ArabicTextUtils.stripHarakat(current.contextSentence)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Deck Progress Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Kartu ${cardIdx + 1} dari ${deck.size}",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tahap ${current.masteryStage}/${KamusSrsScheduler.MASTERED_STAGE}",
                color = palette.goldAccent,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = { ((cardIdx + 1).toFloat() / deck.size.toFloat()).coerceIn(0.05f, 1f) },
            color = palette.emeraldPrimary,
            trackColor = palette.pillBg,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
        )

        // Flip Flashcard Surface
        Surface(
            onClick = { revealed = !revealed },
            shape = RoundedCornerShape(24.dp),
            color = palette.cardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = palette.pillBg) {
                        Text(
                            text = current.kind.label,
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        onClick = { onSpeakArabic(current.arabic) },
                        shape = CircleShape,
                        color = palette.emeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = palette.emeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Dengar", color = palette.emeraldPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = displayArabic,
                        color = palette.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 42.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (current.transliteration.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = current.transliteration,
                        color = palette.emeraldPrimary,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                if (!revealed) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(shape = CircleShape, color = palette.pillBg) {
                        Text(
                            text = "Ketuk kartu untuk melihat arti & akar Turats",
                            color = palette.textMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = revealed) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.hairline)
                        )
                        if (current.meaning.isNotBlank()) {
                            Text(
                                text = current.meaning,
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (current.turatsOrigin.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(14.dp), color = palette.goldSoft, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Akar Turats: ${current.turatsOrigin}",
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (current.synonymNote.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(14.dp), color = palette.emeraldSoft, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Nuansa Sinonim: ${current.synonymNote}",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (displayContext.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(14.dp), color = palette.cardSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Konteks Kalimat:", color = palette.textMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = displayContext,
                                            color = palette.textSecondary,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (current.lessonId.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            onClick = { onOpenLessonClip(current.lessonId, current.segmentStartSec) },
                                            shape = CircleShape,
                                            color = palette.pillBg
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, null, tint = palette.emeraldPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Putar Klip ${ArabicCatalogParser.formatTimestamp(current.segmentStartSec)}",
                                                    color = palette.emeraldPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SRS Grading Action Buttons
        if (revealed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onGradeSrs(current.id, false)
                        if (cardIdx + 1 < deck.size) {
                            cardIdx += 1
                            revealed = false
                        } else {
                            sessionCompleted = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ulangi Lagi", color = palette.statusWarnFg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        onGradeSrs(current.id, true)
                        if (cardIdx + 1 < deck.size) {
                            cardIdx += 1
                            revealed = false
                        } else {
                            sessionCompleted = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Ingat (${KamusSrsScheduler.intervalLabelForNextRecall(current.masteryStage)})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KamusClozeQuizMode(
    entries: List<KamusEntry>,
    showHarakat: Boolean,
    onToggleHarakat: () -> Unit,
    onGradeSrs: (String, Boolean) -> Unit,
    onOpenLessonClip: (String, Int) -> Unit,
    onSpeakArabic: (String) -> Unit,
    onBackToList: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val questions = remember(entries, showHarakat) {
        entries.mapIndexedNotNull { idx, entry ->
            ArabicTextUtils.buildClozeQuestion(
                targetEntry = entry,
                allEntries = entries,
                showHarakat = showHarakat,
                seed = idx
            )
        }
    }

    var qIdx by remember(questions.size) { mutableIntStateOf(0) }
    var selectedOption by remember(qIdx) { mutableStateOf<String?>(null) }
    var score by remember(questions.size) { mutableIntStateOf(0) }

    if (questions.isEmpty()) {
        KamusDrillEmptyState(
            title = "Belum Ada Soal Kalimat Konteks",
            subtitle = "Ketuk kata/frase langsung dari transkrip Teks Interaktif agar kalimat contohnya tersimpan otomatis untuk kuis melengkapi kalimat.",
            onAction = onBackToList,
            actionLabel = "Kembali ke Daftar"
        )
        return
    }

    if (qIdx >= questions.size) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = palette.goldAccent, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Skor Kuis: $score / ${questions.size}",
                color = palette.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Jawaban benar otomatis memajukan jadwal hafalan SRS.",
                color = palette.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { qIdx = 0; score = 0; selectedOption = null },
                colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary)
            ) {
                Text("Ulangi Kuis")
            }
        }
        return
    }

    val q: ClozeQuestion = questions[qIdx]
    val answered = selectedOption != null
    val isCorrect = selectedOption != null &&
        ArabicTextUtils.normalizeForLookup(selectedOption!!) == ArabicTextUtils.normalizeForLookup(q.correctAnswer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Soal ${qIdx + 1} dari ${questions.size}",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Skor: $score",
                color = palette.emeraldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Cloze Sentence Card
        Surface(
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
                    Text(
                        text = "LENGKAPI KALIMAT MODUL",
                        color = palette.goldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        onClick = { onSpeakArabic(q.targetEntry.contextSentence) },
                        shape = CircleShape,
                        color = palette.emeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = palette.emeraldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dengar Kalimat", color = palette.emeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = q.maskedSentence,
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 31.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (q.indonesianHint.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = palette.pillBg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Petunjuk arti target: ${q.indonesianHint}",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 4 Choices
        q.options.forEach { opt ->
            val optNorm = ArabicTextUtils.normalizeForLookup(opt)
            val ansNorm = ArabicTextUtils.normalizeForLookup(q.correctAnswer)
            val isThisSelected = selectedOption == opt
            val isThisCorrect = optNorm == ansNorm

            val bgColor = when {
                !answered -> palette.cardSurface
                isThisCorrect -> palette.statusOkBg
                isThisSelected -> palette.statusWarnBg
                else -> palette.cardSurface
            }
            val fgColor = when {
                !answered -> palette.textPrimary
                isThisCorrect -> palette.statusOkFg
                isThisSelected -> palette.statusWarnFg
                else -> palette.textSecondary
            }

            Surface(
                onClick = {
                    if (!answered) {
                        selectedOption = opt
                        val right = ArabicTextUtils.normalizeForLookup(opt) == ansNorm
                        if (right) score += 1
                        onGradeSrs(q.targetEntry.id, right)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = opt,
                        color = fgColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp, horizontal = 16.dp)
                    )
                }
            }
        }

        if (answered) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (q.targetEntry.lessonId.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onOpenLessonClip(q.targetEntry.lessonId, q.targetEntry.segmentStartSec) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Klip ${ArabicCatalogParser.formatTimestamp(q.targetEntry.segmentStartSec)}", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        qIdx += 1
                        selectedOption = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (qIdx + 1 < questions.size) "Soal Berikutnya" else "Lihat Skor Akhir",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun KamusDrillEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Translate, null, tint = palette.emeraldPrimary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, color = palette.textSecondary, fontSize = 12.5.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary)
        ) {
            Text(actionLabel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KamusEntryEditorSheet(
    initialEntry: KamusEntry?,
    onDismiss: () -> Unit,
    onSave: (KamusEntry) -> Unit,
    onSpeakArabic: (String) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var arabic by remember(initialEntry) { mutableStateOf(initialEntry?.arabic.orEmpty()) }
    var meaning by remember(initialEntry) { mutableStateOf(initialEntry?.meaning.orEmpty()) }
    var transliteration by remember(initialEntry) { mutableStateOf(initialEntry?.transliteration.orEmpty()) }
    var turats by remember(initialEntry) { mutableStateOf(initialEntry?.turatsOrigin.orEmpty()) }
    var synonymNote by remember(initialEntry) { mutableStateOf(initialEntry?.synonymNote.orEmpty()) }
    var personalNote by remember(initialEntry) { mutableStateOf(initialEntry?.personalNote.orEmpty()) }
    var kind by remember(initialEntry) { mutableStateOf(initialEntry?.kind ?: KamusEntryKind.WORD) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.emeraldPrimary,
        unfocusedBorderColor = palette.cardBorder,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        focusedLabelColor = palette.emeraldPrimary,
        unfocusedLabelColor = palette.textSecondary
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initialEntry == null) "Tambah ke Kamus Saya" else "Simpan ke Kamus Saya",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (arabic.isNotBlank()) {
                    Surface(
                        onClick = { onSpeakArabic(arabic) },
                        shape = CircleShape,
                        color = palette.emeraldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = palette.emeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Dengar", color = palette.emeraldPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Kind Pills
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KamusEntryKind.entries.forEach { k ->
                    val sel = kind == k
                    Surface(
                        onClick = { kind = k },
                        shape = CircleShape,
                        color = if (sel) palette.emeraldPrimary else palette.pillBg
                    ) {
                        Text(
                            text = k.label,
                            color = if (sel) Color.White else palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = arabic,
                onValueChange = { arabic = it },
                label = { Text("Kata / Frase Bahasa Arab") },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = meaning,
                onValueChange = { meaning = it },
                label = { Text("Arti Indonesia") },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (turats.isNotBlank() || initialEntry != null) {
                OutlinedTextField(
                    value = turats,
                    onValueChange = { turats = it },
                    label = { Text("Akar Turats -> Modern (Opsional)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (synonymNote.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = palette.emeraldSoft, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nuansa Sinonim: $synonymNote",
                        color = palette.textSecondary,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (initialEntry?.contextSentence?.isNotBlank() == true) {
                Surface(shape = RoundedCornerShape(12.dp), color = palette.cardSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Konteks Segmen ${initialEntry.segmentLabel}",
                            color = palette.textMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = initialEntry.contextSentence,
                                color = palette.textSecondary,
                                fontSize = 13.5.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = personalNote,
                onValueChange = { personalNote = it },
                label = { Text("Catatan Pribadi / Transliterasi (Opsional)") },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val trimmedAr = arabic.trim()
                    if (trimmedAr.isNotEmpty()) {
                        val base = initialEntry ?: KamusEntry(
                            id = KamusCodec.createStableId(trimmedAr, ""),
                            arabic = trimmedAr
                        )
                        onSave(
                            base.copy(
                                arabic = trimmedAr,
                                meaning = meaning.trim(),
                                transliteration = transliteration.trim(),
                                turatsOrigin = turats.trim(),
                                synonymNote = synonymNote.trim(),
                                personalNote = personalNote.trim(),
                                kind = kind
                            )
                        )
                    }
                },
                enabled = arabic.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.emeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan ke Kamus Saya", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
