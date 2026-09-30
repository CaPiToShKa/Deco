package com.example.decosocio.data

import com.example.decosocio.data.news.NewsRepositoryImpl
import com.example.decosocio.data.remote.BffBackend
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason
import com.example.decosocio.domain.model.NewsSource
import com.example.decosocio.domain.repository.DateProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RemoteRepositoriesTest {
    private val dates = object : DateProvider {
        override fun today() = LocalDate(2026, 9, 30)
        override fun nowEpochMillis() = 0L
    }
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun newsFallsBackToSamplesWhenTheFeedFails() = runTest {
        val repo = NewsRepositoryImpl("https://feed.example/rss", dates, MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        assertEquals(NewsSource.SAMPLE_FALLBACK, repo.latest().source)
    }

    @Test
    fun newsReadsAJsonArray() = runTest {
        val body = """[{"id":"a1","title":"T","summary":"S","publishedOn":"2026-09-29","extra":"ignored"}]"""
        val repo = NewsRepositoryImpl("https://cms.example/api/news", dates, MockEngine { respond(body, HttpStatusCode.OK, json) })
        val feed = repo.latest()
        assertEquals(NewsSource.REMOTE, feed.source)
        assertEquals("a1", feed.articles.single().id)
    }

    @Test
    fun bffBusinessErrorsBecomeDomainErrors() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/demo-login")) {
                respond("""{"accessToken":"t","expiresInSeconds":3600,"contactKey":"K","displayName":"Ana"}""", HttpStatusCode.OK, json)
            } else {
                respond("""{"code":"NOT_ELIGIBLE:WITHDRAWAL_PERIOD_OVER","message":"late"}""", HttpStatusCode.Conflict, json)
            }
        }
        val backend = BffBackend("https://bff.example", engine)
        backend.login("demo@exemplo.pt", "demo1234")
        val error = assertFailsWith<DomainError.NotEligible> { backend.withdrawAddOn("x") }
        assertEquals(Reason.WITHDRAWAL_PERIOD_OVER, error.reason)
    }

    @Test
    fun unauthorizedClearsTheSession() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/demo-login")) {
                respond("""{"accessToken":"t","expiresInSeconds":3600,"contactKey":"K","displayName":"Ana"}""", HttpStatusCode.OK, json)
            } else {
                respond("""{"code":"UNAUTHORIZED","message":"expired"}""", HttpStatusCode.Unauthorized, json)
            }
        }
        val backend = BffBackend("https://bff.example", engine)
        backend.login("demo@exemplo.pt", "demo1234")
        assertFailsWith<DomainError.Unauthorized> { backend.profile() }
        assertNull(backend.session.value)
    }
}
