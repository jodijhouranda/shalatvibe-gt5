package com.jadwalshalat.gt5.arabic

import android.content.Context
import java.io.File

/**
 * Repositori offline untuk "Kamus Saya" milik pengguna.
 * Menyimpan seluruh koleksi kata/frase/idiom ke penyimpanan internal aplikasi (`arabic_kamus_v1.json`)
 * secara atomik sehingga aman dari corrupt dan tidak terbatasi kuota SharedPreferences.
 */
class KamusRepository(context: Context) {

    private val storageFile = File(context.applicationContext.filesDir, KAMUS_FILE_NAME)
    private val tempFile = File(context.applicationContext.filesDir, "$KAMUS_FILE_NAME.tmp")
    private val lock = Any()

    fun loadEntries(): List<KamusEntry> = synchronized(lock) {
        if (!storageFile.exists()) return emptyList()
        val raw = runCatching { storageFile.readText(Charsets.UTF_8) }.getOrNull() ?: return emptyList()
        return KamusCodec.fromBackupJson(raw)
            .sortedByDescending { it.createdAtMillis }
    }

    fun upsertEntry(entry: KamusEntry): List<KamusEntry> = synchronized(lock) {
        val current = loadEntries().toMutableList()
        val key = entry.normalizedKey
        val existingIdx = current.indexOfFirst {
            it.id == entry.id || (key.isNotBlank() && it.normalizedKey == key)
        }
        val cleanEntry = if (existingIdx >= 0) {
            val prev = current[existingIdx]
            entry.copy(
                id = prev.id,
                transliteration = entry.transliteration.ifBlank { prev.transliteration },
                meaning = entry.meaning.ifBlank { prev.meaning },
                turatsOrigin = entry.turatsOrigin.ifBlank { prev.turatsOrigin },
                synonymNote = entry.synonymNote.ifBlank { prev.synonymNote },
                lessonId = entry.lessonId.ifBlank { prev.lessonId },
                lessonTitle = entry.lessonTitle.ifBlank { prev.lessonTitle },
                segmentLabel = entry.segmentLabel.ifBlank { prev.segmentLabel },
                segmentStartSec = if (entry.segmentStartSec > 0) entry.segmentStartSec else prev.segmentStartSec,
                contextSentence = entry.contextSentence.ifBlank { prev.contextSentence },
                personalNote = entry.personalNote.ifBlank { prev.personalNote },
                masteryStage = prev.masteryStage,
                nextReviewEpochDay = prev.nextReviewEpochDay,
                createdAtMillis = if (prev.createdAtMillis > 0L) prev.createdAtMillis else entry.createdAtMillis,
                reviewCount = prev.reviewCount
            )
        } else {
            val now = if (entry.createdAtMillis > 0L) entry.createdAtMillis else System.currentTimeMillis()
            val stableId = entry.id.ifBlank { KamusCodec.createStableId(entry.arabic, entry.lessonId) }
            entry.copy(
                id = stableId,
                createdAtMillis = now
            )
        }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
        }
        current.add(0, cleanEntry)
        writeEntriesLocked(current)
        return current
    }

    fun deleteEntry(entryId: String): List<KamusEntry> = synchronized(lock) {
        val updated = loadEntries().filterNot { it.id == entryId }
        writeEntriesLocked(updated)
        return updated
    }

    fun gradeSrs(
        entryId: String,
        remembered: Boolean,
        todayEpochDay: Long = KamusSrsScheduler.todayEpochDay()
    ): List<KamusEntry> = synchronized(lock) {
        val current = loadEntries().map { entry ->
            if (entry.id == entryId) {
                KamusSrsScheduler.advance(entry, remembered, todayEpochDay)
            } else {
                entry
            }
        }
        writeEntriesLocked(current)
        return current
    }

    fun toggleMastered(
        entryId: String,
        todayEpochDay: Long = KamusSrsScheduler.todayEpochDay()
    ): List<KamusEntry> = synchronized(lock) {
        val current = loadEntries().map { entry ->
            if (entry.id == entryId) {
                if (entry.isMastered) {
                    entry.copy(masteryStage = 0, nextReviewEpochDay = todayEpochDay)
                } else {
                    entry.copy(
                        masteryStage = KamusSrsScheduler.MASTERED_STAGE,
                        nextReviewEpochDay = todayEpochDay + KamusSrsScheduler.STAGE_INTERVALS_DAYS[KamusSrsScheduler.MASTERED_STAGE]
                    )
                }
            } else {
                entry
            }
        }
        writeEntriesLocked(current)
        return current
    }

    fun importBackupJson(rawJson: String): Pair<List<KamusEntry>, Int> = synchronized(lock) {
        val imported = KamusCodec.fromBackupJson(rawJson)
        if (imported.isEmpty()) return loadEntries() to 0
        val current = loadEntries().toMutableList()
        var addedOrUpdated = 0
        for (incoming in imported) {
            val incKey = incoming.normalizedKey
            val existingIdx = current.indexOfFirst {
                it.id == incoming.id || (incKey.isNotBlank() && it.normalizedKey == incKey)
            }
            if (existingIdx < 0) {
                current.add(
                    incoming.copy(
                        createdAtMillis = if (incoming.createdAtMillis > 0L) incoming.createdAtMillis else System.currentTimeMillis()
                    )
                )
                addedOrUpdated++
            }
        }
        val sorted = current.sortedByDescending { it.createdAtMillis }
        writeEntriesLocked(sorted)
        return sorted to addedOrUpdated
    }

    private fun writeEntriesLocked(entries: List<KamusEntry>) {
        val json = KamusCodec.toBackupJson(entries)
        runCatching {
            tempFile.parentFile?.mkdirs()
            tempFile.writeText(json, Charsets.UTF_8)
            if (storageFile.exists()) storageFile.delete()
            if (!tempFile.renameTo(storageFile)) {
                storageFile.writeText(json, Charsets.UTF_8)
                tempFile.delete()
            }
        }.onFailure {
            runCatching { storageFile.writeText(json, Charsets.UTF_8) }
        }
    }

    companion object {
        private const val KAMUS_FILE_NAME = "arabic_kamus_v1.json"
    }
}
