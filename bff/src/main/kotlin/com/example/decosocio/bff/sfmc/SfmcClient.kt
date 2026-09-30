package com.example.decosocio.bff.sfmc

import com.example.decosocio.bff.SfmcCredentials
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/** One Data Extension row: primary-key columns in [keys], other columns in [values]. */
@Serializable
data class DeRow(val keys: Map<String, String>, val values: Map<String, String>)

/** The two SFMC operations the app needs. Everything else stays inside Marketing Cloud. */
interface SfmcClient {
    /** Synchronous upsert: POST /hub/v1/dataevents/key:{externalKey}/rowset */
    suspend fun upsertRows(dataExtensionKey: String, rows: List<DeRow>)

    /** Journey Builder API entry event: POST /interaction/v1/events */
    suspend fun fireEntryEvent(eventDefinitionKey: String, contactKey: String, data: Map<String, String>)
}

class SfmcException(message: String) : Exception(message)

@Serializable
internal data class TokenRequest(
    @SerialName("grant_type") val grantType: String = "client_credentials",
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("account_id") val accountId: String? = null,
)

@Serializable
internal data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("rest_instance_url") val restInstanceUrl: String,
)

@Serializable
internal data class EntryEvent(
    @SerialName("ContactKey") val contactKey: String,
    @SerialName("EventDefinitionKey") val eventDefinitionKey: String,
    @SerialName("Data") val data: Map<String, String>,
)

/**
 * Real SFMC REST client (server-to-server OAuth 2.0 client credentials).
 * The access token lasts about 20 minutes; it is cached and reused, refreshed a minute before
 * expiry or after a 401, because requesting a token per call counts against API limits.
 * [http] must have ContentNegotiation with JSON installed.
 */
class LiveSfmcClient(
    private val credentials: SfmcCredentials,
    private val http: HttpClient,
    private val clock: () -> Long = System::currentTimeMillis,
) : SfmcClient {

    private data class CachedToken(val value: String, val restBaseUrl: String, val expiresAtMs: Long)

    private val mutex = Mutex()
    private var cached: CachedToken? = null

    private suspend fun token(forceRefresh: Boolean): CachedToken = mutex.withLock { fetchToken(forceRefresh) }

    private suspend fun fetchToken(forceRefresh: Boolean): CachedToken {
        val now = clock()
        val current = cached
        if (!forceRefresh && current != null && current.expiresAtMs - 60_000 > now) return current
        val response = http.post("${credentials.authBaseUrl}/v2/token") {
            contentType(ContentType.Application.Json)
            setBody(TokenRequest(clientId = credentials.clientId, clientSecret = credentials.clientSecret, accountId = credentials.accountId))
        }
        if (!response.status.isSuccess()) {
            throw SfmcException("SFMC token request failed: ${response.status.value} ${response.bodyAsText().take(300)}")
        }
        val body = response.body<TokenResponse>()
        return CachedToken(body.accessToken, body.restInstanceUrl.trimEnd('/'), now + body.expiresIn * 1000).also { cached = it }
    }

    private suspend fun authorizedPost(path: String, send: suspend (url: String, token: String) -> HttpResponse) {
        var token = token(forceRefresh = false)
        var response = send(token.restBaseUrl + path, token.value)
        if (response.status == HttpStatusCode.Unauthorized) {
            token = token(forceRefresh = true)
            response = send(token.restBaseUrl + path, token.value)
        }
        if (!response.status.isSuccess()) {
            throw SfmcException("SFMC $path failed: ${response.status.value} ${response.bodyAsText().take(500)}")
        }
    }

    override suspend fun upsertRows(dataExtensionKey: String, rows: List<DeRow>) {
        if (rows.isEmpty()) return
        authorizedPost("/hub/v1/dataevents/key:$dataExtensionKey/rowset") { url, token ->
            http.post(url) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(rows)
            }
        }
    }

    override suspend fun fireEntryEvent(eventDefinitionKey: String, contactKey: String, data: Map<String, String>) {
        authorizedPost("/interaction/v1/events") { url, token ->
            http.post(url) {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(EntryEvent(contactKey, eventDefinitionKey, data))
            }
        }
    }
}

/** Mock mode: remembers what would have been sent, for demos and tests. */
class RecordingSfmcClient : SfmcClient {
    @Serializable
    data class Call(val operation: String, val target: String, val payload: Map<String, String>)

    private val log = LoggerFactory.getLogger(RecordingSfmcClient::class.java)
    val calls = CopyOnWriteArrayList<Call>()

    override suspend fun upsertRows(dataExtensionKey: String, rows: List<DeRow>) {
        rows.forEach { row ->
            calls += Call("upsertRow", dataExtensionKey, row.keys + row.values)
            log.info("[SFMC mock] upsert {} {}", dataExtensionKey, row)
        }
    }

    override suspend fun fireEntryEvent(eventDefinitionKey: String, contactKey: String, data: Map<String, String>) {
        calls += Call("entryEvent", eventDefinitionKey, data + ("ContactKey" to contactKey))
        log.info("[SFMC mock] event {} for {} {}", eventDefinitionKey, contactKey, data)
    }
}
