package com.example.decosocio.ui.common

import kotlin.coroutines.cancellation.CancellationException

/** Like runCatching, but never swallows coroutine cancellation. */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
