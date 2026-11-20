package com.jadwalshalat.gt5.ui.screens

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.prayer.QiblaCalculator
import com.jadwalshalat.gt5.ui.ShalatVibeUiState
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    state: ShalatVibeUiState,
    onOpenCalibrationSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalShalatVibePalette.current
    val haptic = LocalHapticFeedback.current

    val qiblaBearing = remember(state.latitude, state.longitude) {
        QiblaCalculator.calculateBearingDegrees(state.latitude, state.longitude).toFloat()
    }
    val distanceKm = remember(state.latitude, state.longitude) {
        QiblaCalculator.calculateDistanceKm(state.latitude, state.longitude)
    }
    val cardinal = remember(qiblaBearing) {
        QiblaCalculator.formatCardinalDirection(qiblaBearing.toDouble())
    }
    val magneticDeclination = remember(state.latitude, state.longitude) {
        GeomagneticField(
            state.latitude.toFloat(),
            state.longitude.toFloat(),
            0f,
            System.currentTimeMillis()
        ).declination
    }

    var smoothedAzimuth by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var wasAligned by remember { mutableStateOf(false) }

    DisposableEffect(context, magneticDeclination) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (sensorManager == null || (rotationSensor == null && (accelSensor == null || magnetSensor == null))) {
            hasSensor = false
            return@DisposableEffect onDispose {}
        }

        hasSensor = true
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnet = false
        var initialized = false
        var currentSmoothed = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawMagneticAzimuth: Float
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotMatrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    rawMagneticAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                } else {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMagnet = true
                    }
                    if (!hasGravity || !hasMagnet) return
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (!SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) return
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    rawMagneticAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }

                val trueAzimuth = ((rawMagneticAzimuth + magneticDeclination) % 360f + 360f) % 360f
                if (!initialized) {
                    currentSmoothed = trueAzimuth
                    initialized = true
                } else {
                    val delta = QiblaCalculator.shortestAngleDiff(
                        trueAzimuth.toDouble(),
                        currentSmoothed.toDouble()
                    )
                    currentSmoothed = (((currentSmoothed + 0.16 * delta) % 360.0 + 360.0) % 360.0).toFloat()
                }
                smoothedAzimuth = currentSmoothed
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                sensorAccuracy = accuracy
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val diff = QiblaCalculator.shortestAngleDiff(qiblaBearing.toDouble(), smoothedAzimuth.toDouble())
    val isAligned = hasSensor && abs(diff) <= 3.5

    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasAligned = isAligned
    }

    val statusBg by animateColorAsState(
        targetValue = if (isAligned) palette.statusOkBg else palette.cardSurface,
        animationSpec = tween(250),
        label = "qibla_status_bg"
    )
    val statusBorder by animateColorAsState(
        targetValue = if (isAligned) palette.emeraldPrimary else palette.cardBorder,
        animationSpec = tween(250),
        label = "qibla_status_border"
    )
    val statusTextColor by animateColorAsState(
        targetValue = if (isAligned) palette.statusOkFg else palette.textPrimary,
        animationSpec = tween(250),
        label = "qibla_status_fg"
    )

    val needsCalibration = sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
        sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kompas Kiblat",
                    color = palette.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = palette.emeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.cityName,
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                onClick = onOpenCalibrationSheet,
                shape = CircleShape,
                color = if (needsCalibration) palette.statusWarnBg else palette.cardSurface,
                border = BorderStroke(
                    1.dp,
                    if (needsCalibration) palette.statusWarnFg.copy(alpha = 0.4f) else palette.cardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Kalibrasi",
                        tint = if (needsCalibration) palette.statusWarnFg else palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (needsCalibration) "Kalibrasi" else "Akurasi",
                        color = if (needsCalibration) palette.statusWarnFg else palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Summary Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cardSurface,
            border = BorderStroke(1.dp, palette.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ARAH KA'BAH",
                        color = palette.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f° %s", qiblaBearing, cardinal),
                        style = TextStyle(
                            color = palette.emeraldPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "JARAK KE MAKKAH",
                        color = palette.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f km", distanceKm),
                        style = TextStyle(
                            color = palette.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Watch-Bezel Precision Compass Dial
        WatchBezelCompass(
            azimuthDegrees = smoothedAzimuth,
            qiblaBearingDegrees = qiblaBearing,
            isAligned = isAligned,
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Live Alignment Status Pill
        Surface(
            shape = CircleShape,
            color = statusBg,
            border = BorderStroke(if (isAligned) 1.5.dp else 1.dp, statusBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    !hasSensor -> {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "Sensor kompas tidak tersedia • Sudut %.1f°",
                                qiblaBearing
                            ),
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    isAligned -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = palette.emeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "Menghadap Kiblat • %.0f°",
                                smoothedAzimuth
                            ),
                            style = TextStyle(
                                color = statusTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                    else -> {
                        val turnRight = diff > 0
                        Icon(
                            imageVector = if (turnRight) Icons.AutoMirrored.Rounded.RotateRight else Icons.AutoMirrored.Rounded.RotateLeft,
                            contentDescription = null,
                            tint = palette.goldAccent,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%s %.0f°  •  HP %.0f°",
                                if (turnRight) "Putar kanan" else "Putar kiri",
                                abs(diff),
                                smoothedAzimuth
                            ),
                            style = TextStyle(
                                color = statusTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Pegang HP mendatar sejajar lantai. Arahkan penanda emas di atas ke ikon Ka'bah.",
            color = palette.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun WatchBezelCompass(
    azimuthDegrees: Float,
    qiblaBearingDegrees: Float,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val infinite = rememberInfiniteTransition(label = "aligned_pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val dialBg = if (palette.isDark) Color(0xFF0E1719) else Color(0xFFFFFFFF)
    val outerBezel = if (palette.isDark) Color(0xFF1A292C) else Color(0xFFE6EEEC)
    val tickMinorColor = if (palette.isDark) Color(0xFF475569) else Color(0xFF94A3B8)
    val tickMajorColor = if (palette.isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val cardinalColor = if (palette.isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val northColor = Color(0xFFEF4444)
    val emeraldGlow = palette.emeraldPrimary
    val gold = palette.goldAccent

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = minOf(cx, cy) - 18.dp.toPx()

        // Outer alignment halo glow
        if (isAligned) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        emeraldGlow.copy(alpha = pulseAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = maxRadius + 20.dp.toPx()
                ),
                radius = maxRadius + 20.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        // Luxury Watch Bezel Ring
        drawCircle(
            color = outerBezel,
            radius = maxRadius + 6.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = dialBg,
            radius = maxRadius - 4.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isAligned) emeraldGlow else palette.cardBorder,
            radius = maxRadius + 6.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = if (isAligned) 3.dp.toPx() else 1.5.dp.toPx())
        )

        // Inner precision emerald guide ring
        drawCircle(
            color = emeraldGlow.copy(alpha = if (isAligned) 0.55f else 0.16f),
            radius = maxRadius * 0.64f,
            center = Offset(cx, cy),
            style = Stroke(width = if (isAligned) 2.5.dp.toPx() else 1.2.dp.toPx())
        )

        // Subtle crosshair
        val crossColor = tickMinorColor.copy(alpha = 0.25f)
        drawLine(
            color = crossColor,
            start = Offset(cx - maxRadius * 0.55f, cy),
            end = Offset(cx + maxRadius * 0.55f, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = crossColor,
            start = Offset(cx, cy - maxRadius * 0.55f),
            end = Offset(cx, cy + maxRadius * 0.55f),
            strokeWidth = 1.dp.toPx()
        )

        // Fixed golden indicator at 12 o'clock (top of phone)
        val markerPath = Path().apply {
            moveTo(cx, cy - maxRadius - 14.dp.toPx())
            lineTo(cx - 8.dp.toPx(), cy - maxRadius - 1.dp.toPx())
            lineTo(cx + 8.dp.toPx(), cy - maxRadius - 1.dp.toPx())
            close()
        }
        drawPath(path = markerPath, color = if (isAligned) emeraldGlow else gold)

        // Rotating Compass Dial (North stays pointing to True North)
        rotate(degrees = -azimuthDegrees, pivot = Offset(cx, cy)) {
            for (deg in 0 until 360 step 5) {
                val rad = Math.toRadians(deg.toDouble())
                val isCardinal = deg % 90 == 0
                val isMajor = deg % 30 == 0
                val tickLen = when {
                    isCardinal -> 14.dp.toPx()
                    isMajor -> 10.dp.toPx()
                    else -> 5.dp.toPx()
                }
                val outerR = maxRadius - 6.dp.toPx()
                val innerR = outerR - tickLen
                drawLine(
                    color = when {
                        deg == 0 -> northColor
                        isMajor -> tickMajorColor
                        else -> tickMinorColor.copy(alpha = 0.65f)
                    },
                    start = Offset(cx + (sin(rad) * innerR).toFloat(), cy - (cos(rad) * innerR).toFloat()),
                    end = Offset(cx + (sin(rad) * outerR).toFloat(), cy - (cos(rad) * outerR).toFloat()),
                    strokeWidth = if (isCardinal) 2.4.dp.toPx() else if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Cardinal letters U / T / S / B
            val labelR = maxRadius - 30.dp.toPx()
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 15.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val labels = listOf(
                Triple("U", Offset(cx, cy - labelR + 5.dp.toPx()), northColor),
                Triple("T", Offset(cx + labelR, cy + 5.dp.toPx()), cardinalColor),
                Triple("S", Offset(cx, cy + labelR + 5.dp.toPx()), cardinalColor),
                Triple("B", Offset(cx - labelR, cy + 5.dp.toPx()), cardinalColor)
            )
            for ((txt, pos, col) in labels) {
                textPaint.color = col.toArgb()
                drawContext.canvas.nativeCanvas.drawText(txt, pos.x, pos.y, textPaint)
            }

            // Qibla Needle + Ka'bah Marker rotated to Qibla bearing
            rotate(degrees = qiblaBearingDegrees, pivot = Offset(cx, cy)) {
                val tipY = cy - maxRadius + 38.dp.toPx()
                val tailY = cy + maxRadius * 0.35f

                // Front luminous emerald needle
                val needlePath = Path().apply {
                    moveTo(cx, tipY)
                    lineTo(cx - 10.dp.toPx(), cy)
                    lineTo(cx, cy - 4.dp.toPx())
                    lineTo(cx + 10.dp.toPx(), cy)
                    close()
                }
                drawPath(
                    path = needlePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(emeraldGlow, emeraldGlow.copy(alpha = 0.65f)),
                        startY = tipY,
                        endY = cy
                    )
                )

                // Counterweight tail
                val tailPath = Path().apply {
                    moveTo(cx, tailY)
                    lineTo(cx - 6.dp.toPx(), cy)
                    lineTo(cx + 6.dp.toPx(), cy)
                    close()
                }
                drawPath(
                    path = tailPath,
                    color = tickMinorColor.copy(alpha = 0.45f)
                )

                // Crisp Ka'bah icon positioned near tip
                val kaabaSize = 19.dp.toPx()
                val kaabaTopLeft = Offset(cx - kaabaSize / 2f, tipY - kaabaSize - 4.dp.toPx())
                drawRect(
                    color = Color(0xFF111827),
                    topLeft = kaabaTopLeft,
                    size = Size(kaabaSize, kaabaSize)
                )
                drawRect(
                    color = gold,
                    topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + kaabaSize * 0.25f),
                    size = Size(kaabaSize, kaabaSize * 0.16f)
                )
                drawRect(
                    color = gold.copy(alpha = 0.7f),
                    topLeft = kaabaTopLeft,
                    size = Size(kaabaSize, kaabaSize),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Center watch pivot cap
        drawCircle(color = outerBezel, radius = 9.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = if (isAligned) emeraldGlow else gold, radius = 5.dp.toPx(), center = Offset(cx, cy))
    }
}
