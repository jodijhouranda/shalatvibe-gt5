package com.jadwalshalat.gt5

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.jadwalshalat.gt5.ai.configureAppCheck
import com.jadwalshalat.gt5.arabic.ArabicStudyViewModel
import com.jadwalshalat.gt5.learning.LearningViewModel
import com.jadwalshalat.gt5.location.LocationHelper
import com.jadwalshalat.gt5.tafsir.TafsirViewModel
import com.jadwalshalat.gt5.ui.ShalatVibeApp
import com.jadwalshalat.gt5.ui.ShalatVibeViewModel
import com.jadwalshalat.gt5.ui.theme.ShalatVibeTheme
import com.jadwalshalat.gt5.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * Activity utama ShalatVibe GT5 v2.3 berbasis Kotlin + Jetpack Compose + Material 3.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ShalatVibeViewModel by viewModels()
    private val arabicViewModel: ArabicStudyViewModel by viewModels()
    private val learningViewModel: LearningViewModel by viewModels()
    private val tafsirViewModel: TafsirViewModel by viewModels()
    private val snackbarHostState = SnackbarHostState()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onAppResumed()
            lifecycleScope.launch {
                snackbarHostState.showSnackbar(
                    if (granted) "Izin notifikasi aktif."
                    else "Izin notifikasi ditolak; GT 5 tidak dapat menerima alert."
                )
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (LocationHelper.hasLocationPermission(this)) {
                applyGpsAndNotify()
            } else {
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        "Izin lokasi ditolak. Anda tetap bisa memilih kota secara offline."
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val arabicState by arabicViewModel.uiState.collectAsStateWithLifecycle()
            val learningState by learningViewModel.uiState.collectAsStateWithLifecycle()
            val tafsirState by tafsirViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (state.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            val hostState = remember { snackbarHostState }

            ShalatVibeTheme(themeMode = state.themeMode) {
                ShalatVibeApp(
                    state = state,
                    viewModel = viewModel,
                    arabicState = arabicState,
                    arabicViewModel = arabicViewModel,
                    learningState = learningState,
                    learningViewModel = learningViewModel,
                    tafsirState = tafsirState,
                    tafsirViewModel = tafsirViewModel,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onOpenExactAlarmSettings = ::openExactAlarmSettings,
                    onRequestIgnoreBattery = ::requestIgnoreBatteryOptimization,
                    onRequestGpsLocation = ::onGpsRequested,
                    snackbarHostState = hostState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppDetailsSettings()
        }
    }

    private fun onGpsRequested() {
        if (LocationHelper.hasLocationPermission(this)) {
            applyGpsAndNotify()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun applyGpsAndNotify() {
        val city = viewModel.applyGpsLocation()
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                if (city != null) "Lokasi GPS diperbarui: $city"
                else "Lokasi GPS terakhir belum tersedia. Pastikan GPS aktif atau pilih kota manual."
            )
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                openAppDetailsSettings()
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            openBatterySettings()
        }
    }

    private fun openBatterySettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}
