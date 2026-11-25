package com.jadwalshalat.gt5.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.notification.PhoneRingerState
import com.jadwalshalat.gt5.notification.PhoneVibrationPattern
import com.jadwalshalat.gt5.ui.ShalatVibeUiState
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@Composable
fun PhoneVibrationSettingsCard(
    state: ShalatVibeUiState,
    onTogglePhoneVibrate: (Boolean) -> Unit,
    onToggleVibrateOnSilent: (Boolean) -> Unit,
    onToggleVibrateOnPreReminder: (Boolean) -> Unit,
    onSelectPattern: (PhoneVibrationPattern) -> Unit,
    onTestPhoneVibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val isSilent = state.phoneRingerState == PhoneRingerState.SILENT

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.cardSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.emeraldSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = palette.emeraldPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Getar HP & Mode Silent",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kendali motor getar ponsel saat waktu shalat",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isSilent) palette.statusWarnBg else palette.statusOkBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        if (isSilent) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = palette.statusWarnFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = state.phoneRingerState.label,
                            color = if (isSilent) palette.statusWarnFg else palette.statusOkFg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CompactSwitchRow(
                title = "Aktifkan Getar HP",
                subtitle = "Getarkan ponsel secara lokal selain meneruskan notifikasi ke jam",
                checked = state.phoneVibrateEnabled,
                onCheckedChange = onTogglePhoneVibrate,
                switchTestTag = "phone_vibrate_enabled_switch"
            )

            AnimatedVisibility(
                visible = state.phoneVibrateEnabled,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.emeraldSoft.copy(alpha = if (palette.isDark) 0.55f else 0.72f),
                        border = BorderStroke(1.dp, palette.emeraldPrimary.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            CompactSwitchRow(
                                title = "Tetap Bergetar Saat HP Silent",
                                subtitle = "Memakai jalur getar Alarm agar HP tetap bergetar meski mode senyap aktif",
                                checked = state.phoneVibrateOnSilent,
                                onCheckedChange = onToggleVibrateOnSilent,
                                switchTestTag = "phone_vibrate_silent_switch"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kapan getar HP aktif",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScopeOptionChip(
                            label = "Adzan & Pengingat",
                            selected = state.phoneVibrateOnPreReminder,
                            onClick = { onToggleVibrateOnPreReminder(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_vibrate_scope_all")
                        )
                        ScopeOptionChip(
                            label = "Saat Adzan Saja",
                            selected = !state.phoneVibrateOnPreReminder,
                            onClick = { onToggleVibrateOnPreReminder(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_vibrate_scope_adzan")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pola & kekuatan getar HP",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ketuk salah satu pola untuk langsung merasakan pratinjau getarnya",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhoneVibrationPattern.entries.forEach { pattern ->
                            val selected = state.phoneVibratePattern == pattern
                            val bg by animateColorAsState(
                                targetValue = if (selected) palette.emeraldPrimary else palette.pillBg,
                                animationSpec = tween(180),
                                label = "pattern_bg"
                            )
                            Surface(
                                onClick = { onSelectPattern(pattern) },
                                shape = RoundedCornerShape(14.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("phone_vibrate_pattern_${pattern.key.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = pattern.label,
                                        color = if (selected) {
                                            if (palette.isDark) Color(0xFF03231D) else Color.White
                                        } else {
                                            palette.textPrimary
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = pattern.description,
                                        color = if (selected) {
                                            if (palette.isDark) Color(0xFF03231D).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.88f)
                                        } else {
                                            palette.textSecondary
                                        },
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onTestPhoneVibration,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.emeraldPrimary,
                            contentColor = if (palette.isDark) Color(0xFF03231D) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("test_phone_vibrate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSilent && state.phoneVibrateOnSilent) {
                                "Tes Getar HP (Tembus Silent)"
                            } else {
                                "Tes Getar HP Sekarang"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyWorkManagerCard(
    state: ShalatVibeUiState,
    onToggleDailySummary: (Boolean) -> Unit,
    onSyncWorkNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.cardSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.emeraldSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = palette.emeraldPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sinkronisasi Harian (WorkManager)",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lokasi tersimpan: ${state.cityName}",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (state.reminderActive) palette.statusOkBg else palette.statusWarnBg
                ) {
                    Text(
                        text = if (state.reminderActive) "Aktif" else "Dijeda",
                        color = if (state.reminderActive) palette.statusOkFg else palette.statusWarnFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CompactSwitchRow(
                title = "Notifikasi Ringkasan Jadwal Harian",
                subtitle = "WorkManager mengirim ringkasan 5 waktu shalat tiap hari & menyinkronkan ulang alarm otomatis",
                checked = state.dailySummaryEnabled,
                onCheckedChange = onToggleDailySummary,
                switchTestTag = "daily_summary_switch"
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = palette.hairline,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            Text(
                text = state.lastWorkSyncLog.ifBlank { "Menunggu sinkronisasi harian WorkManager." },
                color = palette.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                onClick = onSyncWorkNow,
                shape = RoundedCornerShape(14.dp),
                color = palette.pillBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sync_workmanager_now_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = palette.emeraldPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sinkronkan & Uji WorkManager Sekarang",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopeOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val bg by animateColorAsState(
        targetValue = if (selected) palette.emeraldPrimary else palette.pillBg,
        animationSpec = tween(180),
        label = "scope_bg"
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                color = if (selected) {
                    if (palette.isDark) Color(0xFF03231D) else Color.White
                } else {
                    palette.textSecondary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompactSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
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
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.emeraldPrimary,
                uncheckedTrackColor = palette.pillBg,
                uncheckedBorderColor = palette.cardBorder
            )
        )
    }
}
