package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BackupRecordEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar

class FinancialRepository(private val db: AppDatabase) {

    val loans: Flow<List<LoanEntity>> = db.loanDao().getAllLoans()
    val receivables: Flow<List<ReceivableEntity>> = db.receivableDao().getAllReceivables()
    val payables: Flow<List<PayableEntity>> = db.payableDao().getAllPayables()
    val allHistory: Flow<List<PaymentHistoryEntity>> = db.paymentHistoryDao().getAllHistory()

    private val _backupRecords = MutableStateFlow<List<BackupRecordEntity>>(emptyList())
    val backupRecords: Flow<List<BackupRecordEntity>> = _backupRecords

    fun getHistoryForItem(type: String, itemId: Long): Flow<List<PaymentHistoryEntity>> {
        return db.paymentHistoryDao().getHistoryForItem(type, itemId)
    }

    // --- Loans ---
    suspend fun insertLoan(loan: LoanEntity): Long = db.loanDao().insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = db.loanDao().updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) {
        db.withTransaction {
            db.loanDao().deleteLoan(loan)
            db.paymentHistoryDao().deleteHistoryForItem("LOAN", loan.id)
        }
    }

    suspend fun addLoanPayment(loanId: Long, amount: Double, note: String) {
        db.withTransaction {
            val loan = db.loanDao().getLoanById(loanId) ?: return@withTransaction
            val newReceived = loan.amountReceived + amount
            val updated = loan.copy(amountReceived = newReceived)
            db.loanDao().updateLoan(updated)
            db.paymentHistoryDao().insertPayment(
                PaymentHistoryEntity(
                    itemType = "LOAN",
                    itemId = loanId,
                    amountPaid = amount,
                    note = note.ifBlank { "Recebimento parcial/total" }
                )
            )
        }
    }

    suspend fun rolloverLoanDueDate(loanId: Long, monthsToAdd: Int = 1) {
        val loan = db.loanDao().getLoanById(loanId) ?: return
        val baseTime = loan.dueDate ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            add(Calendar.MONTH, monthsToAdd)
        }
        val updated = loan.copy(
            dueDate = cal.timeInMillis,
            hasNoDueDate = false
        )
        db.loanDao().updateLoan(updated)
    }

    // --- Receivables ---
    suspend fun insertReceivable(receivable: ReceivableEntity): Long = db.receivableDao().insertReceivable(receivable)
    suspend fun updateReceivable(receivable: ReceivableEntity) = db.receivableDao().updateReceivable(receivable)
    suspend fun deleteReceivable(receivable: ReceivableEntity) {
        db.withTransaction {
            db.receivableDao().deleteReceivable(receivable)
            db.paymentHistoryDao().deleteHistoryForItem("RECEIVABLE", receivable.id)
        }
    }

    suspend fun addReceivablePayment(receivableId: Long, amount: Double, note: String) {
        db.withTransaction {
            val item = db.receivableDao().getReceivableById(receivableId) ?: return@withTransaction
            val newReceived = item.amountReceived + amount
            val updated = item.copy(amountReceived = newReceived)
            db.receivableDao().updateReceivable(updated)
            db.paymentHistoryDao().insertPayment(
                PaymentHistoryEntity(
                    itemType = "RECEIVABLE",
                    itemId = receivableId,
                    amountPaid = amount,
                    note = note.ifBlank { "Recebimento parcial/total" }
                )
            )
        }
    }

    // --- Payables ---
    suspend fun insertPayable(payable: PayableEntity): Long = db.payableDao().insertPayable(payable)
    suspend fun updatePayable(payable: PayableEntity) = db.payableDao().updatePayable(payable)
    suspend fun deletePayable(payable: PayableEntity) {
        db.withTransaction {
            db.payableDao().deletePayable(payable)
            db.paymentHistoryDao().deleteHistoryForItem("PAYABLE", payable.id)
        }
    }

    suspend fun addPayablePayment(payableId: Long, amount: Double, note: String) {
        db.withTransaction {
            val payable = db.payableDao().getPayableById(payableId) ?: return@withTransaction
            val newPaid = payable.amountPaid + amount
            val updated = payable.copy(amountPaid = newPaid)
            db.payableDao().updatePayable(updated)
            db.paymentHistoryDao().insertPayment(
                PaymentHistoryEntity(
                    itemType = "PAYABLE",
                    itemId = payableId,
                    amountPaid = amount,
                    note = note.ifBlank { "Pagamento parcial/total" }
                )
            )
        }
    }

    suspend fun rolloverPayableDueDate(payableId: Long, monthsToAdd: Int = 1) {
        val payable = db.payableDao().getPayableById(payableId) ?: return
        val baseTime = payable.dueDate ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseTime
            add(Calendar.MONTH, monthsToAdd)
        }
        val updated = payable.copy(
            dueDate = cal.timeInMillis,
            hasNoDueDate = false
        )
        db.payableDao().updatePayable(updated)
    }

    // --- Database Management ---
    suspend fun insertPaymentHistory(history: PaymentHistoryEntity) {
        db.paymentHistoryDao().insertPayment(history)
    }

    suspend fun clearAllData() {
        db.withTransaction {
            db.loanDao().deleteAll()
            db.receivableDao().deleteAll()
            db.payableDao().deleteAll()
            db.paymentHistoryDao().deleteAll()
        }
    }

    suspend fun addBackupRecord(record: BackupRecordEntity): Long {
        _backupRecords.value = listOf(record) + _backupRecords.value
        return record.id
    }

    suspend fun updateBackupRecordStatus(id: Long, status: String) {
        _backupRecords.value = _backupRecords.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    suspend fun restoreAll(
        loans: List<LoanEntity>,
        receivables: List<ReceivableEntity>,
        payables: List<PayableEntity>,
        history: List<PaymentHistoryEntity>
    ) {
        db.withTransaction {
            clearAllData()
            loans.forEach { db.loanDao().insertLoan(it) }
            receivables.forEach { db.receivableDao().insertReceivable(it) }
            payables.forEach { db.payableDao().insertPayable(it) }
            history.forEach { db.paymentHistoryDao().insertPayment(it) }
        }
    }

    suspend fun restoreDataTransactional(
        loans: List<LoanEntity>,
        receivables: List<ReceivableEntity>,
        payables: List<PayableEntity>,
        history: List<PaymentHistoryEntity>
    ) = restoreAll(loans, receivables, payables, history)
}
