package com.vantage.data

import android.content.Context
import android.content.SharedPreferences

class VantagePrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vantage", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(v) = prefs.edit().putBoolean(KEY_ONBOARDING, v).apply()

    var currentScene: String
        get() = prefs.getString(KEY_SCENE, "mountains") ?: "mountains"
        set(v) = prefs.edit().putString(KEY_SCENE, v).apply()

    /** When true, weather is auto-resolved (local feed or sensible default) and manual selection is hidden. */
    var weatherAuto: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_AUTO, true)
        set(v) = prefs.edit().putBoolean(KEY_WEATHER_AUTO, v).apply()

    var intensity: Float
        get() = prefs.getFloat(KEY_INTENSITY, 0.5f)
        set(v) = prefs.edit().putFloat(KEY_INTENSITY, v).apply()

    var weatherEnabled: Boolean
        get() = prefs.getBoolean(KEY_WEATHER, true)
        set(v) = prefs.edit().putBoolean(KEY_WEATHER, v).apply()

    var pauseOnBattery: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_PAUSE, true)
        set(v) = prefs.edit().putBoolean(KEY_BATTERY_PAUSE, v).apply()

    var frameRate: Int
        get() = prefs.getInt(KEY_FRAME_RATE, 30)
        set(v) = prefs.edit().putInt(KEY_FRAME_RATE, v).apply()

    var locationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION, false)
        set(v) = prefs.edit().putBoolean(KEY_LOCATION, v).apply()

    var manualCity: String
        get() = prefs.getString(KEY_CITY, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CITY, v).apply()

    var pauseScreenOff: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_OFF, true)
        set(v) = prefs.edit().putBoolean(KEY_SCREEN_OFF, v).apply()

    var weatherType: String
        get() = prefs.getString(KEY_WEATHER_TYPE, "clear") ?: "clear"
        set(v) = prefs.edit().putString(KEY_WEATHER_TYPE, v).apply()

    var seasonOverride: String
        get() = prefs.getString(KEY_SEASON, "auto") ?: "auto"
        set(v) = prefs.edit().putString(KEY_SEASON, v).apply()

    var timeOverride: Float
        get() = prefs.getFloat(KEY_TIME_OVERRIDE, -1f)
        set(v) = prefs.edit().putFloat(KEY_TIME_OVERRIDE, v).apply()

    val useAutoTime: Boolean get() = timeOverride < 0f

    companion object {
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_SCENE = "current_scene"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_WEATHER = "weather_enabled"
        private const val KEY_BATTERY_PAUSE = "battery_pause"
        private const val KEY_FRAME_RATE = "frame_rate"
        private const val KEY_LOCATION = "location_enabled"
        private const val KEY_CITY = "manual_city"
        private const val KEY_SCREEN_OFF = "pause_screen_off"
        private const val KEY_WEATHER_TYPE = "weather_type"
        private const val KEY_SEASON = "season_override"
        private const val KEY_TIME_OVERRIDE = "time_override"
        private const val KEY_WEATHER_AUTO = "weather_auto"
    }
}
