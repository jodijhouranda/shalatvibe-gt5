package com.jadwalshalat.gt5.learning

import android.content.Context
import com.jadwalshalat.gt5.arabic.ArabicCatalogParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class CourseSyncResult(
    val success: Boolean,
    val totalTracks: Int,
    val updatedCourses: Int,
    val message: String
)

class CourseRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val courseJsonCacheDir = File(appContext.filesDir, "course_json_cache").apply { mkdirs() }
    private val coursePdfCacheDir = File(appContext.filesDir, "course_pdfs").apply { mkdirs() }
    private val syncedIndexFile = File(appContext.filesDir, "courses_index_synced.json")

    fun loadTracks(): List<LearningTrackEntry> {
        val bundled = runCatching {
            val json = appContext.assets.open(ASSET_COURSES_INDEX).bufferedReader().use { it.readText() }
            CourseCatalogParser.parseTracksIndexJson(json)
        }.getOrDefault(emptyList())

        val synced = if (syncedIndexFile.exists()) {
            runCatching {
                CourseCatalogParser.parseTracksIndexJson(syncedIndexFile.readText())
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        if (synced.isEmpty()) return bundled

        val merged = LinkedHashMap<String, LearningTrackEntry>()
        for (entry in synced) merged[entry.id] = entry
        for (entry in bundled) {
            if (!merged.containsKey(entry.id)) merged[entry.id] = entry
        }
        return merged.values.toList()
    }

    fun loadSeriesCourse(entry: LearningTrackEntry): SeriesCourse? {
        val cachedFile = File(courseJsonCacheDir, "${entry.id}.json")
        if (cachedFile.exists()) {
            val parsed = runCatching {
                CourseCatalogParser.parseSeriesCourseJson(cachedFile.readText())
            }.getOrNull()
            if (parsed != null) return parsed
        }
        val assetPath = entry.assetPath.ifBlank { "courses/${entry.id}.json" }
        return runCatching {
            val json = appContext.assets.open(assetPath).bufferedReader().use { it.readText() }
            CourseCatalogParser.parseSeriesCourseJson(json)
        }.getOrNull()
    }

    fun loadAllSeriesCourses(tracks: List<LearningTrackEntry>): Map<String, SeriesCourse> {
        val map = LinkedHashMap<String, SeriesCourse>()
        for (track in tracks) {
            if (track.type == LearningTrackType.SERIES_COURSE) {
                loadSeriesCourse(track)?.let { map[track.id] = it }
            }
        }
        return map
    }

    suspend fun resolveCoursePdf(course: SeriesCourse): File? = withContext(Dispatchers.IO) {
        if (course.bookPdfFileName.isBlank()) return@withContext null

        val cached = File(coursePdfCacheDir, course.bookPdfFileName)
        if (cached.exists() && cached.length() > 1024L) {
            return@withContext cached
        }

        val copiedFromAssets = runCatching {
            appContext.assets.open("courses/${course.bookPdfFileName}").use { input ->
                FileOutputStream(cached).use { output -> input.copyTo(output) }
            }
            cached.exists() && cached.length() > 1024L
        }.getOrDefault(false)

        if (copiedFromAssets) return@withContext cached

        val rawUrl = course.bookPdfUrl.trim()
        if (rawUrl.isBlank()) return@withContext null

        val normalized = ArabicCatalogParser.normalizeDownloadUrl(rawUrl)
        val ok = downloadBinary(normalized, cached)
        if (ok && cached.length() > 1024L) cached else null
    }

    suspend fun syncCoursesFromRemote(indexUrlOverride: String? = null): CourseSyncResult =
        withContext(Dispatchers.IO) {
            val targetUrl = (indexUrlOverride ?: getCoursesIndexUrl()).trim()
            if (targetUrl.isBlank()) {
                return@withContext CourseSyncResult(false, loadTracks().size, 0, "URL index kursus kosong.")
            }
            val indexBody = fetchText(ArabicCatalogParser.normalizeDownloadUrl(targetUrl))
            if (indexBody == null) {
                return@withContext CourseSyncResult(false, loadTracks().size, 0, "Gagal mengunduh daftar kursus.")
            }

            val parsedTracks = runCatching { CourseCatalogParser.parseTracksIndexJson(indexBody) }
                .getOrNull()
                .orEmpty()
            if (parsedTracks.isEmpty()) {
                return@withContext CourseSyncResult(false, loadTracks().size, 0, "Format index kursus tidak valid.")
            }

            syncedIndexFile.writeText(indexBody)
            var updatedSeriesCount = 0
            for (track in parsedTracks) {
                if (track.type == LearningTrackType.SERIES_COURSE && track.manifestUrl.isNotBlank()) {
                    val body = fetchText(ArabicCatalogParser.normalizeDownloadUrl(track.manifestUrl))
                    val parsedCourse = body?.let {
                        runCatching { CourseCatalogParser.parseSeriesCourseJson(it) }.getOrNull()
                    }
                    if (body != null && parsedCourse != null) {
                        File(courseJsonCacheDir, "${track.id}.json").writeText(body)
                        updatedSeriesCount++
                    }
                }
            }
            CourseSyncResult(
                success = true,
                totalTracks = parsedTracks.size,
                updatedCourses = updatedSeriesCount,
                message = "Sinkronisasi pustaka berhasil ($updatedSeriesCount seri diperbarui)."
            )
        }

    fun getCompletedBlockIds(): Set<String> =
        prefs.getStringSet(KEY_COMPLETED_BLOCKS, emptySet())?.toSet() ?: emptySet()

    fun toggleBlockCompleted(blockId: String): Set<String> {
        val updated = getCompletedBlockIds().toMutableSet()
        if (!updated.add(blockId)) updated.remove(blockId)
        prefs.edit().putStringSet(KEY_COMPLETED_BLOCKS, updated).apply()
        return updated
    }

    fun markBlockCompleted(blockId: String): Set<String> {
        val updated = getCompletedBlockIds().toMutableSet()
        updated.add(blockId)
        prefs.edit().putStringSet(KEY_COMPLETED_BLOCKS, updated).apply()
        return updated
    }

    fun mergeCompletedBlocksFromCloud(cloudBlocks: Set<String>): Set<String> {
        val merged = (getCompletedBlockIds() + cloudBlocks).filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet(KEY_COMPLETED_BLOCKS, merged).apply()
        return merged
    }

    fun recordLastOpened(trackId: String, courseId: String? = null, blockId: String? = null) {
        prefs.edit()
            .putString(KEY_LAST_TRACK_ID, trackId)
            .putString(KEY_LAST_COURSE_ID, courseId)
            .putString(KEY_LAST_BLOCK_ID, blockId)
            .apply()
    }

    fun getLastTrackId(): String? = prefs.getString(KEY_LAST_TRACK_ID, null)
    fun getLastCourseId(): String? = prefs.getString(KEY_LAST_COURSE_ID, null)
    fun getLastBlockId(): String? = prefs.getString(KEY_LAST_BLOCK_ID, null)

    fun getCoursesIndexUrl(): String =
        prefs.getString(KEY_COURSES_INDEX_URL, DEFAULT_COURSES_INDEX_URL) ?: DEFAULT_COURSES_INDEX_URL

    private fun fetchText(urlString: String): String? = runCatching {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "JadwalGT5/2.3")
        }
        conn.useResponse {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        }
    }.getOrNull()

    private fun downloadBinary(urlString: String, target: File): Boolean = runCatching {
        val temp = File(target.parentFile, "${target.name}.tmp")
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "JadwalGT5/2.3")
        }
        conn.useResponse {
            if (conn.responseCode !in 200..299) return@useResponse false
            conn.inputStream.use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            }
            if (temp.exists() && temp.length() > 1024L) {
                if (target.exists()) target.delete()
                temp.renameTo(target)
            } else {
                temp.delete()
                false
            }
        }
    }.getOrDefault(false)

    private inline fun <T> HttpURLConnection.useResponse(block: () -> T): T {
        return try {
            block()
        } finally {
            disconnect()
        }
    }

    companion object {
        private const val PREF_NAME = "shalatvibe_learning_courses"
        private const val ASSET_COURSES_INDEX = "courses/courses.json"
        private const val KEY_COMPLETED_BLOCKS = "completed_block_ids"
        private const val KEY_LAST_TRACK_ID = "last_track_id"
        private const val KEY_LAST_COURSE_ID = "last_course_id"
        private const val KEY_LAST_BLOCK_ID = "last_block_id"
        private const val KEY_COURSES_INDEX_URL = "courses_index_url"
        const val DEFAULT_COURSES_INDEX_URL =
            "https://raw.githubusercontent.com/jodijhouranda/shalatvibe-gt5/main/lessons/courses.json"
    }
}
