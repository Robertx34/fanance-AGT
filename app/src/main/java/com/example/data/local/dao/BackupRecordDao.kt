package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BackupRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupRecordDao {
    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BackupRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BackupRecordEntity): Long

    @Update
    suspend fun updateRecord(record: BackupRecordEntity)

    @Query("UPDATE backup_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): BackupRecordEntity?
}
