package br.com.apolo.booster.audio

/**
 * Curvas de equalizacao em dB (-12..+12) para 5 bandas de referencia.
 * Sao interpoladas para a quantidade real de bandas do aparelho.
 */
object EqPresets {

    const val FLAT = 0
    const val BASS = 1
    const val VOCAL = 2
    const val ROCK = 3
    const val ELECTRONIC = 4

    private val reference = listOf(
        listOf(0, 0, 0, 0, 0),
        listOf(9, 6, 2, 0, -1),
        listOf(-2, 0, 5, 6, 1),
        listOf(6, 4, -2, 3, 5),
        listOf(8, 5, 0, 3, 6),
    )

    fun gains(preset: Int, bands: Int): List<Int> {
        if (bands <= 0) return emptyList()
        val shape = reference.getOrElse(preset) { reference[FLAT] }
        return List(bands) { index ->
            val position = index * shape.size / bands
            shape[position.coerceIn(0, shape.lastIndex)]
        }
    }

    fun nameKey(preset: Int): String = when (preset) {
        FLAT -> "flat"
        BASS -> "bass"
        VOCAL -> "vocal"
        ROCK -> "rock"
        ELECTRONIC -> "electronic"
        else -> "flat"
    }
}
