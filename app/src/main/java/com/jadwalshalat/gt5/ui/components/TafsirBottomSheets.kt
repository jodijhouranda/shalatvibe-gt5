package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.tafsir.QuranSurahMeta
import com.jadwalshalat.gt5.tafsir.TafsirBook
import com.jadwalshalat.gt5.tafsir.TafsirBookmark
import com.jadwalshalat.gt5.tafsir.TafsirCatalog
import com.jadwalshalat.gt5.tafsir.TafsirCategory
import com.jadwalshalat.gt5.tafsir.TafsirFontSize
import com.jadwalshalat.gt5.tafsir.TafsirPaperTheme
import com.jadwalshalat.gt5.tafsir.TafsirTextProcessor
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBookPickerSheet(
    activeBook: TafsirBook,
    onSelectBook: (TafsirBook) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val groupedBooks = remember(query) {
        val q = query.trim().lowercase()
        TafsirCategory.entries.mapNotNull { category ->
            val matched = TafsirCatalog.books.filter { book ->
                book.category == category && (
                    q.isBlank() ||
                        book.nameLatin.lowercase().contains(q) ||
                        book.nameArabic.contains(q) ||
                        book.authorArabic.contains(q) ||
                        book.id.lowercase().contains(q) ||
                        category.titleId.lowercase().contains(q) ||
                        category.titleArabic.contains(q)
                    )
            }
            if (matched.isEmpty()) null else category to matched
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
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Pilih Kitab Tafsir (22 Kitab)",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mode 1 kitab aktif sekali jalan — ringan & fokus membaca",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            SheetSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari nama kitab (Latin / Arab, mis. Ibnu Katsir, الطبري)...",
                testTag = "tafsir_book_search_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (groupedBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada kitab yang cocok dengan \"$query\"",
                        color = palette.textMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedBooks.forEach { (category, books) ->
                        item(key = "cat_${category.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.titleId.uppercase(),
                                    color = palette.emeraldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = category.titleArabic,
                                        color = palette.textMuted,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        items(books, key = { it.id }) { book ->
                            val isSelected = book.id == activeBook.id
                            Surface(
                                onClick = {
                                    onSelectBook(book)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) palette.highlightSurface else palette.cardSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, palette.emeraldPrimary, RoundedCornerShape(16.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("tafsir_book_item_${book.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = book.nameLatin,
                                                color = if (isSelected) palette.emeraldPrimary else palette.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                Text(
                                                    text = book.nameArabic,
                                                    color = if (isSelected) palette.emeraldPrimary else palette.textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (book.isScannedOnly) {
                                                    MiniTag("Scan Browser", palette.goldSoft, palette.goldAccent)
                                                } else if (book.cdnSlug == null) {
                                                    MiniTag("tafsir.app", palette.emeraldSoft, palette.emeraldPrimary)
                                                }
                                                if (book.isSelective) {
                                                    MiniTag("Selektif", palette.pillBg, palette.textSecondary)
                                                }
                                            }
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                Text(
                                                    text = "${book.authorArabic} • ${book.volumeNoteArabic}",
                                                    color = palette.textSecondary,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(palette.emeraldPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Aktif",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirSurahPickerSheet(
    activeSurah: Int,
    onSelectSurah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filteredSurahs = remember(query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            TafsirCatalog.surahs
        } else {
            TafsirCatalog.surahs.filter { s ->
                s.number.toString() == q ||
                    s.nameLatin.lowercase().contains(q) ||
                    s.nameLatin.lowercase().replace("-", "").replace("'", "").contains(q.replace("-", "").replace("'", "")) ||
                    s.nameArabic.contains(q)
            }
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
                .fillMaxHeight(0.84f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Pilih Surah (1–114)",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            SheetSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "Cari nomor, nama Latin, atau Arab (mis. 18, Kahfi, الكهف)...",
                testTag = "tafsir_surah_search_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredSurahs, key = { it.number }) { surah ->
                    val isSelected = surah.number == activeSurah
                    Surface(
                        onClick = {
                            onSelectSurah(surah.number)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) palette.highlightSurface else palette.cardSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tafsir_surah_item_${surah.number}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) palette.emeraldPrimary else palette.pillBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = surah.number.toString(),
                                    color = if (isSelected) Color.White else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = surah.nameLatin,
                                    color = if (isSelected) palette.emeraldPrimary else palette.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${surah.revelationType} • ${surah.ayahCount} ayat",
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = surah.nameArabic,
                                    color = if (isSelected) palette.emeraldPrimary else palette.textPrimary,
                                    fontSize = 18.sp,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirAyahPickerSheet(
    surah: QuranSurahMeta,
    activeAyah: Int,
    onSelectAyah: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var jumpInput by remember { mutableStateOf("") }

    val ayahNumbers = remember(surah.ayahCount, jumpInput) {
        val q = jumpInput.trim()
        val all = (1..surah.ayahCount).toList()
        if (q.isBlank()) all else all.filter { it.toString().contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${surah.number}. ${surah.nameLatin}",
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih ayat (1–${surah.ayahCount})",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = surah.nameArabic,
                        color = palette.emeraldPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (surah.ayahCount > 20) {
                Spacer(modifier = Modifier.height(10.dp))
                SheetSearchBar(
                    value = jumpInput,
                    onValueChange = { next -> jumpInput = next.filter { it.isDigit() } },
                    placeholder = "Ketik nomor ayat (mis. 255)...",
                    keyboardType = KeyboardType.Number,
                    testTag = "tafsir_ayah_jump_input"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 58.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ayahNumbers, key = { it }) { num ->
                    val isSelected = num == activeAyah
                    Surface(
                        onClick = {
                            onSelectAyah(num)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) palette.emeraldPrimary else palette.cardSurfaceElevated,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = TafsirTextProcessor.toArabicDigits(num),
                                color = if (isSelected) Color.White else palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = num.toString(),
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else palette.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TafsirDisplayAndBookmarksSheet(
    showHarakat: Boolean,
    fontSize: TafsirFontSize,
    paperTheme: TafsirPaperTheme,
    isAyahCollapsed: Boolean,
    bookmarks: List<TafsirBookmark>,
    onSetShowHarakat: (Boolean) -> Unit,
    onSetFontSize: (TafsirFontSize) -> Unit,
    onSetPaperTheme: (TafsirPaperTheme) -> Unit,
    onToggleAyahCollapsed: () -> Unit,
    onOpenBookmark: (TafsirBookmark) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "disp_header") {
                Column {
                    Text(
                        text = "Tampilan Baca & Bookmark",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preferensi tersimpan lokal di perangkat Anda",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Harakat vs Gundul
            item(key = "disp_harakat") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gaya Tulisan Arab",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectableChoiceChip(
                            title = "تَشْكِيل (Berharakat)",
                            selected = showHarakat,
                            onClick = { onSetShowHarakat(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tafsir_mode_harakat")
                        )
                        SelectableChoiceChip(
                            title = "Gundul (بسم الله)",
                            selected = !showHarakat,
                            onClick = { onSetShowHarakat(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tafsir_mode_gundul")
                        )
                    }
                }
            }

            // 2. Ukuran Huruf Proporsional
            item(key = "disp_font_size") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ukuran Huruf Kitab",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${fontSize.bodySp.toInt()} sp • spasi ~1.9x",
                            color = palette.textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TafsirFontSize.entries.forEach { preset ->
                            SelectableChoiceChip(
                                title = preset.label,
                                selected = preset == fontSize,
                                onClick = { onSetFontSize(preset) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tafsir_font_${preset.name.lowercase()}")
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            val sample = TafsirTextProcessor.formatArabicForDisplay(
                                "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ ﴿١﴾",
                                showHarakat
                            )
                            Text(
                                text = sample,
                                color = palette.textPrimary,
                                fontSize = fontSize.bodySp.sp,
                                lineHeight = (fontSize.bodySp * fontSize.lineHeightMultiplier).sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // 3. Warna Kertas Baca
            item(key = "disp_paper") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Warna Kertas Baca",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TafsirPaperTheme.entries.forEach { theme ->
                            val isSelected = theme == paperTheme
                            val swatchBg = when (theme) {
                                TafsirPaperTheme.FOLLOW_THEME -> palette.background
                                TafsirPaperTheme.SEPIA -> Color(0xFFF7F1E3)
                                TafsirPaperTheme.DARK -> Color(0xFF0B1012)
                            }
                            val swatchInk = when (theme) {
                                TafsirPaperTheme.FOLLOW_THEME -> palette.textPrimary
                                TafsirPaperTheme.SEPIA -> Color(0xFF2B2118)
                                TafsirPaperTheme.DARK -> Color(0xFFECEFE9)
                            }
                            Surface(
                                onClick = { onSetPaperTheme(theme) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) palette.highlightSurface else palette.cardSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.2.dp, palette.emeraldPrimary, RoundedCornerShape(14.dp))
                                        } else Modifier
                                    )
                                    .testTag("tafsir_paper_${theme.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(swatchBg)
                                            .border(1.dp, palette.cardBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ع",
                                            color = swatchInk,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = theme.label,
                                        color = if (isSelected) palette.emeraldPrimary else palette.textPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Mode Fokus Tafsir
            item(key = "disp_focus") {
                Surface(
                    onClick = onToggleAyahCollapsed,
                    shape = RoundedCornerShape(14.dp),
                    color = palette.cardSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAyahCollapsed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Fokus Tafsir",
                                color = palette.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lipat kartu ayat agar teks tafsir memenuhi layar",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = isAyahCollapsed,
                            onCheckedChange = { onToggleAyahCollapsed() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = palette.emeraldPrimary
                            )
                        )
                    }
                }
            }

            // 5. Bookmark Tersimpan
            item(key = "disp_bm_divider") {
                HorizontalDivider(color = palette.hairline)
            }

            item(key = "disp_bm_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bookmark Tafsir (${bookmarks.size})",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ketuk untuk lompat",
                        color = palette.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            if (bookmarks.isEmpty()) {
                item(key = "disp_bm_empty") {
                    Text(
                        text = "Belum ada bookmark. Ketuk ikon pita di bilah atas saat membaca ayat untuk menandai.",
                        color = palette.textMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                items(bookmarks, key = { it.id }) { bm ->
                    Surface(
                        onClick = {
                            onOpenBookmark(bm)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = palette.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${bm.surahNameLatin} : ${bm.ayah} • ${bm.bookNameLatin}",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (bm.ayahPreview.isNotBlank()) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = TafsirTextProcessor.formatArabicForDisplay(bm.ayahPreview, showHarakat),
                                            color = palette.textSecondary,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onRemoveBookmark(bm.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Hapus bookmark",
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text
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
                .padding(horizontal = 14.dp, vertical = 11.dp),
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
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    cursorBrush = SolidColor(palette.emeraldPrimary),
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.5.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }
            if (value.isNotEmpty()) {
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
private fun SelectableChoiceChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.emeraldPrimary else palette.cardSurfaceElevated,
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = title,
                color = if (selected) Color.White else palette.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniTag(
    label: String,
    bg: Color,
    fg: Color
) {
    Surface(shape = CircleShape, color = bg) {
        Text(
            text = label,
            color = fg,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
