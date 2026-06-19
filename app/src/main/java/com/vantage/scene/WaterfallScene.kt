package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class WaterfallScene : VantageScene {

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

        drawHighClouds(canvas, sky, params)
        drawFarCliffs(canvas, haze, sky)
        drawTopTrees(canvas, params, haze)
        drawCliffBack(canvas, haze, sky)
        drawUpperCliff(canvas, haze)
        drawMoss(canvas, params)
        drawCascades(canvas, params)
        drawMidPool(canvas, sky, haze, params)
        drawLowerRocks(canvas)
        drawLowerPool(canvas, sky, haze, params)
        drawSpray(canvas, params)
        drawForeground(canvas, params, haze)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private fun drawHighClouds(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val warm = lerpColor(0xFFFFE0CC.toInt(), sky.midColor, 0.30f)
        val rim = lerpColor(0xFFFFF6E0.toInt(), sky.topColor, 0.20f)
        val rng = PRNG(91)
        val drift = (params.elapsedMs * 0.000005f) % 1f
        for (i in 0 until 4) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.06f + rng.next() * 0.10f)
            val scale = h * (0.025f + rng.next() * 0.025f)
            drawFluffyCloud(canvas, cx, cy, scale, warm, rim, alpha = 180)
        }
    }

    private fun drawFarCliffs(canvas: Canvas, haze: Int, sky: SkyState) {
        smoothRidgePath(path, w, h, h * 0.30f, h * 0.04f, seed = 1001)
        drawAerialLayer(canvas, path, h * 0.22f, h * 0.40f, 0xFF6E7B96.toInt(), haze, depth = 0.80f)
    }

    private fun drawTopTrees(canvas: Canvas, params: SceneParams, haze: Int) {
        val bright: Int; val mid: Int; val dark: Int
        when (params.season) {
            Season.SPRING -> { bright = 0xFFB7CE82.toInt(); mid = 0xFF6A8E48.toInt(); dark = 0xFF2E4A2E.toInt() }
            Season.SUMMER -> { bright = 0xFF88A648.toInt(); mid = 0xFF436A26.toInt(); dark = 0xFF1F3A14.toInt() }
            Season.AUTUMN -> { bright = 0xFFE8AE6E.toInt(); mid = 0xFFA66A3E.toInt(); dark = 0xFF4A2810.toInt() }
            Season.WINTER -> { bright = 0xFFCFCCC2.toInt(); mid = 0xFF6A6E68.toInt(); dark = 0xFF333338.toInt() }
        }
        val midHazed = lerpColor(mid, haze, 0.25f)
        val rng = PRNG(100)
        for (i in 0 until 12) {
            val x = rng.next() * w
            val r = 18f + rng.next() * 24f
            val y = h * 0.08f + rng.next() * h * 0.06f
            drawPaintedCanopy(canvas, x, y, r, midHazed, bright, alpha = 235)
            // a wisp of dark base shadow under canopy
            paint.color = withAlpha(dark, 110)
            canvas.drawOval(x - r * 0.9f, y + r * 0.55f, x + r * 0.9f, y + r * 0.85f, paint)
            paint.alpha = 255
        }
    }

    private fun drawCliffBack(canvas: Canvas, haze: Int, sky: SkyState) {
        val base = lerpColor(0xFF5C5A50.toInt(), haze, 0.20f)
        val dark = lerpColor(0xFF36352C.toInt(), 0xFF000000.toInt(), 0.05f)
        path.reset()
        path.moveTo(0f, h * 0.12f)
        path.lineTo(w * 0.32f, h * 0.10f)
        path.quadTo(w * 0.36f, h * 0.30f, w * 0.40f, h * 0.50f)
        path.lineTo(w * 0.60f, h * 0.50f)
        path.quadTo(w * 0.64f, h * 0.30f, w * 0.68f, h * 0.10f)
        path.lineTo(w.toFloat(), h * 0.12f)
        path.lineTo(w.toFloat(), h * 0.55f)
        path.lineTo(0f, h * 0.55f)
        path.close()
        paint.shader = LinearGradient(
            0f, h * 0.10f, 0f, h * 0.55f,
            base, dark, Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    private fun drawUpperCliff(canvas: Canvas, haze: Int) {
        paint.color = lerpColor(0xFF6C6A60.toInt(), haze, 0.10f)
        canvas.drawRect(0f, h * 0.14f, w * 0.40f, h * 0.20f, paint)
        canvas.drawRect(w * 0.60f, h * 0.14f, w.toFloat(), h * 0.20f, paint)
        paint.color = lerpColor(0xFF5A5848.toInt(), 0xFF000000.toInt(), 0.08f)
        canvas.drawRect(w * 0.30f, h * 0.38f, w * 0.70f, h * 0.42f, paint)

        // soft cliff edge highlight
        paint.shader = LinearGradient(
            0f, h * 0.14f, 0f, h * 0.16f,
            withAlpha(0xFFFFE6C0.toInt(), 90), withAlpha(0xFFFFE6C0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.14f, w * 0.40f, h * 0.18f, paint)
        canvas.drawRect(w * 0.60f, h * 0.14f, w.toFloat(), h * 0.18f, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawMoss(canvas: Canvas, params: SceneParams) {
        val deep = 0xFF3A5A1E.toInt()
        val mid = 0xFF5A7E30.toInt()
        val bright = 0xFF8AA84A.toInt()
        val rng = PRNG(200)
        for (side in 0..1) {
            val xBase = if (side == 0) w * 0.30f else w * 0.62f
            for (i in 0 until 10) {
                val x = xBase + rng.next() * w * 0.08f
                val y = h * 0.15f + rng.next() * h * 0.35f
                drawSoftFogPuff(canvas, x, y, 14f, 5f, deep, alpha = 200)
                paint.color = mid
                canvas.drawOval(x - 7f, y - 3f, x + 7f, y + 3f, paint)
                paint.color = bright
                canvas.drawOval(x - 3.5f, y - 2f, x + 3.5f, y + 1f, paint)
                paint.color = withAlpha(mid, 200)
                paint.strokeWidth = 1.5f
                paint.style = Paint.Style.STROKE
                val tendrilLen = 8f + rng.next() * 14f
                canvas.drawLine(x, y + 3f, x + (rng.next() - 0.5f) * 4f, y + 3f + tendrilLen, paint)
                paint.style = Paint.Style.FILL
                paint.alpha = 255
            }
        }
    }

    private fun drawCascades(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(300)
        val cx = w * 0.5f

        // sheet of water — soft white gradient underneath the streaks
        paint.shader = LinearGradient(
            cx, h * 0.18f, cx, h * 0.52f,
            withAlpha(0xFFE8F0F8.toInt(), 160), withAlpha(0xFFE8F0F8.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(w * 0.36f, h * 0.18f, w * 0.64f, h * 0.52f, paint)
        paint.shader = null

        // main cascade streaks
        paint.color = 0xFFE8F0F8.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 16) {
            val streakX = cx + (rng.next() - 0.5f) * w * 0.14f
            val streakY = h * 0.18f + rng.next() * h * 0.02f
            val speed = 220f + rng.next() * 160f
            val yOff = ((params.elapsedMs / 1000f * speed + rng.next() * h * 0.3f) % (h * 0.34f))
            val streakLen = 18f + rng.next() * 22f
            paint.alpha = (160 + rng.next() * 95f).toInt().coerceIn(0, 255)
            paint.strokeWidth = 1.5f + rng.next() * 1.8f
            canvas.drawLine(streakX, streakY + yOff, streakX + (rng.next() - 0.5f) * 3, streakY + yOff + streakLen, paint)
        }

        // side cascades
        for (side in listOf(w * 0.36f, w * 0.64f)) {
            for (i in 0 until 6) {
                val sx = side + (rng.next() - 0.5f) * w * 0.04f
                val speed = 160f + rng.next() * 100f
                val yOff = ((params.elapsedMs / 1000f * speed + rng.next() * h * 0.2f) % (h * 0.2f))
                paint.alpha = (110 + rng.next() * 80f).toInt().coerceIn(0, 255)
                paint.strokeWidth = 1f + rng.next()
                canvas.drawLine(sx, h * 0.2f + yOff, sx, h * 0.2f + yOff + 12f + rng.next() * 12f, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.alpha = 255
    }

    private fun drawMidPool(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val base = lerpColor(sky.botColor, 0xFF345E7E.toInt(), 0.45f)
        paint.shader = LinearGradient(
            0f, h * 0.5f, 0f, h * 0.58f,
            lerpColor(base, sky.midColor, 0.25f), base,
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(w * 0.25f, h * 0.5f, w * 0.75f, h * 0.58f, paint)
        paint.shader = null

        // ripples expanding from impact
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in 0 until 5) {
            val phase = (params.elapsedMs / 1500.0 + i * 1.3).toFloat()
            val r = 5f + (Math.sin(phase.toDouble()) * 18f + 18f).toFloat()
            paint.color = withAlpha(0xFFFFFFFF.toInt(), (60 - r * 1.2f).toInt().coerceIn(0, 255))
            canvas.drawCircle(w * 0.5f, h * 0.54f, r, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun drawLowerRocks(canvas: Canvas) {
        val rng = PRNG(400)
        for (i in 0 until 7) {
            val x = w * 0.2f + rng.next() * w * 0.6f
            val y = h * 0.58f + rng.next() * h * 0.06f
            val rx = 12f + rng.next() * 16f
            val ry = 7f + rng.next() * 8f
            paint.shader = LinearGradient(
                x, y - ry, x, y + ry,
                lerpColor(0xFF7E7C72.toInt(), 0xFFFFFFFF.toInt(), 0.10f),
                lerpColor(0xFF302E26.toInt(), 0xFF000000.toInt(), 0.10f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint)
            paint.shader = null
        }
    }

    private fun drawLowerPool(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        drawPaintedWater(
            canvas, w,
            top = h * 0.65f, bot = h * 0.82f,
            sky = sky, haze = haze,
            intensity = params.intensity, elapsedMs = params.elapsedMs,
            glintCxFrac = 0.5f, seed = 421,
        )
    }

    private fun drawSpray(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(500)
        val count = (16 + (params.intensity * 22).toInt()).coerceIn(16, 38)
        for (i in 0 until count) {
            val baseX = w * 0.5f + (rng.next() - 0.5f) * w * 0.22f
            val baseY = h * 0.52f
            val speed = 32f + rng.next() * 50f
            val angle = rng.next() * 3.14f - 1.57f
            val life = ((params.elapsedMs / 1000f * speed + rng.next() * 100f) % 50f) / 50f
            val x = baseX + (Math.cos(angle.toDouble()) * life * 30f).toFloat()
            val y = baseY - (Math.sin(angle.toDouble()) * life * 22f).toFloat() + life * life * 28f
            paint.color = withAlpha(0xFFFFFFFF.toInt(), ((1f - life) * 130f).toInt().coerceIn(0, 255))
            canvas.drawCircle(x, y, 1.6f + rng.next(), paint)
        }

        // big soft mist above pool
        val mist = 0xFFE8E4E0.toInt()
        for (i in 0 until 5) {
            val x = w * 0.30f + rng.next() * w * 0.40f + (Math.sin(params.elapsedMs / 4000.0 + i) * 10).toFloat()
            val y = h * 0.48f + rng.next() * h * 0.06f
            drawSoftFogPuff(canvas, x, y, 75f, 15f, mist, alpha = 60)
        }
        paint.alpha = 255
    }

    private fun drawForeground(canvas: Canvas, params: SceneParams, haze: Int) {
        // Outflowing stream
        paint.shader = LinearGradient(
            0f, h * 0.82f, 0f, h.toFloat(),
            lerpColor(0xFF5A8AA0.toInt(), 0xFFFFFFFF.toInt(), 0.10f),
            lerpColor(0xFF2A4458.toInt(), 0xFF000000.toInt(), 0.20f),
            Shader.TileMode.CLAMP,
        )
        path.reset()
        path.moveTo(w * 0.35f, h * 0.82f)
        path.quadTo(w * 0.4f, h * 0.88f, w * 0.45f, h.toFloat())
        path.lineTo(w * 0.6f, h.toFloat())
        path.quadTo(w * 0.58f, h * 0.88f, w * 0.65f, h * 0.82f)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null

        // Banks
        paint.shader = LinearGradient(
            0f, h * 0.82f, 0f, h.toFloat(),
            0xFF3F5436.toInt(), 0xFF1F2A1A.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.82f, w * 0.35f, h.toFloat(), paint)
        canvas.drawRect(w * 0.65f, h * 0.82f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Stones
        val rng = PRNG(600)
        for (i in 0 until 5) {
            val x = w * 0.4f + rng.next() * w * 0.2f
            val y = h * 0.87f + rng.next() * h * 0.07f
            paint.shader = LinearGradient(x, y - 4f, x, y + 4f, 0xFF7E7C72.toInt(), 0xFF302E26.toInt(), Shader.TileMode.CLAMP)
            canvas.drawOval(x - 7f, y - 4f, x + 7f, y + 4f, paint)
            paint.shader = null
        }

        // Front ferns
        paint.color = 0xFF4A8A3A.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 7) {
            val x = if (i < 4) rng.next() * w * 0.32f else w * 0.68f + rng.next() * w * 0.32f
            val y = h * 0.88f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i.toDouble()) * 5.0 * params.intensity).toFloat()
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x - 15f + sway, y - 25f, x - 25f + sway, y - 18f)
            canvas.drawPath(path, paint)
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x + 16f + sway, y - 22f, x + 26f + sway, y - 14f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT

        // Autumn leaf drift
        if (params.season == Season.AUTUMN && params.intensity > 0.05f) {
            val lrng = PRNG(700)
            for (i in 0 until 10) {
                val lx = lrng.next() * w
                val speed = 22f + lrng.next() * 30f
                val ly = ((params.elapsedMs / 1000f * speed + lrng.next() * h) % (h * 1.1f)) - h * 0.05f
                val dx = (Math.sin(params.elapsedMs / 1800.0 + lrng.next() * 6.28) * 16).toFloat()
                paint.color = withAlpha(0xFFD87838.toInt(), (180 + lrng.next() * 75f).toInt().coerceIn(0, 255))
                canvas.drawOval(lx + dx - 3.5f, ly - 1.8f, lx + dx + 3.5f, ly + 1.8f, paint)
            }
            paint.alpha = 255
        }
    }
}
