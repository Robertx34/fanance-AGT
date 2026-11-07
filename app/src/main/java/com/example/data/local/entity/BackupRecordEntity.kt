package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val isEncrypted: Boolean = false,
    val backupVersion: Int = 2,
    val status: String = "GERADO" // "GERADO", "EXPORTADO", "FALHA"
)
