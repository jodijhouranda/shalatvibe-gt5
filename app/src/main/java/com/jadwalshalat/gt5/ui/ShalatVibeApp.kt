package com.jadwalshalat.gt5.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.ui.components.CityPickerBottomSheet
import com.jadwalshalat.gt5.ui.components.HuaweiGuideBottomSheet
import com.jadwalshalat.gt5.ui.components.MethodPickerBottomSheet
import com.jadwalshalat.gt5.ui.components.PrayerDetailBottomSheet
import com.jadwalshalat.gt5.ui.components.QiblaCalibrationBottomSheet
import com.jadwalshalat.gt5.ui.components.ThemePickerBottomSheet
import com.jadwalshalat.gt5.ui.screens.Gt5HubScreen
import com.jadwalshalat.gt5.ui.screens.QiblaScreen
import com.jadwalshalat.gt5.ui.screens.ScheduleScreen
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import kotlinx.coroutines.launch

@Composable
fun ShalatVibeApp(
    state: ShalatVibeUiState,
    viewModel: ShalatVibeViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestGpsLocation: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val palette = LocalShalatVibePalette.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(MainTab.SCHEDULE) }
    var showCitySheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showHuaweiGuideSheet by remember { mutableStateOf(false) }
    var showCalibrationSheet by remember { mutableStateOf(false) }
    var detailPrayerType by remember { mutableStateOf<PrayerType?>(null) }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = palette.navBarBg,
                tonalElevation = 0.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val selectedIcon = when (tab) {
                        MainTab.SCHEDULE -> Icons.Rounded.CalendarMonth
                        MainTab.QIBLA -> Icons.Rounded.Explore
                        MainTab.GT5_HUB -> Icons.Rounded.Watch
                    }
                    val unselectedIcon = when (tab) {
                        MainTab.SCHEDULE -> Icons.Outlined.CalendarMonth
                        MainTab.QIBLA -> Icons.Outlined.Explore
                        MainTab.GT5_HUB -> Icons.Outlined.Watch
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == MainTab.GT5_HUB && state.missingCriticalPermissionCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = palette.goldAccent,
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) selectedIcon else unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) selectedIcon else unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.emeraldPrimary,
                            selectedTextColor = palette.emeraldPrimary,
                            indicatorColor = palette.emeraldSoft,
                            unselectedIconColor = palette.textMuted,
                            unselectedTextColor = palette.textMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(220),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    MainTab.SCHEDULE -> ScheduleScreen(
                        state = state,
                        onOpenCityPicker = { showCitySheet = true },
                        onOpenMethodPicker = { showMethodSheet = true },
                        onOpenThemePicker = { showThemeSheet = true },
                        onNavigateToGt5Hub = { currentTab = MainTab.GT5_HUB },
                        onPreviousDay = { viewModel.selectPreviousDay() },
                        onNextDay = { viewModel.selectNextDay() },
                        onResetToToday = { viewModel.resetToToday() },
                        onTogglePrayer = { viewModel.togglePrayerEnabled(it) },
                        onOpenPrayerDetail = { detailPrayerType = it }
                    )

                    MainTab.QIBLA -> QiblaScreen(
                        state = state,
                        onOpenCalibrationSheet = { showCalibrationSheet = true }
                    )

                    MainTab.GT5_HUB -> Gt5HubScreen(
                        state = state,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        onRequestIgnoreBattery = onRequestIgnoreBattery,
                        onToggleMasterReminder = { viewModel.setReminderActive(it) },
                        onSelectGlobalPreMinutes = { viewModel.setGlobalPreReminderMinutes(it) },
                        onToggleNotifyOnTime = { viewModel.setNotifyOnTime(it) },
                        onToggleDoublePing = { viewModel.setDoublePing(it) },
                        onToggleIhtiyat = { viewModel.setUseIhtiyat(it) },
                        onToggleAlarmClockMode = { viewModel.setUseAlarmClockMode(it) },
                        onToggleCompactGt5 = { viewModel.setCompactGt5Format(it) },
                        onTestImmediate = {
                            val ok = viewModel.sendImmediateTestNotification()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) "Notifikasi tes dikirim ke HP & GT 5."
                                    else "Aktifkan izin Notifikasi terlebih dahulu."
                                )
                            }
                        },
                        onTestDelayed5s = {
                            val ok = viewModel.triggerDelayedTestNotification(5)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) "Notifikasi dikirim dalam 5 detik. Segera kunci layar HP!"
                                    else "Aktifkan izin Notifikasi terlebih dahulu."
                                )
                            }
                        },
                        onOpenHuaweiGuide = { showHuaweiGuideSheet = true },
                        onOpenThemePicker = { showThemeSheet = true }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheets
    if (showCitySheet) {
        CityPickerBottomSheet(
            currentCityName = state.cityName,
            currentLat = state.latitude,
            currentLon = state.longitude,
            onSelectCity = { preset ->
                viewModel.selectCityPreset(preset)
                scope.launch {
                    snackbarHostState.showSnackbar("Lokasi diubah ke ${preset.name}")
                }
            },
            onUseGps = onRequestGpsLocation,
            onDismiss = { showCitySheet = false }
        )
    }

    if (showMethodSheet) {
        MethodPickerBottomSheet(
            currentMethod = state.calculationMethod,
            useIhtiyat = state.useIhtiyat,
            previews = state.methodPreviews,
            onSelectMethod = { viewModel.setCalculationMethod(it) },
            onToggleIhtiyat = { viewModel.setUseIhtiyat(it) },
            onDismiss = { showMethodSheet = false }
        )
    }

    if (showThemeSheet) {
        ThemePickerBottomSheet(
            currentTheme = state.themeMode,
            onSelectTheme = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showHuaweiGuideSheet) {
        HuaweiGuideBottomSheet(
            checklistStates = state.huaweiChecklistStates,
            onToggleStep = { viewModel.toggleHuaweiChecklistStep(it) },
            onDismiss = { showHuaweiGuideSheet = false }
        )
    }

    if (showCalibrationSheet) {
        QiblaCalibrationBottomSheet(
            onDismiss = { showCalibrationSheet = false }
        )
    }

    detailPrayerType?.let { prayerType ->
        val row = state.prayerRows.firstOrNull { it.type == prayerType }
        if (row != null) {
            PrayerDetailBottomSheet(
                row = row,
                globalPreMinutes = state.globalPreReminderMinutes,
                currentOverride = viewModel.getPrayerPreReminderOverride(prayerType),
                onToggleEnabled = { viewModel.setPrayerEnabled(prayerType, it) },
                onSelectOverride = { viewModel.setPrayerPreReminderOverride(prayerType, it) },
                onDismiss = { detailPrayerType = null }
            )
        }
    }
}
