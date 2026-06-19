package com.vantage.data

data class SceneMeta(
    val id: String,
    val name: String,
    val title: String,
    val story: String,
    val engine: List<String>,
    val note: String,
    val visible: Boolean = true,
)

val AllScenes: List<SceneMeta> = listOf(
    SceneMeta(
        id = "mountains",
        name = "Mountains",
        title = "A distant range, breathing.",
        story = "Layered ridges receding into haze. The sun crosses the sky; a sea of clouds gathers in the valley at dawn and dusk. Snow lingers on the peaks; rim light catches the highest summit at golden hour.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Snow line shifts with month, not just season. Mist density depends on humidity reported by the weather feed.",
    ),
    SceneMeta(
        id = "space",
        name = "Space",
        title = "A planet, slowly turning.",
        story = "A view from low orbit. A planet curves across the lower half of the screen, slowly turning. The stars beyond twinkle at their real positions for tonight; rare comets arc past at high intensity.",
        engine = listOf("time-of-day"),
        note = "Comets only at intensity 60% and above. Aurora ribbons appear when local geomagnetic activity is reported.",
    ),
    // Hidden — kept on disk so the engine can still resolve them, but not surfaced in UI.
    SceneMeta(
        id = "forest",
        name = "Forest",
        title = "A grove that holds the weather.",
        story = "A dim grove of close trunks. Sunbeams find their way through the canopy in morning and at golden hour. Autumn brings drifting leaves; summer nights bring a few fireflies. Light rain through everything when the sky asks.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Particles bind to the wind direction reported by the weather feed. Frame budget caps at 30 fps to keep the device cool.",
        visible = false,
    ),
    SceneMeta(
        id = "bamboo",
        name = "Bamboo",
        title = "Stalks lean into the wind.",
        story = "Tall stalks against a pale, open sky. The whole grove leans gently with the wind. Sparrows pass at dusk; fog settles at the base on cold mornings.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Sway amplitude tracks the motion slider directly. Sparrows only appear at dawn and dusk when intensity is at least 40%.",
        visible = false,
    ),
    SceneMeta(
        id = "river",
        name = "River",
        title = "A slow bend, catching the light.",
        story = "A wide, slow river under an open sky. Ripples catch every change in the sun. Reeds shiver at the edge. Mist sits on the water on cold mornings.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "The sun reflection follows the real sun position. Ripple speed scales with the motion slider — never zero, never harsh.",
        visible = false,
    ),
    SceneMeta(
        id = "waterfall",
        name = "Waterfall",
        title = "A narrow falls, with spray.",
        story = "A thin sheet of falling water between two dark walls. Spray gathers at the base into a soft, breathing mist that catches the sky tint above.",
        engine = listOf("seasons", "weather", "time-of-day"),
        note = "Streak density is the heaviest of all scenes — the slider gates the upper half hard for battery.",
        visible = false,
    ),
    SceneMeta(
        id = "eclipse",
        name = "Eclipse",
        title = "Asleep until totality.",
        story = "An event scene. The wallpaper waits quietly until a real eclipse is visible from your location. During totality, the corona breathes against a dark horizon. Otherwise the scene is mostly still.",
        engine = listOf("time-of-day"),
        note = "Next totality near Brooklyn: Aug 12, 2026 at 3:41 pm. Outside that window the slider has no effect — by design.",
        visible = false,
    ),
)

val VisibleScenes: List<SceneMeta> = AllScenes.filter { it.visible }

fun findScene(id: String): SceneMeta = AllScenes.first { it.id == id }
