package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class EclipseScene : VantageScene {

    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coronaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mtPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val figurePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val totality = isTotality(params.timeOfDay)
        drawSky(canvas, params, totality)

        if (!totality) {
            drawPartialEclipse(canvas, params)
        }

        drawStars(canvas, w, h, if (totality) 0.8f else 0f, params.elapsedMs)

        if (totality) {
            // Corona rays only during totality AND intensity > 0.05
            if (params.intensity > 0.05f) {
                drawCorona(canvas, params)
            }
            drawTotalEclipse(canvas)
        }

        drawHorizonGlow(canvas, totality)
        drawDistantMountains(canvas)
        drawMidMountains(canvas)
        drawNearHorizon(canvas, params)
        drawObservers(canvas)
        drawForegroundGrass(canvas, params)
    }

    private fun isTotality(time: Float) = time in 15f..16f

    private fun drawSky(canvas: Canvas, params: SceneParams, totality: Boolean) {
        val topColor: Int
        val botColor: Int
        if (totality) {
            topColor = 0xFF0A0E1A.toInt()
            botColor = 0xFF1A2038.toInt()
        } else {
            val sky = interpolateSky(params.timeOfDay)
            topColor = sky.topColor
            botColor = sky.botColor
        }
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            topColor, botColor,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
        skyPaint.shader = null
    }

    private fun drawPartialEclipse(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = 36f

        // Sun
        sunPaint.color = 0xFFFFF4D0.toInt()
        canvas.drawCircle(cx, cy, r, sunPaint)

        // Moon covering part of sun
        val coverage = when {
            params.timeOfDay < 14f -> (params.timeOfDay - 12f) / 2f
            params.timeOfDay > 16f -> 1f - (params.timeOfDay - 16f) / 2f
            else -> 1f
        }.coerceIn(0f, 1f)

        moonPaint.color = 0xFF1A1A20.toInt()
        val moonOffset = r * 2f * (1f - coverage)
        canvas.drawCircle(cx + moonOffset, cy, r * 1.02f, moonPaint)
    }

    private fun drawTotalEclipse(canvas: Canvas) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = 36f

        // Black moon disk
        moonPaint.color = 0xFF000000.toInt()
        canvas.drawCircle(cx, cy, r, moonPaint)
    }

    private fun drawCorona(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f
        val cy = h * 0.25f
        val r = 36f

        // Outer corona glow
        coronaPaint.color = 0xFFE8E0F0.toInt()
        coronaPaint.alpha = 30
        canvas.drawCircle(cx, cy, r * 3f, coronaPaint)
        coronaPaint.alpha = 50
        canvas.drawCircle(cx, cy, r * 2f, coronaPaint)
        coronaPaint.alpha = 80
        canvas.drawCircle(cx, cy, r * 1.4f, coronaPaint)

        // Corona rays
        coronaPaint.color = 0xFFFFFFFF.toInt()
        coronaPaint.strokeWidth = 1.5f
        coronaPaint.style = Paint.Style.STROKE
        val rotation = (params.elapsedMs / 30000f) % 360f
        val rng = PRNG(99)
        for (i in 0 until 24) {
            val angle = Math.toRadians((i * 15f + rotation).toDouble())
            val len = r * (1.5f + rng.next() * 2f)
            val innerR = r * 1.05f
            val x1 = cx + (Math.cos(angle) * innerR).toFloat()
            val y1 = cy + (Math.sin(angle) * innerR).toFloat()
            val x2 = cx + (Math.cos(angle) * len).toFloat()
            val y2 = cy + (Math.sin(angle) * len).toFloat()
            coronaPaint.alpha = (40 + rng.next() * 50).toInt().coerceIn(0, 255)
            canvas.drawLine(x1, y1, x2, y2, coronaPaint)
        }
        coronaPaint.style = Paint.Style.FILL
        coronaPaint.alpha = 255
    }

    private fun drawHorizonGlow(canvas: Canvas, totality: Boolean) {
        if (totality) {
            // 360-degree sunset glow at horizon during totality
            // Warm glow band spanning the full horizon
            val glowColor = 0xFFC87458.toInt()
            horizonPaint.shader = LinearGradient(
                0f, h * 0.65f, 0f, h * 0.85f,
                intArrayOf(0x00000000, glowColor and 0x50FFFFFF, glowColor and 0x38FFFFFF, 0x00000000),
                floatArrayOf(0f, 0.4f, 0.7f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.65f, w.toFloat(), h * 0.85f, horizonPaint)
            horizonPaint.shader = null

            // Additional warm tint at edges for 360-degree effect
            val edgeGlow = 0xFFD08060.toInt()
            horizonPaint.shader = LinearGradient(
                0f, 0f, w * 0.15f, 0f,
                edgeGlow and 0x28FFFFFF, 0x00000000,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.68f, w * 0.15f, h * 0.82f, horizonPaint)
            horizonPaint.shader = LinearGradient(
                w.toFloat(), 0f, w * 0.85f, 0f,
                edgeGlow and 0x28FFFFFF, 0x00000000,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(w * 0.85f, h * 0.68f, w.toFloat(), h * 0.82f, horizonPaint)
            horizonPaint.shader = null
        } else {
            val glowColor = 0xFFE8A868.toInt()
            horizonPaint.shader = LinearGradient(
                0f, h * 0.7f, 0f, h * 0.85f,
                0x00000000, glowColor and 0x40FFFFFF,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.7f, w.toFloat(), h * 0.85f, horizonPaint)
            horizonPaint.shader = null
        }
    }

    private fun drawDistantMountains(canvas: Canvas) {
        mtPaint.color = 0xFF2A2838.toInt()
        mtPaint.alpha = 180
        path.reset()
        path.moveTo(0f, h * 0.72f)
        val rng = PRNG(111)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.68f + rng.next() * 0.06f))
            x += w * 0.08f
        }
        path.lineTo(w.toFloat(), h * 0.75f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, mtPaint)
        mtPaint.alpha = 255
    }

    private fun drawMidMountains(canvas: Canvas) {
        mtPaint.color = 0xFF1E1C28.toInt()
        path.reset()
        path.moveTo(0f, h * 0.76f)
        val rng = PRNG(222)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.72f + rng.next() * 0.05f))
            x += w * 0.06f
        }
        path.lineTo(w.toFloat(), h * 0.78f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, mtPaint)
    }

    private fun drawNearHorizon(canvas: Canvas, params: SceneParams) {
        mtPaint.color = 0xFF141218.toInt()
        canvas.drawRect(0f, h * 0.8f, w.toFloat(), h.toFloat(), mtPaint)

        // Tree silhouettes
        treePaint.color = 0xFF0A0810.toInt()
        val rng = PRNG(333)
        for (i in 0 until 15) {
            val tx = rng.next() * w
            val baseY = h * 0.8f
            val treeH = h * 0.04f + rng.next() * h * 0.06f
            val treeW = treeH * 0.3f
            path.reset()
            path.moveTo(tx, baseY - treeH)
            path.lineTo(tx - treeW, baseY)
            path.lineTo(tx + treeW, baseY)
            path.close()
            canvas.drawPath(path, treePaint)
            // Trunk
            canvas.drawRect(tx - 1.5f, baseY - treeH * 0.3f, tx + 1.5f, baseY, treePaint)
        }
    }

    private fun drawObservers(canvas: Canvas) {
        figurePaint.color = 0xFF0A0810.toInt()
        val groundY = h * 0.84f

        // Adult figure
        val ax = w * 0.4f
        canvas.drawCircle(ax, groundY - 28f, 5f, figurePaint)
        canvas.drawRect(ax - 4f, groundY - 23f, ax + 4f, groundY - 8f, figurePaint)
        canvas.drawRect(ax - 6f, groundY - 8f, ax - 2f, groundY, figurePaint)
        canvas.drawRect(ax + 2f, groundY - 8f, ax + 6f, groundY, figurePaint)
        // Arm pointing up
        figurePaint.strokeWidth = 2f
        figurePaint.style = Paint.Style.STROKE
        canvas.drawLine(ax + 4f, groundY - 20f, ax + 14f, groundY - 32f, figurePaint)
        figurePaint.style = Paint.Style.FILL

        // Child figure
        val cx = w * 0.45f
        canvas.drawCircle(cx, groundY - 18f, 4f, figurePaint)
        canvas.drawRect(cx - 3f, groundY - 14f, cx + 3f, groundY - 4f, figurePaint)
        canvas.drawRect(cx - 4f, groundY - 4f, cx - 1f, groundY, figurePaint)
        canvas.drawRect(cx + 1f, groundY - 4f, cx + 4f, groundY, figurePaint)

        // Telescope
        val tx = w * 0.58f
        // Tripod legs
        figurePaint.strokeWidth = 1.5f
        figurePaint.style = Paint.Style.STROKE
        canvas.drawLine(tx, groundY - 16f, tx - 8f, groundY, figurePaint)
        canvas.drawLine(tx, groundY - 16f, tx + 8f, groundY, figurePaint)
        canvas.drawLine(tx, groundY - 16f, tx, groundY, figurePaint)
        // Tube
        canvas.drawLine(tx - 2f, groundY - 16f, tx + 12f, groundY - 30f, figurePaint)
        figurePaint.style = Paint.Style.FILL
    }

    private fun drawForegroundGrass(canvas: Canvas, params: SceneParams) {
        grassPaint.color = 0xFF0A0810.toInt()
        grassPaint.strokeWidth = 2f
        grassPaint.style = Paint.Style.STROKE
        val rng = PRNG(444)
        for (i in 0 until 20) {
            val x = rng.next() * w
            val baseY = h * 0.9f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 3000.0) + i) * 3 * params.intensity).toFloat()
            val bladeH = 10f + rng.next() * 15f
            canvas.drawLine(x, baseY, x + sway, baseY - bladeH, grassPaint)
            canvas.drawLine(x + 3f, baseY, x + 3f + sway * 0.8f, baseY - bladeH * 0.7f, grassPaint)
        }
        grassPaint.style = Paint.Style.FILL
    }
}
