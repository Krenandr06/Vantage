package com.vantage.scene

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Color as AColor

data class SkyAnchor(
    val t: Float,
    val top: Int,
    val mid: Int,
    val bot: Int,
    val starsOp: Float = 0f,
    val sunY: Float = -1f,
    val sunSize: Float = 0f,
    val moonY: Float = -1f,
    val moonSize: Float = 0f,
)

private val SKY_ANCHORS = listOf(
    SkyAnchor(0f, 0xFF0a0e1a.toInt(), 0xFF161a2a.toInt(), 0xFF1c2030.toInt(), starsOp = 0.95f, moonY = 0.25f, moonSize = 28f),
    SkyAnchor(4.5f, 0xFF101428.toInt(), 0xFF1a1e38.toInt(), 0xFF242840.toInt(), starsOp = 0.85f, moonY = 0.35f, moonSize = 26f),
    SkyAnchor(5.8f, 0xFF2a2842.toInt(), 0xFF4a3850.toInt(), 0xFF8a5a52.toInt(), starsOp = 0.25f, sunY = 0.96f, sunSize = 54f),
    SkyAnchor(6.5f, 0xFF5a4a6a.toInt(), 0xFF9a6a5a.toInt(), 0xFFd4916a.toInt(), starsOp = 0.05f, sunY = 0.88f, sunSize = 50f),
    SkyAnchor(7f, 0xFF7090a8.toInt(), 0xFF90a8b8.toInt(), 0xFFc8b898.toInt(), sunY = 0.78f, sunSize = 44f),
    SkyAnchor(8f, 0xFF88b4d0.toInt(), 0xFFa0c4d8.toInt(), 0xFFd0d4c0.toInt(), sunY = 0.62f, sunSize = 40f),
    SkyAnchor(10f, 0xFF78b8e8.toInt(), 0xFF90c8e8.toInt(), 0xFFc8dce0.toInt(), sunY = 0.38f, sunSize = 36f),
    SkyAnchor(12f, 0xFF68b0f0.toInt(), 0xFF88c4f0.toInt(), 0xFFb8d8e8.toInt(), sunY = 0.22f, sunSize = 34f),
    SkyAnchor(14f, 0xFF78b8e8.toInt(), 0xFF90c8e8.toInt(), 0xFFc8dce0.toInt(), sunY = 0.30f, sunSize = 36f),
    SkyAnchor(16f, 0xFF88b4d0.toInt(), 0xFFa0c4d8.toInt(), 0xFFd0d4c0.toInt(), sunY = 0.52f, sunSize = 40f),
    SkyAnchor(17.5f, 0xFF9a8060.toInt(), 0xFFc89050.toInt(), 0xFFe8a040.toInt(), sunY = 0.78f, sunSize = 48f),
    SkyAnchor(18.5f, 0xFF5a3050.toInt(), 0xFF8a4a48.toInt(), 0xFFc86838.toInt(), sunY = 0.92f, sunSize = 54f),
    SkyAnchor(19.5f, 0xFF2a1838.toInt(), 0xFF3a2848.toInt(), 0xFF5a3850.toInt(), starsOp = 0.15f),
    SkyAnchor(21f, 0xFF0a0e1a.toInt(), 0xFF161a2a.toInt(), 0xFF1c2030.toInt(), starsOp = 0.9f, moonY = 0.40f, moonSize = 26f),
)

data class SkyState(
    val topColor: Int,
    val midColor: Int,
    val botColor: Int,
    val starsOpacity: Float,
    val sunY: Float,
    val sunSize: Float,
    val moonY: Float,
    val moonSize: Float,
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

    return SkyState(
        topColor = lerpColor(lo.top, hi.top, frac),
        midColor = lerpColor(lo.mid, hi.mid, frac),
        botColor = lerpColor(lo.bot, hi.bot, frac),
        starsOpacity = lerp(lo.starsOp, hi.starsOp, frac),
        sunY = lerp(lo.sunY, hi.sunY, frac),
        sunSize = lerp(lo.sunSize, hi.sunSize, frac),
        moonY = lerp(lo.moonY, hi.moonY, frac),
        moonSize = lerp(lo.moonSize, hi.moonSize, frac),
    )
}

private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

fun drawSkyGradient(canvas: Canvas, w: Int, h: Int, sky: SkyState) {
    skyPaint.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        intArrayOf(sky.topColor, sky.midColor, sky.botColor),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
    skyPaint.shader = null
}

fun drawSun(canvas: Canvas, w: Int, h: Int, sky: SkyState) {
    if (sky.sunY < 0f || sky.sunSize <= 0f) return
    val cx = w * 0.5f
    val cy = h * sky.sunY
    val r = sky.sunSize
    skyPaint.color = 0xFFFFF4D0.toInt()
    skyPaint.alpha = 220
    canvas.drawCircle(cx, cy, r * 1.5f, skyPaint)
    skyPaint.alpha = 255
    canvas.drawCircle(cx, cy, r, skyPaint)
}

fun drawMoon(canvas: Canvas, w: Int, h: Int, sky: SkyState) {
    if (sky.moonY < 0f || sky.moonSize <= 0f) return
    val cx = w * 0.65f
    val cy = h * sky.moonY
    val r = sky.moonSize
    skyPaint.color = 0xFFE8E4D8.toInt()
    skyPaint.alpha = 200
    canvas.drawCircle(cx, cy, r, skyPaint)
}

fun drawStars(canvas: Canvas, w: Int, h: Int, opacity: Float, elapsedMs: Long) {
    if (opacity <= 0.02f) return
    val rng = PRNG(42)
    val count = 80
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
