package br.com.apolo.booster.audio

import android.media.AudioManager

/**
 * Sonda os passos de volume "ocultos" de alguns OEMs.
 *
 * A maioria dos aparelhos recusa indice acima do maximo. Quando aceita, o ganho
 * e de hardware e vale a pena; quando recusa, restauramos o volume original.
 */
object VolumeProbe {

    data class Result(val supported: Boolean, val volumeBefore: Int, val volumeAfter: Int)

    fun tryExceed(manager: AudioManager): Result {
        val stream = AudioManager.STREAM_MUSIC
        val before = runCatching { manager.getStreamVolume(stream) }.getOrDefault(0)
        val max = runCatching { manager.getStreamMaxVolume(stream) }.getOrDefault(0)
        return try {
            manager.setStreamVolume(stream, max + 1, 0)
            val after = manager.getStreamVolume(stream)
            val exceeded = after > max
            if (!exceeded) {
                runCatching { manager.setStreamVolume(stream, before, 0) }
            }
            Result(exceeded, before, after)
        } catch (_: Throwable) {
            runCatching { manager.setStreamVolume(stream, before, 0) }
            Result(false, before, before)
        }
    }

    fun restore(manager: AudioManager, volume: Int) {
        if (volume < 0) return
        runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) }
    }
}
