package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DashboardItem
import com.example.ui.components.SummaryCard
import com.example.ui.theme.ExpectedTeal
import com.example.ui.theme.InboundBlue
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PayableRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.FinancialViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToReceivables: () -> Unit,
    onNavigateToPayables: () -> Unit,
    onNavigateToLoansThisMonth: () -> Unit,
    onAddNewClick: () -> Unit,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalSalesAmount by viewModel.totalSalesAmount.collectAsStateWithLifecycle()
    val pendingSalesCount by viewModel.pendingSalesCount.collectAsStateWithLifecycle()
    val totalInterestToReceive by viewModel.totalInterestToReceive.collectAsStateWithLifecycle()
    val totalToPay by viewModel.totalToPay.collectAsStateWithLifecycle()
    val totalToReceive by viewModel.totalToReceive.collectAsStateWithLifecycle()

    val upcomingCollections by viewModel.upcomingCollections.collectAsStateWithLifecycle()
    val upcomingPayables by viewModel.upcomingPayables.collectAsStateWithLifecycle()
    val overdueItems by viewModel.overdueItems.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("dashboard_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Lançamento",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 76.dp, top = 4.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 2x2 Grid for Dashboard Cards (Organized Order)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: 1. Vendas a Receber | 2. Juros a Receber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = "Vendas a Receber",
                            value = totalSalesAmount,
                            subtitle = when (pendingSalesCount) {
                                0 -> "Nenhuma venda pendente"
                                1 -> "1 venda pendente"
                                else -> "$pendingSalesCount vendas pendentes"
                            },
                            icon = Icons.Default.Storefront,
                            accentColor = InboundBlue,
                            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                            testTag = "card_vendas_a_receber",
                            modifier = Modifier.weight(1f),
                            isPrivate = isPrivacyMode,
                            onClick = onNavigateToSales
                        )

                        SummaryCard(
                            title = "Juros a Receber",
                            value = totalInterestToReceive,
                            subtitle = "Somente mês vigente",
                            icon = Icons.Default.Percent,
                            accentColor = PrimaryBlue,
                            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                            testTag = "card_juros_a_receber",
                            modifier = Modifier.weight(1f),
                            isPrivate = isPrivacyMode,
                            onClick = onNavigateToLoansThisMonth
                        )
                    }

                    // Row 2: 3. Total a Pagar | 4. Total a Receber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = "Total a Pagar",
                            value = totalToPay,
                            subtitle = "Dívidas pendentes",
                            icon = Icons.Default.MoneyOff,
                            accentColor = PayableRose,
                            gradientColors = listOf(Color(0xFF2A1920), Color(0xFF170D12)),
                            testTag = "card_total_a_pagar",
                            modifier = Modifier.weight(1f),
                            isPrivate = isPrivacyMode,
                            onClick = onNavigateToPayables
                        )

                        SummaryCard(
                            title = "Total a Receber",
                            value = totalToReceive,
                            subtitle = "Empréstimos + Recebíveis",
                            icon = Icons.Default.CalendarMonth,
                            accentColor = ExpectedTeal,
                            gradientColors = listOf(Color(0xFF132A27), Color(0xFF0A1816)),
                            testTag = "card_total_a_receber",
                            modifier = Modifier.weight(1f),
                            isPrivate = isPrivacyMode,
                            onClick = onNavigateToReceivables
                        )
                    }
                }
            }

            // 1. Section: Próximas cobranças
            item {
                DashboardSectionHeader(
                    title = "Próximas Cobranças",
                    subtitle = "Recebimentos e empréstimos",
                    icon = Icons.Default.Schedule,
                    iconTint = InboundBlue
                )
            }

            if (upcomingCollections.isEmpty()) {
                item {
                    EmptySectionCard("Nenhuma cobrança próxima cadastrada")
                }
            } else {
                items(upcomingCollections.take(4)) { item ->
                    DashboardListCard(
                        title = item.name,
                        subtitle = "Categoria: ${item.categoryOrType}",
                        amount = item.amount,
                        badgeText = item.categoryOrType,
                        dueDate = item.dueDate,
                        isOverdue = false,
                        onClick = onNavigateToReceivables
                    )
                }
            }

            // 2. Section: Próximos vencimentos
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DashboardSectionHeader(
                    title = "Próximos Vencimentos",
                    subtitle = "Dívidas e compromissos",
                    icon = Icons.Default.EventBusy,
                    iconTint = PayableRose
                )
            }

            if (upcomingPayables.isEmpty()) {
                item {
                    EmptySectionCard("Nenhum vencimento próximo cadastrado")
                }
            } else {
                items(upcomingPayables.take(4)) { payable ->
                    DashboardListCard(
                        title = payable.creditorName,
                        subtitle = payable.description.ifBlank { "Sem descrição" },
                        amount = payable.remainingBalance,
                        badgeText = payable.category,
                        dueDate = payable.dueDate,
                        isOverdue = false,
                        onClick = onNavigateToPayables
                    )
                }
            }

            // 3. Section: Contas vencidas
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DashboardSectionHeader(
                    title = "Contas Vencidas",
                    subtitle = "Lançamentos em atraso",
                    icon = Icons.Default.Warning,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }

            if (overdueItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PaidGreen.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PaidGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nenhuma conta vencida! Tudo em dia.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = PaidGreen
                            )
                        }
                    }
                }
            } else {
                items(overdueItems) { item ->
                    DashboardListCard(
                        title = item.name,
                        subtitle = "Atrasado • Vencimento: ${DateUtils.formatDate(item.dueDate)}",
                        amount = item.amount,
                        badgeText = "VENCIDO",
                        dueDate = item.dueDate,
                        isOverdue = true,
                        onClick = when (item) {
                            is DashboardItem.Payable -> onNavigateToPayables
                            else -> onNavigateToReceivables
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun DashboardListCard(
    title: String,
    subtitle: String,
    amount: Double,
    badgeText: String,
    dueDate: Long?,
    isOverdue: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isOverdue) MaterialTheme.colorScheme.onError
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOverdue) subtitle else "Vencimento: ${DateUtils.formatDate(dueDate)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
