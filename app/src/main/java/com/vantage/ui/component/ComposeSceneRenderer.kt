package com.vantage.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.vantage.scene.*

class SceneCanvasView(context: Context) : View(context) {

    private var scene: VantageScene? = null
    private var sceneId: String = ""
    private val weatherOverlay = WeatherOverlay()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var startMs = System.currentTimeMillis()
    private var lastMs = startMs

    var timeOfDay: Float = 12f
    var sceneSeason: Season = Season.SUMMER
    var sceneWeather: WeatherType = WeatherType.CLEAR
    var intensity: Float = 0.5f

    fun setSceneId(id: String) {
        if (id == sceneId) return
        sceneId = id
        scene?.release()
        scene = createScene(id)
        if (width > 0 && height > 0) scene?.init(width, height)
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            handler.postDelayed(this, 33)
        }
    }

    fun startRendering() {
        if (running) return
        running = true
        startMs = System.currentTimeMillis()
        lastMs = startMs
        handler.post(frameRunnable)
    }

    fun stopRendering() {
        running = false
        handler.removeCallbacks(frameRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            scene?.init(w, h)
            weatherOverlay.init(w, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val s = scene ?: return
        val now = System.currentTimeMillis()
        val deltaMs = (now - lastMs).coerceAtLeast(1)
        lastMs = now

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRendering()
    }

    override fun onDetachedFromWindow() {
        stopRendering()
        super.onDetachedFromWindow()
    }
}

@Composable
fun ComposeSceneRenderer(
    sceneId: String,
    timeOfDay: Float,
    season: Season,
    weather: WeatherType,
    intensity: Float,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx ->
            SceneCanvasView(ctx).apply {
                setSceneId(sceneId)
                this.timeOfDay = timeOfDay
                this.sceneSeason = season
                this.sceneWeather = weather
                this.intensity = intensity
            }
        },
        update = { view ->
            view.setSceneId(sceneId)
            view.timeOfDay = timeOfDay
            view.sceneSeason = season
            view.sceneWeather = weather
            view.intensity = intensity
        },
        modifier = modifier.fillMaxSize(),
    )
}
