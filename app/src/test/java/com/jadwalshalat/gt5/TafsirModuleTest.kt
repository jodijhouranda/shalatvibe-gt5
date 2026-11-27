package com.jadwalshalat.gt5

import com.jadwalshalat.gt5.tafsir.TafsirCatalog
import com.jadwalshalat.gt5.tafsir.TafsirCategory
import com.jadwalshalat.gt5.tafsir.TafsirTextProcessor
import com.jadwalshalat.gt5.tafsir.TafsirViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirModuleTest {

    @Test
    fun catalogHasExactly22BooksAcrossAll5CategoriesWithExactSlugs() {
        val books = TafsirCatalog.books
        assertEquals(22, books.size)
        assertEquals(22, books.map { it.id }.toSet().size)

        assertEquals(6, books.count { it.category == TafsirCategory.RINGKAS_KONTEMPORER })
        assertEquals(3, books.count { it.category == TafsirCategory.MUTAWASSITH })
        assertEquals(9, books.count { it.category == TafsirCategory.ENSIKLOPEDI })
        assertEquals(2, books.count { it.category == TafsirCategory.BAHASA_BALAGHAH })
        assertEquals(2, books.count { it.category == TafsirCategory.PILIHAN })

        // Ibnu Utsaimin harus langsung ke tafsir.app (cdnSlug == null)
        val uthaymeen = TafsirCatalog.bookById("ibn-uthaymeen")
        assertNull(uthaymeen.cdnSlug)
        assertEquals("ibn-uthaymeen", uthaymeen.tafsirAppSlug)
        assertFalse(uthaymeen.isScannedOnly)

        // Ibnu Taimiyyah harus berstatus scan halaman dengan tautan browser tepat
        val taymiyyah = TafsirCatalog.bookById("ibn-taymiyyah")
        assertNull(taymiyyah.cdnSlug)
        assertTrue(taymiyyah.isScannedOnly)
        assertEquals("https://tafsir.app/ibn-taymiyyah/2/255", taymiyyah.tafsirAppUrl(2, 255))

        // Adwa'ul Bayan ditandai selektif
        val adwa = TafsirCatalog.bookById("adwaa-albayan")
        assertTrue(adwa.isSelective)
        assertEquals("adwa-al-bayan", adwa.cdnSlug)
    }

    @Test
    fun quranSurahCatalogHas114SurahsAnd6236Ayahs() {
        val surahs = TafsirCatalog.surahs
        assertEquals(114, surahs.size)
        for (i in 1..114) {
            assertEquals(i, surahs[i - 1].number)
            assertTrue(surahs[i - 1].ayahCount > 0)
        }
        assertEquals(6236, surahs.sumOf { it.ayahCount })
        assertEquals(7, TafsirCatalog.surahByNumber(1).ayahCount)
        assertEquals(286, TafsirCatalog.surahByNumber(2).ayahCount)
        assertEquals(6, TafsirCatalog.surahByNumber(114).ayahCount)
    }

    @Test
    fun gundulModeNormalizesAlifWaslaAndFormatsArabicNumerals() {
        val uthmani = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
        val gundul = TafsirTextProcessor.formatArabicForDisplay(uthmani, showHarakat = false)
        assertEquals("بسم الله الرحمن الرحيم", gundul)
        assertEquals(uthmani, TafsirTextProcessor.formatArabicForDisplay(uthmani, showHarakat = true))
        assertEquals("﴿١﴾", TafsirTextProcessor.formatAyahOrnament(1))
        assertEquals("﴿٢٥٥﴾", TafsirTextProcessor.formatAyahOrnament(255))
    }

    @Test
    fun htmlCleanupAndFootnoteExtractionWorkCleanly() {
        val raw = "<p>قوله تعالى: &laquo;الحمد لله&raquo; [[انظر جامع البيان ١/١٣٥]] ثناءٌ على الله.<br/>والله أعلم [[رواه البخاري]].</p>"
        val paragraphs = TafsirTextProcessor.parseTafsirRawText(raw)
        assertEquals(2, paragraphs.size)
        assertFalse(paragraphs[0].body.contains("<p>"))
        assertFalse(paragraphs[0].body.contains("[["))
        assertTrue(paragraphs[0].body.contains("(١)"))
        assertEquals(1, paragraphs[0].footnotes.size)
        assertEquals("انظر جامع البيان ١/١٣٥", paragraphs[0].footnotes[0].text)

        assertTrue(paragraphs[1].body.contains("(٢)"))
        assertEquals(1, paragraphs[1].footnotes.size)
        assertEquals(2, paragraphs[1].footnotes[0].index)
        assertEquals("رواه البخاري", paragraphs[1].footnotes[0].text)
    }

    @Test
    fun tafsirKamusLessonIdParsesBackToReference() {
        val parsed = TafsirViewModel.parseTafsirKamusLessonId("tafsir:ibn-katheer:2:255")
        assertEquals(Triple("ibn-katheer", 2, 255), parsed)
        assertNull(TafsirViewModel.parseTafsirKamusLessonId("arabic_day_1"))
    }

    @Test
    fun directSurahAyahReferenceParsingWorksForNumbersAndNames() {
        assertEquals(2 to 255, com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("2:255"))
        assertEquals(18 to 10, com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("18 10"))
        assertEquals(2 to 255, com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("baqarah 255"))
        assertEquals(18 to 10, com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("الكهف 10"))
        assertNull(com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("1:99"))
        assertNull(com.jadwalshalat.gt5.tafsir.TafsirRepository.parseDirectReference("115:1"))
    }

    @Test
    fun focusedContextWindowSlicesLongTafsirToConciseSentenceAroundTappedWord() {
        val filler = "قال الإمام الطبري في تفسيره للآية الكريمة وهذا قول مشهور عند أهل العلم والتفسير "
        val targetSentence = "فإنَّ التَّدَبُّرَ يَفْتَحُ أَبْوَابَ الْهِدَايَةِ لِلْقَلْبِ السَّلِيمِ."
        val longTafsir = filler.repeat(12) + " " + targetSentence + " " + filler.repeat(12)
        val tokens = com.jadwalshalat.gt5.arabic.ArabicTextUtils.tokenizeSentenceWords(longTafsir)
        assertTrue(tokens.size > 200)

        val tappedIdx = tokens.indexOfFirst { it.normalizedKey == "التدبر" }
        assertTrue(tappedIdx > 100)

        val focused = TafsirTextProcessor.buildFocusedContextWindow(
            allTokens = tokens,
            startIndex = tappedIdx,
            endIndex = tappedIdx
        )

        assertTrue("Konteks terlalu panjang: ${focused.contextSentence.length}", focused.contextSentence.length <= 185)
        assertTrue(focused.tokens.size <= 20)
        assertEquals("التدبر", focused.tokens[focused.startIndex].normalizedKey)
        assertEquals(focused.startIndex, focused.endIndex)
        assertTrue(focused.contextSentence.contains("التَّدَبُّرَ"))
    }

    @Test
    fun extractContextSnippetMatchesGundulWordInsideVocalizedLongTafsir() {
        val filler = "وَقَالَ الْمُفَسِّرُونَ فِي مَعْنَى هَذِهِ الْآيَةِ كَلَامًا كَثِيرًا مَبْسُوطًا "
        val targetSentence = "وَالمَقْصُودُ بِالِاسْتِقَامَةِ لُزُومُ الطَّاعَةِ وَالثَّبَاتُ عَلَيْهَا."
        val longTafsir = filler.repeat(10) + " " + targetSentence + " " + filler.repeat(10)

        val snippet = TafsirTextProcessor.extractContextSnippet(
            fullParagraph = longTafsir,
            tappedWord = "بالاستقامة"
        )
        assertTrue("Snippet terlalu panjang: ${snippet.length}", snippet.length <= 185)
        assertTrue(snippet.contains("بِالِاسْتِقَامَةِ"))
    }
}
