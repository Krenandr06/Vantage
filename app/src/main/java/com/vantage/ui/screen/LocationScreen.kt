package com.vantage.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vantage.ui.component.VantagePrimaryButton
import com.vantage.ui.component.VantageSecondaryButton
import com.vantage.ui.theme.Bone
import com.vantage.ui.theme.GraphiteMute
import com.vantage.ui.theme.GraphiteSoft

@Composable
fun LocationScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Bone).padding(32.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Step 2 of 3",
                style = MaterialTheme.typography.labelMedium,
                color = GraphiteMute,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Use your local\nweather?",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Weather data stays on-device.\nWe never store or share your location.",
                style = MaterialTheme.typography.bodyMedium,
                color = GraphiteSoft,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            VantagePrimaryButton(
                text = "Allow location",
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            VantageSecondaryButton(
                text = "Skip for now",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
