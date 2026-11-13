package com.example.ui.ledger

import com.example.data.model.DailyEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerCalculationsTest {

    @Test
    fun dailyEntry_calculatesEarningsExpensesAndProfitAutomatically() {
        val entry = DailyEntry(
            year = 2026L,
            month = 11L,
            day = 10L,
            yearMonth = "2026-11",
            uber = 220.50,
            pop99 = 99.50,
            combustivel = 75.00,
            recarga = 15.00,
            aluguel = 60.00,
            alimento = 28.00,
            manutencao = 22.00,
            kmRodado = 190.0
        )

        assertEquals(320.00, entry.totalGanhos, 0.001)
        assertEquals(200.00, entry.totalDespesas, 0.001)
        assertEquals(120.00, entry.lucroDoDia, 0.001)
    }

    @Test
    fun monthlySummary_aggregatesGrossRevenueExpensesNetProfitAndKmAutomatically() {
        val day1 = DaySlot(
            year = 2026,
            month = 11,
            day = 1,
            weekdayShort = "Dom",
            monthShort = "Nov",
            isToday = false,
            entry = DailyEntry(
                year = 2026L,
                month = 11L,
                day = 1L,
                yearMonth = "2026-11",
                uber = 200.0,
                pop99 = 100.0,
                combustivel = 60.0,
                recarga = 10.0,
                aluguel = 50.0,
                alimento = 20.0,
                manutencao = 0.0,
                kmRodado = 150.0
            )
        )
        val day2 = DaySlot(
            year = 2026,
            month = 11,
            day = 2,
            weekdayShort = "Seg",
            monthShort = "Nov",
            isToday = false,
            entry = DailyEntry(
                year = 2026L,
                month = 11L,
                day = 2L,
                yearMonth = "2026-11",
                uber = 150.0,
                pop99 = 50.0,
                combustivel = 40.0,
                recarga = 0.0,
                aluguel = 50.0,
                alimento = 15.0,
                manutencao = 155.0,
                kmRodado = 100.0
            )
        )

        val summary = DriverLedgerViewModel.computeMonthlySummary(listOf(day1, day2))

        assertEquals(500.0, summary.totalFaturamentoBruto, 0.001)
        assertEquals(400.0, summary.totalDespesas, 0.001)
        assertEquals(100.0, summary.lucroLiquido, 0.001)
        assertEquals(250.0, summary.totalKm, 0.001)
        assertEquals(0.40, summary.lucroPorKm, 0.001)
        assertEquals(2, summary.activeDaysCount)
    }

    @Test
    fun decimalParser_handlesPortugueseAndStandardInputs() {
        assertEquals(125.50, LedgerFormatters.parseDecimalInput("125,50"), 0.001)
        assertEquals(1250.75, LedgerFormatters.parseDecimalInput("1.250,75"), 0.001)
        assertEquals(89.9, LedgerFormatters.parseDecimalInput("89.9"), 0.001)
        assertEquals(0.0, LedgerFormatters.parseDecimalInput("-50"), 0.001)
    }

    @Test
    fun monthlyAndDailyFuelEfficiency_calculatesCostPerKmLitersAndKmPerLiter() {
        val summary = MonthlySummary(
            totalCombustivel = 300.0,
            totalKm = 600.0
        )

        // Cost per km works even without fuel price per liter
        assertEquals(0.50, summary.custoCombustivelPorKm, 0.001)
        assertEquals(0.0, summary.litrosEstimados(0.0), 0.001)
        assertEquals(0.0, summary.mediaKmPorLitro(0.0), 0.001)

        // With R$ 6,00/L: R$ 300 / R$ 6,00 = 50 L -> 600 km / 50 L = 12,0 km/L
        assertEquals(50.0, summary.litrosEstimados(6.0), 0.001)
        assertEquals(12.0, summary.mediaKmPorLitro(6.0), 0.001)

        val slot = DaySlot(
            year = 2026,
            month = 11,
            day = 5,
            weekdayShort = "Qui",
            monthShort = "Nov",
            isToday = false,
            entry = DailyEntry(
                year = 2026L,
                month = 11L,
                day = 5L,
                yearMonth = "2026-11",
                combustivel = 120.0,
                kmRodado = 200.0
            )
        )

        // R$ 120 / R$ 6,00 = 20 L -> 200 km / 20 L = 10,0 km/L
        assertEquals(0.60, slot.custoCombustivelPorKm, 0.001)
        assertEquals(20.0, slot.litrosEstimados(6.0), 0.001)
        assertEquals(10.0, slot.mediaKmPorLitro(6.0), 0.001)
        assertEquals("10 km/L", LedgerFormatters.formatKmPerLiter(10.0))
        assertEquals("20 L", LedgerFormatters.formatLiters(20.0))
    }
}
