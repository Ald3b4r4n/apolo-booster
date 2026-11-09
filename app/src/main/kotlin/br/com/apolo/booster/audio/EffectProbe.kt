package br.com.apolo.booster.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build

/**
 * Capacidades reais do aparelho. Cada efeito e criado e liberado na hora:
 * se falhar, o app sabe que nao pode contar com ele.
 */
data class EffectSupport(
    val loudness: Boolean = false,
    val equalizer: Boolean = false,
    val bassBoost: Boolean = false,
    val dynamics: Boolean = false,
    val eqBands: Int = 0,
    val eqMinLevel: Short = 0,
    val eqMaxLevel: Short = 0,
    val eqCenterFreqs: List<Int> = emptyList(),
) {
    val hasAnyBoost: Boolean get() = loudness || equalizer || bassBoost
}

object EffectProbe {

    private const val GLOBAL_SESSION = 0

    fun probe(): EffectSupport {
        var loudness = false
        var equalizer = false
        var bass = false
        var bands = 0
        var minLevel: Short = 0
        var maxLevel: Short = 0
        var centers: List<Int> = emptyList()

        try {
            LoudnessEnhancer(GLOBAL_SESSION).useEffect { loudness = true }
        } catch (_: Throwable) {
            loudness = false
        }

        try {
            Equalizer(0, GLOBAL_SESSION).useEffect { eq ->
                equalizer = true
                bands = eq.numberOfBands.toInt()
                val range = runCatching { eq.bandLevelRange }.getOrNull()
                if (range != null && range.size >= 2) {
                    minLevel = range[0]
                    maxLevel = range[1]
                }
                centers = runCatching {
                    (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
                }.getOrDefault(emptyList())
            }
        } catch (_: Throwable) {
            equalizer = false
        }

        try {
            BassBoost(0, GLOBAL_SESSION).useEffect { bass = true }
        } catch (_: Throwable) {
            bass = false
        }

        val dynamics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                AudioEffect.queryEffects()?.any {
                    it.type == AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING
                } == true
            }.getOrDefault(false)
        } else {
            false
        }

        return EffectSupport(
            loudness = loudness,
            equalizer = equalizer,
            bassBoost = bass,
            dynamics = dynamics,
            eqBands = bands,
            eqMinLevel = minLevel,
            eqMaxLevel = maxLevel,
            eqCenterFreqs = centers,
        )
    }

    private inline fun <T : AudioEffect> T.useEffect(block: (T) -> Unit) {
        try {
            block(this)
        } finally {
            runCatching { setEnabled(false) }
            runCatching { release() }
        }
    }
}

