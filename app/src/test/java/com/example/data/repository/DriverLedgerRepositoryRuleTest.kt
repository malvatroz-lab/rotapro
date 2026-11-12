package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DailyEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class DriverLedgerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndObserveMonthEntries_authenticatedOwner_succeedsAndComputesTotals() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = DriverLedgerRepository(firestore)
        val entry = DailyEntry(
            userId = uid,
            year = 2026L,
            month = 11L,
            day = 12L,
            yearMonth = "2026-11",
            uber = 240.0,
            pop99 = 110.0,
            combustivel = 80.0,
            recarga = 15.0,
            aluguel = 60.0,
            alimento = 25.0,
            manutencao = 20.0,
            kmRodado = 185.5,
            notes = "Turno manhã e tarde"
        )

        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDailyEntry(uid, entry)
        }
        assertEquals("2026-11-12", docId)

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDailyEntry(uid, docId)
        }
        assertNotNull(loaded)
        assertEquals(350.0, loaded!!.totalGanhos, 0.01)
        assertEquals(200.0, loaded.totalDespesas, 0.01)
        assertEquals(150.0, loaded.lucroDoDia, 0.01)

        // Update existing entry
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDailyEntry(uid, entry.copy(uber = 300.0, kmRodado = 210.0))
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMonthEntries(uid, "2026-11")
                .first { list -> list.any { it.id == docId && it.uber == 300.0 } }
        }
        val updatedEntry = observed.first { it.id == docId }
        assertEquals(410.0, updatedEntry.totalGanhos, 0.01)
        assertEquals(210.0, updatedEntry.lucroDoDia, 0.01)
        assertEquals(210.0, updatedEntry.kmRodado, 0.01)

        // Delete entry
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteDailyEntry(uid, docId)
        }
        val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDailyEntry(uid, docId)
        }
        assertNull(afterDelete)
    }

    @Test
    fun getDailyEntry_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DriverLedgerRepository(firestore)
        val entry = DailyEntry(
            userId = aliceUid,
            year = 2026L,
            month = 11L,
            day = 5L,
            yearMonth = "2026-11",
            uber = 180.0
        )
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertDailyEntry(aliceUid, entry)
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DriverLedgerRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getDailyEntry(aliceUid, docId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeMonthEntries_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = DriverLedgerRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMonthEntries("some_uid", "2026-11").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_driver@test.com"
        const val BOB_EMAIL = "bob_driver@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
