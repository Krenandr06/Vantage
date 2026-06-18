package com.vantage.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOCATION = "location"
    const val HOME = "home"
    const val SCENE_PICKER = "scene_picker"
    const val PREVIEW = "preview/{sceneId}"
    const val SETTINGS = "settings"

    fun preview(sceneId: String) = "preview/$sceneId"
}
