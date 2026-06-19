package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

class BambooScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawPath = Path()
    private var w = 0
    private var h = 0

    private data class BambooPalette(
        val stalk: Int, val stalkHi: Int, val stalkLo: Int,
        val leafBright: Int, val leaf: Int, val leafDark: Int,
        val deep: Int,
        val pathLight: Int, val pathShade: Int,
        val wall: Int, val mossWall: Int,
        val fenceCane: Int, val rope: Int,
        val accent: Int?,
    )

    private fun seasonPalette(season: Season) = when (season) {
        Season.SPRING -> BambooPalette(
            stalk = 0xFFC4D49A.toInt(), stalkHi = 0xFFE0E8B6.toInt(), stalkLo = 0xFF8AA256.toInt(),
            leafBright = 0xFFAACC6E.toInt(), leaf = 0xFF6E8E3E.toInt(), leafDark = 0xFF3E5A22.toInt(),
            deep = 0xFF1C2E14.toInt(), pathLight = 0xFFAB9D80.toInt(), pathShade = 0xFF5A5040.toInt(),
            wall = 0xFF6A6258.toInt(), mossWall = 0xFF7EA25A.toInt(), fenceCane = 0xFFB89E62.toInt(),
            rope = 0xFF3A2818.toInt(), accent = 0xFFF4C4D6.toInt(),
        )
        Season.SUMMER -> BambooPalette(
            stalk = 0xFFB6C882.toInt(), stalkHi = 0xFFD6DEA0.toInt(), stalkLo = 0xFF7A924A.toInt(),
            leafBright = 0xFF88A84A.toInt(), leaf = 0xFF436A26.toInt(), leafDark = 0xFF1F3A14.toInt(),
            deep = 0xFF0E1E0C.toInt(), pathLight = 0xFF8C8068.toInt(), pathShade = 0xFF4A4234.toInt(),
            wall = 0xFF5E5650.toInt(), mossWall = 0xFF4A6A2E.toInt(), fenceCane = 0xFFA08550.toInt(),
            rope = 0xFF2A1E12.toInt(), accent = null,
        )
        Season.AUTUMN -> BambooPalette(
            stalk = 0xFFCABE72.toInt(), stalkHi = 0xFFE0D488.toInt(), stalkLo = 0xFF8E7A3A.toInt(),
            leafBright = 0xFFC8A850.toInt(), leaf = 0xFF7A5E28.toInt(), leafDark = 0xFF3A2A14.toInt(),
            deep = 0xFF1C1408.toInt(), pathLight = 0xFFA08868.toInt(), pathShade = 0xFF5A4830.toInt(),
            wall = 0xFF6A5E50.toInt(), mossWall = 0xFF7A6228.toInt(), fenceCane = 0xFF8C6A3A.toInt(),
            rope = 0xFF2A1A0C.toInt(), accent = 0xFFD9A058.toInt(),
        )
        Season.WINTER -> BambooPalette(
            stalk = 0xFFA8AEA0.toInt(), stalkHi = 0xFFC2C6B8.toInt(), stalkLo = 0xFF76806C.toInt(),
            leafBright = 0xFF9CAA90.toInt(), leaf = 0xFF5A6850.toInt(), leafDark = 0xFF2E362A.toInt(),
            deep = 0xFF181C18.toInt(), pathLight = 0xFF8E8A82.toInt(), pathShade = 0xFF5A564E.toInt(),
            wall = 0xFF787268.toInt(), mossWall = 0xFF787A68.toInt(), fenceCane = 0xFF9A8A6A.toInt(),
            rope = 0xFF3A2A1A.toInt(), accent = 0xFFE8E4D8.toInt(),
        )
    }

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
        val haze = hazeColor(sky)
        val p = seasonPalette(params.season)

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = 0.30f)
        drawMoon(canvas, w, h, sky, cxFrac = 0.70f)

        drawCanopy(canvas, p, haze)
        drawFarStalks(canvas, params, p, haze)
        drawCanopyHaze(canvas, haze)
        drawStoneWalls(canvas, p)
        drawStonePath(canvas, params, p)
        drawDappledLight(canvas, params, sky)
        drawMidStalks(canvas, params, p)
        drawForegroundStalks(canvas, params, p)
        drawLeafClusters(canvas, params, p)
        drawLantern(canvas, params, p)
        if (shouldDrawFog(params)) drawSoftFog(canvas, haze, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 90), strength = 0.60f)
    }

    private fun drawCanopy(canvas: Canvas, p: BambooPalette, haze: Int) {
        val rng = PRNG(110)
        for (i in 0 until 18) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.18f
            val rx = 38f + rng.next() * 56f
            val ry = 16f + rng.next() * 22f
            drawSoftFogPuff(canvas, x, y, rx, ry, p.leafDark, alpha = 200)
        }
    }

    private fun drawFarStalks(canvas: Canvas, params: SceneParams, p: BambooPalette, haze: Int) {
        val color = lerpColor(p.stalkHi, haze, 0.55f)
        val rng = PRNG(220)
        for (i in 0 until 14) {
            val x = rng.next() * w
            val stalkW = 4f + rng.next() * 3f
            val sway = (Math.sin((params.elapsedMs / 3500.0) + i * 0.8) * 3 * params.intensity).toFloat()
            paint.color = withAlpha(color, 140)
            canvas.drawRect(x - stalkW / 2 + sway, 0f, x + stalkW / 2 + sway, h * 0.72f, paint)
            for (n in 0 until 6) {
                val ny = h * 0.1f + n * h * 0.11f
                canvas.drawRect(x - stalkW / 2 - 1 + sway, ny - 1, x + stalkW / 2 + 1 + sway, ny + 1, paint)
            }
        }
        paint.alpha = 255
    }

    private fun drawCanopyHaze(canvas: Canvas, haze: Int) {
        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.30f)
        drawHazeBand(canvas, w, h * 0.18f, h * 0.42f, mist, peakAlpha = 90)
    }

    private fun drawStoneWalls(canvas: Canvas, p: BambooPalette) {
        // gradient walls
        paint.shader = LinearGradient(
            0f, h * 0.65f, 0f, h * 0.85f,
            lerpColor(p.wall, 0xFFFFFFFF.toInt(), 0.10f),
            lerpColor(p.wall, 0xFF000000.toInt(), 0.25f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.65f, w * 0.15f, h * 0.85f, paint)
        canvas.drawRect(w * 0.85f, h * 0.6f, w.toFloat(), h * 0.82f, paint)
        paint.shader = null

        // moss
        val rng = PRNG(330)
        for (side in 0..1) {
            val xBase = if (side == 0) 0f else w * 0.86f
            val xRange = if (side == 0) w * 0.14f else w * 0.13f
            val yBase = if (side == 0) h * 0.65f else h * 0.60f
            for (i in 0 until 7) {
                val x = xBase + rng.next() * xRange
                val y = yBase + rng.next() * h * 0.17f
                drawSoftFogPuff(canvas, x, y, 12f, 4f, p.mossWall, alpha = 170)
            }
        }
    }

    private fun drawStonePath(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        paint.shader = LinearGradient(
            0f, h * 0.2f, 0f, h.toFloat(),
            lerpColor(p.pathLight, 0xFFFFFFFF.toInt(), 0.10f),
            lerpColor(p.pathLight, 0xFF000000.toInt(), 0.20f),
            Shader.TileMode.CLAMP,
        )
        drawPath.reset()
        drawPath.moveTo(w * 0.3f, h.toFloat())
        drawPath.quadTo(w * 0.45f, h * 0.75f, w * 0.5f, h * 0.55f)
        drawPath.quadTo(w * 0.55f, h * 0.4f, w * 0.52f, h * 0.2f)
        drawPath.lineTo(w * 0.58f, h * 0.2f)
        drawPath.quadTo(w * 0.62f, h * 0.4f, w * 0.57f, h * 0.55f)
        drawPath.quadTo(w * 0.52f, h * 0.75f, w * 0.7f, h.toFloat())
        drawPath.close()
        canvas.drawPath(drawPath, paint)
        paint.shader = null

        paint.color = p.pathShade
        val rng = PRNG(440)
        for (i in 0 until 7) {
            val t = 0.28f + i * 0.10f
            val cx = w * (0.42f + (Math.sin(t * 3.0) * 0.06).toFloat())
            val cy = h * (0.48f + t * 0.48f)
            canvas.drawOval(cx - 13f, cy - 5.5f, cx + 13f, cy + 5.5f, paint)
            // soft highlight on each stone
            paint.color = withAlpha(p.pathLight, 130)
            canvas.drawOval(cx - 11f, cy - 5.5f, cx + 11f, cy - 1f, paint)
            paint.color = p.pathShade
        }
        paint.alpha = 255
    }

    private fun drawDappledLight(canvas: Canvas, params: SceneParams, sky: SkyState) {
        if (params.timeOfDay < 7f || params.timeOfDay > 17f) return
        val warm = lerpColor(0xFFFFF6D6.toInt(), sky.midColor, 0.20f)
        val rng = PRNG(550)
        for (i in 0 until 12) {
            val x = w * 0.28f + rng.next() * w * 0.44f
            val y = h * 0.40f + rng.next() * h * 0.42f
            val r = 10f + rng.next() * 18f
            val flicker = (Math.sin(params.elapsedMs / 2000.0 + i * 1.2) * 0.3 + 0.7).toFloat()
            drawSoftFogPuff(canvas, x, y, r, r * 0.6f, warm, alpha = (60 * flicker).toInt().coerceIn(0, 255))
        }
    }

    private fun drawMidStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val rng = PRNG(660)
        for (i in 0 until 9) {
            val x = rng.next() * w
            val stalkW = 7f + rng.next() * 5f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + i * 1.1) * 5 * params.intensity).toFloat()
            val top = -h * 0.05f
            val bot = h * 0.85f + rng.next() * h * 0.1f

            paint.shader = LinearGradient(
                x - stalkW / 2 + sway, 0f, x + stalkW / 2 + sway, 0f,
                p.stalkHi, p.stalkLo, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(x - stalkW / 2 + sway, top, x + stalkW / 2 + sway, bot, paint)
            paint.shader = null

            paint.color = lerpColor(p.stalk, p.stalkLo, 0.4f)
            val nodeSpacing = h * 0.08f
            var ny = h * 0.05f
            while (ny < bot) {
                canvas.drawRect(x - stalkW / 2 - 2 + sway, ny - 1.5f, x + stalkW / 2 + 2 + sway, ny + 1.5f, paint)
                ny += nodeSpacing + rng.next() * nodeSpacing * 0.5f
            }
        }
    }

    private fun drawForegroundStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val rng = PRNG(770)
        for (i in 0 until 4) {
            val side = if (i % 2 == 0) -w * 0.02f + rng.next() * w * 0.12f
                       else w * 0.88f + rng.next() * w * 0.14f
            val stalkW = 11f + rng.next() * 6f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i * 0.7) * 8 * params.intensity).toFloat()
            paint.shader = LinearGradient(
                side - stalkW / 2 + sway, 0f, side + stalkW / 2 + sway, 0f,
                lerpColor(p.stalk, p.stalkHi, 0.2f),
                lerpColor(p.stalkLo, 0xFF000000.toInt(), 0.15f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(side - stalkW / 2 + sway, -10f, side + stalkW / 2 + sway, h * 0.95f, paint)
            paint.shader = null
        }
    }

    private fun drawLeafClusters(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val rng = PRNG(880)
        for (i in 0 until 28) {
            val x = rng.next() * w
            val y = rng.next() * h * 0.62f
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i) * 4 * params.intensity).toFloat()
            val tint = if (rng.next() < 0.25f) p.leafBright else p.leaf
            paint.color = tint
            drawPath.reset()
            drawPath.moveTo(x + sway, y)
            drawPath.quadTo(x + 12f + sway, y - 7f, x + 22f + sway, y)
            drawPath.quadTo(x + 12f + sway, y + 5f, x + sway, y)
            canvas.drawPath(drawPath, paint)
        }
    }

    private fun drawLantern(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val lx = w * 0.48f
        val ly = h * 0.62f

        paint.color = p.rope
        canvas.drawRect(lx - 3f, ly, lx + 3f, ly + 40f, paint)
        paint.color = p.fenceCane
        canvas.drawRect(lx - 11f, ly - 18f, lx + 11f, ly, paint)
        paint.color = lerpColor(p.fenceCane, p.rope, 0.5f)
        canvas.drawRect(lx - 13f, ly - 20f, lx + 13f, ly - 18f, paint)
        canvas.drawRect(lx - 13f, ly, lx + 13f, ly + 2f, paint)

        if (lanternGlowActive(params.timeOfDay)) {
            val flicker = (Math.sin(params.elapsedMs / 600.0) * 0.1 + 0.9).toFloat()
            paint.color = withAlpha(0xFFFFCC55.toInt(), (200 * flicker).toInt().coerceIn(0, 255))
            canvas.drawRect(lx - 9f, ly - 16f, lx + 9f, ly - 2f, paint)
            drawSoftGlow(canvas, lx, ly - 9f, 14f, withAlpha(0xFFFFAA30.toInt(), (200 * flicker).toInt().coerceIn(0, 255)), intensity = flicker)
            paint.alpha = 255
        }
    }

    private fun drawSoftFog(canvas: Canvas, haze: Int, params: SceneParams) {
        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.35f)
        val baseAlpha = if (params.weather == WeatherType.FOG) 75 else 50
        val rng = PRNG(990)
        for (i in 0 until 10) {
            val x = rng.next() * w
            val y = h * 0.50f + rng.next() * h * 0.42f
            val rx = 70f + rng.next() * 130f
            val ry = 14f + rng.next() * 20f
            val drift = (Math.sin(params.elapsedMs / 5000.0 + i * 0.9) * 22).toFloat()
            val pulse = (Math.sin(params.elapsedMs / 4000.0 + i * 1.4) * 12).toInt()
            drawSoftFogPuff(canvas, x + drift, y, rx, ry, mist, alpha = (baseAlpha + pulse).coerceIn(0, 255))
        }
    }
}
