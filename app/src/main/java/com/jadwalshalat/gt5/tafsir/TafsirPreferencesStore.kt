package com.jadwalshalat.gt5.tafsir

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan lokal khusus modul Pustaka Tafsir (SharedPreferences).
 * Sengaja dipisahkan dan tidak menyentuh Firestore `/users/{uid}/progress/learning_v1`
 * agar skema cloud sync tetap 100% identik dengan versi web.
 */
class TafsirPreferencesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadLastRead(): TafsirLastRead? {
        val bookId = prefs.getString(KEY_LAST_BOOK_ID, null) ?: return null
        val surah = prefs.getInt(KEY_LAST_SURAH, 1).coerceIn(1, 114)
        val maxAyah = TafsirCatalog.surahByNumber(surah).ayahCount
        val ayah = prefs.getInt(KEY_LAST_AYAH, 1).coerceIn(1, maxAyah)
        val updatedAt = prefs.getLong(KEY_LAST_UPDATED_AT, 0L)
        return TafsirLastRead(
            bookId = TafsirCatalog.bookById(bookId).id,
            surah = surah,
            ayah = ayah,
            updatedAtMillis = updatedAt
        )
    }

    fun saveLastRead(bookId: String, surah: Int, ayah: Int): TafsirLastRead {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_LAST_BOOK_ID, bookId)
            .putInt(KEY_LAST_SURAH, surah)
            .putInt(KEY_LAST_AYAH, ayah)
            .putLong(KEY_LAST_UPDATED_AT, now)
            .apply()
        return TafsirLastRead(bookId, surah, ayah, now)
    }

    fun loadShowHarakat(): Boolean = prefs.getBoolean(KEY_SHOW_HARAKAT, true)

    fun saveShowHarakat(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HARAKAT, show).apply()
    }

    fun loadFontSize(): TafsirFontSize = TafsirFontSize.fromName(prefs.getString(KEY_FONT_SIZE, null))

    fun saveFontSize(size: TafsirFontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
    }

    fun loadPaperTheme(): TafsirPaperTheme = TafsirPaperTheme.fromName(prefs.getString(KEY_PAPER_THEME, null))

    fun savePaperTheme(theme: TafsirPaperTheme) {
        prefs.edit().putString(KEY_PAPER_THEME, theme.name).apply()
    }

    fun loadAyahCollapsed(): Boolean = prefs.getBoolean(KEY_AYAH_COLLAPSED, false)

    fun saveAyahCollapsed(collapsed: Boolean) {
        prefs.edit().putBoolean(KEY_AYAH_COLLAPSED, collapsed).apply()
    }

    fun loadBookmarks(): List<TafsirBookmark> {
        val raw = prefs.getString(KEY_BOOKMARKS_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = ArrayList<TafsirBookmark>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val bookId = o.optString("bookId").ifBlank { TafsirCatalog.defaultBook.id }
                val surah = o.optInt("surah", 1).coerceIn(1, 114)
                val ayah = o.optInt("ayah", 1).coerceAtLeast(1)
                val surahMeta = TafsirCatalog.surahByNumber(surah)
                val book = TafsirCatalog.bookById(bookId)
                list += TafsirBookmark(
                    id = o.optString("id").ifBlank { TafsirBookmark.buildId(book.id, surah, ayah) },
                    bookId = book.id,
                    bookNameLatin = o.optString("bookNameLatin").ifBlank { book.nameLatin },
                    surah = surah,
                    ayah = ayah.coerceIn(1, surahMeta.ayahCount),
                    surahNameLatin = o.optString("surahNameLatin").ifBlank { surahMeta.nameLatin },
                    surahNameArabic = o.optString("surahNameArabic").ifBlank { surahMeta.nameArabic },
                    ayahPreview = o.optString("ayahPreview"),
                    createdAtMillis = o.optLong("createdAtMillis", 0L)
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        }.getOrDefault(emptyList())
    }

    fun saveBookmarks(bookmarks: List<TafsirBookmark>) {
        val arr = JSONArray()
        bookmarks.take(150).forEach { b ->
            arr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("bookId", b.bookId)
                    put("bookNameLatin", b.bookNameLatin)
                    put("surah", b.surah)
                    put("ayah", b.ayah)
                    put("surahNameLatin", b.surahNameLatin)
                    put("surahNameArabic", b.surahNameArabic)
                    put("ayahPreview", b.ayahPreview)
                    put("createdAtMillis", b.createdAtMillis)
                }
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS_JSON, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "nida_tafsir_local_prefs"
        private const val KEY_LAST_BOOK_ID = "last_book_id"
        private const val KEY_LAST_SURAH = "last_surah"
        private const val KEY_LAST_AYAH = "last_ayah"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
        private const val KEY_SHOW_HARAKAT = "show_harakat"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_PAPER_THEME = "paper_theme"
        private const val KEY_AYAH_COLLAPSED = "ayah_collapsed"
        private const val KEY_BOOKMARKS_JSON = "bookmarks_json"
    }
}
