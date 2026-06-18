package com.vantage.scene

import android.graphics.Canvas

enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

enum class WeatherType { CLEAR, RAIN, SNOW, FOG, CLOUDY }

data class SceneParams(
    val width: Int,
    val height: Int,
    val timeOfDay: Float,
    val season: Season,
    val weather: WeatherType,
    val intensity: Float,
    val deltaMs: Long,
    val elapsedMs: Long,
)

interface VantageScene {
    fun init(width: Int, height: Int)
    fun draw(canvas: Canvas, params: SceneParams)
    fun release() {}
}

fun currentSeason(): Season {
    val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    return when (month) {
        in 2..4 -> Season.SPRING
        in 5..7 -> Season.SUMMER
        in 8..10 -> Season.AUTUMN
        else -> Season.WINTER
    }
}

fun currentTimeOfDay(): Float {
    val cal = java.util.Calendar.getInstance()
    return cal.get(java.util.Calendar.HOUR_OF_DAY) +
        cal.get(java.util.Calendar.MINUTE) / 60f
}

fun intensityLabel(intensity: Float): String = when {
    intensity < 0.08f -> "Still"
    intensity < 0.30f -> "Hush"
    intensity < 0.55f -> "Calm"
    intensity < 0.80f -> "Breeze"
    else -> "Lively"
}
