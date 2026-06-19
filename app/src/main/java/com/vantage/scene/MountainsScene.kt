package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class MountainsScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) {
        w = width
        h = height
    }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        val haze = hazeColor(sky)
        val sunCx = w * sunCxForTime(params.timeOfDay)

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)

        // High wisps + a single volumetric cloud anchored to the sun side at golden hour
        drawHighSky(canvas, sky, sunCx, params)

        drawSun(canvas, w, h, sky, cxFrac = sunCx / w)
        drawMoon(canvas, w, h, sky, cxFrac = 0.18f)

        drawRange(canvas, sky, haze, sunCx, params)
        drawSeaOfClouds(
            canvas, w, h,
            yTop = h * 0.50f, yBot = h * 0.72f,
            baseColor = lerpColor(haze, sky.horizonColor, 0.35f),
            rimColor = lerpColor(sky.horizonColor, 0xFFFFFFFF.toInt(), 0.40f),
            elapsedMs = params.elapsedMs,
            seed = 4011,
            density = 1.1f,
        )
        drawValleyRidge(canvas, sky, haze, sunCx, params)
        drawForegroundAnchor(canvas, w, h,
            topY = h * 0.86f,
            baseColor = lerpColor(0xFF221d28.toInt(), sky.botColor, 0.10f),
            seed = 6611,
        )
        drawPollen(canvas, sky, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private fun sunCxForTime(t: Float): Float {
        // east-to-west arc: at 6am cxFrac=0.10, at noon=0.50, at 6pm=0.90.
        val tod = t.coerceIn(5f, 19f)
        return ((tod - 5f) / 14f).coerceIn(0.10f, 0.90f)
    }

    private fun drawHighSky(canvas: Canvas, sky: SkyState, sunCx: Float, params: SceneParams) {
        // 3 cirrus wisps
        val rng = PRNG(202)
        val drift = (params.elapsedMs * 0.000003f) % 1f
        val wispCol = lerpColor(0xFFFFE6CC.toInt(), sky.horizonColor, 0.35f)
        for (i in 0 until 4) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.06f + rng.next() * 0.12f)
            val halfW = w * (0.18f + rng.next() * 0.14f)
            val halfH = h * (0.010f + rng.next() * 0.012f)
            val alpha = (110 + sky.goldenHour * 80f).toInt().coerceIn(60, 200)
            drawWispCloud(canvas, cx, cy, halfW, halfH, wispCol, alpha)
        }

        // One hero volumetric cloud near sun during golden hour
        if (sky.goldenHour > 0.35f) {
            val cx = sunCx + (if (sunCx > w * 0.5f) -w * 0.22f else w * 0.22f)
            val cy = h * 0.18f
            val rx = w * 0.18f
            val ry = h * 0.06f
            val base = lerpColor(sky.midColor, 0xFFC68C6E.toInt(), 0.45f)
            val rim = lerpColor(0xFFFFD8B0.toInt(), sky.horizonColor, 0.25f)
            drawVolumetricCloud(canvas, cx, cy, rx, ry, base, rim, sunCx, alpha = 230)
        }
    }

    private fun drawRange(canvas: Canvas, sky: SkyState, haze: Int, sunCx: Float, params: SceneParams) {
        val snowAmt = when (params.season) {
            Season.WINTER -> 0.85f
            Season.AUTUMN, Season.SPRING -> 0.55f
            Season.SUMMER -> 0.30f
        }
        // 5 ridges back-to-front + a hero peak.
        val cool = 0xFF4a5e7e.toInt()
        val layers = listOf(
            RidgeLayer(seed = 1101, baseY = h * 0.42f, amplitude = h * 0.05f,
                color = lerpColor(0xFF7088a8.toInt(), haze, 0.10f), depth = 0.90f, rim = 0.5f, snow = snowAmt * 0.4f),
            RidgeLayer(seed = 2202, baseY = h * 0.46f, amplitude = h * 0.07f,
                color = lerpColor(0xFF607896.toInt(), haze, 0.05f), depth = 0.78f, rim = 0.8f, snow = snowAmt * 0.6f),
            RidgeLayer(seed = 3303, baseY = h * 0.52f, amplitude = h * 0.085f,
                color = lerpColor(cool, haze, 0.0f), depth = 0.60f, rim = 1.0f, snow = snowAmt),
            RidgeLayer(seed = 4404, baseY = h * 0.58f, amplitude = h * 0.07f,
                color = lerpColor(0xFF36465c.toInt(), haze, -0.05f), depth = 0.42f, rim = 0.7f, snow = snowAmt * 0.7f),
        )
        drawRidgeStack(canvas, w, h, layers, haze, sky, sunCx)

        // Hero peak — a dramatic asymmetric Matterhorn-style summit on the back layer.
        drawHeroPeak(canvas, sky, haze, sunCx, snowAmt)
    }

    private fun drawHeroPeak(canvas: Canvas, sky: SkyState, haze: Int, sunCx: Float, snowAmt: Float) {
        // Matterhorn-style: pointed asymmetric summit. Left face steep & smooth,
        // right face has a jagged shoulder dropping in steps. Sun on the right.
        val peakCx = w * 0.30f
        val peakY = h * 0.22f
        val baseY = h * 0.54f
        val leftFoot = peakCx - w * 0.32f
        val rightFoot = peakCx + w * 0.36f

        path.reset()
        path.moveTo(leftFoot, baseY)
        // Smoothly climbing left face — slightly concave near the top
        path.lineTo(peakCx - w * 0.22f, baseY - h * 0.06f)
        path.cubicTo(
            peakCx - w * 0.18f, baseY - h * 0.13f,
            peakCx - w * 0.10f, baseY - h * 0.24f,
            peakCx - w * 0.025f, peakY + h * 0.03f,
        )
        // Apex
        path.lineTo(peakCx, peakY)
        // Right shoulder — jagged step-down
        path.lineTo(peakCx + w * 0.025f, peakY + h * 0.035f)
        path.lineTo(peakCx + w * 0.045f, peakY + h * 0.075f)
        path.lineTo(peakCx + w * 0.080f, peakY + h * 0.110f)
        path.lineTo(peakCx + w * 0.110f, peakY + h * 0.180f)
        path.lineTo(peakCx + w * 0.155f, peakY + h * 0.225f)
        path.lineTo(peakCx + w * 0.220f, peakY + h * 0.280f)
        path.lineTo(peakCx + w * 0.300f, baseY - h * 0.02f)
        path.lineTo(rightFoot, baseY)
        path.lineTo(rightFoot, h.toFloat())
        path.lineTo(leftFoot, h.toFloat())
        path.close()

        val base = lerpColor(0xFF353244.toInt(), haze, 0.05f)
        drawAerialLayer(canvas, path, peakY, baseY, base, haze, depth = 0.30f)

        // Shadow side (away from sun)
        val shadow = Path()
        shadow.moveTo(peakCx, peakY)
        if (sunCx > peakCx) {
            // shadow on left face
            shadow.cubicTo(
                peakCx - w * 0.10f, baseY - h * 0.24f,
                peakCx - w * 0.18f, baseY - h * 0.13f,
                peakCx - w * 0.22f, baseY - h * 0.06f,
            )
            shadow.lineTo(leftFoot, baseY)
            shadow.lineTo(peakCx - w * 0.05f, baseY)
            shadow.lineTo(peakCx, peakY + h * 0.20f)
        } else {
            // shadow on right shoulder
            shadow.lineTo(peakCx + w * 0.045f, peakY + h * 0.075f)
            shadow.lineTo(peakCx + w * 0.110f, peakY + h * 0.180f)
            shadow.lineTo(peakCx + w * 0.220f, peakY + h * 0.280f)
            shadow.lineTo(peakCx + w * 0.300f, baseY - h * 0.02f)
            shadow.lineTo(peakCx, baseY - h * 0.02f)
        }
        shadow.close()
        paint.color = withAlpha(0xFF1a1822.toInt(), 100)
        canvas.drawPath(shadow, paint)

        // Warm rim along the lit side of the silhouette
        val rim = rimLightColor(sky)
        paint.strokeWidth = 2.6f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val rimAlpha = (190f * (0.5f + sky.goldenHour * 0.5f)).toInt().coerceIn(80, 220)
        paint.color = withAlpha(rim, rimAlpha)
        if (sunCx > peakCx) {
            // rim on right shoulder
            canvas.drawLine(peakCx, peakY, peakCx + w * 0.025f, peakY + h * 0.035f, paint)
            canvas.drawLine(peakCx + w * 0.025f, peakY + h * 0.035f, peakCx + w * 0.045f, peakY + h * 0.075f, paint)
            canvas.drawLine(peakCx + w * 0.045f, peakY + h * 0.075f, peakCx + w * 0.080f, peakY + h * 0.110f, paint)
            canvas.drawLine(peakCx + w * 0.080f, peakY + h * 0.110f, peakCx + w * 0.110f, peakY + h * 0.180f, paint)
            canvas.drawLine(peakCx + w * 0.110f, peakY + h * 0.180f, peakCx + w * 0.155f, peakY + h * 0.225f, paint)
        } else {
            // rim on left face — single curve
            path.reset()
            path.moveTo(peakCx, peakY)
            path.cubicTo(
                peakCx - w * 0.025f, peakY + h * 0.05f,
                peakCx - w * 0.10f, baseY - h * 0.24f,
                peakCx - w * 0.22f, baseY - h * 0.06f,
            )
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT

        // Snow cap — small irregular patches near the summit, NOT a triangle reaching the apex.
        // Place a few small white blobs on the upper third of each face, biased to the lit side.
        drawSnowPatches(canvas, sky, peakCx, peakY, baseY, sunCx, snowAmt)
    }

    private fun drawSnowPatches(
        canvas: Canvas,
        sky: SkyState,
        peakCx: Float,
        peakY: Float,
        baseY: Float,
        sunCx: Float,
        snowAmt: Float,
    ) {
        val capCol = lerpColor(0xFFF8F2E6.toInt(), sky.horizonColor, 0.15f)
        val capColShadow = lerpColor(capCol, 0xFF6a7090.toInt(), 0.35f)
        val sunSide = if (sunCx > peakCx) 1f else -1f
        val rng = PRNG(8801)
        val blobCount = (5 + snowAmt * 6f).toInt()
        for (i in 0 until blobCount) {
            val t = rng.next() // 0..1 along the ridge from the peak down
            // bias more snow toward the top (small t)
            val tt = t * t
            // mostly on the lit side
            val side = if (rng.next() < 0.78f) sunSide else -sunSide
            val depth = tt * 0.30f
            val cx = peakCx + side * w * (0.005f + depth)
            val cy = peakY + h * (0.005f + tt * 0.20f)
            val rx = w * (0.012f + rng.next() * 0.016f)
            val ry = h * (0.008f + rng.next() * 0.010f)
            // shadow underneath
            paint.color = withAlpha(capColShadow, 100)
            canvas.drawOval(cx - rx, cy - ry * 0.4f, cx + rx, cy + ry * 1.1f, paint)
            // main blob — irregular by jittering control points
            path.reset()
            path.moveTo(cx - rx, cy)
            path.cubicTo(
                cx - rx * 0.6f, cy - ry * (1.3f + rng.next() * 0.4f),
                cx + rx * 0.4f, cy - ry * (1.1f + rng.next() * 0.5f),
                cx + rx, cy - ry * 0.2f,
            )
            path.cubicTo(
                cx + rx * 0.6f, cy + ry * 0.7f,
                cx - rx * 0.4f, cy + ry * 0.6f,
                cx - rx, cy,
            )
            path.close()
            paint.color = withAlpha(capCol, (220f * (0.55f + snowAmt * 0.45f)).toInt().coerceIn(120, 240))
            canvas.drawPath(path, paint)
        }
        // A tiny snow tip right at the apex
        val tipR = w * 0.010f
        paint.color = withAlpha(capCol, (200f * (0.55f + snowAmt * 0.45f)).toInt())
        canvas.drawCircle(peakCx, peakY + tipR * 0.4f, tipR, paint)
    }

    private fun drawValleyRidge(canvas: Canvas, sky: SkyState, haze: Int, sunCx: Float, params: SceneParams) {
        // Single near foreground ridge sitting in front of the sea of clouds — gives depth past the fog.
        val base = when (params.season) {
            Season.SUMMER -> 0xFF2a3a30.toInt()
            Season.SPRING -> 0xFF34492f.toInt()
            Season.AUTUMN -> 0xFF6a4426.toInt()
            Season.WINTER -> 0xFF34384a.toInt()
        }
        val layer = RidgeLayer(
            seed = 5505, baseY = h * 0.78f, amplitude = h * 0.045f,
            color = lerpColor(base, sky.botColor, 0.05f), depth = 0.18f, rim = 0.4f,
        )
        drawRidgeStack(canvas, w, h, listOf(layer), haze, sky, sunCx)

        // tiny conifer crests
        val crestCol = lerpColor(base, 0xFF000000.toInt(), 0.35f)
        val rng = PRNG(5510)
        for (i in 0 until 22) {
            val x = rng.next() * w
            val baseY = h * 0.76f + rng.next() * h * 0.025f
            val treeH = h * 0.02f + rng.next() * h * 0.025f
            drawPaintedConifer(canvas, x, baseY, treeH, crestCol)
        }
    }

    private fun drawPollen(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val warmth = lerpColor(0xFFFFF5DA.toInt(), sky.horizonColor, 0.20f)
        drawSparkles(
            canvas, w, h,
            count = (18 + params.intensity * 26f).toInt(),
            seed = 909,
            color = withAlpha(warmth, 160),
            elapsedMs = params.elapsedMs,
            intensity = 0.5f + params.intensity * 0.4f,
            yLimit = 0.78f,
        )
    }
}
