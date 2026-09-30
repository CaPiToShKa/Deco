package com.example.decosocio.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.AppConfig
import com.example.decosocio.R
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.demo.DemoSettings
import com.example.decosocio.data.demo.SubscriptionScenario
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.push.PushState
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.SectionTitle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Presentation controls. Scenario switches only affect the in-memory demo backend. */
class DemoMenuViewModel(
    private val control: DemoControl,
    private val push: PushRegistrar,
    val config: AppConfig,
) : ViewModel() {
    val settings: StateFlow<DemoSettings> = control.settings
    val pushState: StateFlow<PushState> = push.state

    fun setScenario(scenario: SubscriptionScenario) = control.setSubscription(scenario)
    fun setNetworkError(enabled: Boolean) = control.setNetworkError(enabled)
    fun setEmptyCoupons(enabled: Boolean) = control.setEmptyCoupons(enabled)
    fun resetData() = control.resetData()
    fun sendTestPush(title: String, body: String): Boolean = push.showLocalTestNotification(title, body)
}

@Composable
fun DemoMenuScreen(onBack: () -> Unit, viewModel: DemoMenuViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pushState by viewModel.pushState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pushTitle = stringResource(R.string.demo_test_push_title)
    val pushBody = stringResource(R.string.demo_test_push_body)
    val pushSent = stringResource(R.string.demo_test_push_sent)
    val pushBlocked = stringResource(R.string.demo_test_push_blocked)
    val isDemo = viewModel.config.isDemo

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.demo_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isDemo) {
                Text(stringResource(R.string.demo_bff_mode_note), style = MaterialTheme.typography.bodyMedium)
            }
            SectionTitle(stringResource(R.string.demo_subscription_section))
            SubscriptionScenario.entries.forEach { scenario ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.subscription == scenario,
                            enabled = isDemo,
                            role = Role.RadioButton,
                        ) { viewModel.setScenario(scenario) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.subscription == scenario, onClick = null, enabled = isDemo)
                    Text(stringResource(scenario.labelRes()), modifier = Modifier.padding(start = 12.dp))
                }
            }

            SectionTitle(stringResource(R.string.demo_states_section))
            SwitchRow(stringResource(R.string.demo_network_error), settings.networkError, isDemo, viewModel::setNetworkError)
            SwitchRow(stringResource(R.string.demo_empty_coupons), settings.emptyCoupons, isDemo, viewModel::setEmptyCoupons)
            OutlinedButton(onClick = viewModel::resetData, enabled = isDemo) {
                Text(stringResource(R.string.demo_reset))
            }

            SectionTitle(stringResource(R.string.demo_push_section))
            Button(onClick = {
                val sent = viewModel.sendTestPush(pushTitle, pushBody)
                scope.launch { snackbar.showSnackbar(if (sent) pushSent else pushBlocked) }
            }) { Text(stringResource(R.string.demo_test_push)) }

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val lines = listOf(
                        "backend = ${viewModel.config.backendMode}",
                        "bff = ${viewModel.config.bffBaseUrl.ifBlank { "-" }}",
                        "news = ${viewModel.config.newsFeedUrl.ifBlank { "sample" }}",
                        "sfmc.configured = ${pushState.sfmcConfigured}",
                        "sfmc.sdkReady = ${pushState.sdkReady}",
                        "sfmc.error = ${pushState.sdkError ?: "-"}",
                        "push.requested = ${pushState.pushRequested}",
                        "contactKey = ${pushState.contactKey ?: "-"}",
                    )
                    lines.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private fun SubscriptionScenario.labelRes(): Int = when (this) {
    SubscriptionScenario.ACTIVE -> R.string.status_active
    SubscriptionScenario.EXPIRING -> R.string.status_expiring
    SubscriptionScenario.EXPIRED -> R.string.status_expired
}
