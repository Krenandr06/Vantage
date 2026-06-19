package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

private val kitPaint = Paint(Paint.ANTI_ALIAS_FLAG)

fun hazeColor(sky: SkyState): Int = lerpColor(sky.midColor, sky.botColor, 0.55f)

fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

fun fadeColor(color: Int, factor: Float): Int =
    withAlpha(color, (AColor.alpha(color) * factor).toInt().coerceIn(0, 255))

fun drawAerialLayer(
    canvas: Canvas,
    path: Path,
    topY: Float,
    bottomY: Float,
    baseColor: Int,
    haze: Int,
    depth: Float,
) {
    val d = depth.coerceIn(0f, 1f)
    val topBlend = lerpColor(baseColor, haze, (0.55f + d * 0.45f).coerceIn(0f, 1f))
    val botBlend = lerpColor(baseColor, haze, (d * 0.35f).coerceIn(0f, 1f))
    kitPaint.shader = LinearGradient(
        0f, topY, 0f, bottomY,
        topBlend, botBlend, Shader.TileMode.CLAMP,
    )
    canvas.drawPath(path, kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

fun drawSoftGlow(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, intensity: Float = 1f) {
    val transparent = color and 0x00FFFFFF
    val baseA = AColor.alpha(color).toFloat()

    val outerR = radius * 3.2f
    val outerA = (baseA * 0.30f * intensity).toInt().coerceIn(0, 255)
    kitPaint.shader = RadialGradient(
        cx, cy, outerR,
        withAlpha(color, outerA), transparent, Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(cx, cy, outerR, kitPaint)

    val innerR = radius * 1.55f
    val innerA = (baseA * 0.85f * intensity).toInt().coerceIn(0, 255)
    kitPaint.shader = RadialGradient(
        cx, cy, innerR,
        withAlpha(color, innerA), transparent, Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(cx, cy, innerR, kitPaint)

    kitPaint.shader = null
    kitPaint.alpha = 255
}

private val puffs = floatArrayOf(
    -1.5f, 0.05f, 0.85f,
    -0.75f, -0.30f, 1.05f,
    0.05f, -0.40f, 1.15f,
    0.85f, -0.20f, 1.00f,
    1.55f, 0.10f, 0.80f,
    -0.20f, 0.20f, 0.90f,
    0.65f, 0.25f, 0.85f,
)

fun drawFluffyCloud(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    scale: Float,
    baseColor: Int,
    rimColor: Int,
    alpha: Int = 220,
) {
    kitPaint.color = baseColor
    kitPaint.alpha = alpha
    var i = 0
    while (i < puffs.size) {
        canvas.drawCircle(cx + puffs[i] * scale, cy + puffs[i + 1] * scale, puffs[i + 2] * scale, kitPaint)
        i += 3
    }
    kitPaint.color = rimColor
    kitPaint.alpha = (alpha * 0.55f).toInt().coerceIn(0, 255)
    i = 0
    while (i < puffs.size) {
        val rx = cx + puffs[i] * scale
        val ry = cy + puffs[i + 1] * scale - puffs[i + 2] * scale * 0.22f
        canvas.drawCircle(rx, ry, puffs[i + 2] * scale * 0.75f, kitPaint)
        i += 3
    }
    kitPaint.alpha = 255
}

fun drawHazeBand(
    canvas: Canvas,
    w: Int,
    yTop: Float,
    yBot: Float,
    color: Int,
    peakAlpha: Int,
) {
    val transparent = color and 0x00FFFFFF
    val opaque = withAlpha(color, peakAlpha)
    kitPaint.shader = LinearGradient(
        0f, yTop, 0f, yBot,
        intArrayOf(transparent, opaque, transparent),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, yTop, w.toFloat(), yBot, kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

fun drawVignette(canvas: Canvas, w: Int, h: Int, color: Int, strength: Float = 0.55f) {
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = Math.hypot(w.toDouble(), h.toDouble()).toFloat() * 0.7f
    val transparent = color and 0x00FFFFFF
    val edge = withAlpha(color, (AColor.alpha(color) * strength).toInt().coerceIn(0, 255))
    kitPaint.shader = RadialGradient(
        cx, cy, r,
        intArrayOf(transparent, transparent, edge),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

/**
 * Smooth ridge silhouette built from a sum of low-frequency sines. The result
 * curves naturally (unlike the original jagged peaks) and stays deterministic per seed.
 */
fun smoothRidgePath(
    out: Path,
    w: Int,
    h: Int,
    baseY: Float,
    amplitude: Float,
    seed: Int,
    segments: Int = 96,
) {
    out.reset()
    val rng = PRNG(seed)
    val a1 = 0.45f + rng.next() * 0.35f
    val a2 = 0.25f + rng.next() * 0.25f
    val a3 = 0.10f + rng.next() * 0.15f
    val f1 = 1.2f + rng.next() * 1.6f
    val f2 = 2.6f + rng.next() * 2.4f
    val f3 = 5.0f + rng.next() * 3.5f
    val p1 = rng.next() * 6.283f
    val p2 = rng.next() * 6.283f
    val p3 = rng.next() * 6.283f

    out.moveTo(0f, h.toFloat())
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val n = Math.sin((t * f1 + p1).toDouble()).toFloat() * a1 +
            Math.sin((t * f2 + p2).toDouble()).toFloat() * a2 +
            Math.sin((t * f3 + p3).toDouble()).toFloat() * a3
        val y = baseY - n * amplitude
        out.lineTo(t * w, y)
    }
    out.lineTo(w.toFloat(), h.toFloat())
    out.close()
}

/**
 * A single tall conical peak (Fuji-style) centered at cx with rounded shoulders.
 * Returns the actual peak y (apex).
 */
fun coneRidgePath(out: Path, w: Int, h: Int, cx: Float, peakY: Float, baseY: Float, halfBase: Float): Float {
    out.reset()
    out.moveTo(0f, h.toFloat())
    out.lineTo(0f, baseY)
    // gentle slope from horizon up to the peak
    val leftFoot = cx - halfBase
    val rightFoot = cx + halfBase
    // Use cubic curves for soft Fuji slopes
    out.lineTo(leftFoot - halfBase * 0.5f, baseY * 0.98f)
    out.cubicTo(
        leftFoot + halfBase * 0.2f, baseY * 0.85f,
        cx - halfBase * 0.25f, peakY + (baseY - peakY) * 0.18f,
        cx, peakY,
    )
    out.cubicTo(
        cx + halfBase * 0.25f, peakY + (baseY - peakY) * 0.18f,
        rightFoot - halfBase * 0.2f, baseY * 0.85f,
        rightFoot + halfBase * 0.5f, baseY * 0.98f,
    )
    out.lineTo(w.toFloat(), baseY)
    out.lineTo(w.toFloat(), h.toFloat())
    out.close()
    return peakY
}

fun drawSnowCap(
    canvas: Canvas,
    cx: Float,
    peakY: Float,
    baseY: Float,
    halfBase: Float,
    capColor: Int,
    alpha: Int = 220,
) {
    val tmp = Path()
    val capBottom = peakY + (baseY - peakY) * 0.22f
    val capHalf = halfBase * 0.18f
    tmp.moveTo(cx - capHalf, capBottom)
    // jagged underside
    tmp.lineTo(cx - capHalf * 0.6f, capBottom - capHalf * 0.25f)
    tmp.lineTo(cx - capHalf * 0.3f, capBottom + capHalf * 0.15f)
    tmp.lineTo(cx, capBottom - capHalf * 0.20f)
    tmp.lineTo(cx + capHalf * 0.35f, capBottom + capHalf * 0.10f)
    tmp.lineTo(cx + capHalf * 0.7f, capBottom - capHalf * 0.18f)
    tmp.lineTo(cx + capHalf, capBottom)
    // up to peak
    tmp.cubicTo(
        cx + capHalf * 0.55f, peakY + (capBottom - peakY) * 0.3f,
        cx + capHalf * 0.20f, peakY + (capBottom - peakY) * 0.10f,
        cx, peakY,
    )
    tmp.cubicTo(
        cx - capHalf * 0.20f, peakY + (capBottom - peakY) * 0.10f,
        cx - capHalf * 0.55f, peakY + (capBottom - peakY) * 0.3f,
        cx - capHalf, capBottom,
    )
    tmp.close()
    kitPaint.color = capColor
    kitPaint.alpha = alpha
    canvas.drawPath(tmp, kitPaint)
    kitPaint.alpha = 255
}

fun drawSparkles(
    canvas: Canvas,
    w: Int,
    h: Int,
    count: Int,
    seed: Int,
    color: Int,
    elapsedMs: Long,
    intensity: Float = 1f,
    yLimit: Float = 0.85f,
) {
    val rng = PRNG(seed)
    kitPaint.color = color
    for (i in 0 until count) {
        val sx = rng.next()
        val sy = rng.next()
        val sr = 0.6f + rng.next() * 1.6f
        val phase = rng.next() * 6.283f
        val drift = (Math.sin(elapsedMs / 2800.0 + phase) * 14f).toFloat()
        val bob = (Math.sin(elapsedMs / 4200.0 + phase * 1.7) * 9f).toFloat()
        val flicker = ((Math.sin(elapsedMs / 650.0 + phase * 3.0) * 0.4 + 0.6) * intensity).toFloat()
        var px = sx * w + drift
        if (px < 0f) px += w
        if (px > w) px -= w
        val py = sy * h * yLimit + bob
        kitPaint.alpha = (flicker * AColor.alpha(color)).toInt().coerceIn(0, 255)
        canvas.drawCircle(px, py, sr, kitPaint)
    }
    kitPaint.alpha = 255
}

/**
 * Draws a horizontal hazy ground line — used to bed mountain feet into atmosphere
 * so silhouettes don't look like cardboard cutouts.
 */
fun drawGroundHaze(canvas: Canvas, w: Int, yTop: Float, yBot: Float, hazeCol: Int, peakAlpha: Int) {
    val opaque = withAlpha(hazeCol, peakAlpha)
    val transparent = hazeCol and 0x00FFFFFF
    kitPaint.shader = LinearGradient(
        0f, yTop, 0f, yBot,
        intArrayOf(transparent, opaque),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, yTop, w.toFloat(), yBot, kitPaint)
    kitPaint.shader = null
}
