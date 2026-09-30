package com.example.decosocio.data

import com.example.decosocio.api.ApiRoutes
import com.example.decosocio.data.demo.DemoBackend
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.news.NewsRepositoryImpl
import com.example.decosocio.data.remote.BffBackend
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.LoyaltyRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.domain.repository.MembershipRepository
import com.example.decosocio.domain.repository.NewsRepository

enum class BackendMode {
    /** Everything in memory on the device, with the hidden demo menu. */
    DEMO,

    /** Talks to the BFF (see the :bff module), which syncs with Salesforce Marketing Cloud. */
    BFF,
}

class Repositories(
    val auth: AuthRepository,
    val member: MemberRepository,
    val membership: MembershipRepository,
    val loyalty: LoyaltyRepository,
    val news: NewsRepository,
)

object Backends {
    fun create(
        mode: BackendMode,
        bffBaseUrl: String,
        newsFeedUrl: String,
        dates: DateProvider,
        demoControl: DemoControl,
    ): Repositories {
        // In BFF mode without an explicit feed, news comes through the BFF too.
        val feed = newsFeedUrl.ifBlank {
            if (mode == BackendMode.BFF && bffBaseUrl.isNotBlank()) bffBaseUrl.trimEnd('/') + ApiRoutes.NEWS else ""
        }
        val news = NewsRepositoryImpl(feed, dates)
        return when (mode) {
            BackendMode.DEMO -> {
                val backend = DemoBackend(demoControl, dates)
                Repositories(backend, backend, backend, backend, news)
            }
            BackendMode.BFF -> {
                require(bffBaseUrl.isNotBlank()) { "BFF_BASE_URL must be set when BACKEND_MODE=bff" }
                val backend = BffBackend(bffBaseUrl)
                Repositories(backend, backend, backend, backend, news)
            }
        }
    }
}
