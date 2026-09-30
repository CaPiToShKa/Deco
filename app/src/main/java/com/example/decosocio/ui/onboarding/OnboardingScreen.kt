package com.example.decosocio.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.AppConfig
import com.example.decosocio.R
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.resolve
import com.example.decosocio.ui.privacy.ConsentRow
import com.example.decosocio.ui.privacy.ConsentsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * First-run privacy choices. Every marketing purpose starts OFF and is shown separately,
 * with equal-weight buttons; nothing is pre-ticked (GDPR art. 7, CNPD Diretriz 2022/1).
 * The Android notification permission is explained first and requested only on tap.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: ConsentsViewModel = koinViewModel(),
    push: PushRegistrar = koinInject(),
    config: AppConfig = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var permissionGranted by remember { mutableStateOf(push.notificationsPermitted()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        push.syncPushWithPermission()
    }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null && !state.saved) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.onboarding_title)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.loadError != null -> ErrorBox(state.loadError!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyMedium)

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                        Text(stringResource(R.string.onboarding_notifications_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.onboarding_notifications_body), style = MaterialTheme.typography.bodySmall)
                        if (permissionGranted) {
                            Text(
                                stringResource(R.string.onboarding_notifications_granted),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text(stringResource(R.string.onboarding_notifications_allow))
                            }
                        }
                    }
                }

                Text(stringResource(R.string.onboarding_marketing_heading), style = MaterialTheme.typography.titleMedium)
                ConsentPurpose.entries.forEach { purpose ->
                    ConsentRow(
                        purpose = purpose,
                        granted = state.draft[purpose] == true,
                        enabled = !state.saving,
                        onChange = { viewModel.toggle(purpose, it) },
                    )
                    HorizontalDivider()
                }
                Text(
                    stringResource(R.string.onboarding_withdraw_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { uriHandler.openUri(config.privacyPolicyUrl) }) {
                    Text(stringResource(R.string.action_privacy_policy))
                }
                // Two equal-weight choices: accepting nothing is as easy as saving a selection.
                Button(
                    onClick = { viewModel.save(ConsentSource.ONBOARDING) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.onboarding_save)) }
                OutlinedButton(
                    onClick = {
                        ConsentPurpose.entries.forEach { viewModel.toggle(it, false) }
                        viewModel.save(ConsentSource.ONBOARDING)
                    },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.onboarding_reject_all)) }
            }
        }
    }
}
