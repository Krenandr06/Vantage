package com.vantage.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.view.Choreographer
import android.view.TextureView
import com.vantage.scene.*

class SceneTextureView(context: Context) : TextureView(context),
    TextureView.SurfaceTextureListener, Choreographer.FrameCallback {

    private var scene: VantageScene? = null
    private var sceneId: String = ""
    private val weatherOverlay = WeatherOverlay()
    private var running = false
    private var surfaceReady = false
    private var startMs = System.currentTimeMillis()
    private var lastFrameNs = 0L

    var timeOfDay: Float = 12f
    var sceneSeason: Season = Season.SUMMER
    var sceneWeather: WeatherType = WeatherType.CLEAR
    var intensity: Float = 0.5f

    /**
     * When false the Choreographer callback is detached so this view stops
     * burning CPU. Used for off-screen pager pages so swipes stay smooth.
     * Last-drawn frame remains visible on the surface.
     */
    var active: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value && surfaceReady) {
                // Refresh the poster with current params before resuming the
                // animation loop, so the user sees the latest state immediately.
                drawSingleFrame()
                startRendering()
            } else {
                stopRendering()
                // Leave one final still frame on the surface so an off-screen
                // page shows a poster instead of going black.
                if (surfaceReady) drawSingleFrame()
            }
        }

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    fun setScene(id: String) {
        if (id == sceneId && scene != null) return
        sceneId = id
        scene?.release()
        scene = createScene(id).also {
            if (width > 0 && height > 0) it.init(width, height)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
        scene?.init(w, h)
        weatherOverlay.init(w, h)
        surfaceReady = true
        // Always paint a first frame so an inactive page shows a poster, not black.
        drawSingleFrame()
        if (active) startRendering()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {
        scene?.init(w, h)
        weatherOverlay.init(w, h)
        drawSingleFrame()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceReady = false
        stopRendering()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun doFrame(frameTimeNanos: Long) {
        if (!running || !surfaceReady) return

        val deltaNs = if (lastFrameNs > 0) (frameTimeNanos - lastFrameNs).coerceAtLeast(0) else 16_000_000L
        lastFrameNs = frameTimeNanos
        val deltaMs = deltaNs / 1_000_000

        renderFrame(deltaMs)

        if (running) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun drawSingleFrame() {
        if (!surfaceReady || width <= 0 || height <= 0) return
        renderFrame(16L)
    }

    private fun renderFrame(deltaMs: Long) {
        val now = System.currentTimeMillis()
        val canvas: Canvas = try { lockCanvas() } catch (_: Exception) { null } ?: return
        try {
            val s = scene ?: return
            if (width <= 0 || height <= 0) return
            val params = SceneParams(
                width = width,
                height = height,
                timeOfDay = timeOfDay,
                season = sceneSeason,
                weather = sceneWeather,
                intensity = intensity,
                deltaMs = deltaMs,
                elapsedMs = now - startMs,
            )
            s.draw(canvas, params)
            if (sceneWeather != WeatherType.CLEAR) {
                weatherOverlay.draw(canvas, sceneWeather, intensity, deltaMs)
            }
        } finally {
            try { unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    private fun startRendering() {
        if (running) return
        running = true
        startMs = System.currentTimeMillis()
        lastFrameNs = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun stopRendering() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun cleanup() {
        stopRendering()
        scene?.release()
        scene = null
    }
}
