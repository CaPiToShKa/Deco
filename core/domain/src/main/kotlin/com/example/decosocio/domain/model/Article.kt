package com.example.decosocio.domain.model

import kotlinx.datetime.LocalDate

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val body: String,
    val category: String,
    val publishedOn: LocalDate,
    val imageUrl: String?,
    val url: String?,
    val membersOnly: Boolean,
    /** Placeholder text shipped with the demo, clearly labelled in the UI. */
    val isSample: Boolean,
)

enum class NewsSource {
    /** Loaded from the configured feed endpoint. */
    REMOTE,

    /** No endpoint configured: sample articles by design. */
    SAMPLE,

    /** Endpoint configured but unreachable: sample articles as a fallback. */
    SAMPLE_FALLBACK,
}

data class NewsFeed(
    val articles: List<Article>,
    val source: NewsSource,
)
