package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PayableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayableDao {
    @Query("SELECT * FROM payables ORDER BY createdAt DESC")
    fun getAllPayables(): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables WHERE id = :id")
    suspend fun getPayableById(id: Long): PayableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayable(payable: PayableEntity): Long

    @Update
    suspend fun updatePayable(payable: PayableEntity)

    @Delete
    suspend fun deletePayable(payable: PayableEntity)

    @Query("DELETE FROM payables")
    suspend fun deleteAll()
}
