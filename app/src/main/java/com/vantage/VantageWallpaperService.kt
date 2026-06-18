package com.vantage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import com.vantage.data.VantagePrefs
import com.vantage.scene.*

class VantageWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VantageEngine()

    inner class VantageEngine : Engine(), Choreographer.FrameCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs by lazy { VantagePrefs(this@VantageWallpaperService) }
        private var scene: VantageScene = ForestScene()
        private val weatherOverlay = WeatherOverlay()
        private var choreographer: Choreographer? = null
        private var running = false
        private var visible = false
        private var w = 0
        private var h = 0
        private var lastFrameNs = 0L
        private var startTimeMs = System.currentTimeMillis()
        private var lowBattery = false
        private var screenOn = true

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                lowBattery = (level * 100 / scale) < 15
            }
        }

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn = false
                        if (prefs.pauseScreenOff) stopRendering()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenOn = true
                        if (visible) startRendering()
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadScene()
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenReceiver, screenFilter)
            getSharedPreferences("vantage", MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            stopRendering()
            scene.release()
            try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            getSharedPreferences("vantage", MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, fmt: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, fmt, width, height)
            w = width; h = height
            scene.init(w, h)
            weatherOverlay.init(w, h)
        }

        override fun onVisibilityChanged(vis: Boolean) {
            visible = vis
            if (vis && screenOn) startRendering() else stopRendering()
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == "current_scene") loadScene()
        }

        private fun loadScene() {
            scene.release()
            scene = createScene(prefs.currentScene)
            if (w > 0 && h > 0) scene.init(w, h)
        }

        private fun startRendering() {
            if (running) return
            running = true
            lastFrameNs = System.nanoTime()
            choreographer = Choreographer.getInstance()
            choreographer?.postFrameCallback(this)
        }

        private fun stopRendering() {
            running = false
            choreographer?.removeFrameCallback(this)
            choreographer = null
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running || !visible) return

            if (lowBattery && prefs.pauseOnBattery) {
                choreographer?.postFrameCallback(this)
                return
            }

            val deltaNs = (frameTimeNanos - lastFrameNs).coerceAtLeast(0)
            lastFrameNs = frameTimeNanos
            val deltaMs = deltaNs / 1_000_000
            val elapsedMs = System.currentTimeMillis() - startTimeMs

            val holder = surfaceHolder
            val canvas = try { holder.lockCanvas() } catch (_: Exception) { null }
            if (canvas != null) {
                try {
                    val resolvedTime = if (prefs.useAutoTime) currentTimeOfDay() else prefs.timeOverride
                    val resolvedSeason = when (prefs.seasonOverride) {
                        "spring" -> Season.SPRING
                        "summer" -> Season.SUMMER
                        "autumn" -> Season.AUTUMN
                        "winter" -> Season.WINTER
                        else -> currentSeason()
                    }
                    val resolvedWeather = if (prefs.weatherEnabled) when (prefs.weatherType) {
                        "rain" -> WeatherType.RAIN
                        "snow" -> WeatherType.SNOW
                        "fog" -> WeatherType.FOG
                        "cloudy" -> WeatherType.CLOUDY
                        else -> WeatherType.CLEAR
                    } else WeatherType.CLEAR
                    val params = SceneParams(
                        width = w,
                        height = h,
                        timeOfDay = resolvedTime,
                        season = resolvedSeason,
                        weather = resolvedWeather,
                        intensity = prefs.intensity,
                        deltaMs = deltaMs,
                        elapsedMs = elapsedMs,
                    )
                    scene.draw(canvas, params)
                    if (prefs.weatherEnabled) {
                        weatherOverlay.draw(canvas, params.weather, params.intensity, deltaMs)
                    }
                } finally {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
                }
            }

            // Frame pacing
            val targetInterval = 1_000_000_000L / prefs.frameRate
            val elapsed = System.nanoTime() - frameTimeNanos
            val delay = ((targetInterval - elapsed) / 1_000_000).coerceAtLeast(0)
            if (running) {
                choreographer?.postFrameCallbackDelayed(this, delay)
            }
        }
    }
}
