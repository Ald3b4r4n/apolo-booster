package br.com.apolo.booster.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BoostState(
    val enabled: Boolean = false,
    val percent: Int = GainMath.DEFAULT_PERCENT,
    val eqGains: List<Int> = emptyList(),
    val eqPreset: Int = EqPresets.FLAT,
    val bassStrength: Int = 0,
    val turbo: Boolean = false,
    val turboActive: Boolean = false,
    val support: EffectSupport = EffectSupport(),
    val systemVolume: Int = 0,
    val systemMaxVolume: Int = 1,
    val error: String? = null,
)

/**
 * Dono único dos AudioEffects do processo.
 *
 * Sessão 0 = mix global de saída: pega música, vídeo, jogos e áudio de outros apps
 * quando o OEM permite. Todo acesso é defensivo: efeito indisponível não derruba nada.
 */
object BoostController {

    private const val PREFS = "apolo_boost"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PERCENT = "percent"
    private const val KEY_EQ = "eq"
    private const val KEY_EQ_PRESET = "eq_preset"
    private const val KEY_BASS = "bass"

    private const val GLOBAL_SESSION = 0

    private lateinit var appContext: Context

    private val _state = MutableStateFlow(BoostState())
    val state: StateFlow<BoostState> = _state.asStateFlow()

    private var loudness: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var volumeBeforeTurbo = -1

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext

        val support = EffectProbe.probe()
        val prefs = prefs()
        val savedEq = readEq(prefs, support.eqBands)
        val manager = audioManager()

        _state.value = BoostState(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            percent = prefs.getInt(KEY_PERCENT, GainMath.DEFAULT_PERCENT)
                .coerceIn(GainMath.MIN_PERCENT, GainMath.MAX_PERCENT),
            eqGains = savedEq,
            eqPreset = prefs.getInt(KEY_EQ_PRESET, EqPresets.FLAT),
            bassStrength = prefs.getInt(KEY_BASS, 0).coerceIn(0, 100),
            turbo = false,
            support = support,
            systemVolume = runCatching { manager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0),
            systemMaxVolume = runCatching { manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1),
        )

        // Processo recriado com boost ligado: reaplica imediatamente.
        if (_state.value.enabled) applyAll()
    }

    fun setEnabled(enabled: Boolean) {
        update { it.copy(enabled = enabled, error = null) }
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) applyAll() else releaseEffects()
    }

    fun setPercent(percent: Int) {
        val clamped = percent.coerceIn(GainMath.MIN_PERCENT, GainMath.MAX_PERCENT)
        update { it.copy(percent = clamped) }
        prefs().edit().putInt(KEY_PERCENT, clamped).apply()
        if (_state.value.enabled) applyGain()
    }

    fun setEqGains(gains: List<Int>, preset: Int) {
        val bands = _state.value.support.eqBands
        val normalized = List(bands) { index -> (gains.getOrElse(index) { 0 }).coerceIn(-12, 12) }
        update { it.copy(eqGains = normalized, eqPreset = preset) }
        prefs().edit()
            .putString(KEY_EQ, normalized.joinToString(","))
            .putInt(KEY_EQ_PRESET, preset)
            .apply()
        if (_state.value.enabled) applyEqualizer()
    }

    fun setBandGain(band: Int, gainDb: Int) {
        val current = _state.value.eqGains.toMutableList()
        if (band !in current.indices) return
        current[band] = gainDb.coerceIn(-12, 12)
        setEqGains(current, -1)
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 100)
        update { it.copy(bassStrength = clamped) }
        prefs().edit().putInt(KEY_BASS, clamped).apply()
        if (_state.value.enabled) applyBass()
    }

    fun setTurbo(enabled: Boolean) {
        val manager = audioManager()
        if (enabled) {
            val result = VolumeProbe.tryExceed(manager)
            if (result.supported) volumeBeforeTurbo = result.volumeBefore
            update {
                it.copy(
                    turbo = true,
                    turboActive = result.supported,
                    systemVolume = currentVolume(),
                    systemMaxVolume = maxVolume(),
                )
            }
        } else {
            VolumeProbe.restore(manager, volumeBeforeTurbo)
            volumeBeforeTurbo = -1
            update {
                it.copy(turbo = false, turboActive = false, systemVolume = currentVolume())
            }
        }
    }

    fun refreshSystemVolume() {
        update { it.copy(systemVolume = currentVolume(), systemMaxVolume = maxVolume()) }
    }

    fun diagnosticsReport(): String {
        val st = _state.value
        val s = st.support
        return buildString {
            appendLine("Apolo Booster — diagnóstico")
            appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("LoudnessEnhancer: ${status(s.loudness)}")
            appendLine("Equalizer: ${if (s.equalizer) "disponível (${s.eqBands} bandas, ${s.eqMinLevel}..${s.eqMaxLevel} mB)" else "indisponível"}")
            appendLine("BassBoost: ${status(s.bassBoost)}")
            appendLine("DynamicsProcessing: ${status(s.dynamics)}")
            appendLine("Bandas: ${s.eqCenterFreqs.joinToString(", ")} Hz")
            appendLine()
            appendLine("Volume do sistema: ${st.systemVolume}/${st.systemMaxVolume}")
            appendLine("Turbo: ${turboLabel(st)}")
            appendLine("Boost: ${st.percent}% (~+${GainMath.percentToDb(st.percent)} dB)")
            appendLine("Boost ativo: ${status(st.enabled)}")
        }
    }

    private fun turboLabel(st: BoostState): String = when {
        !st.turbo -> "desligado"
        st.turboActive -> "ativo (passo extra aceito)"
        else -> "solicitado (aparelho recusou)"
    }

    private fun status(value: Boolean) = if (value) "disponível" else "indisponível"

    private fun applyAll() {
        val st = _state.value
        if (!st.support.hasAnyBoost) {
            update { it.copy(enabled = false, error = "Efeitos de áudio indisponíveis neste aparelho") }
            return
        }
        applyGain()
        applyEqualizer()
        applyBass()
    }

    private fun applyGain() {
        val st = _state.value
        if (!st.enabled || !st.support.loudness) return
        val effect = loudness ?: createLoudness() ?: return
        val millibels = GainMath.clampToDevice(
            GainMath.percentToMillibels(st.percent),
            GainMath.MAX_GAIN_MB,
        )
        runCatching {
            effect.setTargetGain(millibels)
            effect.setEnabled(true)
        }
    }

    private fun applyEqualizer() {
        val st = _state.value
        if (!st.enabled || !st.support.equalizer) return
        val effect = equalizer ?: createEqualizer() ?: return
        runCatching {
            st.eqGains.forEachIndexed { index, gain ->
                if (index < effect.numberOfBands) {
                    effect.setBandLevel(
                        index.toShort(),
                        GainMath.bandGainToLevel(gain, st.support.eqMinLevel, st.support.eqMaxLevel),
                    )
                }
            }
            effect.setEnabled(true)
        }
    }

    private fun applyBass() {
        val st = _state.value
        if (!st.enabled || !st.support.bassBoost) return
        val effect = bassBoost ?: createBass() ?: return
        runCatching {
            effect.setStrength((st.bassStrength * 10).coerceIn(0, 1000).toShort())
            effect.setEnabled(true)
        }
    }

    private fun createLoudness(): LoudnessEnhancer? = try {
        LoudnessEnhancer(GLOBAL_SESSION).also { loudness = it }
    } catch (_: Throwable) {
        null
    }

    private fun createEqualizer(): Equalizer? = try {
        Equalizer(0, GLOBAL_SESSION).also { equalizer = it }
    } catch (_: Throwable) {
        null
    }

    private fun createBass(): BassBoost? = try {
        BassBoost(0, GLOBAL_SESSION).also { bassBoost = it }
    } catch (_: Throwable) {
        null
    }

    private fun releaseEffects() {
        runCatching { loudness?.setEnabled(false) }
        runCatching { equalizer?.setEnabled(false) }
        runCatching { bassBoost?.setEnabled(false) }
        runCatching { loudness?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        loudness = null
        equalizer = null
        bassBoost = null
    }

    private fun readEq(prefs: SharedPreferences, bands: Int): List<Int> {
        if (bands <= 0) return emptyList()
        val raw = prefs.getString(KEY_EQ, null) ?: return List(bands) { 0 }
        val parsed = raw.split(',').mapNotNull { it.trim().toIntOrNull() }
        return List(bands) { index -> parsed.getOrElse(index) { 0 }.coerceIn(-12, 12) }
    }

    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun audioManager(): AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun currentVolume(): Int =
        runCatching { audioManager().getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)

    private fun maxVolume(): Int =
        runCatching { audioManager().getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1)

    private inline fun update(block: (BoostState) -> BoostState) {
        _state.value = block(_state.value)
    }
}
