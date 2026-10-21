package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.util.DateUtils

@Entity(tableName = "payables")
data class PayableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditorName: String,
    val category: String = PayableCategory.SUPPLIER.label,
    val description: String = "",
    val originalAmount: Double,
    val amountPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val hasNoDueDate: Boolean = false,
    val notes: String = ""
) {
    val remainingBalance: Double
        get() = maxOf(0.0, originalAmount - amountPaid)

    val status: ItemStatus
        get() {
            return when {
                amountPaid >= originalAmount -> ItemStatus.PAID
                amountPaid > 0 -> ItemStatus.PARTIAL
                !hasNoDueDate && dueDate != null && dueDate < DateUtils.getStartOfDay() -> ItemStatus.OVERDUE
                else -> ItemStatus.PENDING
            }
        }
}
