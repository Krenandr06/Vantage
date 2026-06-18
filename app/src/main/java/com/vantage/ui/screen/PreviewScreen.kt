package com.vantage.ui.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vantage.VantageWallpaperService
import com.vantage.data.VantagePrefs
import com.vantage.data.findScene
import com.vantage.ui.component.ScenePreviewView
import com.vantage.ui.component.VantagePrimaryButton
import com.vantage.ui.theme.*

@Composable
fun PreviewScreen(
    sceneId: String,
    onBack: () -> Unit,
    onCustomize: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val meta = findScene(sceneId)

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen preview
        AndroidView(
            factory = { ctx -> ScenePreviewView(ctx, sceneId, prefs.intensity) },
            modifier = Modifier.fillMaxSize(),
        )

        // Top bar
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        // Bottom floating panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(meta.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(meta.title, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Default.Settings, "Customize", tint = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                VantagePrimaryButton(
                    text = "Set as wallpaper",
                    onClick = {
                        prefs.currentScene = sceneId
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, VantageWallpaperService::class.java),
                            )
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
