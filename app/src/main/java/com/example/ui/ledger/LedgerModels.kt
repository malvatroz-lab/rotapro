package com.example.ui.ledger

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CarRental
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.data.model.DailyEntry
import java.text.NumberFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

enum class TabKind {
    SUMMARY,
    REVENUE,
    EXPENSE,
    DISTANCE,
    PROFIT
}

enum class LedgerTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val kind: TabKind,
) {
    OVERVIEW(R.string.tab_overview, Icons.Rounded.Dashboard, TabKind.SUMMARY),
    UBER(R.string.tab_uber, Icons.Rounded.DirectionsCar, TabKind.REVENUE),
    POP99(R.string.tab_pop99, Icons.Rounded.LocalTaxi, TabKind.REVENUE),
    COMBUSTIVEL(R.string.tab_fuel, Icons.Rounded.LocalGasStation, TabKind.EXPENSE),
    RECARGA(R.string.tab_recharge, Icons.Rounded.Bolt, TabKind.EXPENSE),
    ALUGUEL(R.string.tab_rent, Icons.Rounded.CarRental, TabKind.EXPENSE),
    ALIMENTO(R.string.tab_food, Icons.Rounded.Restaurant, TabKind.EXPENSE),
    MANUTENCAO(R.string.tab_maintenance, Icons.Rounded.Build, TabKind.EXPENSE),
    KM(R.string.tab_km, Icons.Rounded.Route, TabKind.DISTANCE),
    LUCRO_DIA(R.string.tab_daily_profit, Icons.Rounded.Savings, TabKind.PROFIT);

    fun valueFrom(slot: DaySlot): Double = when (this) {
        OVERVIEW -> slot.lucroDoDia
        UBER -> slot.uber
        POP99 -> slot.pop99
        COMBUSTIVEL -> slot.combustivel
        RECARGA -> slot.recarga
        ALUGUEL -> slot.aluguel
        ALIMENTO -> slot.alimento
        MANUTENCAO -> slot.manutencao
        KM -> slot.kmRodado
        LUCRO_DIA -> slot.lucroDoDia
    }

    fun applyValue(entry: DailyEntry, newValue: Double): DailyEntry = when (this) {
        UBER -> entry.copy(uber = newValue)
        POP99 -> entry.copy(pop99 = newValue)
        COMBUSTIVEL -> entry.copy(combustivel = newValue)
        RECARGA -> entry.copy(recarga = newValue)
        ALUGUEL -> entry.copy(aluguel = newValue)
        ALIMENTO -> entry.copy(alimento = newValue)
        MANUTENCAO -> entry.copy(manutencao = newValue)
        KM -> entry.copy(kmRodado = newValue)
        OVERVIEW, LUCRO_DIA -> entry
    }
}

data class DaySlot(
    val year: Int,
    val month: Int,
    val day: Int,
    val weekdayShort: String,
    val monthShort: String,
    val isToday: Boolean,
    val entry: DailyEntry?,
) {
    val uber: Double get() = entry?.uber ?: 0.0
    val pop99: Double get() = entry?.pop99 ?: 0.0
    val combustivel: Double get() = entry?.combustivel ?: 0.0
    val recarga: Double get() = entry?.recarga ?: 0.0
    val aluguel: Double get() = entry?.aluguel ?: 0.0
    val alimento: Double get() = entry?.alimento ?: 0.0
    val manutencao: Double get() = entry?.manutencao ?: 0.0
    val kmRodado: Double get() = entry?.kmRodado ?: 0.0
    val notes: String get() = entry?.notes.orEmpty()

    val totalGanhos: Double get() = uber + pop99
    val totalDespesas: Double get() = combustivel + recarga + aluguel + alimento + manutencao
    val lucroDoDia: Double get() = totalGanhos - totalDespesas
    val hasData: Boolean get() = entry?.hasAnyValue == true
}

data class MonthlySummary(
    val totalUber: Double = 0.0,
    val totalPop99: Double = 0.0,
    val totalCombustivel: Double = 0.0,
    val totalRecarga: Double = 0.0,
    val totalAluguel: Double = 0.0,
    val totalAlimento: Double = 0.0,
    val totalManutencao: Double = 0.0,
    val totalKm: Double = 0.0,
    val activeDaysCount: Int = 0,
) {
    val totalFaturamentoBruto: Double get() = totalUber + totalPop99
    val totalDespesas: Double
        get() = totalCombustivel + totalRecarga + totalAluguel + totalAlimento + totalManutencao
    val lucroLiquido: Double get() = totalFaturamentoBruto - totalDespesas
    val lucroPorKm: Double get() = if (totalKm > 0.0) lucroLiquido / totalKm else 0.0

    fun totalForTab(tab: LedgerTab): Double = when (tab) {
        LedgerTab.OVERVIEW -> lucroLiquido
        LedgerTab.UBER -> totalUber
        LedgerTab.POP99 -> totalPop99
        LedgerTab.COMBUSTIVEL -> totalCombustivel
        LedgerTab.RECARGA -> totalRecarga
        LedgerTab.ALUGUEL -> totalAluguel
        LedgerTab.ALIMENTO -> totalAlimento
        LedgerTab.MANUTENCAO -> totalManutencao
        LedgerTab.KM -> totalKm
        LedgerTab.LUCRO_DIA -> lucroLiquido
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object LedgerFormatters {
    private val ptBrLocale = Locale("pt", "BR")

    private val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance(ptBrLocale)

    private val decimalFormat: NumberFormat
        get() = NumberFormat.getNumberInstance(ptBrLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }

    fun formatBrl(value: Double): String = currencyFormat.format(value)

    fun formatKm(value: Double): String = "${decimalFormat.format(value)} km"

    fun formatInputNumber(value: Double): String {
        if (value == 0.0) return ""
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun parseDecimalInput(raw: String): Double {
        val cleaned = raw.trim()
            .replace("R$", "")
            .replace(" ", "")
            .let { str ->
                if (str.contains(',') && str.contains('.')) {
                    str.replace(".", "").replace(',', '.')
                } else {
                    str.replace(',', '.')
                }
            }
        return cleaned.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}

object LedgerCalendar {
    private val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    private val monthShorts = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    )

    fun monthNamePtBr(month: Int): String = monthNames.getOrElse(month - 1) { "Mês" }

    fun monthShortPtBr(month: Int): String = monthShorts.getOrElse(month - 1) { "Mês" }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = GregorianCalendar(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun weekdayShortPtBr(year: Int, month: Int, day: Int): String {
        val cal = GregorianCalendar(year, month - 1, day)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Dom"
            Calendar.MONDAY -> "Seg"
            Calendar.TUESDAY -> "Ter"
            Calendar.WEDNESDAY -> "Qua"
            Calendar.THURSDAY -> "Qui"
            Calendar.FRIDAY -> "Sex"
            Calendar.SATURDAY -> "Sáb"
            else -> ""
        }
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun currentDay(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
