package com.vantage.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vantage.data.VantagePrefs
import com.vantage.scene.Season
import com.vantage.scene.currentSeason
import com.vantage.scene.intensityLabel
import com.vantage.scene.randomizeWeather
import com.vantage.ui.theme.*

private fun phaseLabel(t: Float): String = when {
    t < 4.5f || t >= 21f -> "Deep night"
    t < 6.5f -> "Dawn"
    t < 8.5f -> "Morning"
    t < 11f -> "Mid-morning"
    t < 14f -> "Noon"
    t < 16.5f -> "Afternoon"
    t < 18f -> "Golden hour"
    t < 19.3f -> "Sunset"
    t < 20.2f -> "Dusk"
    else -> "Evening"
}

private fun timeLabel(t: Float): String {
    val h = t.toInt() % 24
    val m = ((t - t.toInt()) * 60).toInt()
    val ampm = if (h < 12 || h == 24) "am" else "pm"
    val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
    return "$h12:${m.toString().padStart(2, '0')} $ampm"
}

private val PastelPeach = Color(0xFFF9E2D2)
private val PastelLavender = Color(0xFFE5DCEC)
private val PastelSage = Color(0xFFE2EADD)
private val CardWash = Color(0xCCFFFEFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val haptic = LocalHapticFeedback.current

    var weatherType by remember { mutableStateOf(prefs.weatherType) }
    var weatherAuto by remember { mutableStateOf(prefs.weatherAuto) }
    var seasonOverride by remember { mutableStateOf(prefs.seasonOverride) }
    var timeOverride by remember { mutableFloatStateOf(prefs.timeOverride) }
    var timeAuto by remember { mutableStateOf(prefs.useAutoTime) }
    var intensity by remember { mutableFloatStateOf(prefs.intensity) }
    var locationEnabled by remember { mutableStateOf(prefs.locationEnabled) }
    var weatherEnabled by remember { mutableStateOf(prefs.weatherEnabled) }
    var batteryPause by remember { mutableStateOf(prefs.pauseOnBattery) }
    var screenOffPause by remember { mutableStateOf(prefs.pauseScreenOff) }

    val pageBrush = remember {
        Brush.verticalGradient(
            0f to PastelPeach,
            0.30f to PastelLavender,
            0.65f to Bone,
            1f to PastelSage,
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Graphite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        modifier = Modifier.background(pageBrush),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Scene card ──
            SoftCard {
                CardHeader("Scene", "Mood, light, and weather of your wallpaper.")

                // Weather — auto toggle + (when manual) pill selector
                FieldRowWithAuto(
                    label = "Weather",
                    subtitle = if (weatherAuto) "Auto — picked daily for the scene"
                               else "Overlay applied to all scenes",
                    autoLabel = "Auto",
                    isAuto = weatherAuto,
                    onToggleAuto = {
                        weatherAuto = !weatherAuto
                        prefs.weatherAuto = weatherAuto
                    },
                )
                if (!weatherAuto) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf("clear", "rain", "snow", "fog", "cloudy").forEach { wOpt ->
                            Pill(
                                label = wOpt.replaceFirstChar { it.uppercase() },
                                active = weatherType == wOpt,
                                onClick = {
                                    weatherType = wOpt
                                    prefs.weatherType = wOpt
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    RandomizeWeatherButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                        val resolvedSeason = when (prefs.seasonOverride) {
                            "spring" -> Season.SPRING
                            "summer" -> Season.SUMMER
                            "autumn" -> Season.AUTUMN
                            "winter" -> Season.WINTER
                            else -> currentSeason()
                        }
                        val picked = randomizeWeather(prefs.currentScene, resolvedSeason)
                        weatherType = picked
                        prefs.weatherType = picked
                    })
                }

                Spacer(Modifier.height(18.dp))

                // Season — auto toggle + (when manual) season pills
                val seasonAuto = seasonOverride == "auto"
                FieldRowWithAuto(
                    label = "Season",
                    subtitle = if (seasonAuto) "Following your calendar"
                               else seasonOverride.replaceFirstChar { it.uppercase() },
                    autoLabel = "Auto",
                    isAuto = seasonAuto,
                    onToggleAuto = {
                        val next = if (seasonAuto) {
                            // Coming out of auto — seed with current calendar season
                            when (currentSeason()) {
                                Season.SPRING -> "spring"
                                Season.SUMMER -> "summer"
                                Season.AUTUMN -> "autumn"
                                Season.WINTER -> "winter"
                            }
                        } else "auto"
                        seasonOverride = next
                        prefs.seasonOverride = next
                    },
                )
                if (!seasonAuto) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("spring", "summer", "autumn", "winter").forEach { s ->
                            Pill(
                                label = s.replaceFirstChar { it.uppercase() },
                                active = seasonOverride == s,
                                onClick = {
                                    seasonOverride = s
                                    prefs.seasonOverride = s
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Time of day — auto toggle + (when manual) slider
                FieldRowWithAuto(
                    label = "Time of day",
                    subtitle = if (timeAuto) "Following device clock"
                               else "${timeLabel(timeOverride)} — ${phaseLabel(timeOverride)}",
                    autoLabel = "Auto",
                    isAuto = timeAuto,
                    onToggleAuto = {
                        timeAuto = !timeAuto
                        if (timeAuto) {
                            timeOverride = -1f
                            prefs.timeOverride = -1f
                        } else {
                            // Coming out of auto — seed with current device time
                            val seed = com.vantage.scene.currentTimeOfDay()
                            timeOverride = seed
                            prefs.timeOverride = seed
                        }
                    },
                )
                if (!timeAuto) {
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = if (timeOverride < 0f) 12f else timeOverride,
                        onValueChange = {
                            timeOverride = it
                            prefs.timeOverride = it
                        },
                        valueRange = 0f..24f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Clay,
                            activeTrackColor = Clay,
                            inactiveTrackColor = Hair,
                        ),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("12a", "6a", "12p", "6p", "12a").forEach {
                            Text(it, fontSize = 10.sp, color = GraphiteMute)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Motion intensity
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Motion intensity", style = MaterialTheme.typography.bodyLarge)
                    Text(intensityLabel(intensity), style = MaterialTheme.typography.bodySmall, color = Clay)
                }
                Text("Particle count and animation speed", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
                Spacer(Modifier.height(6.dp))
                Slider(
                    value = intensity,
                    onValueChange = {
                        intensity = it
                        prefs.intensity = it
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Clay,
                        activeTrackColor = Clay,
                        inactiveTrackColor = Hair,
                    ),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("Still", "Hush", "Breeze", "Lively").forEach {
                        Text(it, fontSize = 10.sp, color = GraphiteMute)
                    }
                }
            }

            // ── Engine card ──
            SoftCard {
                CardHeader("Engine", "Behavior and battery.")

                SoftToggle(
                    label = "Use local weather",
                    subtitle = "Match wallpaper weather to real conditions",
                    checked = locationEnabled,
                    onCheckedChange = {
                        locationEnabled = it
                        prefs.locationEnabled = it
                    },
                )
                SoftToggle(
                    label = "Weather overlay",
                    checked = weatherEnabled,
                    onCheckedChange = {
                        weatherEnabled = it
                        prefs.weatherEnabled = it
                    },
                )
                SoftToggle(
                    label = "Pause on low battery",
                    subtitle = "Stop rendering below 15%",
                    checked = batteryPause,
                    onCheckedChange = {
                        batteryPause = it
                        prefs.pauseOnBattery = it
                    },
                )
                SoftToggle(
                    label = "Pause when screen off",
                    checked = screenOffPause,
                    onCheckedChange = {
                        screenOffPause = it
                        prefs.pauseScreenOff = it
                    },
                )
            }

            // ── About card ──
            SoftCard {
                CardHeader("About", null)
                Text("Vantage", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Version 1.0 · Slumbering Thread Studios",
                    style = MaterialTheme.typography.bodySmall,
                    color = GraphiteSoft,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SoftCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWash)
            .border(1.dp, Hair, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun CardHeader(title: String, subtitle: String?) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = GraphiteMute,
    )
    if (subtitle != null) {
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun FieldHeader(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
    }
    Spacer(Modifier.height(8.dp))
}

/**
 * A field header with a compact Auto chip on the right. When Auto is on, the chip
 * is filled; tapping toggles. The subtitle communicates current state to the user.
 */
@Composable
private fun FieldRowWithAuto(
    label: String,
    subtitle: String?,
    autoLabel: String,
    isAuto: Boolean,
    onToggleAuto: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAuto) GraphiteSoft else Clay,
                )
            }
        }
        AutoChip(
            label = autoLabel,
            active = isAuto,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                onToggleAuto()
            },
        )
    }
}

@Composable
private fun AutoChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (active) Clay else Color.White.copy(alpha = 0.65f),
        animationSpec = tween(180),
        label = "auto_bg",
    )
    val fg by animateColorAsState(
        if (active) Bone else Graphite,
        animationSpec = tween(180),
        label = "auto_fg",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (active) Clay else Hair, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun Pill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        if (active) Clay else Color.White.copy(alpha = 0.65f),
        animationSpec = tween(180),
        label = "pill_bg",
    )
    val fgColor by animateColorAsState(
        if (active) Bone else Graphite,
        animationSpec = tween(180),
        label = "pill_fg",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, if (active) Clay else Hair, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = fgColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RandomizeWeatherButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Sage.copy(alpha = 0.22f))
            .border(1.dp, Sage.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = Moss,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Randomize weather",
                style = MaterialTheme.typography.bodyLarge,
                color = Graphite,
            )
            Text(
                "Picks something fitting your scene and season",
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteSoft,
            )
        }
    }
}

@Composable
private fun SoftToggle(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = Clay,
                checkedThumbColor = Bone,
                uncheckedTrackColor = Cream,
                uncheckedThumbColor = GraphiteMute,
            ),
        )
    }
}
