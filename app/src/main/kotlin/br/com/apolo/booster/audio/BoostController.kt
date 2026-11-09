package br.com.apolo.booster.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    val maximizeVolume: Boolean = true,
    val useLoudness: Boolean = true,
    val useEqualizer: Boolean = false,
    val useBass: Boolean = false,
    val support: EffectSupport = EffectSupport(),
    val systemVolume: Int = 0,
    val systemMaxVolume: Int = 1,
    val telemetry: BoostTelemetry = BoostTelemetry(),
    val error: String? = null,
)

/**
 * Dono único dos AudioEffects do processo.
 *
 * Sessão 0 = mix global de saída. No Android moderno isso é depreciado e alguns
 * OEMs simplesmente não inserem o efeito na cadeia: o objeto é criado, mas não
 * processa nada. Por isso existe telemetria e reconexão ao iniciar a música.
 */
object BoostController {

    private const val PREFS = "apolo_boost"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PERCENT = "percent"
    private const val KEY_EQ = "eq"
    private const val KEY_EQ_PRESET = "eq_preset"
    private const val KEY_BASS = "bass"
    private const val KEY_MAXIMIZE = "maximize_volume"
    private const val KEY_USE_LOUDNESS = "use_loudness"
    private const val KEY_USE_EQUALIZER = "use_equalizer"
    private const val KEY_USE_BASS = "use_bass"

    private const val GLOBAL_SESSION = 0
    private const val WATCHDOG_MS = 2_000L

    private lateinit var appContext: Context

    private val _state = MutableStateFlow(BoostState())
    val state: StateFlow<BoostState> = _state.asStateFlow()

    private var loudness: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var volumeBeforeTurbo = -1

    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = object : Runnable {
        override fun run() {
            if (!_state.value.enabled) return
            val active = runCatching { audioManager().isMusicActive }.getOrDefault(false)
            val previous = _state.value.telemetry.musicActive
            update {
                it.copy(
                    telemetry = it.telemetry.copy(
                        musicActive = active,
                        outputRoute = outputRoute(),
                    ),
                )
            }
            if (active && !previous) {
                reattach("música iniciou")
            }
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

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
            maximizeVolume = prefs.getBoolean(KEY_MAXIMIZE, true),
            useLoudness = prefs.getBoolean(KEY_USE_LOUDNESS, true),
            useEqualizer = prefs.getBoolean(KEY_USE_EQUALIZER, false),
            useBass = prefs.getBoolean(KEY_USE_BASS, false),
            turbo = false,
            support = support,
            systemVolume = runCatching { manager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0),
            systemMaxVolume = runCatching { manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(1),
            telemetry = BoostTelemetry(outputRoute = outputRoute()),
        )

        // Processo recriado com boost ligado: reaplica imediatamente.
        if (_state.value.enabled) {
            applyAll()
            handler.postDelayed(watchdog, WATCHDOG_MS)
        }
    }

    fun setEnabled(enabled: Boolean) {
        update { it.copy(enabled = enabled, error = null) }
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            applyAll()
            handler.removeCallbacks(watchdog)
            handler.postDelayed(watchdog, WATCHDOG_MS)
        } else {
            handler.removeCallbacks(watchdog)
            releaseEffects()
            refreshTelemetry("desligado")
        }
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

    fun setUseLoudness(enabled: Boolean) = setModule(KEY_USE_LOUDNESS, enabled) { it.copy(useLoudness = enabled) }

    fun setUseEqualizer(enabled: Boolean) = setModule(KEY_USE_EQUALIZER, enabled) { it.copy(useEqualizer = enabled) }

    fun setUseBass(enabled: Boolean) = setModule(KEY_USE_BASS, enabled) { it.copy(useBass = enabled) }

    private fun setModule(key: String, enabled: Boolean, transform: (BoostState) -> BoostState) {
        update(transform)
        prefs().edit().putBoolean(key, enabled).apply()
        if (_state.value.enabled) {
            releaseEffects()
            applyAll()
        }
    }

    /** Desliga tudo por alguns segundos para comparação auditiva honesta. */
    fun compareBypass(seconds: Int = 5) {
        if (!_state.value.enabled) return
        handler.removeCallbacks(watchdog)
        releaseEffects()
        update {
            it.copy(
                telemetry = it.telemetry.copy(
                    lastEvent = "bypass de ${seconds}s",
                    loudnessEnabled = false,
                    equalizerEnabled = false,
                    bassEnabled = false,
                ),
            )
        }
        handler.postDelayed({
            if (_state.value.enabled) {
                applyAll()
                handler.postDelayed(watchdog, WATCHDOG_MS)
            }
        }, seconds * 1_000L)
    }

    fun setMaximizeVolume(enabled: Boolean) {
        update { it.copy(maximizeVolume = enabled) }
        prefs().edit().putBoolean(KEY_MAXIMIZE, enabled).apply()
        if (enabled && _state.value.enabled) maximizeSystemVolume()
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
            refreshTelemetry(if (result.supported) "turbo aceito" else "turbo recusado")
        } else {
            VolumeProbe.restore(manager, volumeBeforeTurbo)
            volumeBeforeTurbo = -1
            update {
                it.copy(turbo = false, turboActive = false, systemVolume = currentVolume())
            }
            refreshTelemetry("turbo desligado")
        }
    }

    /** Libera e recria a cadeia. Util quando a música já estava tocando antes do boost. */
    fun reattach(reason: String) {
        if (!_state.value.enabled) return
        releaseEffects()
        update {
            it.copy(
                telemetry = it.telemetry.copy(
                    reattachCount = it.telemetry.reattachCount + 1,
                    lastEvent = reason,
                ),
            )
        }
        applyAll()
    }

    fun refreshSystemVolume() {
        update { it.copy(systemVolume = currentVolume(), systemMaxVolume = maxVolume()) }
        refreshTelemetry()
    }

    fun diagnosticsReport(): String {
        val st = _state.value
        val s = st.support
        val t = st.telemetry
        return buildString {
            appendLine("Apolo Booster — diagnóstico")
            appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("== Efeitos ==")
            appendLine("LoudnessEnhancer: ${status(s.loudness)}")
            appendLine("Equalizer: ${if (s.equalizer) "disponível (${s.eqBands} bandas, ${s.eqMinLevel}..${s.eqMaxLevel} mB)" else "indisponível"}")
            appendLine("BassBoost: ${status(s.bassBoost)}")
            appendLine("DynamicsProcessing: ${status(s.dynamics)}")
            appendLine("Bandas: ${s.eqCenterFreqs.joinToString(", ")} Hz")
            appendLine()
            appendLine("== Estado dos efeitos (sem medição de saída) ==")
            appendLine("Loudness criado: ${status(t.loudnessCreated)}")
            appendLine("Loudness com controle: ${status(t.loudnessControl)}")
            appendLine("Loudness habilitado: ${status(t.loudnessEnabled)}")
            appendLine("Loudness alvo: ${t.loudnessTargetMb} mB")
            appendLine("Loudness status do set: ${t.loudnessSetStatus}")
            appendLine("Equalizer com controle: ${status(t.equalizerControl)}")
            appendLine("Equalizer habilitado: ${status(t.equalizerEnabled)}")
            appendLine("Equalizer configurado (dB): ${st.eqGains.joinToString(", ")}")
            appendLine("BassBoost habilitado: ${status(t.bassEnabled)}")
            appendLine("BassBoost configurado: ${st.bassStrength}%")
            appendLine("Música ativa: ${status(t.musicActive)}")
            appendLine("Saídas disponíveis: ${t.outputRoute}")
            appendLine("Reconexões: ${t.reattachCount} (${t.lastEvent})")
            appendLine()
            appendLine("== Sessão ==")
            appendLine("Volume do sistema: ${st.systemVolume}/${st.systemMaxVolume}")
            appendLine("Volume máximo automático: ${status(st.maximizeVolume)}")
            appendLine("Módulos: loudness=${status(st.useLoudness)} eq=${status(st.useEqualizer)} bass=${status(st.useBass)}")
            appendLine("Turbo: ${turboLabel(st)}")
            appendLine("Boost solicitado: ${st.percent}% (+${GainMath.percentToDb(st.percent)} dB de ganho alvo)")
            appendLine("Boost ativo: ${status(st.enabled)}")
        }
    }

    private fun turboLabel(st: BoostState): String = when {
        !st.turbo -> "desligado"
        st.turboActive -> "ativo (passo extra aceito)"
        else -> "solicitado (aparelho recusou)"
    }

    private fun status(value: Boolean) = if (value) "sim" else "não"

    private fun applyAll() {
        val st = _state.value
        if (!st.support.hasAnyBoost) {
            update { it.copy(enabled = false, error = "Efeitos de áudio indisponíveis neste aparelho") }
            return
        }
        if (st.maximizeVolume) maximizeSystemVolume()
        if (st.useLoudness) applyGain()
        if (st.useEqualizer) applyEqualizer()
        if (st.useBass) applyBass()
        refreshTelemetry("cadeia aplicada")
    }

    private fun maximizeSystemVolume() {
        val maximum = maxVolume()
        runCatching {
            audioManager().setStreamVolume(AudioManager.STREAM_MUSIC, maximum, 0)
        }
        update { it.copy(systemVolume = currentVolume(), systemMaxVolume = maximum) }
    }

    private fun applyGain() {
        val st = _state.value
        if (!st.enabled || !st.useLoudness || !st.support.loudness) return
        val millibels = GainMath.clampToDevice(
            GainMath.percentToMillibels(st.percent),
            GainMath.MAX_GAIN_MB,
        )
        if (millibels == 0) {
            runCatching { loudness?.setEnabled(false) }
            refreshTelemetry("loudness em bypass")
            return
        }
        val effect = loudness ?: createLoudness() ?: return
        val setResult = runCatching { effect.setTargetGain(millibels) }
        val setStatus = if (setResult.isSuccess) 0 else -1
        val enableStatus = runCatching { effect.setEnabled(setResult.isSuccess) }.getOrDefault(Int.MIN_VALUE)
        update {
            it.copy(
                telemetry = it.telemetry.copy(
                    loudnessCreated = true,
                    loudnessSetStatus = setStatus,
                    lastEvent = "ganho $millibels mB · set=$setStatus · enable=$enableStatus",
                ),
            )
        }
        refreshTelemetry()
    }

    private fun applyEqualizer() {
        val st = _state.value
        if (!st.enabled || !st.useEqualizer || !st.support.equalizer) return
        if (st.eqGains.all { it == 0 }) {
            runCatching { equalizer?.setEnabled(false) }
            refreshTelemetry()
            return
        }
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
        refreshTelemetry()
    }

    private fun applyBass() {
        val st = _state.value
        if (!st.enabled || !st.useBass || !st.support.bassBoost) return
        if (st.bassStrength == 0) {
            runCatching { bassBoost?.setEnabled(false) }
            refreshTelemetry()
            return
        }
        val effect = bassBoost ?: createBass() ?: return
        runCatching {
            if (effect.strengthSupported) {
                effect.setStrength((st.bassStrength * 10).toShort())
            }
            effect.setEnabled(true)
        }
        refreshTelemetry()
    }

    private fun refreshTelemetry(event: String? = null) {
        val loud = loudness
        val eq = equalizer
        val bass = bassBoost
        update { st ->
            st.copy(
                telemetry = st.telemetry.copy(
                    loudnessCreated = loud != null,
                    loudnessControl = controlOf(loud),
                    loudnessEnabled = enabledOf(loud),
                    loudnessTargetMb = targetGainOf(loud),
                    equalizerControl = controlOf(eq),
                    equalizerEnabled = enabledOf(eq),
                    bassControl = controlOf(bass),
                    bassEnabled = enabledOf(bass),
                    musicActive = runCatching { audioManager().isMusicActive }.getOrDefault(false),
                    outputRoute = outputRoute(),
                    lastEvent = event ?: st.telemetry.lastEvent,
                ),
            )
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

    private fun outputRoute(): String {
        val devices = runCatching {
            audioManager().getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrDefault(emptyArray())
        val labels = devices.map { typeName(it.type) }.distinct()
        return labels.joinToString(", ").ifBlank { "-" }
    }

    private fun typeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "alto-falante"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "auricular"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "fone com fio"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "headset"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
        else -> "tipo $type"
    }

    private fun controlOf(effect: android.media.audiofx.AudioEffect?): Boolean =
        effect?.let { runCatching { it.hasControl() }.getOrDefault(false) } ?: false

    private fun enabledOf(effect: android.media.audiofx.AudioEffect?): Boolean =
        effect?.let { runCatching { it.getEnabled() }.getOrDefault(false) } ?: false

    private fun targetGainOf(effect: LoudnessEnhancer?): Int =
        effect?.let { instance -> runCatching { instance.getTargetGain().toInt() }.getOrDefault(0) } ?: 0

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
