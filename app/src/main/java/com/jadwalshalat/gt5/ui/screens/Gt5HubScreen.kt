package com.jadwalshalat.gt5.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.ui.ShalatVibeUiState
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@Composable
fun Gt5HubScreen(
    state: ShalatVibeUiState,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onToggleMasterReminder: (Boolean) -> Unit,
    onSelectGlobalPreMinutes: (Int) -> Unit,
    onToggleNotifyOnTime: (Boolean) -> Unit,
    onToggleDoublePing: (Boolean) -> Unit,
    onToggleIhtiyat: (Boolean) -> Unit,
    onToggleAlarmClockMode: (Boolean) -> Unit,
    onToggleCompactGt5: (Boolean) -> Unit,
    onTestImmediate: () -> Unit,
    onTestDelayed5s: () -> Unit,
    onOpenHuaweiGuide: () -> Unit,
    onOpenThemePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hub_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GT 5 Hub",
                        color = palette.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pusat sinkronisasi & getar Huawei Watch GT 5",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (state.allSystemsReady) palette.statusOkBg else palette.statusWarnBg
                ) {
                    Text(
                        text = if (state.allSystemsReady) "Siap 24/7" else "Perlu Cek",
                        color = if (state.allSystemsReady) palette.statusOkFg else palette.statusWarnFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 1. Live Circular Huawei Watch GT 5 Notification Preview
        item(key = "watch_preview") {
            WatchGt5LivePreviewCard(state = state)
        }

        // 2. 4-Point Readiness Health Check Grid
        item(key = "health_check") {
            ReadinessHealthCheckCard(
                state = state,
                onNotificationClick = onRequestNotificationPermission,
                onExactAlarmClick = onOpenExactAlarmSettings,
                onBatteryClick = onRequestIgnoreBattery,
                onAlarmClockClick = { onToggleAlarmClockMode(!state.useAlarmClockMode) }
            )
        }

        // 3. Test Vibe Lab Card
        item(key = "test_vibe") {
            TestVibeLabCard(
                delayedCountdown = state.delayedTestCountdown,
                onTestDelayed5s = onTestDelayed5s,
                onTestImmediate = onTestImmediate
            )
        }

        // 4. Reminder & Vibration Settings Card
        item(key = "reminder_settings") {
            ReminderSettingsCard(
                state = state,
                onToggleMasterReminder = onToggleMasterReminder,
                onSelectGlobalPreMinutes = onSelectGlobalPreMinutes,
                onToggleNotifyOnTime = onToggleNotifyOnTime,
                onToggleDoublePing = onToggleDoublePing,
                onToggleIhtiyat = onToggleIhtiyat,
                onToggleAlarmClockMode = onToggleAlarmClockMode,
                onToggleCompactGt5 = onToggleCompactGt5
            )
        }

        // 5. Huawei Health Guide Checklist & Theme shortcuts
        item(key = "guide_and_theme") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenHuaweiGuide)
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(palette.emeraldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = null,
                                tint = palette.emeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Panduan Setup Huawei Health",
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Checklist interaktif (${state.completedHuaweiSteps}/5 langkah selesai)",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = palette.textMuted
                        )
                    }

                    HorizontalDivider(
                        color = palette.cardBorder,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenThemePicker)
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(palette.pillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tema Tampilan",
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = state.themeMode.label,
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = palette.textMuted
                        )
                    }
                }
            }
        }

        // 6. Last Alarm Log Footer Card
        item(key = "last_log") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LOG ALARM TERAKHIR",
                            color = palette.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = state.lastAlarmLog,
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchGt5LivePreviewCard(state: ShalatVibeUiState) {
    val palette = LocalShalatVibePalette.current
    val previewTitle = if (state.compactGt5Format) {
        "${state.globalPreReminderMinutes} mnt • ${state.nextPrayer.displayName}"
    } else {
        "${state.globalPreReminderMinutes} Menit Lagi ${state.nextPrayer.displayName}"
    }
    val previewBody = if (state.compactGt5Format) {
        "Pukul ${state.nextPrayerTimeFormatted} • ${state.cityName}"
    } else {
        "Jadwal ${state.nextPrayer.displayName} pukul ${state.nextPrayerTimeFormatted} • ${state.cityName}"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PRATINJAU NOTIFIKASI DI LAYAR GT 5",
                color = palette.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.size(182.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val bezelR = size.minDimension * 0.44f

                    // Top & bottom strap hints
                    val strapW = bezelR * 0.92f
                    val strapH = 16.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - strapW / 2f, cy - bezelR - strapH + 6.dp.toPx()),
                        size = Size(strapW, strapH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - strapW / 2f, cy + bezelR - 6.dp.toPx()),
                        size = Size(strapW, strapH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )

                    // Rotating Crown & Side Button (Huawei Watch GT 5 silhouette)
                    drawRoundRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(cx + bezelR - 2.dp.toPx(), cy - 26.dp.toPx()),
                        size = Size(8.dp.toPx(), 18.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(cx + bezelR - 2.dp.toPx(), cy + 10.dp.toPx()),
                        size = Size(6.dp.toPx(), 16.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )

                    // Outer metallic watch case
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF334155))
                        ),
                        radius = bezelR + 3.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // AMOLED Watch Screen
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0E2A26), Color(0xFF050809)),
                            center = Offset(cx, cy - 18.dp.toPx()),
                            radius = bezelR
                        ),
                        radius = bezelR - 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // Glowing emerald top arc on AMOLED face
                    val arcInset = 11.dp.toPx()
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF34D399))
                        ),
                        startAngle = 215f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(cx - bezelR + arcInset, cy - bezelR + arcInset),
                        size = Size((bezelR - arcInset) * 2f, (bezelR - arcInset) * 2f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Live notification content inside watch face
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = previewTitle,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previewBody,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (state.compactGt5Format) {
                    "Mode ringkas aktif • terbaca utuh tanpa terpotong di layar bulat"
                } else {
                    "Mode teks lengkap aktif"
                },
                color = palette.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReadinessHealthCheckCard(
    state: ShalatVibeUiState,
    onNotificationClick: () -> Unit,
    onExactAlarmClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onAlarmClockClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Status Kesiapan Sistem",
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pastikan keempat indikator hijau agar GT 5 tidak pernah telat bergetar",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HealthCheckTile(
                    title = "Notifikasi",
                    isReady = state.hasNotificationPermission,
                    icon = Icons.Rounded.Notifications,
                    onClick = onNotificationClick,
                    modifier = Modifier.weight(1f)
                )
                HealthCheckTile(
                    title = "Alarm Presisi",
                    isReady = state.hasExactAlarmPermission,
                    icon = Icons.Rounded.Alarm,
                    onClick = onExactAlarmClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HealthCheckTile(
                    title = "Baterai Bebas",
                    isReady = state.isIgnoringBatteryOptimizations,
                    icon = Icons.Rounded.BatteryChargingFull,
                    onClick = onBatteryClick,
                    modifier = Modifier.weight(1f)
                )
                HealthCheckTile(
                    title = "Anti-Tidur",
                    isReady = state.useAlarmClockMode,
                    icon = Icons.Rounded.LockClock,
                    onClick = onAlarmClockClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthCheckTile(
    title: String,
    isReady: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val bg = if (isReady) palette.statusOkBg else palette.statusWarnBg
    val fg = if (isReady) palette.statusOkFg else palette.statusWarnFg

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isReady) "Aktif" else "Ketuk aktifkan",
                    color = fg,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TestVibeLabCard(
    delayedCountdown: Int,
    onTestDelayed5s: () -> Unit,
    onTestImmediate: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val isCounting = delayedCountdown > 0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = palette.emeraldPrimary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Uji Getar ke Huawei Watch GT 5",
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gunakan tes jeda 5 detik lalu segera kunci layar HP untuk memastikan getaran masuk ke jam.",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onTestDelayed5s,
                enabled = !isCounting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.emeraldPrimary,
                    contentColor = if (palette.isDark) Color(0xFF03231D) else Color.White,
                    disabledContainerColor = palette.emeraldPrimary.copy(alpha = 0.6f),
                    disabledContentColor = if (palette.isDark) Color(0xFF03231D) else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isCounting) {
                        "Segera kunci layar HP... ($delayedCountdown dtk)"
                    } else {
                        "Tes Getar 5 Detik (Kunci Layar)"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onTestImmediate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = palette.emeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kirim Notifikasi Tes Sekarang",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReminderSettingsCard(
    state: ShalatVibeUiState,
    onToggleMasterReminder: (Boolean) -> Unit,
    onSelectGlobalPreMinutes: (Int) -> Unit,
    onToggleNotifyOnTime: (Boolean) -> Unit,
    onToggleDoublePing: (Boolean) -> Unit,
    onToggleIhtiyat: (Boolean) -> Unit,
    onToggleAlarmClockMode: (Boolean) -> Unit,
    onToggleCompactGt5: (Boolean) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val minuteOptions = listOf(5, 10, 15, 20, 30)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardSurface,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pengaturan Getar & Hisab",
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Pengingat otomatis ke GT 5",
                subtitle = "Jadwalkan notifikasi harian secara otomatis di latar belakang",
                checked = state.reminderActive,
                onCheckedChange = onToggleMasterReminder
            )

            HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "Jeda getar sebelum waktu shalat (Global)",
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Bisa dikustomisasi per waktu shalat dengan mengetuk baris di tab Jadwal",
                color = palette.textSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                minuteOptions.forEach { mins ->
                    val selected = state.globalPreReminderMinutes == mins
                    val chipBg by animateColorAsState(
                        targetValue = if (selected) palette.emeraldPrimary else palette.pillBg,
                        animationSpec = tween(180),
                        label = "chip_bg"
                    )
                    Surface(
                        onClick = { onSelectGlobalPreMinutes(mins) },
                        shape = CircleShape,
                        color = chipBg,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = if (selected) {
                                if (palette.isDark) Color(0xFF03231D) else Color.White
                            } else {
                                palette.textSecondary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.cardBorder, modifier = Modifier.padding(vertical = 12.dp))

            SettingSwitchRow(
                title = "Getar tepat saat Adzan tiba",
                subtitle = "Kirim getaran tambahan ketika waktu shalat masuk",
                checked = state.notifyOnTime,
                onCheckedChange = onToggleNotifyOnTime
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Getar ganda (Double Ping)",
                subtitle = "Kirim 2 notifikasi selang 30 detik agar tidak terlewat",
                checked = state.doublePing,
                onCheckedChange = onToggleDoublePing
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Format ringkas layar GT 5",
                subtitle = "Judul & isi singkat agar tampil utuh di layar bulat 1.43\"",
                checked = state.compactGt5Format,
                onCheckedChange = onToggleCompactGt5
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Gunakan Ihtiyat +2 menit",
                subtitle = "Tambahan kehati-hatian standar resmi Kemenag RI",
                checked = state.useIhtiyat,
                onCheckedChange = onToggleIhtiyat
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Mode Anti-Tidur (AlarmClock)",
                subtitle = "Wajib aktif agar alarm menembus Doze Mode saat layar HP mati",
                checked = state.useAlarmClockMode,
                onCheckedChange = onToggleAlarmClockMode
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.emeraldPrimary,
                uncheckedTrackColor = palette.pillBg,
                uncheckedBorderColor = palette.cardBorder
            )
        )
    }
}
