package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.util.DateUtils

@Entity(tableName = "receivables")
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val category: String = ReceivableCategory.SALE.label,
    val description: String = "",
    val originalAmount: Double,
    val amountReceived: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val hasNoDueDate: Boolean = false,
    val notes: String = ""
) {
    val remainingBalance: Double
        get() = maxOf(0.0, originalAmount - amountReceived)

    val status: ItemStatus
        get() {
            return when {
                amountReceived >= originalAmount -> ItemStatus.PAID
                amountReceived > 0 -> ItemStatus.PARTIAL
                !hasNoDueDate && dueDate != null && dueDate < DateUtils.getStartOfDay() -> ItemStatus.OVERDUE
                else -> ItemStatus.PENDING
            }
        }
}
