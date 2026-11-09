package br.com.apolo.booster.audio

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Curvas em dB, interpoladas pela frequência central de cada banda.
 */
object EqPresets {

    const val FLAT = 0
    const val BASS = 1
    const val VOCAL = 2
    const val ROCK = 3
    const val ELECTRONIC = 4
    const val HEAVY_METAL = 5
    const val MUSIC = 6

    private val frequencies = listOf(60, 230, 910, 3600, 14000)
    private val reference = listOf(
        listOf(0, 0, 0, 0, 0),
        listOf(5, 3, 0, 0, 0),
        listOf(-3, -1, 3, 4, 1),
        listOf(3, 1, 1, 3, 2),
        listOf(5, 2, -1, 2, 3),
        listOf(4, -2, 2, 4, 1),
        listOf(2, 0, 0, 1, 2),
    )

    fun gains(preset: Int, bands: Int, centers: List<Int> = emptyList()): List<Int> {
        if (bands <= 0) return emptyList()
        val shape = reference.getOrElse(preset) { reference[FLAT] }
        return List(bands) { index ->
            val center = centers.getOrElse(index) { 0 }
            if (center <= 0) {
                val position = if (bands == 1) 2 else
                    (index * shape.lastIndex.toDouble() / (bands - 1)).roundToInt()
                shape[position]
            } else {
                gainAtFrequency(center, shape)
            }
        }
    }

    private fun gainAtFrequency(center: Int, shape: List<Int>): Int {
        if (center <= frequencies.first()) return shape.first()
        if (center >= frequencies.last()) return shape.last()
        val upper = frequencies.indexOfFirst { it >= center }
        val lower = upper - 1
        val from = frequencies[lower].toDouble()
        val fraction = ln(center / from) / ln(frequencies[upper] / from)
        return (shape[lower] + fraction * (shape[upper] - shape[lower])).roundToInt()
    }

    fun nameKey(preset: Int): String = when (preset) {
        FLAT -> "flat"
        BASS -> "bass"
        VOCAL -> "vocal"
        ROCK -> "rock"
        ELECTRONIC -> "electronic"
        HEAVY_METAL -> "heavy_metal"
        MUSIC -> "music"
        else -> "flat"
    }
}
