package br.com.apolo.booster.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostState
import br.com.apolo.booster.audio.EqPresets
import br.com.apolo.booster.ui.components.ScreenHeader
import br.com.apolo.booster.ui.components.SectionCard
import br.com.apolo.booster.ui.theme.Gold
import br.com.apolo.booster.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.roundToInt

private val eqPresetLabels = listOf(
    EqPresets.FLAT to R.string.eq_preset_flat,
    EqPresets.MUSIC to R.string.eq_preset_music,
    EqPresets.HEAVY_METAL to R.string.eq_preset_heavy_metal,
    EqPresets.ROCK to R.string.eq_preset_rock,
    EqPresets.BASS to R.string.eq_preset_bass,
    EqPresets.VOCAL to R.string.eq_preset_vocal,
    EqPresets.ELECTRONIC to R.string.eq_preset_electronic,
)

@Composable
fun EqualizerScreen(
    state: BoostState,
    onUseEqualizerChange: (Boolean) -> Unit,
    onUseBassChange: (Boolean) -> Unit,
    onPreset: (Int) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.eq_title),
            subtitle = stringResource(R.string.eq_subtitle),
        )

        if (!state.support.equalizer) {
            SectionCard {
                Text(
                    text = "Este aparelho não expôs o Equalizer para o mix global.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        } else {
            SectionCard {
                val status = when {
                    !state.useEqualizer -> R.string.eq_status_off
                    !state.enabled -> R.string.eq_status_boost_off
                    state.eqGains.all { it == 0 } -> R.string.eq_status_flat
                    state.telemetry.equalizerEnabled && state.telemetry.equalizerControl ->
                        R.string.eq_status_enabled
                    else -> R.string.eq_status_unavailable
                }
                ModuleRow(
                    label = stringResource(R.string.eq_enable),
                    hint = stringResource(status),
                    checked = state.useEqualizer,
                    onCheckedChange = onUseEqualizerChange,
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                eqPresetLabels.forEach { (preset, labelRes) ->
                    FilterChip(
                        selected = state.eqPreset == preset,
                        onClick = { onPreset(preset) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            SectionCard(title = "Bandas") {
                state.eqGains.forEachIndexed { index, gain ->
                    BandRow(
                        index = index,
                        frequency = state.support.eqCenterFreqs.getOrElse(index) { 0 },
                        gain = gain,
                        onBandChange = onBandChange,
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.eq_bass)) {
            if (state.support.bassBoost) {
                val status = when {
                    !state.useBass -> R.string.eq_bass_off
                    !state.enabled -> R.string.eq_status_boost_off
                    state.bassStrength == 0 -> R.string.eq_bass_zero
                    !state.telemetry.bassEnabled || !state.telemetry.bassControl -> R.string.eq_status_unavailable
                    state.telemetry.bassStrengthSupported == false -> R.string.eq_bass_fixed
                    else -> R.string.eq_status_enabled
                }
                ModuleRow(
                    label = stringResource(R.string.eq_bass_enable),
                    hint = stringResource(status),
                    checked = state.useBass,
                    onCheckedChange = onUseBassChange,
                )
            } else {
                Text(stringResource(R.string.eq_bass_unavailable), color = TextMuted)
            }
            Text("${state.bassStrength}%", style = MaterialTheme.typography.titleMedium, color = Gold)
            Slider(
                value = state.bassStrength.toFloat(),
                onValueChange = { onBassChange(it.roundToInt()) },
                valueRange = 0f..100f,
                enabled = state.support.bassBoost,
            )
        }
    }
}

@Composable
private fun BandRow(
    index: Int,
    frequency: Int,
    gain: Int,
    onBandChange: (Int, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = frequencyLabel(frequency),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Text(
                text = if (gain > 0) "+$gain dB" else "$gain dB",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (gain == 0) TextMuted else Gold,
            )
        }
        Slider(
            value = gain.toFloat(),
            onValueChange = { onBandChange(index, it.roundToInt()) },
            valueRange = -12f..12f,
            steps = 23,
        )
    }
}

private fun frequencyLabel(frequency: Int): String = when {
    frequency <= 0 -> "—"
    frequency >= 1000 -> String.format(Locale.getDefault(), "%.1f kHz", frequency / 1000f)
    else -> "$frequency Hz"
}
