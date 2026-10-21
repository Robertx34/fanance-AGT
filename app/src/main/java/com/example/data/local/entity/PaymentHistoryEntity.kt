package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_history")
data class PaymentHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemType: String, // "LOAN", "RECEIVABLE", "PAYABLE"
    val itemId: Long,
    val amountPaid: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val note: String = ""
)
