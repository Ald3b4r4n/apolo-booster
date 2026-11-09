package br.com.apolo.booster.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostState
import br.com.apolo.booster.audio.GainMath
import br.com.apolo.booster.ui.components.BoostDial
import br.com.apolo.booster.ui.components.SectionCard
import br.com.apolo.booster.ui.components.StatRow
import br.com.apolo.booster.ui.theme.Caution
import br.com.apolo.booster.ui.theme.Danger
import br.com.apolo.booster.ui.theme.Ember
import br.com.apolo.booster.ui.theme.Gold
import br.com.apolo.booster.ui.theme.Outline
import br.com.apolo.booster.ui.theme.Success
import br.com.apolo.booster.ui.theme.TextMuted
import br.com.apolo.booster.ui.theme.TextPrimary
import kotlin.math.roundToInt

private val presets = listOf(
    R.string.home_preset_auto to 180,
    R.string.home_preset_headphone to 160,
    R.string.home_preset_bluetooth to 200,
    R.string.home_preset_max to 250,
)

@Composable
fun HomeScreen(
    state: BoostState,
    onEnabledChange: (Boolean) -> Unit,
    onPercentChange: (Int) -> Unit,
    onTurboChange: (Boolean) -> Unit,
    onMaximizeVolumeChange: (Boolean) -> Unit,
    onUseLoudnessChange: (Boolean) -> Unit,
    onUseEqualizerChange: (Boolean) -> Unit,
    onUseBassChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    letterSpacing = 3.sp,
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                )
            }
            StatusPill(enabled = state.enabled)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoostDial(
                percent = state.percent,
                enabled = state.enabled,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f),
            )
        }

        Slider(
            value = state.percent.toFloat(),
            onValueChange = { onPercentChange(it.roundToInt()) },
            valueRange = GainMath.MIN_PERCENT.toFloat()..GainMath.MAX_PERCENT.toFloat(),
            steps = 39,
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = Outline,
            ),
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = state.percent == preset.second,
                    onClick = { onPercentChange(preset.second) },
                    label = { Text(stringResource(preset.first)) },
                )
            }
        }

        Button(
            onClick = { onEnabledChange(!state.enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.enabled) Ember else Gold,
                contentColor = if (state.enabled) Color.White else Color(0xFF1A1200),
            ),
        ) {
            Text(
                text = if (state.enabled) {
                    stringResource(R.string.home_master_off)
                } else {
                    stringResource(R.string.home_master_on)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SectionCard {
            StatRow(
                label = stringResource(R.string.home_system_volume),
                value = "${state.systemVolume}/${state.systemMaxVolume}",
            )
            LinearProgressIndicator(
                progress = { GainMath.volumeFraction(state.systemVolume, state.systemMaxVolume) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Gold,
                trackColor = Outline,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_maximize_volume),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.home_maximize_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = state.maximizeVolume,
                    onCheckedChange = onMaximizeVolumeChange,
                )
            }
        }

        SectionCard(title = stringResource(R.string.home_modules)) {
            Text(
                text = stringResource(R.string.home_modules_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            ModuleRow(
                label = stringResource(R.string.home_module_loudness),
                hint = stringResource(R.string.home_module_loudness_hint),
                checked = state.useLoudness,
                onCheckedChange = onUseLoudnessChange,
            )
            ModuleRow(
                label = stringResource(R.string.home_module_equalizer),
                hint = stringResource(R.string.home_module_equalizer_hint),
                checked = state.useEqualizer,
                onCheckedChange = onUseEqualizerChange,
            )
            ModuleRow(
                label = stringResource(R.string.home_module_bass),
                hint = stringResource(R.string.home_module_bass_hint),
                checked = state.useBass,
                onCheckedChange = onUseBassChange,
            )
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_turbo),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_turbo_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
                Switch(checked = state.turbo, onCheckedChange = onTurboChange)
            }
        }

        if (state.percent >= GainMath.SAFETY_WARNING_PERCENT) {
            SectionCard(accent = Caution) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Caution)
                    Column {
                        Text(
                            text = stringResource(R.string.home_safety_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.home_safety_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                }
            }
        }

        state.error?.let { message ->
            SectionCard(accent = Danger) {
                Text(text = message, color = Danger, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ModuleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusPill(enabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) Success.copy(alpha = 0.14f) else Outline.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Success else TextMuted),
            )
            Text(
                text = if (enabled) "ATIVO" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Success else TextMuted,
            )
        }
    }
}
