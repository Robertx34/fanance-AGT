package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.util.DateUtils

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val amount: Double,
    val interestRate: Double,
    val interestType: String = InterestType.MONTHLY.shortLabel,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val hasNoDueDate: Boolean = false,
    val amountReceived: Double = 0.0,
    val notes: String = ""
) {
    val calculatedInterest: Double
        get() = amount * (interestRate / 100.0)

    val totalWithInterest: Double
        get() = amount + calculatedInterest

    val remainingBalance: Double
        get() = maxOf(0.0, amount - amountReceived)

    val status: ItemStatus
        get() {
            return when {
                amountReceived >= amount -> ItemStatus.PAID
                amountReceived > 0 -> ItemStatus.PARTIAL
                !hasNoDueDate && dueDate != null && dueDate < DateUtils.getStartOfDay() -> ItemStatus.OVERDUE
                else -> ItemStatus.PENDING
            }
        }
}
