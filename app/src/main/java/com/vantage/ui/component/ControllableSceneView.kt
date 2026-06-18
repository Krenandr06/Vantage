package com.vantage.ui.component

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.vantage.scene.*

class ControllableSceneView(
    context: Context,
    sceneId: String,
) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private var scene: VantageScene = createScene(sceneId)
    private var currentSceneId: String = sceneId
    private val weatherOverlay = WeatherOverlay()
    private var running = false
    private var lastFrameNs = 0L
    private val startTimeMs = System.currentTimeMillis()

    var timeOfDay: Float = currentTimeOfDay()
    var season: Season = currentSeason()
    var weather: WeatherType = WeatherType.CLEAR
    var intensity: Float = 0.5f

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        scene.init(width, height)
        weatherOverlay.init(width, height)
        running = true
        lastFrameNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(h: SurfaceHolder, fmt: Int, w: Int, ht: Int) {
        scene.init(w, ht)
        weatherOverlay.init(w, ht)
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        scene.release()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val deltaNs = (frameTimeNanos - lastFrameNs).coerceAtLeast(0)
        lastFrameNs = frameTimeNanos
        val deltaMs = deltaNs / 1_000_000
        val elapsedMs = System.currentTimeMillis() - startTimeMs

        val canvas: Canvas? = try { holder.lockCanvas() } catch (_: Exception) { null }
        if (canvas != null) {
            try {
                val params = SceneParams(
                    width = width, height = height,
                    timeOfDay = timeOfDay,
                    season = season,
                    weather = weather,
                    intensity = intensity,
                    deltaMs = deltaMs,
                    elapsedMs = elapsedMs,
                )
                scene.draw(canvas, params)
                if (weather != WeatherType.CLEAR) {
                    weatherOverlay.draw(canvas, weather, intensity, deltaMs)
                }
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }

        if (running) {
            Choreographer.getInstance().postFrameCallbackDelayed(this, 33)
        }
    }

    fun updateScene(newSceneId: String) {
        if (newSceneId == currentSceneId) return
        currentSceneId = newSceneId
        scene.release()
        scene = createScene(newSceneId)
        if (width > 0 && height > 0) scene.init(width, height)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(this)
    }
}
