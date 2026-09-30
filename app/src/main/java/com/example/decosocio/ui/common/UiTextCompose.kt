package com.example.decosocio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Plain -> value
}
