package com.vantage.ui.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun HomeScreen(
    onScenePicker: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val sceneId = prefs.currentScene
    val meta = findScene(sceneId)

    Column(
        modifier = Modifier.fillMaxSize().background(Bone),
    ) {
        // Scene preview top half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        ) {
            AndroidView(
                factory = { ctx -> ScenePreviewView(ctx, sceneId, prefs.intensity) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Bottom info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(meta.name, style = MaterialTheme.typography.headlineMedium)
                    Text(meta.title, style = MaterialTheme.typography.bodyMedium, color = GraphiteSoft)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GraphiteMute)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(meta.story, style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)

            Spacer(Modifier.height(20.dp))

            VantagePrimaryButton(
                text = "Set as wallpaper",
                onClick = {
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

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onScenePicker,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose another scene", color = GraphiteSoft)
            }
        }
    }
}
