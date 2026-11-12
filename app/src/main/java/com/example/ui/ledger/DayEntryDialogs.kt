package com.example.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import com.example.ui.theme.SkyKm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEntryBottomSheet(
    slot: DaySlot,
    onDismiss: () -> Unit,
    onSave: (
        uber: Double,
        pop99: Double,
        combustivel: Double,
        recarga: Double,
        aluguel: Double,
        alimento: Double,
        manutencao: Double,
        kmRodado: Double,
        notes: String,
    ) -> Unit,
    onClearDay: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var uberText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.uber)) }
    var pop99Text by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.pop99)) }
    var combustivelText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.combustivel)) }
    var recargaText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.recarga)) }
    var aluguelText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.aluguel)) }
    var alimentoText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.alimento)) }
    var manutencaoText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.manutencao)) }
    var kmText by remember(slot) { mutableStateOf(LedgerFormatters.formatInputNumber(slot.kmRodado)) }
    var notesText by remember(slot) { mutableStateOf(slot.notes) }

    val uberVal = LedgerFormatters.parseDecimalInput(uberText)
    val pop99Val = LedgerFormatters.parseDecimalInput(pop99Text)
    val combustivelVal = LedgerFormatters.parseDecimalInput(combustivelText)
    val recargaVal = LedgerFormatters.parseDecimalInput(recargaText)
    val aluguelVal = LedgerFormatters.parseDecimalInput(aluguelText)
    val alimentoVal = LedgerFormatters.parseDecimalInput(alimentoText)
    val manutencaoVal = LedgerFormatters.parseDecimalInput(manutencaoText)
    val kmVal = LedgerFormatters.parseDecimalInput(kmText)

    val liveGanhos = uberVal + pop99Val
    val liveDespesas = combustivelVal + recargaVal + aluguelVal + alimentoVal + manutencaoVal
    val liveLucro = liveGanhos - liveDespesas
    val profitColor = if (liveLucro >= 0.0) EmeraldPrimary else CoralExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dia ${slot.day} de ${LedgerCalendar.monthNamePtBr(slot.month)} ${slot.year}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = slot.weekdayShort + if (slot.isToday) " • Hoje" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (slot.hasData) {
                    IconButton(
                        onClick = onClearDay,
                        modifier = Modifier.testTag("clear_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_day),
                            tint = CoralExpense
                        )
                    }
                }
            }

            // Live Automatic Daily Profit Preview
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.daily_revenue_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = LedgerFormatters.formatBrl(liveGanhos),
                            style = NumericSmallStyle,
                            color = EmeraldPrimary
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.daily_expense_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = LedgerFormatters.formatBrl(liveDespesas),
                            style = NumericSmallStyle,
                            color = CoralExpense
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.daily_profit_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = LedgerFormatters.formatBrl(liveLucro),
                            style = NumericMediumStyle,
                            color = profitColor,
                            modifier = Modifier.testTag("live_day_profit_preview")
                        )
                    }
                }
            }

            // Ganhos (+ Adicionamento)
            SectionHeader(
                title = stringResource(R.string.section_earnings),
                color = EmeraldPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DecimalInputField(
                    label = stringResource(R.string.tab_uber),
                    prefix = "R$",
                    value = uberText,
                    onValueChange = { uberText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_uber")
                )
                DecimalInputField(
                    label = stringResource(R.string.tab_pop99),
                    prefix = "R$",
                    value = pop99Text,
                    onValueChange = { pop99Text = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_pop99")
                )
            }

            // Gastos (- Despesas)
            SectionHeader(
                title = stringResource(R.string.section_expenses),
                color = CoralExpense
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DecimalInputField(
                    label = stringResource(R.string.tab_fuel),
                    prefix = "R$",
                    value = combustivelText,
                    onValueChange = { combustivelText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_combustivel")
                )
                DecimalInputField(
                    label = stringResource(R.string.tab_recharge),
                    prefix = "R$",
                    value = recargaText,
                    onValueChange = { recargaText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_recarga")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DecimalInputField(
                    label = stringResource(R.string.tab_rent),
                    prefix = "R$",
                    value = aluguelText,
                    onValueChange = { aluguelText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_aluguel")
                )
                DecimalInputField(
                    label = stringResource(R.string.tab_food),
                    prefix = "R$",
                    value = alimentoText,
                    onValueChange = { alimentoText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_alimento")
                )
            }
            DecimalInputField(
                label = stringResource(R.string.tab_maintenance),
                prefix = "R$",
                value = manutencaoText,
                onValueChange = { manutencaoText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_manutencao")
            )

            // Quilometragem & Notas
            SectionHeader(
                title = stringResource(R.string.section_mileage),
                color = SkyKm
            )
            DecimalInputField(
                label = stringResource(R.string.tab_km),
                prefix = "km",
                value = kmText,
                onValueChange = { kmText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_km")
            )
            OutlinedTextField(
                value = notesText,
                onValueChange = { if (it.length <= 200) notesText = it },
                label = { Text(stringResource(R.string.field_notes_label)) },
                placeholder = { Text(stringResource(R.string.field_notes_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notes")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        onSave(
                            uberVal,
                            pop99Val,
                            combustivelVal,
                            recargaVal,
                            aluguelVal,
                            alimentoVal,
                            manutencaoVal,
                            kmVal,
                            notesText
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_day_entry_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTabValueDialog(
    slot: DaySlot,
    tab: LedgerTab,
    onDismiss: () -> Unit,
    onSaveSingle: (amount: Double, addToExisting: Boolean) -> Unit,
    onOpenFullEditor: () -> Unit,
) {
    val existingValue = tab.valueFrom(slot)
    var addToExisting by remember(slot, tab) { mutableStateOf(existingValue > 0.0) }
    var inputText by remember(slot, tab) {
        mutableStateOf(
            if (existingValue > 0.0) "" else ""
        )
    }

    val enteredVal = LedgerFormatters.parseDecimalInput(inputText)
    val finalFieldVal = if (addToExisting) existingValue + enteredVal else enteredVal
    val previewSlot = remember(slot, tab, finalFieldVal) {
        when (tab) {
            LedgerTab.UBER -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(uber = finalFieldVal))
            LedgerTab.POP99 -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(pop99 = finalFieldVal))
            LedgerTab.COMBUSTIVEL -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(combustivel = finalFieldVal))
            LedgerTab.RECARGA -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(recarga = finalFieldVal))
            LedgerTab.ALUGUEL -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(aluguel = finalFieldVal))
            LedgerTab.ALIMENTO -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(alimento = finalFieldVal))
            LedgerTab.MANUTENCAO -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(manutencao = finalFieldVal))
            LedgerTab.KM -> slot.copy(entry = (slot.entry ?: com.example.data.model.DailyEntry()).copy(kmRodado = finalFieldVal))
            else -> slot
        }
    }
    val previewProfit = previewSlot.lucroDoDia
    val profitColor = if (previewProfit >= 0.0) EmeraldPrimary else CoralExpense
    val isKm = tab == LedgerTab.KM

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${stringResource(tab.labelRes)} • Dia ${slot.day} ${slot.monthShort}",
                    style = MaterialTheme.typography.titleLarge
                )
                val badgeText = when (tab.kind) {
                    TabKind.REVENUE -> stringResource(R.string.badge_revenue)
                    TabKind.EXPENSE -> stringResource(R.string.badge_expense)
                    TabKind.DISTANCE -> stringResource(R.string.badge_distance)
                    else -> ""
                }
                if (badgeText.isNotEmpty()) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = when (tab.kind) {
                            TabKind.REVENUE -> EmeraldPrimary
                            TabKind.EXPENSE -> CoralExpense
                            else -> SkyKm
                        }
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingValue > 0.0) {
                    Text(
                        text = "Atual: ${if (isKm) LedgerFormatters.formatKm(existingValue) else LedgerFormatters.formatBrl(existingValue)}",
                        style = NumericSmallStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addToExisting,
                            onClick = { addToExisting = true },
                            label = { Text("Somar (+)") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = !addToExisting,
                            onClick = {
                                addToExisting = false
                                if (inputText.isEmpty()) {
                                    inputText = LedgerFormatters.formatInputNumber(existingValue)
                                }
                            },
                            label = { Text("Substituir") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                DecimalInputField(
                    label = if (addToExisting && existingValue > 0.0) "Valor a somar" else stringResource(tab.labelRes),
                    prefix = if (isKm) "km" else "R$",
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_tab_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Novo valor do dia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isKm) LedgerFormatters.formatKm(finalFieldVal) else LedgerFormatters.formatBrl(finalFieldVal),
                                style = NumericSmallStyle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.daily_profit_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = LedgerFormatters.formatBrl(previewProfit),
                                style = NumericMediumStyle,
                                color = profitColor
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onOpenFullEditor,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Abrir todas as abas do dia")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveSingle(enteredVal, addToExisting) },
                modifier = Modifier.testTag("quick_tab_save_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onSelect: (year: Int, month: Int) -> Unit,
) {
    var pickedYear by remember { mutableIntStateOf(initialYear) }
    var pickedMonth by remember { mutableIntStateOf(initialMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_month_year)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { pickedYear = (pickedYear - 1).coerceAtLeast(2020) }) {
                        Text("${pickedYear - 1}")
                    }
                    Text(
                        text = pickedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { pickedYear = (pickedYear + 1).coerceAtMost(2100) }) {
                        Text("${pickedYear + 1}")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    for (m in 1..12) {
                        val selected = m == pickedMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { pickedMonth = m }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LedgerCalendar.monthShortPtBr(m),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(pickedYear, pickedMonth) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DecimalInputField(
    label: String,
    prefix: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }
            onValueChange(filtered)
        },
        label = { Text(label) },
        prefix = { Text("$prefix ") },
        placeholder = { Text("0,00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    )
}
