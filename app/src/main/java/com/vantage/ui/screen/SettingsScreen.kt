package com.vantage.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vantage.data.VantagePrefs
import com.vantage.ui.component.SectionHeader
import com.vantage.ui.component.ToggleRow
import com.vantage.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }

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
            SectionHeader("Location")

            ToggleRow(
                label = "Use local weather",
                subtitle = "Match wallpaper weather to real conditions",
                checked = locationEnabled,
                onCheckedChange = {
                    locationEnabled = it
                    prefs.locationEnabled = it
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Hair)

            Spacer(Modifier.height(16.dp))
            SectionHeader("Wallpaper engine")

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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Hair)

            Spacer(Modifier.height(16.dp))
            SectionHeader("About")

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Vantage", style = MaterialTheme.typography.bodyLarge)
                Text("Version 1.0", style = MaterialTheme.typography.bodySmall, color = GraphiteSoft)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
