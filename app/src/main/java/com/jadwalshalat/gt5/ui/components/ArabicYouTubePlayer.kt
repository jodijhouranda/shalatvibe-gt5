package com.jadwalshalat.gt5.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import com.jadwalshalat.gt5.ui.theme.LocalShalatVibePalette

@Stable
class YouTubePlayerController(
    initialStartSec: Int,
    initialEndSec: Int
) {
    internal var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var isReady by mutableStateOf(false)
        internal set
    var isPlaying by mutableStateOf(false)
        internal set
    var currentSeconds by mutableIntStateOf(initialStartSec)
        internal set
    var playbackRate by mutableFloatStateOf(1.0f)
        internal set
    var loopEnabled by mutableStateOf(true)
        internal set
    var windowStartSec by mutableIntStateOf(initialStartSec)
        internal set
    var windowEndSec by mutableIntStateOf(initialEndSec)
        internal set
    var embedErrorCode by mutableStateOf<Int?>(null)
        internal set

    fun play() {
        evalJs("if(window.ytCtrl){ytCtrl.play();}")
    }

    fun pause() {
        evalJs("if(window.ytCtrl){ytCtrl.pause();}")
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(seconds: Int, autoPlay: Boolean = true) {
        val safe = seconds.coerceAtLeast(0)
        currentSeconds = safe
        evalJs("if(window.ytCtrl){ytCtrl.seek($safe, $autoPlay);}")
    }

    fun setSpeed(rate: Float) {
        playbackRate = rate
        evalJs("if(window.ytCtrl){ytCtrl.setRate($rate);}")
    }

    fun setSegmentWindow(startSec: Int, endSec: Int, seekNow: Boolean = true) {
        windowStartSec = startSec.coerceAtLeast(0)
        windowEndSec = endSec.coerceAtLeast(windowStartSec)
        evalJs("if(window.ytCtrl){ytCtrl.setWindow($windowStartSec, $windowEndSec, $loopEnabled);}")
        if (seekNow) {
            seekTo(windowStartSec, autoPlay = true)
        }
    }

    fun setLoop(enabled: Boolean) {
        loopEnabled = enabled
        evalJs("if(window.ytCtrl){ytCtrl.setWindow($windowStartSec, $windowEndSec, $loopEnabled);}")
    }

    fun replayWindow() {
        seekTo(windowStartSec, autoPlay = true)
    }

    internal fun onBridgeReady() {
        mainHandler.post {
            isReady = true
            evalJs("if(window.ytCtrl){ytCtrl.setWindow($windowStartSec, $windowEndSec, $loopEnabled); ytCtrl.setRate($playbackRate);}")
        }
    }

    internal fun onBridgeState(state: Int) {
        mainHandler.post {
            // YT.PlayerState.PLAYING == 1
            isPlaying = (state == 1)
        }
    }

    internal fun onBridgeTime(sec: Int) {
        mainHandler.post {
            currentSeconds = sec
        }
    }

    internal fun onBridgeError(code: Int) {
        mainHandler.post {
            embedErrorCode = code
            isPlaying = false
        }
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            webView?.evaluateJavascript(script, null)
        }
    }

    fun release() {
        webView?.let { wv ->
            wv.loadUrl("about:blank")
            wv.destroy()
        }
        webView = null
    }
}

private class YouTubeJsBridge(private val controller: YouTubePlayerController) {
    @JavascriptInterface
    fun onReady() = controller.onBridgeReady()

    @JavascriptInterface
    fun onStateChange(state: Int) = controller.onBridgeState(state)

    @JavascriptInterface
    fun onTimeUpdate(sec: Int) = controller.onBridgeTime(sec)

    @JavascriptInterface
    fun onError(code: Int) = controller.onBridgeError(code)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArabicYouTubePlayerView(
    videoId: String,
    controller: YouTubePlayerController,
    onOpenExternalYouTube: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalShalatVibePalette.current
    val htmlContent = remember(videoId, controller.windowStartSec) {
        buildYouTubeIframeHtml(
            videoId = videoId,
            startSec = controller.windowStartSec,
            endSec = controller.windowEndSec
        )
    }

    LaunchedEffect(controller.windowStartSec, controller.windowEndSec, controller.loopEnabled) {
        if (controller.isReady) {
            controller.setLoop(controller.loopEnabled)
        }
    }

    DisposableEffect(videoId) {
        onDispose {
            controller.release()
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(YouTubeJsBridge(controller), "AndroidBridge")
                    controller.webView = this
                    loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controller.embedErrorCode != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C1517))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircleOutline,
                    contentDescription = null,
                    tint = palette.emeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Video ini membatasi pemutaran embed atau internet belum aktif.",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenExternalYouTube,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.emeraldPrimary,
                        contentColor = Color(0xFF03231D)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buka YouTube (${ArabicCatalogParser.formatTimestamp(controller.windowStartSec)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun buildYouTubeIframeHtml(
    videoId: String,
    startSec: Int,
    endSec: Int
): String {
    val safeVideoId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
            #player { position:fixed; top:0; left:0; width:100%; height:100%; }
          </style>
        </head>
        <body>
          <div id="player"></div>
          <script>
            var player = null;
            var winStart = $startSec;
            var winEnd = $endSec;
            var loopOn = true;
            var ticker = null;

            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            function onYouTubeIframeAPIReady() {
              player = new YT.Player('player', {
                videoId: '$safeVideoId',
                host: 'https://www.youtube-nocookie.com',
                playerVars: {
                  playsinline: 1,
                  rel: 0,
                  modestbranding: 1,
                  start: winStart,
                  controls: 1,
                  fs: 1,
                  origin: 'https://www.youtube-nocookie.com'
                },
                events: {
                  'onReady': function() {
                    if (window.AndroidBridge) AndroidBridge.onReady();
                    startTicker();
                  },
                  'onStateChange': function(e) {
                    if (window.AndroidBridge) AndroidBridge.onStateChange(e.data);
                  },
                  'onError': function(e) {
                    if (window.AndroidBridge) AndroidBridge.onError(e.data);
                  }
                }
              });
            }

            function startTicker() {
              if (ticker) clearInterval(ticker);
              ticker = setInterval(function() {
                if (!player || typeof player.getCurrentTime !== 'function') return;
                var t = Math.floor(player.getCurrentTime());
                if (window.AndroidBridge) AndroidBridge.onTimeUpdate(t);
                if (loopOn && winEnd > winStart && t >= winEnd && player.getPlayerState() === 1) {
                  player.seekTo(winStart, true);
                }
              }, 400);
            }

            window.ytCtrl = {
              play: function() { if (player && player.playVideo) player.playVideo(); },
              pause: function() { if (player && player.pauseVideo) player.pauseVideo(); },
              seek: function(sec, autoPlay) {
                if (player && player.seekTo) {
                  player.seekTo(sec, true);
                  if (autoPlay && player.playVideo) player.playVideo();
                }
              },
              setRate: function(rate) {
                if (player && player.setPlaybackRate) player.setPlaybackRate(rate);
              },
              setWindow: function(s, e, lp) {
                winStart = s;
                winEnd = e;
                loopOn = lp;
              }
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
