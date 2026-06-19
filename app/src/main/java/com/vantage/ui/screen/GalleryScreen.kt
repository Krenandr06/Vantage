package com.vantage.ui.screen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vantage.VantageWallpaperService
import com.vantage.data.VantagePrefs
import com.vantage.data.VisibleScenes
import com.vantage.scene.*
import com.vantage.ui.component.SceneTextureView
import com.vantage.ui.theme.*
import kotlin.math.absoluteValue

@Composable
fun GalleryScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }

    val scenes = VisibleScenes
    val initialPage = scenes.indexOfFirst { it.id == prefs.currentScene }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { scenes.size }

    val currentScene = scenes[pagerState.currentPage]

    LaunchedEffect(pagerState.currentPage) {
        prefs.currentScene = currentScene.id
    }

    val resolvedTime = if (prefs.useAutoTime) currentTimeOfDay() else prefs.timeOverride
    val resolvedSeason = when (prefs.seasonOverride) {
        "spring" -> Season.SPRING
        "summer" -> Season.SUMMER
        "autumn" -> Season.AUTUMN
        "winter" -> Season.WINTER
        else -> currentSeason()
    }
    val resolvedWeather = resolveWeather(prefs, currentScene.id, resolvedSeason)

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        ),
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            flingBehavior = flingBehavior,
            key = { scenes[it].id },
        ) { page ->
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue.coerceIn(0f, 1f)

            // iOS-style subtle scale + parallax fade as a page leaves center
            val scale = 1f - pageOffset * 0.06f
            val alpha = 1f - pageOffset * 0.35f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
            ) {
                AndroidView(
                    factory = { ctx ->
                        SceneTextureView(ctx).apply {
                            setScene(scenes[page].id)
                            this.timeOfDay = resolvedTime
                            this.sceneSeason = resolvedSeason
                            this.sceneWeather = resolvedWeather
                            this.intensity = prefs.intensity
                            this.active = page == pagerState.currentPage
                        }
                    },
                    update = { view ->
                        view.timeOfDay = resolvedTime
                        view.sceneSeason = resolvedSeason
                        view.sceneWeather = resolvedWeather
                        view.intensity = prefs.intensity
                        // Only the settled current page animates — off-screen pages
                        // keep their last frame so swipes don't stutter.
                        view.active = page == pagerState.currentPage &&
                            !pagerState.isScrollInProgress
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        TopOverlay(
            currentPage = pagerState.currentPage,
            pageCount = scenes.size,
            onSettings = onSettings,
        )

        BottomOverlay(
            sceneId = currentScene.id,
            sceneName = currentScene.name,
            sceneTitle = currentScene.title,
        )
    }
}

private fun resolveWeather(prefs: VantagePrefs, sceneId: String, season: Season): WeatherType {
    val raw = if (prefs.weatherAuto) {
        // Stable per (scene, day) so the wallpaper doesn't jitter between weathers.
        val daySeed = (System.currentTimeMillis() / (1000L * 60 * 60 * 24)).toInt() xor sceneId.hashCode()
        randomizeWeather(sceneId, season, kotlin.random.Random(daySeed))
    } else prefs.weatherType
    return when (raw) {
        "rain" -> WeatherType.RAIN
        "snow" -> WeatherType.SNOW
        "fog" -> WeatherType.FOG
        "cloudy" -> WeatherType.CLOUDY
        else -> WeatherType.CLEAR
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
                .background(Color.Black.copy(alpha = 0.30f), CircleShape),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                repeat(pageCount) { i ->
                    val isActive = i == currentPage
                    val size by animateFloatAsState(
                        if (isActive) 8f else 6f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
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
