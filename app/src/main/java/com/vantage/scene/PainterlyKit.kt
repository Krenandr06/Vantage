package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader

private val kitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
private val kitPath = Path()
private val kitPath2 = Path()

fun hazeColor(sky: SkyState): Int = lerpColor(sky.midColor, sky.horizonColor, 0.55f)

/** Warm rim color derived from the sky's golden-hour intensity. */
fun rimLightColor(sky: SkyState): Int {
    val warm = lerpColor(0xFFFFE0B4.toInt(), sky.horizonColor, 0.30f)
    val cool = lerpColor(0xFFB8C8DC.toInt(), sky.midColor, 0.40f)
    return lerpColor(cool, warm, sky.goldenHour.coerceIn(0f, 1f))
}

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

// ---------------------------------------------------------------------------
//  Ridge stack — declarative back-to-front mountain rendering with aerial
//  perspective and (optionally) golden-hour rim light on the lit side.
// ---------------------------------------------------------------------------

data class RidgeLayer(
    val seed: Int,
    val baseY: Float,
    val amplitude: Float,
    val color: Int,
    /** 0 = solid foreground, 1 = fully washed into haze. */
    val depth: Float,
    /** Strength of warm rim light along the ridge crest. 0 = none. */
    val rim: Float = 0f,
    val snow: Float = 0f,
    val segments: Int = 96,
)

fun drawRidgeStack(
    canvas: Canvas,
    w: Int,
    h: Int,
    layers: List<RidgeLayer>,
    haze: Int,
    sky: SkyState,
    sunCx: Float,
) {
    val rim = rimLightColor(sky)
    for (layer in layers) {
        smoothRidgePath(kitPath, w, h, layer.baseY, layer.amplitude, layer.seed, layer.segments)
        val top = layer.baseY - layer.amplitude * 1.2f
        val bot = layer.baseY + h * 0.04f
        drawAerialLayer(canvas, kitPath, top, bot, layer.color, haze, layer.depth)

        if (layer.rim > 0f) {
            drawRimOnRidge(canvas, w, h, layer, rim, sunCx)
        }
        if (layer.snow > 0f) {
            drawSnowDustOnRidge(canvas, w, h, layer)
        }
    }
}

/**
 * Strokes a warm 1–2px gradient stroke along the lit side of a ridge.
 * Approximated by re-tracing the ridge path with a thin stroke whose alpha falls off
 * away from the sun's x position.
 */
private fun drawRimOnRidge(
    canvas: Canvas,
    w: Int,
    h: Int,
    layer: RidgeLayer,
    rimCol: Int,
    sunCx: Float,
) {
    // Trace just the ridge line (no fill).
    val rng = PRNG(layer.seed)
    val a1 = 0.45f + rng.next() * 0.35f
    val a2 = 0.25f + rng.next() * 0.25f
    val a3 = 0.10f + rng.next() * 0.15f
    val f1 = 1.2f + rng.next() * 1.6f
    val f2 = 2.6f + rng.next() * 2.4f
    val f3 = 5.0f + rng.next() * 3.5f
    val p1 = rng.next() * 6.283f
    val p2 = rng.next() * 6.283f
    val p3 = rng.next() * 6.283f
    val seg = layer.segments
    val falloff = w * 0.55f

    kitPaint.style = Paint.Style.STROKE
    kitPaint.strokeCap = Paint.Cap.ROUND
    kitPaint.strokeWidth = 2.2f

    var prevX = 0f
    var prevY = layer.baseY
    for (i in 0..seg) {
        val t = i.toFloat() / seg
        val n = Math.sin((t * f1 + p1).toDouble()).toFloat() * a1 +
            Math.sin((t * f2 + p2).toDouble()).toFloat() * a2 +
            Math.sin((t * f3 + p3).toDouble()).toFloat() * a3
        val y = layer.baseY - n * layer.amplitude
        val x = t * w
        if (i > 0) {
            val midX = (prevX + x) * 0.5f
            val dist = Math.abs(midX - sunCx)
            val k = (1f - (dist / falloff).coerceIn(0f, 1f))
            val a = (k * k * layer.rim * 220f).toInt().coerceIn(0, 220)
            if (a > 4) {
                kitPaint.color = withAlpha(rimCol, a)
                canvas.drawLine(prevX, prevY, x, y, kitPaint)
            }
        }
        prevX = x
        prevY = y
    }
    kitPaint.style = Paint.Style.FILL
    kitPaint.strokeCap = Paint.Cap.BUTT
    kitPaint.alpha = 255
}

private fun drawSnowDustOnRidge(canvas: Canvas, w: Int, h: Int, layer: RidgeLayer) {
    // Stamp small irregular white blobs only at local maxima of the ridge.
    val rng = PRNG(layer.seed)
    val a1 = 0.45f + rng.next() * 0.35f
    val a2 = 0.25f + rng.next() * 0.25f
    val a3 = 0.10f + rng.next() * 0.15f
    val f1 = 1.2f + rng.next() * 1.6f
    val f2 = 2.6f + rng.next() * 2.4f
    val f3 = 5.0f + rng.next() * 3.5f
    val p1 = rng.next() * 6.283f
    val p2 = rng.next() * 6.283f
    val p3 = rng.next() * 6.283f
    val seg = 160
    val snowCol = withAlpha(0xFFFAF4E6.toInt(), (200 * layer.snow).toInt().coerceIn(0, 230))
    kitPaint.color = snowCol

    var prevSlope = 0f
    var prevN = -2f
    var prevY = layer.baseY
    var prevX = 0f
    for (i in 0..seg) {
        val t = i.toFloat() / seg
        val n = Math.sin((t * f1 + p1).toDouble()).toFloat() * a1 +
            Math.sin((t * f2 + p2).toDouble()).toFloat() * a2 +
            Math.sin((t * f3 + p3).toDouble()).toFloat() * a3
        val x = t * w
        val y = layer.baseY - n * layer.amplitude
        if (i > 0) {
            val slope = n - prevN
            // local maximum where slope flips positive→negative and peak is high enough
            if (prevSlope > 0f && slope < 0f && prevN > 0.35f) {
                val capW = layer.amplitude * (0.25f + prevN * 0.30f)
                val capH = layer.amplitude * 0.18f
                // base of the cap sits just below the peak
                kitPath2.reset()
                kitPath2.moveTo(prevX - capW, prevY + capH * 0.7f)
                kitPath2.lineTo(prevX - capW * 0.55f, prevY + capH * 0.2f)
                kitPath2.lineTo(prevX - capW * 0.25f, prevY + capH * 0.5f)
                kitPath2.lineTo(prevX, prevY + capH * 0.1f)
                kitPath2.lineTo(prevX + capW * 0.25f, prevY + capH * 0.4f)
                kitPath2.lineTo(prevX + capW * 0.55f, prevY + capH * 0.15f)
                kitPath2.lineTo(prevX + capW, prevY + capH * 0.7f)
                // hug the peak silhouette on top
                kitPath2.lineTo(prevX + capW * 0.5f, prevY + capH * 0.25f)
                kitPath2.lineTo(prevX, prevY + 2f)
                kitPath2.lineTo(prevX - capW * 0.5f, prevY + capH * 0.25f)
                kitPath2.close()
                canvas.drawPath(kitPath2, kitPaint)
            }
            prevSlope = slope
        }
        prevN = n
        prevX = x
        prevY = y
    }
}

// ---------------------------------------------------------------------------
//  Volumetric clouds & sea-of-clouds.
// ---------------------------------------------------------------------------

/**
 * A horizontal "sea of clouds" — fills a valley region with billowing fog that
 * occludes the feet of distant mountains. Drifts slowly.
 */
fun drawSeaOfClouds(
    canvas: Canvas,
    w: Int,
    h: Int,
    yTop: Float,
    yBot: Float,
    baseColor: Int,
    rimColor: Int,
    elapsedMs: Long,
    seed: Int = 1337,
    density: Float = 1f,
) {
    // Soft feathered base wash — translucent, top edge fades up, bottom fades into
    // its own color. NOT a fog wall.
    val span = yBot - yTop
    kitPaint.shader = LinearGradient(
        0f, yTop - span * 0.20f, 0f, yBot,
        intArrayOf(rimColor and 0x00FFFFFF, withAlpha(rimColor, 110), withAlpha(baseColor, 150)),
        floatArrayOf(0f, 0.40f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, yTop - span * 0.20f, w.toFloat(), yBot, kitPaint)
    kitPaint.shader = null

    val drift = (elapsedMs * 0.000020f) % 1f
    val rng = PRNG(seed)
    val count = (7 * density).toInt().coerceAtLeast(5)
    for (i in 0 until count) {
        val baseX = (rng.next() + drift * (0.4f + rng.next() * 0.6f)) % 1f
        val cx = baseX * w * 1.3f - w * 0.15f
        // bias puffs toward the top half of the band so the upper edge billows
        val cy = yTop + (0.10f + rng.next() * 0.45f) * span
        val rx = w * (0.16f + rng.next() * 0.20f)
        val ry = span * (0.30f + rng.next() * 0.30f)
        // body — translucent
        drawSoftFogPuff(canvas, cx, cy, rx, ry * 0.85f, rimColor, alpha = 90)
        // soft top highlight
        drawSoftFogPuff(canvas, cx, cy - ry * 0.20f, rx * 0.65f, ry * 0.35f,
            0xFFFFFFFF.toInt(), alpha = 70)
    }
}

/**
 * A large volumetric cloud bank with a rim-lit top. Use for dramatic dolomite/cozy
 * style skies. The rim catches the sun direction.
 */
fun drawVolumetricCloud(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    baseColor: Int,
    rimColor: Int,
    sunCx: Float,
    alpha: Int = 235,
) {
    // bottom shadow
    drawSoftFogPuff(canvas, cx, cy + ry * 0.35f, rx * 0.95f, ry * 0.55f,
        lerpColor(baseColor, 0xFF000000.toInt(), 0.20f), alpha = (alpha * 0.55f).toInt().coerceIn(0, 255))
    // body — overlapping puffs
    val puffsBody = floatArrayOf(
        -0.75f, 0.05f, 0.95f,
        -0.25f, -0.20f, 1.05f,
        0.20f, -0.25f, 1.10f,
        0.65f, -0.10f, 0.95f,
        1.05f, 0.15f, 0.75f,
        -1.05f, 0.20f, 0.70f,
        0.40f, 0.20f, 0.85f,
    )
    kitPaint.color = baseColor
    kitPaint.alpha = alpha
    var i = 0
    while (i < puffsBody.size) {
        canvas.drawCircle(cx + puffsBody[i] * rx, cy + puffsBody[i + 1] * ry, puffsBody[i + 2] * ry, kitPaint)
        i += 3
    }
    // rim — only on side facing the sun
    val sunDir = if (sunCx >= cx) 1f else -1f
    kitPaint.color = rimColor
    val rimAlpha = (alpha * 0.75f).toInt().coerceIn(0, 255)
    kitPaint.alpha = rimAlpha
    i = 0
    while (i < puffsBody.size) {
        val px = cx + puffsBody[i] * rx
        val py = cy + puffsBody[i + 1] * ry - puffsBody[i + 2] * ry * 0.22f
        val pr = puffsBody[i + 2] * ry * 0.70f
        // shift toward sun-facing side
        canvas.drawCircle(px + sunDir * pr * 0.18f, py, pr, kitPaint)
        i += 3
    }
    // top highlight
    kitPaint.color = withAlpha(0xFFFFFFFF.toInt(), (alpha * 0.40f).toInt().coerceIn(0, 255))
    canvas.drawCircle(cx + sunDir * rx * 0.20f, cy - ry * 0.50f, ry * 0.45f, kitPaint)
    kitPaint.alpha = 255
}

/** Thin horizontal wisp cloud — for high-altitude cirrus streaks. */
fun drawWispCloud(canvas: Canvas, cx: Float, cy: Float, halfW: Float, halfH: Float, color: Int, alpha: Int = 130) {
    kitPaint.shader = LinearGradient(
        cx - halfW, cy, cx + halfW, cy,
        intArrayOf(color and 0x00FFFFFF, withAlpha(color, alpha), color and 0x00FFFFFF),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawOval(cx - halfW, cy - halfH, cx + halfW, cy + halfH, kitPaint)
    kitPaint.shader = null
    kitPaint.alpha = 255
}

// ---------------------------------------------------------------------------
//  Rain streaks — diagonal, length & alpha jitter.
// ---------------------------------------------------------------------------

fun drawRainStreaks(
    canvas: Canvas,
    w: Int,
    h: Int,
    elapsedMs: Long,
    intensity: Float,
    angleDeg: Float = 78f,
    color: Int = 0xFFB8CCE0.toInt(),
    seed: Int = 7777,
) {
    if (intensity <= 0.01f) return
    val rad = Math.toRadians(angleDeg.toDouble())
    val dx = Math.cos(rad).toFloat()
    val dy = Math.sin(rad).toFloat()
    val rng = PRNG(seed)
    val count = (90 * intensity).toInt().coerceIn(40, 160)
    val cycle = 900f
    val baseSpeed = h * 1.2f
    kitPaint.style = Paint.Style.STROKE
    kitPaint.strokeCap = Paint.Cap.ROUND
    for (i in 0 until count) {
        val col = rng.next()
        val row = rng.next()
        val speedJit = 0.7f + rng.next() * 0.6f
        val len = (14f + rng.next() * 26f) * (0.7f + intensity * 0.6f)
        val alpha = (110 + rng.next() * 130f * intensity).toInt().coerceIn(40, 230)
        val width = 0.9f + rng.next() * 1.4f
        val t = ((elapsedMs / cycle) * speedJit + rng.next() * 4f) % 1f
        val travel = baseSpeed * 1.2f
        val sx = col * w * 1.3f - w * 0.15f + t * dx * travel
        val sy = -h * 0.1f + t * dy * travel + row * h * 0.3f
        kitPaint.color = withAlpha(color, alpha)
        kitPaint.strokeWidth = width
        canvas.drawLine(sx, sy, sx + dx * len, sy + dy * len, kitPaint)
    }
    kitPaint.style = Paint.Style.FILL
    kitPaint.strokeCap = Paint.Cap.BUTT
    kitPaint.alpha = 255
}

// ---------------------------------------------------------------------------
//  Foreground anchor — dark textured rocks/grass band.
// ---------------------------------------------------------------------------

fun drawForegroundAnchor(
    canvas: Canvas,
    w: Int,
    h: Int,
    topY: Float,
    baseColor: Int,
    seed: Int = 4242,
) {
    // base gradient — slightly darker toward the bottom edge for grounding weight
    val mid = lerpColor(baseColor, 0xFF000000.toInt(), 0.25f)
    val bot = lerpColor(baseColor, 0xFF000000.toInt(), 0.55f)
    kitPaint.shader = LinearGradient(
        0f, topY, 0f, h.toFloat(),
        intArrayOf(baseColor, mid, bot),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, topY, w.toFloat(), h.toFloat(), kitPaint)
    kitPaint.shader = null

    // jagged silhouette ridge on the top edge
    val rng = PRNG(seed)
    kitPath.reset()
    kitPath.moveTo(0f, h.toFloat())
    kitPath.lineTo(0f, topY + rng.next() * 12f)
    var x = 0f
    val step = w / 28f
    while (x < w) {
        val nx = x + step * (0.6f + rng.next() * 0.8f)
        val ny = topY + (rng.next() - 0.4f) * h * 0.025f
        kitPath.lineTo(nx, ny)
        x = nx
    }
    kitPath.lineTo(w.toFloat(), topY + rng.next() * 12f)
    kitPath.lineTo(w.toFloat(), h.toFloat())
    kitPath.close()
    kitPaint.color = lerpColor(baseColor, 0xFF000000.toInt(), 0.15f)
    canvas.drawPath(kitPath, kitPaint)

    // scattered pebbles / texture flecks
    for (i in 0 until 28) {
        val px = rng.next() * w
        val py = topY + rng.next() * (h - topY) * 0.85f
        val pr = 0.8f + rng.next() * 1.8f
        kitPaint.color = withAlpha(0xFF000000.toInt(), (60 + rng.next() * 80f).toInt())
        canvas.drawCircle(px, py, pr, kitPaint)
    }
    // a few brighter highlights
    for (i in 0 until 14) {
        val px = rng.next() * w
        val py = topY + rng.next() * (h - topY) * 0.6f
        kitPaint.color = withAlpha(lerpColor(baseColor, 0xFFFFFFFF.toInt(), 0.35f),
            (40 + rng.next() * 60f).toInt())
        canvas.drawCircle(px, py, 0.9f + rng.next() * 1.3f, kitPaint)
    }
    kitPaint.alpha = 255
}

// ---------------------------------------------------------------------------
//  Tall bamboo stalk — proper segmented cane with darker nodes.
// ---------------------------------------------------------------------------

fun drawBambooStalk(
    canvas: Canvas,
    cx: Float,
    topY: Float,
    bottomY: Float,
    width: Float,
    body: Int,
    highlight: Int,
    shadow: Int,
    nodeColor: Int,
    segmentH: Float,
    sway: Float = 0f,
) {
    val left = cx - width * 0.5f + sway
    val right = cx + width * 0.5f + sway
    // body with vertical lighting gradient (lit left, shadow right)
    kitPaint.shader = LinearGradient(
        left, 0f, right, 0f,
        intArrayOf(highlight, body, shadow),
        floatArrayOf(0f, 0.4f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(left, topY, right, bottomY, kitPaint)
    kitPaint.shader = null

    // segment nodes — subtle horizontal bands every segmentH. A thin dark line, no overhang.
    var ny = topY + segmentH
    val nodeH = (width * 0.16f).coerceAtLeast(1f)
    kitPaint.color = withAlpha(nodeColor, 200)
    while (ny < bottomY) {
        canvas.drawRect(left, ny - nodeH * 0.5f, right, ny + nodeH * 0.5f, kitPaint)
        ny += segmentH
    }
    kitPaint.alpha = 255
}

/** A cluster of small bamboo leaves at a given point. */
fun drawBambooLeafCluster(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    size: Float,
    body: Int,
    highlight: Int,
    seed: Int,
    sway: Float = 0f,
) {
    val rng = PRNG(seed)
    val leafCount = 5 + (rng.next() * 4f).toInt()
    for (i in 0 until leafCount) {
        val angle = (rng.next() - 0.5f) * 1.6f + sway * 0.05f
        val len = size * (0.7f + rng.next() * 0.6f)
        val wid = len * 0.22f
        val tipX = cx + Math.cos(angle.toDouble()).toFloat() * len
        val tipY = cy + Math.sin(angle.toDouble()).toFloat() * len
        kitPath.reset()
        kitPath.moveTo(cx, cy)
        val midX = (cx + tipX) * 0.5f
        val midY = (cy + tipY) * 0.5f
        val perpX = -Math.sin(angle.toDouble()).toFloat() * wid
        val perpY = Math.cos(angle.toDouble()).toFloat() * wid
        kitPath.quadTo(midX + perpX, midY + perpY, tipX, tipY)
        kitPath.quadTo(midX - perpX, midY - perpY, cx, cy)
        kitPath.close()
        kitPaint.color = if (rng.next() < 0.3f) highlight else body
        kitPaint.alpha = (200 + rng.next() * 55f).toInt().coerceIn(0, 255)
        canvas.drawPath(kitPath, kitPaint)
    }
    kitPaint.alpha = 255
}

// ---------------------------------------------------------------------------
//  Waterfall column — a coherent vertical sheet of water with sub-streaks.
// ---------------------------------------------------------------------------

fun drawWaterfallColumn(
    canvas: Canvas,
    cx: Float,
    halfWidth: Float,
    topY: Float,
    bottomY: Float,
    elapsedMs: Long,
    intensity: Float,
    seed: Int = 3030,
) {
    val height = bottomY - topY
    // base sheet — soft white with vertical falloff (brighter at the lip & toward the base where it churns)
    kitPaint.shader = LinearGradient(
        cx, topY, cx, bottomY,
        intArrayOf(withAlpha(0xFFFFFFFF.toInt(), 90), withAlpha(0xFFE2ECF2.toInt(), 220), withAlpha(0xFFCBDAE2.toInt(), 240)),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
    )
    kitPath.reset()
    // slight pinch in the middle so it reads like falling water
    kitPath.moveTo(cx - halfWidth, topY)
    kitPath.quadTo(cx - halfWidth * 0.85f, topY + height * 0.5f, cx - halfWidth * 1.05f, bottomY)
    kitPath.lineTo(cx + halfWidth * 1.05f, bottomY)
    kitPath.quadTo(cx + halfWidth * 0.85f, topY + height * 0.5f, cx + halfWidth, topY)
    kitPath.close()
    canvas.drawPath(kitPath, kitPaint)
    kitPaint.shader = null

    // animated sub-streaks
    val rng = PRNG(seed)
    kitPaint.style = Paint.Style.STROKE
    kitPaint.strokeCap = Paint.Cap.ROUND
    val streakCount = (20 + intensity * 26f).toInt()
    for (i in 0 until streakCount) {
        val col = rng.next()
        val sx = cx - halfWidth + col * halfWidth * 2f
        val speed = 360f + rng.next() * 240f
        val yOff = ((elapsedMs / 1000f * speed + rng.next() * height) % height)
        val len = 22f + rng.next() * 28f
        val strokeW = 1.1f + rng.next() * 1.6f
        val alpha = (140 + rng.next() * 90f).toInt().coerceIn(120, 230)
        // taper alpha away from center column to feather edges
        val edgeFalloff = 1f - Math.abs(col - 0.5f) * 1.6f
        val a = (alpha * edgeFalloff.coerceIn(0f, 1f)).toInt()
        if (a > 10) {
            kitPaint.color = withAlpha(0xFFFFFFFF.toInt(), a)
            kitPaint.strokeWidth = strokeW
            canvas.drawLine(sx, topY + yOff, sx, topY + yOff + len, kitPaint)
        }
    }
    kitPaint.style = Paint.Style.FILL
    kitPaint.strokeCap = Paint.Cap.BUTT
    kitPaint.alpha = 255
}

