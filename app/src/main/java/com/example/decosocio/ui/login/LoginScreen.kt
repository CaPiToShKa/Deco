package com.example.decosocio.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.AppConfig
import com.example.decosocio.R
import com.example.decosocio.ui.common.resolve
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun LoginScreen(
    onLoggedIn: (needsOnboarding: Boolean) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
    config: AppConfig = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.next) {
        state.next?.let {
            onLoggedIn(it == AfterLogin.ONBOARDING)
            viewModel.onNavigated()
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            // Brand slot: replace res/drawable/brand_logo.xml with the official logo.
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = stringResource(R.string.brand_logo_description),
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.field_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                isError = state.error != null,
                supportingText = if (state.error != null) {
                    { Text(state.error!!.resolve()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::login,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_login))
                }
            }
            if (config.isDemo) {
                TextButton(onClick = viewModel::fillDemoAccount) {
                    Text(stringResource(R.string.login_use_demo_account))
                }
                Text(
                    text = stringResource(R.string.login_demo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
