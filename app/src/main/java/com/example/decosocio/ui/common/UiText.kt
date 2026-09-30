package com.example.decosocio.ui.common

import androidx.annotation.StringRes
import com.example.decosocio.R
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason

/** Text produced by a ViewModel without holding a Context; resolved in the UI. */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plain(val value: String) : UiText
}

fun Throwable.toUiText(): UiText = UiText.Res(
    when (this) {
        is DomainError.Network -> R.string.error_network
        is DomainError.Unauthorized -> R.string.error_unauthorized
        is DomainError.Validation -> R.string.error_validation
        is DomainError.NotFound -> R.string.error_not_found
        is DomainError.NotEligible -> when (reason) {
            Reason.SUBSCRIPTION_EXPIRED -> R.string.error_subscription_expired
            Reason.ALREADY_ACTIVE -> R.string.error_already_active
            Reason.NOT_ACTIVE -> R.string.error_not_active
            Reason.WITHDRAWAL_PERIOD_OVER -> R.string.error_withdrawal_over
            Reason.CANCELLATION_NOT_PENDING -> R.string.error_generic
            Reason.INSUFFICIENT_POINTS -> R.string.error_insufficient_points
            Reason.COUPON_EXPIRED -> R.string.error_coupon_expired
            Reason.COUPON_POOL_EMPTY -> R.string.error_coupon_pool_empty
            Reason.DELETION_ALREADY_REQUESTED -> R.string.error_deletion_already_requested
        }
        else -> R.string.error_generic
    },
)
