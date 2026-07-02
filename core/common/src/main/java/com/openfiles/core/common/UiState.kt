package com.openfiles.core.common

/**
 * Generic one-way UI state used by every screen in the app.
 * Loading -> Content | Empty | Error. Never combine loading + content flags separately;
 * this sealed type is the single source of truth for a screen's render state.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Content -> UiState.Content(transform(data))
    UiState.Loading -> UiState.Loading
    UiState.Empty -> UiState.Empty
    is UiState.Error -> this
}
