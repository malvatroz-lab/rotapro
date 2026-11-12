package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@IgnoreExtraProperties
data class DailyEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val year: Long = 2026L,
    val month: Long = 1L,
    val day: Long = 1L,
    val yearMonth: String = "",
    val uber: Double = 0.0,
    val pop99: Double = 0.0,
    val combustivel: Double = 0.0,
    val recarga: Double = 0.0,
    val aluguel: Double = 0.0,
    val alimento: Double = 0.0,
    val manutencao: Double = 0.0,
    val kmRodado: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    @get:Exclude
    val totalGanhos: Double
        get() = uber + pop99

    @get:Exclude
    val totalDespesas: Double
        get() = combustivel + recarga + aluguel + alimento + manutencao

    @get:Exclude
    val lucroDoDia: Double
        get() = totalGanhos - totalDespesas

    @get:Exclude
    val hasAnyValue: Boolean
        get() = uber > 0.0 ||
            pop99 > 0.0 ||
            combustivel > 0.0 ||
            recarga > 0.0 ||
            aluguel > 0.0 ||
            alimento > 0.0 ||
            manutencao > 0.0 ||
            kmRodado > 0.0 ||
            notes.isNotBlank()

    companion object {
        fun documentId(year: Int, month: Int, day: Int): String =
            String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

        fun yearMonthKey(year: Int, month: Int): String =
            String.format(Locale.US, "%04d-%02d", year, month)
    }
}
