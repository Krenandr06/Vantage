package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * Eclipse transit.
 *
 * The moon slides across the sun on a long deterministic cycle. Key insight from
 * the user: in real eclipses the moon itself is not "dark" — you only see a dark
 * disk where it occludes the bright sun. We render the moon as a faint glow when
 * it's away from the sun, and only as a solid silhouette where the two overlap.
 */
class EclipseScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val clipPath = Path()
    private var w = 0
    private var h = 0

    // Full cycle: 8 minutes. Roughly: 3 min approach, 2 min covering, 3 min leaving.
    private val cycleMs = 8 * 60 * 1000L

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val phase = ((params.elapsedMs % cycleMs).toFloat() / cycleMs)
        val transit = transitPosition(phase)         // -1.6..+1.6 (moon dx in sun radii)
        val coverage = coverageFraction(transit)     // 0..1 (how much sun is covered)
        val isTotal = coverage > 0.985f

        val sky = interpolateSky(params.timeOfDay)

        drawDarkenedSky(canvas, sky, coverage)
        if (coverage > 0.6f) {
            drawStars(canvas, w, h, (coverage - 0.6f) / 0.4f * 0.9f, params.elapsedMs)
        }

        val sunCx = w * 0.50f
        val sunCy = h * 0.28f
        val sunR = w * 0.13f

        // distant clouds — fade as totality approaches
        if (coverage < 0.85f) drawDistantClouds(canvas, sky, coverage, params)

        // sun + moon transit
        drawSunWithMoon(canvas, sunCx, sunCy, sunR, transit, coverage, isTotal, params)

        // horizon glow — 360° during totality, normal warm horizon otherwise
        drawHorizonGlow(canvas, sky, coverage)

        // distant mountains
        drawMountains(canvas, sky, coverage, sunCx)

        // foreground
        drawNearHorizon(canvas, sky, coverage, params)
        drawObservers(canvas, coverage)
        drawForegroundAnchor(canvas, w, h,
            topY = h * 0.88f,
            baseColor = lerpColor(0xFF0a0c14.toInt(), sky.botColor, (1f - coverage) * 0.20f),
            seed = 7711,
        )

        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), (80 + coverage * 110f).toInt()), strength = 0.65f)
    }

    /**
     * Moon's horizontal position relative to sun, normalized in sun-radii units.
     * Spans roughly -1.8 (entering left) → 0 (center) → +1.8 (exiting right).
     * Uses a smoothed ease so motion feels deliberate.
     */
    private fun transitPosition(phase: Float): Float {
        val t = smoothstep(phase)
        return lerp(-1.8f, 1.8f, t)
    }

    /** How much of the sun's disk is covered (0..1) for a given moon offset in radii. */
    private fun coverageFraction(transit: Float): Float {
        val d = Math.abs(transit)
        return when {
            d >= 2f -> 0f
            d <= 0f -> 1f
            else -> {
                // Approximate area overlap of two equal circles separated by d radii.
                // Smooth perceptual curve from edge to total.
                val x = (1f - d / 2f).coerceIn(0f, 1f)
                smoothstep(x)
            }
        }
    }

    private fun drawDarkenedSky(canvas: Canvas, sky: SkyState, coverage: Float) {
        val k = (coverage - 0.4f).coerceAtLeast(0f) / 0.6f // darkening kicks in past 40% coverage
        val top = lerpColor(sky.topColor, 0xFF030616.toInt(), k * 0.95f)
        val upper = lerpColor(sky.upperColor, 0xFF080a1e.toInt(), k * 0.90f)
        val mid = lerpColor(sky.midColor, 0xFF161836.toInt(), k * 0.80f)
        val horizon = lerpColor(sky.horizonColor, 0xFF6a3a30.toInt(), k * 0.55f)
        val bot = lerpColor(sky.botColor, 0xFF3a1e28.toInt(), k * 0.60f)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(top, upper, mid, horizon, bot),
            floatArrayOf(0f, 0.35f, 0.62f, 0.84f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawDistantClouds(canvas: Canvas, sky: SkyState, coverage: Float, params: SceneParams) {
        val fade = (1f - coverage).coerceIn(0f, 1f)
        val base = lerpColor(0xFFE0BC9C.toInt(), sky.midColor, 0.45f)
        val rim = lerpColor(0xFFFFE0C0.toInt(), sky.horizonColor, 0.25f)
        val rng = PRNG(81)
        val drift = (params.elapsedMs * 0.000004f) % 1f
        for (i in 0 until 4) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.42f + rng.next() * 0.10f)
            val rx = w * (0.10f + rng.next() * 0.08f)
            val ry = h * (0.018f + rng.next() * 0.020f)
            drawVolumetricCloud(canvas, cx, cy, rx, ry, base, rim, w * 0.5f,
                alpha = (210 * fade).toInt().coerceIn(0, 230))
        }
    }

    private fun drawSunWithMoon(
        canvas: Canvas,
        sunCx: Float, sunCy: Float, sunR: Float,
        transit: Float, coverage: Float, isTotal: Boolean,
        params: SceneParams,
    ) {
        val moonCx = sunCx + transit * sunR
        val moonR = sunR * 1.02f

        if (isTotal) {
            drawCorona(canvas, sunCx, sunCy, sunR, params)
            // dark moon disk
            paint.shader = RadialGradient(
                sunCx - sunR * 0.25f, sunCy - sunR * 0.25f, sunR * 1.2f,
                intArrayOf(0xFF1a1a22.toInt(), 0xFF000000.toInt()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(moonCx, sunCy, moonR, paint)
            paint.shader = null
            return
        }

        // 1. Draw full sun
        drawSunBody(canvas, sunCx, sunCy, sunR)

        // 2. Draw moon — but ONLY the part overlapping the sun is dark.
        //    Outside the sun, render moon as a very faint silver hint.
        val d = Math.abs(transit)
        if (d < 1.95f) {
            // Faint moon ghost outside the sun (only visible against bright sky)
            if (d > 0.05f) {
                paint.shader = RadialGradient(
                    moonCx, sunCy, moonR,
                    intArrayOf(withAlpha(0xFF98a0b0.toInt(), 70), withAlpha(0xFF98a0b0.toInt(), 0)),
                    floatArrayOf(0.6f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(moonCx, sunCy, moonR, paint)
                paint.shader = null
            }

            // Dark silhouette clipped to the sun disk
            canvas.save()
            clipPath.reset()
            clipPath.addCircle(sunCx, sunCy, sunR * 1.01f, Path.Direction.CW)
            canvas.clipPath(clipPath)
            paint.shader = RadialGradient(
                moonCx - moonR * 0.25f, sunCy - moonR * 0.25f, moonR * 1.2f,
                intArrayOf(0xFF18181f.toInt(), 0xFF000000.toInt()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(moonCx, sunCy, moonR, paint)
            paint.shader = null
            canvas.restore()

            // Diamond-ring sparkle at the leading/trailing edge during deep partial
            if (coverage in 0.92f..0.98f) {
                val side = if (transit < 0) 1f else -1f
                val edgeX = moonCx + side * moonR * 0.95f
                drawSoftGlow(canvas, edgeX, sunCy, sunR * 0.18f,
                    withAlpha(0xFFFFFFFF.toInt(), 235), intensity = 1.4f)
            }
        }
    }

    private fun drawSunBody(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        // Big outer halo
        paint.shader = RadialGradient(
            cx, cy, r * 3.5f,
            withAlpha(0xFFFFE8B0.toInt(), 100), withAlpha(0xFFFFE8B0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 3.5f, paint)
        // Inner halo
        paint.shader = RadialGradient(
            cx, cy, r * 1.7f,
            withAlpha(0xFFFFEAC0.toInt(), 200), withAlpha(0xFFFFEAC0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 1.7f, paint)
        // Disk
        paint.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(0xFFFFFAE8.toInt(), 0xFFFFD074.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
    }

    private fun drawCorona(canvas: Canvas, cx: Float, cy: Float, r: Float, params: SceneParams) {
        // multi-layer outer glow
        paint.shader = RadialGradient(
            cx, cy, r * 4.2f,
            withAlpha(0xFFFFEBD0.toInt(), 80), withAlpha(0xFFFFEBD0.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 4.2f, paint)
        paint.shader = RadialGradient(
            cx, cy, r * 2.4f,
            withAlpha(0xFFFFE0E8.toInt(), 150), withAlpha(0xFFFFE0E8.toInt(), 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r * 2.4f, paint)
        paint.shader = RadialGradient(
            cx, cy, r * 1.45f,
            withAlpha(0xFFFFFFFF.toInt(), 220), withAlpha(0xFFFFFFFF.toInt(), 0),
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
        for (i in 0 until 36) {
            val angle = Math.toRadians((i * 10f + rotation).toDouble())
            val len = r * (1.4f + rng.next() * 2.4f)
            val innerR = r * 1.06f
            val x1 = cx + (Math.cos(angle) * innerR).toFloat()
            val y1 = cy + (Math.sin(angle) * innerR).toFloat()
            val x2 = cx + (Math.cos(angle) * len).toFloat()
            val y2 = cy + (Math.sin(angle) * len).toFloat()
            paint.alpha = (70 + rng.next() * 90f).toInt().coerceIn(0, 255)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.alpha = 255
    }

    private fun drawHorizonGlow(canvas: Canvas, sky: SkyState, coverage: Float) {
        if (coverage > 0.85f) {
            // 360° sunset glow during totality
            val glowColor = 0xFFd47858.toInt()
            paint.shader = LinearGradient(
                0f, h * 0.62f, 0f, h * 0.88f,
                intArrayOf(
                    0x00000000,
                    withAlpha(glowColor, 110),
                    withAlpha(glowColor, 60),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.45f, 0.78f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, h * 0.62f, w.toFloat(), h * 0.88f, paint)
            paint.shader = null
        }
    }

    private fun drawMountains(canvas: Canvas, sky: SkyState, coverage: Float, sunCx: Float) {
        val darkness = (coverage - 0.3f).coerceAtLeast(0f) / 0.7f
        val far = lerpColor(0xFF3a3c50.toInt(), 0xFF1a1828.toInt(), darkness)
        val mid = lerpColor(0xFF26242e.toInt(), 0xFF120f1d.toInt(), darkness)
        val haze = hazeColor(sky)
        val layers = listOf(
            RidgeLayer(seed = 111, baseY = h * 0.70f, amplitude = h * 0.05f,
                color = far, depth = 0.65f, rim = if (coverage < 0.5f) 0.4f else 0f),
            RidgeLayer(seed = 222, baseY = h * 0.76f, amplitude = h * 0.045f,
                color = mid, depth = 0.35f),
        )
        drawRidgeStack(canvas, w, h, layers, haze, sky, sunCx)
    }

    private fun drawNearHorizon(canvas: Canvas, sky: SkyState, coverage: Float, params: SceneParams) {
        val darkness = (coverage - 0.3f).coerceAtLeast(0f) / 0.7f
        val base = lerpColor(0xFF161320.toInt(), 0xFF06040c.toInt(), darkness)
        paint.shader = LinearGradient(
            0f, h * 0.80f, 0f, h.toFloat(),
            lerpColor(base, 0xFFFFFFFF.toInt(), 0.05f), 0xFF020306.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.80f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val pineColor = 0xFF040208.toInt()
        val rng = PRNG(333)
        for (i in 0 until 22) {
            val tx = rng.next() * w
            val baseY = h * 0.80f + rng.next() * h * 0.01f
            val treeH = h * 0.04f + rng.next() * h * 0.06f
            drawPaintedConifer(canvas, tx, baseY, treeH, pineColor)
        }
    }

    private fun drawObservers(canvas: Canvas, coverage: Float) {
        paint.color = 0xFF050309.toInt()
        val groundY = h * 0.84f

        // Adult
        val ax = w * 0.40f
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
}
