package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.data.PreferencesManager
import com.jadwalshalat.gt5.model.CalculationMethod
import com.jadwalshalat.gt5.model.CityPreset
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.ui.MethodPreviewUi
import com.jadwalshalat.gt5.ui.PrayerRowUi
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import com.jadwalshalat.gt5.ui.theme.ThemeMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerBottomSheet(
    currentCityName: String,
    currentLat: Double,
    currentLon: Double,
    onSelectCity: (CityPreset) -> Unit,
    onUseGps: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allCities = remember { CityPreset.getIndonesianPresets() }
    var query by remember { mutableStateOf("") }

    val filteredCities = remember(query, allCities) {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) allCities
        else allCities.filter {
            it.name.lowercase(Locale.getDefault()).contains(q) ||
                it.region.lowercase(Locale.getDefault()).contains(q) ||
                it.timezoneLabel.lowercase(Locale.getDefault()).contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Lokasi Jadwal Shalat",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "Aktif: %s (%.3f, %.3f)", currentCityName, currentLat, currentLon),
                color = palette.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1-Tap GPS Detection Card
            Surface(
                onClick = {
                    onUseGps()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                color = palette.emeraldSoft,
                border = BorderStroke(1.dp, palette.emeraldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = palette.emeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Deteksi Lokasi GPS Otomatis",
                            color = palette.emeraldPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bekerja offline tanpa Google Play Services",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text("Cari kota atau provinsi (52 kota)...", fontSize = 13.sp, color = palette.textMuted)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textMuted)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = palette.textMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.emeraldPrimary,
                    unfocusedBorderColor = palette.cardBorder,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredCities, key = { it.name }) { city ->
                    val isSelected = city.name.equals(currentCityName, ignoreCase = true)
                    Surface(
                        onClick = {
                            onSelectCity(city)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) palette.highlightSurface else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, palette.highlightBorder) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = city.name,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (city.region.isNotBlank()) {
                                    Text(
                                        text = city.region,
                                        color = palette.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = palette.pillBg
                            ) {
                                Text(
                                    text = city.timezoneLabel,
                                    color = palette.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Terpilih",
                                    tint = palette.emeraldPrimary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodPickerBottomSheet(
    currentMethod: CalculationMethod,
    useIhtiyat: Boolean,
    previews: List<MethodPreviewUi>,
    onSelectMethod: (CalculationMethod) -> Unit,
    onToggleIhtiyat: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
        ) {
            Text(
                text = "Metode Perhitungan Hisab",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pratinjau langsung jam Subuh & Isya hari ini di lokasi Anda",
                color = palette.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                previews.forEach { item ->
                    val selected = item.method == currentMethod
                    Surface(
                        onClick = {
                            onSelectMethod(item.method)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) palette.highlightSurface else palette.background,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) palette.highlightBorder else palette.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.method.label,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Subuh ${item.subuhTime}  •  Isya ${item.isyaTime}",
                                    style = TextStyle(
                                        color = if (selected) palette.emeraldPrimary else palette.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Aktif",
                                    tint = palette.emeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.cardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gunakan Ihtiyat +2 menit",
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Standar kehati-hatian Kemenag RI & MABIMS",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = useIhtiyat,
                    onCheckedChange = onToggleIhtiyat,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.emeraldPrimary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerDetailBottomSheet(
    row: PrayerRowUi,
    globalPreMinutes: Int,
    currentOverride: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectOverride: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val options = listOf(
        PreferencesManager.PRE_MINUTES_FOLLOW_GLOBAL to "Ikuti Global (${globalPreMinutes}m)",
        0 to "Hanya saat Adzan",
        5 to "5 mnt",
        10 to "10 mnt",
        15 to "15 mnt",
        20 to "20 mnt",
        30 to "30 mnt"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengaturan ${row.type.displayName}",
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Jadwal hari ini pukul ${row.formattedTime}",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = row.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.emeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Jeda getar khusus sebelum ${row.type.displayName}",
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Contoh: atur Subuh 20 menit sebelumnya, sementara shalat lain mengikuti pengaturan global.",
                color = palette.textSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (value, label) ->
                    val selected = currentOverride == value
                    Surface(
                        onClick = { onSelectOverride(value) },
                        shape = CircleShape,
                        color = if (selected) palette.emeraldPrimary else palette.pillBg,
                        border = if (selected) null else BorderStroke(1.dp, palette.cardBorder)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) {
                                if (palette.isDark) Color(0xFF03231D) else Color.White
                            } else {
                                palette.textPrimary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerBottomSheet(
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tema Tampilan",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            ThemeMode.entries.forEach { mode ->
                val selected = mode == currentTheme
                Surface(
                    onClick = {
                        onSelectTheme(mode)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) palette.highlightSurface else palette.background,
                    border = BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) palette.highlightBorder else palette.cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = mode.description,
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Aktif",
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(20.dp)
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
fun HuaweiGuideBottomSheet(
    checklistStates: List<Boolean>,
    onToggleStep: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val steps = remember {
        listOf(
            "Buka Huawei Health di HP" to "Masuk ke tab Perangkat lalu pilih HUAWEI WATCH GT 5 Anda.",
            "Buka Menu Notifikasi" to "Pastikan saklar utama Notifikasi dalam keadaan aktif.",
            "Centang ShalatVibe GT5" to "Gulir daftar aplikasi dan aktifkan toggle untuk ShalatVibe GT5.",
            "Cek Opsi Saat Layar Menyala" to "Nonaktifkan \"Bisukan notifikasi saat memakai ponsel\" agar jam tetap bergetar meski HP sedang dipakai.",
            "Uji dengan Tes 5 Detik" to "Tekan tombol Tes Getar 5 Detik di GT 5 Hub lalu segera kunci layar HP untuk verifikasi getaran."
        )
    }

    val doneCount = checklistStates.count { it }
    val progress = doneCount.toFloat() / steps.size.toFloat()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Checklist Setup Huawei Health",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk setiap langkah setelah Anda selesaikan ($doneCount/${steps.size})",
                color = palette.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = palette.emeraldPrimary,
                trackColor = palette.pillBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { idx, (title, desc) ->
                    val checked = checklistStates.getOrElse(idx) { false }
                    Surface(
                        onClick = { onToggleStep(idx) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (checked) palette.statusOkBg else palette.background,
                        border = BorderStroke(
                            1.dp,
                            if (checked) palette.emeraldPrimary.copy(alpha = 0.4f) else palette.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(13.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (checked) palette.emeraldPrimary else palette.textMuted,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. $title",
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCalibrationBottomSheet(
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kalibrasi Sensor Kompas",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Gerakkan HP Anda membentuk pola angka 8 (∞) sebanyak 3 kali untuk menstabilkan magnetometer.",
                color = palette.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Minimalist Figure-8 Infinity Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.background),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .width(180.dp)
                        .height(64.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.5f)
                        cubicTo(w * 0.72f, 0f, w, 0f, w * 0.96f, h * 0.5f)
                        cubicTo(w, h, w * 0.72f, h, w * 0.5f, h * 0.5f)
                        cubicTo(w * 0.28f, 0f, 0f, 0f, w * 0.04f, h * 0.5f)
                        cubicTo(0f, h, w * 0.28f, h, w * 0.5f, h * 0.5f)
                    }
                    drawPath(
                        path = path,
                        color = palette.emeraldPrimary,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = palette.goldAccent,
                        radius = 6.dp.toPx(),
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Jauhkan HP dari casing magnetik, laptop, speaker, atau meja besi saat membidik arah Kiblat.",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArabicSyncSettingsBottomSheet(
    currentUrl: String,
    lastSyncLabel: String,
    onSaveAndSync: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardSurface,
        contentColor = palette.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sinkronisasi Modul (Opsi 1)",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Status terakhir: $lastSyncLabel",
                color = palette.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "URL Manifest catalog.json (GitHub Raw atau link Google Drive publik):",
                color = palette.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.emeraldPrimary,
                    unfocusedBorderColor = palette.cardBorder,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tips: Saat ada PDF harian baru, tambahkan file PDF ke folder lessons/pdfs/ dan entri JSON ke lessons/catalog.json di GitHub (atau file catalog.json di Google Drive yang di-share Viewer), lalu tekan tombol Sync (↻) di tab Belajar — tanpa perlu build ulang APK.",
                color = palette.textSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = {
                        onResetDefault()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.pillBg,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset Default",
                            color = palette.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    onClick = {
                        onSaveAndSync(urlInput)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.emeraldPrimary,
                    modifier = Modifier.weight(1.3f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Simpan & Sync",
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

