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
import com.vantage.ui.theme.Bone
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
        delay(600)
        alpha.animateTo(0f, animationSpec = tween(500))
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Bone),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Vantage",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.alpha(alpha.value),
        )
    }
}

private val EaseOutCubic = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
