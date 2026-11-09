package br.com.apolo.booster.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostController
import br.com.apolo.booster.audio.BoostState
import br.com.apolo.booster.audio.GainMath
import br.com.apolo.booster.ui.components.ScreenHeader
import br.com.apolo.booster.ui.components.SectionCard
import br.com.apolo.booster.ui.components.StatRow
import br.com.apolo.booster.ui.theme.Success
import br.com.apolo.booster.ui.theme.TextMuted

@Composable
fun DiagnosticsScreen(
    state: BoostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val support = state.support

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

        SectionCard(title = "Aparelho") {
            StatRow("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}")
            StatRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }

        SectionCard(title = "Efeitos de áudio") {
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
    }
}

private fun statusLabel(available: Boolean): String =
    if (available) "disponível" else "indisponível"
