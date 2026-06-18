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
    private val drawPath = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) { w = width; h = height }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)

        drawCanopy(canvas, params)
        drawFarStalks(canvas, params)
        drawStoneWalls(canvas, params)
        drawStonePath(canvas, params)
        drawDappledLight(canvas, params)
        drawMidStalks(canvas, params)
        drawForegroundStalks(canvas, params)
        drawLeafClusters(canvas, params)
        drawLantern(canvas, params)
    }

    private fun drawCanopy(canvas: Canvas, params: SceneParams) {
        val green = when (params.season) {
            Season.SPRING -> 0xFF5A9A48.toInt()
            Season.SUMMER -> 0xFF3A7A30.toInt()
            Season.AUTUMN -> 0xFF7A8A40.toInt()
            Season.WINTER -> 0xFF4A6A48.toInt()
        }
        canopyPaint.color = green
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

    private fun drawFarStalks(canvas: Canvas, params: SceneParams) {
        stalkPaint.color = lerpColor(0xFF6A9A58.toInt(), 0xFF5A8A48.toInt(), 0.5f)
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

    private fun drawStoneWalls(canvas: Canvas, params: SceneParams) {
        stonePaint.color = 0xFF8A8070.toInt()
        canvas.drawRect(0f, h * 0.65f, w * 0.15f, h * 0.85f, stonePaint)
        canvas.drawRect(w * 0.85f, h * 0.6f, w.toFloat(), h * 0.82f, stonePaint)

        // Moss on walls
        stonePaint.color = 0xFF5A7A48.toInt()
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

    private fun drawStonePath(canvas: Canvas, params: SceneParams) {
        pathPaint.color = 0xFFB0A890.toInt()
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
        stonePaint.color = 0xFF9A9080.toInt()
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

    private fun drawMidStalks(canvas: Canvas, params: SceneParams) {
        val green = when (params.season) {
            Season.SPRING -> 0xFF5CA048.toInt()
            Season.SUMMER -> 0xFF4A8A38.toInt()
            Season.AUTUMN -> 0xFF7A9040.toInt()
            Season.WINTER -> 0xFF4A7048.toInt()
        }
        stalkPaint.color = green
        val rng = PRNG(660)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val stalkW = 6f + rng.next() * 5f
            val sway = (Math.sin((params.elapsedMs / 2800.0) + i * 1.1) * 5 * params.intensity).toFloat()
            val top = -h * 0.05f
            val bot = h * 0.85f + rng.next() * h * 0.1f
            canvas.drawRect(x - stalkW / 2 + sway, top, x + stalkW / 2 + sway, bot, stalkPaint)

            // Nodes
            stalkPaint.color = lerpColor(green, 0xFF3A6A28.toInt(), 0.3f)
            val nodeSpacing = h * 0.08f
            var ny = h * 0.05f
            while (ny < bot) {
                canvas.drawRect(x - stalkW / 2 - 2 + sway, ny - 1.5f, x + stalkW / 2 + 2 + sway, ny + 1.5f, stalkPaint)
                ny += nodeSpacing + rng.next() * nodeSpacing * 0.5f
            }
            stalkPaint.color = green
        }
    }

    private fun drawForegroundStalks(canvas: Canvas, params: SceneParams) {
        stalkPaint.color = 0xFF3A7A28.toInt()
        val rng = PRNG(770)
        for (i in 0 until 4) {
            val side = if (i % 2 == 0) -w * 0.02f + rng.next() * w * 0.12f
                       else w * 0.88f + rng.next() * w * 0.14f
            val stalkW = 10f + rng.next() * 6f
            val sway = (Math.sin((params.elapsedMs / 2200.0) + i * 0.7) * 8 * params.intensity).toFloat()
            canvas.drawRect(side - stalkW / 2 + sway, -10f, side + stalkW / 2 + sway, h * 0.95f, stalkPaint)
        }
    }

    private fun drawLeafClusters(canvas: Canvas, params: SceneParams) {
        val green = when (params.season) {
            Season.SPRING -> 0xFF6AB050.toInt()
            Season.SUMMER -> 0xFF4A9038.toInt()
            Season.AUTUMN -> 0xFF8A9838.toInt()
            Season.WINTER -> 0xFF5A7850.toInt()
        }
        leafPaint.color = green
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

    private fun drawLantern(canvas: Canvas, params: SceneParams) {
        val lx = w * 0.48f
        val ly = h * 0.62f

        // Post
        lanternPaint.color = 0xFF5A4A3A.toInt()
        canvas.drawRect(lx - 3f, ly, lx + 3f, ly + 40f, lanternPaint)

        // Lantern body
        lanternPaint.color = 0xFF8A7A5A.toInt()
        canvas.drawRect(lx - 10f, ly - 16f, lx + 10f, ly, lanternPaint)
        lanternPaint.color = 0xFF6A5A3A.toInt()
        canvas.drawRect(lx - 12f, ly - 18f, lx + 12f, ly - 16f, lanternPaint)
        canvas.drawRect(lx - 12f, ly, lx + 12f, ly + 2f, lanternPaint)

        // Glow after sunset
        if (params.timeOfDay > 17.5f || params.timeOfDay < 6f) {
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
}
