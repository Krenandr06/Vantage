package com.vantage.scene

import kotlin.random.Random

/**
 * Picks a weather type appropriate to the current scene and season.
 *
 * Scenes that don't support certain weathers (e.g. space has no atmosphere,
 * so no rain/snow/fog) restrict the pool, and the remaining options are
 * weighted by season (rain in autumn, snow in winter, mostly clear in summer).
 */
fun randomizeWeather(sceneId: String, season: Season, rng: Random = Random.Default): String {
    val allowed = allowedWeather(sceneId)
    val weights = seasonalWeights(season).filterKeys { it in allowed }
    if (weights.isEmpty()) return "clear"
    val total = weights.values.sum()
    if (total <= 0f) return weights.keys.first()
    var r = rng.nextFloat() * total
    for ((w, weight) in weights) {
        r -= weight
        if (r <= 0f) return w
    }
    return weights.keys.last()
}

private fun allowedWeather(scene: String): Set<String> = when (scene) {
    // Vacuum — no atmospheric weather.
    "space" -> setOf("clear")
    // Eclipse: keep skies mostly readable; clouds & fog are OK, no precipitation.
    "eclipse" -> setOf("clear", "cloudy", "fog")
    // Indoors-ish: bamboo grove rarely has snow.
    "bamboo" -> setOf("clear", "rain", "fog", "cloudy")
    // Waterfall: snow would freeze the falls in the art — skip it.
    "waterfall" -> setOf("clear", "rain", "fog", "cloudy")
    else -> setOf("clear", "rain", "snow", "fog", "cloudy")
}

private fun seasonalWeights(season: Season): Map<String, Float> = when (season) {
    Season.WINTER -> linkedMapOf(
        "clear" to 1.0f, "snow" to 2.5f, "cloudy" to 2.0f, "fog" to 1.2f, "rain" to 0.4f,
    )
    Season.SPRING -> linkedMapOf(
        "clear" to 1.5f, "rain" to 2.0f, "cloudy" to 1.5f, "fog" to 0.8f, "snow" to 0.2f,
    )
    Season.SUMMER -> linkedMapOf(
        "clear" to 3.0f, "cloudy" to 1.2f, "rain" to 1.0f, "fog" to 0.3f, "snow" to 0.0f,
    )
    Season.AUTUMN -> linkedMapOf(
        "clear" to 1.0f, "rain" to 2.0f, "cloudy" to 2.0f, "fog" to 1.5f, "snow" to 0.3f,
    )
}
