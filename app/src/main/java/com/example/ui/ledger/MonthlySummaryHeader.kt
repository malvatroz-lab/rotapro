package com.example.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberBright
import com.example.ui.theme.CoralExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NumericHeroStyle
import com.example.ui.theme.NumericMediumStyle
import com.example.ui.theme.NumericSmallStyle
import com.example.ui.theme.SkyKm

@Composable
fun MonthlySummaryHeader(
    monthName: String,
    year: Int,
    summary: MonthlySummary,
    fuelPricePerLiter: Double = 0.0,
    onEditFuelPrice: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showBreakdown by rememberSaveable { mutableStateOf(true) }
    val profitColor = if (summary.lucroLiquido >= 0.0) EmeraldPrimary else CoralExpense

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Profit Row + Collapse Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showBreakdown = !showBreakdown }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = profitColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.summary_net_profit)} • $monthName $year",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = LedgerFormatters.formatBrl(summary.lucroLiquido),
                        style = NumericHeroStyle,
                        color = profitColor,
                        modifier = Modifier.testTag("monthly_net_profit_text")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (summary.totalKm > 0.0) {
                        Surface(
                            color = profitColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${LedgerFormatters.formatBrl(summary.lucroPorKm)}/km",
                                style = NumericSmallStyle,
                                color = profitColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${summary.activeDaysCount} dias",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showBreakdown) {
                                Icons.Rounded.KeyboardArrowUp
                            } else {
                                Icons.Rounded.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showBreakdown) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricMiniTile(
                            label = stringResource(R.string.summary_gross_revenue),
                            value = LedgerFormatters.formatBrl(summary.totalFaturamentoBruto),
                            accent = EmeraldPrimary,
                            icon = Icons.Rounded.ArrowUpward,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("monthly_gross_revenue_tile")
                        )
                        MetricMiniTile(
                            label = stringResource(R.string.summary_total_expenses),
                            value = LedgerFormatters.formatBrl(summary.totalDespesas),
                            accent = CoralExpense,
                            icon = Icons.Rounded.ArrowDownward,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("monthly_expenses_tile")
                        )
                        MetricMiniTile(
                            label = stringResource(R.string.summary_total_km),
                            value = LedgerFormatters.formatKm(summary.totalKm),
                            accent = SkyKm,
                            icon = Icons.Rounded.Route,
                            modifier = Modifier
                                .weight(0.88f)
                                .testTag("monthly_km_tile")
                        )
                    }

                    FuelEfficiencyStrip(
                        summary = summary,
                        fuelPricePerLiter = fuelPricePerLiter,
                        onEditFuelPrice = onEditFuelPrice
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelEfficiencyStrip(
    summary: MonthlySummary,
    fuelPricePerLiter: Double,
    onEditFuelPrice: () -> Unit,
) {
    val hasFuelAndKm = summary.totalKm > 0.0 && summary.totalCombustivel > 0.0
    val mediaKmL = summary.mediaKmPorLitro(fuelPricePerLiter)
    val litros = summary.litrosEstimados(fuelPricePerLiter)
    val custoPorKm = summary.custoCombustivelPorKm

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fuel_efficiency_strip"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberBright.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalGasStation,
                        contentDescription = null,
                        tint = AmberBright,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasFuelAndKm && fuelPricePerLiter > 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${stringResource(R.string.fuel_avg_label)}: ${LedgerFormatters.formatKmPerLiter(mediaKmL)}",
                                style = NumericSmallStyle,
                                color = AmberBright,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("monthly_km_per_liter_text")
                            )
                            Text(
                                text = "• ${LedgerFormatters.formatLiters(litros)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${stringResource(R.string.fuel_cost_per_km_label)}: ${LedgerFormatters.formatBrl(custoPorKm)}/km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (hasFuelAndKm) {
                        Text(
                            text = "${stringResource(R.string.fuel_cost_per_km_label)}: ${LedgerFormatters.formatBrl(custoPorKm)}/km",
                            style = NumericSmallStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.fuel_price_missing_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.fuel_efficiency_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (fuelPricePerLiter > 0.0) {
                                "Lance KM e Combustível para ver km/L"
                            } else {
                                "Calcule km/L com KM + Combustível"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                onClick = onEditFuelPrice,
                shape = RoundedCornerShape(10.dp),
                color = AmberBright.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, AmberBright.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("fuel_price_button")
            ) {
                Text(
                    text = if (fuelPricePerLiter > 0.0) {
                        "${LedgerFormatters.formatBrl(fuelPricePerLiter)}/L"
                    } else {
                        stringResource(R.string.fuel_price_set_cta)
                    },
                    style = NumericSmallStyle,
                    color = AmberBright,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniTile(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = NumericMediumStyle.copy(fontSize = 14.spCompat()),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Int.spCompat() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(),
    androidx.compose.ui.unit.TextUnitType.Sp
)
