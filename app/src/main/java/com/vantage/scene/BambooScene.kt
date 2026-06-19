package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

/**
 * Bamboo grove — calm, deliberate composition.
 *
 *  • Soft hazy backdrop of distant canopy (no individual canes back there).
 *  • A mid grove of well-spaced canes on the left and right edges.
 *  • Two foreground hero canes framing the scene.
 *  • A small handful of young shoots and leaf tufts at deliberate positions.
 *  • Centerpiece paper lantern.
 */
class BambooScene : VantageScene {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var w = 0
    private var h = 0

    private data class BambooPalette(
        val stalk: Int, val stalkHi: Int, val stalkLo: Int, val stalkNode: Int,
        val leafBright: Int, val leaf: Int, val leafDark: Int,
        val groundDark: Int,
        val lanternGlow: Int,
    )

    private fun seasonPalette(season: Season) = when (season) {
        Season.SPRING -> BambooPalette(
            stalk = 0xFFcfd896.toInt(), stalkHi = 0xFFe6ecb6.toInt(), stalkLo = 0xFF8a9852.toInt(), stalkNode = 0xFF5c6c3a.toInt(),
            leafBright = 0xFFb8cf6e.toInt(), leaf = 0xFF6a8e3e.toInt(), leafDark = 0xFF364a22.toInt(),
            groundDark = 0xFF222818.toInt(), lanternGlow = 0xFFFFD478.toInt(),
        )
        Season.SUMMER -> BambooPalette(
            stalk = 0xFFb6c882.toInt(), stalkHi = 0xFFd4dea0.toInt(), stalkLo = 0xFF6e8a44.toInt(), stalkNode = 0xFF3e4e26.toInt(),
            leafBright = 0xFF8eb148.toInt(), leaf = 0xFF436a26.toInt(), leafDark = 0xFF1f3a14.toInt(),
            groundDark = 0xFF181e10.toInt(), lanternGlow = 0xFFFFC862.toInt(),
        )
        Season.AUTUMN -> BambooPalette(
            stalk = 0xFFcabe72.toInt(), stalkHi = 0xFFe0d488.toInt(), stalkLo = 0xFF8e7a3a.toInt(), stalkNode = 0xFF4a3818.toInt(),
            leafBright = 0xFFd8a850.toInt(), leaf = 0xFF8a5a28.toInt(), leafDark = 0xFF3a2814.toInt(),
            groundDark = 0xFF221608.toInt(), lanternGlow = 0xFFFFB05A.toInt(),
        )
        Season.WINTER -> BambooPalette(
            stalk = 0xFFb6bab0.toInt(), stalkHi = 0xFFcfd2c2.toInt(), stalkLo = 0xFF7e8870.toInt(), stalkNode = 0xFF4a4e44.toInt(),
            leafBright = 0xFFa2b099.toInt(), leaf = 0xFF6a7860.toInt(), leafDark = 0xFF323a32.toInt(),
            groundDark = 0xFF1c2020.toInt(), lanternGlow = 0xFFFFD0A0.toInt(),
        )
    }

    private fun lanternGlowActive(time: Float): Boolean = time < 6.5f || time > 17.5f

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        val haze = hazeColor(sky)
        val p = seasonPalette(params.season)
        val sunCx = w * 0.32f

        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky, cxFrac = sunCx / w)
        drawMoon(canvas, w, h, sky, cxFrac = 0.74f)

        // Distant canopy — soft hazy mass at the top of the frame.
        drawDistantCanopy(canvas, p, haze)
        // Smooth horizon haze that bleeds the distant canopy into the sky.
        drawHazeBand(canvas, w, h * 0.32f, h * 0.52f, lerpColor(haze, 0xFFFFFFFF.toInt(), 0.28f), peakAlpha = 90)

        // Mid grove — a few canes on the left and right leaving open middle.
        drawMidGrove(canvas, params, p)

        // Dappled light shafts in the open middle.
        drawDappledLight(canvas, params, sky)

        // Foreground hero canes.
        drawForegroundStalks(canvas, params, p)

        // Young shoots & leaf tufts — small, deliberate.
        drawYoungShoots(canvas, params, p)

        // Lantern.
        drawLantern(canvas, params, p)

        // Ground.
        drawGround(canvas, params, p, haze)

        if (shouldDrawFog(params)) drawSoftFog(canvas, haze, params)
        drawVignette(canvas, w, h, withAlpha(0xFF000010.toInt(), 90), strength = 0.60f)
    }

    private fun shouldDrawFog(params: SceneParams): Boolean =
        params.weather == WeatherType.FOG ||
            ((params.timeOfDay < 7f || params.timeOfDay > 19f) && params.intensity > 0.15f) ||
            params.season == Season.WINTER

    private fun drawDistantCanopy(canvas: Canvas, p: BambooPalette, haze: Int) {
        // A horizontal band of soft canopy mass. Three layers, increasingly hazed.
        val deep = lerpColor(p.leafDark, haze, 0.50f)
        val rng = PRNG(110)
        for (i in 0 until 10) {
            val x = rng.next() * w
            val y = h * 0.28f + rng.next() * h * 0.12f
            val rx = 60f + rng.next() * 90f
            val ry = 20f + rng.next() * 24f
            drawSoftFogPuff(canvas, x, y, rx, ry, deep, alpha = 180)
        }
        val mid = lerpColor(p.leaf, haze, 0.40f)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val y = h * 0.20f + rng.next() * h * 0.10f
            val rx = 50f + rng.next() * 70f
            val ry = 16f + rng.next() * 20f
            drawSoftFogPuff(canvas, x, y, rx, ry, mid, alpha = 150)
        }
    }

    private fun drawMidGrove(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        // 4 canes on the left, 4 canes on the right, deliberately spaced.
        val color = p.stalk
        val hi = p.stalkHi
        val lo = p.stalkLo
        val node = p.stalkNode
        val rng = PRNG(660)
        val positions = floatArrayOf(0.06f, 0.13f, 0.22f, 0.30f, 0.70f, 0.79f, 0.87f, 0.94f)
        for ((idx, frac) in positions.withIndex()) {
            val x = w * frac + (rng.next() - 0.5f) * w * 0.02f
            val stalkW = 6.5f + rng.next() * 3.5f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + idx * 1.1) * 3.5 * params.intensity).toFloat()
            val top = -h * 0.05f
            val bot = h * 0.84f + rng.next() * h * 0.04f
            val segH = h * (0.10f + rng.next() * 0.02f)
            drawBambooStalk(canvas, x, top, bot, stalkW, color, hi, lo, node, segH, sway)
        }
    }

    private fun drawDappledLight(canvas: Canvas, params: SceneParams, sky: SkyState) {
        if (params.timeOfDay < 7f || params.timeOfDay > 17f) return
        val warm = lerpColor(0xFFFFF6D6.toInt(), sky.horizonColor, 0.25f)
        // a couple of soft vertical light shafts in the open middle
        drawLightShaft(canvas, w * 0.42f, 0f, h * 0.85f, w * 0.055f, warm, topAlpha = 55)
        drawLightShaft(canvas, w * 0.58f, 0f, h * 0.85f, w * 0.045f, warm, topAlpha = 42)

        // a few drifting warm motes
        val rng = PRNG(550)
        for (i in 0 until 6) {
            val x = w * 0.30f + rng.next() * w * 0.40f
            val y = h * 0.45f + rng.next() * h * 0.35f
            val r = 12f + rng.next() * 18f
            val flicker = (Math.sin(params.elapsedMs / 2000.0 + i * 1.2) * 0.3 + 0.7).toFloat()
            drawSoftFogPuff(canvas, x, y, r, r * 0.6f, warm, alpha = (40 * flicker).toInt().coerceIn(0, 255))
        }
    }

    private fun drawForegroundStalks(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val color = lerpColor(p.stalk, p.stalkLo, 0.12f)
        val hi = p.stalkHi
        val lo = lerpColor(p.stalkLo, 0xFF000000.toInt(), 0.18f)
        val node = lerpColor(p.stalkNode, 0xFF000000.toInt(), 0.20f)
        // 2 hero canes framing the composition
        val positions = floatArrayOf(0.03f, 0.97f)
        for ((idx, frac) in positions.withIndex()) {
            val x = w * frac
            val stalkW = 14f + (if (idx == 0) 2f else 0f)
            val sway = (Math.sin((params.elapsedMs / 2200.0) + idx * 0.7) * 6 * params.intensity).toFloat()
            drawBambooStalk(canvas, x, -h * 0.05f, h * 0.97f, stalkW, color, hi, lo, node, h * 0.10f, sway)
        }
    }

    private fun drawYoungShoots(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val color = lerpColor(p.stalk, p.leafBright, 0.30f)
        val hi = lerpColor(p.stalkHi, p.leafBright, 0.20f)
        val lo = p.stalkLo
        val node = p.stalkNode
        // 5 deliberate shoots nestled near the lantern area
        val positions = floatArrayOf(0.36f, 0.46f, 0.54f, 0.62f, 0.70f)
        val tops = floatArrayOf(0.56f, 0.62f, 0.58f, 0.65f, 0.60f)
        for (i in positions.indices) {
            val x = w * positions[i]
            val stalkW = 3.5f
            val sway = (Math.sin((params.elapsedMs / 2400.0) + i) * 2.5 * params.intensity).toFloat()
            val top = h * tops[i]
            val bot = h * 0.84f
            drawBambooStalk(canvas, x, top, bot, stalkW, color, hi, lo, node, h * 0.06f, sway)
            // tiny leaf tuft at the top
            drawBambooLeafCluster(canvas, x + sway, top - 6f, h * 0.022f, p.leaf, p.leafBright, seed = 1000 + i, sway = sway)
        }

        // 6 leaf cluster patches scattered through mid grove canopy area
        val rng = PRNG(990)
        for (i in 0 until 6) {
            val side = if (rng.next() < 0.5f) rng.next() * 0.30f else 0.70f + rng.next() * 0.28f
            val x = w * side
            val y = h * (0.20f + rng.next() * 0.40f)
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i) * 3 * params.intensity).toFloat()
            val size = h * (0.028f + rng.next() * 0.022f)
            drawBambooLeafCluster(canvas, x + sway, y, size, p.leaf, p.leafBright, seed = 990 + i, sway = sway)
        }
    }

    private fun drawLantern(canvas: Canvas, params: SceneParams, p: BambooPalette) {
        val lx = w * 0.50f
        val ly = h * 0.66f
        // hanging cord
        paint.color = lerpColor(p.stalkNode, 0xFF000000.toInt(), 0.40f)
        paint.strokeWidth = 1.6f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(lx, ly - h * 0.22f, lx, ly - 24f, paint)
        paint.style = Paint.Style.FILL
        // top cap
        paint.color = 0xFF3a2a18.toInt()
        canvas.drawRect(lx - 18f, ly - 24f, lx + 18f, ly - 20f, paint)
        // body
        val bodyTop = ly - 20f
        val bodyBot = ly + 18f
        paint.shader = LinearGradient(
            lx - 16f, 0f, lx + 16f, 0f,
            intArrayOf(0xFFf6dfa6.toInt(), 0xFFe6c270.toInt(), 0xFFa07840.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(lx - 17f, bodyTop, lx + 17f, bodyBot, paint)
        paint.shader = null
        // ribs
        paint.color = withAlpha(0xFF6a4828.toInt(), 200)
        paint.strokeWidth = 1.3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(lx - 14f, bodyTop + 6f, lx + 14f, bodyTop + 6f, paint)
        canvas.drawLine(lx - 16f, (bodyTop + bodyBot) * 0.5f, lx + 16f, (bodyTop + bodyBot) * 0.5f, paint)
        canvas.drawLine(lx - 14f, bodyBot - 6f, lx + 14f, bodyBot - 6f, paint)
        paint.style = Paint.Style.FILL
        // bottom cap
        paint.color = 0xFF3a2a18.toInt()
        canvas.drawRect(lx - 18f, bodyBot, lx + 18f, bodyBot + 4f, paint)

        if (lanternGlowActive(params.timeOfDay)) {
            val flicker = (Math.sin(params.elapsedMs / 600.0) * 0.08 + 0.92).toFloat()
            drawSoftGlow(canvas, lx, (bodyTop + bodyBot) * 0.5f, 22f,
                withAlpha(p.lanternGlow, (220 * flicker).toInt().coerceIn(0, 255)),
                intensity = flicker)
        }
    }

    private fun drawGround(canvas: Canvas, params: SceneParams, p: BambooPalette, haze: Int) {
        drawGroundHaze(canvas, w, h * 0.80f, h * 0.92f, lerpColor(haze, p.groundDark, 0.55f), peakAlpha = 110)
        drawForegroundAnchor(canvas, w, h,
            topY = h * 0.92f,
            baseColor = lerpColor(p.groundDark, 0xFF000000.toInt(), 0.15f),
            seed = 6112,
        )
    }

    private fun drawSoftFog(canvas: Canvas, haze: Int, params: SceneParams) {
        val mist = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.40f)
        val baseAlpha = if (params.weather == WeatherType.FOG) 80 else 50
        val rng = PRNG(990)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val y = h * 0.55f + rng.next() * h * 0.30f
            val rx = 90f + rng.next() * 140f
            val ry = 16f + rng.next() * 22f
            val drift = (Math.sin(params.elapsedMs / 5000.0 + i * 0.9) * 22).toFloat()
            val pulse = (Math.sin(params.elapsedMs / 4000.0 + i * 1.4) * 12).toInt()
            drawSoftFogPuff(canvas, x + drift, y, rx, ry, mist, alpha = (baseAlpha + pulse).coerceIn(0, 255))
        }
    }
}
