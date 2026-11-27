package com.jadwalshalat.gt5.tafsir

import androidx.compose.runtime.Immutable

enum class TafsirCategory(
    val titleId: String,
    val titleArabic: String
) {
    RINGKAS_KONTEMPORER("Ringkas & Kontemporer", "الميسرة والمعاصرة"),
    MUTAWASSITH("Mutawassith", "متوسطة وجامعة"),
    ENSIKLOPEDI("Ensiklopedi", "موسوعات"),
    BAHASA_BALAGHAH("Bahasa & Balaghah", "لغة وبلاغة"),
    PILIHAN("Pilihan", "منتقاة")
}

@Immutable
data class TafsirBook(
    val id: String,
    val nameLatin: String,
    val nameArabic: String,
    val authorArabic: String,
    val volumeNoteArabic: String,
    val category: TafsirCategory,
    val cdnSlug: String?,
    val tafsirAppSlug: String,
    val isSelective: Boolean = false,
    val isScannedOnly: Boolean = false,
    val specialNoteId: String = ""
) {
    fun tafsirAppUrl(surah: Int, ayah: Int): String {
        return "https://tafsir.app/$tafsirAppSlug/$surah/$ayah"
    }
}

@Immutable
data class QuranSurahMeta(
    val number: Int,
    val nameLatin: String,
    val nameArabic: String,
    val ayahCount: Int,
    val revelationType: String
)

@Immutable
data class TafsirFootnote(
    val index: Int,
    val text: String
)

@Immutable
data class TafsirParagraph(
    val body: String,
    val footnotes: List<TafsirFootnote> = emptyList()
)

@Immutable
data class ParsedTafsirContent(
    val paragraphs: List<TafsirParagraph>,
    val sourceLabel: String,
    val fromCache: Boolean = false
)

sealed interface TafsirLoadState {
    data object Loading : TafsirLoadState
    data class Ready(val content: ParsedTafsirContent) : TafsirLoadState
    data class EmptyForAyah(
        val book: TafsirBook,
        val surah: Int,
        val ayah: Int,
        val externalUrl: String
    ) : TafsirLoadState
    data class ScannedOnly(
        val book: TafsirBook,
        val surah: Int,
        val ayah: Int,
        val externalUrl: String
    ) : TafsirLoadState
    data class Error(
        val message: String,
        val externalUrl: String
    ) : TafsirLoadState
}

enum class TafsirFontSize(
    val label: String,
    val bodySp: Float,
    val ayahSp: Float,
    val lineHeightMultiplier: Float
) {
    SM("SM", 16f, 21f, 1.88f),
    MD("MD", 18.5f, 24f, 1.90f),
    LG("LG", 21f, 27f, 1.92f),
    XL("XL", 24f, 30f, 1.92f);

    companion object {
        fun fromName(name: String?): TafsirFontSize {
            return entries.firstOrNull { it.name == name } ?: MD
        }
    }
}

enum class TafsirPaperTheme(
    val label: String,
    val subtitle: String
) {
    FOLLOW_THEME("Ikuti Tema", "Selaras tema aplikasi"),
    SEPIA("Sepia", "Kertas kitab hangat"),
    DARK("Gelap", "Nyaman baca malam");

    companion object {
        fun fromName(name: String?): TafsirPaperTheme {
            return entries.firstOrNull { it.name == name } ?: FOLLOW_THEME
        }
    }
}

@Immutable
data class TafsirBookmark(
    val id: String,
    val bookId: String,
    val bookNameLatin: String,
    val surah: Int,
    val ayah: Int,
    val surahNameLatin: String,
    val surahNameArabic: String,
    val ayahPreview: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(bookId: String, surah: Int, ayah: Int): String = "$bookId:$surah:$ayah"
    }
}

@Immutable
data class TafsirLastRead(
    val bookId: String,
    val surah: Int,
    val ayah: Int,
    val updatedAtMillis: Long
)

@Immutable
data class TafsirAyahSearchHit(
    val surah: Int,
    val ayah: Int,
    val surahNameLatin: String,
    val surahNameArabic: String,
    val arabicSnippet: String,
    val secondarySnippet: String = "",
    val sourceLabel: String = ""
)

@Immutable
data class TafsirUiState(
    val activeBook: TafsirBook = TafsirCatalog.defaultBook,
    val surahNumber: Int = 1,
    val ayahNumber: Int = 1,
    val ayahText: String = "",
    val isLoadingAyah: Boolean = false,
    val ayahErrorMessage: String? = null,
    val tafsirState: TafsirLoadState = TafsirLoadState.Loading,
    val showHarakat: Boolean = true,
    val fontSize: TafsirFontSize = TafsirFontSize.MD,
    val paperTheme: TafsirPaperTheme = TafsirPaperTheme.FOLLOW_THEME,
    val isAyahCollapsed: Boolean = false,
    val bookmarks: List<TafsirBookmark> = emptyList(),
    val lastRead: TafsirLastRead? = null,
    val ayahSearchQuery: String = "",
    val ayahSearchResults: List<TafsirAyahSearchHit> = emptyList(),
    val isSearchingAyah: Boolean = false,
    val ayahSearchError: String? = null
) {
    val activeSurah: QuranSurahMeta
        get() = TafsirCatalog.surahByNumber(surahNumber)

    val isCurrentBookmarked: Boolean
        get() {
            val targetId = TafsirBookmark.buildId(activeBook.id, surahNumber, ayahNumber)
            return bookmarks.any { it.id == targetId }
        }

    val hasPreviousAyah: Boolean
        get() = surahNumber > 1 || ayahNumber > 1

    val hasNextAyah: Boolean
        get() = surahNumber < 114 || ayahNumber < activeSurah.ayahCount
}
