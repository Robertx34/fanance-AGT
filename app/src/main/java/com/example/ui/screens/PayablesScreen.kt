package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.ui.components.AddEditPayableDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.GlobalSearchBar
import com.example.ui.components.HistoryDialog
import com.example.ui.components.PayableItemCard
import com.example.ui.components.PaymentDialog
import com.example.ui.components.RolloverDialog
import com.example.ui.theme.PayableRose
import com.example.ui.viewmodel.FinancialViewModel
import com.example.util.CurrencyFormatter

@Composable
fun PayablesScreen(
    viewModel: FinancialViewModel,
    modifier: Modifier = Modifier
) {
    val payables by viewModel.filteredPayables.collectAsStateWithLifecycle()

    var showAddEditPayable by remember { mutableStateOf(false) }
    var payableToEdit by remember { mutableStateOf<PayableEntity?>(null) }

    var itemToPay by remember { mutableStateOf<PayableEntity?>(null) }
    var itemToRollover by remember { mutableStateOf<PayableEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<PayableEntity?>(null) }
    var historyList by remember { mutableStateOf<List<PaymentHistoryEntity>?>(null) }

    val totalToPay = payables.sumOf { it.remainingBalance }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    payableToEdit = null
                    showAddEditPayable = true
                },
                containerColor = PayableRose,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("payables_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar Dívida",
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
            // Header Summary Card
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
                            text = "SALDO RESTANTE TOTAL A PAGAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(totalToPay),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = PayableRose
                        )
                    }
                }
            }

            // List
            if (payables.isEmpty()) {
                EmptyListPlaceholder("Nenhuma dívida a pagar cadastrada.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 76.dp)
                ) {
                    items(payables, key = { it.id }) { item ->
                        PayableItemCard(
                            payable = item,
                            onPaymentClick = { itemToPay = item },
                            onRolloverClick = { itemToRollover = item },
                            onHistoryClick = { /* open history */ },
                            onEditClick = {
                                payableToEdit = item
                                showAddEditPayable = true
                            },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddEditPayable) {
        AddEditPayableDialog(
            itemToEdit = payableToEdit,
            onDismiss = { showAddEditPayable = false },
            onSave = { item ->
                viewModel.savePayable(item)
                showAddEditPayable = false
            }
        )
    }

    if (itemToPay != null) {
        val target = itemToPay!!
        PaymentDialog(
            title = "Pagamento / Abatimento de Dívida",
            remainingBalance = target.remainingBalance,
            onDismiss = { itemToPay = null },
            onConfirmPayment = { amount, note ->
                viewModel.addPayablePayment(target.id, amount, note)
                itemToPay = null
            }
        )
    }

    if (itemToRollover != null) {
        val payable = itemToRollover!!
        RolloverDialog(
            currentDueDate = payable.dueDate,
            onDismiss = { itemToRollover = null },
            onConfirmRollover = { months ->
                viewModel.rolloverPayableDueDate(payable.id, months)
                itemToRollover = null
            }
        )
    }

    if (itemToDelete != null) {
        val target = itemToDelete!!
        ConfirmDeleteDialog(
            title = "Excluir Dívida a Pagar",
            message = "Tem certeza que deseja excluir esta dívida? O histórico associado será removido.",
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deletePayable(target)
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
