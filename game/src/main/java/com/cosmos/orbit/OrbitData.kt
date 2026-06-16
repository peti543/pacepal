package com.cosmos.orbit

import androidx.compose.ui.graphics.Color

/** A labelled fact shown in the detail panel. */
data class Fact(val label: String, val value: String)

enum class BodyType(val label: String) {
    STAR("Star"),
    TERRESTRIAL("Terrestrial planet"),
    GAS_GIANT("Gas giant"),
    ICE_GIANT("Ice giant"),
    DWARF("Dwarf planet")
}

/**
 * A celestial body in the explorer. Sizes, orbit radii and speeds are tuned for a
 * pleasant, readable layout — they are NOT to true astronomical scale (the real
 * solar system is mostly empty space and would be impossible to view at once).
 */
data class CelestialBody(
    val id: String,
    val name: String,
    val type: BodyType,
    val color: Color,
    val bandColor: Color,
    val displayRadius: Float,   // world units
    val orbitRadius: Float,     // world units, 0 for the Sun
    val orbitSpeed: Float,      // relative angular velocity (rad/sec)
    val spinSpeed: Float,       // relative spin for the detail view
    val startAngle: Float,      // initial orbit angle (rad)
    val hasRings: Boolean = false,
    val ringColor: Color = Color(0xFFB9A98C),
    val description: String,
    val facts: List<Fact>
)

object OrbitData {

    val bodies: List<CelestialBody> = listOf(
        CelestialBody(
            id = "sun",
            name = "Sun",
            type = BodyType.STAR,
            color = Color(0xFFFFD24D),
            bandColor = Color(0xFFFF9E3D),
            displayRadius = 46f,
            orbitRadius = 0f,
            orbitSpeed = 0f,
            spinSpeed = 0.25f,
            startAngle = 0f,
            description = "The Sun is the star at the heart of our solar system — a roaring ball " +
                "of hydrogen and helium so massive that it holds every planet in orbit. It " +
                "accounts for about 99.8% of all the mass in the solar system.",
            facts = listOf(
                Fact("Type", "G-type main-sequence star"),
                Fact("Diameter", "1,392,700 km"),
                Fact("Surface temp.", "~5,500 °C"),
                Fact("Core temp.", "~15 million °C"),
                Fact("Age", "~4.6 billion years"),
                Fact("Composition", "73% hydrogen, 25% helium")
            )
        ),
        CelestialBody(
            id = "mercury",
            name = "Mercury",
            type = BodyType.TERRESTRIAL,
            color = Color(0xFF9C8C7A),
            bandColor = Color(0xFF6E6155),
            displayRadius = 8f,
            orbitRadius = 112f,
            orbitSpeed = 0.95f,
            spinSpeed = 0.4f,
            startAngle = 0.4f,
            description = "The smallest planet and the closest to the Sun. Mercury has almost no " +
                "atmosphere, so it swings between scorching days and freezing nights more " +
                "extremely than anywhere else in the solar system.",
            facts = listOf(
                Fact("Order from Sun", "1st"),
                Fact("Diameter", "4,879 km"),
                Fact("Distance from Sun", "58 million km"),
                Fact("Day length", "59 Earth days"),
                Fact("Year length", "88 Earth days"),
                Fact("Moons", "0")
            )
        ),
        CelestialBody(
            id = "venus",
            name = "Venus",
            type = BodyType.TERRESTRIAL,
            color = Color(0xFFE6C98C),
            bandColor = Color(0xFFC9A45E),
            displayRadius = 13f,
            orbitRadius = 158f,
            orbitSpeed = 0.72f,
            spinSpeed = 0.18f,
            startAngle = 2.1f,
            description = "Wrapped in thick clouds of sulfuric acid, Venus is the hottest planet — " +
                "hotter even than Mercury — thanks to a runaway greenhouse effect. It also " +
                "spins backwards compared to most planets.",
            facts = listOf(
                Fact("Order from Sun", "2nd"),
                Fact("Diameter", "12,104 km"),
                Fact("Distance from Sun", "108 million km"),
                Fact("Day length", "243 Earth days"),
                Fact("Year length", "225 Earth days"),
                Fact("Moons", "0"),
                Fact("Surface temp.", "~465 °C")
            )
        ),
        CelestialBody(
            id = "earth",
            name = "Earth",
            type = BodyType.TERRESTRIAL,
            color = Color(0xFF4A8FE0),
            bandColor = Color(0xFF3FA66B),
            displayRadius = 14f,
            orbitRadius = 212f,
            orbitSpeed = 0.62f,
            spinSpeed = 0.5f,
            startAngle = 3.6f,
            description = "Our home — the only world known to harbour life. Liquid water covers " +
                "about 71% of its surface, and a protective magnetic field and atmosphere " +
                "shield it from the Sun's radiation.",
            facts = listOf(
                Fact("Order from Sun", "3rd"),
                Fact("Diameter", "12,742 km"),
                Fact("Distance from Sun", "150 million km"),
                Fact("Day length", "24 hours"),
                Fact("Year length", "365.25 days"),
                Fact("Moons", "1 (the Moon)")
            )
        ),
        CelestialBody(
            id = "mars",
            name = "Mars",
            type = BodyType.TERRESTRIAL,
            color = Color(0xFFD2603A),
            bandColor = Color(0xFF9C3F26),
            displayRadius = 11f,
            orbitRadius = 268f,
            orbitSpeed = 0.5f,
            spinSpeed = 0.48f,
            startAngle = 5.0f,
            description = "The 'Red Planet' gets its colour from iron oxide — rust — in its soil. " +
                "Mars hosts the tallest volcano and one of the deepest canyons in the solar " +
                "system, and is a prime target in the search for past life.",
            facts = listOf(
                Fact("Order from Sun", "4th"),
                Fact("Diameter", "6,779 km"),
                Fact("Distance from Sun", "228 million km"),
                Fact("Day length", "24.6 hours"),
                Fact("Year length", "687 Earth days"),
                Fact("Moons", "2 (Phobos, Deimos)")
            )
        ),
        CelestialBody(
            id = "jupiter",
            name = "Jupiter",
            type = BodyType.GAS_GIANT,
            color = Color(0xFFD9A066),
            bandColor = Color(0xFFA8703F),
            displayRadius = 34f,
            orbitRadius = 372f,
            orbitSpeed = 0.27f,
            spinSpeed = 0.9f,
            startAngle = 1.2f,
            description = "The giant of the solar system — so big that all the other planets could " +
                "fit inside it. Its Great Red Spot is a storm wider than Earth that has raged " +
                "for centuries.",
            facts = listOf(
                Fact("Order from Sun", "5th"),
                Fact("Diameter", "139,820 km"),
                Fact("Distance from Sun", "778 million km"),
                Fact("Day length", "~10 hours"),
                Fact("Year length", "12 Earth years"),
                Fact("Moons", "95+ (Io, Europa, …)")
            )
        ),
        CelestialBody(
            id = "saturn",
            name = "Saturn",
            type = BodyType.GAS_GIANT,
            color = Color(0xFFE0C68C),
            bandColor = Color(0xFFC2A05E),
            displayRadius = 30f,
            orbitRadius = 452f,
            orbitSpeed = 0.2f,
            spinSpeed = 0.8f,
            startAngle = 4.3f,
            hasRings = true,
            ringColor = Color(0xFFCBB789),
            description = "Famous for its dazzling rings — billions of chunks of ice and rock that " +
                "stretch wide but are only metres thick. Saturn is the least dense planet; it " +
                "would float in a big enough bathtub.",
            facts = listOf(
                Fact("Order from Sun", "6th"),
                Fact("Diameter", "116,460 km"),
                Fact("Distance from Sun", "1.4 billion km"),
                Fact("Day length", "~10.7 hours"),
                Fact("Year length", "29 Earth years"),
                Fact("Moons", "146+ (Titan, …)")
            )
        ),
        CelestialBody(
            id = "uranus",
            name = "Uranus",
            type = BodyType.ICE_GIANT,
            color = Color(0xFF9FE0E0),
            bandColor = Color(0xFF6FBFC4),
            displayRadius = 22f,
            orbitRadius = 524f,
            orbitSpeed = 0.14f,
            spinSpeed = 0.55f,
            startAngle = 0.9f,
            hasRings = true,
            ringColor = Color(0xFF7FA8B8),
            description = "An ice giant that rotates on its side, likely knocked over by an ancient " +
                "collision. Methane in its atmosphere gives Uranus its pale blue-green tint.",
            facts = listOf(
                Fact("Order from Sun", "7th"),
                Fact("Diameter", "50,724 km"),
                Fact("Distance from Sun", "2.9 billion km"),
                Fact("Day length", "~17 hours"),
                Fact("Year length", "84 Earth years"),
                Fact("Moons", "28")
            )
        ),
        CelestialBody(
            id = "neptune",
            name = "Neptune",
            type = BodyType.ICE_GIANT,
            color = Color(0xFF3F63C9),
            bandColor = Color(0xFF2C4699),
            displayRadius = 21f,
            orbitRadius = 590f,
            orbitSpeed = 0.11f,
            spinSpeed = 0.6f,
            startAngle = 3.0f,
            description = "The most distant planet — a deep-blue, windswept world with the fastest " +
                "winds in the solar system, reaching over 2,000 km/h. It was the first planet " +
                "found by mathematical prediction rather than observation.",
            facts = listOf(
                Fact("Order from Sun", "8th"),
                Fact("Diameter", "49,244 km"),
                Fact("Distance from Sun", "4.5 billion km"),
                Fact("Day length", "~16 hours"),
                Fact("Year length", "165 Earth years"),
                Fact("Moons", "16 (Triton, …)")
            )
        )
    )

    fun byId(id: String?): CelestialBody = bodies.firstOrNull { it.id == id } ?: bodies.first()

    /** Largest orbit radius, used to fit the whole system into view. */
    val maxOrbitRadius: Float = bodies.maxOf { it.orbitRadius + it.displayRadius }
}
