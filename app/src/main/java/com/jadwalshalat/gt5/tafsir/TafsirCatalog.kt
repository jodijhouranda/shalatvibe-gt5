package com.jadwalshalat.gt5.tafsir

object TafsirCatalog {

    val books: List<TafsirBook> = listOf(
        // 1. Ringkas & Kontemporer / الميسرة والمعاصرة
        TafsirBook(
            id = "muyassar",
            nameLatin = "Al-Muyassar",
            nameArabic = "الميسر",
            authorArabic = "مجمع الملك فهد",
            volumeNoteArabic = "نحو مجلد",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = "ar-tafsir-muyassar",
            tafsirAppSlug = "muyassar"
        ),
        TafsirBook(
            id = "mukhtasar",
            nameLatin = "Al-Mukhtashar",
            nameArabic = "المختصر",
            authorArabic = "مركز تفسير",
            volumeNoteArabic = "نحو مجلد",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = "ar-tafsir-al-mukhtasar",
            tafsirAppSlug = "mukhtasar"
        ),
        TafsirBook(
            id = "saadi",
            nameLatin = "As-Sa'di",
            nameArabic = "تيسير الكريم الرحمن",
            authorArabic = "السعدي ١٣٧٦ هـ",
            volumeNoteArabic = "نحو ٤ مجلدات",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = "ar-tafseer-al-saddi",
            tafsirAppSlug = "saadi"
        ),
        TafsirBook(
            id = "aysar-altafasir",
            nameLatin = "Al-Jaza'iri",
            nameArabic = "أيسر التفاسير",
            authorArabic = "أبو بكر الجزائري ١٤٣٩ هـ",
            volumeNoteArabic = "نحو ٣ مجلدات",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = "abu-bakr-jabir-al-jazairi",
            tafsirAppSlug = "aysar-altafasir"
        ),
        TafsirBook(
            id = "tadabbur-wa-amal",
            nameLatin = "Tadabbur & Amal",
            nameArabic = "القرآن – تدبّر وعمل",
            authorArabic = "شركة الخبرات الذكية",
            volumeNoteArabic = "نحو ٣ مجلدات",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = "tadabbur-wa-amal",
            tafsirAppSlug = "tadabbur-wa-amal"
        ),
        TafsirBook(
            id = "ibn-uthaymeen",
            nameLatin = "Ibnu Utsaimin",
            nameArabic = "تفسير القرآن الكريم",
            authorArabic = "ابن عثيمين ١٤٢١ هـ",
            volumeNoteArabic = "نحو ١٨ مجلدًا",
            category = TafsirCategory.RINGKAS_KONTEMPORER,
            cdnSlug = null, // Langsung ke tafsir.app (jangan lewat jsDelivr karena 403)
            tafsirAppSlug = "ibn-uthaymeen",
            isSelective = true,
            specialNoteId = "Dimuat langsung dari tafsir.app (beberapa surah/ayat yang belum dibukukan dapat kosong)."
        ),

        // 2. Mutawassith / متوسطة وجامعة
        TafsirBook(
            id = "fath-albayan",
            nameLatin = "Fathul Bayan",
            nameArabic = "فتح البيان",
            authorArabic = "صديق حسن خان ١٣٠٧ هـ",
            volumeNoteArabic = "نحو ١٢ مجلدًا",
            category = TafsirCategory.MUTAWASSITH,
            cdnSlug = "fath-al-bayan-li-al-qanuji",
            tafsirAppSlug = "fath-albayan"
        ),
        TafsirBook(
            id = "fath-alqadeer",
            nameLatin = "Fathul Qadir",
            nameArabic = "فتح القدير",
            authorArabic = "الشوكاني ١٢٥٠ هـ",
            volumeNoteArabic = "نحو ١١ مجلدًا",
            category = TafsirCategory.MUTAWASSITH,
            cdnSlug = "fath-al-qadir-al-shawkani",
            tafsirAppSlug = "fath-alqadeer"
        ),
        TafsirBook(
            id = "altasheel",
            nameLatin = "At-Tashil",
            nameArabic = "التسهيل لعلوم التنزيل",
            authorArabic = "ابن جُزَيّ ٧٤١ هـ",
            volumeNoteArabic = "نحو ٣ مجلدات",
            category = TafsirCategory.MUTAWASSITH,
            cdnSlug = "tafsir-ibn-juzay",
            tafsirAppSlug = "altasheel"
        ),

        // 3. Ensiklopedi / موسوعات
        TafsirBook(
            id = "alaloosi",
            nameLatin = "Ruhul Ma'ani",
            nameArabic = "روح المعاني",
            authorArabic = "الآلوسي ١٢٧٠ هـ",
            volumeNoteArabic = "نحو ٢٨ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "tafsir-al-alusi",
            tafsirAppSlug = "alaloosi"
        ),
        TafsirBook(
            id = "tabari",
            nameLatin = "Ath-Thabari",
            nameArabic = "جامع البيان",
            authorArabic = "ابن جرير الطبري ٣١٠ هـ",
            volumeNoteArabic = "نحو ٢٨ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "ar-tafsir-al-tabari",
            tafsirAppSlug = "tabari"
        ),
        TafsirBook(
            id = "ibn-katheer",
            nameLatin = "Ibnu Katsir",
            nameArabic = "تفسير القرآن العظيم",
            authorArabic = "ابن كثير ٧٧٤ هـ",
            volumeNoteArabic = "نحو ١٩ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "ar-tafsir-ibn-kathir",
            tafsirAppSlug = "ibn-katheer"
        ),
        TafsirBook(
            id = "qurtubi",
            nameLatin = "Al-Qurthubi",
            nameArabic = "الجامع لأحكام القرآن",
            authorArabic = "القرطبي ٦٧١ هـ",
            volumeNoteArabic = "نحو ٢٤ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "ar-tafseer-al-qurtubi",
            tafsirAppSlug = "qurtubi"
        ),
        TafsirBook(
            id = "baghawi",
            nameLatin = "Al-Baghawi",
            nameArabic = "معالم التنزيل",
            authorArabic = "البغوي ٥١٦ هـ",
            volumeNoteArabic = "نحو ١١ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "ar-tafsir-al-baghawi",
            tafsirAppSlug = "baghawi"
        ),
        TafsirBook(
            id = "zad-almaseer",
            nameLatin = "Zadul Masir",
            nameArabic = "زاد المسير",
            authorArabic = "ابن الجوزي ٥٩٧ هـ",
            volumeNoteArabic = "نحو ٥ مجلدات",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "tafsir-ibn-al-jawzi",
            tafsirAppSlug = "zad-almaseer"
        ),
        TafsirBook(
            id = "almawirdee",
            nameLatin = "An-Nukat wal-Uyun",
            nameArabic = "النكت والعيون",
            authorArabic = "الماوردي ٤٥٠ هـ",
            volumeNoteArabic = "نحو ٦ مجلدات",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "tafsir-al-mawardi",
            tafsirAppSlug = "almawirdee"
        ),
        TafsirBook(
            id = "adwaa-albayan",
            nameLatin = "Adwa'ul Bayan",
            nameArabic = "أضواء البيان",
            authorArabic = "محمد الأمين الشنقيطي ١٣٩٤ هـ",
            volumeNoteArabic = "نحو ١١ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "adwa-al-bayan",
            tafsirAppSlug = "adwaa-albayan",
            isSelective = true,
            specialNoteId = "Kitab ini bersifat selektif (menafsirkan Al-Qur'an dengan Al-Qur'an), sehingga sebagian ayat memang tidak memiliki entri tafsir."
        ),
        TafsirBook(
            id = "nathm-aldurar",
            nameLatin = "Nazhmud Durar",
            nameArabic = "نظم الدرر",
            authorArabic = "البقاعي ٨٨٥ هـ",
            volumeNoteArabic = "نحو ٢٠ مجلدًا",
            category = TafsirCategory.ENSIKLOPEDI,
            cdnSlug = "nazam-al-durar-al-biqa-i",
            tafsirAppSlug = "nathm-aldurar"
        ),

        // 4. Bahasa & Balaghah / لغة وبلاغة
        TafsirBook(
            id = "ibn-aashoor",
            nameLatin = "At-Tahrir wat-Tanwir",
            nameArabic = "التحرير والتنوير",
            authorArabic = "ابن عاشور ١٣٩٣ هـ",
            volumeNoteArabic = "نحو ٢٤ مجلدًا",
            category = TafsirCategory.BAHASA_BALAGHAH,
            cdnSlug = "ar-tafseer-tahrir-al-tanwir",
            tafsirAppSlug = "ibn-aashoor"
        ),
        TafsirBook(
            id = "ibn-atiyah",
            nameLatin = "Al-Muharrar Al-Wajiz",
            nameArabic = "المحرر الوجيز",
            authorArabic = "ابن عطية ٥٤٦ هـ",
            volumeNoteArabic = "نحو ٨ مجلدات",
            category = TafsirCategory.BAHASA_BALAGHAH,
            cdnSlug = "al-muharrar-al-wajiz-ibn-atiyyah",
            tafsirAppSlug = "ibn-atiyah"
        ),

        // 5. Pilihan / منتقاة
        TafsirBook(
            id = "ibn-alqayyim",
            nameLatin = "Tafsir Ibnul Qayyim",
            nameArabic = "تفسير ابن قيّم الجوزيّة",
            authorArabic = "ابن القيم ٧٥١ هـ",
            volumeNoteArabic = "نحو ١٢ مجلدًا",
            category = TafsirCategory.PILIHAN,
            cdnSlug = "tafsir-ibn-al-qayyim",
            tafsirAppSlug = "ibn-alqayyim",
            isSelective = true,
            specialNoteId = "Kumpulan tafsir tematik/pilihan dari karya-karya Ibnul Qayyim; sebagian ayat mungkin tidak memiliki catatan."
        ),
        TafsirBook(
            id = "ibn-taymiyyah",
            nameLatin = "Tafsir Syaikhul Islam Ibnu Taimiyyah",
            nameArabic = "تفسير شيخ الإسلام",
            authorArabic = "ابن تيمية ٧٢٨ هـ",
            volumeNoteArabic = "نحو ٧ مجلدات",
            category = TafsirCategory.PILIHAN,
            cdnSlug = null,
            tafsirAppSlug = "ibn-taymiyyah",
            isScannedOnly = true,
            specialNoteId = "Di tafsir.app kitab ini tersedia dalam format pindaian halaman (scan) tanpa teks digital."
        )
    )

    val defaultBook: TafsirBook = books.first()

    fun bookById(id: String?): TafsirBook {
        if (id.isNullOrBlank()) return defaultBook
        return books.firstOrNull { it.id == id || it.tafsirAppSlug == id || it.cdnSlug == id } ?: defaultBook
    }

    val surahs: List<QuranSurahMeta> = listOf(
        QuranSurahMeta(1, "Al-Fatihah", "الفاتحة", 7, "Makkiyah"),
        QuranSurahMeta(2, "Al-Baqarah", "البقرة", 286, "Madaniyah"),
        QuranSurahMeta(3, "Ali 'Imran", "آل عمران", 200, "Madaniyah"),
        QuranSurahMeta(4, "An-Nisa'", "النساء", 176, "Madaniyah"),
        QuranSurahMeta(5, "Al-Ma'idah", "المائدة", 120, "Madaniyah"),
        QuranSurahMeta(6, "Al-An'am", "الأنعام", 165, "Makkiyah"),
        QuranSurahMeta(7, "Al-A'raf", "الأعراف", 206, "Makkiyah"),
        QuranSurahMeta(8, "Al-Anfal", "الأنفال", 75, "Madaniyah"),
        QuranSurahMeta(9, "At-Taubah", "التوبة", 129, "Madaniyah"),
        QuranSurahMeta(10, "Yunus", "يونس", 109, "Makkiyah"),
        QuranSurahMeta(11, "Hud", "هود", 123, "Makkiyah"),
        QuranSurahMeta(12, "Yusuf", "يوسف", 111, "Makkiyah"),
        QuranSurahMeta(13, "Ar-Ra'd", "الرعد", 43, "Madaniyah"),
        QuranSurahMeta(14, "Ibrahim", "إبراهيم", 52, "Makkiyah"),
        QuranSurahMeta(15, "Al-Hijr", "الحجر", 99, "Makkiyah"),
        QuranSurahMeta(16, "An-Nahl", "النحل", 128, "Makkiyah"),
        QuranSurahMeta(17, "Al-Isra'", "الإسراء", 111, "Makkiyah"),
        QuranSurahMeta(18, "Al-Kahfi", "الكهف", 110, "Makkiyah"),
        QuranSurahMeta(19, "Maryam", "مريم", 98, "Makkiyah"),
        QuranSurahMeta(20, "Taha", "طه", 135, "Makkiyah"),
        QuranSurahMeta(21, "Al-Anbiya'", "الأنبياء", 112, "Makkiyah"),
        QuranSurahMeta(22, "Al-Hajj", "الحج", 78, "Madaniyah"),
        QuranSurahMeta(23, "Al-Mu'minun", "المؤمنون", 118, "Makkiyah"),
        QuranSurahMeta(24, "An-Nur", "النور", 64, "Madaniyah"),
        QuranSurahMeta(25, "Al-Furqan", "الفرقان", 77, "Makkiyah"),
        QuranSurahMeta(26, "Asy-Syu'ara'", "الشعراء", 227, "Makkiyah"),
        QuranSurahMeta(27, "An-Naml", "النمل", 93, "Makkiyah"),
        QuranSurahMeta(28, "Al-Qasas", "القصص", 88, "Makkiyah"),
        QuranSurahMeta(29, "Al-'Ankabut", "العنكبوت", 69, "Makkiyah"),
        QuranSurahMeta(30, "Ar-Rum", "الروم", 60, "Makkiyah"),
        QuranSurahMeta(31, "Luqman", "لقمان", 34, "Makkiyah"),
        QuranSurahMeta(32, "As-Sajdah", "السجدة", 30, "Makkiyah"),
        QuranSurahMeta(33, "Al-Ahzab", "الأحزاب", 73, "Madaniyah"),
        QuranSurahMeta(34, "Saba'", "سبأ", 54, "Makkiyah"),
        QuranSurahMeta(35, "Fatir", "فاطر", 45, "Makkiyah"),
        QuranSurahMeta(36, "Yasin", "يس", 83, "Makkiyah"),
        QuranSurahMeta(37, "As-Saffat", "الصافات", 182, "Makkiyah"),
        QuranSurahMeta(38, "Sad", "ص", 88, "Makkiyah"),
        QuranSurahMeta(39, "Az-Zumar", "الزمر", 75, "Makkiyah"),
        QuranSurahMeta(40, "Ghafir", "غافر", 85, "Makkiyah"),
        QuranSurahMeta(41, "Fussilat", "فصلت", 54, "Makkiyah"),
        QuranSurahMeta(42, "Asy-Syura", "الشورى", 53, "Makkiyah"),
        QuranSurahMeta(43, "Az-Zukhruf", "الزخرف", 89, "Makkiyah"),
        QuranSurahMeta(44, "Ad-Dukhan", "الدخان", 59, "Makkiyah"),
        QuranSurahMeta(45, "Al-Jasiyah", "الجاثية", 37, "Makkiyah"),
        QuranSurahMeta(46, "Al-Ahqaf", "الأحقاف", 35, "Makkiyah"),
        QuranSurahMeta(47, "Muhammad", "محمد", 38, "Madaniyah"),
        QuranSurahMeta(48, "Al-Fath", "الفتح", 29, "Madaniyah"),
        QuranSurahMeta(49, "Al-Hujurat", "الحجرات", 18, "Madaniyah"),
        QuranSurahMeta(50, "Qaf", "ق", 45, "Makkiyah"),
        QuranSurahMeta(51, "Az-Zariyat", "الذاريات", 60, "Makkiyah"),
        QuranSurahMeta(52, "At-Tur", "الطور", 49, "Makkiyah"),
        QuranSurahMeta(53, "An-Najm", "النجم", 62, "Makkiyah"),
        QuranSurahMeta(54, "Al-Qamar", "القمر", 55, "Makkiyah"),
        QuranSurahMeta(55, "Ar-Rahman", "الرحمن", 78, "Madaniyah"),
        QuranSurahMeta(56, "Al-Waqi'ah", "الواقعة", 96, "Makkiyah"),
        QuranSurahMeta(57, "Al-Hadid", "الحديد", 29, "Madaniyah"),
        QuranSurahMeta(58, "Al-Mujadalah", "المجادلة", 22, "Madaniyah"),
        QuranSurahMeta(59, "Al-Hasyr", "الحشر", 24, "Madaniyah"),
        QuranSurahMeta(60, "Al-Mumtahanah", "الممتحنة", 13, "Madaniyah"),
        QuranSurahMeta(61, "As-Saff", "الصف", 14, "Madaniyah"),
        QuranSurahMeta(62, "Al-Jumu'ah", "الجمعة", 11, "Madaniyah"),
        QuranSurahMeta(63, "Al-Munafiqun", "المنافقون", 11, "Madaniyah"),
        QuranSurahMeta(64, "At-Taghabun", "التغابن", 18, "Madaniyah"),
        QuranSurahMeta(65, "At-Talaq", "الطلاق", 12, "Madaniyah"),
        QuranSurahMeta(66, "At-Tahrim", "التحريم", 12, "Madaniyah"),
        QuranSurahMeta(67, "Al-Mulk", "الملك", 30, "Makkiyah"),
        QuranSurahMeta(68, "Al-Qalam", "القلم", 52, "Makkiyah"),
        QuranSurahMeta(69, "Al-Haqqah", "الحاقة", 52, "Makkiyah"),
        QuranSurahMeta(70, "Al-Ma'arij", "المعارج", 44, "Makkiyah"),
        QuranSurahMeta(71, "Nuh", "نوح", 28, "Makkiyah"),
        QuranSurahMeta(72, "Al-Jinn", "الجن", 28, "Makkiyah"),
        QuranSurahMeta(73, "Al-Muzzammil", "المزمل", 20, "Makkiyah"),
        QuranSurahMeta(74, "Al-Muddassir", "المدثر", 56, "Makkiyah"),
        QuranSurahMeta(75, "Al-Qiyamah", "القيامة", 40, "Makkiyah"),
        QuranSurahMeta(76, "Al-Insan", "الإنسان", 31, "Madaniyah"),
        QuranSurahMeta(77, "Al-Mursalat", "المرسلات", 50, "Makkiyah"),
        QuranSurahMeta(78, "An-Naba'", "النبأ", 40, "Makkiyah"),
        QuranSurahMeta(79, "An-Nazi'at", "النازعات", 46, "Makkiyah"),
        QuranSurahMeta(80, "'Abasa", "عبس", 42, "Makkiyah"),
        QuranSurahMeta(81, "At-Takwir", "التكوير", 29, "Makkiyah"),
        QuranSurahMeta(82, "Al-Infitar", "الانفطار", 19, "Makkiyah"),
        QuranSurahMeta(83, "Al-Mutaffifin", "المطففين", 36, "Makkiyah"),
        QuranSurahMeta(84, "Al-Insyiqaq", "الانشقاق", 25, "Makkiyah"),
        QuranSurahMeta(85, "Al-Buruj", "البروج", 22, "Makkiyah"),
        QuranSurahMeta(86, "At-Tariq", "الطارق", 17, "Makkiyah"),
        QuranSurahMeta(87, "Al-A'la", "الأعلى", 19, "Makkiyah"),
        QuranSurahMeta(88, "Al-Ghasyiyah", "الغاشية", 26, "Makkiyah"),
        QuranSurahMeta(89, "Al-Fajr", "الفجر", 30, "Makkiyah"),
        QuranSurahMeta(90, "Al-Balad", "البلد", 20, "Makkiyah"),
        QuranSurahMeta(91, "Asy-Syams", "الشمس", 15, "Makkiyah"),
        QuranSurahMeta(92, "Al-Lail", "الليل", 21, "Makkiyah"),
        QuranSurahMeta(93, "Ad-Duha", "الضحى", 11, "Makkiyah"),
        QuranSurahMeta(94, "Asy-Syarh", "الشرح", 8, "Makkiyah"),
        QuranSurahMeta(95, "At-Tin", "التين", 8, "Makkiyah"),
        QuranSurahMeta(96, "Al-'Alaq", "العلق", 19, "Makkiyah"),
        QuranSurahMeta(97, "Al-Qadr", "القدر", 5, "Makkiyah"),
        QuranSurahMeta(98, "Al-Bayyinah", "البينة", 8, "Madaniyah"),
        QuranSurahMeta(99, "Az-Zalzalah", "الزلزلة", 8, "Madaniyah"),
        QuranSurahMeta(100, "Al-'Adiyat", "العاديات", 11, "Makkiyah"),
        QuranSurahMeta(101, "Al-Qari'ah", "القارعة", 11, "Makkiyah"),
        QuranSurahMeta(102, "At-Takasur", "التكاثر", 8, "Makkiyah"),
        QuranSurahMeta(103, "Al-'Asr", "العصر", 3, "Makkiyah"),
        QuranSurahMeta(104, "Al-Humazah", "الهمزة", 9, "Makkiyah"),
        QuranSurahMeta(105, "Al-Fil", "الفيل", 5, "Makkiyah"),
        QuranSurahMeta(106, "Quraisy", "قريش", 4, "Makkiyah"),
        QuranSurahMeta(107, "Al-Ma'un", "الماعون", 7, "Makkiyah"),
        QuranSurahMeta(108, "Al-Kausar", "الكوثر", 3, "Makkiyah"),
        QuranSurahMeta(109, "Al-Kafirun", "الكافرون", 6, "Makkiyah"),
        QuranSurahMeta(110, "An-Nasr", "النصر", 3, "Madaniyah"),
        QuranSurahMeta(111, "Al-Lahab", "المسد", 5, "Makkiyah"),
        QuranSurahMeta(112, "Al-Ikhlas", "الإخلاص", 4, "Makkiyah"),
        QuranSurahMeta(113, "Al-Falaq", "الفلق", 5, "Makkiyah"),
        QuranSurahMeta(114, "An-Nas", "الناس", 6, "Makkiyah")
    )

    fun surahByNumber(number: Int): QuranSurahMeta {
        return surahs.getOrElse(number - 1) { surahs.first() }
    }
}
