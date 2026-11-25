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
    val hairline: Color,
    val highlightSurface: Color,
    val highlightBorder: Color,
    val emeraldPrimary: Color,
    val emeraldBright: Color,
    val emeraldSoft: Color,
    val goldAccent: Color,
    val goldSoft: Color,
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
    background = Color(0xFFF3F5F3),
    cardSurface = Color(0xFFFFFFFF),
    cardSurfaceElevated = Color(0xFFF7F9F8),
    cardBorder = Color(0xFFE2E8E5),
    hairline = Color(0x100F1C19),
    highlightSurface = Color(0xFFDDF2EB),
    highlightBorder = Color(0xFF0B6E62),
    emeraldPrimary = Color(0xFF0B6E62),
    emeraldBright = Color(0xFF10B981),
    emeraldSoft = Color(0x1C0B6E62),
    goldAccent = Color(0xFFB97316),
    goldSoft = Color(0x1EB97316),
    textPrimary = Color(0xFF0F1C19),
    textSecondary = Color(0xFF4B5D59),
    textMuted = Color(0xFF849490),
    navBarBg = Color(0xFFFAFBFA),
    statusOkBg = Color(0xFFDCFCE7),
    statusOkFg = Color(0xFF15803D),
    statusWarnBg = Color(0xFFFEF3C7),
    statusWarnFg = Color(0xFFB45309),
    pillBg = Color(0xFFE7ECE8)
)

private val DarkPalette = ShalatVibePalette(
    isDark = true,
    background = Color(0xFF060A0B),
    cardSurface = Color(0xFF131D20),
    cardSurfaceElevated = Color(0xFF19262A),
    cardBorder = Color(0xFF1F2D30),
    hairline = Color(0x10FFFFFF),
    highlightSurface = Color(0xFF0C2A24),
    highlightBorder = Color(0xFF10B981),
    emeraldPrimary = Color(0xFF10B981),
    emeraldBright = Color(0xFF34D399),
    emeraldSoft = Color(0x2410B981),
    goldAccent = Color(0xFFF5B041),
    goldSoft = Color(0x22F5B041),
    textPrimary = Color(0xFFF1F6F5),
    textSecondary = Color(0xFF98A9A5),
    textMuted = Color(0xFF5A6B67),
    navBarBg = Color(0xFF0C1315),
    statusOkBg = Color(0xFF0D2E22),
    statusOkFg = Color(0xFF34D399),
    statusWarnBg = Color(0xFF332207),
    statusWarnFg = Color(0xFFFBBF24),
    pillBg = Color(0xFF0F1719)
)

val LocalShalatVibePalette = staticCompositionLocalOf { DarkPalette }

fun heroAtmosphereFor(phase: PrayerType, isDark: Boolean): HeroAtmosphere {
    return when (phase) {
        PrayerType.SUBUH, PrayerType.TERBIT -> HeroAtmosphere(
            startColor = Color(0xFF0D3C47),
            midColor = Color(0xFF0F5258),
            endColor = if (isDark) Color(0xFF071D23) else Color(0xFF0B4148),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFF34D399),
            arcProgressEnd = Color(0xFFFBBF24),
            orbColor = Color(0xFFFDE68A),
            phaseLabel = "Fajar & Pagi"
        )
        PrayerType.DZUHUR -> HeroAtmosphere(
            startColor = Color(0xFF0B5E56),
            midColor = Color(0xFF0D4B47),
            endColor = if (isDark) Color(0xFF072426) else Color(0xFF093C3B),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFF6EE7B7),
            arcProgressEnd = Color(0xFFFCD34D),
            orbColor = Color(0xFFFEF08A),
            phaseLabel = "Tengah Hari"
        )
        PrayerType.ASHAR -> HeroAtmosphere(
            startColor = Color(0xFF0E554E),
            midColor = Color(0xFF245746),
            endColor = if (isDark) Color(0xFF16241E) else Color(0xFF123F36),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFF34D399),
            arcProgressEnd = Color(0xFFF5B041),
            orbColor = Color(0xFFFBBF24),
            phaseLabel = "Sore Hari"
        )
        PrayerType.MAGHRIB -> HeroAtmosphere(
            startColor = Color(0xFF1E3B3B),
            midColor = Color(0xFF483333),
            endColor = if (isDark) Color(0xFF161822) else Color(0xFF26232E),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFFF59E0B),
            arcProgressEnd = Color(0xFFFB7185),
            orbColor = Color(0xFFFDBA74),
            phaseLabel = "Senja Maghrib"
        )
        PrayerType.ISYA -> HeroAtmosphere(
            startColor = Color(0xFF0B3135),
            midColor = Color(0xFF0D202A),
            endColor = Color(0xFF060D12),
            arcTrackColor = Color.White.copy(alpha = 0.14f),
            arcProgressStart = Color(0xFF10B981),
            arcProgressEnd = Color(0xFF6EE7B7),
            orbColor = Color(0xFFA7F3D0),
            phaseLabel = "Malam Tenang"
        )
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B6E62),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2EB),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFB97316),
    onSecondary = Color.White,
    background = Color(0xFFF3F5F3),
    onBackground = Color(0xFF0F1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1C19),
    surfaceVariant = Color(0xFFE7ECE8),
    onSurfaceVariant = Color(0xFF4B5D59),
    outline = Color(0xFFD5DDD9)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF0C2A24),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFF5B041),
    onSecondary = Color(0xFF1F1602),
    background = Color(0xFF060A0B),
    onBackground = Color(0xFFF1F6F5),
    surface = Color(0xFF131D20),
    onSurface = Color(0xFFF1F6F5),
    surfaceVariant = Color(0xFF0F1719),
    onSurfaceVariant = Color(0xFF98A9A5),
    outline = Color(0xFF1F2D30)
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
