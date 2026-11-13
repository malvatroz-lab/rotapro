package com.example.ui.ledger

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.repository.DriverLedgerRepository
import com.example.ui.auth.signOut
import com.example.ui.theme.AmberBright
import com.example.ui.theme.CoralExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import com.example.ui.theme.SkyKm
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DriverLedgerScreen(
    currentUserId: String,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DriverLedgerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val prefs = app.getSharedPreferences("rotalucro_prefs", Context.MODE_PRIVATE)
                val savedFuelPrice = prefs.getFloat("fuel_price_per_liter", 0f).toDouble()
                DriverLedgerViewModel(
                    repository = DriverLedgerRepository(db),
                    currentUserId = currentUserId,
                    initialFuelPricePerLiter = savedFuelPrice,
                    onSaveFuelPrice = { newPrice ->
                        prefs.edit().putFloat("fuel_price_per_liter", newPrice.toFloat()).apply()
                    }
                )
            }
        }
    )

    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val fuelPricePerLiter by viewModel.fuelPricePerLiter.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var fullEditorSlot by remember { mutableStateOf<DaySlot?>(null) }
    var quickDialogSlot by remember { mutableStateOf<DaySlot?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showFuelPriceDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val (year, month) = selectedYearMonth
    val monthName = LedgerCalendar.monthNamePtBr(month)
    val tabs = LedgerTab.entries

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val currentSlots = (ledgerState as? UiState.Success)?.data?.first
            if (currentSlots != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val todayDay = viewModel.jumpToCurrentMonth()
                        val target = currentSlots.firstOrNull { it.day == todayDay }
                            ?: currentSlots.firstOrNull()
                        if (target != null) {
                            fullEditorSlot = target
                        }
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Lançar Hoje") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_log_today")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                // Top Bar + Month Switcher
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    val todayDay = viewModel.jumpToCurrentMonth()
                                    scope.launch {
                                        listState.animateScrollToItem((todayDay - 1).coerceAtLeast(0))
                                    }
                                },
                                modifier = Modifier.testTag("jump_today_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.jump_to_today))
                            }

                            IconButton(
                                onClick = { showSignOutDialog = true },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.sign_out),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Month Selector Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                modifier = Modifier.testTag("prev_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.prev_month_cd)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMonthPicker = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("month_year_picker_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$monthName $year",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier.testTag("next_month_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.next_month_cd)
                                )
                            }
                        }
                    }
                }

                if (isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                when (val state = ledgerState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.loading_ledger),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val (slots, summary) = state.data

                        // Always-visible Live Monthly Summary Card
                        MonthlySummaryHeader(
                            monthName = monthName,
                            year = year,
                            summary = summary,
                            fuelPricePerLiter = fuelPricePerLiter,
                            onEditFuelPrice = { showFuelPriceDialog = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        // Category Tabs Row
                        ScrollableTabRow(
                            selectedTabIndex = tabs.indexOf(selectedTab),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 16.dp,
                            divider = {},
                            modifier = Modifier.testTag("ledger_tabs_row")
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab == selectedTab
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Focused Tab Header Banner (for individual category tabs)
                        if (selectedTab != LedgerTab.OVERVIEW) {
                            TabTotalBanner(
                                tab = selectedTab,
                                monthName = monthName,
                                totalValue = summary.totalForTab(selectedTab),
                                summary = summary,
                                fuelPricePerLiter = fuelPricePerLiter,
                                onEditFuelPrice = { showFuelPriceDialog = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Daily Rows (1..daysInMonth with month name)
                        val maxProfitAbs = remember(slots) {
                            slots.maxOfOrNull { abs(it.lucroDoDia) }?.coerceAtLeast(1.0) ?: 1.0
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("days_lazy_column"),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 92.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(slots, key = { "${it.year}-${it.month}-${it.day}" }) { slot ->
                                when (selectedTab) {
                                    LedgerTab.OVERVIEW -> OverviewDayCard(
                                        slot = slot,
                                        fuelPricePerLiter = fuelPricePerLiter,
                                        onClick = { fullEditorSlot = slot }
                                    )
                                    LedgerTab.LUCRO_DIA -> DailyProfitRowCard(
                                        slot = slot,
                                        maxProfitAbs = maxProfitAbs,
                                        onClick = { fullEditorSlot = slot }
                                    )
                                    else -> SingleTabDayCard(
                                        slot = slot,
                                        tab = selectedTab,
                                        fuelPricePerLiter = fuelPricePerLiter,
                                        onClick = { quickDialogSlot = slot },
                                        onEditFullDay = { fullEditorSlot = slot }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fullEditorSlot?.let { slot ->
        DayEntryBottomSheet(
            slot = slot,
            fuelPricePerLiter = fuelPricePerLiter,
            onDismiss = { fullEditorSlot = null },
            onSave = { uber, pop99, combustivel, recarga, aluguel, alimento, manutencao, km, notes ->
                viewModel.saveFullDayEntry(
                    year = slot.year,
                    month = slot.month,
                    day = slot.day,
                    uber = uber,
                    pop99 = pop99,
                    combustivel = combustivel,
                    recarga = recarga,
                    aluguel = aluguel,
                    alimento = alimento,
                    manutencao = manutencao,
                    kmRodado = km,
                    notes = notes
                )
                fullEditorSlot = null
            },
            onClearDay = {
                viewModel.clearDayEntry(slot)
                fullEditorSlot = null
            }
        )
    }

    quickDialogSlot?.let { slot ->
        QuickTabValueDialog(
            slot = slot,
            tab = selectedTab,
            onDismiss = { quickDialogSlot = null },
            onSaveSingle = { amount, addToExisting ->
                viewModel.saveSingleTabValue(slot, selectedTab, amount, addToExisting)
                quickDialogSlot = null
            },
            onOpenFullEditor = {
                quickDialogSlot = null
                fullEditorSlot = slot
            }
        )
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialYear = year,
            initialMonth = month,
            onDismiss = { showMonthPicker = false },
            onSelect = { pickedYear, pickedMonth ->
                viewModel.selectMonthYear(pickedYear, pickedMonth)
                showMonthPicker = false
            }
        )
    }

    if (showFuelPriceDialog) {
        FuelPriceDialog(
            initialPrice = fuelPricePerLiter,
            onDismiss = { showFuelPriceDialog = false },
            onSave = { price ->
                viewModel.setFuelPricePerLiter(price)
                showFuelPriceDialog = false
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.sign_out_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        signOut(credentialManager, scope)
                    }
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TabTotalBanner(
    tab: LedgerTab,
    monthName: String,
    totalValue: Double,
    summary: MonthlySummary,
    fuelPricePerLiter: Double,
    onEditFuelPrice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (tab.kind) {
        TabKind.REVENUE -> EmeraldPrimary
        TabKind.EXPENSE -> CoralExpense
        TabKind.DISTANCE -> SkyKm
        TabKind.PROFIT -> if (totalValue >= 0.0) EmeraldPrimary else CoralExpense
        TabKind.SUMMARY -> MaterialTheme.colorScheme.primary
    }
    val badge = when (tab.kind) {
        TabKind.REVENUE -> stringResource(R.string.badge_revenue)
        TabKind.EXPENSE -> stringResource(R.string.badge_expense)
        TabKind.DISTANCE -> stringResource(R.string.badge_distance)
        TabKind.PROFIT -> stringResource(R.string.badge_profit)
        TabKind.SUMMARY -> ""
    }
    val isFuelOrKmTab = tab == LedgerTab.COMBUSTIVEL || tab == LedgerTab.KM
    val mediaKmL = summary.mediaKmPorLitro(fuelPricePerLiter)
    val litros = summary.litrosEstimados(fuelPricePerLiter)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${stringResource(tab.labelRes)} • $monthName",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (badge.isNotEmpty()) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
                Text(
                    text = if (tab == LedgerTab.KM) LedgerFormatters.formatKm(totalValue)
                    else LedgerFormatters.formatBrl(totalValue),
                    style = NumericMediumStyle,
                    color = accent,
                    modifier = Modifier.testTag("tab_total_value")
                )
            }

            if (isFuelOrKmTab && summary.totalKm > 0.0 && summary.totalCombustivel > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mediaKmL > 0.0) {
                            "Média: ${LedgerFormatters.formatKmPerLiter(mediaKmL)} • ${LedgerFormatters.formatLiters(litros)} • ${LedgerFormatters.formatBrl(summary.custoCombustivelPorKm)}/km"
                        } else {
                            "Comb./km: ${LedgerFormatters.formatBrl(summary.custoCombustivelPorKm)}/km"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberBright,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (fuelPricePerLiter > 0.0) "${LedgerFormatters.formatBrl(fuelPricePerLiter)}/L" else "Definir R$/L",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberBright,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEditFuelPrice)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayDateBadge(slot: DaySlot) {
    val bgColor = if (slot.isToday) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fgColor = if (slot.isToday) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = slot.day.toString().padStart(2, '0'),
            style = NumericMediumStyle,
            color = fgColor
        )
        Text(
            text = "${slot.monthShort} • ${slot.weekdayShort}",
            style = MaterialTheme.typography.labelSmall,
            color = if (slot.isToday) fgColor.copy(alpha = 0.9f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun OverviewDayCard(
    slot: DaySlot,
    fuelPricePerLiter: Double,
    onClick: () -> Unit,
) {
    val profitColor = when {
        !slot.hasData -> MaterialTheme.colorScheme.onSurfaceVariant
        slot.lucroDoDia >= 0.0 -> EmeraldPrimary
        else -> CoralExpense
    }
    val dayMediaKmL = slot.mediaKmPorLitro(fuelPricePerLiter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_card_${slot.day}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (slot.isToday) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DayDateBadge(slot)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!slot.hasData) {
                    Text(
                        text = "Toque para lançar dia ${slot.day} ${slot.monthShort}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ ${LedgerFormatters.formatBrl(slot.totalGanhos)}",
                            style = NumericSmallStyle,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "- ${LedgerFormatters.formatBrl(slot.totalDespesas)}",
                            style = NumericSmallStyle,
                            color = CoralExpense
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (slot.uber > 0.0) {
                            MiniTag("Uber ${LedgerFormatters.formatBrl(slot.uber)}")
                        }
                        if (slot.pop99 > 0.0) {
                            MiniTag("99 ${LedgerFormatters.formatBrl(slot.pop99)}")
                        }
                        if (slot.kmRodado > 0.0) {
                            MiniTag(LedgerFormatters.formatKm(slot.kmRodado), tint = SkyKm)
                        }
                        if (dayMediaKmL > 0.0) {
                            MiniTag(LedgerFormatters.formatKmPerLiter(dayMediaKmL), tint = AmberBright)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.daily_profit_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (slot.hasData) LedgerFormatters.formatBrl(slot.lucroDoDia) else "R$ 0,00",
                    style = NumericMediumStyle,
                    color = profitColor
                )
            }
        }
    }
}

@Composable
private fun SingleTabDayCard(
    slot: DaySlot,
    tab: LedgerTab,
    fuelPricePerLiter: Double,
    onClick: () -> Unit,
    onEditFullDay: () -> Unit,
) {
    val tabValue = tab.valueFrom(slot)
    val accent = when (tab.kind) {
        TabKind.REVENUE -> EmeraldPrimary
        TabKind.EXPENSE -> CoralExpense
        TabKind.DISTANCE -> SkyKm
        else -> MaterialTheme.colorScheme.primary
    }
    val displayValue = if (tab == LedgerTab.KM) {
        LedgerFormatters.formatKm(tabValue)
    } else {
        LedgerFormatters.formatBrl(tabValue)
    }
    val isFuelOrKmTab = tab == LedgerTab.COMBUSTIVEL || tab == LedgerTab.KM
    val dayMediaKmL = slot.mediaKmPorLitro(fuelPricePerLiter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_day_card_${slot.day}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (slot.isToday) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DayDateBadge(slot)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayValue,
                    style = NumericMediumStyle,
                    color = if (tabValue > 0.0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isFuelOrKmTab && slot.combustivel > 0.0 && slot.kmRodado > 0.0) {
                    Text(
                        text = if (dayMediaKmL > 0.0) {
                            "Média: ${LedgerFormatters.formatKmPerLiter(dayMediaKmL)} • ${LedgerFormatters.formatBrl(slot.custoCombustivelPorKm)}/km"
                        } else {
                            "Comb./km: ${LedgerFormatters.formatBrl(slot.custoCombustivelPorKm)}/km"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberBright
                    )
                } else if (slot.hasData) {
                    val profitColor = if (slot.lucroDoDia >= 0.0) EmeraldPrimary else CoralExpense
                    Text(
                        text = "Lucro do dia: ${LedgerFormatters.formatBrl(slot.lucroDoDia)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = profitColor
                    )
                }
            }

            IconButton(
                onClick = onEditFullDay,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Editar dia completo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyProfitRowCard(
    slot: DaySlot,
    maxProfitAbs: Double,
    onClick: () -> Unit,
) {
    val profit = slot.lucroDoDia
    val profitColor = when {
        !slot.hasData -> MaterialTheme.colorScheme.onSurfaceVariant
        profit >= 0.0 -> EmeraldPrimary
        else -> CoralExpense
    }
    val fraction = if (slot.hasData) (abs(profit) / maxProfitAbs).toFloat().coerceIn(0.06f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profit_day_card_${slot.day}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DayDateBadge(slot)

                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Bruto: ${LedgerFormatters.formatBrl(slot.totalGanhos)}",
                            style = NumericSmallStyle,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Gastos: ${LedgerFormatters.formatBrl(slot.totalDespesas)}",
                            style = NumericSmallStyle,
                            color = CoralExpense
                        )
                    }
                    Text(
                        text = "KM: ${LedgerFormatters.formatKm(slot.kmRodado)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyKm
                    )
                }

                Text(
                    text = LedgerFormatters.formatBrl(profit),
                    style = NumericMediumStyle,
                    color = profitColor
                )
            }

            if (slot.hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(profitColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTag(
    text: String,
    tint: Color = AmberBright,
) {
    Surface(
        color = tint.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}
