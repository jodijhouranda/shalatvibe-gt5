package com.jadwalshalat.gt5.cloud

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.jadwalshalat.gt5.R
import com.jadwalshalat.gt5.arabic.KamusCodec
import com.jadwalshalat.gt5.arabic.KamusEntry
import com.jadwalshalat.gt5.arabic.KamusSrsScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repositori Firestore real-time untuk sinkronisasi Kamus Saya & progres belajar
 * antara aplikasi Android Nida dan Web Nida Belajar.
 *
 * Selalu menggunakan named database ID dari `R.string.firestore_database_id`.
 */
class NidaCloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum menyinkronkan ke cloud.")
    }

    private fun sanitizeDocId(rawId: String, arabic: String, lessonId: String): String {
        val candidate = rawId.trim()
        if (candidate.isNotEmpty() && candidate.length <= 128 && VALID_ID_REGEX.matches(candidate)) {
            return candidate
        }
        return KamusCodec.createStableId(arabic, lessonId)
    }

    /**
     * Real-time stream seluruh entri Kamus Saya milik [userId] dari `/users/{userId}/kamus`.
     */
    fun observeKamusEntries(userId: String): Flow<List<KamusEntry>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }

        val path = "users/$userId/kamus"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("kamus")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        KamusEntryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.arabic.isNotBlank() }
                        .map { it.toKamusEntry() }
                        .sortedByDescending { it.createdAtMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream progres belajar milik [userId] dari `/users/{userId}/progress/learning_v1`.
     */
    fun observeLearningProgress(userId: String): Flow<LearningProgressDoc?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }

        val path = "users/$userId/progress/$PROGRESS_DOC_ID"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("progress")
                .document(PROGRESS_DOC_ID)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LearningProgressDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Membuat atau memperbarui 1 entri Kamus Saya di `/users/{userId}/kamus/{entryId}`.
     * Mempertahankan `createdAt` dan `createdAtMillis` yang immutable pada pembaruan.
     */
    suspend fun upsertKamusEntry(userId: String, entry: KamusEntry): Result<Unit> {
        val cleanArabic = entry.arabic.trim().take(500)
        if (cleanArabic.isEmpty()) {
            return Result.failure(IllegalArgumentException("Teks Arab tidak boleh kosong."))
        }
        val docId = sanitizeDocId(entry.id, cleanArabic, entry.lessonId)
        var docPath = "users/$userId/kamus/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }

            val docRef = db.collection("users")
                .document(uid)
                .collection("kamus")
                .document(docId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            val mutablePayload = buildMutableKamusFields(entry, cleanArabic)
            if (existingSnap.exists()) {
                docRef.update(mutablePayload).await()
            } else {
                val createdAtMillis = entry.createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
                val createPayload = LinkedHashMap<String, Any>(mutablePayload.size + 2).apply {
                    putAll(mutablePayload)
                    put("createdAtMillis", createdAtMillis)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Pembaruan parsial khusus jadwal SRS (masteryStage, nextReviewEpochDay, reviewCount, updatedAt).
     */
    suspend fun updateKamusSrs(userId: String, entry: KamusEntry): Result<Unit> {
        val cleanArabic = entry.arabic.trim().take(500)
        val docId = sanitizeDocId(entry.id, cleanArabic, entry.lessonId)
        var docPath = "users/$userId/kamus/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }

            val docRef = db.collection("users")
                .document(uid)
                .collection("kamus")
                .document(docId)
            docPath = docRef.path

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "masteryStage" to entry.masteryStage.coerceIn(0, KamusSrsScheduler.MASTERED_STAGE),
                        "nextReviewEpochDay" to entry.nextReviewEpochDay.coerceAtLeast(0L),
                        "reviewCount" to entry.reviewCount.coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(Unit)
            } else {
                upsertKamusEntry(userId, entry)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Menghapus 1 entri Kamus Saya dari cloud.
     */
    suspend fun deleteKamusEntry(userId: String, entryId: String): Result<Unit> {
        val trimmedId = entryId.trim()
        if (trimmedId.isEmpty() || trimmedId.length > 128 || !VALID_ID_REGEX.matches(trimmedId)) {
            return Result.success(Unit)
        }
        var docPath = "users/$userId/kamus/$trimmedId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }

            val docRef = db.collection("users")
                .document(uid)
                .collection("kamus")
                .document(trimmedId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Mengunggah entri Kamus lokal yang belum ada di cloud saat pengguna baru masuk Google,
     * sehingga kata yang sebelumnya disimpan offline di HP langsung tersedia juga di web.
     */
    suspend fun uploadMissingLocalKamusEntries(
        userId: String,
        localEntries: List<KamusEntry>
    ): Result<Int> {
        if (localEntries.isEmpty()) return Result.success(0)
        val colPath = "users/$userId/kamus"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user merge." }

            val snapshot = db.collection("users")
                .document(uid)
                .collection("kamus")
                .get()
                .await()

            val existingDocs = snapshot.toObjects(
                KamusEntryDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val existingIds = existingDocs.map { it.id }.toHashSet()
            val existingNormalizedKeys = existingDocs
                .map { it.toKamusEntry().normalizedKey }
                .filter { it.isNotBlank() }
                .toHashSet()

            var uploaded = 0
            for (local in localEntries) {
                val cleanArabic = local.arabic.trim().take(500)
                if (cleanArabic.isEmpty()) continue
                val docId = sanitizeDocId(local.id, cleanArabic, local.lessonId)
                if (existingIds.contains(docId) || existingNormalizedKeys.contains(local.normalizedKey)) {
                    continue
                }
                val result = upsertKamusEntry(uid, local.copy(id = docId))
                if (result.isSuccess) {
                    uploaded++
                    existingIds.add(docId)
                    if (local.normalizedKey.isNotBlank()) {
                        existingNormalizedKeys.add(local.normalizedKey)
                    }
                }
            }
            Result.success(uploaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    /**
     * Menyimpan progres belajar (modul selesai, favorit, blok kursus selesai) ke cloud.
     */
    suspend fun saveLearningProgress(
        userId: String,
        completedArabicIds: Set<String>,
        favoriteArabicIds: Set<String>,
        completedCourseBlockIds: Set<String>,
        lastOpenedLessonId: String?
    ): Result<Unit> {
        var docPath = "users/$userId/progress/$PROGRESS_DOC_ID"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }

            val docRef = db.collection("users")
                .document(uid)
                .collection("progress")
                .document(PROGRESS_DOC_ID)
            docPath = docRef.path

            val safeCompleted = completedArabicIds.filter { it.isNotBlank() }.take(500)
            val safeFavorites = favoriteArabicIds.filter { it.isNotBlank() }.take(500)
            val safeBlocks = completedCourseBlockIds.filter { it.isNotBlank() }.take(500)
            val safeLastOpened = lastOpenedLessonId?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

            val mutableMap = linkedMapOf<String, Any?>(
                "completedArabicIds" to safeCompleted,
                "favoriteArabicIds" to safeFavorites,
                "completedCourseBlockIds" to safeBlocks,
                "lastOpenedLessonId" to safeLastOpened,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            @Suppress("UNCHECKED_CAST")
            val nonNullMutable = mutableMap as Map<String, Any>

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(nonNullMutable).await()
            } else {
                val createPayload = LinkedHashMap<String, Any>(nonNullMutable.size + 1).apply {
                    putAll(nonNullMutable)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun buildMutableKamusFields(entry: KamusEntry, cleanArabic: String): Map<String, Any> {
        val rawMap = linkedMapOf<String, Any?>(
            "arabic" to cleanArabic,
            "transliteration" to entry.transliteration.trim().take(500),
            "meaning" to entry.meaning.trim().take(2000),
            "turatsOrigin" to entry.turatsOrigin.trim().take(1000),
            "synonymNote" to entry.synonymNote.trim().take(1500),
            "kind" to entry.kind.raw,
            "lessonId" to entry.lessonId.trim().take(200),
            "lessonTitle" to entry.lessonTitle.trim().take(300),
            "segmentLabel" to entry.segmentLabel.trim().take(200),
            "segmentStartSec" to entry.segmentStartSec.coerceIn(0, 86400),
            "contextSentence" to entry.contextSentence.trim().take(2000),
            "personalNote" to entry.personalNote.trim().take(2000),
            "masteryStage" to entry.masteryStage.coerceIn(0, KamusSrsScheduler.MASTERED_STAGE),
            "nextReviewEpochDay" to entry.nextReviewEpochDay.coerceAtLeast(0L),
            "reviewCount" to entry.reviewCount.coerceAtLeast(0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return rawMap.filterValues { it != null } as Map<String, Any>
    }

    companion object {
        const val PROGRESS_DOC_ID = "learning_v1"
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
