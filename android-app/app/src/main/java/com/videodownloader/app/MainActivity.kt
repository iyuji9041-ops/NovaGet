package com.videodownloader.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.videodownloader.app.engine.ExtractionEngine
import com.videodownloader.app.ui.DownloadViewModel
import com.videodownloader.app.ui.MainScreen
import com.videodownloader.app.ui.theme.NovaGetTheme
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize standalone ExtractionEngine with Python 3.12 & FFmpeg
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ExtractionEngine.init(applicationContext)
                Log.d("MainActivity", "ExtractionEngine initialized successfully")

                // Auto-update yt-dlp to latest version to bypass new YouTube restrictions
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(applicationContext)
                    val ver = YoutubeDL.getInstance().version(applicationContext)
                    Log.d("MainActivity", "YoutubeDL auto-update check: $status ($ver)")
                } catch (ue: Exception) {
                    Log.d("MainActivity", "Auto-update check completed or skipped: ${ue.message}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to init ExtractionEngine", e)
            }
        }

        lifecycleScope.launch {
            viewModel.keepScreenOn.collect { keepAwake ->
                if (keepAwake) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        handleShareIntent(intent)

        setContent {
            NovaGetTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.loadUrlInDownloader(sharedText)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank()) {
                    viewModel.loadUrlInDownloader(dataUri)
                }
            }
        }
    }
}
