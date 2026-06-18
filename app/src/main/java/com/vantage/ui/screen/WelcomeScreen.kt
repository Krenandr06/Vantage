package com.vantage.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.vantage.ui.component.VantagePrimaryButton
import com.vantage.ui.theme.Bone
import com.vantage.ui.theme.GraphiteSoft

@Composable
fun WelcomeScreen(onBegin: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Bone).padding(32.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Vantage", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                "A live wallpaper,\nnothing else.",
                style = MaterialTheme.typography.bodyLarge,
                color = GraphiteSoft,
            )
        }

        VantagePrimaryButton(
            text = "Begin",
            showArrow = true,
            onClick = onBegin,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .alpha(alpha.value),
        )
    }
}
