package com.vantage.scene

fun createScene(id: String): VantageScene = when (id) {
    "forest" -> ForestScene()
    "bamboo" -> BambooScene()
    "mountains" -> MountainsScene()
    "river" -> RiverScene()
    "waterfall" -> WaterfallScene()
    "space" -> SpaceScene()
    "eclipse" -> EclipseScene()
    else -> ForestScene()
}
