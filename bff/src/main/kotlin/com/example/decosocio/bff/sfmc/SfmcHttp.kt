package com.example.decosocio.bff.sfmc

import com.example.decosocio.api.ApiJson
import com.example.decosocio.bff.BffConfig
import com.example.decosocio.bff.SfmcMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

fun sfmcHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) { json(ApiJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
    }
}

fun defaultSfmcClient(config: BffConfig): SfmcClient = when (config.sfmcMode) {
    SfmcMode.MOCK -> RecordingSfmcClient()
    SfmcMode.LIVE -> LiveSfmcClient(requireNotNull(config.credentials), sfmcHttpClient())
}
