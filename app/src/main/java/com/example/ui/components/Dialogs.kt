package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InterestType
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableCategory
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableCategory
import com.example.data.local.entity.ReceivableEntity
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar

// --- Add/Edit Loan Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanDialog(
    loanToEdit: LoanEntity? = null,
    onDismiss: () -> Unit,
    onSave: (LoanEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(loanToEdit?.clientName ?: "") }
    var amountStr by remember { mutableStateOf(loanToEdit?.amount?.toString() ?: "") }
    var rateStr by remember { mutableStateOf(loanToEdit?.interestRate?.toString() ?: "5.0") }
    var interestType by remember { mutableStateOf(loanToEdit?.interestType ?: InterestType.MONTHLY.shortLabel) }
    var hasNoDueDate by remember { mutableStateOf(loanToEdit?.hasNoDueDate ?: false) }
    var dueDate by remember {
        mutableStateOf(
            loanToEdit?.dueDate ?: Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var notes by remember { mutableStateOf(loanToEdit?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val calculatedInterest = amount * (rate / 100.0)
    val totalAmount = amount + calculatedInterest

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (loanToEdit == null) "Cadastro" else "Edição",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (loanToEdit == null) "Preencha os dados do empréstimo" else "Atualize os dados do empréstimo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nome do Cliente") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_client_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor Emprestado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_amount_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Taxa de Juros (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("loan_rate_input"),
                        singleLine = true
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Tipo de Juros",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = interestType == InterestType.MONTHLY.shortLabel,
                                onClick = { interestType = InterestType.MONTHLY.shortLabel }
                            )
                            Text("A.M.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            RadioButton(
                                selected = interestType == InterestType.DAILY.shortLabel,
                                onClick = { interestType = InterestType.DAILY.shortLabel }
                            )
                            Text("A.D.", fontSize = 12.sp)
                        }
                    }
                }

                // Automatic calculation preview box
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cálculo Automático dos Juros:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Juros: ${CurrencyFormatter.format(calculatedInterest)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Total a Cobrar: ${CurrencyFormatter.format(totalAmount)}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasNoDueDate,
                        onCheckedChange = { hasNoDueDate = it }
                    )
                    Text("Sem prazo de vencimento")
                }

                if (!hasNoDueDate) {
                    OutlinedTextField(
                        value = DateUtils.formatDate(dueDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de Vencimento") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Selecionar data",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank() && amount > 0) {
                        onSave(
                            LoanEntity(
                                id = loanToEdit?.id ?: 0L,
                                clientName = clientName.trim(),
                                amount = amount,
                                interestRate = rate,
                                interestType = interestType,
                                createdAt = loanToEdit?.createdAt ?: System.currentTimeMillis(),
                                dueDate = if (hasNoDueDate) null else dueDate,
                                hasNoDueDate = hasNoDueDate,
                                amountReceived = loanToEdit?.amountReceived ?: 0.0,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("loan_save_button"),
                enabled = clientName.isNotBlank() && amount > 0
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.localToUtcMidnight(dueDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dueDate = DateUtils.utcMidnightToLocal(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// --- Add/Edit Receivable Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReceivableDialog(
    itemToEdit: ReceivableEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ReceivableEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(itemToEdit?.clientName ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: ReceivableCategory.SALE.label) }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var amountStr by remember { mutableStateOf(itemToEdit?.originalAmount?.toString() ?: "") }
    var hasNoDueDate by remember { mutableStateOf(itemToEdit?.hasNoDueDate ?: false) }
    var dueDate by remember {
        mutableStateOf(
            itemToEdit?.dueDate ?: Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (itemToEdit == null) "Cadastro" else "Edição",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (itemToEdit == null) "Preencha os dados da venda / receita" else "Atualize os dados da venda / receita",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Cliente / Pagador") },
                    modifier = Modifier.fillMaxWidth().testTag("receivable_client_input"),
                    singleLine = true
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        ReceivableCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat.label
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor Original (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("receivable_amount_input"),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasNoDueDate,
                        onCheckedChange = { hasNoDueDate = it }
                    )
                    Text("Sem prazo de vencimento")
                }

                if (!hasNoDueDate) {
                    OutlinedTextField(
                        value = DateUtils.formatDate(dueDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de Vencimento") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Data",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank() && amount > 0) {
                        onSave(
                            ReceivableEntity(
                                id = itemToEdit?.id ?: 0L,
                                clientName = clientName.trim(),
                                category = category,
                                description = description.trim(),
                                originalAmount = amount,
                                amountReceived = itemToEdit?.amountReceived ?: 0.0,
                                createdAt = itemToEdit?.createdAt ?: System.currentTimeMillis(),
                                dueDate = if (hasNoDueDate) null else dueDate,
                                hasNoDueDate = hasNoDueDate,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("receivable_save_button"),
                enabled = clientName.isNotBlank() && amount > 0
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.localToUtcMidnight(dueDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = DateUtils.utcMidnightToLocal(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// --- Add/Edit Payable Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPayableDialog(
    itemToEdit: PayableEntity? = null,
    onDismiss: () -> Unit,
    onSave: (PayableEntity) -> Unit
) {
    var creditorName by remember { mutableStateOf(itemToEdit?.creditorName ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: PayableCategory.SUPPLIER.label) }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var amountStr by remember { mutableStateOf(itemToEdit?.originalAmount?.toString() ?: "") }
    var hasNoDueDate by remember { mutableStateOf(itemToEdit?.hasNoDueDate ?: false) }
    var dueDate by remember {
        mutableStateOf(
            itemToEdit?.dueDate ?: Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (itemToEdit == null) "Cadastro" else "Edição",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (itemToEdit == null) "Preencha os dados da dívida" else "Atualize os dados da dívida",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = creditorName,
                    onValueChange = { creditorName = it },
                    label = { Text("Credor / Fornecedor") },
                    modifier = Modifier.fillMaxWidth().testTag("payable_creditor_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        PayableCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat.label
                                    isCategoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor Original (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("payable_amount_input"),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasNoDueDate,
                        onCheckedChange = { hasNoDueDate = it }
                    )
                    Text("Sem prazo de vencimento")
                }

                if (!hasNoDueDate) {
                    OutlinedTextField(
                        value = DateUtils.formatDate(dueDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de Vencimento") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Data",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (creditorName.isNotBlank() && amount > 0) {
                        onSave(
                            PayableEntity(
                                id = itemToEdit?.id ?: 0L,
                                creditorName = creditorName.trim(),
                                category = category,
                                description = description.trim(),
                                originalAmount = amount,
                                amountPaid = itemToEdit?.amountPaid ?: 0.0,
                                createdAt = itemToEdit?.createdAt ?: System.currentTimeMillis(),
                                dueDate = if (hasNoDueDate) null else dueDate,
                                hasNoDueDate = hasNoDueDate,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("payable_save_button"),
                enabled = creditorName.isNotBlank() && amount > 0
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.localToUtcMidnight(dueDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = DateUtils.utcMidnightToLocal(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// --- Payment/Abatement Dialog ---
@Composable
fun PaymentDialog(
    title: String,
    remainingBalance: Double,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val paymentAmount = amountStr.toDoubleOrNull() ?: 0.0
    val newRemaining = maxOf(0.0, remainingBalance - paymentAmount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Saldo Restante Atual:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = CurrencyFormatter.format(remainingBalance),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor a Abater / Pagar (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    singleLine = true
                )

                if (paymentAmount > 0) {
                    Text(
                        text = "Novo Saldo Restante: ${CurrencyFormatter.format(newRemaining)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Observação do lançamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paymentAmount > 0) {
                        onConfirmPayment(paymentAmount, note.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_payment_btn"),
                enabled = paymentAmount > 0
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- Rollover Date Dialog ---
@Composable
fun RolloverDialog(
    currentDueDate: Long?,
    onDismiss: () -> Unit,
    onConfirmRollover: (monthsToAdd: Int) -> Unit
) {
    var selectedMonths by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar Pagamento / Vencimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mover a data de vencimento para os próximos meses:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Vencimento Atual: ${DateUtils.formatDate(currentDueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(1, 2, 3, 6, 12).forEach { m ->
                        TextButton(
                            onClick = { selectedMonths = m },
                            modifier = Modifier.testTag("rollover_month_$m")
                        ) {
                            Text(
                                text = "+$m mês${if (m > 1) "es" else ""}",
                                fontWeight = if (selectedMonths == m) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMonths == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRollover(selectedMonths) },
                modifier = Modifier.testTag("confirm_rollover_btn")
            ) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- History Dialog ---
@Composable
fun HistoryDialog(
    historyList: List<PaymentHistoryEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Registros de movimentações e pagamentos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Text(
                    text = "Nenhum histórico registrado até o momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = CurrencyFormatter.format(item.amountPaid),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = item.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = DateUtils.formatDateTime(item.paymentDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

// --- Confirm Delete Dialog ---
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
