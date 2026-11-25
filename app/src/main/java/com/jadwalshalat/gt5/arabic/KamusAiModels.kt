package com.jadwalshalat.gt5.arabic

import androidx.compose.runtime.Immutable

@Immutable
data class KamusAiAnalysis(
    val vocalizedArabic: String,
    val indonesianMeaning: String,
    val transliteration: String,
    val turatsOrigin: String,
    val synonymNote: String,
    val suggestedKind: KamusEntryKind,
    val fromCache: Boolean = false
)

@Immutable
sealed interface KamusAiUiState {
    data object Idle : KamusAiUiState
    data class Loading(val queryArabic: String) : KamusAiUiState
    data class Success(val queryArabic: String, val analysis: KamusAiAnalysis) : KamusAiUiState
    data class Error(val queryArabic: String, val message: String) : KamusAiUiState
}
