package com.example.util

import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    const val CURRENT_BACKUP_VERSION = 1

    fun generateBackupJson(
        loans: List<LoanEntity>,
        receivables: List<ReceivableEntity>,
        payables: List<PayableEntity>,
        history: List<PaymentHistoryEntity>
    ): String {
        val root = JSONObject()
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        root.put("app", "Controle Financeiro")
        val now = System.currentTimeMillis()
        root.put("createdAt", now)
        root.put("createdAtFormatted", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(now)))

        val dataObj = JSONObject()

        // Loans
        val loansArray = JSONArray()
        loans.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("clientName", item.clientName)
            obj.put("amount", item.amount)
            obj.put("interestRate", item.interestRate)
            obj.put("interestType", item.interestType)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("amountReceived", item.amountReceived)
            obj.put("notes", item.notes)
            loansArray.put(obj)
        }
        dataObj.put("loans", loansArray)

        // Receivables
        val recArray = JSONArray()
        receivables.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("clientName", item.clientName)
            obj.put("category", item.category)
            obj.put("description", item.description)
            obj.put("originalAmount", item.originalAmount)
            obj.put("amountReceived", item.amountReceived)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("notes", item.notes)
            recArray.put(obj)
        }
        dataObj.put("receivables", recArray)

        // Payables
        val payArray = JSONArray()
        payables.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("creditorName", item.creditorName)
            obj.put("category", item.category)
            obj.put("description", item.description)
            obj.put("originalAmount", item.originalAmount)
            obj.put("amountPaid", item.amountPaid)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("notes", item.notes)
            payArray.put(obj)
        }
        dataObj.put("payables", payArray)

        // History
        val histArray = JSONArray()
        history.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("itemType", item.itemType)
            obj.put("itemId", item.itemId)
            obj.put("amountPaid", item.amountPaid)
            obj.put("paymentDate", item.paymentDate)
            obj.put("note", item.note)
            histArray.put(obj)
        }
        dataObj.put("history", histArray)

        root.put("data", dataObj)

        // Also duplicate at root level for backward compatibility
        root.put("loans", loansArray)
        root.put("receivables", recArray)
        root.put("payables", payArray)
        root.put("history", histArray)

        return root.toString(2)
    }

    data class ParsedBackup(
        val loans: List<LoanEntity>,
        val receivables: List<ReceivableEntity>,
        val payables: List<PayableEntity>,
        val history: List<PaymentHistoryEntity>,
        val version: Int = 1,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun parseBackupJson(jsonString: String): ParsedBackup {
        val root = JSONObject(jsonString)
        val version = root.optInt("backupVersion", root.optInt("version", 1))
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())

        val dataSource = if (root.has("data") && !root.isNull("data")) {
            root.getJSONObject("data")
        } else {
            root
        }

        val loans = mutableListOf<LoanEntity>()
        if (dataSource.has("loans")) {
            val arr = dataSource.getJSONArray("loans")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                loans.add(
                    LoanEntity(
                        id = obj.optLong("id", 0),
                        clientName = obj.optString("clientName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        interestRate = obj.optDouble("interestRate", 0.0),
                        interestType = obj.optString("interestType", "A.M."),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        amountReceived = obj.optDouble("amountReceived", 0.0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val receivables = mutableListOf<ReceivableEntity>()
        if (dataSource.has("receivables")) {
            val arr = dataSource.getJSONArray("receivables")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                receivables.add(
                    ReceivableEntity(
                        id = obj.optLong("id", 0),
                        clientName = obj.optString("clientName", ""),
                        category = obj.optString("category", "Venda"),
                        description = obj.optString("description", ""),
                        originalAmount = obj.optDouble("originalAmount", 0.0),
                        amountReceived = obj.optDouble("amountReceived", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val payables = mutableListOf<PayableEntity>()
        if (dataSource.has("payables")) {
            val arr = dataSource.getJSONArray("payables")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                payables.add(
                    PayableEntity(
                        id = obj.optLong("id", 0),
                        creditorName = obj.optString("creditorName", ""),
                        category = obj.optString("category", "Fornecedor"),
                        description = obj.optString("description", ""),
                        originalAmount = obj.optDouble("originalAmount", 0.0),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val history = mutableListOf<PaymentHistoryEntity>()
        if (dataSource.has("history")) {
            val arr = dataSource.getJSONArray("history")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                history.add(
                    PaymentHistoryEntity(
                        id = obj.optLong("id", 0),
                        itemType = obj.optString("itemType", "LOAN"),
                        itemId = obj.optLong("itemId", 0),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }
        }

        return ParsedBackup(loans, receivables, payables, history, version, createdAt)
    }
}
