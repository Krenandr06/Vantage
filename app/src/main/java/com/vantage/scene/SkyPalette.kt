package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Color as AColor

data class SkyAnchor(
    val t: Float,
    val top: Int,
    val upper: Int,
    val mid: Int,
    val horizon: Int,
    val bot: Int,
    val starsOp: Float = 0f,
    val sunY: Float = -1f,
    val sunSize: Float = 0f,
    val moonY: Float = -1f,
    val moonSize: Float = 0f,
    val golden: Float = 0f,
)

private val SKY_ANCHORS = listOf(
    // pre-dawn
    SkyAnchor(0f,    0xFF080B17.toInt(), 0xFF0c1124.toInt(), 0xFF141a30.toInt(), 0xFF1c2236.toInt(), 0xFF222840.toInt(), starsOp = 0.95f, moonY = 0.25f, moonSize = 44f),
    SkyAnchor(4.5f,  0xFF0e1228.toInt(), 0xFF161c38.toInt(), 0xFF22264a.toInt(), 0xFF2e3258.toInt(), 0xFF38406a.toInt(), starsOp = 0.85f, moonY = 0.35f, moonSize = 42f),
    // first light — warm horizon band emerges
    SkyAnchor(5.6f,  0xFF1a1c3a.toInt(), 0xFF35334e.toInt(), 0xFF5a3e5e.toInt(), 0xFF9c5c52.toInt(), 0xFFc47a52.toInt(), starsOp = 0.40f, sunY = 0.98f, sunSize = 90f, golden = 0.55f),
    // sunrise — peak golden
    SkyAnchor(6.4f,  0xFF3a3a64.toInt(), 0xFF6a587a.toInt(), 0xFFa6786e.toInt(), 0xFFe89a6a.toInt(), 0xFFf8c486.toInt(), starsOp = 0.10f, sunY = 0.88f, sunSize = 82f, golden = 1.0f),
    // morning haze
    SkyAnchor(7.2f,  0xFF6a8aae.toInt(), 0xFF8aa6c2.toInt(), 0xFFb4c6d4.toInt(), 0xFFd8cab4.toInt(), 0xFFe6d4be.toInt(), sunY = 0.78f, sunSize = 72f, golden = 0.55f),
    SkyAnchor(8.5f,  0xFF7eaad0.toInt(), 0xFF9ac0dc.toInt(), 0xFFbcd6e2.toInt(), 0xFFd8dccc.toInt(), 0xFFe2dec8.toInt(), sunY = 0.58f, sunSize = 62f, golden = 0.20f),
    SkyAnchor(11f,   0xFF5aa4ea.toInt(), 0xFF7ab8ec.toInt(), 0xFF98c8ea.toInt(), 0xFFb4d6e0.toInt(), 0xFFc6e0e0.toInt(), sunY = 0.28f, sunSize = 56f, golden = 0.05f),
    SkyAnchor(13f,   0xFF4e9eec.toInt(), 0xFF74b6ec.toInt(), 0xFF94c6ea.toInt(), 0xFFb0d2e0.toInt(), 0xFFbcdce4.toInt(), sunY = 0.22f, sunSize = 54f, golden = 0.05f),
    SkyAnchor(15f,   0xFF5aa4ea.toInt(), 0xFF7ab8ec.toInt(), 0xFF98c8ea.toInt(), 0xFFb4d6e0.toInt(), 0xFFc6e0e0.toInt(), sunY = 0.32f, sunSize = 56f, golden = 0.10f),
    SkyAnchor(16.5f, 0xFF7eaad0.toInt(), 0xFF9ac0dc.toInt(), 0xFFbcd6e2.toInt(), 0xFFd8d0bc.toInt(), 0xFFe6cea4.toInt(), sunY = 0.54f, sunSize = 64f, golden = 0.40f),
    // golden hour evening
    SkyAnchor(17.6f, 0xFF8a7aae.toInt(), 0xFFa680a0.toInt(), 0xFFc88072.toInt(), 0xFFe89060.toInt(), 0xFFf6b066.toInt(), sunY = 0.74f, sunSize = 76f, golden = 0.90f),
    // sunset — deep
    SkyAnchor(18.4f, 0xFF4a3068.toInt(), 0xFF7c3a64.toInt(), 0xFFb44a44.toInt(), 0xFFe06038.toInt(), 0xFFf07a30.toInt(), sunY = 0.90f, sunSize = 88f, golden = 1.0f),
    // dusk
    SkyAnchor(19.4f, 0xFF1c1838.toInt(), 0xFF2e2244.toInt(), 0xFF4c2c4c.toInt(), 0xFF6c3848.toInt(), 0xFF8a4438.toInt(), starsOp = 0.15f, golden = 0.55f),
    // twilight
    SkyAnchor(20.4f, 0xFF0e1226.toInt(), 0xFF161a32.toInt(), 0xFF22243e.toInt(), 0xFF2c2e48.toInt(), 0xFF383a54.toInt(), starsOp = 0.70f, moonY = 0.30f, moonSize = 44f),
    SkyAnchor(22f,   0xFF080b17.toInt(), 0xFF0c1124.toInt(), 0xFF141a30.toInt(), 0xFF1c2236.toInt(), 0xFF222840.toInt(), starsOp = 0.92f, moonY = 0.40f, moonSize = 42f),
)

data class SkyState(
    val topColor: Int,
    val upperColor: Int,
    val midColor: Int,
    val horizonColor: Int,
    val botColor: Int,
    val starsOpacity: Float,
    val sunY: Float,
    val sunSize: Float,
    val moonY: Float,
    val moonSize: Float,
    val goldenHour: Float,
)

fun interpolateSky(time: Float): SkyState {
    val t = time % 24f
    var lo = SKY_ANCHORS.last()
    var hi = SKY_ANCHORS.first()
    for (i in 0 until SKY_ANCHORS.size - 1) {
        if (t >= SKY_ANCHORS[i].t && t < SKY_ANCHORS[i + 1].t) {
            lo = SKY_ANCHORS[i]
            hi = SKY_ANCHORS[i + 1]
            break
        }
    }
    if (t >= SKY_ANCHORS.last().t) {
        lo = SKY_ANCHORS.last()
        hi = SKY_ANCHORS.first()
    }

    val range = if (hi.t > lo.t) hi.t - lo.t else (24f - lo.t + hi.t)
    val frac = if (range > 0f) {
        val d = if (t >= lo.t) t - lo.t else t + 24f - lo.t
        (d / range).coerceIn(0f, 1f)
    } else 0f
    // ease the interpolation a touch so transitions feel painted, not linear
    val f = smoothstep(frac)

    return SkyState(
        topColor = lerpColor(lo.top, hi.top, f),
        upperColor = lerpColor(lo.upper, hi.upper, f),
        midColor = lerpColor(lo.mid, hi.mid, f),
        horizonColor = lerpColor(lo.horizon, hi.horizon, f),
        botColor = lerpColor(lo.bot, hi.bot, f),
        starsOpacity = lerp(lo.starsOp, hi.starsOp, f),
        sunY = lerp(lo.sunY, hi.sunY, f),
        sunSize = lerp(lo.sunSize, hi.sunSize, f),
        moonY = lerp(lo.moonY, hi.moonY, f),
        moonSize = lerp(lo.moonSize, hi.moonSize, f),
        goldenHour = lerp(lo.golden, hi.golden, f),
    )
}

private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

/**
 * 5-stop sky gradient. Stops are tuned so the warm horizon band reads at a
 * realistic ~80–95% down the screen instead of bleeding halfway up.
 */
fun drawSkyGradient(canvas: Canvas, w: Int, h: Int, sky: SkyState) {
    skyPaint.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        intArrayOf(sky.topColor, sky.upperColor, sky.midColor, sky.horizonColor, sky.botColor),
        floatArrayOf(0f, 0.35f, 0.62f, 0.84f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
    skyPaint.shader = null
}

fun drawSun(canvas: Canvas, w: Int, h: Int, sky: SkyState, cxFrac: Float = 0.5f, sizeScale: Float = 1f) {
    if (sky.sunY < 0f || sky.sunSize <= 0f) return
    val cx = w * cxFrac
    val cy = h * sky.sunY
    val r = sky.sunSize * sizeScale
    val warm = lerpColor(0xFFFFE5B0.toInt(), sky.horizonColor, 0.25f)
    drawSoftGlow(canvas, cx, cy, r, withAlpha(warm, 220), intensity = 1f + sky.goldenHour * 0.4f)
    skyPaint.shader = RadialGradient(
        cx, cy, r,
        intArrayOf(0xFFFFF6DE.toInt(), withAlpha(warm, 255)),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(cx, cy, r, skyPaint)
    skyPaint.shader = null
    skyPaint.alpha = 255
}

fun drawMoon(canvas: Canvas, w: Int, h: Int, sky: SkyState, cxFrac: Float = 0.65f, sizeScale: Float = 1f) {
    if (sky.moonY < 0f || sky.moonSize <= 0f) return
    val cx = w * cxFrac
    val cy = h * sky.moonY
    val r = sky.moonSize * sizeScale
    drawSoftGlow(canvas, cx, cy, r, withAlpha(0xFFD8DCEC.toInt(), 180), intensity = 0.8f)
    skyPaint.shader = RadialGradient(
        cx, cy, r,
        intArrayOf(0xFFF2EFE2.toInt(), 0xFFB8BCC8.toInt()),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(cx, cy, r, skyPaint)
    skyPaint.shader = null

    // subtle mare shading
    skyPaint.color = withAlpha(0xFF8A8E9A.toInt(), 55)
    canvas.drawCircle(cx - r * 0.22f, cy - r * 0.10f, r * 0.18f, skyPaint)
    canvas.drawCircle(cx + r * 0.18f, cy + r * 0.15f, r * 0.13f, skyPaint)
    skyPaint.alpha = 255
}

fun drawStars(canvas: Canvas, w: Int, h: Int, opacity: Float, elapsedMs: Long) {
    if (opacity <= 0.02f) return
    val rng = PRNG(42)
    val count = 90
    skyPaint.color = AColor.WHITE
    for (i in 0 until count) {
        val x = rng.next() * w
        val y = rng.next() * h * 0.7f
        val size = 0.5f + rng.next() * 1.5f
        val twinkle = (Math.sin((elapsedMs / 1000.0 + rng.next() * 10.0) * (0.5 + rng.next() * 2.0)) * 0.3 + 0.7).toFloat()
        skyPaint.alpha = (opacity * twinkle * 255).toInt().coerceIn(0, 255)
        canvas.drawCircle(x, y, size, skyPaint)
    }
    skyPaint.alpha = 255
}

fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

fun lerpColor(c1: Int, c2: Int, f: Float): Int {
    val a = lerp(AColor.alpha(c1).toFloat(), AColor.alpha(c2).toFloat(), f).toInt()
    val r = lerp(AColor.red(c1).toFloat(), AColor.red(c2).toFloat(), f).toInt()
    val g = lerp(AColor.green(c1).toFloat(), AColor.green(c2).toFloat(), f).toInt()
    val b = lerp(AColor.blue(c1).toFloat(), AColor.blue(c2).toFloat(), f).toInt()
    return AColor.argb(a, r, g, b)
}

class PRNG(seed: Int) {
    private var state = seed
    fun next(): Float {
        state = state * 1664525 + 1013904223
        return ((state ushr 8) and 0xFFFFFF) / 16777216f
    }
}
