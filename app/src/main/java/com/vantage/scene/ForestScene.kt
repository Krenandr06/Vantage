package com.vantage.scene

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class ForestScene : VantageScene {

    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val canopyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaftPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fernPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var w = 0
    private var h = 0

    override fun init(width: Int, height: Int) {
        w = width
        h = height
    }

    override fun draw(canvas: Canvas, params: SceneParams) {
        val sky = interpolateSky(params.timeOfDay)
        drawSkyGradient(canvas, w, h, sky)
        drawStars(canvas, w, h, sky.starsOpacity, params.elapsedMs)
        drawSun(canvas, w, h, sky)
        drawMoon(canvas, w, h, sky)

        val palette = seasonPalette(params.season)
        drawFarTrees(canvas, palette, sky)
        drawMidTrees(canvas, palette, params)
        drawLightShafts(canvas, params)
        drawNearTrees(canvas, palette, params)
        drawForestFloor(canvas, palette, params)
        drawFerns(canvas, palette, params)

        if (params.season == Season.AUTUMN) drawFallingLeaves(canvas, palette, params)
        if (params.season == Season.SUMMER && params.timeOfDay > 20f || params.timeOfDay < 5f)
            drawFireflies(canvas, params)
        if (params.timeOfDay in 5f..7f || params.timeOfDay in 18f..20f)
            drawFloorMist(canvas, params)
    }

    private data class ForestPalette(
        val farTree: Int, val midTree: Int, val nearTree: Int,
        val canopy: Int, val canopyLight: Int, val trunk: Int,
        val floor: Int, val fern: Int, val leaf: Int,
    )

    private fun seasonPalette(season: Season) = when (season) {
        Season.SPRING -> ForestPalette(
            0xFF4A7A48.toInt(), 0xFF3D6B3A.toInt(), 0xFF2D5A2A.toInt(),
            0xFF5C9A50.toInt(), 0xFF7AB868.toInt(), 0xFF5A4A3A.toInt(),
            0xFF3A5A2A.toInt(), 0xFF4A8A3A.toInt(), 0xFF6ABB55.toInt(),
        )
        Season.SUMMER -> ForestPalette(
            0xFF3A6A38.toInt(), 0xFF2D5A2A.toInt(), 0xFF1E4A1C.toInt(),
            0xFF4A8A40.toInt(), 0xFF5CA850.toInt(), 0xFF4A3A2A.toInt(),
            0xFF2A4A1A.toInt(), 0xFF3A7A2A.toInt(), 0xFF4A9A38.toInt(),
        )
        Season.AUTUMN -> ForestPalette(
            0xFF8A6A38.toInt(), 0xFF9A5A28.toInt(), 0xFF7A4A20.toInt(),
            0xFFC88A40.toInt(), 0xFFE8A848.toInt(), 0xFF5A4030.toInt(),
            0xFF6A5030.toInt(), 0xFF9A7A40.toInt(), 0xFFD87838.toInt(),
        )
        Season.WINTER -> ForestPalette(
            0xFF5A6A68.toInt(), 0xFF4A5A58.toInt(), 0xFF3A4A48.toInt(),
            0xFF6A7A70.toInt(), 0xFF7A8A80.toInt(), 0xFF5A5048.toInt(),
            0xFF4A5A50.toInt(), 0xFF5A6A58.toInt(), 0xFF6A7A68.toInt(),
        )
    }

    private fun drawFarTrees(canvas: Canvas, p: ForestPalette, sky: SkyState) {
        treePaint.color = lerpColor(p.farTree, sky.botColor, 0.3f)
        val rng = PRNG(101)
        val baseY = h * 0.3f
        for (i in 0 until 12) {
            val x = rng.next() * w
            val treeH = h * (0.25f + rng.next() * 0.15f)
            val treeW = w * (0.06f + rng.next() * 0.04f)
            path.reset()
            path.moveTo(x, baseY)
            path.lineTo(x - treeW, baseY + treeH)
            path.lineTo(x + treeW, baseY + treeH)
            path.close()
            canvas.drawPath(path, treePaint)
        }
        treePaint.color = lerpColor(p.farTree, sky.botColor, 0.25f)
        canvas.drawRect(0f, baseY + h * 0.2f, w.toFloat(), h.toFloat(), treePaint)
    }

    private fun drawMidTrees(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        val rng = PRNG(202)
        val baseY = h * 0.35f
        for (i in 0 until 8) {
            val x = rng.next() * w
            val trunkH = h * (0.3f + rng.next() * 0.15f)
            val trunkW = w * 0.02f + rng.next() * w * 0.015f

            treePaint.color = p.trunk
            canvas.drawRect(x - trunkW / 2, baseY, x + trunkW / 2, baseY + trunkH, treePaint)

            canopyPaint.color = p.canopy
            val canopyR = w * (0.08f + rng.next() * 0.06f)
            canvas.drawCircle(x, baseY - canopyR * 0.3f, canopyR, canopyPaint)

            canopyPaint.color = p.canopyLight
            canopyPaint.alpha = 140
            canvas.drawCircle(x - canopyR * 0.2f, baseY - canopyR * 0.5f, canopyR * 0.6f, canopyPaint)
            canopyPaint.alpha = 255
        }
    }

    private fun drawLightShafts(canvas: Canvas, params: SceneParams) {
        if (params.timeOfDay < 6f || params.timeOfDay > 18f) return
        shaftPaint.color = 0xFFFFF8E0.toInt()
        val rng = PRNG(303)
        val count = 4
        for (i in 0 until count) {
            val x = rng.next() * w
            val shaftW = w * (0.03f + rng.next() * 0.04f)
            val sway = (Math.sin((params.elapsedMs / 3000.0) + rng.next() * 6.28) * w * 0.01).toFloat()
            shaftPaint.alpha = (20 + (rng.next() * 25).toInt()).coerceIn(0, 255)
            canvas.drawRect(x + sway - shaftW / 2, 0f, x + sway + shaftW / 2, h * 0.8f, shaftPaint)
        }
        shaftPaint.alpha = 255
    }

    private fun drawNearTrees(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        val rng = PRNG(404)
        for (i in 0 until 5) {
            val x = rng.next() * w
            val trunkH = h * (0.4f + rng.next() * 0.2f)
            val trunkW = w * 0.03f + rng.next() * w * 0.02f
            val baseY = h * 0.55f

            treePaint.color = p.trunk
            canvas.drawRect(x - trunkW / 2, baseY, x + trunkW / 2, baseY + trunkH, treePaint)

            canopyPaint.color = p.nearTree
            val r = w * (0.1f + rng.next() * 0.08f)
            canvas.drawCircle(x, baseY - r * 0.2f, r, canopyPaint)

            // Vines
            treePaint.color = p.fern
            treePaint.alpha = 100
            treePaint.strokeWidth = 2f
            treePaint.style = Paint.Style.STROKE
            val vineLen = h * 0.05f + rng.next() * h * 0.08f
            val sway = (Math.sin((params.elapsedMs / 2500.0) + i) * 3).toFloat()
            canvas.drawLine(x - r * 0.3f, baseY, x - r * 0.3f + sway, baseY + vineLen, treePaint)
            canvas.drawLine(x + r * 0.4f, baseY, x + r * 0.4f + sway, baseY + vineLen * 0.8f, treePaint)
            treePaint.style = Paint.Style.FILL
            treePaint.alpha = 255
        }
    }

    private fun drawForestFloor(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        floorPaint.color = p.floor
        canvas.drawRect(0f, h * 0.78f, w.toFloat(), h.toFloat(), floorPaint)

        // Mushrooms and stones
        val rng = PRNG(505)
        for (i in 0 until 6) {
            val x = rng.next() * w
            val y = h * 0.8f + rng.next() * h * 0.12f
            if (i % 2 == 0) {
                floorPaint.color = 0xFF8A7A6A.toInt()
                canvas.drawOval(x - 8f, y - 4f, x + 8f, y + 4f, floorPaint)
            } else {
                floorPaint.color = 0xFFC84040.toInt()
                canvas.drawCircle(x, y - 6f, 5f, floorPaint)
                floorPaint.color = 0xFFE8D8C0.toInt()
                canvas.drawRect(x - 2f, y - 2f, x + 2f, y + 4f, floorPaint)
            }
        }

        // Moss patches
        floorPaint.color = 0xFF4A7A3A.toInt()
        floorPaint.alpha = 120
        for (i in 0 until 10) {
            val x = rng.next() * w
            val y = h * 0.82f + rng.next() * h * 0.1f
            canvas.drawOval(x - 15f, y - 4f, x + 15f, y + 4f, floorPaint)
        }
        floorPaint.alpha = 255
    }

    private fun drawFerns(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        fernPaint.color = p.fern
        val rng = PRNG(606)
        for (i in 0 until 8) {
            val x = rng.next() * w
            val baseY = h * 0.85f + rng.next() * h * 0.1f
            val sway = (Math.sin((params.elapsedMs / 2000.0) + i * 1.5) * 8 * params.intensity).toFloat()

            path.reset()
            path.moveTo(x, baseY)
            path.quadTo(x - 20f + sway, baseY - 40f, x - 35f + sway * 1.5f, baseY - 30f)
            path.moveTo(x, baseY)
            path.quadTo(x + 18f + sway, baseY - 35f, x + 30f + sway * 1.5f, baseY - 25f)

            fernPaint.style = Paint.Style.STROKE
            fernPaint.strokeWidth = 3f
            canvas.drawPath(path, fernPaint)
            fernPaint.style = Paint.Style.FILL
        }
    }

    private fun drawFallingLeaves(canvas: Canvas, p: ForestPalette, params: SceneParams) {
        leafPaint.color = p.leaf
        val rng = PRNG(707)
        for (i in 0 until 15) {
            val baseX = rng.next() * w
            val speed = 30f + rng.next() * 40f
            val phase = rng.next() * 6.28f
            val y = ((params.elapsedMs / 1000f * speed * 0.3f + rng.next() * h) % (h * 1.1f)) - h * 0.05f
            val x = baseX + (Math.sin((params.elapsedMs / 1500.0) + phase) * 30).toFloat()
            val rot = ((params.elapsedMs / 800f + phase) % 360f)

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)
            leafPaint.alpha = (180 + (rng.next() * 75).toInt()).coerceIn(0, 255)
            canvas.drawOval(-4f, -2f, 4f, 2f, leafPaint)
            canvas.restore()
        }
        leafPaint.alpha = 255
    }

    private fun drawFireflies(canvas: Canvas, params: SceneParams) {
        val rng = PRNG(808)
        glowPaint.color = 0xFFE8E060.toInt()
        for (i in 0 until 12) {
            val baseX = rng.next() * w
            val baseY = h * 0.4f + rng.next() * h * 0.4f
            val phase = rng.next() * 6.28f
            val x = baseX + (Math.sin((params.elapsedMs / 3000.0) + phase) * 20).toFloat()
            val y = baseY + (Math.cos((params.elapsedMs / 4000.0) + phase * 1.3) * 15).toFloat()
            val brightness = ((Math.sin((params.elapsedMs / 1200.0) + phase * 2) + 1) / 2).toFloat()
            glowPaint.alpha = (brightness * 200).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, 4f, glowPaint)
            glowPaint.alpha = (brightness * 60).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, 10f, glowPaint)
        }
        glowPaint.alpha = 255
    }

    private fun drawFloorMist(canvas: Canvas, params: SceneParams) {
        mistPaint.color = 0xFFD8D0C8.toInt()
        mistPaint.alpha = 40
        val rng = PRNG(909)
        for (i in 0 until 6) {
            val x = rng.next() * w + (Math.sin(params.elapsedMs / 5000.0 + i) * 20).toFloat()
            val y = h * 0.75f + rng.next() * h * 0.15f
            canvas.drawOval(x - 80f, y - 12f, x + 80f, y + 12f, mistPaint)
        }
        mistPaint.alpha = 255
    }
}
