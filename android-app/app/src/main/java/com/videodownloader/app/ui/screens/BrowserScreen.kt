package com.videodownloader.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.components.GlassButton
import com.videodownloader.app.ui.components.GlassCard
import com.videodownloader.app.ui.components.GlassIconButton
import com.videodownloader.app.ui.components.GlassTextField
import com.videodownloader.app.ui.components.NovaAccentPurple
import com.videodownloader.app.ui.components.NovaElevatedButtonBorderBrush
import com.videodownloader.app.ui.components.NovaElevatedButtonBrush
import com.videodownloader.app.ui.components.NovaIcons
import com.videodownloader.app.ui.theme.GlassAcrylicElevated
import com.videodownloader.app.ui.theme.NeonAmber
import com.videodownloader.app.ui.theme.NeonCyan
import com.videodownloader.app.ui.theme.NeonEmerald
import com.videodownloader.app.ui.theme.NeonPink
import com.videodownloader.app.ui.theme.NeonViolet
import com.videodownloader.app.ui.theme.TextPrimary
import com.videodownloader.app.ui.theme.TextSecondary
import com.videodownloader.app.ui.theme.TextTertiary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: DownloadViewModel,
    contentPadding: PaddingValues
) {
    val focusManager = LocalFocusManager.current

    var currentUrl by rememberSaveable { mutableStateOf("https://m.youtube.com") }
    var inputUrl by rememberSaveable { mutableStateOf("https://m.youtube.com") }
    var pageTitle by rememberSaveable { mutableStateOf("YouTube") }
    var webProgress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }

    val externalPlatformUrl by viewModel.browserUrlToLoad.collectAsState()
    androidx.compose.runtime.LaunchedEffect(externalPlatformUrl) {
        externalPlatformUrl?.let { url ->
            if (url.isNotBlank() && url != currentUrl) {
                currentUrl = url
                inputUrl = url
                webViewInstance?.loadUrl(url)
            }
        }
    }

    // Download modal setup
    com.videodownloader.app.ui.components.DownloadSetupDialog(viewModel = viewModel)

    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Browser Address Bar - ChatGPT Stadium Capsule Style (IMG_20260925_093355.jpg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlassIconButton(
                        onClick = { webViewInstance?.goBack() },
                        icon = Icons.Default.ArrowBack,
                        size = 36.dp,
                        tint = if (canGoBack) Color.White else TextTertiary
                    )
                    GlassIconButton(
                        onClick = { webViewInstance?.goForward() },
                        icon = Icons.Default.ArrowForward,
                        size = 36.dp,
                        tint = if (canGoForward) Color.White else TextTertiary
                    )
                    GlassIconButton(
                        onClick = {
                            if (isLoading) webViewInstance?.stopLoading() else webViewInstance?.reload()
                        },
                        icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        size = 36.dp,
                        tint = Color.White
                    )

                    // Stadium Capsule Address Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF22232A),
                                        Color(0xFF191A20)
                                    )
                                )
                            )
                            .border(
                                width = 1.1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0x45FFFFFF),
                                        Color(0x18FFFFFF)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NovaAccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (inputUrl.isEmpty()) {
                                    Text(
                                        text = "Search or type URL",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(NovaAccentPurple),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            focusManager.clearFocus()
                                            var target = inputUrl.trim()
                                            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                                target = if (target.contains(".") && !target.contains(" ")) {
                                                    "https://$target"
                                                } else {
                                                    "https://www.google.com/search?q=" + java.net.URLEncoder.encode(target, "UTF-8")
                                                }
                                            }
                                            currentUrl = target
                                            webViewInstance?.loadUrl(target)
                                        }
                                    )
                                )
                            }

                            // AdBlock Active Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3010B981))
                                    .border(0.8.dp, Color(0x6010B981), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🛡️ AdBlock",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7),
                                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp")
                                )
                            }
                        }
                    }

                    GlassIconButton(
                        onClick = {
                            val home = "https://m.youtube.com"
                            inputUrl = home
                            currentUrl = home
                            webViewInstance?.loadUrl(home)
                        },
                        icon = Icons.Default.Home,
                        size = 36.dp
                    )
                }

                // Web Loading Progress
                if (isLoading && webProgress < 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = webProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = NovaAccentPurple,
                        trackColor = Color(0x20FFFFFF)
                    )
                }

                // Quick Platform Bookmarks (NewPipe / Snaptube style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bookmarks = listOf(
                        "YouTube" to "https://m.youtube.com",
                        "Instagram" to "https://www.instagram.com",
                        "TikTok" to "https://www.tiktok.com",
                        "Twitter / X" to "https://x.com",
                        "Facebook" to "https://m.facebook.com",
                        "Reddit" to "https://www.reddit.com"
                    )

                    bookmarks.forEach { (name, url) ->
                        val isCurrent = currentUrl.contains(name.lowercase().take(5))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) Color(0x35A855F7) else Color(0x18FFFFFF))
                                .border(1.dp, if (isCurrent) Color(0x80A855F7) else Color(0x25FFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    inputUrl = url
                                    currentUrl = url
                                    webViewInstance?.loadUrl(url)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                color = if (isCurrent) Color.White else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Embedded Android WebView (Full HTML5 video playback like NewPipe)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = false
                            allowContentAccess = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        addJavascriptInterface(
                            BrowserMediaBridge(
                                getTopUrl = { webViewInstance?.url ?: currentUrl },
                                onPlayingStateChanged = { playing, pageUrl ->
                                    isVideoPlaying = playing
                                    if (!pageUrl.isNullOrBlank()) {
                                        val cleaned = cleanVideoWatchUrl(pageUrl)
                                        if (cleaned.isNotBlank()) {
                                            activeVideoUrl = cleaned
                                        }
                                    }
                                },
                                onUrlChanged = { newUrl ->
                                    currentUrl = newUrl
                                    inputUrl = newUrl
                                    val cleaned = cleanVideoWatchUrl(newUrl)
                                    if (cleaned.isNotBlank()) {
                                        activeVideoUrl = cleaned
                                    }
                                }
                            ),
                            "NovaMediaBridge"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val targetUrl = request?.url?.toString() ?: return false
                                val uri = request.url ?: return false
                                val scheme = uri.scheme?.lowercase() ?: ""

                                if (scheme != "http" && scheme != "https") {
                                    try {
                                        val intent = android.content.Intent.parseUri(targetUrl, android.content.Intent.URI_INTENT_SCHEME)
                                        view?.context?.startActivity(intent)
                                    } catch (ignored: Exception) {}
                                    return true
                                }

                                // GitHub should ONLY be accessed when explicitly clicked from developer profile/page in Settings tab
                                val host = uri.host?.lowercase() ?: ""
                                if (host == "github.com" || host.endsWith(".github.com") || host.endsWith(".github.io")) {
                                    return true // block browser redirect to github
                                }

                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    inputUrl = it
                                    currentUrl = it
                                    val cleaned = cleanVideoWatchUrl(it)
                                    if (cleaned.isNotBlank()) {
                                        activeVideoUrl = cleaned
                                    }
                                    if (it.contains("youtube.com") || it.contains("youtu.be")) {
                                        // Layer 2: Inject safe fetch hook before player scripts run
                                        view?.evaluateJavascript(YOUTUBE_AD_STRIPPER_EARLY_JS, null)
                                    }
                                    if (it.contains("youtube.com/watch") || it.contains("youtu.be/") ||
                                        it.contains("youtube.com/shorts") ||
                                        it.contains("instagram.com/reel") || it.contains("instagram.com/p/") ||
                                        it.contains("tiktok.com/@") || it.contains("twitter.com/") || it.contains("x.com/") ||
                                        it.contains("facebook.com/reel") || it.contains("facebook.com/watch") ||
                                        it.contains("facebook.com/share") || it.contains("fb.watch/") || it.contains("facebook.com/story")
                                    ) {
                                        detectedMediaUrl = if (cleaned.isNotBlank()) cleaned else it
                                    }
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                pageTitle = view?.title ?: "Web Browser"
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                url?.let {
                                    val cleaned = cleanVideoWatchUrl(it)
                                    if (cleaned.isNotBlank()) {
                                        activeVideoUrl = cleaned
                                    }
                                    if (it.contains("youtube.com") || it.contains("youtu.be")) {
                                        view?.evaluateJavascript(YOUTUBE_AD_STRIPPER_EARLY_JS, null)
                                        view?.evaluateJavascript(YOUTUBE_APP_CLEANER_JS, null)
                                    } else {
                                        view?.evaluateJavascript(MEDIA_MONITOR_JS, null)
                                    }
                                    if (it.contains("instagram.com")) {
                                        try {
                                            android.webkit.CookieManager.getInstance().flush()
                                            com.videodownloader.app.engine.ExtractionEngine.getInstagramCookieFile(ctx)
                                        } catch (ignored: Exception) {}
                                    }
                                    if (it.contains("facebook.com") || it.contains("fb.watch")) {
                                        try {
                                            android.webkit.CookieManager.getInstance().flush()
                                            com.videodownloader.app.engine.ExtractionEngine.getFacebookCookieFile(ctx)
                                        } catch (ignored: Exception) {}
                                    }
                                    if (it.contains("youtube.com") || it.contains("youtu.be") ||
                                        it.contains("instagram.com") || it.contains("tiktok.com") ||
                                        it.contains("facebook.com") || it.contains("fb.watch") ||
                                        it.contains("x.com") || it.contains("twitter.com")) {
                                        detectedMediaUrl = if (cleaned.isNotBlank()) cleaned else it
                                    }
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""
                                val lower = reqUrl.lowercase()

                                // Layer 1: Exemption for actual video playback stream (NEVER block video)
                                if (lower.contains("googlevideo.com/videoplayback")) {
                                    if (!lower.contains("&adformat=")) {
                                        detectedMediaUrl = reqUrl
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                // Layer 1: Network Request Interceptor - drop ad domains and ad paths with 200 OK empty response
                                if (isAdUrl(reqUrl)) {
                                    return android.webkit.WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }

                                // Video Link Detector / Sniffer
                                if (lower.endsWith(".mp4") || lower.endsWith(".m4v") ||
                                    (lower.contains("cdninstagram.com") && lower.contains(".mp4")) ||
                                    (lower.contains("fbcdn.net") && lower.contains(".mp4"))
                                ) {
                                    detectedMediaUrl = reqUrl
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) pageTitle = title
                            }
                        }

                        webViewInstance = this
                        loadUrl(currentUrl)
                    }
                }
            )
        }

        // Floating Action Download Button: visible ONLY when a video is playing
        AnimatedVisibility(
            visible = isVideoPlaying,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val targetDownloadUrl = remember(activeVideoUrl, currentUrl, detectedMediaUrl) {
                val candidate = activeVideoUrl ?: currentUrl
                val cleaned = cleanVideoWatchUrl(candidate)
                if (cleaned.isNotBlank()) cleaned else (detectedMediaUrl ?: candidate)
            }

            // Compact Floating Action Download Button (Icon only)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFA855F7), // Neon Purple
                                Color(0xFF7C3AED)  // Deep Royal Violet
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xE0FFFFFF),
                                Color(0x60A855F7)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        viewModel.analyzeUrl(targetDownloadUrl, autoOpenDialog = true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NovaIcons.Download,
                    contentDescription = "Download Video",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Normalizes URLs to canonical platform watch URLs, avoiding raw internal media fragments.
 */
fun cleanVideoWatchUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank() || trimmed.contains("googlevideo.com")) return ""
    return com.videodownloader.app.engine.ExtractionEngine.sanitizeUrl(trimmed)
}

private fun isTrustedOrigin(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = try { android.net.Uri.parse(url) } catch (e: Exception) { return false }
    val host = uri.host?.lowercase() ?: return false
    return host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtu.be" ||
            host == "instagram.com" || host.endsWith(".instagram.com") ||
            host == "facebook.com" || host.endsWith(".facebook.com") ||
            host == "fb.watch" || host == "fb.com" || host.endsWith(".fb.com") ||
            host == "tiktok.com" || host.endsWith(".tiktok.com") ||
            host == "x.com" || host.endsWith(".x.com") ||
            host == "twitter.com" || host.endsWith(".twitter.com")
}

/**
 * JavaScript Interface Bridge to communicate HTML5 media playback events
 * and SPA route navigations back to Android Compose.
 */
class BrowserMediaBridge(
    private val getTopUrl: () -> String?,
    private val onPlayingStateChanged: (Boolean, String?) -> Unit,
    private val onUrlChanged: (String) -> Unit
) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @android.webkit.JavascriptInterface
    fun onMediaPlaying(isPlaying: Boolean, pageUrl: String?) {
        val topUrl = getTopUrl()
        if (!isTrustedOrigin(topUrl) && !isTrustedOrigin(pageUrl)) {
            return
        }
        mainHandler.post {
            onPlayingStateChanged(isPlaying, pageUrl)
        }
    }

    @android.webkit.JavascriptInterface
    fun onUrlNavigate(newUrl: String) {
        val topUrl = getTopUrl()
        if (!isTrustedOrigin(topUrl) || !isTrustedOrigin(newUrl)) {
            return
        }
        mainHandler.post {
            onUrlChanged(newUrl)
        }
    }
}

private fun isAdUrl(url: String): Boolean {
    val lower = url.lowercase()
    // Explicit Layer 1 exemption: NEVER block actual YouTube video streams
    if (lower.contains("googlevideo.com/videoplayback")) return false

    // Known ad & tracking domains
    val isAdDomain = lower.contains("doubleclick.net") ||
            lower.contains("googlesyndication.com") ||
            lower.contains("googleadservices.com") ||
            lower.contains("ads.youtube.com") ||
            lower.contains("an.facebook.com") ||
            lower.contains("taboola.com") ||
            lower.contains("outbrain.com") ||
            lower.contains("googleads") ||
            lower.contains("adservice.google") ||
            lower.contains("static.doubleclick.net") ||
            lower.contains("pubads.g.doubleclick.net") ||
            lower.contains("securepubads.g.doubleclick.net") ||
            lower.contains("innovid.com") ||
            lower.contains("moatads.com") ||
            lower.contains("ad.doubleclick.net") ||
            lower.contains("adservice.google.com")

    // Known ad & telemetry tracking paths
    val isAdPath = lower.contains("/pagead/") ||
            lower.contains("/api/stats/ads") ||
            lower.contains("/ptracking") ||
            lower.contains("/get_midroll_") ||
            lower.contains("youtube.com/api/stats/qoe") ||
            lower.contains("youtube.com/pcs/activeview")

    return isAdDomain || isAdPath
}

// Layer 2: Network Ad Fetch Filter (Injected on onPageStarted and onPageFinished)
private val YOUTUBE_AD_STRIPPER_EARLY_JS = """
    (function() {
        if (window.__novaAdStripperInjected) return;
        window.__novaAdStripperInjected = true;

        // Hook window.fetch to drop telemetry tracking and ad metrics without breaking app state
        if (window.fetch) {
            var origFetch = window.fetch;
            window.fetch = function() {
                var args = arguments;
                var url = (args[0] && typeof args[0] === 'string') ? args[0] : (args[0] && args[0].url ? args[0].url : '');
                if (url && (url.indexOf('/api/stats/ads') !== -1 || url.indexOf('/pagead/') !== -1 || url.indexOf('/ptracking') !== -1 || url.indexOf('/get_midroll_') !== -1)) {
                    return Promise.resolve(new Response('{}', { status: 200, statusText: 'OK' }));
                }
                return origFetch.apply(this, args);
            };
        }
    })();
""".trimIndent()

private val MEDIA_MONITOR_JS = """
    (function() {
        function notifyUrl() {
            var href = window.location.href;
            if (window.NovaMediaBridge && window.NovaMediaBridge.onUrlNavigate) {
                window.NovaMediaBridge.onUrlNavigate(href);
            }
        }

        var originalPush = history.pushState;
        if (originalPush) {
            history.pushState = function() {
                var ret = originalPush.apply(this, arguments);
                notifyUrl();
                return ret;
            };
        }
        var originalReplace = history.replaceState;
        if (originalReplace) {
            history.replaceState = function() {
                var ret = originalReplace.apply(this, arguments);
                notifyUrl();
                return ret;
            };
        }
        window.addEventListener('popstate', notifyUrl);

        function checkVideoPlaying() {
            var videos = document.querySelectorAll('video');
            var anyPlaying = false;
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                if (!v.paused && !v.ended && v.readyState > 1) {
                    anyPlaying = true;
                    break;
                }
            }
            if (window.NovaMediaBridge && window.NovaMediaBridge.onMediaPlaying) {
                window.NovaMediaBridge.onMediaPlaying(anyPlaying, window.location.href);
            }
        }

        setInterval(checkVideoPlaying, 1000);
        document.addEventListener('play', function(e) {
            if (e.target && e.target.tagName === 'VIDEO') {
                checkVideoPlaying();
            }
        }, true);
        document.addEventListener('pause', function(e) {
            if (e.target && e.target.tagName === 'VIDEO') {
                checkVideoPlaying();
            }
        }, true);
    })();
""".trimIndent()

// Layer 3: YouTube Cosmetic CSS Filter & Video Ad Auto-Skipper
private val YOUTUBE_APP_CLEANER_JS = """
    (function() {
        function notifyUrl() {
            var href = window.location.href;
            if (window.NovaMediaBridge && window.NovaMediaBridge.onUrlNavigate) {
                window.NovaMediaBridge.onUrlNavigate(href);
            }
        }

        var originalPush = history.pushState;
        if (originalPush) {
            history.pushState = function() {
                var ret = originalPush.apply(this, arguments);
                notifyUrl();
                return ret;
            };
        }
        var originalReplace = history.replaceState;
        if (originalReplace) {
            history.replaceState = function() {
                var ret = originalReplace.apply(this, arguments);
                notifyUrl();
                return ret;
            };
        }
        window.addEventListener('popstate', notifyUrl);
        window.addEventListener('yt-navigate-finish', notifyUrl);

        function checkVideoPlaying() {
            var videos = document.querySelectorAll('video');
            var anyPlaying = false;
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                if (!v.paused && !v.ended && v.readyState > 1) {
                    anyPlaying = true;
                    break;
                }
            }
            if (window.NovaMediaBridge && window.NovaMediaBridge.onMediaPlaying) {
                window.NovaMediaBridge.onMediaPlaying(anyPlaying, window.location.href);
            }
        }

        setInterval(checkVideoPlaying, 1000);
        document.addEventListener('play', function(e) {
            if (e.target && e.target.tagName === 'VIDEO') {
                checkVideoPlaying();
            }
        }, true);
        document.addEventListener('pause', function(e) {
            if (e.target && e.target.tagName === 'VIDEO') {
                checkVideoPlaying();
            }
        }, true);

        // Layer 3: Cosmetic CSS Filter to hide ad containers
        function injectAdStyles() {
            if (!document.getElementById('zaswix-yt-style')) {
                var style = document.createElement('style');
                style.id = 'zaswix-yt-style';
                style.innerHTML = `
                    /* Layer 3: Cosmetic CSS Filter */
                    ytm-promoted-sparkles-web-renderer,
                    ytm-companion-ad-renderer,
                    ytm-promoted-sparkles-text-search-web-renderer,
                    ytm-mealbar-promo-renderer,
                    ytm-app-banner,
                    .open-app-banner,
                    .ad-container,
                    .ad-showing,
                    .ad-interrupting,
                    #ad-companion-slot,
                    #player-ads,
                    .ytp-ad-overlay-container,
                    .ytp-ad-message-container,
                    .ytm-paid-content-overlay-renderer,
                    .ytm-upsell-dialog-renderer,
                    ytd-promoted-video-renderer,
                    ytd-display-ad-renderer,
                    ytd-statement-banner-renderer,
                    ytd-in-feed-ad-layout-renderer,
                    ytd-banner-promo-renderer-background,
                    ytm-ad-slot-renderer,
                    .video-ads,
                    .ytp-ad-module {
                        display: none !important;
                        opacity: 0 !important;
                        pointer-events: none !important;
                        height: 0 !important;
                    }
                    /* Clean dark theme */
                    body {
                        background-color: #0f0f12 !important;
                        user-select: none;
                        -webkit-user-select: none;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            }
        }

        // Layer 3: Auto Skip Video Ads Safely
        function autoSkipAds() {
            injectAdStyles();

            // 1. Auto-click YouTube ad skip buttons
            var skipSelectors = [
                '.ytp-ad-skip-button',
                '.ytp-ad-skip-button-modern',
                '.ytp-skip-ad-button',
                '.videoAdUiSkipButton',
                'button.ytp-ad-skip-button-text',
                'button.ytp-ad-skip-button-modern',
                '.ytp-ad-overlay-close-button',
                'button[id*="skip"]',
                '.ytp-ad-text[class*="skip"]'
            ];
            for (var i = 0; i < skipSelectors.length; i++) {
                var btns = document.querySelectorAll(skipSelectors[i]);
                for (var j = 0; j < btns.length; j++) {
                    try {
                        btns[j].click();
                    } catch(e) {}
                }
            }

            // 2. Safe ad fast-forward without jumping normal videos
            var player = document.querySelector('#movie_player, .html5-video-player');
            var isAdShowing = player && (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting'));
            var video = document.querySelector('video');
            if (video) {
                if (isAdShowing) {
                    try {
                        video.muted = true;
                        video.playbackRate = 8.0;
                    } catch(e) {}
                } else if (video.playbackRate > 2.0) {
                    try {
                        video.playbackRate = 1.0;
                        video.muted = false;
                    } catch(e) {}
                }
            }
        }

        autoSkipAds();
        setInterval(autoSkipAds, 350);

        // Attach MutationObserver for dynamic single-page DOM changes
        if (window.MutationObserver) {
            var observer = new MutationObserver(function() {
                autoSkipAds();
            });
            observer.observe(document.documentElement || document.body, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['class']
            });
        }
    })();
""".trimIndent()
