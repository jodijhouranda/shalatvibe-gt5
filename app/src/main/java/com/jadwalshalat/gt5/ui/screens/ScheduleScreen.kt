package com.jadwalshalat.gt5.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.ui.PrayerRowUi
import com.jadwalshalat.gt5.ui.ShalatVibeUiState
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import com.jadwalshalat.gt5.ui.theme.heroAtmosphereFor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScheduleScreen(
    state: ShalatVibeUiState,
    onOpenCityPicker: () -> Unit,
    onOpenMethodPicker: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onNavigateToGt5Hub: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToToday: () -> Unit,
    onTogglePrayer: (PrayerType) -> Unit,
    onOpenPrayerDetail: (PrayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Minimal Top Bar
        item(key = "top_bar") {
            ScheduleTopBar(
                cityName = state.cityName,
                methodLabel = shortMethodLabel(state.calculationMethod.name),
                isDark = palette.isDark,
                onOpenCityPicker = onOpenCityPicker,
                onOpenMethodPicker = onOpenMethodPicker,
                onOpenThemePicker = onOpenThemePicker
            )
        }

        // 2. Compact Readiness Alert Pill (hanya muncul jika izin kritis belum lengkap)
        if (state.missingCriticalPermissionCount > 0) {
            item(key = "perm_pill") {
                PermissionNoticePill(
                    missingCount = state.missingCriticalPermissionCount,
                    onClick = onNavigateToGt5Hub
                )
            }
        }

        // 3. Dynamic Atmosphere Hero Card + Solar Arc
        item(key = "hero_card") {
            DynamicHeroCard(state = state)
        }

        // 4. Date Switcher
        item(key = "date_switcher") {
            DateSwitcherBar(
                dateLabel = state.selectedDateHeaderLabel,
                hijriLabel = state.hijriDateLabel,
                isViewingToday = state.isViewingToday,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onResetToToday = onResetToToday
            )
        }

        // 5. Prayer Schedule Cards
        items(state.prayerRows, key = { it.type.name }) { row ->
            PrayerScheduleRowCard(
                row = row,
                reminderActive = state.reminderActive,
                onToggleEnabled = { onTogglePrayer(row.type) },
                onRowClick = { onOpenPrayerDetail(row.type) }
            )
        }
    }
}

@Composable
private fun ScheduleTopBar(
    cityName: String,
    methodLabel: String,
    isDark: Boolean,
    onOpenCityPicker: () -> Unit,
    onOpenMethodPicker: () -> Unit,
    onOpenThemePicker: () -> Unit
) {
    val palette = LocalShalatVibePalette.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onOpenCityPicker,
            shape = CircleShape,
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = "Pilih lokasi",
                    tint = palette.emeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = cityName,
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            onClick = onOpenMethodPicker,
            shape = CircleShape,
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.cardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = "Metode hisab",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = methodLabel,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Surface(
            onClick = onOpenThemePicker,
            shape = CircleShape,
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Ubah tema",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionNoticePill(
    missingCount: Int,
    onClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = palette.statusWarnBg,
        border = BorderStroke(1.dp, palette.statusWarnFg.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = palette.statusWarnFg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$missingCount izin GT 5 belum aktif • Ketuk untuk aktifkan",
                color = palette.statusWarnFg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = palette.statusWarnFg,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DynamicHeroCard(state: ShalatVibeUiState) {
    val palette = LocalShalatVibePalette.current
    val atmosphere = heroAtmosphereFor(state.currentPhase, palette.isDark)
    val animatedProgress by animateFloatAsState(
        targetValue = state.solarProgress.coerceIn(0.02f, 0.98f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "solar_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        atmosphere.startColor,
                        atmosphere.midColor,
                        atmosphere.endColor
                    )
                )
            )
    ) {
        // Subtle geometric Islamic 8-point star watermark in corners
        Canvas(modifier = Modifier.matchParentSize()) {
            val watermarkColor = Color.White.copy(alpha = 0.045f)
            drawIslamicStarWatermark(
                center = Offset(size.width * 0.92f, size.height * 0.18f),
                radius = 64.dp.toPx(),
                color = watermarkColor
            )
            drawIslamicStarWatermark(
                center = Offset(size.width * 0.08f, size.height * 0.85f),
                radius = 52.dp.toPx(),
                color = watermarkColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Solar Arc + Countdown overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    val strokePx = 4.dp.toPx()
                    val diameter = minOf(size.width - 36.dp.toPx(), 248.dp.toPx())
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = 14.dp.toPx()
                    )
                    val arcSize = Size(diameter, diameter)

                    // Background arc track (195° to 345° -> sweep 150°? Better: 190° sweep 160°)
                    val startAngle = 190f
                    val maxSweep = 160f

                    drawArc(
                        color = atmosphere.arcTrackColor,
                        startAngle = startAngle,
                        sweepAngle = maxSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    val activeSweep = maxSweep * animatedProgress
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                atmosphere.arcProgressStart,
                                atmosphere.arcProgressEnd
                            )
                        ),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx + 1.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Glowing celestial orb at the tip of the arc
                    val angleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val r = diameter / 2f
                    val centerX = topLeft.x + r
                    val centerY = topLeft.y + r
                    val orbX = centerX + (cos(angleRad) * r).toFloat()
                    val orbY = centerY + (sin(angleRad) * r).toFloat()

                    drawCircle(
                        color = atmosphere.orbColor.copy(alpha = 0.28f),
                        radius = 12.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )
                    drawCircle(
                        color = atmosphere.orbColor,
                        radius = 5.5.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = atmosphere.phaseLabel.uppercase(),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.countdownFormatted,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Menuju ${state.nextPrayer.displayName} • ${state.nextPrayerTimeFormatted}",
                        color = Color(0xFFD1FAE5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gregorian & Hijri Date line
            Text(
                text = if (state.hijriDateLabel.isNotBlank()) {
                    "${state.gregorianDateLabel}  •  ${state.hijriDateLabel}"
                } else {
                    state.gregorianDateLabel
                },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GT 5 Next Vibration Frosted Pill
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.13f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.nextGt5TriggerSummary,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIslamicStarWatermark(
    center: Offset,
    radius: Float,
    color: Color
) {
    val side = radius * 1.35f
    val topLeft = Offset(center.x - side / 2f, center.y - side / 2f)
    val squareSize = Size(side, side)
    val stroke = Stroke(width = 1.5.dp.toPx())
    drawRect(color = color, topLeft = topLeft, size = squareSize, style = stroke)
    rotate(degrees = 45f, pivot = center) {
        drawRect(color = color, topLeft = topLeft, size = squareSize, style = stroke)
    }
    drawCircle(color = color, radius = radius * 0.45f, center = center, style = stroke)
}

@Composable
private fun DateSwitcherBar(
    dateLabel: String,
    hijriLabel: String,
    isViewingToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToToday: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = "Hari sebelumnya",
                tint = palette.textSecondary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateLabel,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isViewingToday && hijriLabel.isNotBlank()) {
                Text(
                    text = hijriLabel,
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (!isViewingToday) {
            Surface(
                onClick = onResetToToday,
                shape = CircleShape,
                color = palette.emeraldSoft
            ) {
                Text(
                    text = "Hari Ini",
                    color = palette.emeraldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
            onClick = onNextDay,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Hari berikutnya",
                tint = palette.textSecondary
            )
        }
    }
}

@Composable
private fun PrayerScheduleRowCard(
    row: PrayerRowUi,
    reminderActive: Boolean,
    onToggleEnabled: () -> Unit,
    onRowClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = if (row.isNext) palette.highlightSurface else palette.cardSurface,
        animationSpec = tween(250),
        label = "row_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (row.isNext) palette.highlightBorder else palette.cardBorder,
        animationSpec = tween(250),
        label = "row_border"
    )

    val rowAlpha = if (row.isPassed && !row.isNext) 0.62f else 1f
    val bellActive = reminderActive && row.isEnabled

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(if (row.isNext) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onRowClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Celestial Prayer Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (row.isNext) palette.emeraldPrimary.copy(alpha = 0.16f)
                        else palette.pillBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = prayerIcon(row.type),
                    contentDescription = null,
                    tint = if (row.isNext) palette.emeraldPrimary else palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + Next badge + Reminder subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.type.displayName,
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (row.isNext) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = palette.emeraldPrimary
                        ) {
                            Text(
                                text = "Berikutnya",
                                color = if (palette.isDark) Color(0xFF03231D) else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else if (row.hasCustomOverride && row.isEnabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Pengaturan khusus",
                            tint = palette.goldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = row.reminderSubtitle,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Tabular Prayer Time
            Text(
                text = row.formattedTime,
                style = TextStyle(
                    color = if (row.isNext) palette.emeraldPrimary else palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Lightweight interactive Bell / Vibrate Toggle Button
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleEnabled()
                },
                shape = CircleShape,
                color = if (bellActive) {
                    palette.emeraldPrimary.copy(alpha = if (palette.isDark) 0.22f else 0.14f)
                } else {
                    palette.pillBg
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (bellActive) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.NotificationsOff
                        },
                        contentDescription = if (bellActive) "Nonaktifkan getar" else "Aktifkan getar",
                        tint = if (bellActive) palette.emeraldPrimary else palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun prayerIcon(type: PrayerType): ImageVector {
    return when (type) {
        PrayerType.SUBUH -> Icons.Rounded.WbTwilight
        PrayerType.TERBIT -> Icons.Rounded.WbSunny
        PrayerType.DZUHUR -> Icons.Outlined.LightMode
        PrayerType.ASHAR -> Icons.Rounded.WbCloudy
        PrayerType.MAGHRIB -> Icons.Rounded.NightsStay
        PrayerType.ISYA -> Icons.Rounded.Bedtime
    }
}

private fun shortMethodLabel(enumName: String): String {
    return when (enumName) {
        "KEMENAG_RI" -> "Kemenag RI"
        "JAKIM_MUIS" -> "MABIMS"
        "MWL" -> "MWL"
        "UMM_AL_QURA" -> "Umm Al-Qura"
        "ISNA" -> "ISNA"
        else -> enumName.replace('_', ' ')
    }
}
