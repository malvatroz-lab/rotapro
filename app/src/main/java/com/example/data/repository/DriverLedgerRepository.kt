package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.DailyEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DriverLedgerRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userEntriesCollection(userId: String) =
        db.collection("users").document(userId).collection("daily_entries")

    fun observeMonthEntries(userId: String, yearMonth: String): Flow<List<DailyEntry>> {
        val collectionRef = userEntriesCollection(userId)
        val path = "users/$userId/daily_entries"
        return collectionRef
            .whereEqualTo("yearMonth", yearMonth)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DailyEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                val currentUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
                if (exceptionToLog != null && currentUid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getDailyEntry(userId: String, entryId: String): DailyEntry? {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(entryId.isNotBlank()) { "Entry ID must not be blank" }
        val docRef = userEntriesCollection(userId).document(entryId)
        return try {
            docRef.get().await().toObject(
                DailyEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun upsertDailyEntry(userId: String, entry: DailyEntry): String {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val yearInt = entry.year.toInt()
        val monthInt = entry.month.toInt()
        val dayInt = entry.day.toInt()
        val docId = DailyEntry.documentId(yearInt, monthInt, dayInt)
        val yearMonth = DailyEntry.yearMonthKey(yearInt, monthInt)
        val docRef = userEntriesCollection(userId).document(docId)

        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to userId,
                    "year" to entry.year,
                    "month" to entry.month,
                    "day" to entry.day,
                    "yearMonth" to yearMonth,
                    "uber" to entry.uber.coerceAtLeast(0.0),
                    "pop99" to entry.pop99.coerceAtLeast(0.0),
                    "combustivel" to entry.combustivel.coerceAtLeast(0.0),
                    "recarga" to entry.recarga.coerceAtLeast(0.0),
                    "aluguel" to entry.aluguel.coerceAtLeast(0.0),
                    "alimento" to entry.alimento.coerceAtLeast(0.0),
                    "manutencao" to entry.manutencao.coerceAtLeast(0.0),
                    "kmRodado" to entry.kmRodado.coerceAtLeast(0.0),
                    "notes" to entry.notes.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "uber" to entry.uber.coerceAtLeast(0.0),
                    "pop99" to entry.pop99.coerceAtLeast(0.0),
                    "combustivel" to entry.combustivel.coerceAtLeast(0.0),
                    "recarga" to entry.recarga.coerceAtLeast(0.0),
                    "aluguel" to entry.aluguel.coerceAtLeast(0.0),
                    "alimento" to entry.alimento.coerceAtLeast(0.0),
                    "manutencao" to entry.manutencao.coerceAtLeast(0.0),
                    "kmRodado" to entry.kmRodado.coerceAtLeast(0.0),
                    "notes" to entry.notes.take(200),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updates).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDailyEntry(userId: String, entryId: String) {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        require(entryId.isNotBlank()) { "Entry ID must not be blank" }
        val docRef = userEntriesCollection(userId).document(entryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
