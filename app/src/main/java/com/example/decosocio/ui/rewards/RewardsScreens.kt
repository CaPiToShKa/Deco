package com.example.decosocio.ui.rewards

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.R
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.CouponStatus
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.LoyaltyRules
import com.example.decosocio.domain.model.LoyaltyTier
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.EmptyState
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.StatusPill
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.formatDate
import com.example.decosocio.ui.common.resolve
import com.example.decosocio.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RewardsScreen(onOpenCoupon: (String) -> Unit, viewModel: RewardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }
    LaunchedEffect(state.openCouponId) {
        state.openCouponId?.let {
            onOpenCoupon(it)
            viewModel.couponOpened()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rewards_title),
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val account = state.account
        val today = state.today
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            account == null || today == null -> ErrorBox(
                message = state.error ?: UiText.Res(R.string.error_generic),
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PointsCard(account) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RewardsTab.entries.forEach { tab ->
                            FilterChip(
                                selected = state.tab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                label = { Text(stringResource(tab.labelRes())) },
                            )
                        }
                    }
                }
                when (state.tab) {
                    RewardsTab.REWARDS -> {
                        if (state.rewards.isEmpty()) item { EmptyState(stringResource(R.string.rewards_empty)) }
                        items(state.rewards, key = { it.id }) { reward ->
                            RewardCard(reward, account.points, busy = state.busyId == reward.id, onRedeem = { viewModel.askRedeem(reward) })
                        }
                    }
                    RewardsTab.CAMPAIGNS -> {
                        if (state.campaigns.isEmpty()) item { EmptyState(stringResource(R.string.campaigns_empty)) }
                        items(state.campaigns, key = { it.id }) { campaign ->
                            CampaignCard(
                                campaign = campaign,
                                today = today,
                                alreadyClaimed = state.coupons.any { it.campaignId == campaign.id },
                                busy = state.busyId == campaign.id,
                                onClaim = { viewModel.claim(campaign) },
                                onOpen = { id -> onOpenCoupon(id) },
                                claimedCouponId = state.coupons.firstOrNull { it.campaignId == campaign.id }?.id,
                            )
                        }
                    }
                    RewardsTab.MY_COUPONS -> {
                        if (state.coupons.isEmpty()) item { EmptyState(stringResource(R.string.coupons_empty)) }
                        items(state.coupons, key = { it.id }) { coupon ->
                            CouponRow(coupon, coupon.status(today), onClick = { onOpenCoupon(coupon.id) })
                        }
                    }
                }
            }
        }
    }

    state.confirmReward?.let { reward ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRedeem,
            title = { Text(stringResource(R.string.reward_confirm_title, reward.title)) },
            text = { Text(stringResource(R.string.reward_confirm_body, reward.costPoints, reward.validDays)) },
            confirmButton = { TextButton(onClick = viewModel::confirmRedeem) { Text(stringResource(R.string.reward_redeem)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissRedeem) { Text(stringResource(R.string.action_back)) } },
        )
    }
}

private fun RewardsTab.labelRes(): Int = when (this) {
    RewardsTab.REWARDS -> R.string.rewards_tab_rewards
    RewardsTab.CAMPAIGNS -> R.string.rewards_tab_campaigns
    RewardsTab.MY_COUPONS -> R.string.rewards_tab_coupons
}

private fun LoyaltyTier.labelRes(): Int = when (this) {
    LoyaltyTier.BRONZE -> R.string.tier_bronze
    LoyaltyTier.SILVER -> R.string.tier_silver
    LoyaltyTier.GOLD -> R.string.tier_gold
}

@Composable
private fun PointsCard(account: LoyaltyAccount) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.rewards_points_label), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.rewards_points_value, account.points),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.rewards_tier, stringResource(account.tier.labelRes())), style = MaterialTheme.typography.bodyMedium)
            val next = LoyaltyRules.nextTier(account.lifetimePoints)
            if (next != null) {
                val (tier, missing) = next
                val progress = account.lifetimePoints.toFloat() / tier.minLifetimePoints.toFloat()
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.rewards_next_tier, missing, stringResource(tier.labelRes())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RewardCard(reward: Reward, points: Int, busy: Boolean, onRedeem: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reward.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(reward.discountLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(reward.partner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(reward.description, style = MaterialTheme.typography.bodyMedium)
            val affordable = points >= reward.costPoints
            Button(onClick = onRedeem, enabled = affordable && !busy) {
                Text(stringResource(R.string.reward_redeem_for, reward.costPoints))
            }
            if (!affordable) {
                Text(
                    stringResource(R.string.reward_missing_points, reward.costPoints - points),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CampaignCard(
    campaign: CouponCampaign,
    today: LocalDate,
    alreadyClaimed: Boolean,
    busy: Boolean,
    onClaim: () -> Unit,
    onOpen: (String) -> Unit,
    claimedCouponId: String?,
) {
    val colors = LocalStatusColors.current
    val expired = today > campaign.validUntil
    val soldOut = campaign.remainingCodes == 0
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(campaign.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(campaign.discountLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(campaign.partner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(campaign.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.campaign_valid_until, formatDate(campaign.validUntil)),
                style = MaterialTheme.typography.bodySmall,
            )
            when {
                alreadyClaimed && claimedCouponId != null -> OutlinedButton(onClick = { onOpen(claimedCouponId) }) {
                    Text(stringResource(R.string.campaign_show_code))
                }
                expired -> StatusPill(stringResource(R.string.coupon_status_expired), colors.neutral, colors.onNeutral)
                soldOut -> StatusPill(stringResource(R.string.campaign_sold_out), colors.neutral, colors.onNeutral)
                else -> Button(onClick = onClaim, enabled = !busy) { Text(stringResource(R.string.campaign_claim)) }
            }
            campaign.remainingCodes?.takeIf { it in 1..5 && !alreadyClaimed && !expired }?.let { left ->
                Text(
                    stringResource(R.string.campaign_codes_left, left),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CouponStatusPill(status: CouponStatus) {
    val colors = LocalStatusColors.current
    when (status) {
        CouponStatus.ACTIVE -> StatusPill(stringResource(R.string.coupon_status_active), colors.positive, colors.onPositive)
        CouponStatus.USED -> StatusPill(stringResource(R.string.coupon_status_used), colors.neutral, colors.onNeutral)
        CouponStatus.EXPIRED -> StatusPill(stringResource(R.string.coupon_status_expired), colors.neutral, colors.onNeutral)
    }
}

@Composable
private fun CouponRow(coupon: Coupon, status: CouponStatus, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.coupon_open), onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(coupon.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                CouponStatusPill(status)
            }
            Text(coupon.partner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.campaign_valid_until, formatDate(coupon.validUntil)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CouponScreen(
    couponId: String,
    onBack: () -> Unit,
    viewModel: CouponViewModel = koinViewModel(key = "coupon-$couponId") { parametersOf(couponId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedText = stringResource(R.string.coupon_copied)

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.coupon_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val coupon = state.coupon
        val status = state.status
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            coupon == null || status == null -> EmptyState(stringResource(R.string.error_not_found), Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    coupon.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(coupon.partner, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(coupon.discountLabel, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                CouponStatusPill(status)
                if (status == CouponStatus.ACTIVE) {
                    QrCode(content = coupon.code, contentDescription = stringResource(R.string.coupon_qr_description))
                }
                Text(
                    coupon.code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, coupon.code)
                        scope.launch { snackbar.showSnackbar(copiedText) }
                    },
                    enabled = status == CouponStatus.ACTIVE,
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text(stringResource(R.string.coupon_copy), modifier = Modifier.padding(start = 8.dp))
                }
                Text(stringResource(R.string.campaign_valid_until, formatDate(coupon.validUntil)), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.coupon_single_use_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("coupon", code))
}
