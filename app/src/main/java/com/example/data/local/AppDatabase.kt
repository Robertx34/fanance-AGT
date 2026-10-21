package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.PayableDao
import com.example.data.local.dao.PaymentHistoryDao
import com.example.data.local.dao.ReceivableDao
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity

@Database(
    entities = [
        LoanEntity::class,
        ReceivableEntity::class,
        PayableEntity::class,
        PaymentHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun payableDao(): PayableDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_financeiro.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
