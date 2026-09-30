package com.example.decosocio.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.R
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.formatDate
import com.example.decosocio.ui.common.resolve
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, viewModel: DeleteAccountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.delete_title), onBack = if (state.receipt == null) onBack else null) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val receipt = state.receipt
            if (receipt == null) {
                Text(stringResource(R.string.delete_intro), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.delete_what_is_deleted), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.delete_what_is_kept), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.delete_subscription_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = state.understood, role = Role.Checkbox, onValueChange = viewModel::setUnderstood)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = state.understood, onCheckedChange = null)
                    Text(stringResource(R.string.delete_confirm_checkbox), modifier = Modifier.padding(start = 12.dp))
                }
                state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = viewModel::requestDeletion,
                    enabled = state.understood && !state.loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.delete_action)) }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.delete_done_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.delete_done_reference, receipt.requestId))
                        Text(stringResource(R.string.delete_done_deadline, formatDate(receipt.completesBy)))
                        Text(stringResource(R.string.delete_done_email), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = viewModel::finish, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_finish_and_logout))
                }
            }
        }
    }
}
