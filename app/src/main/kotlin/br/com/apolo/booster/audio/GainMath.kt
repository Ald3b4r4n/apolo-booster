package br.com.apolo.booster.audio

import kotlin.math.roundToInt

/**
 * Matematica de ganho, pura e testavel em JVM.
 *
 * Curva: 100% = 0 dB, 200% = +10 dB, 300% = +20 dB.
 * Ganho digital nunca passa de +20 dB; acima disso a distorcao destroi a experiencia.
 */
object GainMath {

    const val MIN_PERCENT = 100
    const val MAX_PERCENT = 300
    const val DEFAULT_PERCENT = 150
    const val SAFETY_WARNING_PERCENT = 200

    /** Teto absoluto aplicado ao LoudnessEnhancer (+20 dB). */
    const val MAX_GAIN_MB = 2000

    fun percentToDb(percent: Int): Float {
        val clamped = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
        return (clamped - MIN_PERCENT) / 10f
    }

    fun percentToMillibels(percent: Int): Int = (percentToDb(percent) * 100f).roundToInt()

    fun clampToDevice(requestedMb: Int, deviceMaxMb: Int): Int {
        val ceiling = minOf(deviceMaxMb, MAX_GAIN_MB).coerceAtLeast(0)
        return requestedMb.coerceIn(0, ceiling)
    }

    /** Converte um ganho de banda (-12..+12 dB) para o nivel nativo do equalizador. */
    fun bandGainToLevel(gainDb: Int, minLevel: Short, maxLevel: Short): Short {
        val normalized = (gainDb / 12f).coerceIn(-1f, 1f)
        val level = if (normalized >= 0f) normalized * maxLevel else normalized * -minLevel
        return level.roundToInt().toShort()
    }

    /** Percentual de 0..100 do volume do sistema, para a barra da UI. */
    fun volumeFraction(volume: Int, max: Int): Float {
        if (max <= 0) return 0f
        return (volume.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }
}
