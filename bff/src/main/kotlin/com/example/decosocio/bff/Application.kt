package com.example.decosocio.bff

import com.example.decosocio.api.ApiJson
import com.example.decosocio.api.ApiRoutes
import com.example.decosocio.api.ConsentsUpdateDto
import com.example.decosocio.api.DemoLoginRequest
import com.example.decosocio.api.ErrorCodes
import com.example.decosocio.api.ErrorDto
import com.example.decosocio.api.LoginResponse
import com.example.decosocio.api.ProfileUpdateDto
import com.example.decosocio.api.changesToDomain
import com.example.decosocio.api.sourceToDomain
import com.example.decosocio.api.toDomain
import com.example.decosocio.api.toDto
import com.example.decosocio.api.toErrorDto
import com.example.decosocio.bff.sfmc.RecordingSfmcClient
import com.example.decosocio.bff.sfmc.SfmcClient
import com.example.decosocio.bff.sfmc.SfmcSync
import com.example.decosocio.bff.sfmc.defaultSfmcClient
import com.example.decosocio.data.SystemDateProvider
import com.example.decosocio.data.demo.DemoBackend
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.news.NewsRepositoryImpl
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.model.SubscriptionRules
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.Session
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable

fun main() {
    val config = BffConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        bffModule(config)
    }.start(wait = true)
}

@Serializable
data class SfmcDebugDto(val calls: List<RecordingSfmcClient.Call>, val failures: List<String>)

/**
 * The BFF: authenticates the member, owns business rules and state, and keeps SFMC in sync.
 * The demo keeps member state in memory (via the same DemoBackend the app uses); in production
 * that is the billing/CRM system of record plus the BFF's own database.
 */
fun Application.bffModule(
    config: BffConfig,
    sfmcClient: SfmcClient = defaultSfmcClient(config),
    syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    dates: DateProvider = SystemDateProvider(),
) {
    val tokens = TokenService(config.tokenTtlSeconds)
    val store = DemoBackend(DemoControl(), dates, latencyMs = 0)
    val sync = SfmcSync(sfmcClient, config.keys, syncScope)
    val news = NewsRepositoryImpl(config.newsFeedUrl, dates)

    install(ContentNegotiation) { json(ApiJson) }
    install(CallLogging)
    install(StatusPages) {
        exception<DomainError> { call, cause ->
            val status = when (cause) {
                is DomainError.NotEligible -> HttpStatusCode.Conflict
                is DomainError.Validation -> HttpStatusCode.UnprocessableEntity
                is DomainError.NotFound -> HttpStatusCode.NotFound
                is DomainError.Unauthorized -> HttpStatusCode.Unauthorized
                else -> HttpStatusCode.BadGateway
            }
            call.respond(status, cause.toErrorDto())
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorDto("BAD_REQUEST", "Malformed request"))
        }
        exception<Throwable> { call, cause ->
            this@bffModule.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto(ErrorCodes.INTERNAL, "Internal error"))
        }
    }
    install(Authentication) {
        bearer("app") {
            realm = "deco-socio"
            authenticate { credential -> tokens.contactKeyFor(credential.token)?.let { UserIdPrincipal(it) } }
        }
    }

    /** Resolves the calling member and lets the store act on their behalf. */
    fun ApplicationCall.member(): DemoBackend {
        val contactKey = principal<UserIdPrincipal>()?.name ?: throw DomainError.Unauthorized()
        store.actAs(Session(contactKey, accessToken = "", displayName = ""))
        return store
    }

    fun ApplicationCall.contactKey(): String = principal<UserIdPrincipal>()?.name ?: throw DomainError.Unauthorized()

    fun ApplicationCall.pathId(): String = parameters["id"] ?: throw DomainError.NotFound("id")

    routing {
        get("/health") { call.respondText("ok") }

        post(ApiRoutes.LOGIN) {
            val request = call.receive<DemoLoginRequest>()
            val session = store.login(request.email, request.password)
            call.respond(
                LoginResponse(
                    accessToken = tokens.issue(session.contactKey),
                    expiresInSeconds = tokens.ttlSeconds,
                    contactKey = session.contactKey,
                    displayName = session.displayName,
                ),
            )
        }

        // Public news: proxies the CMS feed (NEWS_FEED_URL) or serves sample articles.
        get(ApiRoutes.NEWS) {
            call.respond(news.latest().articles.map { it.toDto() })
        }

        if (sfmcClient is RecordingSfmcClient) {
            get("/debug/sfmc-calls") { call.respond(SfmcDebugDto(sfmcClient.calls.toList(), sync.failures.toList())) }
        }

        authenticate("app") {
            get(ApiRoutes.PROFILE) { call.respond(call.member().profile().toDto()) }

            patch(ApiRoutes.PROFILE) {
                val update = call.receive<ProfileUpdateDto>().toDomain()
                val profile = call.member().updateProfile(update)
                sync.profileUpdated(profile)
                call.respond(profile.toDto())
            }

            get(ApiRoutes.CONSENTS) { call.respond(call.member().consents().toDto()) }

            put(ApiRoutes.CONSENTS) {
                val request = call.receive<ConsentsUpdateDto>()
                val consents = call.member().updateConsents(request.changesToDomain(), request.sourceToDomain())
                sync.consentsChanged(call.contactKey(), consents)
                call.respond(consents.toDto())
            }

            get(ApiRoutes.SUBSCRIPTION) {
                val subscription = call.member().subscription()
                call.respond(subscription.toDto(SubscriptionRules.status(subscription, dates.today())))
            }

            get(ApiRoutes.ADDONS) { call.respond(call.member().addOns().map { it.toDto() }) }

            post(ApiRoutes.addOnActivation("{id}")) {
                val addOn = call.member().activateAddOn(call.pathId())
                sync.addOnChanged(call.contactKey(), addOn, "ACTIVATED")
                call.respond(addOn.toDto())
            }

            post(ApiRoutes.addOnWithdrawal("{id}")) {
                val member = call.member()
                val id = call.pathId()
                val receipt = member.withdrawAddOn(id)
                member.addOns().firstOrNull { it.id == id }?.let { sync.addOnChanged(call.contactKey(), it, "WITHDRAWN") }
                call.respond(receipt.toDto())
            }

            post(ApiRoutes.addOnCancellation("{id}")) {
                val addOn = call.member().cancelAddOn(call.pathId())
                sync.addOnChanged(call.contactKey(), addOn, "CANCELLATION_REQUESTED")
                call.respond(addOn.toDto())
            }

            delete(ApiRoutes.addOnCancellation("{id}")) {
                val addOn = call.member().undoCancellation(call.pathId())
                sync.addOnChanged(call.contactKey(), addOn, "CANCELLATION_UNDONE")
                call.respond(addOn.toDto())
            }

            get(ApiRoutes.LOYALTY) { call.respond(call.member().account().toDto()) }

            get(ApiRoutes.REWARDS) { call.respond(call.member().rewards().map { it.toDto() }) }

            post(ApiRoutes.rewardRedemption("{id}")) {
                val coupon = call.member().redeem(call.pathId())
                sync.couponIssued(call.contactKey(), coupon)
                call.respond(coupon.toDto())
            }

            get(ApiRoutes.CAMPAIGNS) { call.respond(call.member().campaigns().map { it.toDto() }) }

            get(ApiRoutes.COUPONS) { call.respond(call.member().coupons().map { it.toDto() }) }

            post(ApiRoutes.campaignClaim("{id}")) {
                val coupon = call.member().claim(call.pathId())
                sync.couponIssued(call.contactKey(), coupon)
                call.respond(coupon.toDto())
            }

            get(ApiRoutes.EXPORT) {
                call.respondText(call.member().exportData(), ContentType.Application.Json)
            }

            delete(ApiRoutes.ME) {
                val contactKey = call.contactKey()
                val receipt = call.member().requestAccountDeletion()
                sync.deletionRequested(contactKey, receipt)
                tokens.revokeAll(contactKey)
                call.respond(receipt.toDto())
            }
        }
    }
}
