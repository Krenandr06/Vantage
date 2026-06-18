package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class MountainsScene : VantageScene {

    private val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lakePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

        drawFarRidge(canvas, sky, params)
        drawMidRidge(canvas, params, sky)
        drawMainRidge(canvas, params)
        drawNearRidge(canvas, params)
        drawLake(canvas, sky, params)
        drawValleyMist(canvas, params)
        drawForeground(canvas, params)
    }

    private fun drawFarRidge(canvas: Canvas, sky: SkyState, params: SceneParams) {
        ridgePaint.color = lerpColor(0xFF6A7A9A.toInt(), sky.midColor, 0.4f)
        path.reset()
        path.moveTo(0f, h * 0.35f)
        val rng = PRNG(111)
        var x = 0f
        while (x <= w) {
            val peakH = h * (0.28f + rng.next() * 0.08f)
            path.lineTo(x, peakH)
            x += w * 0.08f + rng.next() * w * 0.06f
        }
        path.lineTo(w.toFloat(), h * 0.45f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, ridgePaint)

        // Snow caps on far ridge
        val snowAmount = when (params.season) {
            Season.WINTER -> 0.05f
            Season.AUTUMN, Season.SPRING -> 0.03f
            Season.SUMMER -> 0.01f
        }
        snowPaint.color = 0xFFE8E4E0.toInt()
        snowPaint.alpha = 160
        val rng2 = PRNG(111)
        x = 0f
        while (x <= w) {
            val peakH = h * (0.28f + rng2.next() * 0.08f)
            val capH = h * snowAmount
            path.reset()
            path.moveTo(x - w * 0.025f, peakH + capH)
            path.lineTo(x, peakH)
            path.lineTo(x + w * 0.025f, peakH + capH)
            path.close()
            canvas.drawPath(path, snowPaint)
            x += w * 0.08f + rng2.next() * w * 0.06f
        }
        snowPaint.alpha = 255
    }

    private fun drawMidRidge(canvas: Canvas, params: SceneParams, sky: SkyState) {
        ridgePaint.color = lerpColor(0xFF5A6A7A.toInt(), sky.botColor, 0.2f)
        path.reset()
        path.moveTo(0f, h * 0.4f)
        val rng = PRNG(222)
        var x = 0f
        while (x <= w) {
            val peakH = h * (0.32f + rng.next() * 0.1f)
            path.lineTo(x, peakH)
            x += w * 0.07f + rng.next() * w * 0.05f
        }
        path.lineTo(w.toFloat(), h * 0.48f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, ridgePaint)

        // Snow caps
        val snowAmount = when (params.season) {
            Season.WINTER -> 0.07f
            Season.AUTUMN, Season.SPRING -> 0.04f
            Season.SUMMER -> 0.02f
        }
        snowPaint.color = 0xFFE8E4E0.toInt()
        snowPaint.alpha = 200
        val rng2 = PRNG(222)
        x = 0f
        while (x <= w) {
            val peakH = h * (0.32f + rng2.next() * 0.1f)
            val capH = h * snowAmount
            path.reset()
            val px = x
            path.moveTo(px - w * 0.03f, peakH + capH)
            path.lineTo(px, peakH)
            path.lineTo(px + w * 0.03f, peakH + capH)
            path.close()
            canvas.drawPath(path, snowPaint)
            x += w * 0.07f + rng2.next() * w * 0.05f
        }
        snowPaint.alpha = 255
    }

    private fun drawMainRidge(canvas: Canvas, params: SceneParams) {
        ridgePaint.color = 0xFF3A4A3A.toInt()
        path.reset()
        path.moveTo(0f, h * 0.48f)
        val rng = PRNG(333)
        var x = 0f
        while (x <= w) {
            val peakH = h * (0.42f + rng.next() * 0.08f)
            path.lineTo(x, peakH)
            x += w * 0.05f + rng.next() * w * 0.04f
        }
        path.lineTo(w.toFloat(), h * 0.55f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, ridgePaint)

        // Pine silhouettes on ridge
        pinePaint.color = 0xFF2A3A2A.toInt()
        val rng2 = PRNG(3330)
        for (i in 0 until 20) {
            val px = rng2.next() * w
            val baseY = h * 0.46f + rng2.next() * h * 0.06f
            val treeH = h * 0.03f + rng2.next() * h * 0.04f
            val treeW = treeH * 0.3f
            path.reset()
            path.moveTo(px, baseY - treeH)
            path.lineTo(px - treeW, baseY)
            path.lineTo(px + treeW, baseY)
            path.close()
            canvas.drawPath(path, pinePaint)
        }
    }

    private fun drawNearRidge(canvas: Canvas, params: SceneParams) {
        ridgePaint.color = 0xFF2A3828.toInt()
        path.reset()
        path.moveTo(0f, h * 0.55f)
        val rng = PRNG(444)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.52f + rng.next() * 0.05f))
            x += w * 0.04f + rng.next() * w * 0.03f
        }
        path.lineTo(w.toFloat(), h * 0.58f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, ridgePaint)

        // Larger pines
        pinePaint.color = 0xFF1A2818.toInt()
        val rng2 = PRNG(4440)
        for (i in 0 until 15) {
            val px = rng2.next() * w
            val baseY = h * 0.54f + rng2.next() * h * 0.04f
            val treeH = h * 0.05f + rng2.next() * h * 0.06f
            val treeW = treeH * 0.25f
            path.reset()
            path.moveTo(px, baseY - treeH)
            path.lineTo(px - treeW, baseY)
            path.lineTo(px + treeW, baseY)
            path.close()
            canvas.drawPath(path, pinePaint)
        }
    }

    private fun drawLake(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val lakeTop = h * 0.6f
        val lakeBot = h * 0.78f

        // Reflection (muted sky colors)
        lakePaint.color = lerpColor(sky.botColor, 0xFF3A5A7A.toInt(), 0.4f)
        lakePaint.alpha = 200
        canvas.drawRect(0f, lakeTop, w.toFloat(), lakeBot, lakePaint)

        // Ripples - sway amplitude scales with intensity
        ripplePaint.color = 0xFFFFFFFF.toInt()
        ripplePaint.style = Paint.Style.STROKE
        ripplePaint.strokeWidth = 1f
        val rng = PRNG(555)
        val rippleIntensity = 0.3f + params.intensity * 0.7f
        for (i in 0 until 8) {
            val rx = rng.next() * w
            val ry = lakeTop + rng.next() * (lakeBot - lakeTop)
            val phase = (params.elapsedMs / 2000.0 + rng.next() * 6.28).toFloat()
            val rippleW = (10f + (Math.sin(phase.toDouble()) * 8).toFloat()) * rippleIntensity
            ripplePaint.alpha = ((30 + (Math.sin(phase.toDouble()) * 20).toInt()) * rippleIntensity).toInt().coerceIn(0, 255)
            canvas.drawLine(rx - rippleW, ry, rx + rippleW, ry, ripplePaint)
        }
        ripplePaint.style = Paint.Style.FILL
        ripplePaint.alpha = 255
    }

    private fun drawValleyMist(canvas: Canvas, params: SceneParams) {
        // Mist at dawn/dusk or when weather is fog
        val isDawnDusk = params.timeOfDay in 5.5f..7.5f || params.timeOfDay in 17f..19f
        val isFog = params.weather == WeatherType.FOG
        if (!isDawnDusk && !isFog) return

        val mistAlpha = if (isFog) 50 else 30
        mistPaint.color = 0xFFD8D4D0.toInt()
        val rng = PRNG(777)
        for (i in 0 until 6) {
            val mx = rng.next() * w + (Math.sin(params.elapsedMs / 5000.0 + i) * 15).toFloat()
            val my = h * 0.56f + rng.next() * h * 0.08f
            mistPaint.alpha = (mistAlpha + (rng.next() * 15).toInt()).coerceIn(0, 255)
            canvas.drawOval(mx - 60f, my - 12f, mx + 60f, my + 12f, mistPaint)
        }
        mistPaint.alpha = 255
    }

    private fun drawForeground(canvas: Canvas, params: SceneParams) {
        // Ground
        fgPaint.color = 0xFF2A3A28.toInt()
        canvas.drawRect(0f, h * 0.78f, w.toFloat(), h.toFloat(), fgPaint)

        // Boulder
        fgPaint.color = 0xFF5A5A52.toInt()
        canvas.drawOval(w * 0.1f, h * 0.8f, w * 0.25f, h * 0.9f, fgPaint)

        // Grass tufts
        fgPaint.color = 0xFF3A5A30.toInt()
        fgPaint.style = Paint.Style.STROKE
        fgPaint.strokeWidth = 2f
        val rng = PRNG(666)
        for (i in 0 until 15) {
            val x = rng.next() * w
            val y = h * 0.82f + rng.next() * h * 0.12f
            val sway = (Math.sin((params.elapsedMs / 2500.0) + i) * 3 * params.intensity).toFloat()
            canvas.drawLine(x, y, x - 5f + sway, y - 15f, fgPaint)
            canvas.drawLine(x, y, x + 4f + sway, y - 12f, fgPaint)
            canvas.drawLine(x, y, x + sway, y - 18f, fgPaint)
        }
        fgPaint.style = Paint.Style.FILL
    }
}
