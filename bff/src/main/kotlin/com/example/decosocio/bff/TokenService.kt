package com.example.decosocio.bff

import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Opaque app tokens for the demo login. In production the app signs in with the member's
 * existing identity provider (OIDC Authorization Code + PKCE) and the BFF validates that
 * provider's JWTs instead (Ktor `jwt` auth with the issuer's JWKS).
 */
class TokenService(
    val ttlSeconds: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val contactKey: String, val expiresAtMs: Long)

    private val random = SecureRandom()
    private val tokens = ConcurrentHashMap<String, Entry>()

    fun issue(contactKey: String): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        tokens[token] = Entry(contactKey, clock() + ttlSeconds * 1000)
        return token
    }

    fun contactKeyFor(token: String): String? {
        val entry = tokens[token] ?: return null
        if (entry.expiresAtMs < clock()) {
            tokens.remove(token)
            return null
        }
        return entry.contactKey
    }

    fun revokeAll(contactKey: String) {
        tokens.entries.removeIf { it.value.contactKey == contactKey }
    }
}
