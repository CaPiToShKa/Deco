package com.example.decosocio.ui.membership

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.AppConfig
import com.example.decosocio.R
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnKind
import com.example.decosocio.domain.model.AddOnRules
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.SubscriptionStatus
import com.example.decosocio.domain.model.WithdrawalReceipt
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.InfoBanner
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.SectionTitle
import com.example.decosocio.ui.common.StatusPill
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.formatDate
import com.example.decosocio.ui.common.formatMoney
import com.example.decosocio.ui.common.formatPrice
import com.example.decosocio.ui.common.resolve
import com.example.decosocio.ui.theme.LocalStatusColors
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MembershipScreen(
    viewModel: MembershipViewModel = koinViewModel(),
    config: AppConfig = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.membership_title),
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val subscription = state.subscription
        val status = state.status
        val today = state.today
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            subscription == null || status == null || today == null ->
                ErrorBox(
                    message = state.error ?: UiText.Res(R.string.error_generic),
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(padding),
                )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SubscriptionCard(
                        subscription = subscription,
                        status = status,
                        daysLeft = state.daysLeft,
                        renewUrl = config.renewUrl,
                    )
                }
                if (status == SubscriptionStatus.EXPIRED) {
                    item { InfoBanner(stringResource(R.string.membership_expired_banner), icon = Icons.Outlined.WarningAmber) }
                }
                item { SectionTitle(stringResource(R.string.addons_title)) }
                item {
                    Text(
                        stringResource(R.string.addons_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.addOns, key = { it.id }) { addOn ->
                    AddOnCard(
                        addOn = addOn,
                        today = today,
                        subscriptionExpired = status == SubscriptionStatus.EXPIRED,
                        busy = state.busyAddOnId == addOn.id,
                        onAction = viewModel::ask,
                    )
                }
            }
        }
    }

    state.confirm?.let { action ->
        ConfirmActionDialog(action, onConfirm = viewModel::confirm, onDismiss = viewModel::dismissConfirm)
    }
    state.receipt?.let { receipt ->
        WithdrawalReceiptDialog(receipt, onDismiss = viewModel::dismissReceipt)
    }
}

@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    status: SubscriptionStatus,
    daysLeft: Int,
    renewUrl: String,
) {
    val colors = LocalStatusColors.current
    val uriHandler = LocalUriHandler.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subscription.plan.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                when (status) {
                    SubscriptionStatus.ACTIVE -> StatusPill(stringResource(R.string.status_active), colors.positive, colors.onPositive)
                    SubscriptionStatus.EXPIRING -> StatusPill(stringResource(R.string.status_expiring), colors.warning, colors.onWarning)
                    SubscriptionStatus.EXPIRED -> StatusPill(stringResource(R.string.status_expired), colors.negative, colors.onNegative)
                }
            }
            Text(formatPrice(subscription.plan.priceCents, subscription.plan.period), style = MaterialTheme.typography.bodyLarge)
            val renewal = formatDate(subscription.renewalDate)
            Text(
                when (status) {
                    SubscriptionStatus.EXPIRED -> stringResource(R.string.membership_expired_on, renewal)
                    SubscriptionStatus.EXPIRING -> stringResource(R.string.membership_expires_on, renewal) + " · " +
                        pluralStringResource(R.plurals.days_left, daysLeft, daysLeft)
                    SubscriptionStatus.ACTIVE -> if (subscription.autoRenew) {
                        stringResource(R.string.membership_renews_on, renewal)
                    } else {
                        stringResource(R.string.membership_valid_until, renewal)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    if (subscription.autoRenew) R.string.membership_auto_renew_on else R.string.membership_auto_renew_off,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.membership_payment_method, subscription.paymentMethodLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status != SubscriptionStatus.ACTIVE) {
                Button(onClick = { uriHandler.openUri(renewUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.membership_renew))
                }
                Text(
                    stringResource(R.string.membership_renew_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddOnCard(
    addOn: AddOn,
    today: LocalDate,
    subscriptionExpired: Boolean,
    busy: Boolean,
    onAction: (AddOnAction) -> Unit,
) {
    val colors = LocalStatusColors.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(addOn.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (addOn.state) {
                    AddOnState.Available -> Unit
                    is AddOnState.Active -> StatusPill(stringResource(R.string.addon_state_active), colors.positive, colors.onPositive)
                    is AddOnState.CancellationRequested -> StatusPill(stringResource(R.string.addon_state_ending), colors.warning, colors.onWarning)
                }
            }
            Text(
                formatPrice(addOn.priceCents, addOn.period) + " · " + stringResource(addOn.kind.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(addOn.description, style = MaterialTheme.typography.bodyMedium)

            when (val state = addOn.state) {
                AddOnState.Available -> {
                    if (addOn.kind == AddOnKind.DIGITAL) {
                        Text(
                            stringResource(R.string.addon_digital_billing_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { onAction(AddOnAction.Activate(addOn)) },
                        enabled = !busy && !subscriptionExpired,
                    ) { Text(stringResource(R.string.addon_activate)) }
                }
                is AddOnState.Active -> {
                    val canWithdraw = AddOnRules.canWithdraw(addOn, today)
                    Text(
                        if (canWithdraw) {
                            stringResource(R.string.addon_active_since_withdraw, formatDate(state.activatedOn), formatDate(state.withdrawalDeadline))
                        } else {
                            stringResource(R.string.addon_active_since, formatDate(state.activatedOn))
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRowActions {
                        if (canWithdraw) {
                            // Directive (EU) 2023/2673: a clearly labelled withdrawal function.
                            OutlinedButton(onClick = { onAction(AddOnAction.Withdraw(addOn)) }, enabled = !busy) {
                                Text(stringResource(R.string.addon_withdraw))
                            }
                        }
                        TextButton(onClick = { onAction(AddOnAction.Cancel(addOn)) }, enabled = !busy) {
                            Text(stringResource(R.string.addon_cancel))
                        }
                    }
                }
                is AddOnState.CancellationRequested -> {
                    Text(
                        stringResource(R.string.addon_ends_on, formatDate(state.endsOn)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { onAction(AddOnAction.UndoCancel(addOn)) }, enabled = !busy) {
                        Text(stringResource(R.string.addon_undo_cancel))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowActions(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

private fun AddOnKind.labelRes(): Int = when (this) {
    AddOnKind.DIGITAL -> R.string.addon_kind_digital
    AddOnKind.SERVICE -> R.string.addon_kind_service
    AddOnKind.INSURANCE -> R.string.addon_kind_insurance
    AddOnKind.ADVICE -> R.string.addon_kind_advice
}

@Composable
private fun ConfirmActionDialog(action: AddOnAction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val addOn = action.addOn
    val price = formatPrice(addOn.priceCents, addOn.period)
    val (title, body, confirmLabel) = when (action) {
        is AddOnAction.Activate -> Triple(
            stringResource(R.string.addon_confirm_activate_title, addOn.name),
            stringResource(R.string.addon_confirm_activate_body, price, AddOnRules.WITHDRAWAL_DAYS),
            // EU button rule (Directive 2011/83 art. 8(2)): the order button must say it implies payment.
            stringResource(R.string.addon_confirm_activate_button),
        )
        is AddOnAction.Withdraw -> Triple(
            stringResource(R.string.addon_confirm_withdraw_title),
            stringResource(R.string.addon_confirm_withdraw_body, addOn.name, formatMoney(addOn.priceCents)),
            stringResource(R.string.addon_confirm_withdraw_button),
        )
        is AddOnAction.Cancel -> Triple(
            stringResource(R.string.addon_confirm_cancel_title, addOn.name),
            stringResource(R.string.addon_confirm_cancel_body),
            stringResource(R.string.addon_confirm_cancel_button),
        )
        is AddOnAction.UndoCancel -> Triple(
            stringResource(R.string.addon_confirm_undo_title, addOn.name),
            stringResource(R.string.addon_confirm_undo_body),
            stringResource(R.string.addon_confirm_undo_button),
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_back)) } },
    )
}

@Composable
private fun WithdrawalReceiptDialog(receipt: WithdrawalReceipt, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.withdrawal_receipt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.withdrawal_receipt_service, receipt.addOnName))
                Text(stringResource(R.string.withdrawal_receipt_date, formatDate(receipt.withdrawnOn)))
                Text(stringResource(R.string.withdrawal_receipt_refund, formatMoney(receipt.refundCents)))
                Text(stringResource(R.string.withdrawal_receipt_reference, receipt.confirmationId))
                Text(stringResource(R.string.withdrawal_receipt_email), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
