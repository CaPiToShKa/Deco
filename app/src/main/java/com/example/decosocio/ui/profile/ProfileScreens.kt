package com.example.decosocio.ui.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.AppConfig
import com.example.decosocio.R
import com.example.decosocio.domain.ProfileField
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.SectionTitle
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resolve
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onConsents: () -> Unit,
    onMyData: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDemoMenu: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
    config: AppConfig = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serviceNotifications by viewModel.serviceNotifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var permitted by remember { mutableStateOf(viewModel.notificationsPermitted()) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = granted
        viewModel.onNotificationPermissionResult()
    }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.profile_title)) }) { padding ->
        val profile = state.profile
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            profile == null -> ErrorBox(
                message = state.error ?: UiText.Res(R.string.error_generic),
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.profile_member_number, profile.memberNumber), style = MaterialTheme.typography.bodyMedium)
                        Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        profile.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        profile.address?.let {
                            Text("${it.street}, ${it.postalCode} ${it.city}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                NavRow(Icons.Outlined.Edit, stringResource(R.string.profile_edit), onClick = onEditProfile)

                SectionTitle(stringResource(R.string.profile_notifications_section), Modifier.padding(top = 16.dp))
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.profile_notifications_system)) },
                    supportingContent = {
                        Text(stringResource(if (permitted) R.string.profile_notifications_on else R.string.profile_notifications_off))
                    },
                    trailingContent = {
                        if (!permitted) {
                            TextButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                        )
                                    }
                                },
                            ) { Text(stringResource(R.string.action_enable)) }
                        }
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Switch) { viewModel.setServiceNotifications(!serviceNotifications) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.profile_service_notifications), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.profile_service_notifications_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = serviceNotifications, onCheckedChange = null)
                }

                SectionTitle(stringResource(R.string.profile_privacy_section), Modifier.padding(top = 16.dp))
                NavRow(Icons.Outlined.PrivacyTip, stringResource(R.string.consents_title), onClick = onConsents)
                NavRow(Icons.Outlined.Download, stringResource(R.string.my_data_title), onClick = onMyData)
                NavRow(Icons.Outlined.Policy, stringResource(R.string.action_privacy_policy), onClick = { uriHandler.openUri(config.privacyPolicyUrl) })
                NavRow(Icons.Outlined.DeleteForever, stringResource(R.string.delete_title), onClick = onDeleteAccount)

                SectionTitle(stringResource(R.string.profile_app_section), Modifier.padding(top = 16.dp))
                NavRow(Icons.Outlined.Language, stringResource(R.string.profile_language), onClick = { showLanguageDialog = true })
                NavRow(Icons.AutoMirrored.Outlined.Logout, stringResource(R.string.action_logout), onClick = viewModel::logout)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                // Tapping the version five times opens the presentation menu (demo scenarios, test push).
                Text(
                    text = stringResource(R.string.profile_version, config.versionName, config.backendMode.name.lowercase()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            versionTaps++
                            if (versionTaps >= 5) {
                                versionTaps = 0
                                onDemoMenu()
                            }
                        }
                        .padding(16.dp),
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }
}

@Composable
private fun NavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Per-app language (Android 13+ system setting, AppCompat back-port below). */
@Composable
private fun LanguageDialog(onDismiss: () -> Unit) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "pt-PT" to stringResource(R.string.language_portuguese),
        "en" to stringResource(R.string.language_english),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_language)) },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    val selected = current == tag || (tag.isNotEmpty() && current.startsWith(tag))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton) {
                                AppCompatDelegate.setApplicationLocales(
                                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                                )
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_back)) } },
    )
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.profile_edit), onBack = onBack) },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileTextField(state.firstName, ProfileField.FIRST_NAME, R.string.field_first_name, state.invalid, viewModel::edit)
                ProfileTextField(state.lastName, ProfileField.LAST_NAME, R.string.field_last_name, state.invalid, viewModel::edit)
                ProfileTextField(state.email, ProfileField.EMAIL, R.string.field_email, state.invalid, viewModel::edit, KeyboardType.Email)
                ProfileTextField(state.phone, ProfileField.PHONE, R.string.field_phone, state.invalid, viewModel::edit, KeyboardType.Phone)
                SectionTitle(stringResource(R.string.profile_address_section))
                ProfileTextField(state.street, ProfileField.STREET, R.string.field_street, state.invalid, viewModel::edit)
                ProfileTextField(state.postalCode, ProfileField.POSTAL_CODE, R.string.field_postal_code, state.invalid, viewModel::edit, KeyboardType.Number)
                ProfileTextField(state.city, ProfileField.CITY, R.string.field_city, state.invalid, viewModel::edit)
                Text(
                    stringResource(R.string.profile_edit_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    field: ProfileField,
    label: Int,
    invalid: Set<ProfileField>,
    onEdit: (ProfileField, String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val isError = field in invalid
    OutlinedTextField(
        value = value,
        onValueChange = { onEdit(field, it) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(field.errorRes())) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun ProfileField.errorRes(): Int = when (this) {
    ProfileField.FIRST_NAME, ProfileField.LAST_NAME, ProfileField.STREET, ProfileField.CITY -> R.string.field_error_required
    ProfileField.EMAIL -> R.string.field_error_email
    ProfileField.PHONE -> R.string.field_error_phone
    ProfileField.POSTAL_CODE -> R.string.field_error_postal_code
}
