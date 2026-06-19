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

/** Soft conifer silhouette with rounded shoulders + subtle highlight on the lit side. */
fun drawPaintedConifer(
    canvas: Canvas,
    cx: Float,
    baseY: Float,
    height: Float,
    body: Int,
    highlight: Int = lerpColor(body, 0xFFFFFFFF.toInt(), 0.20f),
    shadow: Int = lerpColor(body, 0xFF000000.toInt(), 0.30f),
) {
    val halfBase = height * 0.30f
    val tmp = Path()
    tmp.moveTo(cx, baseY - height)
    tmp.quadTo(cx + halfBase * 0.8f, baseY - height * 0.35f, cx + halfBase, baseY)
    tmp.lineTo(cx - halfBase, baseY)
    tmp.quadTo(cx - halfBase * 0.8f, baseY - height * 0.35f, cx, baseY - height)
    tmp.close()
    kitPaint.color = body
    canvas.drawPath(tmp, kitPaint)

    // lit side
    val lit = Path()
    lit.moveTo(cx, baseY - height)
    lit.quadTo(cx + halfBase * 0.45f, baseY - height * 0.5f, cx + halfBase * 0.55f, baseY - height * 0.05f)
    lit.lineTo(cx + halfBase * 0.05f, baseY - height * 0.05f)
    lit.close()
    kitPaint.color = highlight
    kitPaint.alpha = 110
    canvas.drawPath(lit, kitPaint)
    kitPaint.alpha = 255

    // shadow side
    val shd = Path()
    shd.moveTo(cx, baseY - height)
    shd.quadTo(cx - halfBase * 0.45f, baseY - height * 0.5f, cx - halfBase * 0.55f, baseY - height * 0.05f)
    shd.lineTo(cx - halfBase * 0.05f, baseY - height * 0.05f)
    shd.close()
    kitPaint.color = shadow
    kitPaint.alpha = 90
    canvas.drawPath(shd, kitPaint)
    kitPaint.alpha = 255
}

/** Soft deciduous canopy — overlapping circles with a top highlight. */
fun drawPaintedCanopy(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    body: Int,
    highlight: Int,
    alpha: Int = 235,
) {
    kitPaint.color = body
    kitPaint.alpha = alpha
    canvas.drawCircle(cx - radius * 0.55f, cy + radius * 0.1f, radius * 0.85f, kitPaint)
    canvas.drawCircle(cx + radius * 0.55f, cy + radius * 0.05f, radius * 0.90f, kitPaint)
    canvas.drawCircle(cx, cy - radius * 0.15f, radius * 1.00f, kitPaint)
    canvas.drawCircle(cx + radius * 0.25f, cy + radius * 0.30f, radius * 0.75f, kitPaint)
    canvas.drawCircle(cx - radius * 0.30f, cy + radius * 0.30f, radius * 0.70f, kitPaint)

    kitPaint.color = highlight
    kitPaint.alpha = (alpha * 0.55f).toInt().coerceIn(0, 255)
    canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.30f, radius * 0.50f, kitPaint)
    canvas.drawCircle(cx + radius * 0.05f, cy - radius * 0.45f, radius * 0.45f, kitPaint)
    kitPaint.alpha = 255
}

/**
 * Painterly water surface — gradient base + drifting horizontal highlights + soft sun glint column.
 * Use for lakes, ponds, pools, river surfaces.
 */
fun drawPaintedWater(
    canvas: Canvas,
    w: Int,
    top: Float,
    bot: Float,
    sky: SkyState,
    haze: Int,
    intensity: Float,
    elapsedMs: Long,
    glintCxFrac: Float = 0.5f,
    seed: Int = 555,
) {
    val baseTop = lerpColor(sky.midColor, 0xFF223347.toInt(), 0.55f)
    val baseBot = lerpColor(sky.botColor, 0xFF0c1623.toInt(), 0.55f)
    kitPaint.shader = LinearGradient(
        0f, top, 0f, bot,
        baseTop, baseBot, Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, top, w.toFloat(), bot, kitPaint)
    kitPaint.shader = null

    // soft reflection of the haze near the shore line
    val refl = lerpColor(haze, 0xFFFFFFFF.toInt(), 0.25f)
    kitPaint.shader = LinearGradient(
        0f, top, 0f, top + (bot - top) * 0.55f,
        intArrayOf(withAlpha(refl, 95), withAlpha(refl, 0)),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, top, w.toFloat(), bot, kitPaint)
    kitPaint.shader = null

    // ripples
    val rng = PRNG(seed)
    val rippleIntensity = 0.35f + intensity * 0.65f
    kitPaint.style = Paint.Style.STROKE
    kitPaint.strokeWidth = 1.2f
    for (i in 0 until 18) {
        val rx = rng.next() * w
        val ry = top + rng.next() * (bot - top)
        val phase = (elapsedMs / 2200.0 + rng.next() * 6.28).toFloat()
        val rl = (12f + (Math.sin(phase.toDouble()) * 9f).toFloat()) * rippleIntensity
        val a = ((38 + Math.sin(phase.toDouble()) * 28.0) * rippleIntensity).toInt().coerceIn(0, 255)
        kitPaint.color = withAlpha(0xFFFFFFFF.toInt(), a)
        canvas.drawLine(rx - rl, ry, rx + rl, ry, kitPaint)
    }
    kitPaint.style = Paint.Style.FILL
    kitPaint.alpha = 255

    // sun-glint column
    if (sky.sunY in 0f..1f && sky.sunSize > 0f) {
        val cx = w * glintCxFrac
        val glintCol = lerpColor(0xFFFFF1C8.toInt(), sky.botColor, 0.25f)
        for (i in 0 until 14) {
            val phase = (elapsedMs / 700.0 + i * 1.3).toFloat()
            val flick = ((Math.sin(phase.toDouble()) * 0.5 + 0.5)).toFloat()
            val gy = top + (i + 1) * (bot - top) / 16f + (Math.sin(phase.toDouble()) * 2f).toFloat()
            val gw = 6f + flick * (bot - top) * 0.04f
            kitPaint.color = withAlpha(glintCol, (40 + flick * 90).toInt().coerceIn(0, 255))
            canvas.drawOval(cx - gw, gy - 1.5f, cx + gw, gy + 1.5f, kitPaint)
        }
        kitPaint.alpha = 255
    }
}

/** Soft god-ray light shaft. */
fun drawLightShaft(
    canvas: Canvas,
    cx: Float,
    topY: Float,
    botY: Float,
    halfWidth: Float,
    color: Int,
    topAlpha: Int = 80,
) {
    val transparent = color and 0x00FFFFFF
    kitPaint.shader = LinearGradient(
        cx - halfWidth, 0f, cx + halfWidth, 0f,
        intArrayOf(transparent, withAlpha(color, topAlpha), transparent),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    val path = Path()
    path.moveTo(cx - halfWidth * 0.4f, topY)
    path.lineTo(cx + halfWidth * 0.4f, topY)
    path.lineTo(cx + halfWidth, botY)
    path.lineTo(cx - halfWidth, botY)
    path.close()
    canvas.drawPath(path, kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

/** Soft volumetric fog blob — bigger and softer than the WeatherOverlay's. */
fun drawSoftFogPuff(canvas: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, color: Int, alpha: Int) {
    kitPaint.shader = RadialGradient(
        cx, cy, rx,
        intArrayOf(withAlpha(color, alpha), color and 0x00FFFFFF),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

/** Painterly firefly / glow particle with halo. */
fun drawGlowParticle(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
    val transparent = color and 0x00FFFFFF
    kitPaint.shader = RadialGradient(
        cx, cy, r * 3f,
        withAlpha(color, (alpha * 0.5f).toInt().coerceIn(0, 255)), transparent, Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(cx, cy, r * 3f, kitPaint)
    kitPaint.shader = null
    kitPaint.color = withAlpha(color, alpha)
    canvas.drawCircle(cx, cy, r, kitPaint)
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
