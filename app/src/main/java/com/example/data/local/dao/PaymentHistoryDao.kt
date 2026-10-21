package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PaymentHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentHistoryDao {
    @Query("SELECT * FROM payment_history WHERE itemType = :type AND itemId = :itemId ORDER BY paymentDate DESC")
    fun getHistoryForItem(type: String, itemId: Long): Flow<List<PaymentHistoryEntity>>

    @Query("SELECT * FROM payment_history ORDER BY paymentDate DESC")
    fun getAllHistory(): Flow<List<PaymentHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentHistoryEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentHistoryEntity)

    @Query("DELETE FROM payment_history WHERE itemType = :type AND itemId = :itemId")
    suspend fun deleteHistoryForItem(type: String, itemId: Long)

    @Query("DELETE FROM payment_history")
    suspend fun deleteAll()
}
