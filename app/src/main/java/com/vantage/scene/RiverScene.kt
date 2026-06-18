package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class RiverScene : VantageScene {

    private val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lilyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bankPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky)
        drawMoon(canvas, w, h, sky)

        drawDistantMountains(canvas, sky)
        drawMidHills(canvas, params)
        drawFarBank(canvas, params)
        drawWater(canvas, sky, params)
        drawLilyPads(canvas, params)
        drawReeds(canvas, params)
        drawWaterMist(canvas, params)
        drawForegroundBank(canvas, params)
    }

    private fun drawDistantMountains(canvas: Canvas, sky: SkyState) {
        hillPaint.color = lerpColor(0xFF7A8AA0.toInt(), sky.midColor, 0.5f)
        path.reset()
        path.moveTo(0f, h * 0.3f)
        val rng = PRNG(100)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.22f + rng.next() * 0.1f))
            x += w * 0.1f
        }
        path.lineTo(w.toFloat(), h * 0.35f)
        path.lineTo(w.toFloat(), h * 0.4f)
        path.lineTo(0f, h * 0.38f)
        path.close()
        canvas.drawPath(path, hillPaint)
    }

    private fun drawMidHills(canvas: Canvas, params: SceneParams) {
        val green = when (params.season) {
            Season.SPRING -> 0xFF5A8A50.toInt()
            Season.SUMMER -> 0xFF4A7A40.toInt()
            Season.AUTUMN -> 0xFF8A7A40.toInt()
            Season.WINTER -> 0xFF5A6A58.toInt()
        }
        hillPaint.color = green
        path.reset()
        path.moveTo(0f, h * 0.38f)
        val rng = PRNG(200)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.34f + rng.next() * 0.06f))
            x += w * 0.08f
        }
        path.lineTo(w.toFloat(), h * 0.42f)
        path.lineTo(w.toFloat(), h * 0.48f)
        path.lineTo(0f, h * 0.46f)
        path.close()
        canvas.drawPath(path, hillPaint)
    }

    private fun drawFarBank(canvas: Canvas, params: SceneParams) {
        // Tree line
        treePaint.color = 0xFF3A5A32.toInt()
        path.reset()
        path.moveTo(0f, h * 0.44f)
        val rng = PRNG(300)
        var x = 0f
        while (x <= w) {
            val bump = rng.next() * h * 0.03f
            path.lineTo(x, h * 0.42f - bump)
            x += w * 0.03f
        }
        path.lineTo(w.toFloat(), h * 0.48f)
        path.lineTo(w.toFloat(), h * 0.5f)
        path.lineTo(0f, h * 0.5f)
        path.close()
        canvas.drawPath(path, treePaint)

        // Dock - visible in non-winter, daytime only
        val isDaytime = params.timeOfDay in 6.5f..18f
        if (params.season != Season.WINTER && isDaytime) {
            bankPaint.color = 0xFF6A5A4A.toInt()
            val dockX = w * 0.7f
            val dockY = h * 0.48f
            canvas.drawRect(dockX, dockY, dockX + w * 0.08f, dockY + 4f, bankPaint)
            canvas.drawRect(dockX, dockY, dockX + 3f, dockY + 12f, bankPaint)
            canvas.drawRect(dockX + w * 0.08f - 3f, dockY, dockX + w * 0.08f, dockY + 12f, bankPaint)
        }
    }

    private fun drawWater(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val waterTop = h * 0.5f
        val waterBot = h * 0.82f

        waterPaint.color = lerpColor(sky.botColor, 0xFF3A6A8A.toInt(), 0.5f)
        waterPaint.alpha = 220
        canvas.drawRect(0f, waterTop, w.toFloat(), waterBot, waterPaint)

        // Ripple bands
        waterPaint.color = 0xFFFFFFFF.toInt()
        waterPaint.style = Paint.Style.STROKE
        waterPaint.strokeWidth = 1f
        for (band in 0 until 3) {
            val bandY = waterTop + (waterBot - waterTop) * (0.2f + band * 0.3f)
            val phase = params.elapsedMs / 2500.0 + band * 2.1
            val rng = PRNG(400 + band)
            for (i in 0 until 12) {
                val rx = rng.next() * w
                val offset = (Math.sin(phase + rx * 0.01) * 3).toFloat()
                val lineW = 8f + rng.next() * 15f
                waterPaint.alpha = (20 + rng.next() * 25).toInt().coerceIn(0, 255)
                canvas.drawLine(rx - lineW, bandY + offset, rx + lineW, bandY + offset, waterPaint)
            }
        }
        waterPaint.style = Paint.Style.FILL

        // Sun glint column
        if (sky.sunY in 0f..1f) {
            glintPaint.color = 0xFFFFF8E0.toInt()
            val cx = w * 0.5f
            val rng = PRNG(450)
            for (i in 0 until 8) {
                val gy = waterTop + rng.next() * (waterBot - waterTop)
                val gx = cx + (rng.next() - 0.5f) * w * 0.06f
                val twinkle = (Math.sin(params.elapsedMs / 400.0 + rng.next() * 10) * 0.5 + 0.5).toFloat()
                glintPaint.alpha = (twinkle * 100).toInt().coerceIn(0, 255)
                canvas.drawCircle(gx, gy, 2f + rng.next() * 2f, glintPaint)
            }
        }
        waterPaint.alpha = 255
    }

    private fun drawLilyPads(canvas: Canvas, params: SceneParams) {
        lilyPaint.color = 0xFF4A8A3A.toInt()
        val rng = PRNG(500)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val y = h * 0.6f + rng.next() * h * 0.15f
            val r = 6f + rng.next() * 5f
            val drift = (Math.sin(params.elapsedMs / 3000.0 + i) * 2).toFloat()
            canvas.drawOval(x - r + drift, y - r * 0.5f, x + r + drift, y + r * 0.5f, lilyPaint)

            // Flower on some - only in spring/summer
            if (rng.next() > 0.5f && (params.season == Season.SPRING || params.season == Season.SUMMER)) {
                lilyPaint.color = 0xFFE8A0C0.toInt()
                canvas.drawCircle(x + drift, y - 2f, 3f, lilyPaint)
                lilyPaint.color = 0xFF4A8A3A.toInt()
            }
        }
    }

    private fun drawReeds(canvas: Canvas, params: SceneParams) {
        reedPaint.color = 0xFF4A6A38.toInt()
        reedPaint.strokeWidth = 2.5f
        reedPaint.style = Paint.Style.STROKE
        reedPaint.strokeCap = Paint.Cap.ROUND

        val rng = PRNG(600)
        // Left side reeds
        for (i in 0 until 6) {
            val x = rng.next() * w * 0.2f
            val baseY = h * 0.78f + rng.next() * h * 0.05f
            val topY = baseY - h * 0.1f - rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i * 1.3) * 6 * params.intensity).toFloat()
            canvas.drawLine(x, baseY, x + sway, topY, reedPaint)
        }
        // Right side reeds
        for (i in 0 until 6) {
            val x = w * 0.8f + rng.next() * w * 0.2f
            val baseY = h * 0.78f + rng.next() * h * 0.05f
            val topY = baseY - h * 0.1f - rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i * 1.1) * 6 * params.intensity).toFloat()
            canvas.drawLine(x, baseY, x + sway, topY, reedPaint)
        }
        reedPaint.style = Paint.Style.FILL
    }

    private fun drawWaterMist(canvas: Canvas, params: SceneParams) {
        // Mist on cold mornings, winter, or fog weather
        val isColdMorning = params.timeOfDay in 5f..8f
        val isWinter = params.season == Season.WINTER
        val isFog = params.weather == WeatherType.FOG
        if (!isColdMorning && !isWinter && !isFog) return

        val mistAlpha = when {
            isFog -> 55
            isWinter -> 40
            else -> 30
        }
        mistPaint.color = 0xFFD8D8D4.toInt()
        val rng = PRNG(800)
        for (i in 0 until 5) {
            val mx = rng.next() * w + (Math.sin(params.elapsedMs / 6000.0 + i) * 12).toFloat()
            val my = h * 0.48f + rng.next() * h * 0.06f
            mistPaint.alpha = (mistAlpha + (rng.next() * 15).toInt()).coerceIn(0, 255)
            canvas.drawOval(mx - 50f, my - 10f, mx + 50f, my + 10f, mistPaint)
        }
        mistPaint.alpha = 255
    }

    private fun drawForegroundBank(canvas: Canvas, params: SceneParams) {
        bankPaint.color = 0xFF3A4A30.toInt()
        canvas.drawRect(0f, h * 0.82f, w.toFloat(), h.toFloat(), bankPaint)

        // Cattails
        bankPaint.color = 0xFF2A3A20.toInt()
        bankPaint.strokeWidth = 3f
        bankPaint.style = Paint.Style.STROKE
        val rng = PRNG(700)
        for (i in 0 until 5) {
            val x = rng.next() * w
            val baseY = h * 0.85f + rng.next() * h * 0.08f
            val topY = baseY - h * 0.12f - rng.next() * h * 0.06f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + i) * 4 * params.intensity).toFloat()
            canvas.drawLine(x, baseY, x + sway, topY, bankPaint)

            // Cattail head
            bankPaint.style = Paint.Style.FILL
            bankPaint.color = 0xFF5A3A20.toInt()
            canvas.drawOval(x + sway - 3f, topY - 10f, x + sway + 3f, topY + 2f, bankPaint)
            bankPaint.style = Paint.Style.STROKE
            bankPaint.color = 0xFF2A3A20.toInt()
        }
        bankPaint.style = Paint.Style.FILL
    }
}
