package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

class EclipseScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val totality = isTotality(params.timeOfDay)
        val sky = interpolateSky(params.timeOfDay)
        val haze = hazeColor(sky)

        drawSky(canvas, params, totality)
        if (totality) drawStars(canvas, w, h, 0.85f, params.elapsedMs)

        drawDistantClouds(canvas, sky, haze, totality, params)

        if (totality) {
            if (params.intensity > 0.05f) drawCorona(canvas, params)
            drawTotalEclipse(canvas)
        } else {
            drawPartialEclipse(canvas, params)
        }

        drawHorizonGlow(canvas, totality)
        drawDistantMountains(canvas, haze, totality)
        drawMidMountains(canvas, haze, totality)
        drawNearHorizon(canvas, params, totality)
        drawObservers(canvas)
        drawForegroundGrass(canvas, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), if (totality) 160 else 80), strength = 0.65f)
    }

    private fun isTotality(time: Float) = time in 15f..16f

    private fun centerpieceR(): Float = w * 0.18f  // ~Much larger than the other scenes' suns.

    private fun drawSky(canvas: Canvas, params: SceneParams, totality: Boolean) {
        val topColor: Int
        val midColor: Int
        val botColor: Int
        if (totality) {
            topColor = 0xFF06081A.toInt()
            midColor = 0xFF161A30.toInt()
            botColor = 0xFF2A1E3A.toInt()
        } else {
            val sky = interpolateSky(params.timeOfDay)
            topColor = sky.topColor
            midColor = sky.midColor
            botColor = sky.botColor
        }
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(topColor, midColor, botColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawDistantClouds(canvas: Canvas, sky: SkyState, haze: Int, totality: Boolean, params: SceneParams) {
        if (totality) return
        val warm = lerpColor(0xFFFFE0CC.toInt(), sky.midColor, 0.40f)
        val rim = lerpColor(0xFFFFF6E0.toInt(), sky.topColor, 0.20f)
        val rng = PRNG(81)
        val drift = (params.elapsedMs * 0.000004f) % 1f
        for (i in 0 until 3) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.40f + rng.next() * 0.10f)
            val scale = h * (0.025f + rng.next() * 0.020f)
            drawFluffyCloud(canvas, cx, cy, scale, warm, rim, alpha = 170)
        }
    }

    private fun drawPartialEclipse(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = centerpieceR()

        // Big soft glow halo (multi-pass)
        paint.shader = RadialGradient(
            cx, cy, r * 3.5f,
            withAlpha(0xFFFFE8B0.toInt(), 100), withAlpha(0xFFFFE8B0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 3.5f, paint)
        paint.shader = RadialGradient(
            cx, cy, r * 1.7f,
            withAlpha(0xFFFFEAC0.toInt(), 180), withAlpha(0xFFFFEAC0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 1.7f, paint)
        // Sun disk
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(0xFFFFF8E2.toInt(), 0xFFFFD074.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // Moon covering part of sun
        val coverage = when {
            params.timeOfDay < 14f -> (params.timeOfDay - 12f) / 2f
            params.timeOfDay > 16f -> 1f - (params.timeOfDay - 16f) / 2f
            else -> 1f
        }.coerceIn(0f, 1f)
        val moonOffset = r * 2f * (1f - coverage)
        paint.shader = RadialGradient(
            cx + moonOffset - r * 0.3f, cy - r * 0.3f, r * 1.4f,
            intArrayOf(0xFF2A2A38.toInt(), 0xFF06060A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx + moonOffset, cy, r * 1.02f, paint)
        paint.shader = null
    }

    private fun drawTotalEclipse(canvas: Canvas) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = centerpieceR()
        paint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            intArrayOf(0xFF1A1A22.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
    }

    private fun drawCorona(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = centerpieceR()

        // multi-layer outer glow
        paint.shader = RadialGradient(
            cx, cy, r * 4f,
            withAlpha(0xFFFFEBD0.toInt(), 70), withAlpha(0xFFFFEBD0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 4f, paint)
        paint.shader = RadialGradient(
            cx, cy, r * 2.4f,
            withAlpha(0xFFFFE0E8.toInt(), 130), withAlpha(0xFFFFE0E8.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 2.4f, paint)
        paint.shader = RadialGradient(
            cx, cy, r * 1.45f,
            withAlpha(0xFFFFFFFF.toInt(), 180), withAlpha(0xFFFFFFFF.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 1.45f, paint)
        paint.shader = null

        // Corona rays
        paint.color = 0xFFFFFFFF.toInt()
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rotation = (params.elapsedMs / 30000f) % 360f
        val rng = PRNG(99)
        for (i in 0 until 32) {
            val angle = Math.toRadians((i * 11.25f + rotation).toDouble())
            val len = r * (1.4f + rng.next() * 2.2f)
            val innerR = r * 1.06f
            val x1 = cx + (Math.cos(angle) * innerR).toFloat()
            val y1 = cy + (Math.sin(angle) * innerR).toFloat()
            val x2 = cx + (Math.cos(angle) * len).toFloat()
            val y2 = cy + (Math.sin(angle) * len).toFloat()
            paint.alpha = (60 + rng.next() * 80f).toInt().coerceIn(0, 255)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.alpha = 255
    }

    private fun drawHorizonGlow(canvas: Canvas, totality: Boolean) {
        if (totality) {
            // 360° sunset glow during totality
            val glowColor = 0xFFD47858.toInt()
            paint.shader = LinearGradient(
                0f, h * 0.62f, 0f, h * 0.86f,
                intArrayOf(
                    0x00000000,
                    withAlpha(glowColor, 90),
                    withAlpha(glowColor, 55),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.45f, 0.75f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.62f, w.toFloat(), h * 0.86f, paint)
            paint.shader = null

            // edge tint
            val edge = 0xFFD88060.toInt()
            paint.shader = LinearGradient(
                0f, 0f, w * 0.2f, 0f,
                withAlpha(edge, 70), 0x00000000, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.66f, w * 0.2f, h * 0.84f, paint)
            paint.shader = LinearGradient(
                w.toFloat(), 0f, w * 0.8f, 0f,
                withAlpha(edge, 70), 0x00000000, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(w * 0.8f, h * 0.66f, w.toFloat(), h * 0.84f, paint)
            paint.shader = null
        } else {
            val glowColor = 0xFFE8A868.toInt()
            paint.shader = LinearGradient(
                0f, h * 0.68f, 0f, h * 0.86f,
                0x00000000, withAlpha(glowColor, 90), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.68f, w.toFloat(), h * 0.86f, paint)
            paint.shader = null
        }
    }

    private fun drawDistantMountains(canvas: Canvas, haze: Int, totality: Boolean) {
        val base = if (totality) 0xFF2A2A40.toInt() else 0xFF3A3C50.toInt()
        smoothRidgePath(path, w, h, h * 0.70f, h * 0.05f, seed = 111)
        drawAerialLayer(canvas, path, h * 0.62f, h * 0.78f, base, haze, depth = 0.75f)
    }

    private fun drawMidMountains(canvas: Canvas, haze: Int, totality: Boolean) {
        val base = if (totality) 0xFF1A1828.toInt() else 0xFF26242E.toInt()
        smoothRidgePath(path, w, h, h * 0.76f, h * 0.045f, seed = 222)
        drawAerialLayer(canvas, path, h * 0.70f, h * 0.83f, base, haze, depth = 0.45f)
    }

    private fun drawNearHorizon(canvas: Canvas, params: SceneParams, totality: Boolean) {
        val base = if (totality) 0xFF0A0814.toInt() else 0xFF161320.toInt()
        paint.shader = LinearGradient(
            0f, h * 0.80f, 0f, h.toFloat(),
            lerpColor(base, 0xFFFFFFFF.toInt(), 0.05f), 0xFF030308.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.80f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val pineColor = 0xFF050309.toInt()
        val rng = PRNG(333)
        for (i in 0 until 20) {
            val tx = rng.next() * w
            val baseY = h * 0.80f + rng.next() * h * 0.01f
            val treeH = h * 0.04f + rng.next() * h * 0.06f
            drawPaintedConifer(canvas, tx, baseY, treeH, pineColor)
        }
    }

    private fun drawObservers(canvas: Canvas) {
        paint.color = 0xFF050309.toInt()
        val groundY = h * 0.84f

        // Adult
        val ax = w * 0.4f
        canvas.drawCircle(ax, groundY - 28f, 5f, paint)
        canvas.drawRect(ax - 4f, groundY - 23f, ax + 4f, groundY - 8f, paint)
        canvas.drawRect(ax - 6f, groundY - 8f, ax - 2f, groundY, paint)
        canvas.drawRect(ax + 2f, groundY - 8f, ax + 6f, groundY, paint)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(ax + 4f, groundY - 20f, ax + 14f, groundY - 32f, paint)
        paint.style = Paint.Style.FILL

        // Child
        val cx = w * 0.45f
        canvas.drawCircle(cx, groundY - 18f, 4f, paint)
        canvas.drawRect(cx - 3f, groundY - 14f, cx + 3f, groundY - 4f, paint)
        canvas.drawRect(cx - 4f, groundY - 4f, cx - 1f, groundY, paint)
        canvas.drawRect(cx + 1f, groundY - 4f, cx + 4f, groundY, paint)

        // Telescope
        val tx = w * 0.58f
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(tx, groundY - 16f, tx - 8f, groundY, paint)
        canvas.drawLine(tx, groundY - 16f, tx + 8f, groundY, paint)
        canvas.drawLine(tx, groundY - 16f, tx, groundY, paint)
        canvas.drawLine(tx - 2f, groundY - 16f, tx + 12f, groundY - 30f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawForegroundGrass(canvas: Canvas, params: SceneParams) {
        paint.color = 0xFF080610.toInt()
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rng = PRNG(444)
        for (i in 0 until 24) {
            val x = rng.next() * w
            val baseY = h * 0.90f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 3000.0) + i) * 3 * params.intensity).toFloat()
            val bladeH = 10f + rng.next() * 16f
            canvas.drawLine(x, baseY, x + sway, baseY - bladeH, paint)
            canvas.drawLine(x + 3f, baseY, x + 3f + sway * 0.8f, baseY - bladeH * 0.7f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }
}
