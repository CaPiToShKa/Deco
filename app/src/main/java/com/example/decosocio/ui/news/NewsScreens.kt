package com.example.decosocio.ui.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.decosocio.R
import com.example.decosocio.domain.model.Article
import com.example.decosocio.domain.model.NewsSource
import com.example.decosocio.ui.common.AppTopBar
import com.example.decosocio.ui.common.EmptyState
import com.example.decosocio.ui.common.ErrorBox
import com.example.decosocio.ui.common.InfoBanner
import com.example.decosocio.ui.common.LoadingBox
import com.example.decosocio.ui.common.StatusPill
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.formatDate
import com.example.decosocio.ui.theme.LocalStatusColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NewsScreen(onOpenArticle: (String) -> Unit, viewModel: NewsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.news_title),
                actions = {
                    IconButton(onClick = { viewModel.load(force = true) }, enabled = !state.refreshing) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        val feed = state.feed
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            feed == null -> ErrorBox(
                message = state.error ?: UiText.Res(R.string.error_generic),
                onRetry = { viewModel.load(force = true) },
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.refreshing) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }
                when (feed.source) {
                    NewsSource.SAMPLE -> item { InfoBanner(stringResource(R.string.news_sample_banner)) }
                    NewsSource.SAMPLE_FALLBACK -> item { InfoBanner(stringResource(R.string.news_fallback_banner)) }
                    NewsSource.REMOTE -> Unit
                }
                if (feed.articles.isEmpty()) item { EmptyState(stringResource(R.string.news_empty)) }
                items(feed.articles, key = { it.id }) { article ->
                    ArticleCard(article, onClick = { onOpenArticle(article.id) })
                }
            }
        }
    }
}

@Composable
private fun ArticleMeta(article: Article) {
    val status = LocalStatusColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (article.category.isNotBlank()) {
            Text(article.category.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(formatDate(article.publishedOn), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (article.membersOnly) {
            StatusPill(stringResource(R.string.news_members_only), status.warning, status.onWarning)
        }
        if (article.isSample) {
            StatusPill(stringResource(R.string.news_sample_label), status.neutral, status.onNeutral)
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.action_read), onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ArticleMeta(article)
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ArticleScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: ArticleViewModel = koinViewModel(key = "article-$articleId") { parametersOf(articleId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    Scaffold(topBar = { AppTopBar(stringResource(R.string.news_article_title), onBack = onBack) }) { padding ->
        val article = state.article
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            article == null -> EmptyState(stringResource(R.string.error_not_found), Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArticleMeta(article)
                Text(
                    article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(article.summary, style = MaterialTheme.typography.titleSmall)
                Text(article.body, style = MaterialTheme.typography.bodyLarge)
                article.url?.let { url ->
                    OutlinedButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Text(stringResource(R.string.news_open_website), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
