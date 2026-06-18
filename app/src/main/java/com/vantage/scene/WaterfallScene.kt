package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class WaterfallScene : VantageScene {

    private val cliffPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cascadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val poolPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sprayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fernPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky)

        drawTopTrees(canvas, params)
        drawCliffBack(canvas)
        drawUpperCliff(canvas)
        drawMoss(canvas, params)
        drawCascades(canvas, params)
        drawMidPool(canvas, sky, params)
        drawLowerRocks(canvas)
        drawLowerPool(canvas, sky, params)
        drawSpray(canvas, params)
        drawForeground(canvas, params)
    }

    private fun drawTopTrees(canvas: Canvas, params: SceneParams) {
        // Canopy colors change by season per design spec
        val canopyBright: Int
        val canopyMid: Int
        val canopyDark: Int
        when (params.season) {
            Season.SPRING -> { canopyBright = 0xFF9BC06E.toInt(); canopyMid = 0xFF5A8A3A.toInt(); canopyDark = 0xFF2A4A1A.toInt() }
            Season.SUMMER -> { canopyBright = 0xFF88A648.toInt(); canopyMid = 0xFF436A26.toInt(); canopyDark = 0xFF1F3A14.toInt() }
            Season.AUTUMN -> { canopyBright = 0xFFD9A058.toInt(); canopyMid = 0xFFA05A24.toInt(); canopyDark = 0xFF4A2810.toInt() }
            Season.WINTER -> { canopyBright = 0xFFA8A89A.toInt(); canopyMid = 0xFF5A5A52.toInt(); canopyDark = 0xFF2A2A26.toInt() }
        }
        val rng = PRNG(100)
        for (i in 0 until 10) {
            val x = rng.next() * w
            val r = 15f + rng.next() * 25f
            val y = h * 0.08f + rng.next() * h * 0.06f
            // Layer: dark base, mid body, bright highlight
            treePaint.color = canopyDark
            canvas.drawCircle(x, y + 2f, r, treePaint)
            treePaint.color = canopyMid
            canvas.drawCircle(x, y, r * 0.9f, treePaint)
            treePaint.color = canopyBright
            canvas.drawCircle(x - r * 0.15f, y - r * 0.15f, r * 0.5f, treePaint)
        }
    }

    private fun drawCliffBack(canvas: Canvas) {
        cliffPaint.color = 0xFF5A5A50.toInt()
        path.reset()
        path.moveTo(0f, h * 0.12f)
        path.lineTo(w * 0.35f, h * 0.1f)
        path.lineTo(w * 0.4f, h * 0.5f)
        path.lineTo(w * 0.6f, h * 0.5f)
        path.lineTo(w * 0.65f, h * 0.1f)
        path.lineTo(w.toFloat(), h * 0.12f)
        path.lineTo(w.toFloat(), h * 0.55f)
        path.lineTo(0f, h * 0.55f)
        path.close()
        canvas.drawPath(path, cliffPaint)
    }

    private fun drawUpperCliff(canvas: Canvas) {
        cliffPaint.color = 0xFF6A6A60.toInt()
        // Left ledge
        canvas.drawRect(0f, h * 0.14f, w * 0.38f, h * 0.2f, cliffPaint)
        // Right ledge
        canvas.drawRect(w * 0.62f, h * 0.14f, w.toFloat(), h * 0.2f, cliffPaint)
        // Mid ledge
        cliffPaint.color = 0xFF5A5848.toInt()
        canvas.drawRect(w * 0.3f, h * 0.38f, w * 0.7f, h * 0.42f, cliffPaint)
    }

    private fun drawMoss(canvas: Canvas, params: SceneParams) {
        // Vibrant moss colors per design spec
        val mossBright = 0xFF8AA84A.toInt()
        val mossMid = 0xFF5A7E30.toInt()
        val mossDeep = 0xFF3A5A1E.toInt()
        val rng = PRNG(200)
        // Left side moss
        for (i in 0 until 8) {
            val x = w * 0.3f + rng.next() * w * 0.08f
            val y = h * 0.15f + rng.next() * h * 0.35f
            mossPaint.color = mossDeep
            canvas.drawOval(x - 7f, y - 3.5f, x + 7f, y + 3.5f, mossPaint)
            mossPaint.color = mossMid
            canvas.drawOval(x - 6f, y - 3f, x + 6f, y + 3f, mossPaint)
            mossPaint.color = mossBright
            canvas.drawOval(x - 3f, y - 2f, x + 3f, y + 1f, mossPaint)
            // Drippy tendrils
            mossPaint.color = mossMid
            mossPaint.strokeWidth = 1.5f
            mossPaint.style = Paint.Style.STROKE
            val tendrilLen = 8f + rng.next() * 12f
            canvas.drawLine(x, y + 3f, x + (rng.next() - 0.5f) * 4f, y + 3f + tendrilLen, mossPaint)
            mossPaint.style = Paint.Style.FILL
        }
        // Right side moss
        for (i in 0 until 8) {
            val x = w * 0.62f + rng.next() * w * 0.08f
            val y = h * 0.15f + rng.next() * h * 0.35f
            mossPaint.color = mossDeep
            canvas.drawOval(x - 7f, y - 3.5f, x + 7f, y + 3.5f, mossPaint)
            mossPaint.color = mossMid
            canvas.drawOval(x - 6f, y - 3f, x + 6f, y + 3f, mossPaint)
            mossPaint.color = mossBright
            canvas.drawOval(x - 3f, y - 2f, x + 3f, y + 1f, mossPaint)
            mossPaint.color = mossMid
            mossPaint.strokeWidth = 1.5f
            mossPaint.style = Paint.Style.STROKE
            val tendrilLen = 8f + rng.next() * 12f
            canvas.drawLine(x, y + 3f, x + (rng.next() - 0.5f) * 4f, y + 3f + tendrilLen, mossPaint)
            mossPaint.style = Paint.Style.FILL
        }
    }

    private fun drawCascades(canvas: Canvas, params: SceneParams) {
        cascadePaint.color = 0xFFE8F0F8.toInt()
        val rng = PRNG(300)

        // Main cascade (center)
        val cx = w * 0.5f
        for (i in 0 until 12) {
            val streakX = cx + (rng.next() - 0.5f) * w * 0.12f
            val streakY = h * 0.18f + rng.next() * h * 0.02f
            val speed = 200f + rng.next() * 150f
            val yOff = ((params.elapsedMs / 1000f * speed + rng.next() * h * 0.3f) % (h * 0.35f))
            val streakLen = 15f + rng.next() * 20f
            cascadePaint.alpha = (150 + rng.next() * 105).toInt().coerceIn(0, 255)
            cascadePaint.strokeWidth = 1.5f + rng.next() * 2f
            cascadePaint.style = Paint.Style.STROKE
            cascadePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(streakX, streakY + yOff, streakX + (rng.next() - 0.5f) * 3, streakY + yOff + streakLen, cascadePaint)
        }

        // Side cascades
        for (side in listOf(w * 0.36f, w * 0.64f)) {
            for (i in 0 until 5) {
                val sx = side + (rng.next() - 0.5f) * w * 0.04f
                val speed = 150f + rng.next() * 100f
                val yOff = ((params.elapsedMs / 1000f * speed + rng.next() * h * 0.2f) % (h * 0.2f))
                cascadePaint.alpha = (100 + rng.next() * 80).toInt().coerceIn(0, 255)
                cascadePaint.strokeWidth = 1f + rng.next()
                canvas.drawLine(sx, h * 0.2f + yOff, sx, h * 0.2f + yOff + 10f + rng.next() * 10f, cascadePaint)
            }
        }
        cascadePaint.style = Paint.Style.FILL
        cascadePaint.alpha = 255
    }

    private fun drawMidPool(canvas: Canvas, sky: SkyState, params: SceneParams) {
        poolPaint.color = lerpColor(sky.botColor, 0xFF4A7A9A.toInt(), 0.5f)
        poolPaint.alpha = 200
        canvas.drawOval(w * 0.25f, h * 0.5f, w * 0.75f, h * 0.58f, poolPaint)

        // Ripples
        poolPaint.style = Paint.Style.STROKE
        poolPaint.strokeWidth = 1f
        poolPaint.color = 0xFFFFFFFF.toInt()
        for (i in 0 until 4) {
            val phase = (params.elapsedMs / 1500.0 + i * 1.5).toFloat()
            val r = 5f + (Math.sin(phase.toDouble()) * 15 + 15).toFloat()
            poolPaint.alpha = (30 - r * 0.8f).toInt().coerceIn(0, 255)
            canvas.drawCircle(w * 0.5f, h * 0.54f, r, poolPaint)
        }
        poolPaint.style = Paint.Style.FILL
        poolPaint.alpha = 255
    }

    private fun drawLowerRocks(canvas: Canvas) {
        rockPaint.color = 0xFF5A5A50.toInt()
        val rng = PRNG(400)
        for (i in 0 until 6) {
            val x = w * 0.2f + rng.next() * w * 0.6f
            val y = h * 0.58f + rng.next() * h * 0.06f
            val rx = 10f + rng.next() * 15f
            val ry = 6f + rng.next() * 8f
            canvas.drawOval(x - rx, y - ry, x + rx, y + ry, rockPaint)
        }
    }

    private fun drawLowerPool(canvas: Canvas, sky: SkyState, params: SceneParams) {
        poolPaint.color = lerpColor(sky.botColor, 0xFF3A6A8A.toInt(), 0.4f)
        poolPaint.alpha = 210
        canvas.drawRect(0f, h * 0.65f, w.toFloat(), h * 0.82f, poolPaint)
        poolPaint.alpha = 255
    }

    private fun drawSpray(canvas: Canvas, params: SceneParams) {
        sprayPaint.color = 0xFFFFFFFF.toInt()
        val rng = PRNG(500)
        val sprayCount = (12 + (params.intensity * 18).toInt()).coerceIn(12, 30)
        for (i in 0 until sprayCount) {
            val baseX = w * 0.5f + (rng.next() - 0.5f) * w * 0.2f
            val baseY = h * 0.52f
            val speed = 30f + rng.next() * 50f
            val angle = rng.next() * 3.14f - 1.57f
            val life = ((params.elapsedMs / 1000f * speed + rng.next() * 100f) % 50f) / 50f
            val x = baseX + (Math.cos(angle.toDouble()) * life * 30).toFloat()
            val y = baseY - (Math.sin(angle.toDouble()) * life * 20).toFloat() + life * life * 25
            sprayPaint.alpha = ((1f - life) * 120).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, 1.5f + rng.next(), sprayPaint)
        }

        // Mist above pool
        sprayPaint.color = 0xFFE8E4E0.toInt()
        sprayPaint.alpha = 25
        for (i in 0 until 4) {
            val x = w * 0.3f + rng.next() * w * 0.4f + (Math.sin(params.elapsedMs / 4000.0 + i) * 10).toFloat()
            val y = h * 0.48f + rng.next() * h * 0.06f
            canvas.drawOval(x - 40f, y - 10f, x + 40f, y + 10f, sprayPaint)
        }
        sprayPaint.alpha = 255
    }

    private fun drawForeground(canvas: Canvas, params: SceneParams) {
        // Stream
        poolPaint.color = 0xFF5A8AA0.toInt()
        poolPaint.alpha = 180
        path.reset()
        path.moveTo(w * 0.35f, h * 0.82f)
        path.quadTo(w * 0.4f, h * 0.88f, w * 0.45f, h.toFloat())
        path.lineTo(w * 0.6f, h.toFloat())
        path.quadTo(w * 0.58f, h * 0.88f, w * 0.65f, h * 0.82f)
        path.close()
        canvas.drawPath(path, poolPaint)
        poolPaint.alpha = 255

        // Bank
        fernPaint.color = 0xFF3A4A30.toInt()
        canvas.drawRect(0f, h * 0.82f, w * 0.35f, h.toFloat(), fernPaint)
        canvas.drawRect(w * 0.65f, h * 0.82f, w.toFloat(), h.toFloat(), fernPaint)

        // Stones in stream
        rockPaint.color = 0xFF6A6A60.toInt()
        val rng = PRNG(600)
        for (i in 0 until 4) {
            val x = w * 0.4f + rng.next() * w * 0.2f
            val y = h * 0.86f + rng.next() * h * 0.08f
            canvas.drawOval(x - 6f, y - 4f, x + 6f, y + 4f, rockPaint)
        }

        // Front ferns
        fernPaint.color = 0xFF4A8A3A.toInt()
        fernPaint.style = Paint.Style.STROKE
        fernPaint.strokeWidth = 2.5f
        for (i in 0 until 6) {
            val x = if (i < 3) rng.next() * w * 0.3f else w * 0.7f + rng.next() * w * 0.3f
            val y = h * 0.88f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i) * 5 * params.intensity).toFloat()
            path.reset()
            path.moveTo(x, y)
            path.quadTo(x - 15f + sway, y - 25f, x - 25f + sway, y - 18f)
            canvas.drawPath(path, fernPaint)
        }
        fernPaint.style = Paint.Style.FILL

        // Leaf drift in autumn only when wind intensity > 0.05
        if (params.season == Season.AUTUMN && params.intensity > 0.05f) {
            fernPaint.color = 0xFFD87838.toInt()
            val lrng = PRNG(700)
            for (i in 0 until 8) {
                val lx = lrng.next() * w
                val speed = 20f + lrng.next() * 30f
                val ly = ((params.elapsedMs / 1000f * speed + lrng.next() * h) % (h * 1.1f)) - h * 0.05f
                val dx = (Math.sin(params.elapsedMs / 1800.0 + lrng.next() * 6.28) * 15).toFloat()
                fernPaint.alpha = (160 + lrng.next() * 95).toInt().coerceIn(0, 255)
                canvas.drawOval(lx + dx - 3f, ly - 1.5f, lx + dx + 3f, ly + 1.5f, fernPaint)
            }
            fernPaint.alpha = 255
        }
    }
}
