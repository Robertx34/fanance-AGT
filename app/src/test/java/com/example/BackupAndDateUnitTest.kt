package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ItemStatus
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import com.example.data.repository.FinancialRepository
import com.example.util.BackupManager
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupAndDateUnitTest {

    @Test
    fun testBackupManager_serializationAndParsing() {
        val loans = listOf(
            LoanEntity(
                id = 1L,
                clientName = "Carlos Silva",
                amount = 1500.0,
                interestRate = 10.0,
                interestType = "A.M.",
                createdAt = 1700000000000L,
                dueDate = 1702592000000L,
                hasNoDueDate = false,
                amountReceived = 500.0,
                notes = "Parcela única"
            ),
            LoanEntity(
                id = 2L,
                clientName = "Sem Prazo Cliente",
                amount = 2000.0,
                interestRate = 5.0,
                interestType = "A.D.",
                createdAt = 1700000000000L,
                dueDate = null,
                hasNoDueDate = true,
                amountReceived = 0.0,
                notes = ""
            )
        )

        val receivables = listOf(
            ReceivableEntity(
                id = 10L,
                clientName = "Maria Souza",
                category = "Venda",
                description = "Produto A",
                originalAmount = 300.0,
                amountReceived = 100.0,
                createdAt = 1700000000000L,
                dueDate = 1702592000000L,
                hasNoDueDate = false,
                notes = "Entregue"
            )
        )

        val payables = listOf(
            PayableEntity(
                id = 20L,
                creditorName = "Fornecedor XYZ",
                category = "Fornecedor",
                description = "Matéria prima",
                originalAmount = 1200.0,
                amountPaid = 400.0,
                createdAt = 1700000000000L,
                dueDate = 1702592000000L,
                hasNoDueDate = false,
                notes = "Boleto"
            )
        )

        val history = listOf(
            PaymentHistoryEntity(
                id = 100L,
                itemType = "LOAN",
                itemId = 1L,
                amountPaid = 500.0,
                paymentDate = 1701000000000L,
                note = "Pagamento via Pix"
            )
        )

        val json = BackupManager.generateBackupJson(loans, receivables, payables, history)
        assertTrue(json.contains("\"backupVersion\": 2"))
        assertTrue(json.contains("\"Carlos Silva\""))
        assertTrue(json.contains("\"Maria Souza\""))
        assertTrue(json.contains("\"Fornecedor XYZ\""))
        assertTrue(json.contains("\"Pagamento via Pix\""))

        val parsed = BackupManager.parseBackupJson(json)
        assertEquals(2, parsed.loans.size)
        assertEquals(1, parsed.receivables.size)
        assertEquals(1, parsed.payables.size)
        assertEquals(1, parsed.history.size)

        // Check loan with due date: amount 1500 + 10% (150) = 1650 - 500 = 1150
        val loan1 = parsed.loans.first { it.id == 1L }
        assertEquals("Carlos Silva", loan1.clientName)
        assertEquals(1500.0, loan1.amount, 0.001)
        assertEquals(500.0, loan1.amountReceived, 0.001)
        assertEquals(1150.0, loan1.remainingBalance, 0.001)
        assertEquals(1702592000000L, loan1.dueDate)

        // Check loan without due date
        val loan2 = parsed.loans.first { it.id == 2L }
        assertTrue(loan2.hasNoDueDate)
        assertNull(loan2.dueDate)

        // Check history
        val hist1 = parsed.history.first()
        assertEquals("LOAN", hist1.itemType)
        assertEquals(1L, hist1.itemId)
        assertEquals(500.0, hist1.amountPaid, 0.001)
    }

    @Test
    fun testBackupManager_aesEncryptionAndDecryption() {
        val loans = listOf(
            LoanEntity(
                id = 1L,
                clientName = "Segredo Criptografado",
                amount = 5000.0,
                interestRate = 12.0,
                interestType = "A.M.",
                createdAt = 1700000000000L,
                dueDate = null,
                hasNoDueDate = true,
                amountReceived = 0.0,
                notes = "Confidencial"
            )
        )
        val password = "StrongPassword@123"

        // Generate encrypted backup
        val encryptedJson = BackupManager.generateBackupJson(loans, emptyList(), emptyList(), emptyList(), password = password)
        assertTrue(BackupManager.isBackupEncrypted(encryptedJson))
        assertTrue(encryptedJson.contains("\"encrypted\": true"))
        assertTrue(encryptedJson.contains("\"salt\":"))
        assertTrue(encryptedJson.contains("\"iv\":"))
        assertTrue(encryptedJson.contains("\"ciphertext\":"))

        // Decrypt with correct password
        val decryptedJson = BackupManager.decryptBackup(encryptedJson, password)
        val parsed = BackupManager.parseBackupJson(decryptedJson)
        assertEquals(1, parsed.loans.size)
        assertEquals("Segredo Criptografado", parsed.loans[0].clientName)
        assertEquals(5000.0, parsed.loans[0].amount, 0.001)

        // Decrypt with wrong password must throw SecurityException
        var failedAsExpected = false
        try {
            BackupManager.decryptBackup(encryptedJson, "WrongPassword@456")
        } catch (_: SecurityException) {
            failedAsExpected = true
        }
        assertTrue("Decryption with incorrect password must fail", failedAsExpected)
    }

    @Test
    fun testBackupManager_v1BackwardsCompatibility() {
        val v1Json = """
        {
            "app": "ControleFinanceiro",
            "backupVersion": 1,
            "exportDate": 1700000000000,
            "loans": [
                {
                    "id": 99,
                    "clientName": "Cliente Legado v1",
                    "amount": 800.0,
                    "interestRate": 0.0,
                    "interestType": "A.M.",
                    "amountReceived": 200.0,
                    "createdAt": 1700000000000,
                    "dueDate": null,
                    "hasNoDueDate": true,
                    "notes": "Legado"
                }
            ],
            "receivables": [],
            "payables": [],
            "history": []
        }
        """.trimIndent()

        val parsed = BackupManager.parseBackupJson(v1Json)
        assertEquals(1, parsed.loans.size)
        assertEquals("Cliente Legado v1", parsed.loans[0].clientName)
        assertEquals(600.0, parsed.loans[0].remainingBalance, 0.001)
    }

    @Test
    fun testDateUtils_utcMidnightConversion() {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 25, 0, 0, 0)
        }
        val utcMillis = utcCal.timeInMillis

        val localMillis = DateUtils.utcMidnightToLocal(utcMillis)
        val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }

        assertEquals(2026, localCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, localCal.get(Calendar.MONTH))
        assertEquals(25, localCal.get(Calendar.DAY_OF_MONTH))

        val backToUtc = DateUtils.localToUtcMidnight(localMillis)
        assertEquals(utcMillis, backToUtc)
    }

    @Test
    fun testRestoreAllRollbackOnFailure() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repo = FinancialRepository(db)

        val initialLoan = LoanEntity(
            id = 100L,
            clientName = "Cliente Antes",
            amount = 1000.0,
            interestRate = 10.0,
            amountReceived = 200.0
        )
        repo.insertLoan(initialLoan)
        assertEquals(1, db.loanDao().getAllLoans().first().size)

        var threwException = false
        try {
            db.withTransaction {
                repo.clearAllData()
                repo.insertLoan(LoanEntity(id = 200L, clientName = "Cliente Invalido", amount = -100.0, interestRate = 0.0))
                throw IllegalStateException("Falha simulada na inserção durante restoreAll")
            }
        } catch (_: Exception) {
            threwException = true
        }

        assertTrue("Transação deve falhar e reverter", threwException)
        val loansAfter = db.loanDao().getAllLoans().first()
        assertEquals("Dados anteriores continuam intactos após rollback", 1, loansAfter.size)
        assertEquals("Cliente Antes", loansAfter[0].clientName)
        db.close()
    }

    @Test
    fun testLoanSettlementRules() {
        // Empréstimo 1000 com 10%: totalWithInterest = 1100.0
        // recebido 1000 -> PARTIAL, saldo 100 (flag false)
        val loan1 = LoanEntity(
            clientName = "Teste 1",
            amount = 1000.0,
            interestRate = 10.0,
            amountReceived = 1000.0,
            settledUnderLegacyRule = false
        )
        assertFalse(loan1.isSettled)
        assertEquals(ItemStatus.PARTIAL, loan1.status)
        assertEquals(100.0, loan1.remainingBalance, 0.001)

        // Mesmo empréstimo, recebido 1100 -> PAID, saldo 0
        val loan2 = loan1.copy(amountReceived = 1100.0)
        assertTrue(loan2.isSettled)
        assertEquals(ItemStatus.PAID, loan2.status)
        assertEquals(0.0, loan2.remainingBalance, 0.001)

        // Mesmo empréstimo com settledUnderLegacyRule = true e recebido 1000 -> PAID, saldo 0
        val loan3 = loan1.copy(settledUnderLegacyRule = true)
        assertTrue(loan3.isSettled)
        assertEquals(ItemStatus.PAID, loan3.status)
        assertEquals(0.0, loan3.remainingBalance, 0.001)

        // Flag true mas principal editado para 2000 e recebido 1000 -> NÃO quitado
        val loan4 = loan3.copy(amount = 2000.0) // totalWithInterest = 2200, amountReceived = 1000 < amount (2000)
        assertFalse(loan4.isSettled)
        assertEquals(ItemStatus.PARTIAL, loan4.status)
        assertEquals(1200.0, loan4.remainingBalance, 0.001)
    }

    @Test
    fun testMigration1To2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration_test.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `loans` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `clientName` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `interestRate` REAL NOT NULL,
                            `interestType` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `dueDate` INTEGER,
                            `hasNoDueDate` INTEGER NOT NULL,
                            `amountReceived` REAL NOT NULL,
                            `notes` TEXT NOT NULL
                        )
                    """.trimIndent())
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Inserir um empréstimo quitado (1000 / recebido 1000), um parcial (1000 / recebido 400) e um sem recebimento
        db.execSQL("INSERT INTO loans (id, clientName, amount, interestRate, interestType, createdAt, dueDate, hasNoDueDate, amountReceived, notes) VALUES (1, 'Quitado v1', 1000.0, 10.0, 'A.M.', 1700000000000, 1702592000000, 0, 1000.0, 'nota 1')")
        db.execSQL("INSERT INTO loans (id, clientName, amount, interestRate, interestType, createdAt, dueDate, hasNoDueDate, amountReceived, notes) VALUES (2, 'Parcial v1', 1000.0, 10.0, 'A.M.', 1700000000000, 1702592000000, 0, 400.0, 'nota 2')")
        db.execSQL("INSERT INTO loans (id, clientName, amount, interestRate, interestType, createdAt, dueDate, hasNoDueDate, amountReceived, notes) VALUES (3, 'Zerado v1', 1000.0, 10.0, 'A.M.', 1700000000000, 1702592000000, 0, 0.0, 'nota 3')")

        // Executar MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Confirmar flags 1, 0 e 0 e que todos os outros dados foram preservados
        val cursor = db.query("SELECT id, clientName, amount, amountReceived, settledUnderLegacyRule FROM loans ORDER BY id ASC")
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("Quitado v1", cursor.getString(1))
        assertEquals(1000.0, cursor.getDouble(2), 0.001)
        assertEquals(1000.0, cursor.getDouble(3), 0.001)
        assertEquals(1, cursor.getInt(4)) // Flag 1

        assertTrue(cursor.moveToNext())
        assertEquals(2L, cursor.getLong(0))
        assertEquals("Parcial v1", cursor.getString(1))
        assertEquals(1000.0, cursor.getDouble(2), 0.001)
        assertEquals(400.0, cursor.getDouble(3), 0.001)
        assertEquals(0, cursor.getInt(4)) // Flag 0

        assertTrue(cursor.moveToNext())
        assertEquals(3L, cursor.getLong(0))
        assertEquals("Zerado v1", cursor.getString(1))
        assertEquals(1000.0, cursor.getDouble(2), 0.001)
        assertEquals(0.0, cursor.getDouble(3), 0.001)
        assertEquals(0, cursor.getInt(4)) // Flag 0

        cursor.close()
        db.close()
    }

    @Test
    fun testBackupLegacyFieldParsing() {
        val legacyJson = """
            {
                "app": "Controle Financeiro",
                "backupVersion": 1,
                "loans": [
                    {
                        "id": 1,
                        "clientName": "Legado Quitado",
                        "amount": 1000.0,
                        "interestRate": 10.0,
                        "interestType": "A.M.",
                        "createdAt": 1700000000000,
                        "amountReceived": 1000.0,
                        "notes": ""
                    },
                    {
                        "id": 2,
                        "clientName": "Legado Pendente",
                        "amount": 1000.0,
                        "interestRate": 10.0,
                        "interestType": "A.M.",
                        "createdAt": 1700000000000,
                        "amountReceived": 500.0,
                        "notes": ""
                    }
                ],
                "receivables": [],
                "payables": [],
                "history": []
            }
        """.trimIndent()

        val parsed = BackupManager.parseBackupJson(legacyJson)
        val loanQuitado = parsed.loans.first { it.id == 1L }
        val loanPendente = parsed.loans.first { it.id == 2L }

        // Marca como legado apenas o empréstimo com recebido >= principal
        assertTrue(loanQuitado.settledUnderLegacyRule)
        assertTrue(loanQuitado.isSettled)
        assertEquals(ItemStatus.PAID, loanQuitado.status)
        assertEquals(0.0, loanQuitado.remainingBalance, 0.001)

        assertFalse(loanPendente.settledUnderLegacyRule)
        assertFalse(loanPendente.isSettled)
        assertEquals(ItemStatus.PARTIAL, loanPendente.status)
        assertEquals(600.0, loanPendente.remainingBalance, 0.001)

        // Round-trip gerar -> parsear preserva o campo
        val jsonRoundTrip = BackupManager.generateBackupJson(parsed.loans, emptyList(), emptyList(), emptyList())
        assertTrue(jsonRoundTrip.contains("\"settledUnderLegacyRule\": true"))
        assertTrue(jsonRoundTrip.contains("\"settledUnderLegacyRule\": false"))

        val parsedRoundTrip = BackupManager.parseBackupJson(jsonRoundTrip)
        val rtQuitado = parsedRoundTrip.loans.first { it.id == 1L }
        val rtPendente = parsedRoundTrip.loans.first { it.id == 2L }
        assertTrue(rtQuitado.settledUnderLegacyRule)
        assertFalse(rtPendente.settledUnderLegacyRule)
    }
}
