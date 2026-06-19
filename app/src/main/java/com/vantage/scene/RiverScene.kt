package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class RiverScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        val haze = hazeColor(sky)

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = 0.5f)
        drawMoon(canvas, w, h, sky, cxFrac = 0.78f)

        drawClouds(canvas, sky, params)
        drawDistantMountains(canvas, sky, haze)
        drawMidHills(canvas, haze, params)
        drawHazeBand(canvas, w, h * 0.45f, h * 0.52f, lerpColor(haze, 0xFFFFFFFF.toInt(), 0.35f), peakAlpha = 130)
        drawFarBank(canvas, haze, params)
        drawWater(canvas, sky, haze, params)
        drawLilyPads(canvas, params)
        drawReeds(canvas, params)
        drawWaterMist(canvas, haze, params)
        drawForegroundBank(canvas, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private fun drawClouds(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val warm = lerpColor(0xFFFFE0CC.toInt(), sky.midColor, 0.35f)
        val rim = lerpColor(0xFFFFF6E0.toInt(), sky.topColor, 0.20f)
        val rng = PRNG(111)
        val drift = (params.elapsedMs * 0.000005f) % 1f
        for (i in 0 until 5) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.08f + rng.next() * 0.14f)
            val scale = h * (0.028f + rng.next() * 0.030f)
            drawFluffyCloud(canvas, cx, cy, scale, warm, rim, alpha = 195)
        }
    }

    private fun drawDistantMountains(canvas: Canvas, sky: SkyState, haze: Int) {
        smoothRidgePath(path, w, h, h * 0.30f, h * 0.07f, seed = 100)
        drawAerialLayer(canvas, path, h * 0.20f, h * 0.40f, 0xFF6C7B96.toInt(), haze, depth = 0.85f)
    }

    private fun drawMidHills(canvas: Canvas, haze: Int, params: SceneParams) {
        val green = when (params.season) {
            Season.SPRING -> 0xFF5C8E54.toInt()
            Season.SUMMER -> 0xFF497E44.toInt()
            Season.AUTUMN -> 0xFF947B40.toInt()
            Season.WINTER -> 0xFF656E68.toInt()
        }
        smoothRidgePath(path, w, h, h * 0.40f, h * 0.05f, seed = 200)
        drawAerialLayer(canvas, path, h * 0.32f, h * 0.50f, green, haze, depth = 0.55f)
    }

    private fun drawFarBank(canvas: Canvas, haze: Int, params: SceneParams) {
        val treeBase = when (params.season) {
            Season.SPRING -> 0xFF44694A.toInt()
            Season.SUMMER -> 0xFF2F4F2A.toInt()
            Season.AUTUMN -> 0xFF7E5028.toInt()
            Season.WINTER -> 0xFF555E58.toInt()
        }
        smoothRidgePath(path, w, h, h * 0.50f, h * 0.025f, seed = 300)
        drawAerialLayer(canvas, path, h * 0.44f, h * 0.55f, treeBase, haze, depth = 0.30f)

        // tiny canopies along bank
        val canopyBright = lerpColor(treeBase, 0xFFFFFFFF.toInt(), 0.20f)
        val rng = PRNG(3010)
        for (i in 0 until 22) {
            val x = rng.next() * w
            val y = h * 0.49f + rng.next() * h * 0.02f
            val r = 8f + rng.next() * 12f
            drawPaintedCanopy(canvas, x, y, r, treeBase, canopyBright, alpha = 220)
        }

        // Dock — visible in non-winter, daytime
        val isDaytime = params.timeOfDay in 6.5f..18f
        if (params.season != Season.WINTER && isDaytime) {
            paint.shader = LinearGradient(
                0f, h * 0.48f, 0f, h * 0.50f,
                0xFF8A7460.toInt(), 0xFF4A3E30.toInt(),
                Shader.TileMode.CLAMP,
            )
            val dockX = w * 0.7f
            val dockY = h * 0.48f
            canvas.drawRect(dockX, dockY, dockX + w * 0.08f, dockY + 4f, paint)
            paint.shader = null
            paint.color = 0xFF584834.toInt()
            canvas.drawRect(dockX, dockY, dockX + 3f, dockY + 12f, paint)
            canvas.drawRect(dockX + w * 0.08f - 3f, dockY, dockX + w * 0.08f, dockY + 12f, paint)
        }
    }

    private fun drawWater(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        drawPaintedWater(
            canvas, w,
            top = h * 0.52f, bot = h * 0.82f,
            sky = sky, haze = haze,
            intensity = params.intensity, elapsedMs = params.elapsedMs,
            glintCxFrac = 0.5f, seed = 400,
        )
    }

    private fun drawLilyPads(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(500)
        for (i in 0 until 10) {
            val x = rng.next() * w
            val y = h * 0.62f + rng.next() * h * 0.16f
            val r = 7f + rng.next() * 6f
            val drift = (Math.sin(params.elapsedMs / 3000.0 + i) * 2.5).toFloat()
            paint.color = withAlpha(0xFF1A2A12.toInt(), 160)
            canvas.drawOval(x - r + drift, y - r * 0.5f + 2f, x + r + drift, y + r * 0.5f + 2f, paint)
            paint.color = 0xFF4A8A3A.toInt()
            canvas.drawOval(x - r + drift, y - r * 0.5f, x + r + drift, y + r * 0.5f, paint)
            paint.color = withAlpha(0xFF7AB85A.toInt(), 200)
            canvas.drawOval(x - r * 0.7f + drift, y - r * 0.45f, x + r * 0.3f + drift, y - r * 0.05f, paint)

            if (rng.next() > 0.5f && (params.season == Season.SPRING || params.season == Season.SUMMER)) {
                paint.color = 0xFFE8A0C0.toInt()
                canvas.drawCircle(x + drift, y - 2f, 3.2f, paint)
                paint.color = withAlpha(0xFFFFFFFF.toInt(), 180)
                canvas.drawCircle(x + drift - 1f, y - 3f, 1.3f, paint)
            }
        }
        paint.alpha = 255
    }

    private fun drawReeds(canvas: Canvas, params: SceneParams) {
        paint.color = 0xFF44653A.toInt()
        paint.strokeWidth = 2.5f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rng = PRNG(600)
        for (side in 0..1) {
            val xBase = if (side == 0) 0f else w * 0.80f
            val xRange = if (side == 0) w * 0.20f else w * 0.20f
            val freq = if (side == 0) 2000.0 else 2200.0
            for (i in 0 until 7) {
                val x = xBase + rng.next() * xRange
                val baseY = h * 0.78f + rng.next() * h * 0.05f
                val topY = baseY - h * 0.11f - rng.next() * h * 0.08f
                val sway = (Math.sin((params.elapsedMs / freq) + i * 1.3) * 7 * params.intensity).toFloat()
                canvas.drawLine(x, baseY, x + sway, topY, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawWaterMist(canvas: Canvas, haze: Int, params: SceneParams) {
        val isColdMorning = params.timeOfDay in 5f..8f
        val isWinter = params.season == Season.WINTER
        val isFog = params.weather == WeatherType.FOG
        if (!isColdMorning && !isWinter && !isFog) return

        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.45f)
        val baseAlpha = when {
            isFog -> 95
            isWinter -> 70
            else -> 55
        }
        val rng = PRNG(800)
        for (i in 0 until 6) {
            val mx = rng.next() * w + (Math.sin(params.elapsedMs / 6000.0 + i) * 14).toFloat()
            val my = h * 0.48f + rng.next() * h * 0.08f
            val pulse = (Math.sin(params.elapsedMs / 4000.0 + i) * 15).toInt()
            drawSoftFogPuff(canvas, mx, my, 70f, 14f, mist, alpha = (baseAlpha + pulse).coerceIn(0, 255))
        }
    }

    private fun drawForegroundBank(canvas: Canvas, params: SceneParams) {
        paint.shader = LinearGradient(
            0f, h * 0.82f, 0f, h.toFloat(),
            0xFF3F5430.toInt(), 0xFF1A2412.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.82f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        paint.color = 0xFF263420.toInt()
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rng = PRNG(700)
        for (i in 0 until 6) {
            val x = rng.next() * w
            val baseY = h * 0.85f + rng.next() * h * 0.08f
            val topY = baseY - h * 0.13f - rng.next() * h * 0.06f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + i) * 5 * params.intensity).toFloat()
            canvas.drawLine(x, baseY, x + sway, topY, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF5A3A20.toInt()
            canvas.drawOval(x + sway - 3.2f, topY - 11f, x + sway + 3.2f, topY + 2f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = 0xFF263420.toInt()
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }
}
