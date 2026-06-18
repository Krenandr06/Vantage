package com.vantage.ui.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vantage.VantageWallpaperService
import com.vantage.data.AllScenes
import com.vantage.data.VantagePrefs
import com.vantage.scene.*
import com.vantage.ui.component.SceneTextureView
import com.vantage.ui.theme.*

private val SCENES_ORDER = listOf("forest", "bamboo", "mountains", "river", "waterfall", "space", "eclipse")

@Composable
fun GalleryScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }

    val initialPage = SCENES_ORDER.indexOf(prefs.currentScene).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { SCENES_ORDER.size }

    val currentSceneId = SCENES_ORDER[pagerState.currentPage]
    val meta = AllScenes.first { it.id == currentSceneId }

    LaunchedEffect(pagerState.currentPage) {
        prefs.currentScene = currentSceneId
    }

    val resolvedTime = if (prefs.useAutoTime) currentTimeOfDay() else prefs.timeOverride
    val resolvedSeason = when (prefs.seasonOverride) {
        "spring" -> Season.SPRING
        "summer" -> Season.SUMMER
        "autumn" -> Season.AUTUMN
        "winter" -> Season.WINTER
        else -> currentSeason()
    }
    val resolvedWeather = when (prefs.weatherType) {
        "rain" -> WeatherType.RAIN
        "snow" -> WeatherType.SNOW
        "fog" -> WeatherType.FOG
        "cloudy" -> WeatherType.CLOUDY
        else -> WeatherType.CLEAR
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { SCENES_ORDER[it] },
        ) { page ->
            AndroidView(
                factory = { ctx ->
                    SceneTextureView(ctx).apply {
                        setScene(SCENES_ORDER[page])
                        this.timeOfDay = resolvedTime
                        this.sceneSeason = resolvedSeason
                        this.sceneWeather = resolvedWeather
                        this.intensity = prefs.intensity
                    }
                },
                update = { view ->
                    view.timeOfDay = resolvedTime
                    view.sceneSeason = resolvedSeason
                    view.sceneWeather = resolvedWeather
                    view.intensity = prefs.intensity
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        TopOverlay(
            currentPage = pagerState.currentPage,
            pageCount = SCENES_ORDER.size,
            onSettings = onSettings,
        )

        BottomOverlay(
            sceneId = currentSceneId,
            sceneName = meta.name,
            sceneTitle = meta.title,
        )
    }
}

@Composable
private fun TopOverlay(
    currentPage: Int,
    pageCount: Int,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            repeat(pageCount) { i ->
                val isActive = i == currentPage
                val size by animateFloatAsState(
                    if (isActive) 8f else 6f,
                    animationSpec = tween(200),
                    label = "dot",
                )
                val color by animateColorAsState(
                    if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                    animationSpec = tween(200),
                    label = "dot_c",
                )
                Box(Modifier.size(size.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
private fun BottomOverlay(
    sceneId: String,
    sceneName: String,
    sceneTitle: String,
) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 20.dp),
        ) {
            Text(
                sceneName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.02).sp,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                sceneTitle,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (pressed) 0.96f else 1f,
                animationSpec = tween(120),
                label = "btn",
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    prefs.currentScene = sceneId
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, VantageWallpaperService::class.java),
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(scale),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                ),
                interactionSource = interactionSource,
            ) {
                Text(
                    "Set as Wallpaper",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
