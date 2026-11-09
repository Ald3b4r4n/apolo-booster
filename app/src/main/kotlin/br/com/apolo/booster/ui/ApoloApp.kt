package br.com.apolo.booster.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.apolo.booster.R
import br.com.apolo.booster.audio.BoostController
import br.com.apolo.booster.audio.BoostState
import br.com.apolo.booster.audio.EqPresets
import br.com.apolo.booster.service.BoostService
import br.com.apolo.booster.ui.screen.AboutScreen
import br.com.apolo.booster.ui.screen.DiagnosticsScreen
import br.com.apolo.booster.ui.screen.EqualizerScreen
import br.com.apolo.booster.ui.screen.HomeScreen
import br.com.apolo.booster.ui.theme.Navy
import br.com.apolo.booster.ui.theme.SurfaceDeep

enum class ApoloDestination(val labelRes: Int, val icon: ImageVector) {
    Boost(R.string.nav_home, Icons.Default.Home),
    Equalizer(R.string.nav_equalizer, Icons.Default.List),
    Diagnostics(R.string.nav_diagnostics, Icons.Default.Info),
    About(R.string.nav_about, Icons.Default.AccountCircle),
}

@Composable
fun ApoloApp() {
    val context = LocalContext.current
    val state by BoostController.state.collectAsStateWithLifecycle()
    var destinationName by rememberSaveable { mutableStateOf(ApoloDestination.Boost.name) }
    val destination = ApoloDestination.valueOf(destinationName)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                BoostController.refreshSystemVolume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var pendingEnable by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (pendingEnable) {
            pendingEnable = false
            activate(context)
        }
    }

    fun toggleBoost(enabled: Boolean) {
        if (!enabled) {
            BoostController.setEnabled(false)
            BoostService.stop(context)
            return
        }
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            pendingEnable = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            activate(context)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
    ) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(containerColor = SurfaceDeep) {
                    Spacer(Modifier.height(24.dp))
                    ApoloDestination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destinationName = item.name },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
                ScreenHost(
                    destination = destination,
                    state = state,
                    onToggle = ::toggleBoost,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
            }
        } else {
            Scaffold(
                containerColor = Navy,
                bottomBar = {
                    NavigationBar(containerColor = SurfaceDeep) {
                        ApoloDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destinationName = item.name },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                },
            ) { padding ->
                ScreenHost(
                    destination = destination,
                    state = state,
                    onToggle = ::toggleBoost,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ScreenHost(
    destination: ApoloDestination,
    state: BoostState,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        ApoloDestination.Boost -> HomeScreen(
            state = state,
            onEnabledChange = onToggle,
            onPercentChange = BoostController::setPercent,
            onTurboChange = BoostController::setTurbo,
            modifier = modifier,
        )

        ApoloDestination.Equalizer -> EqualizerScreen(
            state = state,
            onPreset = { preset ->
                BoostController.setEqGains(
                    EqPresets.gains(preset, state.support.eqBands),
                    preset,
                )
            },
            onBandChange = BoostController::setBandGain,
            onBassChange = BoostController::setBassStrength,
            modifier = modifier,
        )

        ApoloDestination.Diagnostics -> DiagnosticsScreen(state = state, modifier = modifier)

        ApoloDestination.About -> AboutScreen(modifier = modifier)
    }
}

private fun activate(context: Context) {
    runCatching { BoostService.start(context) }
    BoostController.setEnabled(true)
}
