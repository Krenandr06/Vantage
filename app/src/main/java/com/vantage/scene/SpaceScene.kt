package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

class SpaceScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0
    private var planetDrift = 0f

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        drawBackground(canvas)
        drawDistantStar(canvas, params)
        drawStarField(canvas, params)
        drawNebula(canvas, params)
        drawAurora(canvas, params)
        drawComet(canvas, params)
        drawSmallMoons(canvas, params)
        drawPlanet(canvas, params)
        drawRings(canvas, params)
        drawAtmosphereHalo(canvas)
        drawCraterRim(canvas)
        drawVignette(canvas, w, h, withAlpha(0xFF000000.toInt(), 140), strength = 0.7f)
    }

    private fun drawBackground(canvas: Canvas) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF06081A.toInt(), 0xFF0A1226.toInt(), 0xFF101A30.toInt(), 0xFF1A2440.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // soft horizon glow behind the planet
        val glow = 0xFF3A5A8C.toInt()
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.55f, w * 0.6f,
            withAlpha(glow, 80), withAlpha(glow, 0), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    /** A large distant star that anchors the composition — much bigger than other scenes' sun. */
    private fun drawDistantStar(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.78f
        val cy = h * 0.18f
        val r = w * 0.06f
        // outer halo
        paint.shader = RadialGradient(
            cx, cy, r * 4f,
            withAlpha(0xFFFFE8B0.toInt(), 50), withAlpha(0xFFFFE8B0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 4f, paint)
        // mid halo
        paint.shader = RadialGradient(
            cx, cy, r * 2f,
            withAlpha(0xFFFFEBB8.toInt(), 140), withAlpha(0xFFFFEBB8.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 2f, paint)
        // body
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(0xFFFFFAE2.toInt(), 0xFFFFE3A0.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // a couple of slow flares
        paint.color = withAlpha(0xFFFFE8B0.toInt(), 120)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        val rot = (params.elapsedMs / 40000.0).toFloat()
        for (i in 0 until 4) {
            val a = rot + i * (Math.PI.toFloat() / 2f)
            val x1 = cx + Math.cos(a.toDouble()).toFloat() * r * 1.2f
            val y1 = cy + Math.sin(a.toDouble()).toFloat() * r * 1.2f
            val x2 = cx + Math.cos(a.toDouble()).toFloat() * r * 3.2f
            val y2 = cy + Math.sin(a.toDouble()).toFloat() * r * 3.2f
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun drawStarField(canvas: Canvas, params: SceneParams) {
        paint.color = 0xFFFFFFFF.toInt()
        val rng = PRNG(10)
        for (i in 0 until 150) {
            val x = rng.next() * w
            val y = rng.next() * h
            val size = 0.3f + rng.next() * 1.4f
            val twinkle = (Math.sin((params.elapsedMs / 1200.0 + rng.next() * 20)) * 0.3 + 0.7).toFloat()
            paint.alpha = (twinkle * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, size, paint)
        }
        // bright stars with soft cross flare
        for (i in 0 until 10) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.7f
            val brightness = (Math.sin((params.elapsedMs / 2000.0 + rng.next() * 10)) * 0.4 + 0.6).toFloat()
            drawGlowParticle(canvas, x, y, 2.2f, 0xFFFFFFFF.toInt(), (brightness * 240).toInt().coerceIn(0, 255))
            paint.color = withAlpha(0xFFFFFFFF.toInt(), (brightness * 90).toInt().coerceIn(0, 255))
            paint.strokeWidth = 0.6f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(x - 7f, y, x + 7f, y, paint)
            canvas.drawLine(x, y - 7f, x, y + 7f, paint)
            paint.style = Paint.Style.FILL
        }
        paint.alpha = 255
    }

    private fun drawNebula(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(20)
        val colors = intArrayOf(
            0xFF6A3070.toInt(), 0xFF3A4090.toInt(), 0xFF5A2880.toInt(),
            0xFF2A4090.toInt(), 0xFF80407A.toInt(), 0xFF40508C.toInt(),
        )
        for (i in 0 until 7) {
            val color = colors[i % colors.size]
            val cx = rng.next() * w
            val cy = rng.next() * h * 0.55f
            val rx = 80f + rng.next() * 140f
            val ry = 40f + rng.next() * 80f
            val drift = (Math.sin(params.elapsedMs / 10000.0 + i) * 6).toFloat()
            paint.shader = RadialGradient(
                cx + drift, cy, rx,
                withAlpha(color, 55), withAlpha(color, 0),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(cx - rx + drift, cy - ry, cx + rx + drift, cy + ry, paint)
            paint.shader = null
        }
        paint.alpha = 255
    }

    private fun drawAurora(canvas: Canvas, params: SceneParams) {
        if (params.intensity <= 0.35f) return
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        val colors = intArrayOf(0xFF40C078.toInt(), 0xFF3098B8.toInt(), 0xFF5070D8.toInt())
        for (band in 0 until 3) {
            paint.color = withAlpha(colors[band], 70)
            path.reset()
            val baseY = h * 0.12f + band * h * 0.05f
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val y = baseY + (Math.sin((x * 0.008 + params.elapsedMs / 4000.0 + band * 1.5)) * h * 0.04).toFloat()
                path.lineTo(x, y)
                x += 4f
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun drawComet(canvas: Canvas, params: SceneParams) {
        if (params.intensity <= 0.6f) return
        val cycle = 30000L
        val t = (params.elapsedMs % cycle).toFloat() / cycle
        if (t > 0.15f) return
        val progress = t / 0.15f
        val cx = w * (1f - progress * 1.3f)
        val cy = h * (0.05f + progress * 0.25f)

        paint.color = withAlpha(0xFFFFFFFF.toInt(), ((1f - progress) * 255).toInt().coerceIn(0, 255))
        canvas.drawCircle(cx, cy, 2.5f, paint)

        paint.strokeWidth = 1.8f
        paint.style = Paint.Style.STROKE
        paint.alpha = ((1f - progress) * 130).toInt().coerceIn(0, 255)
        canvas.drawLine(cx, cy, cx + 50f * progress + 22f, cy - 18f * progress - 10f, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun drawSmallMoons(canvas: Canvas, params: SceneParams) {
        val orbit1 = params.elapsedMs / 20000.0
        val mx1 = w * 0.3f + (Math.cos(orbit1) * w * 0.08f).toFloat()
        val my1 = h * 0.35f + (Math.sin(orbit1) * h * 0.03f).toFloat()
        paint.shader = RadialGradient(
            mx1 - 2f, my1 - 2f, 8f,
            intArrayOf(0xFFCCCCD4.toInt(), 0xFF60606A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(mx1, my1, 7f, paint)
        paint.shader = null

        val orbit2 = params.elapsedMs / 35000.0 + 3.0
        val mx2 = w * 0.20f + (Math.cos(orbit2) * w * 0.05f).toFloat()
        val my2 = h * 0.30f + (Math.sin(orbit2) * h * 0.02f).toFloat()
        paint.shader = RadialGradient(
            mx2 - 1f, my2 - 1f, 5f,
            intArrayOf(0xFFB8B8C0.toInt(), 0xFF505058.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(mx2, my2, 4.5f, paint)
        paint.shader = null
    }

    private fun drawPlanet(canvas: Canvas, params: SceneParams) {
        planetDrift = (Math.sin(params.elapsedMs / 60000.0) * w * 0.02f).toFloat()
        val cx = w * 0.5f + planetDrift
        val cy = h * 0.62f
        val r = w * 0.26f

        // Body with radial shading — lit upper-right, dark lower-left.
        paint.shader = RadialGradient(
            cx + r * 0.35f, cy - r * 0.35f, r * 1.4f,
            intArrayOf(0xFF82A0C8.toInt(), 0xFF42587E.toInt(), 0xFF1A2238.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // Cloud bands clipped to planet
        canvas.save()
        path.reset()
        path.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(path)
        val bandOffset = (params.elapsedMs / 18000f) % (r * 2)
        val bandColors = intArrayOf(0xFF6680A8.toInt(), 0xFF3F557A.toInt(), 0xFF7090B8.toInt(), 0xFF4C6588.toInt())
        for (i in 0 until 7) {
            paint.color = withAlpha(bandColors[i % bandColors.size], 70)
            val bandY = cy - r + i * r * 0.30f + bandOffset * 0.1f
            canvas.drawRect(cx - r, bandY - 5f, cx + r, bandY + 5f, paint)
        }
        canvas.restore()

        // Soft terminator (night side)
        paint.shader = RadialGradient(
            cx - r * 0.5f, cy + r * 0.4f, r * 1.1f,
            intArrayOf(withAlpha(0xFF000010.toInt(), 160), withAlpha(0xFF000010.toInt(), 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawRings(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.5f + planetDrift
        paint.style = Paint.Style.STROKE
        for (i in 0 until 5) {
            paint.color = withAlpha(0xFFC0C0CC.toInt(), 80 - i * 12)
            paint.strokeWidth = 3.5f - i * 0.5f
            val rx = w * 0.40f + i * 9f
            val ry = w * 0.07f + i * 2.4f
            canvas.drawOval(cx - rx, h * 0.62f - ry, cx + rx, h * 0.62f + ry, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun drawAtmosphereHalo(canvas: Canvas) {
        val cx = w * 0.5f + planetDrift
        val cy = h * 0.62f
        val r = w * 0.26f
        paint.shader = RadialGradient(
            cx, cy, r * 1.20f,
            intArrayOf(withAlpha(0xFF6090C0.toInt(), 0), withAlpha(0xFF6090C0.toInt(), 90), withAlpha(0xFF6090C0.toInt(), 0)),
            floatArrayOf(0.84f, 0.94f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 1.20f, paint)
        paint.shader = null
    }

    private fun drawCraterRim(canvas: Canvas) {
        val base = 0xFF1A1C26.toInt()
        smoothRidgePath(path, w, h, h * 0.88f, h * 0.03f, seed = 50)
        paint.shader = LinearGradient(
            0f, h * 0.85f, 0f, h.toFloat(),
            lerpColor(base, 0xFF40455A.toInt(), 0.20f),
            0xFF06080C.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // crater pockmarks
        paint.color = 0xFF222230.toInt()
        val rng = PRNG(50)
        for (i in 0 until 5) {
            val cx = rng.next() * w
            val cy = h * 0.91f + rng.next() * h * 0.05f
            canvas.drawOval(cx - 14f, cy - 3.5f, cx + 14f, cy + 3.5f, paint)
        }
    }
}
