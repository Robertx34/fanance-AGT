package com.example

import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import com.example.util.BackupManager
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertTrue(json.contains("\"backupVersion\": 1"))
        assertTrue(json.contains("\"Carlos Silva\""))
        assertTrue(json.contains("\"Maria Souza\""))
        assertTrue(json.contains("\"Fornecedor XYZ\""))
        assertTrue(json.contains("\"Pagamento via Pix\""))

        val parsed = BackupManager.parseBackupJson(json)
        assertEquals(2, parsed.loans.size)
        assertEquals(1, parsed.receivables.size)
        assertEquals(1, parsed.payables.size)
        assertEquals(1, parsed.history.size)

        // Check loan with due date
        val loan1 = parsed.loans.first { it.id == 1L }
        assertEquals("Carlos Silva", loan1.clientName)
        assertEquals(1500.0, loan1.amount, 0.001)
        assertEquals(500.0, loan1.amountReceived, 0.001)
        assertEquals(1000.0, loan1.remainingBalance, 0.001)
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
    fun testDateUtils_utcMidnightConversion() {
        // Assume user selects Sept 25, 2026 UTC midnight (1790294400000)
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 25, 0, 0, 0)
        }
        val utcMillis = utcCal.timeInMillis

        // Convert to local
        val localMillis = DateUtils.utcMidnightToLocal(utcMillis)
        val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }

        assertEquals(2026, localCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, localCal.get(Calendar.MONTH))
        assertEquals(25, localCal.get(Calendar.DAY_OF_MONTH))

        // Convert back to UTC midnight
        val backToUtc = DateUtils.localToUtcMidnight(localMillis)
        assertEquals(utcMillis, backToUtc)
    }
}
