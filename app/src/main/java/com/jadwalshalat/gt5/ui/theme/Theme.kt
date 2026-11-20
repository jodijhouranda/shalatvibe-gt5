package com.jadwalshalat.gt5.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.model.PrayerType

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("Ikuti Sistem", "Otomatis menyesuaikan tema HP Anda"),
    LIGHT("Serene Light", "Tampilan terang bersih & lega"),
    DARK("AMOLED Dark", "Hitam pekat selaras layar Huawei Watch GT 5");

    companion object {
        fun fromName(name: String?): ThemeMode {
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }
    }
}

@Immutable
data class ShalatVibePalette(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val cardSurfaceElevated: Color,
    val cardBorder: Color,
    val highlightSurface: Color,
    val highlightBorder: Color,
    val emeraldPrimary: Color,
    val emeraldSoft: Color,
    val goldAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val navBarBg: Color,
    val statusOkBg: Color,
    val statusOkFg: Color,
    val statusWarnBg: Color,
    val statusWarnFg: Color,
    val pillBg: Color
)

@Immutable
data class HeroAtmosphere(
    val startColor: Color,
    val midColor: Color,
    val endColor: Color,
    val arcTrackColor: Color,
    val arcProgressStart: Color,
    val arcProgressEnd: Color,
    val orbColor: Color,
    val phaseLabel: String
)

private val LightPalette = ShalatVibePalette(
    isDark = false,
    background = Color(0xFFF5F7F6),
    cardSurface = Color(0xFFFFFFFF),
    cardSurfaceElevated = Color(0xFFFBFCFC),
    cardBorder = Color(0xFFE4EAE8),
    highlightSurface = Color(0xFFE5F6F1),
    highlightBorder = Color(0xFF0D9488),
    emeraldPrimary = Color(0xFF0D6E66),
    emeraldSoft = Color(0xFFCCFBF1),
    goldAccent = Color(0xFFD97706),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF52606D),
    textMuted = Color(0xFF94A3B8),
    navBarBg = Color(0xFFFFFFFF),
    statusOkBg = Color(0xFFDCFCE7),
    statusOkFg = Color(0xFF15803D),
    statusWarnBg = Color(0xFFFEF3C7),
    statusWarnFg = Color(0xFFB45309),
    pillBg = Color(0xFFE8EFED)
)

private val DarkPalette = ShalatVibePalette(
    isDark = true,
    background = Color(0xFF090E0F),
    cardSurface = Color(0xFF11191B),
    cardSurfaceElevated = Color(0xFF172326),
    cardBorder = Color(0xFF1F2F33),
    highlightSurface = Color(0xFF0F2B26),
    highlightBorder = Color(0xFF10B981),
    emeraldPrimary = Color(0xFF10B981),
    emeraldSoft = Color(0xFF0F3B31),
    goldAccent = Color(0xFFF59E0B),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    navBarBg = Color(0xFF0D1416),
    statusOkBg = Color(0xFF0D2E22),
    statusOkFg = Color(0xFF34D399),
    statusWarnBg = Color(0xFF332207),
    statusWarnFg = Color(0xFFFBBF24),
    pillBg = Color(0xFF182427)
)

val LocalShalatVibePalette = staticCompositionLocalOf { DarkPalette }

fun heroAtmosphereFor(phase: PrayerType, isDark: Boolean): HeroAtmosphere {
    return when (phase) {
        PrayerType.SUBUH, PrayerType.TERBIT -> HeroAtmosphere(
            startColor = Color(0xFF0D3B46),
            midColor = Color(0xFF0F5257),
            endColor = if (isDark) Color(0xFF091B20) else Color(0xFF0C4A52),
            arcTrackColor = Color.White.copy(alpha = 0.16f),
            arcProgressStart = Color(0xFF34D399),
            arcProgressEnd = Color(0xFFFBBF24),
            orbColor = Color(0xFFFDE68A),
            phaseLabel = "Fajar & Pagi"
        )
        PrayerType.DZUHUR -> HeroAtmosphere(
            startColor = Color(0xFF0B615A),
            midColor = Color(0xFF0E746B),
            endColor = if (isDark) Color(0xFF072B2A) else Color(0xFF084B47),
            arcTrackColor = Color.White.copy(alpha = 0.18f),
            arcProgressStart = Color(0xFF6EE7B7),
            arcProgressEnd = Color(0xFFFCD34D),
            orbColor = Color(0xFFFEF08A),
            phaseLabel = "Tengah Hari"
        )
        PrayerType.ASHAR -> HeroAtmosphere(
            startColor = Color(0xFF0F5750),
            midColor = Color(0xFF1A5E52),
            endColor = if (isDark) Color(0xFF1A2A24) else Color(0xFF11463E),
            arcTrackColor = Color.White.copy(alpha = 0.18f),
            arcProgressStart = Color(0xFF34D399),
            arcProgressEnd = Color(0xFFF59E0B),
            orbColor = Color(0xFFFBBF24),
            phaseLabel = "Sore Hari"
        )
        PrayerType.MAGHRIB -> HeroAtmosphere(
            startColor = Color(0xFF1B3B3A),
            midColor = Color(0xFF3C3332),
            endColor = if (isDark) Color(0xFF141821) else Color(0xFF232531),
            arcTrackColor = Color.White.copy(alpha = 0.16f),
            arcProgressStart = Color(0xFFF59E0B),
            arcProgressEnd = Color(0xFFFB7185),
            orbColor = Color(0xFFFDBA74),
            phaseLabel = "Senja Maghrib"
        )
        PrayerType.ISYA -> HeroAtmosphere(
            startColor = Color(0xFF0B3534),
            midColor = Color(0xFF0C232A),
            endColor = Color(0xFF081217),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFF10B981),
            arcProgressEnd = Color(0xFF6EE7B7),
            orbColor = Color(0xFFA7F3D0),
            phaseLabel = "Malam Tenang"
        )
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D6E66),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EFED),
    onSurfaceVariant = Color(0xFF52606D),
    outline = Color(0xFFD1DCD9)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF0F3B31),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF1F1602),
    background = Color(0xFF090E0F),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF11191B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF182427),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF24353A)
)

private val ShalatVibeTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun ShalatVibeTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val palette = if (useDark) DarkPalette else LightPalette
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalShalatVibePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShalatVibeTypography,
            content = content
        )
    }
}
