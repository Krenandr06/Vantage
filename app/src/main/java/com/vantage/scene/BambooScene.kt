package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class BambooScene : VantageScene {

    private val stalkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stonePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lanternPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val canopyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawPath = Path()
    private var w = 0
    private var h = 0

    // ── Seasonal palette ────────────────────────────────────────────────
    private data class BambooPalette(
        val stalk: Int,
        val stalkHi: Int,
        val stalkLo: Int,
        val leafBright: Int,
        val leaf: Int,
        val leafDark: Int,
        val deep: Int,
        val pathLight: Int,
        val pathShade: Int,
        val wall: Int,
        val mossWall: Int,
        val fenceCane: Int,
        val rope: Int,
        val accent: Int?,
    )

    private fun seasonPalette(season: Season) = when (season) {
        Season.SPRING -> BambooPalette(
            stalk = 0xFFc4d49a.toInt(), stalkHi = 0xFFe0e8b6.toInt(), stalkLo = 0xFF8aa256.toInt(),
            leafBright = 0xFFaacc6e.toInt(), leaf = 0xFF6e8e3e.toInt(), leafDark = 0xFF3e5a22.toInt(),
            deep = 0xFF1c2e14.toInt(), pathLight = 0xFFa89c80.toInt(), pathShade = 0xFF5a5040.toInt(),
            wall = 0xFF6a6258.toInt(), mossWall = 0xFF7ea25a.toInt(), fenceCane = 0xFFb89e62.toInt(),
            rope = 0xFF3a2818.toInt(), accent = 0xFFf4c4d6.toInt(),
        )
        Season.SUMMER -> BambooPalette(
            stalk = 0xFFb6c882.toInt(), stalkHi = 0xFFd6dea0.toInt(), stalkLo = 0xFF7a924a.toInt(),
            leafBright = 0xFF88a84a.toInt(), leaf = 0xFF436a26.toInt(), leafDark = 0xFF1f3a14.toInt(),
            deep = 0xFF0e1e0c.toInt(), pathLight = 0xFF8c8068.toInt(), pathShade = 0xFF4a4234.toInt(),
            wall = 0xFF5e5650.toInt(), mossWall = 0xFF4a6a2e.toInt(), fenceCane = 0xFFa08550.toInt(),
            rope = 0xFF2a1e12.toInt(), accent = null,
        )
        Season.AUTUMN -> BambooPalette(
            stalk = 0xFFcabe72.toInt(), stalkHi = 0xFFe0d488.toInt(), stalkLo = 0xFF8e7a3a.toInt(),
            leafBright = 0xFFc8a850.toInt(), leaf = 0xFF7a5e28.toInt(), leafDark = 0xFF3a2a14.toInt(),
            deep = 0xFF1c1408.toInt(), pathLight = 0xFFa08868.toInt(), pathShade = 0xFF5a4830.toInt(),
            wall = 0xFF6a5e50.toInt(), mossWall = 0xFF7a6228.toInt(), fenceCane = 0xFF8c6a3a.toInt(),
            rope = 0xFF2a1a0c.toInt(), accent = 0xFFD9A058.toInt(),
        )
        Season.WINTER -> BambooPalette(
            stalk = 0xFFa8aea0.toInt(), stalkHi = 0xFFc2c6b8.toInt(), stalkLo = 0xFF76806c.toInt(),
            leafBright = 0xFF9caa90.toInt(), leaf = 0xFF5a6850.toInt(), leafDark = 0xFF2e362a.toInt(),
            deep = 0xFF181c18.toInt(), pathLight = 0xFF8e8a82.toInt(), pathShade = 0xFF5a564e.toInt(),
            wall = 0xFF787268.toInt(), mossWall = 0xFF787a68.toInt(), fenceCane = 0xFF9a8a6a.toInt(),
            rope = 0xFF3a2a1a.toInt(), accent = 0xFFe8e4d8.toInt(),
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private fun isNight(time: Float): Boolean = time < 5.5f || time > 19.5f

    private fun lanternGlowActive(time: Float): Boolean =
        isNight(time) || (time > 18f && time < 20.5f) || (time > 4.5f && time < 6.5f)

    private fun shouldDrawFog(params: SceneParams): Boolean =
        params.weather == WeatherType.FOG ||
            ((params.timeOfDay < 7f || params.timeOfDay > 19f) && params.intensity > 0.15f) ||
            params.season == Season.WINTER

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)

        val p = seasonPalette(params.season)

        drawCanopy(canvas, params, p)
        drawFarStalks(canvas, params, p)
        drawStoneWalls(canvas, params, p)
        drawStonePath(canvas, params, p)
        drawDappledLight(canvas, params)
        drawMidStalks(canvas, params, p)
        drawForegroundStalks(canvas, params, p)
        drawLeafClusters(canvas, params, p)
        drawLantern(canvas, params, p)
        if (shouldDrawFog(params)) drawFog(canvas, params)
    }

    private fun drawCanopy(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        canopyPaint.color = p.leafDark
        canopyPaint.alpha = 180
        val rng = PRNG(110)
        for (i in 0 until 15) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.15f
            val rx = 30f + rng.next() * 50f
            val ry = 15f + rng.next() * 20f
            canvas.drawOval(x - rx, y - ry, x + rx, y + ry, canopyPaint)
        }
        canopyPaint.alpha = 255
    }

    private fun drawFarStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        stalkPaint.color = lerpColor(p.stalkHi, p.stalk, 0.5f)
        stalkPaint.alpha = 120
        val rng = PRNG(220)
        for (i in 0 until 12) {
            val x = rng.next() * w
            val stalkW = 4f + rng.next() * 3f
            val sway = (Math.sin((params.elapsedMs / 3500.0) + i * 0.8) * 3 * params.intensity).toFloat()
            canvas.drawRect(x - stalkW / 2 + sway, 0f, x + stalkW / 2 + sway, h * 0.7f, stalkPaint)
            // Nodes
            for (n in 0 until 5) {
                val ny = h * 0.1f + n * h * 0.12f
                canvas.drawRect(x - stalkW / 2 - 1 + sway, ny - 1, x + stalkW / 2 + 1 + sway, ny + 1, stalkPaint)
            }
        }
        stalkPaint.alpha = 255
    }

    private fun drawStoneWalls(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        stonePaint.color = p.wall
        canvas.drawRect(0f, h * 0.65f, w * 0.15f, h * 0.85f, stonePaint)
        canvas.drawRect(w * 0.85f, h * 0.6f, w.toFloat(), h * 0.82f, stonePaint)

        // Moss on walls
        stonePaint.color = p.mossWall
        stonePaint.alpha = 150
        val rng = PRNG(330)
        for (i in 0 until 6) {
            val x = rng.next() * w * 0.14f
            val y = h * 0.65f + rng.next() * h * 0.15f
            canvas.drawOval(x - 8f, y - 3f, x + 8f, y + 3f, stonePaint)
        }
        for (i in 0 until 6) {
            val x = w * 0.86f + rng.next() * w * 0.13f
            val y = h * 0.6f + rng.next() * h * 0.17f
            canvas.drawOval(x - 8f, y - 3f, x + 8f, y + 3f, stonePaint)
        }
        stonePaint.alpha = 255
    }

    private fun drawStonePath(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        pathPaint.color = p.pathLight
        drawPath.reset()
        drawPath.moveTo(w * 0.3f, h.toFloat())
        drawPath.quadTo(w * 0.45f, h * 0.75f, w * 0.5f, h * 0.55f)
        drawPath.quadTo(w * 0.55f, h * 0.4f, w * 0.52f, h * 0.2f)
        drawPath.lineTo(w * 0.58f, h * 0.2f)
        drawPath.quadTo(w * 0.62f, h * 0.4f, w * 0.57f, h * 0.55f)
        drawPath.quadTo(w * 0.52f, h * 0.75f, w * 0.7f, h.toFloat())
        drawPath.close()
        canvas.drawPath(drawPath, pathPaint)

        // Stepping stones
        stonePaint.color = p.pathShade
        val rng = PRNG(440)
        for (i in 0 until 6) {
            val t = 0.3f + i * 0.1f
            val cx = w * (0.42f + (Math.sin(t * 3.0) * 0.06).toFloat())
            val cy = h * (0.5f + t * 0.45f)
            canvas.drawOval(cx - 12f, cy - 5f, cx + 12f, cy + 5f, stonePaint)
        }
    }

    private fun drawDappledLight(canvas: Canvas, params: SceneParams) {
        if (params.timeOfDay < 7f || params.timeOfDay > 17f) return
        lightPaint.color = 0xFFFFF8D0.toInt()
        lightPaint.alpha = 30
        val rng = PRNG(550)
        for (i in 0 until 10) {
            val x = w * 0.3f + rng.next() * w * 0.4f
            val y = h * 0.4f + rng.next() * h * 0.4f
            val r = 8f + rng.next() * 15f
            val flicker = (Math.sin(params.elapsedMs / 2000.0 + i * 1.2) * 0.3 + 0.7).toFloat()
            lightPaint.alpha = (30 * flicker).toInt().coerceIn(0, 255)
            canvas.drawOval(x - r, y - r * 0.6f, x + r, y + r * 0.6f, lightPaint)
        }
        lightPaint.alpha = 255
    }

    private fun drawMidStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        stalkPaint.color = p.stalk
        val rng = PRNG(660)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val stalkW = 6f + rng.next() * 5f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + i * 1.1) * 5 * params.intensity).toFloat()
            val top = -h * 0.05f
            val bot = h * 0.85f + rng.next() * h * 0.1f
            canvas.drawRect(x - stalkW / 2 + sway, top, x + stalkW / 2 + sway, bot, stalkPaint)

            // Nodes
            stalkPaint.color = lerpColor(p.stalk, p.stalkLo, 0.3f)
            val nodeSpacing = h * 0.08f
            var ny = h * 0.05f
            while (ny < bot) {
                canvas.drawRect(x - stalkW / 2 - 2 + sway, ny - 1.5f, x + stalkW / 2 + 2 + sway, ny + 1.5f, stalkPaint)
                ny += nodeSpacing + rng.next() * nodeSpacing * 0.5f
            }
            stalkPaint.color = p.stalk
        }
    }

    private fun drawForegroundStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        stalkPaint.color = p.stalkLo
        val rng = PRNG(770)
        for (i in 0 until 4) {
            val side = if (i % 2 == 0) -w * 0.02f + rng.next() * w * 0.12f
                       else w * 0.88f + rng.next() * w * 0.14f
            val stalkW = 10f + rng.next() * 6f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i * 0.7) * 8 * params.intensity).toFloat()
            canvas.drawRect(side - stalkW / 2 + sway, -10f, side + stalkW / 2 + sway, h * 0.95f, stalkPaint)
        }
    }

    private fun drawLeafClusters(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        leafPaint.color = p.leafBright
        val rng = PRNG(880)
        for (i in 0 until 20) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.6f
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i) * 4 * params.intensity).toFloat()
            drawPath.reset()
            drawPath.moveTo(x + sway, y)
            drawPath.quadTo(x + 10f + sway, y - 6f, x + 20f + sway, y)
            drawPath.quadTo(x + 10f + sway, y + 4f, x + sway, y)
            canvas.drawPath(drawPath, leafPaint)
        }
    }

    private fun drawLantern(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val lx = w * 0.48f
        val ly = h * 0.62f

        // Post
        lanternPaint.color = p.rope
        canvas.drawRect(lx - 3f, ly, lx + 3f, ly + 40f, lanternPaint)

        // Lantern body
        lanternPaint.color = p.fenceCane
        canvas.drawRect(lx - 10f, ly - 16f, lx + 10f, ly, lanternPaint)
        lanternPaint.color = lerpColor(p.fenceCane, p.rope, 0.5f)
        canvas.drawRect(lx - 12f, ly - 18f, lx + 12f, ly - 16f, lanternPaint)
        canvas.drawRect(lx - 12f, ly, lx + 12f, ly + 2f, lanternPaint)

        // Glow – active at night and during twilight transitions
        if (lanternGlowActive(params.timeOfDay)) {
            val flicker = (Math.sin(params.elapsedMs / 600.0) * 0.1 + 0.9).toFloat()
            lanternPaint.color = 0xFFFFCC55.toInt()
            lanternPaint.alpha = (180 * flicker).toInt()
            canvas.drawRect(lx - 8f, ly - 14f, lx + 8f, ly - 1f, lanternPaint)

            lanternPaint.color = 0xFFFFAA30.toInt()
            lanternPaint.alpha = (60 * flicker).toInt()
            canvas.drawCircle(lx, ly - 8f, 30f, lanternPaint)
            lanternPaint.alpha = (25 * flicker).toInt()
            canvas.drawCircle(lx, ly - 8f, 60f, lanternPaint)
            lanternPaint.alpha = 255
        }
    }

    private fun drawFog(canvas: Canvas, params: SceneParams) {
        fogPaint.color = 0xFFD0D0D0.toInt()
        val baseAlpha = if (params.weather == WeatherType.FOG) 55 else 30
        val rng = PRNG(990)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val y = h * 0.55f + rng.next() * h * 0.4f
            val rx = 60f + rng.next() * 100f
            val ry = 12f + rng.next() * 18f
            val drift = (Math.sin(params.elapsedMs / 5000.0 + i * 0.9) * 20).toFloat()
            fogPaint.alpha = (baseAlpha + (Math.sin(params.elapsedMs / 4000.0 + i * 1.4) * 10).toInt()).coerceIn(0, 255)
            canvas.drawOval(x - rx + drift, y - ry, x + rx + drift, y + ry, fogPaint)
        }
        fogPaint.alpha = 255
    }
}
