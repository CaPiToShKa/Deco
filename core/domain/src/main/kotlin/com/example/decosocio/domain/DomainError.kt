package com.example.decosocio.domain

/** Business-rule reasons shared by the app, the demo backend and the BFF. */
enum class Reason {
    SUBSCRIPTION_EXPIRED,
    ALREADY_ACTIVE,
    NOT_ACTIVE,
    WITHDRAWAL_PERIOD_OVER,
    CANCELLATION_NOT_PENDING,
    INSUFFICIENT_POINTS,
    COUPON_EXPIRED,
    COUPON_POOL_EMPTY,
    DELETION_ALREADY_REQUESTED,
}

enum class ProfileField { FIRST_NAME, LAST_NAME, EMAIL, PHONE, STREET, POSTAL_CODE, CITY }

sealed class DomainError(message: String) : Exception(message) {
    class NotEligible(val reason: Reason) : DomainError("Not eligible: $reason")
    class NotFound(what: String) : DomainError("Not found: $what")
    class Validation(val fields: Set<ProfileField>) : DomainError("Invalid fields: $fields")
    class Unauthorized : DomainError("Session expired or invalid credentials")
    class Network(cause: Throwable? = null) : DomainError("Network unavailable") {
        init {
            if (cause != null) initCause(cause)
        }
    }
    class Server(val code: String) : DomainError("Server error: $code")
}
