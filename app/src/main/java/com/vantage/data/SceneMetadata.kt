package com.vantage.data

data class SceneMeta(
    val id: String,
    val name: String,
    val title: String,
    val story: String,
    val engine: List<String>,
    val note: String,
)

val AllScenes: List<SceneMeta> = listOf(
    SceneMeta(
        id = "forest",
        name = "Forest",
        title = "A quiet clearing",
        story = "Layered canopy, dappled light, and the hush of old-growth trees.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Fireflies appear on summer nights",
    ),
    SceneMeta(
        id = "bamboo",
        name = "Bamboo",
        title = "Bamboo grove path",
        story = "A winding stone path through tall bamboo, lantern glow at dusk.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Lantern lights up after sunset",
    ),
    SceneMeta(
        id = "mountains",
        name = "Mountains",
        title = "Alpine ridgeline",
        story = "Snow-capped peaks, pine ridges, and a still alpine lake.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Lake reflects the sky",
    ),
    SceneMeta(
        id = "river",
        name = "River",
        title = "River bend at dusk",
        story = "Gentle current, lily pads, reeds swaying in the breeze.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Sun glints on the water surface",
    ),
    SceneMeta(
        id = "waterfall",
        name = "Waterfall",
        title = "Tiered cascade",
        story = "Mossy cliffs, cascading tiers, and a fine spray of mist.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Spray particles catch light",
    ),
    SceneMeta(
        id = "space",
        name = "Space",
        title = "Orbital view",
        story = "A ringed gas giant, nebula wash, and distant stars.",
        engine = listOf("time-of-day"),
        note = "Aurora ribbons shift slowly",
    ),
    SceneMeta(
        id = "eclipse",
        name = "Eclipse",
        title = "Total eclipse",
        story = "The moment of totality — corona, silhouettes, and awe.",
        engine = listOf("time-of-day"),
        note = "Totality window at 3–4 PM",
    ),
)

fun findScene(id: String): SceneMeta = AllScenes.first { it.id == id }
