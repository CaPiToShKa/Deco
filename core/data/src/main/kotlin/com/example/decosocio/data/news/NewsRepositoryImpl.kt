package com.example.decosocio.data.news

import com.example.decosocio.api.ApiJson
import com.example.decosocio.api.ArticleDto
import com.example.decosocio.api.toDomain
import com.example.decosocio.domain.model.Article
import com.example.decosocio.domain.model.NewsFeed
import com.example.decosocio.domain.model.NewsSource
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.NewsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * News from a configurable endpoint, which may return either a JSON array of [ArticleDto]
 * (e.g. a Sitecore headless endpoint or the BFF's /v1/news) or an RSS 2.0 feed.
 * With no endpoint, or when it fails, the app shows clearly labelled sample articles.
 */
class NewsRepositoryImpl(
    private val feedUrl: String?,
    private val dates: DateProvider,
    engine: HttpClientEngine? = null,
) : NewsRepository {

    private val client = HttpClient(engine ?: OkHttp.create()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 8_000
        }
    }
    private val mutex = Mutex()
    private var cache: NewsFeed? = null

    override suspend fun latest(forceRefresh: Boolean): NewsFeed {
        mutex.withLock {
            if (!forceRefresh) cache?.let { return it }
            return load().also { cache = it }
        }
    }

    override suspend fun article(id: String): Article? = latest().articles.firstOrNull { it.id == id }

    private suspend fun load(): NewsFeed {
        val url = feedUrl?.trim().orEmpty()
        if (url.isEmpty()) return NewsFeed(SampleArticles.all(dates.today()), NewsSource.SAMPLE)
        return try {
            val response = client.get(url)
            if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
            val text = response.bodyAsText()
            val articles = parse(text)
            if (articles.isEmpty()) throw IOException("Empty feed")
            NewsFeed(articles.sortedByDescending { it.publishedOn }, NewsSource.REMOTE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NewsFeed(SampleArticles.all(dates.today()), NewsSource.SAMPLE_FALLBACK)
        }
    }

    internal fun parse(text: String): List<Article> =
        if (text.trimStart().startsWith("<")) {
            RssParser.parse(text, dates.today())
        } else {
            ApiJson.decodeFromString<List<ArticleDto>>(text).map { it.toDomain() }
        }
}
