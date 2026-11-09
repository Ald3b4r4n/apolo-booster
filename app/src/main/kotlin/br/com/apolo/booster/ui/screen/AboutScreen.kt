package br.com.apolo.booster.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.apolo.booster.R
import br.com.apolo.booster.ui.components.ScreenHeader
import br.com.apolo.booster.ui.components.SectionCard
import br.com.apolo.booster.ui.components.StatRow
import br.com.apolo.booster.ui.theme.Gold
import br.com.apolo.booster.ui.theme.TextMuted
import br.com.apolo.booster.ui.theme.TextPrimary

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { versionLabel(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.about_title),
            subtitle = stringResource(R.string.app_name),
        )

        SectionCard {
            Text(
                text = stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
        }

        SectionCard(title = "Privacidade") {
            Text(
                text = stringResource(R.string.about_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }

        SectionCard(title = "Versão") {
            StatRow("Apolo Booster", version)
            StatRow("Licença", "MIT")
        }

        Text(
            text = "Feito para ouvir melhor, não para ouvir mais alto do que o alto-falante suporta.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gold,
        )
    }
}

private fun versionLabel(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val name = info.versionName ?: "?"
    @Suppress("DEPRECATION")
    val code = info.versionCode
    "$name ($code)"
}.getOrDefault("?")
