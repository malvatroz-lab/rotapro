package com.example.ui.ledger

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyEntry
import com.example.data.repository.DriverLedgerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DriverLedgerViewModel(
    private val repository: DriverLedgerRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val initialYearMonth = LedgerCalendar.currentYearMonth()

    private val _selectedYearMonth = MutableStateFlow(initialYearMonth)
    val selectedYearMonth: StateFlow<Pair<Int, Int>> = _selectedYearMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(LedgerTab.OVERVIEW)
    val selectedTab: StateFlow<LedgerTab> = _selectedTab.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val entriesFlow: StateFlow<UiState<List<DailyEntry>>> = _selectedYearMonth
        .flatMapLatest { (year, month) ->
            val key = DailyEntry.yearMonthKey(year, month)
            repository.observeMonthEntries(currentUserId, key)
                .map<List<DailyEntry>, UiState<List<DailyEntry>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing entries for $key", rootError)
                    emit(
                        UiState.Error(
                            rootError.localizedMessage
                                ?: "Não foi possível carregar os lançamentos do mês."
                        )
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val ledgerState: StateFlow<UiState<Pair<List<DaySlot>, MonthlySummary>>> = combine(
        _selectedYearMonth,
        entriesFlow
    ) { (year, month), entriesState ->
        when (entriesState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(entriesState.message)
            is UiState.Success -> {
                val entriesByDay = entriesState.data.associateBy { it.day.toInt() }
                val totalDays = LedgerCalendar.daysInMonth(year, month)
                val (todayYear, todayMonth, todayDay) = LedgerCalendar.currentDay()
                val monthShort = LedgerCalendar.monthShortPtBr(month)

                val slots = (1..totalDays).map { day ->
                    DaySlot(
                        year = year,
                        month = month,
                        day = day,
                        weekdayShort = LedgerCalendar.weekdayShortPtBr(year, month, day),
                        monthShort = monthShort,
                        isToday = (year == todayYear && month == todayMonth && day == todayDay),
                        entry = entriesByDay[day]
                    )
                }

                val summary = computeMonthlySummary(slots)
                UiState.Success(slots to summary)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun selectTab(tab: LedgerTab) {
        _selectedTab.value = tab
    }

    fun previousMonth() {
        val (year, month) = _selectedYearMonth.value
        _selectedYearMonth.value = if (month == 1) (year - 1) to 12 else year to (month - 1)
    }

    fun nextMonth() {
        val (year, month) = _selectedYearMonth.value
        _selectedYearMonth.value = if (month == 12) (year + 1) to 1 else year to (month + 1)
    }

    fun selectMonthYear(year: Int, month: Int) {
        _selectedYearMonth.value = year.coerceIn(2020, 2100) to month.coerceIn(1, 12)
    }

    fun jumpToCurrentMonth(): Int {
        val (year, month, day) = LedgerCalendar.currentDay()
        _selectedYearMonth.value = year to month
        return day
    }

    fun saveFullDayEntry(
        year: Int,
        month: Int,
        day: Int,
        uber: Double,
        pop99: Double,
        combustivel: Double,
        recarga: Double,
        aluguel: Double,
        alimento: Double,
        manutencao: Double,
        kmRodado: Double,
        notes: String,
    ) {
        val entry = DailyEntry(
            id = DailyEntry.documentId(year, month, day),
            userId = currentUserId,
            year = year.toLong(),
            month = month.toLong(),
            day = day.toLong(),
            yearMonth = DailyEntry.yearMonthKey(year, month),
            uber = uber.coerceAtLeast(0.0),
            pop99 = pop99.coerceAtLeast(0.0),
            combustivel = combustivel.coerceAtLeast(0.0),
            recarga = recarga.coerceAtLeast(0.0),
            aluguel = aluguel.coerceAtLeast(0.0),
            alimento = alimento.coerceAtLeast(0.0),
            manutencao = manutencao.coerceAtLeast(0.0),
            kmRodado = kmRodado.coerceAtLeast(0.0),
            notes = notes.trim().take(200)
        )
        persistEntry(entry)
    }

    fun saveSingleTabValue(
        slot: DaySlot,
        tab: LedgerTab,
        enteredAmount: Double,
        addToExisting: Boolean,
    ) {
        val baseEntry = slot.entry ?: DailyEntry(
            id = DailyEntry.documentId(slot.year, slot.month, slot.day),
            userId = currentUserId,
            year = slot.year.toLong(),
            month = slot.month.toLong(),
            day = slot.day.toLong(),
            yearMonth = DailyEntry.yearMonthKey(slot.year, slot.month)
        )
        val currentVal = tab.valueFrom(slot)
        val finalValue = if (addToExisting) {
            (currentVal + enteredAmount).coerceAtLeast(0.0)
        } else {
            enteredAmount.coerceAtLeast(0.0)
        }
        val updatedEntry = tab.applyValue(baseEntry, finalValue)
        persistEntry(updatedEntry)
    }

    fun clearDayEntry(slot: DaySlot) {
        val docId = DailyEntry.documentId(slot.year, slot.month, slot.day)
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.deleteDailyEntry(currentUserId, docId)
                _feedbackMessage.value = "Dia ${slot.day} limpo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _feedbackMessage.value = e.localizedMessage ?: "Erro ao limpar dia."
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private fun persistEntry(entry: DailyEntry) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (!entry.hasAnyValue) {
                    val docId = DailyEntry.documentId(
                        entry.year.toInt(),
                        entry.month.toInt(),
                        entry.day.toInt()
                    )
                    repository.deleteDailyEntry(currentUserId, docId)
                } else {
                    repository.upsertDailyEntry(currentUserId, entry)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _feedbackMessage.value = e.localizedMessage ?: "Erro ao salvar lançamento."
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        private const val TAG = "DriverLedgerVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun computeMonthlySummary(slots: List<DaySlot>): MonthlySummary {
            var uber = 0.0
            var pop99 = 0.0
            var combustivel = 0.0
            var recarga = 0.0
            var aluguel = 0.0
            var alimento = 0.0
            var manutencao = 0.0
            var km = 0.0
            var activeDays = 0

            for (slot in slots) {
                if (slot.hasData) activeDays++
                uber += slot.uber
                pop99 += slot.pop99
                combustivel += slot.combustivel
                recarga += slot.recarga
                aluguel += slot.aluguel
                alimento += slot.alimento
                manutencao += slot.manutencao
                km += slot.kmRodado
            }

            return MonthlySummary(
                totalUber = uber,
                totalPop99 = pop99,
                totalCombustivel = combustivel,
                totalRecarga = recarga,
                totalAluguel = aluguel,
                totalAlimento = alimento,
                totalManutencao = manutencao,
                totalKm = km,
                activeDaysCount = activeDays
            )
        }
    }
}
