package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class SpaceScene : VantageScene {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val auroraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val planetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cometPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        drawBackground(canvas)
        drawStarField(canvas, params)
        drawNebula(canvas, params)
        drawAurora(canvas, params)
        drawComet(canvas, params)
        drawSmallMoons(canvas, params)
        drawPlanet(canvas, params)
        drawRings(canvas, params)
        drawAtmosphereHalo(canvas)
        drawCraterRim(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF08081A.toInt(), 0xFF0A0E1E.toInt(), 0xFF101828.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        bgPaint.shader = null
    }

    private fun drawStarField(canvas: Canvas, params: SceneParams) {
        starPaint.color = 0xFFFFFFFF.toInt()
        val rng = PRNG(10)
        for (i in 0 until 120) {
            val x = rng.next() * w
            val y = rng.next() * h
            val size = 0.3f + rng.next() * 1.2f
            val twinkle = (Math.sin((params.elapsedMs / 1200.0 + rng.next() * 20)) * 0.3 + 0.7).toFloat()
            starPaint.alpha = (twinkle * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, size, starPaint)
        }

        // Bright stars with cross flare
        for (i in 0 until 8) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.7f
            val brightness = (Math.sin((params.elapsedMs / 2000.0 + rng.next() * 10)) * 0.4 + 0.6).toFloat()
            starPaint.alpha = (brightness * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, 2f, starPaint)
            starPaint.alpha = (brightness * 80).toInt().coerceIn(0, 255)
            starPaint.strokeWidth = 0.5f
            starPaint.style = Paint.Style.STROKE
            canvas.drawLine(x - 6f, y, x + 6f, y, starPaint)
            canvas.drawLine(x, y - 6f, x, y + 6f, starPaint)
            starPaint.style = Paint.Style.FILL
        }
        starPaint.alpha = 255
    }

    private fun drawNebula(canvas: Canvas, params: SceneParams) {
        nebulaPaint.style = Paint.Style.FILL
        val rng = PRNG(20)
        val colors = intArrayOf(0xFF3A2050.toInt(), 0xFF2A3060.toInt(), 0xFF402848.toInt(), 0xFF1A3058.toInt())
        for (i in 0 until 6) {
            nebulaPaint.color = colors[i % colors.size]
            nebulaPaint.alpha = 25 + (rng.next() * 20).toInt()
            val cx = rng.next() * w
            val cy = rng.next() * h * 0.5f
            val rx = 60f + rng.next() * 100f
            val ry = 30f + rng.next() * 60f
            val drift = (Math.sin(params.elapsedMs / 10000.0 + i) * 5).toFloat()
            canvas.drawOval(cx - rx + drift, cy - ry, cx + rx + drift, cy + ry, nebulaPaint)
        }
        nebulaPaint.alpha = 255
    }

    private fun drawAurora(canvas: Canvas, params: SceneParams) {
        auroraPaint.style = Paint.Style.STROKE
        auroraPaint.strokeWidth = 3f
        val colors = intArrayOf(0xFF30A060.toInt(), 0xFF2080A0.toInt(), 0xFF4060C0.toInt())
        for (band in 0 until 3) {
            auroraPaint.color = colors[band]
            auroraPaint.alpha = 35
            path.reset()
            val baseY = h * 0.15f + band * h * 0.06f
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val y = baseY + (Math.sin((x * 0.008 + params.elapsedMs / 4000.0 + band * 1.5)) * h * 0.04).toFloat()
                path.lineTo(x, y)
                x += 4f
            }
            canvas.drawPath(path, auroraPaint)
        }
        auroraPaint.style = Paint.Style.FILL
        auroraPaint.alpha = 255
    }

    private fun drawComet(canvas: Canvas, params: SceneParams) {
        val cycle = 30000L
        val t = (params.elapsedMs % cycle).toFloat() / cycle
        if (t > 0.15f) return

        val progress = t / 0.15f
        val cx = w * (1f - progress * 1.3f)
        val cy = h * (0.05f + progress * 0.25f)

        cometPaint.color = 0xFFFFFFFF.toInt()
        cometPaint.alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, 2f, cometPaint)

        // Trail
        cometPaint.strokeWidth = 1.5f
        cometPaint.style = Paint.Style.STROKE
        cometPaint.alpha = ((1f - progress) * 120).toInt().coerceIn(0, 255)
        canvas.drawLine(cx, cy, cx + 40f * progress + 20f, cy - 15f * progress - 8f, cometPaint)
        cometPaint.style = Paint.Style.FILL
        cometPaint.alpha = 255
    }

    private fun drawSmallMoons(canvas: Canvas, params: SceneParams) {
        planetPaint.color = 0xFFA0A0A8.toInt()
        val orbit1 = params.elapsedMs / 20000.0
        val mx1 = w * 0.3f + (Math.cos(orbit1) * w * 0.08f).toFloat()
        val my1 = h * 0.35f + (Math.sin(orbit1) * h * 0.03f).toFloat()
        planetPaint.alpha = 180
        canvas.drawCircle(mx1, my1, 6f, planetPaint)

        planetPaint.color = 0xFF8A8A90.toInt()
        val orbit2 = params.elapsedMs / 35000.0 + 3.0
        val mx2 = w * 0.72f + (Math.cos(orbit2) * w * 0.05f).toFloat()
        val my2 = h * 0.3f + (Math.sin(orbit2) * h * 0.02f).toFloat()
        planetPaint.alpha = 140
        canvas.drawCircle(mx2, my2, 4f, planetPaint)
        planetPaint.alpha = 255
    }

    private fun drawPlanet(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f
        val cy = h * 0.55f
        val r = w * 0.22f

        // Planet body
        planetPaint.color = 0xFF4A6080.toInt()
        canvas.drawCircle(cx, cy, r, planetPaint)

        // Cloud bands
        val bandOffset = (params.elapsedMs / 15000f) % (r * 2)
        val bandColors = intArrayOf(0xFF5A7090.toInt(), 0xFF3A5070.toInt(), 0xFF6A80A0.toInt(), 0xFF4A5A78.toInt())
        for (i in 0 until 6) {
            planetPaint.color = bandColors[i % bandColors.size]
            planetPaint.alpha = 60
            val bandY = cy - r + i * r * 0.35f + bandOffset * 0.1f
            val halfW = (Math.sqrt((r * r - (bandY - cy) * (bandY - cy)).coerceAtLeast(0f).toDouble())).toFloat()
            if (halfW > 5f) {
                canvas.drawRect(cx - halfW, bandY - 4f, cx + halfW, bandY + 4f, planetPaint)
            }
        }

        // Terminator (day/night shadow)
        planetPaint.color = 0xFF000020.toInt()
        planetPaint.alpha = 100
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -60f, 180f, true, planetPaint)
        planetPaint.alpha = 255
    }

    private fun drawRings(canvas: Canvas, params: SceneParams) {
        ringPaint.style = Paint.Style.STROKE
        ringPaint.color = 0xFFA0A0B0.toInt()
        for (i in 0 until 4) {
            val rx = w * 0.34f + i * 8f
            val ry = w * 0.06f + i * 2f
            ringPaint.alpha = 60 - i * 10
            ringPaint.strokeWidth = 3f - i * 0.5f
            canvas.drawOval(
                w * 0.5f - rx, h * 0.55f - ry,
                w * 0.5f + rx, h * 0.55f + ry,
                ringPaint,
            )
        }
        ringPaint.style = Paint.Style.FILL
        ringPaint.alpha = 255
    }

    private fun drawAtmosphereHalo(canvas: Canvas) {
        haloPaint.color = 0xFF6090C0.toInt()
        haloPaint.alpha = 20
        haloPaint.style = Paint.Style.STROKE
        haloPaint.strokeWidth = 8f
        canvas.drawCircle(w * 0.5f, h * 0.55f, w * 0.23f, haloPaint)
        haloPaint.alpha = 10
        haloPaint.strokeWidth = 16f
        canvas.drawCircle(w * 0.5f, h * 0.55f, w * 0.25f, haloPaint)
        haloPaint.style = Paint.Style.FILL
        haloPaint.alpha = 255
    }

    private fun drawCraterRim(canvas: Canvas) {
        craterPaint.color = 0xFF2A2A30.toInt()
        path.reset()
        path.moveTo(0f, h * 0.88f)
        val rng = PRNG(50)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h * (0.85f + rng.next() * 0.04f))
            x += w * 0.05f
        }
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
        canvas.drawPath(path, craterPaint)

        // Crater details
        craterPaint.color = 0xFF222228.toInt()
        for (i in 0 until 4) {
            val cx = rng.next() * w
            val cy = h * 0.9f + rng.next() * h * 0.06f
            canvas.drawOval(cx - 12f, cy - 3f, cx + 12f, cy + 3f, craterPaint)
        }
    }
}
