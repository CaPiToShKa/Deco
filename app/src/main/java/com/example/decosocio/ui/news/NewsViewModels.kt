package com.example.decosocio.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.domain.model.Article
import com.example.decosocio.domain.model.NewsFeed
import com.example.decosocio.domain.repository.NewsRepository
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val feed: NewsFeed? = null,
    val error: UiText? = null,
)

class NewsViewModel(private val news: NewsRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        load(force = false)
    }

    fun load(force: Boolean) {
        _state.update { it.copy(loading = it.feed == null, refreshing = it.feed != null, error = null) }
        viewModelScope.launch {
            resultOf { news.latest(forceRefresh = force) }
                .onSuccess { feed -> _state.update { it.copy(loading = false, refreshing = false, feed = feed) } }
                .onFailure { e -> _state.update { it.copy(loading = false, refreshing = false, error = e.toUiText()) } }
        }
    }
}

data class ArticleUiState(val loading: Boolean = true, val article: Article? = null)

class ArticleViewModel(private val articleId: String, private val news: NewsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ArticleUiState())
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val article = resultOf { news.article(articleId) }.getOrNull()
            _state.value = ArticleUiState(loading = false, article = article)
        }
    }
}
