package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class ForestScene : VantageScene {

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
        val p = seasonPalette(params.season)

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = 0.72f)
        drawMoon(canvas, w, h, sky, cxFrac = 0.30f)

        drawSoftClouds(canvas, sky, params)
        drawFarTreeBand(canvas, p, haze, sky)
        drawMidTreeBand(canvas, p, haze, params)
        drawMistBand(canvas, haze)
        drawLightShafts(canvas, sky, params)
        drawNearTrees(canvas, p, params)
        drawForestFloor(canvas, p, haze, params)
        drawFerns(canvas, p, params)

        if (params.season == Season.AUTUMN && params.intensity > 0.05f) drawFallingLeaves(canvas, p, params)
        if (params.season == Season.SUMMER && (params.timeOfDay < 6f || params.timeOfDay > 19f) && params.intensity > 0.1f) {
            drawFireflies(canvas, params)
        }
        if (params.weather == WeatherType.FOG || params.timeOfDay < 7.5f || params.timeOfDay > 19f || params.season == Season.WINTER) {
            drawFloorMist(canvas, haze, params)
        }
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 80), strength = 0.55f)
    }

    private data class ForestPalette(
        val farTree: Int, val midTree: Int, val nearTree: Int,
        val canopyHi: Int, val trunk: Int,
        val floor: Int, val fern: Int, val leaf: Int, val moss: Int,
    )

    private fun seasonPalette(season: Season) = when (season) {
        Season.SPRING -> ForestPalette(
            farTree = 0xFF7B9A6E.toInt(), midTree = 0xFF4E7048.toInt(), nearTree = 0xFF1F3A22.toInt(),
            canopyHi = 0xFFB7CE82.toInt(), trunk = 0xFF2A2418.toInt(),
            floor = 0xFF3F5230.toInt(), fern = 0xFF52743A.toInt(), leaf = 0xFFB7CE82.toInt(), moss = 0xFF82A560.toInt(),
        )
        Season.SUMMER -> ForestPalette(
            farTree = 0xFF44664C.toInt(), midTree = 0xFF2E4A2E.toInt(), nearTree = 0xFF132212.toInt(),
            canopyHi = 0xFF6A8E48.toInt(), trunk = 0xFF1F1A12.toInt(),
            floor = 0xFF253B1E.toInt(), fern = 0xFF2E4A2E.toInt(), leaf = 0xFF6A8E48.toInt(), moss = 0xFF44663A.toInt(),
        )
        Season.AUTUMN -> ForestPalette(
            farTree = 0xFFA66A3E.toInt(), midTree = 0xFF7A4326.toInt(), nearTree = 0xFF2C1A10.toInt(),
            canopyHi = 0xFFE8AE6E.toInt(), trunk = 0xFF2A1C12.toInt(),
            floor = 0xFF3B2A18.toInt(), fern = 0xFF6E462A.toInt(), leaf = 0xFFE48A4E.toInt(), moss = 0xFF7A5828.toInt(),
        )
        Season.WINTER -> ForestPalette(
            farTree = 0xFF7E848C.toInt(), midTree = 0xFF4E5460.toInt(), nearTree = 0xFF1E2228.toInt(),
            canopyHi = 0xFFE6E2D8.toInt(), trunk = 0xFF2A2418.toInt(),
            floor = 0xFFBEB8AA.toInt(), fern = 0xFF6A6E68.toInt(), leaf = 0xFFE8E4D8.toInt(), moss = 0xFF887C68.toInt(),
        )
    }

    private fun drawSoftClouds(canvas: Canvas, sky: SkyState, params: SceneParams) {
        val base = lerpColor(0xFFFFE2C8.toInt(), sky.midColor, 0.40f)
        val rim = lerpColor(0xFFFFF8E2.toInt(), sky.topColor, 0.20f)
        val rng = PRNG(81)
        val drift = (params.elapsedMs * 0.000004f) % 1f
        for (i in 0 until 4) {
            val cx = ((rng.next() + drift) % 1f) * w * 1.4f - w * 0.2f
            val cy = h * (0.08f + rng.next() * 0.14f)
            val scale = h * (0.025f + rng.next() * 0.025f)
            drawFluffyCloud(canvas, cx, cy, scale, base, rim, alpha = 180)
        }
    }

    private fun drawFarTreeBand(canvas: Canvas, p: ForestPalette, haze: Int, sky: SkyState) {
        // Layered foggy tree-line — a smooth ridge with soft conifer crowns peeking out.
        smoothRidgePath(path, w, h, h * 0.40f, h * 0.04f, seed = 821)
        drawAerialLayer(canvas, path, h * 0.32f, h * 0.52f, p.farTree, haze, depth = 0.85f)

        // tiny conifers along crest, also fully aerial-faded
        val crownColor = lerpColor(p.farTree, haze, 0.55f)
        val rng = PRNG(8210)
        for (i in 0 until 22) {
            val x = rng.next() * w
            val baseY = h * 0.38f + rng.next() * h * 0.04f
            val ht = h * 0.025f + rng.next() * h * 0.025f
            drawPaintedConifer(canvas, x, baseY, ht, crownColor)
        }
    }

    private fun drawMidTreeBand(canvas: Canvas, p: ForestPalette, haze: Int, params: SceneParams) {
        smoothRidgePath(path, w, h, h * 0.56f, h * 0.05f, seed = 432)
        drawAerialLayer(canvas, path, h * 0.46f, h * 0.66f, p.midTree, haze, depth = 0.50f)

        val crownColor = lerpColor(p.midTree, haze, 0.30f)
        val rng = PRNG(4320)
        for (i in 0 until 16) {
            val x = rng.next() * w
            val baseY = h * 0.55f + rng.next() * h * 0.04f
            val ht = h * 0.045f + rng.next() * h * 0.045f
            drawPaintedConifer(canvas, x, baseY, ht, crownColor)
        }
    }

    private fun drawMistBand(canvas: Canvas, haze: Int) {
        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.40f)
        drawHazeBand(canvas, w, h * 0.52f, h * 0.64f, mist, peakAlpha = 140)
    }

    private fun drawLightShafts(canvas: Canvas, sky: SkyState, params: SceneParams) {
        if (params.timeOfDay < 6.5f || params.timeOfDay > 18.5f) return
        val warm = lerpColor(0xFFFFF6D6.toInt(), sky.midColor, 0.20f)
        val rng = PRNG(303)
        val count = 4
        for (i in 0 until count) {
            val cx = rng.next() * w
            val sway = (Math.sin((params.elapsedMs / 4000.0) + i.toDouble()) * w * 0.012f).toFloat()
            val halfW = w * (0.045f + rng.next() * 0.05f)
            val alpha = (50 + rng.next() * 40f).toInt().coerceIn(0, 255)
            drawLightShaft(canvas, cx + sway, 0f, h * 0.82f, halfW, warm, topAlpha = alpha)
        }
    }

    private fun drawNearTrees(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        val rng = PRNG(404)
        for (i in 0 until 5) {
            val x = rng.next() * w
            val trunkH = h * (0.40f + rng.next() * 0.20f)
            val trunkW = w * 0.025f + rng.next() * w * 0.02f
            val baseY = h * 0.62f

            paint.shader = LinearGradient(
                x - trunkW / 2, 0f, x + trunkW / 2, 0f,
                lerpColor(p.trunk, 0xFFFFFFFF.toInt(), 0.18f),
                lerpColor(p.trunk, 0xFF000000.toInt(), 0.30f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(x - trunkW / 2, baseY, x + trunkW / 2, baseY + trunkH, paint)
            paint.shader = null

            val r = w * (0.10f + rng.next() * 0.08f)
            drawPaintedCanopy(canvas, x, baseY - r * 0.2f, r, p.nearTree, p.canopyHi, alpha = 240)

            // Vines
            paint.color = withAlpha(p.fern, 130)
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            val vineLen = h * 0.05f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2500.0) + i) * 3.0).toFloat()
            canvas.drawLine(x - r * 0.3f, baseY, x - r * 0.3f + sway, baseY + vineLen, paint)
            canvas.drawLine(x + r * 0.4f, baseY, x + r * 0.4f + sway, baseY + vineLen * 0.8f, paint)
            paint.style = Paint.Style.FILL
            paint.alpha = 255
        }
    }

    private fun drawForestFloor(canvas: Canvas, p: ForestPalette, haze: Int, params: SceneParams) {
        val top = h * 0.78f
        paint.shader = LinearGradient(
            0f, top, 0f, h.toFloat(),
            lerpColor(p.floor, 0xFFFFFFFF.toInt(), 0.05f),
            lerpColor(p.floor, 0xFF000000.toInt(), 0.30f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, top, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // moss patches with soft blur
        val rng = PRNG(505)
        for (i in 0 until 12) {
            val x = rng.next() * w
            val y = h * 0.82f + rng.next() * h * 0.12f
            drawSoftFogPuff(canvas, x, y, 24f, 6f, p.moss, alpha = 120)
        }

        // mushrooms & stones
        for (i in 0 until 6) {
            val x = rng.next() * w
            val y = h * 0.82f + rng.next() * h * 0.10f
            if (i % 2 == 0) {
                paint.color = withAlpha(0xFF8A7A6A.toInt(), 230)
                canvas.drawOval(x - 9f, y - 5f, x + 9f, y + 5f, paint)
                paint.color = withAlpha(0xFFB09A86.toInt(), 200)
                canvas.drawOval(x - 9f, y - 5f, x + 9f, y - 1f, paint)
            } else {
                paint.color = 0xFFC84040.toInt()
                canvas.drawCircle(x, y - 6f, 5f, paint)
                paint.color = withAlpha(0xFFFFE8C8.toInt(), 200)
                canvas.drawCircle(x - 1.5f, y - 7.5f, 1.4f, paint)
                paint.color = 0xFFE8D8C0.toInt()
                canvas.drawRect(x - 2f, y - 2f, x + 2f, y + 4f, paint)
            }
        }
        paint.alpha = 255
    }

    private fun drawFerns(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        paint.color = p.fern
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.strokeCap = Paint.Cap.ROUND
        val rng = PRNG(606)
        for (i in 0 until 10) {
            val x = rng.next() * w
            val baseY = h * 0.86f + rng.next() * h * 0.10f
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i * 1.5) * 8.0 * params.intensity).toFloat()
            path.reset()
            path.moveTo(x, baseY)
            path.quadTo(x - 18f + sway, baseY - 38f, x - 32f + sway * 1.4f, baseY - 28f)
            canvas.drawPath(path, paint)
            path.reset()
            path.moveTo(x, baseY)
            path.quadTo(x + 16f + sway, baseY - 32f, x + 28f + sway * 1.4f, baseY - 22f)
            canvas.drawPath(path, paint)
            path.reset()
            path.moveTo(x, baseY)
            path.quadTo(x + sway, baseY - 42f, x + sway * 0.5f, baseY - 48f)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawFallingLeaves(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        val rng = PRNG(707)
        for (i in 0 until 18) {
            val baseX = rng.next() * w
            val speed = 30f + rng.next() * 40f
            val phase = rng.next() * 6.28f
            val y = ((params.elapsedMs / 1000f * speed * 0.3f + rng.next() * h) % (h * 1.1f)) - h * 0.05f
            val x = baseX + (Math.sin((params.elapsedMs / 1500.0) + phase.toDouble()) * 30).toFloat()
            val rot = ((params.elapsedMs / 800f + phase) % 360f)

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)
            paint.color = withAlpha(p.leaf, (200 + rng.next() * 55f).toInt().coerceIn(0, 255))
            canvas.drawOval(-5f, -2f, 5f, 2f, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }

    private fun drawFireflies(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(808)
        for (i in 0 until 14) {
            val baseX = rng.next() * w
            val baseY = h * 0.4f + rng.next() * h * 0.4f
            val phase = rng.next() * 6.28f
            val x = baseX + (Math.sin((params.elapsedMs / 3000.0) + phase) * 22).toFloat()
            val y = baseY + (Math.cos((params.elapsedMs / 4000.0) + phase * 1.3) * 16).toFloat()
            val brightness = ((Math.sin((params.elapsedMs / 1200.0) + phase * 2) + 1) / 2).toFloat()
            drawGlowParticle(canvas, x, y, 3.5f, 0xFFFFE060.toInt(), (200 * brightness).toInt().coerceIn(0, 255))
        }
    }

    private fun drawFloorMist(canvas: Canvas, haze: Int, params: SceneParams) {
        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.45f)
        val rng = PRNG(909)
        for (i in 0 until 8) {
            val x = rng.next() * w + (Math.sin(params.elapsedMs / 5000.0 + i) * 22).toFloat()
            val y = h * 0.74f + rng.next() * h * 0.18f
            drawSoftFogPuff(canvas, x, y, 110f, 18f, mist, alpha = 70)
        }
    }
}
