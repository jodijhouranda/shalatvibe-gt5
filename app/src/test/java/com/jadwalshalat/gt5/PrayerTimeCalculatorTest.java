package com.jadwalshalat.gt5;

import com.jadwalshalat.gt5.model.CalculationMethod;
import com.jadwalshalat.gt5.model.CityPreset;
import com.jadwalshalat.gt5.model.PrayerSchedule;
import com.jadwalshalat.gt5.model.PrayerType;
import com.jadwalshalat.gt5.prayer.HijriCalendarHelper;
import com.jadwalshalat.gt5.prayer.PrayerEventPlanner;
import com.jadwalshalat.gt5.prayer.PrayerTimeCalculator;
import com.jadwalshalat.gt5.prayer.QiblaCalculator;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrayerTimeCalculatorTest {

    @Test
    public void jakartaScheduleIsOrderedAndReasonable() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        LocalDate date = LocalDate.of(2026, 11, 13);

        PrayerSchedule schedule = PrayerTimeCalculator.calculate(
                date, -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );

        ZonedDateTime subuh = schedule.getTime(PrayerType.SUBUH);
        ZonedDateTime terbit = schedule.getTime(PrayerType.TERBIT);
        ZonedDateTime dzuhur = schedule.getTime(PrayerType.DZUHUR);
        ZonedDateTime ashar = schedule.getTime(PrayerType.ASHAR);
        ZonedDateTime maghrib = schedule.getTime(PrayerType.MAGHRIB);
        ZonedDateTime isya = schedule.getTime(PrayerType.ISYA);

        assertTrue(subuh.isBefore(terbit));
        assertTrue(terbit.isBefore(dzuhur));
        assertTrue(dzuhur.isBefore(ashar));
        assertTrue(ashar.isBefore(maghrib));
        assertTrue(maghrib.isBefore(isya));

        // Jakarta pertengahan November: Subuh sekitar 04:05-04:15, Dzuhur sekitar 11:38-11:48, Maghrib sekitar 17:45-18:00
        assertTrue(subuh.toLocalTime().isAfter(LocalTime.of(3, 55))
                && subuh.toLocalTime().isBefore(LocalTime.of(4, 25)));
        assertTrue(dzuhur.toLocalTime().isAfter(LocalTime.of(11, 30))
                && dzuhur.toLocalTime().isBefore(LocalTime.of(11, 55)));
        assertTrue(maghrib.toLocalTime().isAfter(LocalTime.of(17, 40))
                && maghrib.toLocalTime().isBefore(LocalTime.of(18, 5)));
    }

    @Test
    public void plannerSchedulesNextDaySubuhAfterIsya() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        ZonedDateTime lateNight = ZonedDateTime.of(2026, 11, 13, 22, 30, 0, 0, wib);

        PrayerEventPlanner.ScheduledEvent next = PrayerEventPlanner.findNextEvent(
                lateNight,
                -6.1754,
                106.8272,
                wib,
                CalculationMethod.KEMENAG_RI,
                true,
                EnumSet.of(PrayerType.SUBUH, PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
                10,
                true
        );

        assertNotNull(next);
        assertEquals(PrayerType.SUBUH, next.getPrayerType());
        assertEquals(LocalDate.of(2026, 11, 14), next.getTriggerTime().toLocalDate());
        assertTrue(next.isPreReminder());
    }

    @Test
    public void plannerRespectsPerPrayerPreReminderOverride() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        ZonedDateTime lateNight = ZonedDateTime.of(2026, 11, 13, 22, 30, 0, 0, wib);

        EnumMap<PrayerType, Integer> overrides = new EnumMap<>(PrayerType.class);
        for (PrayerType t : PrayerType.values()) {
            overrides.put(t, 10);
        }
        overrides.put(PrayerType.SUBUH, 25);

        PrayerEventPlanner.ScheduledEvent next = PrayerEventPlanner.findNextEvent(
                lateNight,
                -6.1754,
                106.8272,
                wib,
                CalculationMethod.KEMENAG_RI,
                true,
                EnumSet.of(PrayerType.SUBUH, PrayerType.DZUHUR, PrayerType.ASHAR, PrayerType.MAGHRIB, PrayerType.ISYA),
                overrides,
                true
        );

        assertNotNull(next);
        assertEquals(PrayerType.SUBUH, next.getPrayerType());
        assertTrue(next.isPreReminder());
        assertEquals(25, next.getMinutesBefore());
        assertEquals(next.getPrayerTime().minusMinutes(25), next.getTriggerTime());
    }

    @Test
    public void solarWindowComputesProgressInRange() {
        ZoneId wib = ZoneId.of("Asia/Jakarta");
        LocalDate today = LocalDate.of(2026, 11, 13);
        PrayerSchedule yesterday = PrayerTimeCalculator.calculate(
                today.minusDays(1), -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );
        PrayerSchedule todaySched = PrayerTimeCalculator.calculate(
                today, -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );
        PrayerSchedule tomorrow = PrayerTimeCalculator.calculate(
                today.plusDays(1), -6.1754, 106.8272, wib, CalculationMethod.KEMENAG_RI, true
        );

        ZonedDateTime midAfternoon = ZonedDateTime.of(today, LocalTime.of(13, 30), wib);
        PrayerEventPlanner.SolarWindow window = PrayerEventPlanner.computeSolarWindow(
                yesterday, todaySched, tomorrow, midAfternoon
        );

        assertEquals(PrayerType.DZUHUR, window.getCurrentPhase());
        assertEquals(PrayerType.ASHAR, window.getNextPrayer());
        assertTrue(window.getProgress() > 0.2f && window.getProgress() < 0.8f);
    }

    @Test
    public void hijriDateFormatsOfflineInIndonesian() {
        String formatted = HijriCalendarHelper.INSTANCE.formatIndonesianHijriDate(LocalDate.of(2026, 11, 19));
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        assertTrue(formatted.endsWith(" H"));
    }

    @Test
    public void cityPresetsContainExpandedIndonesianCities() {
        List<CityPreset> presets = CityPreset.getIndonesianPresets();
        assertTrue(presets.size() >= 50);
        for (CityPreset city : presets) {
            assertNotNull(city.getName());
            assertFalse(city.getName().isEmpty());
            assertTrue(
                    "WIB".equals(city.getTimezoneLabel())
                            || "WITA".equals(city.getTimezoneLabel())
                            || "WIT".equals(city.getTimezoneLabel())
            );
        }
    }

    @Test
    public void qiblaBearingJakartaIsWestNorthwest() {
        double bearing = QiblaCalculator.calculateBearingDegrees(-6.1754, 106.8272);
        double distance = QiblaCalculator.calculateDistanceKm(-6.1754, 106.8272);

        assertTrue(bearing > 294.0 && bearing < 296.0);
        assertTrue(distance > 7800.0 && distance < 8100.0);
        assertEquals("Barat Laut", QiblaCalculator.formatCardinalDirection(bearing));
    }

    @Test
    public void arabicTimestampAndDriveUrlHelpersWorkCorrectly() {
        assertEquals("06:08", com.jadwalshalat.gt5.arabic.ArabicCatalogParser.INSTANCE.formatTimestamp(368));
        assertEquals("08:15", com.jadwalshalat.gt5.arabic.ArabicCatalogParser.INSTANCE.formatTimestamp(495));
        assertEquals("1:01:05", com.jadwalshalat.gt5.arabic.ArabicCatalogParser.INSTANCE.formatTimestamp(3665));

        String sharedDrive = "https://drive.google.com/file/d/1u356ZGCsizgkf6uxCfIaRUZsxd7ImsHq/view?usp=sharing";
        assertEquals(
                "https://drive.google.com/uc?export=download&id=1u356ZGCsizgkf6uxCfIaRUZsxd7ImsHq",
                com.jadwalshalat.gt5.arabic.ArabicCatalogParser.INSTANCE.normalizeDownloadUrl(sharedDrive)
        );
    }

    @Test
    public void courseAssetsAndCourseModelsWorkCleanly() throws Exception {
        java.nio.file.Path coursesPath = java.nio.file.Paths.get("src/main/assets/courses/courses.json");
        if (!java.nio.file.Files.exists(coursesPath)) {
            coursesPath = java.nio.file.Paths.get("app/src/main/assets/courses/courses.json");
        }
        assertTrue(java.nio.file.Files.exists(coursesPath));
        String indexJson = new String(
                java.nio.file.Files.readAllBytes(coursesPath),
                java.nio.charset.StandardCharsets.UTF_8
        );
        assertTrue(indexJson.contains("arabic_tafl"));
        assertTrue(indexJson.contains("fiqh_muamalah_syuwaiir"));

        java.nio.file.Path fiqhJsonPath = coursesPath.getParent().resolve("fiqh_muamalat_syuwaiir.json");
        java.nio.file.Path fiqhPdfPath = coursesPath.getParent().resolve("fiqh_muamalat_syuwaiir.pdf");
        assertTrue(java.nio.file.Files.exists(fiqhJsonPath));
        assertTrue(java.nio.file.Files.exists(fiqhPdfPath));
        assertTrue(java.nio.file.Files.size(fiqhPdfPath) > 500_000L);

        String fiqhJson = new String(
                java.nio.file.Files.readAllBytes(fiqhJsonPath),
                java.nio.charset.StandardCharsets.UTF_8
        );
        assertTrue(fiqhJson.contains("p-wwtkRQz7A"));
        assertTrue(fiqhJson.contains("tItFx4_yhQc"));
        assertTrue(fiqhJson.contains("dadT07MTN_Q"));
        assertTrue(fiqhJson.contains("rfa9-WXe3YY"));
        assertTrue(fiqhJson.contains("aCkcJATSLlw"));

        com.jadwalshalat.gt5.learning.TopicBlock block = new com.jadwalshalat.gt5.learning.TopicBlock(
                "s1_b1",
                1,
                "Blok 1: Pengantar",
                0,
                787,
                1,
                "Ringkasan",
                java.util.Collections.singletonList("furu_fiqhiyyah"),
                java.util.Collections.emptyList()
        );
        assertEquals("00:00 – 13:07", block.getTimeRangeLabel());
        assertEquals(13, block.getDurationMinutes());

        com.jadwalshalat.gt5.learning.GlossaryTerm term = new com.jadwalshalat.gt5.learning.GlossaryTerm(
                "furu_fiqhiyyah",
                "الفروع الفقهية",
                "Al-Furu' Al-Fiqhiyyah",
                "Cabang Fikih",
                "Makna",
                "Catatan",
                "Contoh",
                1,
                "s1_b1",
                180
        );
        com.jadwalshalat.gt5.learning.CourseEpisode episode = new com.jadwalshalat.gt5.learning.CourseEpisode(
                1,
                "seri_1",
                "Seri 1",
                "المجلس الأول",
                "p-wwtkRQz7A",
                6022,
                1,
                27,
                "Summary",
                java.util.Collections.singletonList(block)
        );
        com.jadwalshalat.gt5.learning.SeriesCourse course = new com.jadwalshalat.gt5.learning.SeriesCourse(
                "fiqh_muamalah_syuwaiir",
                "Fiqh Muamalah",
                "فقه المعاملات المالية",
                "Syaikh Abdussalam",
                "Subtitle",
                "Book",
                "fiqh_muamalat_syuwaiir.pdf",
                "",
                95,
                java.util.Collections.singletonList(term),
                java.util.Collections.singletonList(episode)
        );
        assertEquals(1, course.getTotalBlockCount());
        assertNotNull(course.glossaryById("furu_fiqhiyyah"));
        assertNotNull(course.findEpisodeAndBlock("s1_b1"));
        assertEquals("1:40:22", episode.getDurationLabel());
        assertEquals("Hal. 1–27", episode.getPdfRangeLabel());
        assertEquals(
                com.jadwalshalat.gt5.learning.LearningTrackType.SERIES_COURSE,
                com.jadwalshalat.gt5.learning.LearningTrackType.Companion.fromRaw("SERIES_COURSE")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fiqhMuamalahTranscriptAndTimestampsAreContinuousAndSynced() throws Exception {
        java.io.File lessonsFile = new java.io.File("lessons/courses/fiqh_muamalat_syuwaiir.json");
        if (!lessonsFile.exists()) {
            lessonsFile = new java.io.File("../lessons/courses/fiqh_muamalat_syuwaiir.json");
        }
        java.io.File assetsFile = new java.io.File("app/src/main/assets/courses/fiqh_muamalat_syuwaiir.json");
        if (!assetsFile.exists()) {
            assetsFile = new java.io.File("src/main/assets/courses/fiqh_muamalat_syuwaiir.json");
        }
        org.junit.Assert.assertTrue("lessons course JSON missing", lessonsFile.exists());
        org.junit.Assert.assertTrue("assets course JSON missing", assetsFile.exists());

        byte[] lessonsBytes = java.nio.file.Files.readAllBytes(lessonsFile.toPath());
        byte[] assetsBytes = java.nio.file.Files.readAllBytes(assetsFile.toPath());
        org.junit.Assert.assertArrayEquals(
                "lessons and assets course JSON must remain byte-identical",
                lessonsBytes,
                assetsBytes
        );

        String rawJson = new String(assetsBytes, java.nio.charset.StandardCharsets.UTF_8);
        java.util.Map<String, Object> course =
                (java.util.Map<String, Object>) new MinimalJsonParser(rawJson).parseValue();

        int bookTotalPages = ((Number) course.get("bookTotalPages")).intValue();
        org.junit.Assert.assertEquals(96, bookTotalPages);

        java.util.List<Object> episodes = (java.util.List<Object>) course.get("episodes");
        org.junit.Assert.assertEquals(5, episodes.size());

        java.util.Map<Integer, java.util.Map<String, int[]>> blockBoundsByEpisode = new java.util.HashMap<>();
        int totalCourseSegments = 0;
        long totalCourseSec = 0L;

        for (int epIdx = 0; epIdx < episodes.size(); epIdx++) {
            java.util.Map<String, Object> ep = (java.util.Map<String, Object>) episodes.get(epIdx);
            int epNum = ((Number) ep.get("episodeNumber")).intValue();
            int durationSec = ((Number) ep.get("durationSec")).intValue();
            int pdfStart = ((Number) ep.get("pdfStartPage")).intValue();
            int pdfEnd = ((Number) ep.get("pdfEndPage")).intValue();
            org.junit.Assert.assertEquals(epIdx + 1, epNum);
            org.junit.Assert.assertTrue(durationSec > 0);
            org.junit.Assert.assertTrue(pdfStart >= 1 && pdfEnd <= bookTotalPages && pdfStart <= pdfEnd);

            java.util.List<Object> blocks = (java.util.List<Object>) ep.get("blocks");
            org.junit.Assert.assertTrue("Episode " + epNum + " has no blocks", !blocks.isEmpty());

            java.util.Map<String, int[]> blockBounds = new java.util.HashMap<>();
            blockBoundsByEpisode.put(epNum, blockBounds);

            int prevBlockEnd = 0;
            int epSegCount = 0;
            for (int bIdx = 0; bIdx < blocks.size(); bIdx++) {
                java.util.Map<String, Object> block = (java.util.Map<String, Object>) blocks.get(bIdx);
                String blockId = (String) block.get("id");
                int bStart = ((Number) block.get("startSec")).intValue();
                int bEnd = ((Number) block.get("endSec")).intValue();
                int pdfHint = ((Number) block.get("pdfPageHint")).intValue();
                org.junit.Assert.assertEquals("Block boundary discontinuity at " + blockId, prevBlockEnd, bStart);
                org.junit.Assert.assertTrue("Invalid block duration at " + blockId, bEnd > bStart);
                org.junit.Assert.assertTrue("Invalid pdfPageHint at " + blockId, pdfHint >= 1 && pdfHint <= bookTotalPages);
                blockBounds.put(blockId, new int[]{bStart, bEnd});

                java.util.List<Object> segments = (java.util.List<Object>) block.get("segments");
                org.junit.Assert.assertTrue(
                        "Block " + blockId + " must have >= 10 dense segments (found " + segments.size() + ")",
                        segments.size() >= 10
                );

                int prevSegEnd = bStart;
                for (Object segObj : segments) {
                    java.util.Map<String, Object> seg = (java.util.Map<String, Object>) segObj;
                    int sStart = ((Number) seg.get("startSec")).intValue();
                    int sEnd = ((Number) seg.get("endSec")).intValue();
                    String arabic = (String) seg.get("arabic");
                    String indonesian = (String) seg.get("indonesian");
                    org.junit.Assert.assertEquals("Segment gap/overlap in " + blockId, prevSegEnd, sStart);
                    org.junit.Assert.assertTrue("Non-positive segment duration in " + blockId, sEnd > sStart);
                    org.junit.Assert.assertTrue("Segment > 35s in " + blockId + ": " + (sEnd - sStart), (sEnd - sStart) <= 35);
                    org.junit.Assert.assertTrue("Empty Arabic in " + blockId, arabic != null && !arabic.trim().isEmpty());
                    org.junit.Assert.assertTrue("Empty Indonesian in " + blockId, indonesian != null && !indonesian.trim().isEmpty());
                    prevSegEnd = sEnd;
                    epSegCount++;
                }
                org.junit.Assert.assertEquals("Block end != last segment end in " + blockId, bEnd, prevSegEnd);
                prevBlockEnd = bEnd;
            }
            org.junit.Assert.assertEquals("Episode duration mismatch for ep " + epNum, durationSec, prevBlockEnd);
            double epAvg = (double) durationSec / epSegCount;
            org.junit.Assert.assertTrue("Episode " + epNum + " avg seg duration too high: " + epAvg, epAvg <= 32.0);
            totalCourseSegments += epSegCount;
            totalCourseSec += durationSec;
        }

        double overallAvg = (double) totalCourseSec / totalCourseSegments;
        org.junit.Assert.assertTrue("Overall avg seg duration too high: " + overallAvg, overallAvg <= 32.0);

        java.util.List<Object> glossary = (java.util.List<Object>) course.get("glossary");
        org.junit.Assert.assertEquals(16, glossary.size());
        for (Object gObj : glossary) {
            java.util.Map<String, Object> g = (java.util.Map<String, Object>) gObj;
            String gid = (String) g.get("id");
            int epNum = ((Number) g.get("episodeNumber")).intValue();
            String blkId = (String) g.get("blockId");
            int sec = ((Number) g.get("exampleTimeSec")).intValue();
            java.util.Map<String, int[]> bMap = blockBoundsByEpisode.get(epNum);
            org.junit.Assert.assertNotNull("Glossary ep missing: " + gid, bMap);
            int[] bounds = bMap.get(blkId);
            org.junit.Assert.assertNotNull("Glossary block missing: " + gid + " -> " + blkId, bounds);
            org.junit.Assert.assertTrue(
                    "Glossary time out of bounds for " + gid + ": " + sec + " not in [" + bounds[0] + "," + bounds[1] + "]",
                    sec >= bounds[0] && sec < bounds[1]
            );
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void arabicContentAssetsAndCatalogAreSyncedAndValid() throws Exception {
        java.io.File catalogFile = new java.io.File("lessons/catalog.json");
        if (!catalogFile.exists()) {
            catalogFile = new java.io.File("../lessons/catalog.json");
        }
        org.junit.Assert.assertTrue("Missing lessons/catalog.json", catalogFile.exists());
        java.io.File repoRoot = catalogFile.getParentFile().getParentFile();
        java.io.File repoContentDir = new java.io.File(repoRoot, "lessons/arabic_content");
        java.io.File assetContentDir = new java.io.File(repoRoot, "app/src/main/assets/arabic_lessons/content");
        org.junit.Assert.assertTrue("Missing repo arabic_content dir", repoContentDir.isDirectory());
        org.junit.Assert.assertTrue("Missing asset arabic_lessons/content dir", assetContentDir.isDirectory());

        String catText = new String(
                java.nio.file.Files.readAllBytes(catalogFile.toPath()),
                java.nio.charset.StandardCharsets.UTF_8
        );
        java.util.Map<String, Object> cat = (java.util.Map<String, Object>) new MinimalJsonParser(catText).parseValue();
        java.util.List<Object> lessons = (java.util.List<Object>) cat.get("lessons");
        org.junit.Assert.assertNotNull(lessons);
        org.junit.Assert.assertTrue(lessons.size() >= 11);

        java.io.File[] repoFiles = repoContentDir.listFiles((dir, name) -> name.endsWith(".json"));
        org.junit.Assert.assertNotNull(repoFiles);
        org.junit.Assert.assertTrue("Expected at least 11 TAFL JSONs", repoFiles.length >= 11);

        java.util.Set<String> validatedNames = new java.util.HashSet<>();
        for (java.io.File repoJson : repoFiles) {
            String jsonName = repoJson.getName();
            java.io.File assetJson = new java.io.File(assetContentDir, jsonName);
            org.junit.Assert.assertTrue("Missing asset JSON: " + jsonName, assetJson.exists());

            byte[] repoBytes = java.nio.file.Files.readAllBytes(repoJson.toPath());
            byte[] assetBytes = java.nio.file.Files.readAllBytes(assetJson.toPath());
            org.junit.Assert.assertArrayEquals("Byte parity mismatch for " + jsonName, repoBytes, assetBytes);

            String raw = new String(assetBytes, java.nio.charset.StandardCharsets.UTF_8);
            com.jadwalshalat.gt5.arabic.ArabicLessonContent parsed =
                    com.jadwalshalat.gt5.arabic.ArabicContentParser.INSTANCE.parseContentJson(raw);
            org.junit.Assert.assertNotNull("Failed parsing " + jsonName, parsed);
            org.junit.Assert.assertTrue("No transcript segments in " + jsonName, parsed.getTranscriptSegments().size() >= 3);
            org.junit.Assert.assertTrue("No vocab items in " + jsonName, parsed.getTotalGlossaryItems() >= 10);
            validatedNames.add(jsonName);
        }

        int matchedInCatalog = 0;
        for (Object lObj : lessons) {
            java.util.Map<String, Object> l = (java.util.Map<String, Object>) lObj;
            String pdf = (String) l.get("pdfFileName");
            org.junit.Assert.assertNotNull(pdf);
            String jsonName = com.jadwalshalat.gt5.arabic.ArabicContentParser.INSTANCE.contentFileNameForPdf(pdf);
            if (validatedNames.contains(jsonName)) matchedInCatalog++;
        }
        org.junit.Assert.assertTrue("Expected at least 11 catalog lessons with interactive JSON", matchedInCatalog >= 11);
    }

    @Test
    public void arabicTextUtilsKamusSrsAndClozeWorkCleanly() {
        com.jadwalshalat.gt5.arabic.ArabicTextUtils utils = com.jadwalshalat.gt5.arabic.ArabicTextUtils.INSTANCE;
        org.junit.Assert.assertEquals("الحضارة", utils.stripHarakat("اَلْحَضَارَةُ"));

        String sentence = "اَلْحَضَارَةُ الْعَرَبِيَّةُ الْإِسْلَامِيَّةُ لَيْسَتْ حَضَارَةً عَابِرَةً.";
        java.util.List<com.jadwalshalat.gt5.arabic.ArabicWordToken> tokens = utils.tokenizeSentenceWords(sentence);
        org.junit.Assert.assertTrue(tokens.size() >= 6);
        String phrase = utils.buildPhraseFromRange(tokens, 4, 5);
        org.junit.Assert.assertEquals("حَضَارَةً عَابِرَةً", phrase);

        com.jadwalshalat.gt5.arabic.KamusEntry entry1 = new com.jadwalshalat.gt5.arabic.KamusEntry(
                com.jadwalshalat.gt5.arabic.KamusCodec.INSTANCE.createStableId("حَضَارَةً عَابِرَةً", "finjan"),
                "حَضَارَةً عَابِرَةً",
                "ḥaḍāratan ‘ābiratan",
                "Peradaban yang hanya numpang lewat",
                "Dari ‘abara (عَبَرَ): menyeberangi sungai / lewat",
                "Ḥaḍārah (peradaban maju) • Madaniyyah (sisi material)",
                com.jadwalshalat.gt5.arabic.KamusEntryKind.PHRASE,
                "finjan",
                "Podcast Finjan: Peradaban",
                "00:49 – 01:25",
                49,
                sentence,
                "",
                0,
                0L,
                1000L,
                0
        );
        com.jadwalshalat.gt5.arabic.KamusEntry entry2 = new com.jadwalshalat.gt5.arabic.KamusEntry(
                "km_2",
                "تَدَهْوُرٌ",
                "tadahwur",
                "Kemerosotan",
                "", "", com.jadwalshalat.gt5.arabic.KamusEntryKind.WORD,
                "finjan", "Finjan", "", 49, "", "", 0, 0L, 1000L, 0
        );

        long today = 20100L;
        com.jadwalshalat.gt5.arabic.KamusSrsScheduler srs = com.jadwalshalat.gt5.arabic.KamusSrsScheduler.INSTANCE;
        org.junit.Assert.assertTrue(srs.isDue(entry1, today));
        com.jadwalshalat.gt5.arabic.KamusEntry st1 = srs.advance(entry1, true, today);
        org.junit.Assert.assertEquals(1, st1.getMasteryStage());
        org.junit.Assert.assertEquals(today + 1L, st1.getNextReviewEpochDay());
        org.junit.Assert.assertFalse(srs.isDue(st1, today));
        com.jadwalshalat.gt5.arabic.KamusEntry stReset = srs.advance(st1, false, today + 1L);
        org.junit.Assert.assertEquals(0, stReset.getMasteryStage());
        org.junit.Assert.assertEquals(today + 1L, stReset.getNextReviewEpochDay());

        com.jadwalshalat.gt5.arabic.ClozeQuestion q = utils.buildClozeQuestion(
                entry1,
                java.util.Arrays.asList(entry1, entry2),
                java.util.Arrays.asList("سَرْدِيَّةٌ", "نَهْضَةٌ"),
                true,
                1
        );
        org.junit.Assert.assertNotNull(q);
        org.junit.Assert.assertTrue(q.getMaskedSentence().contains("________"));
        org.junit.Assert.assertFalse(q.getMaskedSentence().contains("عَابِرَةً"));
        org.junit.Assert.assertEquals(4, q.getOptions().size());
        org.junit.Assert.assertTrue(q.getOptions().contains("حَضَارَةً عَابِرَةً"));

        // Backup JSON & CSV Anki roundtrip
        String backup = com.jadwalshalat.gt5.arabic.KamusCodec.INSTANCE.toBackupJson(
                java.util.Arrays.asList(entry1, entry2)
        );
        java.util.List<com.jadwalshalat.gt5.arabic.KamusEntry> restored =
                com.jadwalshalat.gt5.arabic.KamusCodec.INSTANCE.fromBackupJson(backup);
        org.junit.Assert.assertEquals(2, restored.size());
        org.junit.Assert.assertEquals("حَضَارَةً عَابِرَةً", restored.get(0).getArabic());
        org.junit.Assert.assertEquals(49, restored.get(0).getSegmentStartSec());

        String csv = com.jadwalshalat.gt5.arabic.KamusCodec.INSTANCE.toAnkiCsv(
                java.util.Arrays.asList(entry1, entry2)
        );
        org.junit.Assert.assertTrue(csv.contains("حَضَارَةً عَابِرَةً"));
        org.junit.Assert.assertTrue(csv.contains("Peradaban yang hanya numpang lewat"));
    }

    private static final class MinimalJsonParser {
        private final String src;
        private int pos = 0;

        MinimalJsonParser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWs();
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { pos += 4; return null; }
            return parseNumber();
        }

        private java.util.Map<String, Object> parseObject() {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            pos++; // '{'
            skipWs();
            if (src.charAt(pos) == '}') { pos++; return map; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                pos++; // ':'
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                char c = src.charAt(pos++);
                if (c == '}') break;
            }
            return map;
        }

        private java.util.List<Object> parseArray() {
            java.util.List<Object> list = new java.util.ArrayList<>();
            pos++; // '['
            skipWs();
            if (src.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = src.charAt(pos++);
                if (c == ']') break;
            }
            return list;
        }

        private String parseString() {
            pos++; // '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = src.charAt(pos++);
                    if (e == '"' || e == '\\' || e == '/') sb.append(e);
                    else if (e == 'b') sb.append('\b');
                    else if (e == 'f') sb.append('\f');
                    else if (e == 'n') sb.append('\n');
                    else if (e == 'r') sb.append('\r');
                    else if (e == 't') sb.append('\t');
                    else if (e == 'u') {
                        int code = Integer.parseInt(src.substring(pos, pos + 4), 16);
                        sb.append((char) code);
                        pos += 4;
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseBool() {
            if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            pos += 5; return Boolean.FALSE;
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            boolean isFloat = false;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c >= '0' && c <= '9') pos++;
                else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') { isFloat = true; pos++; }
                else break;
            }
            String s = src.substring(start, pos);
            return isFloat ? Double.valueOf(s) : Long.valueOf(s);
        }

        private void skipWs() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++;
                else break;
            }
        }
    }
}

