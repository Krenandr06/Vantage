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
        startRendering()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {
        scene?.init(w, h)
        weatherOverlay.init(w, h)
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
        val now = System.currentTimeMillis()

        val canvas: Canvas? = try { lockCanvas() } catch (_: Exception) { null }
        if (canvas != null) {
            try {
                val s = scene
                if (s != null && width > 0 && height > 0) {
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
                }
            } finally {
                try { unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }

        if (running) {
            Choreographer.getInstance().postFrameCallback(this)
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
