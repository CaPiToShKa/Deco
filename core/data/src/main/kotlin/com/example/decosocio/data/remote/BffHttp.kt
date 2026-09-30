package com.example.decosocio.data.remote

import com.example.decosocio.api.ApiJson
import com.example.decosocio.api.ErrorDto
import com.example.decosocio.api.toDomainError
import com.example.decosocio.domain.DomainError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException

/**
 * Thin HTTP layer for the BFF. Adds the member's bearer token, maps transport failures to
 * [DomainError.Network] and BFF error bodies to the matching [DomainError].
 */
class BffHttp(
    baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
    engine: HttpClientEngine? = null,
) {
    @PublishedApi
    internal val client: HttpClient = HttpClient(engine ?: OkHttp.create()) {
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest { url(baseUrl.trimEnd('/') + "/") }
    }

    @PublishedApi
    internal suspend fun execute(method: HttpMethod, path: String, configure: HttpRequestBuilder.() -> Unit): HttpResponse {
        val response = try {
            client.request(path.removePrefix("/")) {
                this.method = method
                tokenProvider()?.let { bearerAuth(it) }
                configure()
            }
        } catch (e: IOException) {
            throw DomainError.Network(e)
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            onUnauthorized()
            throw DomainError.Unauthorized()
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorDto>() }.getOrNull()
            throw error?.toDomainError() ?: DomainError.Server("HTTP_${response.status.value}")
        }
        return response
    }

    suspend inline fun <reified T> get(path: String): T =
        execute(HttpMethod.Get, path) {}.body()

    suspend inline fun <reified T> delete(path: String): T =
        execute(HttpMethod.Delete, path) {}.body()

    suspend inline fun <reified B : Any, reified T> send(method: HttpMethod, path: String, body: B): T =
        execute(method, path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    /** POST without a request body (actions like /activation). */
    suspend inline fun <reified T> post(path: String): T =
        execute(HttpMethod.Post, path) {}.body()
}
