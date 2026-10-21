package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import com.example.ui.components.AddEditLoanDialog
import com.example.ui.components.AddEditReceivableDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.GlobalSearchBar
import com.example.ui.components.HistoryDialog
import com.example.ui.components.LoanItemCard
import com.example.ui.components.PaymentDialog
import com.example.ui.components.ReceivableItemCard
import com.example.ui.components.RolloverDialog
import com.example.ui.theme.InboundBlue
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.FinancialViewModel
import com.example.util.CurrencyFormatter

@Composable
fun ReceivablesScreen(
    viewModel: FinancialViewModel,
    initialTab: Int = 0,
    onSubTabChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = Empréstimos, 1 = Vendas / Outros

    val loans by viewModel.filteredLoans.collectAsStateWithLifecycle()
    val receivables by viewModel.filteredReceivables.collectAsStateWithLifecycle()

    // Dialog states
    var showAddEditLoan by remember { mutableStateOf(false) }
    var loanToEdit by remember { mutableStateOf<LoanEntity?>(null) }

    var showAddEditReceivable by remember { mutableStateOf(false) }
    var receivableToEdit by remember { mutableStateOf<ReceivableEntity?>(null) }

    var itemToPay by remember { mutableStateOf<Any?>(null) } // LoanEntity or ReceivableEntity
    var itemToRollover by remember { mutableStateOf<LoanEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    var historyList by remember { mutableStateOf<List<PaymentHistoryEntity>?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        loanToEdit = null
                        showAddEditLoan = true
                    } else {
                        receivableToEdit = null
                        showAddEditReceivable = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("receivables_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().testTag("receivables_tab_row")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onSubTabChange(0)
                    },
                    text = {
                        Text(
                            text = "Empréstimos",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_loans")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onSubTabChange(1)
                    },
                    text = {
                        Text(
                            text = "Vendas a Receber",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_sales")
                )
            }

            // Summary Header Card for Tab
            val totalRemaining = if (selectedTab == 0) {
                loans.sumOf { it.remainingBalance }
            } else {
                receivables.sumOf { it.remainingBalance }
            }

            val totalInterest = if (selectedTab == 0) loans.sumOf { it.calculatedInterest } else 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "RESUMO DE EMPRÉSTIMOS" else "RESUMO A RECEBER",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Saldo Restante Total",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(totalRemaining),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = InboundBlue
                        )
                    }

                    if (selectedTab == 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Juros Calculados Total",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(totalInterest),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            // List View
            if (selectedTab == 0) {
                if (loans.isEmpty()) {
                    EmptyListPlaceholder("Nenhum empréstimo cadastrado.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 76.dp)
                    ) {
                        items(loans, key = { it.id }) { item ->
                            LoanItemCard(
                                loan = item,
                                onPaymentClick = { itemToPay = item },
                                onRolloverClick = { itemToRollover = item },
                                onHistoryClick = {
                                    viewModel.getHistoryForItem("LOAN", item.id).let { flow ->
                                        // Fetch history in coroutine state or flow
                                    }
                                },
                                onEditClick = {
                                    loanToEdit = item
                                    showAddEditLoan = true
                                },
                                onDeleteClick = { itemToDelete = item }
                            )
                        }
                    }
                }
            } else {
                if (receivables.isEmpty()) {
                    EmptyListPlaceholder("Nenhum valor a receber cadastrado.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 76.dp)
                    ) {
                        items(receivables, key = { it.id }) { item ->
                            ReceivableItemCard(
                                receivable = item,
                                onPaymentClick = { itemToPay = item },
                                onHistoryClick = { /* open history */ },
                                onEditClick = {
                                    receivableToEdit = item
                                    showAddEditReceivable = true
                                },
                                onDeleteClick = { itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddEditLoan) {
        AddEditLoanDialog(
            loanToEdit = loanToEdit,
            onDismiss = { showAddEditLoan = false },
            onSave = { loan ->
                viewModel.saveLoan(loan)
                showAddEditLoan = false
            }
        )
    }

    if (showAddEditReceivable) {
        AddEditReceivableDialog(
            itemToEdit = receivableToEdit,
            onDismiss = { showAddEditReceivable = false },
            onSave = { item ->
                viewModel.saveReceivable(item)
                showAddEditReceivable = false
            }
        )
    }

    if (itemToPay != null) {
        val target = itemToPay
        val (title, remBal) = when (target) {
            is LoanEntity -> "Recebimento / Abatimento de Empréstimo" to target.remainingBalance
            is ReceivableEntity -> "Recebimento Parcial / Total" to target.remainingBalance
            else -> "" to 0.0
        }

        PaymentDialog(
            title = title,
            remainingBalance = remBal,
            onDismiss = { itemToPay = null },
            onConfirmPayment = { amount, note ->
                when (target) {
                    is LoanEntity -> viewModel.addLoanPayment(target.id, amount, note)
                    is ReceivableEntity -> viewModel.addReceivablePayment(target.id, amount, note)
                }
                itemToPay = null
            }
        )
    }

    if (itemToRollover != null) {
        val loan = itemToRollover!!
        RolloverDialog(
            currentDueDate = loan.dueDate,
            onDismiss = { itemToRollover = null },
            onConfirmRollover = { months ->
                viewModel.rolloverLoanDueDate(loan.id, months)
                itemToRollover = null
            }
        )
    }

    if (itemToDelete != null) {
        val target = itemToDelete
        val title = when (target) {
            is LoanEntity -> "Excluir Empréstimo"
            is ReceivableEntity -> "Excluir Valor a Receber"
            else -> "Excluir"
        }

        ConfirmDeleteDialog(
            title = title,
            message = "Tem certeza que deseja excluir este registro? O histórico também será apagado.",
            onDismiss = { itemToDelete = null },
            onConfirm = {
                when (target) {
                    is LoanEntity -> viewModel.deleteLoan(target)
                    is ReceivableEntity -> viewModel.deleteReceivable(target)
                }
                itemToDelete = null
            }
        )
    }

    if (historyList != null) {
        HistoryDialog(
            historyList = historyList!!,
            onDismiss = { historyList = null }
        )
    }
}

@Composable
fun EmptyListPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
