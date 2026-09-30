package com.example.decosocio.ui.privacy

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.R
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.EmptyState
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.SectionTitle
import com.example.decosocio.ui.common.currentLocale
import com.example.decosocio.ui.common.resolve
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun ConsentsScreen(onBack: () -> Unit, viewModel: ConsentsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }
    val locale = currentLocale()
    val timestampFormat = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.consents_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.loadError != null -> ErrorBox(state.loadError!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.consents_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(ConsentPurpose.entries, key = { it.name }) { purpose ->
                    ConsentRow(
                        purpose = purpose,
                        granted = state.draft[purpose] == true,
                        enabled = !state.saving,
                        onChange = { viewModel.toggle(purpose, it) },
                    )
                    HorizontalDivider()
                }
                item {
                    Button(
                        onClick = { viewModel.save(ConsentSource.SETTINGS) },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    ) { Text(stringResource(R.string.action_save)) }
                    SectionTitle(stringResource(R.string.consents_history_title))
                    if (state.history.isEmpty()) EmptyState(stringResource(R.string.consents_history_empty))
                }
                items(state.history) { record ->
                    ListItem(
                        headlineContent = { Text(stringResource(record.purpose.titleRes())) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    if (record.granted) R.string.consents_history_granted else R.string.consents_history_revoked,
                                    timestampFormat.format(Date(record.changedAtEpochMs)),
                                    record.textVersion,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}
