package com.jadwalshalat.gt5.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwalshalat.gt5.arabic.PdfReadingMode
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private class PdfRenderSession(file: File) {
    val descriptor: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer: PdfRenderer = PdfRenderer(descriptor)
    val pageCount: Int = renderer.pageCount
    val mutex = Mutex()

    suspend fun renderPage(index: Int, targetWidthPx: Int): Pair<ImageBitmap, Float>? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (index < 0 || index >= pageCount) return@withLock null
                runCatching {
                    renderer.openPage(index).use { page ->
                        val aspect = page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f)
                        val width = targetWidthPx.coerceIn(720, 1800)
                        val height = (width / aspect).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap.asImageBitmap() to aspect
                    }
                }.getOrNull()
            }
        }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

@Composable
fun PdfDocumentViewer(
    pdfFile: File,
    readingMode: PdfReadingMode,
    modifier: Modifier = Modifier,
    bottomActionContent: @Composable () -> Unit = {}
) {
    val palette = LocalShalatVibePalette.current
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val renderWidthPx = remember(screenWidthDp, density) {
        with(density) { (screenWidthDp.dp.toPx() * 2.0f).toInt().coerceIn(900, 1700) }
    }

    var session by remember(pdfFile.absolutePath) { mutableStateOf<PdfRenderSession?>(null) }
    var pageCount by remember(pdfFile.absolutePath) { mutableIntStateOf(0) }
    var openError by remember(pdfFile.absolutePath) { mutableStateOf<String?>(null) }
    val renderedPages = remember(pdfFile.absolutePath) { mutableStateMapOf<Int, Pair<ImageBitmap, Float>>() }

    DisposableEffect(pdfFile.absolutePath) {
        val created = runCatching { PdfRenderSession(pdfFile) }
            .onFailure { openError = "Gagal membuka file PDF." }
            .getOrNull()
        session = created
        pageCount = created?.pageCount ?: 0
        onDispose {
            created?.close()
            session = null
            renderedPages.clear()
        }
    }

    val listState = rememberLazyListState()
    val currentPageIndex by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtLeast(1) }
    }

    var zoomScale by remember(pdfFile.absolutePath) { mutableFloatStateOf(1f) }
    var panOffset by remember(pdfFile.absolutePath) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 2.8f)
        zoomScale = nextScale
        panOffset = if (nextScale > 1.02f) {
            Offset(
                x = (panOffset.x + offsetChange.x).coerceIn(-420f * (nextScale - 1f), 420f * (nextScale - 1f)),
                y = (panOffset.y + offsetChange.y).coerceIn(-300f * (nextScale - 1f), 300f * (nextScale - 1f))
            )
        } else {
            Offset.Zero
        }
    }

    val pageColorFilter = remember(readingMode) {
        when (readingMode) {
            PdfReadingMode.ORIGINAL -> null
            PdfReadingMode.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0f, 0f, 0f, 4f,
                        0f, 0.91f, 0f, 0f, 0f,
                        0f, 0f, 0.80f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            PdfReadingMode.DARK_INVERT -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.88f, 0f, 0f, 0f, 236f,
                        0f, -0.88f, 0f, 0f, 240f,
                        0f, 0f, -0.86f, 0f, 238f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        when {
            openError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = openError ?: "", color = palette.textSecondary, fontSize = 13.sp)
                }
            }
            pageCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.emeraldPrimary)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 76.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 1.65f
                                    }
                                }
                            )
                        }
                        .then(
                            if (zoomScale > 1.02f) Modifier.transformable(transformState) else Modifier
                        )
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                ) {
                    items(pageCount, key = { it }) { pageIdx ->
                        val cached = renderedPages[pageIdx]
                        LaunchedEffect(session, pageIdx) {
                            if (cached == null) {
                                val s = session ?: return@LaunchedEffect
                                val rendered = s.renderPage(pageIdx, renderWidthPx)
                                if (rendered != null) {
                                    renderedPages[pageIdx] = rendered
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (readingMode == PdfReadingMode.DARK_INVERT) Color(0xFF141C1E) else Color.White,
                            border = BorderStroke(1.dp, palette.cardBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (cached != null) {
                                Image(
                                    bitmap = cached.first,
                                    contentDescription = "Halaman PDF ${pageIdx + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    colorFilter = pageColorFilter,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(cached.second)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(420.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = palette.emeraldPrimary,
                                        modifier = Modifier.size(26.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                        }
                    }

                    item(key = "pdf_bottom_cta") {
                        Spacer(modifier = Modifier.height(6.dp))
                        bottomActionContent()
                    }
                }

                // Floating Page Indicator + Zoom Controls Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.cardSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "Hal $currentPageIndex / $pageCount",
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = palette.cardSurface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Surface(
                                onClick = {
                                    zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                    if (zoomScale <= 1.02f) panOffset = Offset.Zero
                                },
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = "Perkecil PDF",
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Surface(
                                onClick = {
                                    zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f)
                                },
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Perbesar PDF",
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(16.dp)
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
