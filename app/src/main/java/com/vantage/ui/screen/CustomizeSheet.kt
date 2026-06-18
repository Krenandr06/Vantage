package com.vantage.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.vantage.data.VantagePrefs
import com.vantage.scene.intensityLabel
import com.vantage.ui.component.SectionHeader
import com.vantage.ui.component.ToggleRow
import com.vantage.ui.component.VantagePrimaryButton
import com.vantage.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val haptic = LocalHapticFeedback.current

    var weatherEnabled by remember { mutableStateOf(prefs.weatherEnabled) }
    var intensity by remember { mutableFloatStateOf(prefs.intensity) }
    var batteryPause by remember { mutableStateOf(prefs.pauseOnBattery) }
    var frameRate by remember { mutableIntStateOf(prefs.frameRate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Bone,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GraphiteMute) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Customize", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(20.dp))

            ToggleRow(
                label = "Weather overlay",
                subtitle = "Rain, snow, and fog from real conditions",
                checked = weatherEnabled,
                onCheckedChange = { weatherEnabled = it },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Motion")
            Text(
                "  ${intensityLabel(intensity)}",
                style = MaterialTheme.typography.labelMedium,
                color = Clay,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Slider(
                value = intensity,
                onValueChange = {
                    intensity = it
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.padding(horizontal = 20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Graphite,
                    activeTrackColor = Graphite,
                    inactiveTrackColor = Cream,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Still", style = MaterialTheme.typography.labelSmall, color = GraphiteMute)
                Text("Lively", style = MaterialTheme.typography.labelSmall, color = GraphiteMute)
            }

            Spacer(Modifier.height(16.dp))

            ToggleRow(
                label = "Pause on low battery",
                checked = batteryPause,
                onCheckedChange = { batteryPause = it },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Frame rate")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(15, 30, 60).forEach { fps ->
                    FilterChip(
                        selected = frameRate == fps,
                        onClick = {
                            frameRate = fps
                            haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                        },
                        label = { Text("${fps}fps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Graphite,
                            selectedLabelColor = Bone,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            VantagePrimaryButton(
                text = "Apply",
                onClick = {
                    prefs.weatherEnabled = weatherEnabled
                    prefs.intensity = intensity
                    prefs.pauseOnBattery = batteryPause
                    prefs.frameRate = frameRate
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
