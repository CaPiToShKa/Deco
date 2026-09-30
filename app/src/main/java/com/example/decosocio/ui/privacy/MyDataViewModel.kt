package com.example.decosocio.ui.privacy

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.R
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MyDataUiState(
    val loading: Boolean = true,
    val json: String? = null,
    val error: UiText? = null,
    val message: UiText? = null,
)

/** GDPR art. 15 (access) and art. 20 (portability): show and export everything held. */
class MyDataViewModel(private val member: MemberRepository) : ViewModel() {
    private val _state = MutableStateFlow(MyDataUiState())
    val state: StateFlow<MyDataUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            resultOf { member.exportData() }
                .onSuccess { json -> _state.update { it.copy(loading = false, json = json) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUiText()) } }
        }
    }

    /** Writes the export to a file the member picked with the system file picker. */
    fun saveTo(uri: Uri, resolver: ContentResolver) {
        val json = _state.value.json ?: return
        viewModelScope.launch {
            val result = resultOf {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: error("Cannot open $uri")
                }
            }
            _state.update {
                it.copy(message = UiText.Res(if (result.isSuccess) R.string.my_data_saved else R.string.my_data_save_failed))
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
