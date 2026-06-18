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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vantage.data.VantagePrefs
import com.vantage.scene.Season
import com.vantage.scene.WeatherType
import com.vantage.scene.intensityLabel
import com.vantage.ui.component.SectionHeader
import com.vantage.ui.component.ToggleRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }

    var weatherType by remember { mutableStateOf(prefs.weatherType) }
    var seasonOverride by remember { mutableStateOf(prefs.seasonOverride) }
    var timeOverride by remember { mutableFloatStateOf(prefs.timeOverride) }
    var intensity by remember { mutableFloatStateOf(prefs.intensity) }
    var locationEnabled by remember { mutableStateOf(prefs.locationEnabled) }
    var weatherEnabled by remember { mutableStateOf(prefs.weatherEnabled) }
    var batteryPause by remember { mutableStateOf(prefs.pauseOnBattery) }
    var screenOffPause by remember { mutableStateOf(prefs.pauseScreenOff) }

    Scaffold(
        containerColor = Bone,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Graphite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bone),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Scene controls ──
            SectionHeader("Scene")

            // Weather
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Weather", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text("Overlay applied to all scenes", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("clear", "rain", "snow", "fog", "cloudy").forEach { w ->
                        Pill(
                            label = w.replaceFirstChar { it.uppercase() },
                            active = weatherType == w,
                            onClick = {
                                weatherType = w
                                prefs.weatherType = w
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Season
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Season", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text("Auto follows your calendar", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("auto", "spring", "summer", "autumn", "winter").forEach { s ->
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

            Spacer(Modifier.height(8.dp))

            // Time of day
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Time of day", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (timeOverride < 0f) "Following device clock"
                            else "${timeLabel(timeOverride)} — ${phaseLabel(timeOverride)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (timeOverride < 0f) GraphiteSoft else Clay,
                        )
                    }
                    if (timeOverride >= 0f) {
                        TextButton(onClick = {
                            timeOverride = -1f
                            prefs.timeOverride = -1f
                        }) {
                            Text("Auto", color = Clay, fontSize = 13.sp)
                        }
                    }
                }
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

            Spacer(Modifier.height(8.dp))

            // Motion intensity
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Motion intensity", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        intensityLabel(intensity),
                        style = MaterialTheme.typography.bodySmall,
                        color = Clay,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text("Particle count and animation speed", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
                Spacer(Modifier.height(8.dp))
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = Hair)

            // ── Engine ──
            SectionHeader("Engine")

            ToggleRow(
                label = "Use local weather",
                subtitle = "Match wallpaper weather to real conditions",
                checked = locationEnabled,
                onCheckedChange = {
                    locationEnabled = it
                    prefs.locationEnabled = it
                },
            )

            ToggleRow(
                label = "Weather overlay",
                checked = weatherEnabled,
                onCheckedChange = {
                    weatherEnabled = it
                    prefs.weatherEnabled = it
                },
            )

            ToggleRow(
                label = "Pause on low battery",
                subtitle = "Stop rendering below 15%",
                checked = batteryPause,
                onCheckedChange = {
                    batteryPause = it
                    prefs.pauseOnBattery = it
                },
            )

            ToggleRow(
                label = "Pause when screen off",
                checked = screenOffPause,
                onCheckedChange = {
                    screenOffPause = it
                    prefs.pauseScreenOff = it
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = Hair)

            SectionHeader("About")

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Vantage", style = MaterialTheme.typography.bodyLarge)
                Text("Version 1.0 · Slumbering Thread Studios", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
            }

            Spacer(Modifier.height(40.dp))
        }
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
        if (active) Graphite else Color.Transparent,
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
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, if (active) Graphite else Hair, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
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
