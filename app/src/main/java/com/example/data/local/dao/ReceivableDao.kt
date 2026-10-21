package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY createdAt DESC")
    fun getAllReceivables(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getReceivableById(id: Long): ReceivableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: ReceivableEntity): Long

    @Update
    suspend fun updateReceivable(receivable: ReceivableEntity)

    @Delete
    suspend fun deleteReceivable(receivable: ReceivableEntity)

    @Query("DELETE FROM receivables")
    suspend fun deleteAll()
}
