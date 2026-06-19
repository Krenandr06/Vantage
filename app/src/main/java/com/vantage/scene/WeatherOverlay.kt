package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint

class WeatherOverlay {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private class Particle(
        var x: Float, var y: Float,
        var speed: Float, var size: Float, var alpha: Float,
        var drift: Float = 0f,
    )

    private var particles = emptyList<Particle>()
    private var lastWeather = WeatherType.CLEAR
    private var w = 0
    private var h = 0

    fun init(width: Int, height: Int) {
        w = width
        h = height
    }

    private var elapsedMs = 0L

    fun draw(canvas: Canvas, weather: WeatherType, intensity: Float, deltaMs: Long) {
        elapsedMs += deltaMs
        if (weather == WeatherType.CLEAR) {
            particles = emptyList()
            lastWeather = weather
            return
        }

        if (weather != lastWeather || particles.isEmpty()) {
            particles = buildParticles(weather, intensity)
            lastWeather = weather
        }

        val dt = deltaMs / 1000f

        when (weather) {
            WeatherType.RAIN -> drawRainStreaks(canvas, w, h, elapsedMs, intensity)
            WeatherType.SNOW -> drawSnow(canvas, dt, intensity)
            WeatherType.FOG -> drawFog(canvas, dt, intensity)
            WeatherType.CLOUDY -> drawClouds(canvas, dt, intensity)
            else -> {}
        }
    }

    private fun buildParticles(weather: WeatherType, intensity: Float): List<Particle> {
        val rng = PRNG(77)
        val count = when (weather) {
            WeatherType.RAIN -> (60 + intensity * 120).toInt()
            WeatherType.SNOW -> (30 + intensity * 60).toInt()
            WeatherType.FOG -> 8
            WeatherType.CLOUDY -> 5
            else -> 0
        }
        return List(count) {
            Particle(
                x = rng.next() * w,
                y = rng.next() * h,
                speed = 100f + rng.next() * 300f,
                size = 1f + rng.next() * 3f,
                alpha = 0.2f + rng.next() * 0.5f,
                drift = (rng.next() - 0.5f) * 40f,
            )
        }
    }

    private fun drawRain(canvas: Canvas, dt: Float, intensity: Float) {
        paint.color = 0xFFB0C4DE.toInt()
        paint.strokeWidth = 1.5f
        paint.strokeCap = Paint.Cap.ROUND
        for (p in particles) {
            p.y += p.speed * dt * (0.8f + intensity * 0.4f)
            p.x += p.drift * dt
            if (p.y > h) { p.y = -10f; p.x = (Math.random() * w).toFloat() }
            if (p.x < 0) p.x += w; if (p.x > w) p.x -= w
            paint.alpha = (p.alpha * 255 * intensity).toInt().coerceIn(0, 255)
            canvas.drawLine(p.x, p.y, p.x + p.drift * 0.02f, p.y + p.size * 6f, paint)
        }
    }

    private fun drawSnow(canvas: Canvas, dt: Float, intensity: Float) {
        paint.color = 0xFFFFFFFF.toInt()
        paint.style = Paint.Style.FILL
        for (p in particles) {
            p.y += p.speed * 0.15f * dt
            p.x += (Math.sin((p.y * 0.01f).toDouble()) * 15f * dt).toFloat() + p.drift * dt * 0.3f
            if (p.y > h) { p.y = -5f; p.x = (Math.random() * w).toFloat() }
            if (p.x < 0) p.x += w; if (p.x > w) p.x -= w
            paint.alpha = (p.alpha * 255 * intensity).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    private fun drawFog(canvas: Canvas, dt: Float, intensity: Float) {
        paint.color = 0xFFD8D0C4.toInt()
        paint.style = Paint.Style.FILL
        for (p in particles) {
            p.x += p.drift * dt * 0.5f
            if (p.x > w + 200) p.x = -200f
            if (p.x < -200) p.x = w + 200f
            paint.alpha = (p.alpha * 80 * intensity).toInt().coerceIn(0, 255)
            canvas.drawOval(
                p.x - p.size * 50f, p.y - p.size * 15f,
                p.x + p.size * 50f, p.y + p.size * 15f, paint,
            )
        }
    }

    private fun drawClouds(canvas: Canvas, dt: Float, intensity: Float) {
        paint.color = 0xFFE0D8CC.toInt()
        paint.style = Paint.Style.FILL
        for (p in particles) {
            p.x += p.drift * dt * 0.2f
            if (p.x > w + 300) p.x = -300f
            if (p.x < -300) p.x = w + 300f
            paint.alpha = (p.alpha * 100 * intensity).toInt().coerceIn(0, 255)
            canvas.drawOval(
                p.x - p.size * 60f, p.y - p.size * 18f,
                p.x + p.size * 60f, p.y + p.size * 18f, paint,
            )
        }
    }
}
