package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class MountainsScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val tmpPath = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) {
        w = width
        h = height
    }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        val haze = hazeColor(sky)

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = 0.62f)
        drawMoon(canvas, w, h, sky, cxFrac = 0.30f)

        drawHighClouds(canvas, sky, params)
        drawFarRidge(canvas, sky, haze)
        drawMidRidge(canvas, sky, haze, params)
        drawFuji(canvas, sky, haze, params)
        drawLowMist(canvas, haze, sky, params)
        drawNearForest(canvas, sky, haze, params)
        drawLake(canvas, sky, haze, params)
        drawForeground(canvas, sky, haze, params)
        drawPollen(canvas, sky, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private fun drawHighClouds(canvas: Canvas, sky: SkyState, params: SceneParams) {
        // Pastel cloud band, soft pink/peach lit by sun direction.
        val warm = lerpColor(0xFFFFE2D6.toInt(), sky.midColor, 0.25f)
        val cool = lerpColor(0xFFC8B4D8.toInt(), sky.topColor, 0.25f)
        val baseColor = lerpColor(warm, cool, 0.4f)
        val rim = lerpColor(0xFFFFF5E2.toInt(), sky.topColor, 0.15f)

        val rng = PRNG(101)
        val drift = (params.elapsedMs * 0.000005f) % 1f
        val cloudCount = 5
        for (i in 0 until cloudCount) {
            val baseX = (rng.next() + drift) % 1f
            val cx = baseX * w * 1.4f - w * 0.2f
            val cy = h * (0.10f + rng.next() * 0.18f)
            val scale = h * (0.030f + rng.next() * 0.030f)
            val alpha = (170 + rng.next() * 60f).toInt().coerceIn(0, 255)
            drawFluffyCloud(canvas, cx, cy, scale, baseColor, rim, alpha)
        }
    }

    private fun drawFarRidge(canvas: Canvas, sky: SkyState, haze: Int) {
        val base = lerpColor(0xFF6E7BA0.toInt(), haze, 0.45f)
        smoothRidgePath(path, w, h, h * 0.46f, h * 0.06f, seed = 211)
        drawAerialLayer(canvas, path, h * 0.36f, h * 0.55f, base, haze, depth = 0.85f)
    }

    private fun drawMidRidge(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val base = lerpColor(0xFF4F5E84.toInt(), haze, 0.30f)
        smoothRidgePath(path, w, h, h * 0.54f, h * 0.07f, seed = 423)
        drawAerialLayer(canvas, path, h * 0.42f, h * 0.62f, base, haze, depth = 0.60f)

        // soft snow dust on the higher peaks
        val snowAmt = when (params.season) {
            Season.WINTER -> 0.45f
            Season.AUTUMN, Season.SPRING -> 0.25f
            Season.SUMMER -> 0.10f
        }
        paint.color = withAlpha(0xFFEFEDE6.toInt(), (90 * snowAmt).toInt().coerceIn(0, 255))
        paint.shader = LinearGradient(
            0f, h * 0.43f, 0f, h * 0.52f,
            withAlpha(0xFFFFFFFF.toInt(), (140 * snowAmt).toInt().coerceIn(0, 255)),
            withAlpha(0xFFFFFFFF.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    private fun drawFuji(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        // Centerpiece conical peak with snow cap, sitting on a soft horizon haze.
        val cx = w * 0.42f
        val peakY = h * 0.30f
        val baseY = h * 0.62f
        val halfBase = w * 0.38f
        coneRidgePath(tmpPath, w, h, cx, peakY, baseY, halfBase)

        val base = lerpColor(0xFF6A4E66.toInt(), haze, 0.25f)
        drawAerialLayer(canvas, tmpPath, peakY, baseY, base, haze, depth = 0.45f)

        // subtle warm rim on the sun-facing side
        val rim = lerpColor(0xFFE6B69A.toInt(), sky.midColor, 0.45f)
        paint.shader = LinearGradient(
            cx - halfBase, peakY, cx + halfBase, baseY,
            intArrayOf(withAlpha(rim, 0), withAlpha(rim, 80), withAlpha(rim, 0)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(tmpPath, paint)
        paint.shader = null

        val capColor = lerpColor(0xFFF7F5EE.toInt(), sky.botColor, 0.10f)
        drawSnowCap(canvas, cx, peakY, baseY, halfBase, capColor, alpha = 235)
    }

    private fun drawLowMist(canvas: Canvas, haze: Int, sky: SkyState, params: SceneParams) {
        // a soft fog layer hanging at the foot of the mountains — sells aerial perspective.
        val mistCol = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.35f)
        drawHazeBand(canvas, w, h * 0.55f, h * 0.66f, mistCol, peakAlpha = 150)
    }

    private fun drawNearForest(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val base = when (params.season) {
            Season.SUMMER -> 0xFF3D5A48.toInt()
            Season.SPRING -> 0xFF49684A.toInt()
            Season.AUTUMN -> 0xFF8A5A38.toInt()
            Season.WINTER -> 0xFF4A5060.toInt()
        }
        smoothRidgePath(path, w, h, h * 0.68f, h * 0.05f, seed = 631)
        drawAerialLayer(canvas, path, h * 0.60f, h * 0.78f, base, haze, depth = 0.25f)

        // tiny conifer silhouettes on the crest
        val pineColor = lerpColor(base, 0xFF000000.toInt(), 0.35f)
        paint.color = pineColor
        val rng = PRNG(6310)
        for (i in 0 until 26) {
            val px = rng.next() * w
            val baseY = h * 0.66f + rng.next() * h * 0.04f
            val treeH = h * 0.025f + rng.next() * h * 0.035f
            val treeW = treeH * 0.30f
            tmpPath.reset()
            tmpPath.moveTo(px, baseY - treeH)
            tmpPath.quadTo(px - treeW * 0.6f, baseY - treeH * 0.3f, px - treeW, baseY)
            tmpPath.lineTo(px + treeW, baseY)
            tmpPath.quadTo(px + treeW * 0.6f, baseY - treeH * 0.3f, px, baseY - treeH)
            tmpPath.close()
            canvas.drawPath(tmpPath, paint)
        }
    }

    private fun drawLake(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val top = h * 0.78f
        val bot = h * 0.92f

        // base water — mirrors lower sky / haze
        val water = lerpColor(sky.botColor, 0xFF2A4060.toInt(), 0.50f)
        paint.shader = LinearGradient(
            0f, top, 0f, bot,
            lerpColor(water, sky.midColor, 0.25f), lerpColor(water, 0xFF101828.toInt(), 0.45f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), bot, paint)
        paint.shader = null

        // Soft inverted reflection of mountain glow
        val refl = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.20f)
        paint.shader = LinearGradient(
            0f, top, 0f, top + (bot - top) * 0.65f,
            intArrayOf(withAlpha(refl, 90), withAlpha(refl, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), bot, paint)
        paint.shader = null

        // ripples
        paint.color = withAlpha(0xFFFFFFFF.toInt(), 60)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        val rng = PRNG(555)
        val intensity = 0.4f + params.intensity * 0.6f
        for (i in 0 until 14) {
            val rx = rng.next() * w
            val ry = top + rng.next() * (bot - top)
            val phase = (params.elapsedMs / 2200.0 + rng.next() * 6.28).toFloat()
            val rl = (12f + (Math.sin(phase.toDouble()) * 8.0).toFloat()) * intensity
            val a = ((40 + Math.sin(phase.toDouble()) * 30.0) * intensity).toInt().coerceIn(0, 255)
            paint.alpha = a
            canvas.drawLine(rx - rl, ry, rx + rl, ry, paint)
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255

        // soft shoreline haze
        drawHazeBand(canvas, w, top - h * 0.02f, top + h * 0.015f, lerpColor(haze, 0xFFFFFFFF.toInt(), 0.4f), 130)
    }

    private fun drawForeground(canvas: Canvas, sky: SkyState, haze: Int, params: SceneParams) {
        val top = h * 0.92f
        val groundDark = lerpColor(0xFF1A2228.toInt(), sky.botColor, 0.10f)
        paint.shader = LinearGradient(
            0f, top, 0f, h.toFloat(),
            lerpColor(groundDark, 0xFF000000.toInt(), 0.15f), 0xFF06080C.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // grass strands
        paint.color = lerpColor(0xFF6A8A60.toInt(), groundDark, 0.5f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        val rng = PRNG(666)
        for (i in 0 until 36) {
            val x = rng.next() * w
            val y = top + rng.next() * (h - top) * 0.7f
            val sway = (Math.sin(params.elapsedMs / 2500.0 + i) * 3.0 * params.intensity).toFloat()
            canvas.drawLine(x, y + 6f, x + sway, y - 12f, paint)
            canvas.drawLine(x - 3f, y + 6f, x - 3f + sway * 0.7f, y - 8f, paint)
            canvas.drawLine(x + 3f, y + 6f, x + 3f + sway * 1.2f, y - 10f, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawPollen(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val warmth = lerpColor(0xFFFFF5DA.toInt(), sky.midColor, 0.20f)
        drawSparkles(
            canvas, w, h,
            count = (20 + params.intensity * 30f).toInt(),
            seed = 909,
            color = withAlpha(warmth, 180),
            elapsedMs = params.elapsedMs,
            intensity = 0.6f + params.intensity * 0.4f,
            yLimit = 0.78f,
        )
    }
}
