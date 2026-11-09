package br.com.apolo.booster.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tom de teste com o MESMO LoudnessEnhancer usado no app, anexado a sessao
 * de um AudioTrack proprio.
 *
 * Serve para provar, sem duvida, se o processamento funciona neste aparelho:
 * mover o ganho e ouvir (ou nao) a diferenca. Se o tom nao muda, o problema e
 * do sistema, nao da configuracao do mix global.
 */
class TonePlayer {

    private var track: AudioTrack? = null
    private var effect: LoudnessEnhancer? = null

    var isPlaying: Boolean = false
        private set

    fun start(gainMb: Int): Boolean {
        stop()
        return try {
            val sampleRate = 44_100
            val samples = ShortArray(sampleRate) { index ->
                (sin(2.0 * PI * 440.0 * index / sampleRate) * 6_000.0).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()

            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)

            val effect = runCatching { LoudnessEnhancer(track.audioSessionId) }.getOrNull()
            runCatching {
                effect?.setTargetGain(gainMb)
                effect?.setEnabled(true)
            }

            track.play()
            this.track = track
            this.effect = effect
            isPlaying = true
            true
        } catch (_: Throwable) {
            stop()
            false
        }
    }

    fun setGain(gainMb: Int) {
        runCatching { effect?.setTargetGain(gainMb) }
    }

    fun stop() {
        runCatching { effect?.setEnabled(false) }
        runCatching { effect?.release() }
        runCatching { track?.stop() }
        runCatching { track?.release() }
        effect = null
        track = null
        isPlaying = false
    }
}
