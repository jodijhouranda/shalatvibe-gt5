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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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
import com.jadwalshalat.gt5.arabic.ArabicStudyUiState
import com.jadwalshalat.gt5.arabic.ArabicStudyViewModel
import com.jadwalshalat.gt5.learning.LearningDestination
import com.jadwalshalat.gt5.learning.LearningHubUiState
import com.jadwalshalat.gt5.learning.LearningViewModel
import com.jadwalshalat.gt5.model.PrayerType
import com.jadwalshalat.gt5.ui.components.ArabicSyncSettingsBottomSheet
import com.jadwalshalat.gt5.ui.components.CityPickerBottomSheet
import com.jadwalshalat.gt5.ui.components.HuaweiGuideBottomSheet
import com.jadwalshalat.gt5.ui.components.MethodPickerBottomSheet
import com.jadwalshalat.gt5.ui.components.PrayerDetailBottomSheet
import com.jadwalshalat.gt5.ui.components.QiblaCalibrationBottomSheet
import com.jadwalshalat.gt5.ui.components.ThemePickerBottomSheet
import com.jadwalshalat.gt5.ui.screens.ArabicCatalogScreen
import com.jadwalshalat.gt5.ui.screens.ArabicKamusScreen
import com.jadwalshalat.gt5.ui.screens.ArabicStudyRoomScreen
import com.jadwalshalat.gt5.ui.screens.CourseFullBookPdfScreen
import com.jadwalshalat.gt5.ui.screens.CourseOverviewScreen
import com.jadwalshalat.gt5.ui.screens.CourseStudyRoomScreen
import com.jadwalshalat.gt5.ui.screens.Gt5HubScreen
import com.jadwalshalat.gt5.ui.screens.LearningLibraryScreen
import com.jadwalshalat.gt5.ui.screens.QiblaScreen
import com.jadwalshalat.gt5.ui.screens.ScheduleScreen
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import kotlinx.coroutines.launch

@Composable
fun ShalatVibeApp(
    state: ShalatVibeUiState,
    viewModel: ShalatVibeViewModel,
    arabicState: ArabicStudyUiState,
    arabicViewModel: ArabicStudyViewModel,
    learningState: LearningHubUiState,
    learningViewModel: LearningViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestGpsLocation: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val palette = LocalShalatVibePalette.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentTab by remember { mutableStateOf(MainTab.SCHEDULE) }
    var showCitySheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showHuaweiGuideSheet by remember { mutableStateOf(false) }
    var showCalibrationSheet by remember { mutableStateOf(false) }
    var showArabicSyncSheet by remember { mutableStateOf(false) }
    var showArabicKamus by remember { mutableStateOf(false) }
    var detailPrayerType by remember { mutableStateOf<PrayerType?>(null) }

    // Jika pengguna membuka Kamus Saya (dari Pustaka Belajar, Katalog Arab, atau Ruang Belajar), tampilkan penuh tanpa bottom bar
    if (showArabicKamus) {
        ArabicKamusScreen(
            entries = arabicState.kamusEntries,
            selectedFilter = arabicState.kamusFilter,
            searchQuery = arabicState.kamusSearchQuery,
            showHarakat = arabicState.showHarakat,
            onToggleHarakat = { arabicViewModel.toggleHarakat() },
            onSelectFilter = { arabicViewModel.setKamusFilter(it) },
            onSearchQueryChange = { arabicViewModel.setKamusSearchQuery(it) },
            onSaveEntry = { arabicViewModel.saveKamusEntry(it) },
            onDeleteEntry = { arabicViewModel.deleteKamusEntry(it) },
            onGradeSrs = { id, ok -> arabicViewModel.gradeKamusSrs(id, ok) },
            onToggleMastered = { id -> arabicViewModel.toggleKamusMastered(id) },
            onImportBackupJson = { raw -> arabicViewModel.importKamusBackupJson(raw) },
            onOpenLessonClip = { lessonId, startSec ->
                showArabicKamus = false
                arabicViewModel.openLessonClip(lessonId, startSec)
            },
            onSpeakArabic = { text ->
                arabicViewModel.speakArabic(text)?.let { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            kamusAiState = arabicState.kamusAiState,
            onLookupKamusAi = { ar, ctx, title, force ->
                arabicViewModel.lookupKamusWithAi(ar, ctx, title, force)
            },
            onClearKamusAiState = { arabicViewModel.clearKamusAiState() },
            onBack = {
                arabicViewModel.clearKamusAiState()
                showArabicKamus = false
            }
        )
        return
    }

    // Jika pengguna sedang membuka Ruang Belajar Bahasa Arab (Video YouTube + Teks Interaktif/PDF), tampilkan penuh tanpa bottom bar
    val openLesson = arabicState.activeLesson
    if (openLesson != null) {
        ArabicStudyRoomScreen(
            lesson = openLesson,
            pdfFile = arabicState.activePdfFile,
            isLoadingPdf = arabicState.isLoadingPdf,
            pdfErrorMessage = arabicState.pdfErrorMessage,
            isCompleted = arabicState.completedIds.contains(openLesson.id),
            readingMode = arabicState.readingMode,
            lessonContent = arabicState.activeLessonContent,
            isLoadingContent = arabicState.isLoadingContent,
            initialSeekSec = arabicState.activeSeekSec,
            showHarakat = arabicState.showHarakat,
            savedNormalizedKeys = arabicState.savedNormalizedKeys,
            kamusEntries = arabicState.kamusEntries,
            kamusCount = arabicState.kamusEntries.size,
            kamusAiState = arabicState.kamusAiState,
            onLookupKamusAi = { ar, ctx, title, force ->
                arabicViewModel.lookupKamusWithAi(ar, ctx, title, force)
            },
            onClearKamusAiState = { arabicViewModel.clearKamusAiState() },
            onToggleHarakat = { arabicViewModel.toggleHarakat() },
            onSaveKamusEntry = { arabicViewModel.saveKamusEntry(it) },
            onOpenKamus = { showArabicKamus = true },
            onSpeakArabic = { text -> arabicViewModel.speakArabic(text) },
            onCycleReadingMode = { arabicViewModel.cycleReadingMode() },
            onToggleCompleted = { arabicViewModel.toggleCompleted(openLesson.id) },
            onBack = {
                arabicViewModel.clearKamusAiState()
                arabicViewModel.closeLesson()
            }
        )
        return
    }

    // Jika pengguna sedang membuka Blok Tema atau Kitab PDF dari Seri Kursus (mis. Fiqh Muamalah), tampilkan penuh tanpa bottom bar
    when (val dest = learningState.destination) {
        is LearningDestination.CourseStudyBlock -> {
            val course = learningState.seriesCourses[dest.courseId]
            val found = course?.findEpisodeAndBlock(dest.blockId)
            if (course != null && found != null) {
                val (episode, block) = found
                val flat = course.allBlocks
                val currentIdx = flat.indexOfFirst { (_, b) -> b.id == block.id }
                val hasNextBlock = currentIdx >= 0 && currentIdx + 1 < flat.size
                CourseStudyRoomScreen(
                    course = course,
                    episode = episode,
                    block = block,
                    initialTimeSec = dest.initialTimeSec,
                    pdfFile = learningState.activeCoursePdfFile,
                    isLoadingPdf = learningState.isLoadingCoursePdf,
                    pdfError = learningState.coursePdfError,
                    isCompleted = learningState.completedBlockIds.contains(block.id),
                    hasNextBlock = hasNextBlock,
                    readingMode = arabicState.readingMode,
                    onCycleReadingMode = { arabicViewModel.cycleReadingMode() },
                    onToggleBlockCompleted = { learningViewModel.toggleBlockCompleted(block.id) },
                    onCompleteAndNext = { learningViewModel.markCompletedAndOpenNext(course.id, block.id) },
                    onSelectOtherBlock = { epId, blkId, seekSec ->
                        learningViewModel.openTopicBlock(course.id, epId, blkId, seekSec)
                    },
                    onBack = { learningViewModel.navigateBack() }
                )
                return
            }
        }
        is LearningDestination.CourseBookPdf -> {
            val course = learningState.seriesCourses[dest.courseId]
            if (course != null) {
                CourseFullBookPdfScreen(
                    course = course,
                    pdfFile = learningState.activeCoursePdfFile,
                    isLoadingPdf = learningState.isLoadingCoursePdf,
                    pdfError = learningState.coursePdfError,
                    initialPage = dest.initialPage,
                    readingMode = arabicState.readingMode,
                    onCycleReadingMode = { arabicViewModel.cycleReadingMode() },
                    onBack = { learningViewModel.navigateBack() }
                )
                return
            }
        }
        else -> Unit
    }

    Scaffold(
        containerColor = palette.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.background(palette.navBarBg)
            ) {
                androidx.compose.material3.HorizontalDivider(
                    thickness = 0.5.dp,
                    color = palette.hairline
                )
                NavigationBar(
                    containerColor = palette.navBarBg,
                    tonalElevation = 0.dp
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        val selectedIcon = when (tab) {
                            MainTab.SCHEDULE -> Icons.Rounded.CalendarMonth
                            MainTab.ARABIC -> Icons.AutoMirrored.Rounded.MenuBook
                            MainTab.QIBLA -> Icons.Rounded.Explore
                            MainTab.GT5_HUB -> Icons.Rounded.Watch
                        }
                        val unselectedIcon = when (tab) {
                            MainTab.SCHEDULE -> Icons.Outlined.CalendarMonth
                            MainTab.ARABIC -> Icons.AutoMirrored.Outlined.MenuBook
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
                                                    .size(8.dp)
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

                    MainTab.ARABIC -> {
                        when (val dest = learningState.destination) {
                            LearningDestination.ArabicCatalog -> ArabicCatalogScreen(
                                state = arabicState,
                                onSelectFilter = { arabicViewModel.setFilter(it) },
                                onSearchQueryChange = { arabicViewModel.setSearchQuery(it) },
                                onOpenLesson = { arabicViewModel.openLesson(it) },
                                onToggleFavorite = { arabicViewModel.toggleFavorite(it) },
                                onSyncNow = {
                                    arabicViewModel.syncCatalog { result ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                },
                                onOpenSyncSettings = { showArabicSyncSheet = true },
                                onOpenKamus = { showArabicKamus = true },
                                onBack = { learningViewModel.navigateBack() }
                            )
                            is LearningDestination.CourseOverview -> {
                                val course = learningState.seriesCourses[dest.courseId]
                                if (course != null) {
                                    CourseOverviewScreen(
                                        course = course,
                                        completedBlockIds = learningState.completedBlockIds,
                                        onBack = { learningViewModel.navigateBack() },
                                        onOpenBlock = { epId, blkId, seekSec ->
                                            learningViewModel.openTopicBlock(course.id, epId, blkId, seekSec)
                                        },
                                        onOpenFullBookPdf = { page ->
                                            learningViewModel.openFullBookPdf(course.id, page)
                                        },
                                        onToggleBlockCompleted = { blkId ->
                                            learningViewModel.toggleBlockCompleted(blkId)
                                        }
                                    )
                                } else {
                                    LearningLibraryScreen(
                                        hubState = learningState,
                                        arabicState = arabicState,
                                        onSearchQueryChange = { learningViewModel.setSearchQuery(it) },
                                        onOpenTrack = { learningViewModel.openTrack(it) },
                                        onOpenArabicLesson = { arabicViewModel.openLesson(it) },
                                        onOpenTopicBlock = { cid, epId, blkId, seekSec ->
                                            learningViewModel.openTopicBlock(cid, epId, blkId, seekSec)
                                        },
                                        onSyncAll = {
                                            arabicViewModel.syncCatalog {}
                                            learningViewModel.syncCourses { result ->
                                                scope.launch { snackbarHostState.showSnackbar(result.message) }
                                            }
                                        },
                                        onOpenSyncSettings = { showArabicSyncSheet = true },
                                        onOpenArabicKamus = { showArabicKamus = true }
                                    )
                                }
                            }
                            else -> LearningLibraryScreen(
                                hubState = learningState,
                                arabicState = arabicState,
                                onSearchQueryChange = { learningViewModel.setSearchQuery(it) },
                                onOpenTrack = { learningViewModel.openTrack(it) },
                                onOpenArabicLesson = { arabicViewModel.openLesson(it) },
                                onOpenTopicBlock = { cid, epId, blkId, seekSec ->
                                    learningViewModel.openTopicBlock(cid, epId, blkId, seekSec)
                                },
                                onSyncAll = {
                                    arabicViewModel.syncCatalog {}
                                    learningViewModel.syncCourses { result ->
                                        scope.launch { snackbarHostState.showSnackbar(result.message) }
                                    }
                                },
                                onOpenSyncSettings = { showArabicSyncSheet = true },
                                onOpenArabicKamus = { showArabicKamus = true }
                            )
                        }
                    }

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
                        onToggleAdzanTripleBurst = { viewModel.setAdzanTripleBurst(it) },
                        onToggleDoublePing = { viewModel.setDoublePing(it) },
                        onToggleIhtiyat = { viewModel.setUseIhtiyat(it) },
                        onToggleAlarmClockMode = { viewModel.setUseAlarmClockMode(it) },
                        onToggleCompactGt5 = { viewModel.setCompactGt5Format(it) },
                        onTogglePhoneVibrate = { viewModel.setPhoneVibrateEnabled(it) },
                        onToggleVibrateOnSilent = { viewModel.setPhoneVibrateOnSilent(it) },
                        onToggleVibrateOnPreReminder = { viewModel.setPhoneVibrateOnPreReminder(it) },
                        onSelectPhoneVibratePattern = { viewModel.setPhoneVibratePattern(it) },
                        onTestPhoneVibration = {
                            val message = viewModel.testPhoneVibration()
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onToggleDailySummary = { viewModel.setDailySummaryEnabled(it) },
                        onSyncDailyWorkNow = {
                            val message = viewModel.triggerManualDailyWorkSync()
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        onTestImmediate = {
                            val ok = viewModel.sendImmediateTestNotification()
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    when {
                                        !ok -> "Aktifkan izin Notifikasi terlebih dahulu."
                                        state.adzanTripleBurst -> "Mengirim 3x getar mulus Adzan ke HP & GT 5."
                                        else -> "Notifikasi tes dikirim ke HP & GT 5."
                                    }
                                )
                            }
                        },
                        onTestDelayed5s = {
                            val ok = viewModel.triggerDelayedTestNotification(5)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    when {
                                        !ok -> "Aktifkan izin Notifikasi terlebih dahulu."
                                        state.adzanTripleBurst -> "3x getar mulus dikirim dalam 5 detik. Segera kunci layar HP!"
                                        else -> "Notifikasi dikirim dalam 5 detik. Segera kunci layar HP!"
                                    }
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

    if (showArabicSyncSheet) {
        ArabicSyncSettingsBottomSheet(
            currentUrl = arabicState.syncManifestUrl,
            lastSyncLabel = arabicState.lastSyncLabel,
            onSaveAndSync = { newUrl ->
                arabicViewModel.saveSyncManifestUrl(newUrl)
                arabicViewModel.syncCatalog { result ->
                    scope.launch {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
            },
            onResetDefault = {
                arabicViewModel.resetSyncManifestUrl()
                scope.launch {
                    snackbarHostState.showSnackbar("URL sinkronisasi dikembalikan ke default.")
                }
            },
            onDismiss = { showArabicSyncSheet = false }
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
