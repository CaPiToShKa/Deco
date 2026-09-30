package com.example.decosocio.bff

import com.example.decosocio.api.ApiJson
import com.example.decosocio.api.ApiRoutes
import com.example.decosocio.api.ConsentsDto
import com.example.decosocio.api.ConsentsUpdateDto
import com.example.decosocio.api.DemoLoginRequest
import com.example.decosocio.api.ErrorDto
import com.example.decosocio.api.LoginResponse
import com.example.decosocio.api.ProfileDto
import com.example.decosocio.bff.sfmc.DeRow
import com.example.decosocio.bff.sfmc.LiveSfmcClient
import com.example.decosocio.bff.sfmc.RecordingSfmcClient
import com.example.decosocio.domain.repository.DateProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object TestDates : DateProvider {
    override fun today() = LocalDate(2026, 9, 30)
    override fun nowEpochMillis() = 1_790_000_000_000L
}

class BffApplicationTest {
    private val config = BffConfig.fromEnvironment(emptyMap())

    private fun ApplicationTestBuilder.start(sfmc: RecordingSfmcClient) =
        application { bffModule(config, sfmc, CoroutineScope(Dispatchers.Unconfined), TestDates) }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json(ApiJson) }
    }

    @Test
    fun requestsWithoutATokenAreRejected() = testApplication {
        start(RecordingSfmcClient())
        assertEquals(HttpStatusCode.Unauthorized, jsonClient().get(ApiRoutes.PROFILE).status)
    }

    @Test
    fun consentChangesAreStoredAndSyncedToSfmc() = testApplication {
        val sfmc = RecordingSfmcClient()
        start(sfmc)
        val client = jsonClient()
        val login: LoginResponse = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(DemoLoginRequest("demo@exemplo.pt", "demo1234"))
        }.body()

        val profile: ProfileDto = client.get(ApiRoutes.PROFILE) { bearerAuth(login.accessToken) }.body()
        assertEquals(login.contactKey, profile.contactKey)

        val consents: ConsentsDto = client.put(ApiRoutes.CONSENTS) {
            bearerAuth(login.accessToken)
            contentType(ContentType.Application.Json)
            setBody(ConsentsUpdateDto(mapOf("MARKETING_PUSH" to true, "NEWSLETTER" to false), "ONBOARDING"))
        }.body()
        assertEquals(true, consents.current["MARKETING_PUSH"])
        assertTrue(
            sfmc.calls.any {
                it.operation == "upsertRow" && it.target == config.keys.consentsDataExtension &&
                    it.payload["ContactKey"] == login.contactKey && it.payload["MarketingPush"] == "true"
            },
        )
    }

    @Test
    fun businessRuleViolationsReturn409WithAStableCode() = testApplication {
        start(RecordingSfmcClient())
        val client = jsonClient()
        val login: LoginResponse = client.post(ApiRoutes.LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(DemoLoginRequest("demo@exemplo.pt", "demo1234"))
        }.body()
        val response = client.post(ApiRoutes.addOnWithdrawal("seguro-compras")) { bearerAuth(login.accessToken) }
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("NOT_ELIGIBLE:WITHDRAWAL_PERIOD_OVER", response.body<ErrorDto>().code)
    }
}

class LiveSfmcClientTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val tokenBody = """{"access_token":"T1","expires_in":1080,"rest_instance_url":"https://tenant.rest.marketingcloudapis.com/","token_type":"Bearer"}"""

    @Test
    fun reusesTheTokenAndCallsTheDocumentedEndpoints() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.host + request.url.encodedPath
            if (request.url.encodedPath == "/v2/token") respond(tokenBody, HttpStatusCode.OK, json) else respond("{}", HttpStatusCode.OK, json)
        }
        val http = HttpClient(engine) { install(ContentNegotiation) { json(ApiJson) } }
        val client = LiveSfmcClient(SfmcCredentials("tenant", "id", "secret", null), http, clock = { 0L })

        client.upsertRows("DECO_App_Profile", listOf(DeRow(mapOf("ContactKey" to "K"), mapOf("City" to "Lisboa"))))
        client.fireEntryEvent("APIEvent-1", "K", mapOf("A" to "1"))

        assertEquals(1, paths.count { it == "tenant.auth.marketingcloudapis.com/v2/token" })
        assertTrue(paths.any { it.startsWith("tenant.rest.marketingcloudapis.com/hub/v1/dataevents/") && it.endsWith("/rowset") })
        assertTrue(paths.contains("tenant.rest.marketingcloudapis.com/interaction/v1/events"))
    }

    @Test
    fun refreshesTheTokenOnce401() = runTest {
        var tokenCalls = 0
        var eventCalls = 0
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v2/token" -> {
                    tokenCalls++
                    respond(tokenBody, HttpStatusCode.OK, json)
                }
                else -> {
                    eventCalls++
                    if (eventCalls == 1) respond("{}", HttpStatusCode.Unauthorized, json) else respond("{}", HttpStatusCode.Created, json)
                }
            }
        }
        val http = HttpClient(engine) { install(ContentNegotiation) { json(ApiJson) } }
        val client = LiveSfmcClient(SfmcCredentials("tenant", "id", "secret", "123"), http, clock = { 0L })
        client.fireEntryEvent("APIEvent-1", "K", emptyMap())
        assertEquals(2, tokenCalls)
        assertEquals(2, eventCalls)
    }
}
