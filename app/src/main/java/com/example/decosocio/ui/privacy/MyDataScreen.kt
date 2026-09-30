package com.example.decosocio.ui.privacy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.R
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.SectionTitle
import com.example.decosocio.ui.common.resolve
import org.koin.androidx.compose.koinViewModel

@Composable
fun MyDataScreen(onBack: () -> Unit, viewModel: MyDataViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.saveTo(uri, context.contentResolver)
    }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.my_data_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.my_data_intro), style = MaterialTheme.typography.bodyMedium)
                SectionTitle(stringResource(R.string.my_data_categories_title))
                Text(stringResource(R.string.my_data_categories), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { saveLauncher.launch("dados-socio.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Text(stringResource(R.string.my_data_export), modifier = Modifier.padding(start = 8.dp))
                }
                SectionTitle(stringResource(R.string.my_data_preview_title))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = state.json.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                    )
                }
            }
        }
    }
}
