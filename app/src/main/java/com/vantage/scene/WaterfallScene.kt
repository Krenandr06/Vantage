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
        val sunCx = w * 0.50f

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = 0.50f)
        drawMoon(canvas, w, h, sky, cxFrac = 0.78f)

        drawFarMountains(canvas, sky, haze, sunCx)
        drawCliffFace(canvas, sky, haze, sunCx, params)
        drawTopForest(canvas, params, haze)
        drawWaterfall(canvas, params)
        drawPlungePool(canvas, sky, haze, params)
        drawMist(canvas, params)
        drawSideRocks(canvas, params)
        drawForegroundStream(canvas, sky, haze, params)
        drawForegroundFerns(canvas, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private fun drawFarMountains(canvas: Canvas, sky: SkyState, haze: Int, sunCx: Float) {
        val layers = listOf(
            RidgeLayer(seed = 1001, baseY = h * 0.22f, amplitude = h * 0.04f,
                color = lerpColor(0xFF5e6b88.toInt(), haze, 0.15f), depth = 0.90f, rim = 0.4f),
            RidgeLayer(seed = 1002, baseY = h * 0.28f, amplitude = h * 0.05f,
                color = lerpColor(0xFF4a5878.toInt(), haze, 0.05f), depth = 0.70f, rim = 0.5f, snow = 0.3f),
        )
        drawRidgeStack(canvas, w, h, layers, haze, sky, sunCx)
    }

    private fun drawCliffFace(canvas: Canvas, sky: SkyState, haze: Int, sunCx: Float, params: SceneParams) {
        // Single coherent cliff massif with a notch where the waterfall pours through.
        val baseRock = lerpColor(0xFF5e5a52.toInt(), haze, 0.18f)
        val rockShadow = lerpColor(0xFF2a2820.toInt(), 0xFF000000.toInt(), 0.10f)

        path.reset()
        // Left wall — climbs to a higher shoulder
        path.moveTo(0f, h * 0.20f)
        path.lineTo(0f, h * 0.55f)
        path.lineTo(w * 0.06f, h * 0.52f)
        path.lineTo(w * 0.14f, h * 0.40f)
        path.lineTo(w * 0.22f, h * 0.30f)
        path.lineTo(w * 0.30f, h * 0.18f)
        path.lineTo(w * 0.36f, h * 0.16f)
        // Notch where water spills (the lip)
        path.lineTo(w * 0.40f, h * 0.22f)
        path.lineTo(w * 0.42f, h * 0.24f)
        path.lineTo(w * 0.58f, h * 0.24f)
        path.lineTo(w * 0.60f, h * 0.22f)
        // Right wall
        path.lineTo(w * 0.64f, h * 0.16f)
        path.lineTo(w * 0.72f, h * 0.20f)
        path.lineTo(w * 0.80f, h * 0.32f)
        path.lineTo(w * 0.88f, h * 0.42f)
        path.lineTo(w * 0.94f, h * 0.50f)
        path.lineTo(w.toFloat(), h * 0.54f)
        path.lineTo(w.toFloat(), h * 0.20f)
        path.close()

        paint.shader = LinearGradient(
            0f, h * 0.16f, 0f, h * 0.65f,
            intArrayOf(
                lerpColor(baseRock, 0xFFFFFFFF.toInt(), 0.08f),
                baseRock,
                rockShadow,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Rock texture — vertical striations
        paint.color = withAlpha(rockShadow, 90)
        paint.strokeWidth = 1.4f
        paint.style = Paint.Style.STROKE
        val rng = PRNG(2200)
        for (i in 0 until 14) {
            val x = if (rng.next() < 0.5f) rng.next() * w * 0.36f else w * 0.64f + rng.next() * w * 0.36f
            val yTop = h * 0.20f + rng.next() * h * 0.08f
            val yBot = yTop + h * (0.12f + rng.next() * 0.18f)
            canvas.drawLine(x, yTop, x + (rng.next() - 0.5f) * 6f, yBot, paint)
        }
        paint.style = Paint.Style.FILL

        // Warm rim on the cliff edges
        val rim = rimLightColor(sky)
        paint.color = withAlpha(rim, (160f * (0.5f + sky.goldenHour * 0.5f)).toInt().coerceIn(60, 200))
        paint.strokeWidth = 2.2f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.22f, h * 0.30f, w * 0.30f, h * 0.18f, paint)
        canvas.drawLine(w * 0.30f, h * 0.18f, w * 0.36f, h * 0.16f, paint)
        canvas.drawLine(w * 0.64f, h * 0.16f, w * 0.72f, h * 0.20f, paint)
        canvas.drawLine(w * 0.72f, h * 0.20f, w * 0.80f, h * 0.32f, paint)
        // lip highlight
        canvas.drawLine(w * 0.42f, h * 0.24f, w * 0.58f, h * 0.24f, paint)
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawTopForest(canvas: Canvas, params: SceneParams, haze: Int) {
        val (bright, mid, dark) = when (params.season) {
            Season.SPRING -> Triple(0xFFb7ce82.toInt(), 0xFF6a8e48.toInt(), 0xFF2e4a2e.toInt())
            Season.SUMMER -> Triple(0xFF88a648.toInt(), 0xFF436a26.toInt(), 0xFF1f3a14.toInt())
            Season.AUTUMN -> Triple(0xFFe8ae6e.toInt(), 0xFFa66a3e.toInt(), 0xFF4a2810.toInt())
            Season.WINTER -> Triple(0xFFcfccc2.toInt(), 0xFF6a6e68.toInt(), 0xFF333338.toInt())
        }
        val midHazed = lerpColor(mid, haze, 0.25f)
        val rng = PRNG(100)
        // Trees only along the top edge of the cliff (not over the notch)
        for (i in 0 until 14) {
            val side = if (rng.next() < 0.5f) rng.next() * 0.38f else 0.62f + rng.next() * 0.38f
            val x = side * w
            val r = 14f + rng.next() * 22f
            // y traces the cliff top
            val y = if (side < 0.5f) h * (0.18f - rng.next() * 0.04f)
                    else h * (0.16f - rng.next() * 0.04f)
            drawPaintedCanopy(canvas, x, y, r, midHazed, bright, alpha = 235)
            paint.color = withAlpha(dark, 110)
            canvas.drawOval(x - r * 0.9f, y + r * 0.5f, x + r * 0.9f, y + r * 0.8f, paint)
            paint.alpha = 255
        }
    }

    private fun drawWaterfall(canvas: Canvas, params: SceneParams) {
        val cx = w * 0.50f
        val lipY = h * 0.24f
        val poolY = h * 0.62f
        val halfW = w * 0.075f
        drawWaterfallColumn(canvas, cx, halfW, lipY, poolY,
            elapsedMs = params.elapsedMs, intensity = params.intensity, seed = 3030)

        // A subtle warm glow on the falling water near the lip (catches sky light)
        paint.shader = LinearGradient(
            cx, lipY, cx, lipY + h * 0.06f,
            withAlpha(0xFFFFFFFF.toInt(), 90), withAlpha(0xFFFFFFFF.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(cx - halfW * 1.1f, lipY, cx + halfW * 1.1f, lipY + h * 0.08f, paint)
        paint.shader = null
    }

    private fun drawPlungePool(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val top = h * 0.62f
        val bot = h * 0.78f

        val base = lerpColor(sky.botColor, 0xFF1e3950.toInt(), 0.55f)
        paint.shader = LinearGradient(
            0f, top, 0f, bot,
            lerpColor(base, sky.midColor, 0.25f),
            lerpColor(base, 0xFF0a1622.toInt(), 0.35f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), bot, paint)
        paint.shader = null

        // expanding ripples from impact
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        val cx = w * 0.50f
        val impactY = top + 6f
        for (i in 0 until 6) {
            val phase = (params.elapsedMs / 1400.0 + i * 1.2).toFloat()
            val r = 12f + (Math.sin(phase.toDouble()) * 28f + 28f).toFloat()
            val fade = ((Math.sin(phase.toDouble()) * 0.5 + 0.5).toFloat())
            paint.color = withAlpha(0xFFFFFFFF.toInt(), ((1f - fade) * 110f).toInt().coerceIn(0, 180))
            canvas.drawCircle(cx, impactY, r, paint)
        }
        paint.style = Paint.Style.FILL

        // surface highlight band
        paint.shader = LinearGradient(
            0f, top, 0f, top + (bot - top) * 0.3f,
            withAlpha(0xFFE0EDF4.toInt(), 110), withAlpha(0xFFE0EDF4.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), top + (bot - top) * 0.3f, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawMist(canvas: Canvas, params: SceneParams) {
        // Big soft mist cloud at the base of the fall
        val cx = w * 0.50f
        val baseY = h * 0.60f
        val mist = 0xFFE8E8E8.toInt()
        val rng = PRNG(500)
        for (i in 0 until 9) {
            val drift = (Math.sin(params.elapsedMs / 4000.0 + i) * 12f).toFloat()
            val x = cx + (rng.next() - 0.5f) * w * 0.4f + drift
            val y = baseY + (rng.next() - 0.5f) * h * 0.05f
            val rx = 70f + rng.next() * 90f
            val ry = 18f + rng.next() * 22f
            drawSoftFogPuff(canvas, x, y, rx, ry, mist, alpha = (60 + rng.next() * 50f).toInt())
        }
        // spray particles
        val count = (12 + (params.intensity * 22).toInt()).coerceIn(12, 34)
        for (i in 0 until count) {
            val angle = rng.next() * 3.14f - 1.57f
            val life = ((params.elapsedMs / 1000f * 50f + rng.next() * 100f) % 60f) / 60f
            val x = cx + (Math.cos(angle.toDouble()) * life * w * 0.18f).toFloat() + (rng.next() - 0.5f) * 20f
            val y = baseY - 8f - (Math.sin(angle.toDouble()) * life * h * 0.04f).toFloat() + life * life * 18f
            paint.color = withAlpha(0xFFFFFFFF.toInt(), ((1f - life) * 150f).toInt().coerceIn(0, 200))
            canvas.drawCircle(x, y, 1.4f + rng.next() * 0.8f, paint)
        }
        paint.alpha = 255
    }

    private fun drawSideRocks(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(400)
        for (i in 0 until 8) {
            val side = if (i < 4) rng.next() * 0.32f else 0.68f + rng.next() * 0.32f
            val x = side * w
            val y = h * 0.64f + rng.next() * h * 0.10f
            val rx = 12f + rng.next() * 20f
            val ry = 7f + rng.next() * 10f
            paint.shader = LinearGradient(
                x, y - ry, x, y + ry,
                lerpColor(0xFF6e6c62.toInt(), 0xFFFFFFFF.toInt(), 0.12f),
                lerpColor(0xFF1a1812.toInt(), 0xFF000000.toInt(), 0.10f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paint)
            paint.shader = null
            // moss patch on top
            paint.color = withAlpha(0xFF5a7a30.toInt(), 200)
            canvas.drawOval(x - rx * 0.7f, y - ry, x + rx * 0.5f, y - ry * 0.5f, paint)
        }
        paint.alpha = 255
    }

    private fun drawForegroundStream(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val top = h * 0.78f
        // Banks
        paint.shader = LinearGradient(
            0f, top, 0f, h.toFloat(),
            0xFF3a4a30.toInt(), 0xFF161e10.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Outflow stream centered
        paint.shader = LinearGradient(
            0f, top, 0f, h.toFloat(),
            lerpColor(0xFF5a8aa0.toInt(), 0xFFFFFFFF.toInt(), 0.12f),
            lerpColor(0xFF1c3344.toInt(), 0xFF000000.toInt(), 0.20f),
            Shader.TileMode.CLAMP,
        )
        path.reset()
        path.moveTo(w * 0.36f, top)
        path.quadTo(w * 0.40f, h * 0.88f, w * 0.42f, h.toFloat())
        path.lineTo(w * 0.58f, h.toFloat())
        path.quadTo(w * 0.60f, h * 0.88f, w * 0.64f, top)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null

        // stream highlights
        paint.color = withAlpha(0xFFFFFFFF.toInt(), 90)
        paint.strokeWidth = 1.1f
        paint.style = Paint.Style.STROKE
        for (i in 0 until 6) {
            val sx = w * (0.42f + i * 0.028f)
            val y0 = top + 4f
            val y1 = h.toFloat()
            canvas.drawLine(sx, y0, sx + (i - 2.5f) * 3f, y1, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawForegroundFerns(canvas: Canvas, params: SceneParams) {
        paint.color = 0xFF3a6a2e.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.strokeCap = Paint.Cap.ROUND
        val rng = PRNG(600)
        for (i in 0 until 8) {
            val x = if (i < 4) rng.next() * w * 0.32f else w * 0.68f + rng.next() * w * 0.32f
            val y = h * 0.88f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i.toDouble()) * 5.0 * params.intensity).toFloat()
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x - 16f + sway, y - 28f, x - 28f + sway, y - 20f)
            canvas.drawPath(path, paint)
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x + 18f + sway, y - 24f, x + 30f + sway, y - 16f)
            canvas.drawPath(path, paint)
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x + sway, y - 34f, x + sway * 0.5f, y - 40f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT

        if (params.season == Season.AUTUMN && params.intensity > 0.05f) {
            val lrng = PRNG(700)
            for (i in 0 until 8) {
                val lx = lrng.next() * w
                val speed = 22f + lrng.next() * 30f
                val ly = ((params.elapsedMs / 1000f * speed + lrng.next() * h) % (h * 1.1f)) - h * 0.05f
                val dx = (Math.sin(params.elapsedMs / 1800.0 + lrng.next() * 6.28) * 16).toFloat()
                paint.color = withAlpha(0xFFd87838.toInt(), (180 + lrng.next() * 75f).toInt().coerceIn(0, 255))
                canvas.drawOval(lx + dx - 3.5f, ly - 1.8f, lx + dx + 3.5f, ly + 1.8f, paint)
            }
            paint.alpha = 255
        }
    }
}
