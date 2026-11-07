package com.example.data.local.entity

import androidx.room.ColumnInfo
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
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val settledUnderLegacyRule: Boolean = false
) {
    val calculatedInterest: Double
        get() = amount * (interestRate / 100.0)

    val totalWithInterest: Double
        get() = amount + calculatedInterest

    val isSettled: Boolean
        get() = amountReceived >= totalWithInterest || (settledUnderLegacyRule && amountReceived >= amount)

    val remainingBalance: Double
        get() = if (isSettled) 0.0 else maxOf(0.0, totalWithInterest - amountReceived)

    val status: ItemStatus
        get() {
            return when {
                isSettled -> ItemStatus.PAID
                amountReceived > 0 -> ItemStatus.PARTIAL
                !hasNoDueDate && dueDate != null && dueDate < DateUtils.getStartOfDay() -> ItemStatus.OVERDUE
                else -> ItemStatus.PENDING
            }
        }
}
