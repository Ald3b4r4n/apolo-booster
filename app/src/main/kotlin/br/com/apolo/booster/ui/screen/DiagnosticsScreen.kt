package br.com.apolo.booster.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostController
import br.com.apolo.booster.audio.BoostState
import br.com.apolo.booster.audio.GainMath
import br.com.apolo.booster.audio.TonePlayer
import br.com.apolo.booster.ui.components.ScreenHeader
import br.com.apolo.booster.ui.components.SectionCard
import br.com.apolo.booster.ui.components.StatRow
import br.com.apolo.booster.ui.theme.Gold
import br.com.apolo.booster.ui.theme.TextMuted
import kotlin.math.roundToInt

@Composable
fun DiagnosticsScreen(
    state: BoostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val support = state.support
    val telemetry = state.telemetry

    val tonePlayer = remember { TonePlayer() }
    var tonePlaying by remember { mutableStateOf(false) }
    var toneGainDb by remember { mutableStateOf(10f) }

    DisposableEffect(Unit) {
        onDispose { tonePlayer.stop() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.diag_title),
            subtitle = stringResource(R.string.diag_subtitle),
        )

        SectionCard(title = stringResource(R.string.diag_tone)) {
            Text(
                text = stringResource(R.string.diag_tone_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.diag_tone_gain)}: +${toneGainDb.roundToInt()} dB",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                )
            }
            Slider(
                value = toneGainDb,
                onValueChange = {
                    toneGainDb = it
                    if (tonePlaying) tonePlayer.setGain((it * 100f).roundToInt())
                },
                valueRange = 0f..20f,
                steps = 19,
            )
            Button(
                onClick = {
                    if (tonePlaying) {
                        tonePlayer.stop()
                        tonePlaying = false
                    } else {
                        val started = tonePlayer.start((toneGainDb * 100f).roundToInt())
                        tonePlaying = started
                        if (!started) {
                            Toast.makeText(
                                context,
                                R.string.diag_tone_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (tonePlaying) {
                        stringResource(R.string.diag_tone_stop)
                    } else {
                        stringResource(R.string.diag_tone_play)
                    },
                )
            }
        }

        SectionCard(title = stringResource(R.string.diag_telemetry)) {
            StatRow("Loudness criado", statusLabel(telemetry.loudnessCreated))
            StatRow("Loudness com controle", statusLabel(telemetry.loudnessControl))
            StatRow("Loudness habilitado", statusLabel(telemetry.loudnessEnabled))
            StatRow("Ganho alvo", "${telemetry.loudnessTargetMb} mB")
            StatRow("Status do setTargetGain", telemetry.loudnessSetStatus.toString())
            StatRow("Equalizer habilitado", statusLabel(telemetry.equalizerEnabled))
            StatRow("BassBoost habilitado", statusLabel(telemetry.bassEnabled))
            StatRow("Música ativa", statusLabel(telemetry.musicActive))
            StatRow("Saída", telemetry.outputRoute)
            StatRow("Reconexões", "${telemetry.reattachCount} · ${telemetry.lastEvent}")
        }

        OutlinedButton(
            onClick = {
                BoostController.reattach("manual")
                Toast.makeText(context, R.string.diag_reconnected, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.diag_reconnect))
        }

        OutlinedButton(
            onClick = { BoostController.compareBypass(5) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.diag_compare))
        }

        SectionCard(title = "Efeitos disponíveis") {
            StatRow("LoudnessEnhancer", statusLabel(support.loudness))
            StatRow(
                "Equalizer",
                if (support.equalizer) "${support.eqBands} bandas" else statusLabel(false),
            )
            StatRow("BassBoost", statusLabel(support.bassBoost))
            StatRow("DynamicsProcessing", if (support.dynamics) "presente" else "ausente")
            if (support.eqCenterFreqs.isNotEmpty()) {
                StatRow("Frequências", support.eqCenterFreqs.joinToString(" · ") + " Hz")
            }
            if (support.equalizer) {
                StatRow("Faixa do EQ", "${support.eqMinLevel}..${support.eqMaxLevel} mB")
            }
        }

        SectionCard(title = "Aparelho") {
            StatRow("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}")
            StatRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }

        SectionCard(title = "Sessão") {
            StatRow("Volume do sistema", "${state.systemVolume}/${state.systemMaxVolume}")
            StatRow(
                "Turbo de hardware",
                when {
                    !state.turbo -> "desligado"
                    state.turboActive -> "ativo"
                    else -> "recusado pelo aparelho"
                },
            )
            StatRow("Boost", "${state.percent}% · +${GainMath.percentToDb(state.percent)} dB")
            StatRow("Ativo", if (state.enabled) "sim" else "não")
        }

        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("apolo-booster-diagnostico", BoostController.diagnosticsReport()),
                )
                Toast.makeText(context, R.string.diag_copied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.diag_copy))
        }

        Text(
            text = "Cole este relatório na issue de feedback do GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )

        Text(
            text = "Dica: se o tom de teste mudar de volume e o áudio de outros apps não, o problema é o mix global do aparelho, não o app.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Gold,
        )
    }
}

private fun statusLabel(value: Boolean): String = if (value) "sim" else "não"
