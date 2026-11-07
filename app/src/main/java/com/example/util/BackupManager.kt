package com.example.util

import android.util.Base64
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.PayableEntity
import com.example.data.local.entity.PaymentHistoryEntity
import com.example.data.local.entity.ReceivableEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    const val CURRENT_BACKUP_VERSION = 2
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12 // Standard 12-byte nonce for GCM
    private const val MAX_BACKUP_SIZE_BYTES = 20 * 1024 * 1024 // 20 MB

    fun generateBackupJson(
        loans: List<LoanEntity>,
        receivables: List<ReceivableEntity>,
        payables: List<PayableEntity>,
        history: List<PaymentHistoryEntity>,
        password: String? = null
    ): String {
        val root = JSONObject()
        root.put("app", "Controle Financeiro")
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        val now = System.currentTimeMillis()
        root.put("createdAt", now)
        root.put("createdAtFormatted", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(now)))

        val dataObj = JSONObject()

        // Loans
        val loansArray = JSONArray()
        loans.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("clientName", item.clientName.trim())
            obj.put("amount", item.amount)
            obj.put("interestRate", item.interestRate)
            obj.put("interestType", item.interestType)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("amountReceived", item.amountReceived)
            obj.put("notes", item.notes.trim())
            obj.put("settledUnderLegacyRule", item.settledUnderLegacyRule)
            loansArray.put(obj)
        }
        dataObj.put("loans", loansArray)

        // Receivables
        val recArray = JSONArray()
        receivables.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("clientName", item.clientName.trim())
            obj.put("category", item.category)
            obj.put("description", item.description.trim())
            obj.put("originalAmount", item.originalAmount)
            obj.put("amountReceived", item.amountReceived)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("notes", item.notes.trim())
            recArray.put(obj)
        }
        dataObj.put("receivables", recArray)

        // Payables
        val payArray = JSONArray()
        payables.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("creditorName", item.creditorName.trim())
            obj.put("category", item.category)
            obj.put("description", item.description.trim())
            obj.put("originalAmount", item.originalAmount)
            obj.put("amountPaid", item.amountPaid)
            obj.put("createdAt", item.createdAt)
            obj.put("dueDate", if (item.dueDate != null) item.dueDate else JSONObject.NULL)
            obj.put("hasNoDueDate", item.hasNoDueDate)
            obj.put("notes", item.notes.trim())
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
            obj.put("note", item.note.trim())
            histArray.put(obj)
        }
        dataObj.put("history", histArray)

        // If password is provided and not blank, encrypt the payload with AES-256-GCM
        if (!password.isNullOrBlank()) {
            val plainJsonString = dataObj.toString()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            val iv = ByteArray(IV_LENGTH_BYTES)
            val random = SecureRandom()
            random.nextBytes(salt)
            random.nextBytes(iv)

            val key = deriveKey(password.toCharArray(), salt, PBKDF2_ITERATIONS)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
            val ciphertext = cipher.doFinal(plainJsonString.toByteArray(Charsets.UTF_8))

            root.put("encrypted", true)
            root.put("kdf", KDF_ALGORITHM)
            root.put("iterations", PBKDF2_ITERATIONS)
            root.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            root.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            root.put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        } else {
            root.put("encrypted", false)
            root.put("data", dataObj)
            // Duplicate top-level keys for backwards-compatibility with v1 parsers
            root.put("loans", loansArray)
            root.put("receivables", recArray)
            root.put("payables", payArray)
            root.put("history", histArray)
        }

        return root.toString(2)
    }

    fun isBackupEncrypted(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            root.optBoolean("encrypted", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun decryptBackup(encryptedJsonStr: String, password: String): String {
        if (encryptedJsonStr.length > MAX_BACKUP_SIZE_BYTES) {
            throw IllegalArgumentException("Arquivo de backup excede o tamanho máximo de segurança (20MB).")
        }

        val root = JSONObject(encryptedJsonStr)
        if (!root.optBoolean("encrypted", false)) {
            return encryptedJsonStr
        }

        val salt = Base64.decode(root.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(root.getString("iv"), Base64.DEFAULT)
        val iterations = root.optInt("iterations", PBKDF2_ITERATIONS)
        val ciphertext = Base64.decode(root.getString("ciphertext"), Base64.DEFAULT)

        try {
            val key = deriveKey(password.toCharArray(), salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw SecurityException("Senha incorreta ou integridade do arquivo violada: ${e.message}", e)
        }
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
        if (jsonString.length > MAX_BACKUP_SIZE_BYTES) {
            throw IllegalArgumentException("Arquivo de backup excede o limite máximo permitido de 20MB.")
        }

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
                val clientName = obj.optString("clientName", "").trim()
                val amount = obj.optDouble("amount", 0.0)
                if (amount.isNaN() || amount.isInfinite() || amount < 0) {
                    throw IllegalArgumentException("Valor de empréstimo inválido no registro $i.")
                }
                val amountReceived = obj.optDouble("amountReceived", 0.0)
                val settledUnderLegacyRule = if (obj.has("settledUnderLegacyRule")) {
                    obj.optBoolean("settledUnderLegacyRule", false)
                } else {
                    amountReceived >= amount
                }
                loans.add(
                    LoanEntity(
                        id = obj.optLong("id", 0),
                        clientName = clientName.take(200),
                        amount = amount,
                        interestRate = obj.optDouble("interestRate", 0.0),
                        interestType = obj.optString("interestType", "A.M."),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        amountReceived = amountReceived,
                        notes = obj.optString("notes", "").take(500),
                        settledUnderLegacyRule = settledUnderLegacyRule
                    )
                )
            }
        }

        val receivables = mutableListOf<ReceivableEntity>()
        if (dataSource.has("receivables")) {
            val arr = dataSource.getJSONArray("receivables")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val originalAmount = obj.optDouble("originalAmount", 0.0)
                if (originalAmount.isNaN() || originalAmount.isInfinite() || originalAmount < 0) {
                    throw IllegalArgumentException("Valor de recebível inválido no registro $i.")
                }
                receivables.add(
                    ReceivableEntity(
                        id = obj.optLong("id", 0),
                        clientName = obj.optString("clientName", "").trim().take(200),
                        category = obj.optString("category", "Venda"),
                        description = obj.optString("description", "").trim().take(200),
                        originalAmount = originalAmount,
                        amountReceived = obj.optDouble("amountReceived", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        notes = obj.optString("notes", "").trim().take(500)
                    )
                )
            }
        }

        val payables = mutableListOf<PayableEntity>()
        if (dataSource.has("payables")) {
            val arr = dataSource.getJSONArray("payables")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val originalAmount = obj.optDouble("originalAmount", 0.0)
                if (originalAmount.isNaN() || originalAmount.isInfinite() || originalAmount < 0) {
                    throw IllegalArgumentException("Valor de dívida inválido no registro $i.")
                }
                payables.add(
                    PayableEntity(
                        id = obj.optLong("id", 0),
                        creditorName = obj.optString("creditorName", "").trim().take(200),
                        category = obj.optString("category", "Fornecedor"),
                        description = obj.optString("description", "").trim().take(200),
                        originalAmount = originalAmount,
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                        hasNoDueDate = obj.optBoolean("hasNoDueDate", false),
                        notes = obj.optString("notes", "").trim().take(500)
                    )
                )
            }
        }

        val history = mutableListOf<PaymentHistoryEntity>()
        if (dataSource.has("history")) {
            val arr = dataSource.getJSONArray("history")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val amountPaid = obj.optDouble("amountPaid", 0.0)
                if (amountPaid.isNaN() || amountPaid.isInfinite() || amountPaid < 0) {
                    throw IllegalArgumentException("Valor de pagamento inválido no histórico $i.")
                }
                history.add(
                    PaymentHistoryEntity(
                        id = obj.optLong("id", 0),
                        itemType = obj.optString("itemType", "LOAN"),
                        itemId = obj.optLong("itemId", 0),
                        amountPaid = amountPaid,
                        paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                        note = obj.optString("note", "").trim().take(500)
                    )
                )
            }
        }

        return ParsedBackup(loans, receivables, payables, history, version, createdAt)
    }
}
