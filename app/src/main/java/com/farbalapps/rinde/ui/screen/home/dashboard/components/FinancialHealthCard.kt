package com.farbalapps.rinde.ui.screen.home.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.ui.screen.home.dashboard.DashboardUiState
import com.farbalapps.rinde.ui.screen.home.dashboard.FinancialHealthStatus
import java.util.Locale

import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Composable
fun FinancialHealthCard(
    uiState: DashboardUiState,
    onEditIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onDeleteExpense: (String) -> Unit = {},
    onEditExpense: (com.farbalapps.rinde.domain.model.ExtraExpense) -> Unit = {},
    onAddExtraIncome: () -> Unit = {},
    onEditExtraIncome: (ExtraIncome) -> Unit = {},
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.expenseFraction,
        animationSpec = tween(durationMillis = 600),
        label = "health_expense_progress"
    )

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.dashboard_financial_health),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                HealthStatusBadge(status = uiState.healthStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monto disponible real (centrado)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val availableFormatted = String.format(Locale.getDefault(), "$%,.0f", uiState.availableAmount)
                Text(
                    text = availableFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (uiState.availableAmount < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${uiState.currency} · ${stringResource(id = R.string.dashboard_available_real)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Barra de progreso (Gastos vs Libre)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Etiquetas de porcentaje: Gastos % vs Libre %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.dashboard_expenses_percent, uiState.expensePercentage),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_free_percent, uiState.freePercentage),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Desglose de rubros: Ingresos
            BreakdownRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(id = R.string.dashboard_income_title),
                amount = uiState.monthlyIncome,
                isPositive = true,
                actionsContent = {
                    IconButton(
                        onClick = onEditIncome,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.dashboard_btn_edit_income),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onAddExtraIncome,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.dashboard_add_extra_income),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )

            // Aviso si no tiene sueldo base configurado
            if (!uiState.hasIncomeConfigured) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditIncome)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.dashboard_no_salary_banner),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Aviso si requiere capturar ingreso del mes en modo variable
            if (uiState.needsMonthlyIncomeCapture) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                        .clickable(onClick = onEditIncome)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.dashboard_income_variable_prompt, uiState.currentMonthName),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Chips compactos horizontales de ingresos extras
            if (uiState.extraIncomes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                ) {
                    items(uiState.extraIncomes, key = { it.id }) { income ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            onClick = { onEditExtraIncome(income) }
                        ) {
                            Text(
                                text = "+ ${income.label}: $${String.format(Locale.getDefault(), "%,.0f", income.amount)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose: Mi Lista
            BreakdownRow(
                icon = Icons.Default.ShoppingCart,
                title = stringResource(id = R.string.dashboard_my_list_title),
                amount = uiState.listTotal,
                isPositive = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose: Gastos Extra del Hogar
            BreakdownRow(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(id = R.string.dashboard_extra_expenses_title),
                amount = uiState.extraExpensesTotal,
                isPositive = false,
                onActionClick = onAddExpense,
                actionIcon = Icons.Default.Add,
                actionDescription = stringResource(id = R.string.dashboard_add_extra_expense)
            )

            // Chips compactos horizontales de gastos extra
            if (uiState.extraExpenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                ) {
                    items(uiState.extraExpenses, key = { it.id }) { expense ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            onClick = { onEditExpense(expense) }
                        ) {
                            Text(
                                text = "${expense.label}: $${String.format(Locale.getDefault(), "%,.0f", expense.amount)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose: Metas
            BreakdownRow(
                icon = Icons.Default.Flag,
                title = stringResource(id = R.string.dashboard_goals_committed_title),
                amount = uiState.goalsCommittedTotal,
                isPositive = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón/indicador para ver el detalle de periodos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .clickable(onClick = onCardClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.financial_health_view_period_detail),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthStatusBadge(
    status: FinancialHealthStatus,
    modifier: Modifier = Modifier
) {
    val (color, textRes) = when (status) {
        FinancialHealthStatus.GOOD -> Pair(Color(0xFF66BB6A), R.string.dashboard_health_good)
        FinancialHealthStatus.WARNING -> Pair(Color(0xFFFFA726), R.string.dashboard_health_warning)
        FinancialHealthStatus.CRITICAL -> Pair(Color(0xFFEF5350), R.string.dashboard_health_critical)
        FinancialHealthStatus.SETUP_REQUIRED -> Pair(MaterialTheme.colorScheme.primary, R.string.dashboard_health_setup_required)
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, shape = CircleShape)
        )
        Text(
            text = stringResource(id = textRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun BreakdownRow(
    icon: ImageVector,
    title: String,
    amount: Double,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    actionsContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (actionsContent != null) {
                actionsContent()
            } else if (onActionClick != null && actionIcon != null) {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        val prefix = if (isPositive) "" else "-"
        val formatted = String.format(Locale.getDefault(), "%s$%,.0f", prefix, amount)
        Text(
            text = formatted,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
