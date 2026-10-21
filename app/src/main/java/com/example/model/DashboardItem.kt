package com.example.model

import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.ReceivableEntity

sealed interface DashboardItem {
    val id: Long
    val name: String
    val dueDate: Long?
    val amount: Double
    val categoryOrType: String

    data class Loan(val loan: LoanEntity) : DashboardItem {
        override val id: Long get() = loan.id
        override val name: String get() = loan.clientName
        override val dueDate: Long? get() = loan.dueDate
        override val amount: Double get() = loan.remainingBalance
        override val categoryOrType: String get() = "Empréstimo"
    }

    data class Receivable(val receivable: ReceivableEntity) : DashboardItem {
        override val id: Long get() = receivable.id
        override val name: String get() = receivable.clientName
        override val dueDate: Long? get() = receivable.dueDate
        override val amount: Double get() = receivable.remainingBalance
        override val categoryOrType: String get() = receivable.category
    }

    data class Payable(val payable: PayableEntity) : DashboardItem {
        override val id: Long get() = payable.id
        override val name: String get() = payable.creditorName
        override val dueDate: Long? get() = payable.dueDate
        override val amount: Double get() = payable.remainingBalance
        override val categoryOrType: String get() = "Dívida (${payable.category})"
    }
}
