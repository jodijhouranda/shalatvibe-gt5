package com.jadwalshalat.gt5.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.HorizontalDivider
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
import com.jadwalshalat.gt5.ui.components.AdaptiveBreakpoints
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val useTwoPane = maxWidth >= 760.dp
        val horizontalPad = if (useTwoPane) 24.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = AdaptiveBreakpoints.WideCatalogMaxWidth),
            contentPadding = PaddingValues(start = horizontalPad, end = horizontalPad, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Sleek Borderless Top Bar (Location + Method subtitle + GT5 status pill + Theme toggle)
            item(key = "top_bar") {
                ScheduleTopBar(
                    cityName = state.cityName,
                    methodLabel = shortMethodLabel(state.calculationMethod.name),
                    timezoneLabel = state.zoneId.id.substringAfter('/').replace('_', ' '),
                    useIhtiyat = state.useIhtiyat,
                    missingPermCount = state.missingCriticalPermissionCount,
                    isDark = palette.isDark,
                    onOpenCityPicker = onOpenCityPicker,
                    onOpenMethodPicker = onOpenMethodPicker,
                    onNavigateToGt5Hub = onNavigateToGt5Hub,
                    onOpenThemePicker = onOpenThemePicker
                )
            }

            if (useTwoPane) {
                item(key = "tablet_schedule_two_pane") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(0.47f)) {
                            DynamicHeroCard(state = state)
                        }
                        Column(
                            modifier = Modifier.weight(0.53f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DateSwitcherBar(
                                dateLabel = state.selectedDateHeaderLabel,
                                hijriLabel = state.hijriDateLabel,
                                isViewingToday = state.isViewingToday,
                                onPreviousDay = onPreviousDay,
                                onNextDay = onNextDay,
                                onResetToToday = onResetToToday
                            )
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = palette.cardSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    state.prayerRows.forEachIndexed { idx, row ->
                                        PrayerScheduleRowItem(
                                            row = row,
                                            reminderActive = state.reminderActive,
                                            onToggleEnabled = { onTogglePrayer(row.type) },
                                            onRowClick = { onOpenPrayerDetail(row.type) }
                                        )
                                        if (idx < state.prayerRows.lastIndex && !row.isNext && !state.prayerRows[idx + 1].isNext) {
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = palette.hairline,
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. Characterful Dynamic Atmosphere Hero Card + Wide Solar Horizon Arc
                item(key = "hero_card") {
                    DynamicHeroCard(state = state)
                }

                // 3. Sleek Borderless Date Switcher
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

                // 4. Unified Tonal Prayer Schedule Surface (Replaces 6 heavy bordered cards)
                item(key = "prayer_schedule_card") {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            state.prayerRows.forEachIndexed { idx, row ->
                                PrayerScheduleRowItem(
                                    row = row,
                                    reminderActive = state.reminderActive,
                                    onToggleEnabled = { onTogglePrayer(row.type) },
                                    onRowClick = { onOpenPrayerDetail(row.type) }
                                )
                                if (idx < state.prayerRows.lastIndex && !row.isNext && !state.prayerRows[idx + 1].isNext) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = palette.hairline,
                                        modifier = Modifier.padding(horizontal = 14.dp)
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

@Composable
private fun ScheduleTopBar(
    cityName: String,
    methodLabel: String,
    timezoneLabel: String,
    useIhtiyat: Boolean,
    missingPermCount: Int,
    isDark: Boolean,
    onOpenCityPicker: () -> Unit,
    onOpenMethodPicker: () -> Unit,
    onNavigateToGt5Hub: () -> Unit,
    onOpenThemePicker: () -> Unit
) {
    val palette = LocalShalatVibePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenCityPicker)
                    .padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = "Pilih lokasi",
                    tint = palette.emeraldPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = cityName,
                    color = palette.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenMethodPicker)
                    .padding(start = 22.dp, top = 1.dp, bottom = 2.dp, end = 6.dp)
            ) {
                Text(
                    text = buildString {
                        append(methodLabel)
                        append(" • ")
                        append(shortZoneTag(timezoneLabel))
                        if (useIhtiyat) append(" • Ihtiyat +2m")
                    },
                    color = palette.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Compact Tonal GT 5 Readiness Pill
        Surface(
            onClick = onNavigateToGt5Hub,
            shape = CircleShape,
            color = if (missingPermCount > 0) palette.statusWarnBg else palette.pillBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (missingPermCount > 0) palette.statusWarnFg else palette.emeraldBright)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (missingPermCount > 0) "$missingPermCount Izin" else "GT 5",
                    color = if (missingPermCount > 0) palette.statusWarnFg else palette.emeraldPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Borderless Tonal Theme Icon Button
        Surface(
            onClick = onOpenThemePicker,
            shape = CircleShape,
            color = palette.pillBg,
            modifier = Modifier.size(36.dp)
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
private fun DynamicHeroCard(state: ShalatVibeUiState) {
    val palette = LocalShalatVibePalette.current
    val atmosphere = heroAtmosphereFor(state.currentPhase, palette.isDark)
    val animatedProgress by animateFloatAsState(
        targetValue = state.solarProgress.coerceIn(0.03f, 0.97f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "solar_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
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
        // Ambient radial glow + subtle Islamic geometric watermark
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(atmosphere.orbColor.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.84f, size.height * 0.16f),
                    radius = 120.dp.toPx()
                ),
                radius = 120.dp.toPx(),
                center = Offset(size.width * 0.84f, size.height * 0.16f)
            )
            drawIslamicStarWatermark(
                center = Offset(size.width * 0.90f, size.height * 0.22f),
                radius = 58.dp.toPx(),
                color = Color.White.copy(alpha = 0.045f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Phase & Hijri Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${atmosphere.phaseLabel.uppercase()} • ${state.currentPhase.displayName.uppercase()}",
                    color = Color(0xFFD1FAE5).copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (state.hijriDateLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.11f)
                    ) {
                        Text(
                            text = state.hijriDateLabel,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wide Solar Horizon Arc + Generously Spaced Editorial Countdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val strokePx = 4.dp.toPx()
                    val diameter = minOf(size.width - 28.dp.toPx(), 262.dp.toPx())
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = 14.dp.toPx()
                    )
                    val arcSize = Size(diameter, diameter)
                    val startAngle = 192f
                    val maxSweep = 156f

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

                    val angleRad = Math.toRadians((startAngle + activeSweep).toDouble())
                    val r = diameter / 2f
                    val centerX = topLeft.x + r
                    val centerY = topLeft.y + r
                    val orbX = centerX + (cos(angleRad) * r).toFloat()
                    val orbY = centerY + (sin(angleRad) * r).toFloat()

                    drawCircle(
                        color = atmosphere.orbColor.copy(alpha = 0.28f),
                        radius = 11.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )
                    drawCircle(
                        color = atmosphere.orbColor,
                        radius = 5.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        text = state.countdownFormatted,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.6).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Menuju ${state.nextPrayer.displayName} • ${state.nextPrayerTimeFormatted}",
                        color = Color(0xFFD1FAE5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Integrated Frosted Watch Dock (no harsh border)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.11f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.nextGt5TriggerSummary,
                        color = Color.White.copy(alpha = 0.94f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
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
    val stroke = Stroke(width = 1.4.dp.toPx())
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
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.size(32.dp)
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
                fontSize = 13.5.sp,
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
            modifier = Modifier.size(32.dp)
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
private fun PrayerScheduleRowItem(
    row: PrayerRowUi,
    reminderActive: Boolean,
    onToggleEnabled: () -> Unit,
    onRowClick: () -> Unit
) {
    val palette = LocalShalatVibePalette.current
    val haptic = LocalHapticFeedback.current

    val rowBg by animateColorAsState(
        targetValue = if (row.isNext) palette.highlightSurface else Color.Transparent,
        animationSpec = tween(250),
        label = "row_bg"
    )

    val rowAlpha = if (row.isPassed && !row.isNext) 0.56f else 1f
    val bellActive = reminderActive && row.isEnabled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(18.dp))
            .background(rowBg)
            .clickable(onClick = onRowClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (row.isNext) 13.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (row.isNext) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(palette.emeraldBright)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Icon(
                imageVector = prayerIcon(row.type),
                contentDescription = null,
                tint = when {
                    row.isNext -> palette.emeraldPrimary
                    row.type == PrayerType.TERBIT || row.type == PrayerType.MAGHRIB -> palette.goldAccent
                    else -> palette.textSecondary
                },
                modifier = Modifier.size(19.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.type.displayName,
                        color = palette.textPrimary,
                        fontSize = if (row.isNext) 15.5.sp else 14.5.sp,
                        fontWeight = if (row.isNext) FontWeight.Bold else FontWeight.SemiBold
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
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = row.reminderSubtitle,
                    color = if (row.isNext) palette.emeraldPrimary else palette.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = row.formattedTime,
                style = TextStyle(
                    color = if (row.isNext) palette.emeraldPrimary else palette.textPrimary,
                    fontSize = if (row.isNext) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleEnabled()
                },
                shape = CircleShape,
                color = if (bellActive) palette.emeraldSoft else palette.pillBg,
                modifier = Modifier.size(32.dp)
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
                        modifier = Modifier.size(16.dp)
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

private fun shortZoneTag(cityRegion: String): String {
    return when {
        cityRegion.contains("Jakarta", ignoreCase = true) ||
            cityRegion.contains("Pontianak", ignoreCase = true) -> "WIB"
        cityRegion.contains("Makassar", ignoreCase = true) -> "WITA"
        cityRegion.contains("Jayapura", ignoreCase = true) -> "WIT"
        else -> cityRegion
    }
}
