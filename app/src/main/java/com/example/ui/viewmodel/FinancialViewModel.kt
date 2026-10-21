package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ItemStatus
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import com.example.data.local.entity.ReceivableCategory
import com.example.data.repository.FinancialRepository
import com.example.model.DashboardItem
import com.example.model.FilterOption
import com.example.util.BackupManager
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinancialRepository(AppDatabase.getInstance(application))

    val rawLoans: StateFlow<List<LoanEntity>> = repository.loans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawReceivables: StateFlow<List<ReceivableEntity>> = repository.receivables.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawPayables: StateFlow<List<PayableEntity>> = repository.payables.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawHistory: StateFlow<List<PaymentHistoryEntity>> = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterOption.ALL)
    val selectedFilter: StateFlow<FilterOption> = _selectedFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: FilterOption) {
        _selectedFilter.value = filter
    }

    // --- Filtered & Sorted Streams ---
    val filteredLoans: StateFlow<List<LoanEntity>> = combine(
        rawLoans, _searchQuery
    ) { list, query ->
        val filtered = list.filter { item ->
            query.isBlank() ||
                    item.clientName.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)
        }
        filtered.sortFinancialItems(
            getDueDate = { it.dueDate },
            getHasNoDueDate = { it.hasNoDueDate },
            getName = { it.clientName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReceivables: StateFlow<List<ReceivableEntity>> = combine(
        rawReceivables, _searchQuery
    ) { list, query ->
        val filtered = list.filter { item ->
            query.isBlank() ||
                    item.clientName.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)
        }
        filtered.sortFinancialItems(
            getDueDate = { it.dueDate },
            getHasNoDueDate = { it.hasNoDueDate },
            getName = { it.clientName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPayables: StateFlow<List<PayableEntity>> = combine(
        rawPayables, _searchQuery
    ) { list, query ->
        val filtered = list.filter { item ->
            query.isBlank() ||
                    item.creditorName.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true)
        }
        filtered.sortFinancialItems(
            getDueDate = { it.dueDate },
            getHasNoDueDate = { it.hasNoDueDate },
            getName = { it.creditorName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun <T> List<T>.sortFinancialItems(
        getDueDate: (T) -> Long?,
        getHasNoDueDate: (T) -> Boolean,
        getName: (T) -> String
    ): List<T> {
        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        fun getGroup(item: T): Int {
            val dueDate = getDueDate(item)
            val noDue = getHasNoDueDate(item)
            if (noDue || dueDate == null) return 4
            return when {
                dueDate < startOfToday -> 1
                dueDate <= endOfToday -> 2
                else -> 3
            }
        }

        return this.sortedWith { a, b ->
            val groupA = getGroup(a)
            val groupB = getGroup(b)
            if (groupA != groupB) {
                groupA.compareTo(groupB)
            } else {
                val dueA = getDueDate(a) ?: Long.MAX_VALUE
                val dueB = getDueDate(b) ?: Long.MAX_VALUE
                if (dueA != dueB) {
                    dueA.compareTo(dueB)
                } else {
                    getName(a).compareTo(getName(b), ignoreCase = true)
                }
            }
        }
    }

    // --- Calculations for Dashboard ---
    // Vendas a Receber Exclusivo (Apenas Categoria Venda)
    val totalSalesAmount: StateFlow<Double> = rawReceivables.combine(rawReceivables) { list, _ ->
        list.filter {
            it.status != ItemStatus.PAID &&
                    (it.category.equals("Venda", ignoreCase = true) || it.category == ReceivableCategory.SALE.label)
        }.sumOf { it.remainingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pendingSalesCount: StateFlow<Int> = rawReceivables.combine(rawReceivables) { list, _ ->
        list.count {
            it.status != ItemStatus.PAID &&
                    (it.category.equals("Venda", ignoreCase = true) || it.category == ReceivableCategory.SALE.label)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLoansReceivable: StateFlow<Double> = rawLoans.combine(rawLoans) { list, _ ->
        list.filter { it.status != ItemStatus.PAID }.sumOf { it.remainingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalReceivablesAmount: StateFlow<Double> = rawReceivables.combine(rawReceivables) { list, _ ->
        list.filter { it.status != ItemStatus.PAID }.sumOf { it.remainingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total a Receber Geral = Total Receivables + Total Loans Balance
    val totalToReceive: StateFlow<Double> = combine(
        totalLoansReceivable, totalReceivablesAmount
    ) { loanBal, recBal ->
        loanBal + recBal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Juros a Receber (Mês Vigente) = Soma dos juros dos empréstimos ativos cujo vencimento (ou data de criação) pertence ao mês atual
    val totalInterestToReceive: StateFlow<Double> = rawLoans.combine(rawLoans) { list, _ ->
        list.filter { loan ->
            if (loan.status == ItemStatus.PAID) return@filter false
            val targetDate = if (!loan.hasNoDueDate && loan.dueDate != null) loan.dueDate else loan.createdAt
            DateUtils.isThisMonth(targetDate)
        }.sumOf { it.calculatedInterest }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total a Pagar = Sum of pending debt balances
    val totalToPay: StateFlow<Double> = rawPayables.combine(rawPayables) { list, _ ->
        list.filter { it.status != ItemStatus.PAID }.sumOf { it.remainingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Entradas Esperadas = Sum of interest for active loans due in current month
    val expectedIncomeCurrentMonth: StateFlow<Double> = totalInterestToReceive

    // Próximas Cobranças (Empréstimos e Recebíveis/Vendas a cobrar próximos)
    val upcomingCollections: StateFlow<List<DashboardItem>> = combine(
        rawLoans, rawReceivables
    ) { loans, receivables ->
        val todayStart = DateUtils.getStartOfDay()
        val loanItems = loans.filter { it.status != ItemStatus.PAID && (it.dueDate == null || it.dueDate >= todayStart) }
            .map { DashboardItem.Loan(it) }
        val recItems = receivables.filter { it.status != ItemStatus.PAID && (it.dueDate == null || it.dueDate >= todayStart) }
            .map { DashboardItem.Receivable(it) }
        (loanItems + recItems).sortedWith(compareBy<DashboardItem> { it.dueDate == null }.thenBy { it.dueDate ?: Long.MAX_VALUE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Próximos Vencimentos (Dívidas a pagar próximas)
    val upcomingPayables: StateFlow<List<PayableEntity>> = rawPayables.combine(rawPayables) { list, _ ->
        val todayStart = DateUtils.getStartOfDay()
        list.filter { it.status != ItemStatus.PAID && (it.dueDate == null || it.dueDate >= todayStart) }
            .sortedWith(compareBy<PayableEntity> { it.dueDate == null }.thenBy { it.dueDate ?: Long.MAX_VALUE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contas Vencidas (Empréstimos, Vendas e Dívidas vencidas)
    val overdueItems: StateFlow<List<DashboardItem>> = combine(
        rawLoans, rawReceivables, rawPayables
    ) { loans, receivables, payables ->
        val loanOverdue = loans.filter { it.status == ItemStatus.OVERDUE }.map { DashboardItem.Loan(it) }
        val recOverdue = receivables.filter { it.status == ItemStatus.OVERDUE }.map { DashboardItem.Receivable(it) }
        val payOverdue = payables.filter { it.status == ItemStatus.OVERDUE }.map { DashboardItem.Payable(it) }
        loanOverdue + recOverdue + payOverdue
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Item Actions ---
    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            if (loan.id == 0L) {
                repository.insertLoan(loan)
            } else {
                repository.updateLoan(loan)
            }
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch { repository.deleteLoan(loan) }
    }

    fun addLoanPayment(loanId: Long, amount: Double, note: String) {
        viewModelScope.launch { repository.addLoanPayment(loanId, amount, note) }
    }

    fun rolloverLoanDueDate(loanId: Long, monthsToAdd: Int = 1) {
        viewModelScope.launch { repository.rolloverLoanDueDate(loanId, monthsToAdd) }
    }

    fun saveReceivable(receivable: ReceivableEntity) {
        viewModelScope.launch {
            if (receivable.id == 0L) {
                repository.insertReceivable(receivable)
            } else {
                repository.updateReceivable(receivable)
            }
        }
    }

    fun deleteReceivable(receivable: ReceivableEntity) {
        viewModelScope.launch { repository.deleteReceivable(receivable) }
    }

    fun addReceivablePayment(receivableId: Long, amount: Double, note: String) {
        viewModelScope.launch { repository.addReceivablePayment(receivableId, amount, note) }
    }

    fun savePayable(payable: PayableEntity) {
        viewModelScope.launch {
            if (payable.id == 0L) {
                repository.insertPayable(payable)
            } else {
                repository.updatePayable(payable)
            }
        }
    }

    fun deletePayable(payable: PayableEntity) {
        viewModelScope.launch { repository.deletePayable(payable) }
    }

    fun addPayablePayment(payableId: Long, amount: Double, note: String) {
        viewModelScope.launch { repository.addPayablePayment(payableId, amount, note) }
    }

    fun rolloverPayableDueDate(payableId: Long, monthsToAdd: Int = 1) {
        viewModelScope.launch { repository.rolloverPayableDueDate(payableId, monthsToAdd) }
    }

    fun getHistoryForItem(type: String, itemId: Long) = repository.getHistoryForItem(type, itemId)

    // --- Backup & Reset ---
    fun generateBackupJson(): String {
        return BackupManager.generateBackupJson(
            loans = rawLoans.value,
            receivables = rawReceivables.value,
            payables = rawPayables.value,
            history = rawHistory.value
        )
    }

    fun restoreBackupJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val parsed = BackupManager.parseBackupJson(jsonString)
                repository.clearAllData()
                parsed.loans.forEach { repository.insertLoan(it) }
                parsed.receivables.forEach { repository.insertReceivable(it) }
                parsed.payables.forEach { repository.insertPayable(it) }
                parsed.history.forEach { repository.insertPaymentHistory(it) }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erro ao restaurar backup.")
            }
        }
    }

    fun clearAllDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }
}
