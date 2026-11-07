package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ExpectedTeal
import com.example.ui.theme.OverdueRed
import com.example.ui.theme.PayableRose
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.util.BackupManager
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    PROFILE(
        title = "Perfil",
        description = "Identificação pessoal, carteira e preferências",
        icon = Icons.Default.Person
    ),
    BACKUP(
        title = "Backup e Restauração",
        description = "Exportar, importar e histórico de cópias",
        icon = Icons.Default.Backup
    ),
    SECURITY(
        title = "Segurança e Privacidade",
        description = "Bloqueio por PIN, ocultação de saldos e LGPD",
        icon = Icons.Default.Security
    ),
    NOTIFICATIONS(
        title = "Notificações",
        description = "Lembretes diários de vencimentos (08:00)",
        icon = Icons.Default.NotificationsActive
    ),
    PREFERENCES(
        title = "Preferências",
        description = "Tema do aplicativo, moeda e exibição",
        icon = Icons.Default.Tune
    ),
    DATA_STORAGE(
        title = "Dados e Armazenamento",
        description = "Métricas do banco, diagnóstico e zona de perigo",
        icon = Icons.Default.Storage
    ),
    ABOUT(
        title = "Sobre o Aplicativo",
        description = "Versão 1.1 (v2) e arquitetura 100% offline",
        icon = Icons.Default.Info
    )
}

@Composable
fun SettingsScreen(
    financialViewModel: FinancialViewModel,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Navigation state
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // Intercept back button when inside a subcategory
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    // State from ThemeViewModel
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val lastBackupTime by themeViewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastExportedTime by themeViewModel.lastExportedTime.collectAsStateWithLifecycle()
    val isPrivacyMode by themeViewModel.isPrivacyMode.collectAsStateWithLifecycle()
    val isPinConfigured by themeViewModel.isPinConfigured.collectAsStateWithLifecycle()
    val skipDriveNotice by themeViewModel.skipDriveNotice.collectAsStateWithLifecycle()

    val userName by themeViewModel.userName.collectAsStateWithLifecycle()
    val userNickname by themeViewModel.userNickname.collectAsStateWithLifecycle()
    val walletName by themeViewModel.walletName.collectAsStateWithLifecycle()
    val defaultCurrency by themeViewModel.defaultCurrency.collectAsStateWithLifecycle()
    val dateFormat by themeViewModel.dateFormat.collectAsStateWithLifecycle()
    val appStartDate by themeViewModel.appStartDate.collectAsStateWithLifecycle()
    val notifyPayables by themeViewModel.notifyPayables.collectAsStateWithLifecycle()
    val notifyReceivables by themeViewModel.notifyReceivables.collectAsStateWithLifecycle()
    val notificationTime by themeViewModel.notificationTime.collectAsStateWithLifecycle()

    // State from FinancialViewModel
    val backupRecords by financialViewModel.backupRecords.collectAsStateWithLifecycle()
    val rawLoans by financialViewModel.rawLoans.collectAsStateWithLifecycle()
    val rawReceivables by financialViewModel.rawReceivables.collectAsStateWithLifecycle()
    val rawPayables by financialViewModel.rawPayables.collectAsStateWithLifecycle()
    val rawHistory by financialViewModel.rawHistory.collectAsStateWithLifecycle()

    // Calculated real metrics
    val totalReceived = remember(rawLoans, rawReceivables) {
        rawLoans.sumOf { it.amountReceived } + rawReceivables.sumOf { it.amountReceived }
    }
    val totalPaid = remember(rawPayables) {
        rawPayables.sumOf { it.amountPaid }
    }
    val usageStartDate = remember(rawLoans, rawReceivables, rawPayables, appStartDate) {
        val dates = mutableListOf<Long>()
        rawLoans.forEach { dates.add(it.createdAt) }
        rawReceivables.forEach { dates.add(it.createdAt) }
        rawPayables.forEach { dates.add(it.createdAt) }
        if (dates.isNotEmpty()) dates.minOrNull() ?: appStartDate else appStartDate
    }

    // Drive Account notice dialog state
    var showDriveNoticeDialog by remember { mutableStateOf(false) }
    var pendingDriveNoticeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dontShowDriveNoticeAgain by remember { mutableStateOf(false) }

    val triggerDriveNoticeOrExecute: (() -> Unit) -> Unit = { action ->
        if (skipDriveNotice) {
            action()
        } else {
            pendingDriveNoticeAction = action
            dontShowDriveNoticeAgain = false
            showDriveNoticeDialog = true
        }
    }

    // Profile Edit Dialog State
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf(userName) }
    var editNicknameInput by remember { mutableStateOf(userNickname) }
    var editWalletInput by remember { mutableStateOf(walletName) }
    var editCurrencyInput by remember { mutableStateOf(defaultCurrency) }
    var editDateFormatInput by remember { mutableStateOf(dateFormat) }

    // Export with Password Dialog state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var exportUseEncryption by remember { mutableStateOf(true) }
    var exportPendingAction by remember { mutableStateOf<String?>(null) } // "SAVE" or "SHARE"

    // Raw JSON view dialog
    var showExportTextDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    // Text restore dialog
    var showRestoreTextDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Decryption Password Input Dialog
    var showDecryptPasswordDialog by remember { mutableStateOf(false) }
    var decryptPasswordInput by remember { mutableStateOf("") }
    var rawPendingJsonForDecryption by remember { mutableStateOf("") }

    // Restore Confirmation Dialog State
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingDecryptedJsonToRestore by remember { mutableStateOf("") }
    var restoreSummaryMessage by remember { mutableStateOf("") }

    // PIN configuration dialog state
    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf("") }

    // App Diagnostics Dialog State
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Danger Zone Dialog State (Two-step verification)
    var showClearWarningDialog by remember { mutableStateOf(false) }
    var showClearFinalDialog by remember { mutableStateOf(false) }
    var deleteConfirmationInput by remember { mutableStateOf("") }

    // Emergency Safety Restore Dialog State
    var showEmergencyRestoreDialog by remember { mutableStateOf(false) }

    // Last recorded backup ID to mark as EXPORTED
    var lastGeneratedRecordId by remember { mutableStateOf<Long?>(null) }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de notificações concedida!", Toast.LENGTH_SHORT).show()
            coroutineScope.launch(Dispatchers.IO) {
                NotificationHelper.checkAndShowDueNotification(context, isTest = true)
            }
        } else {
            Toast.makeText(context, "Permissão de notificação negada.", Toast.LENGTH_SHORT).show()
        }
    }

    // Export Document Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val password = if (exportUseEncryption && exportPasswordInput.isNotBlank()) exportPasswordInput.trim() else null
                val json = financialViewModel.generateBackupJson(password = password)
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                val now = System.currentTimeMillis()
                themeViewModel.updateLastBackupTime(now)
                themeViewModel.updateLastExportedTime(now)

                lastGeneratedRecordId?.let { recId ->
                    financialViewModel.updateBackupStatus(recId, "EXPORTADO")
                }

                Toast.makeText(context, "Backup exportado com sucesso (.json)! Destino confirmado pelo usuário.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar arquivo de backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Import Document Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                }
                if (!content.isNullOrBlank()) {
                    if (BackupManager.isBackupEncrypted(content)) {
                        rawPendingJsonForDecryption = content
                        decryptPasswordInput = ""
                        showDecryptPasswordDialog = true
                    } else {
                        try {
                            val parsed = BackupManager.parseBackupJson(content)
                            pendingDecryptedJsonToRestore = content
                            restoreSummaryMessage = "Arquivo JSON válido (Não criptografado):\n\n" +
                                    "• ${parsed.loans.size} Empréstimo(s)\n" +
                                    "• ${parsed.receivables.size} Venda(s) / Recebível(is)\n" +
                                    "• ${parsed.payables.size} Dívida(s)\n" +
                                    "• ${parsed.history.size} Histórico(s) de Pagamento\n\n" +
                                    "Atenção: Os dados atuais serão substituídos com segurança transacional."
                            showConfirmRestoreDialog = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "O arquivo selecionado não contém um backup válido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao ler arquivo selecionado: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedCategory == null) {
            // ==========================================
            // CENTRAL DE CONFIGURAÇÕES (HUB PRINCIPAL)
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Profile Presentation Card
                item {
                    UserProfileHeaderCard(
                        userName = userName,
                        userNickname = userNickname,
                        walletName = walletName,
                        defaultCurrency = defaultCurrency,
                        totalReceived = totalReceived,
                        totalPaid = totalPaid,
                        startDate = usageStartDate,
                        onEditProfile = {
                            editNameInput = userName
                            editNicknameInput = userNickname
                            editWalletInput = walletName
                            editCurrencyInput = defaultCurrency
                            editDateFormatInput = dateFormat
                            showEditProfileDialog = true
                        },
                        onOpenProfileCategory = {
                            selectedCategory = SettingsCategory.PROFILE
                        }
                    )
                }

                // Section Divider / Subtitle
                item {
                    Text(
                        text = "Categorias de Ajustes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                    )
                }

                // 7 Independent Category Rows
                items(SettingsCategory.values()) { category ->
                    CategoryMenuItem(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        } else {
            // ==========================================
            // TELA ESPECÍFICA DA CATEGORIA SELECIONADA
            // ==========================================
            val currentCat = selectedCategory!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Subscreen Top Navigation Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedCategory = null },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentCat.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentCat.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = currentCat.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Content specific to selected category
                when (currentCat) {
                    SettingsCategory.PROFILE -> {
                        item {
                            ProfileCategoryContent(
                                userName = userName,
                                userNickname = userNickname,
                                walletName = walletName,
                                defaultCurrency = defaultCurrency,
                                dateFormat = dateFormat,
                                startDate = usageStartDate,
                                totalReceived = totalReceived,
                                totalPaid = totalPaid,
                                onEditClicked = {
                                    editNameInput = userName
                                    editNicknameInput = userNickname
                                    editWalletInput = walletName
                                    editCurrencyInput = defaultCurrency
                                    editDateFormatInput = dateFormat
                                    showEditProfileDialog = true
                                }
                            )
                        }
                    }

                    SettingsCategory.BACKUP -> {
                        item {
                            BackupCategoryContent(
                                lastBackupTime = lastBackupTime,
                                lastExportedTime = lastExportedTime,
                                backupRecords = backupRecords,
                                onSaveFile = {
                                    exportPendingAction = "SAVE"
                                    exportPasswordInput = ""
                                    exportUseEncryption = true
                                    showExportPasswordDialog = true
                                },
                                onShareFile = {
                                    exportPendingAction = "SHARE"
                                    exportPasswordInput = ""
                                    exportUseEncryption = true
                                    showExportPasswordDialog = true
                                },
                                onImportFile = {
                                    triggerDriveNoticeOrExecute {
                                        openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    }
                                },
                                onRestoreText = {
                                    showRestoreTextDialog = true
                                },
                                onViewRawJson = {
                                    exportedJsonText = financialViewModel.generateBackupJson(password = null)
                                    showExportTextDialog = true
                                }
                            )
                        }
                    }

                    SettingsCategory.SECURITY -> {
                        item {
                            SecurityCategoryContent(
                                isPrivacyMode = isPrivacyMode,
                                onTogglePrivacyMode = { themeViewModel.togglePrivacyMode() },
                                isPinConfigured = isPinConfigured,
                                onSetOrChangePin = {
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinDialogError = ""
                                    showPinDialog = true
                                },
                                onRemovePin = {
                                    themeViewModel.removeAppPin()
                                    Toast.makeText(context, "PIN de segurança removido.", Toast.LENGTH_SHORT).show()
                                },
                                onLockNow = {
                                    themeViewModel.lockApp()
                                    Toast.makeText(context, "Aplicativo bloqueado com segurança.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    SettingsCategory.NOTIFICATIONS -> {
                        item {
                            NotificationsCategoryContent(
                                notifyPayables = notifyPayables,
                                onToggleNotifyPayables = { themeViewModel.setNotifyPayables(it) },
                                notifyReceivables = notifyReceivables,
                                onToggleNotifyReceivables = { themeViewModel.setNotifyReceivables(it) },
                                notificationTime = notificationTime,
                                onTestDueNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            NotificationHelper.checkAndShowDueNotification(context, isTest = true)
                                        }
                                    }
                                },
                                onTestBackupNotification = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        NotificationHelper.checkAndNotifyAssistedBackup(context, isTest = true)
                                    }
                                    Toast.makeText(context, "Lembrete de backup assistido gerado!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    SettingsCategory.PREFERENCES -> {
                        item {
                            PreferencesCategoryContent(
                                themeMode = themeMode,
                                onSetThemeMode = { themeViewModel.setThemeMode(it) },
                                defaultCurrency = defaultCurrency,
                                dateFormat = dateFormat
                            )
                        }
                    }

                    SettingsCategory.DATA_STORAGE -> {
                        item {
                            DataStorageCategoryContent(
                                totalLoans = rawLoans.size,
                                totalReceivables = rawReceivables.size,
                                totalPayables = rawPayables.size,
                                totalHistory = rawHistory.size,
                                hasSafetyBackup = financialViewModel.hasSafetyBackup(),
                                onOpenDiagnostics = { showDiagnosticsDialog = true },
                                onClearDatabase = { showClearWarningDialog = true },
                                onEmergencyRestore = { showEmergencyRestoreDialog = true }
                            )
                        }
                    }

                    SettingsCategory.ABOUT -> {
                        item {
                            AboutCategoryContent()
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIÁLOGOS DO SISTEMA
    // ==========================================

    // --- Profile Edit Dialog ---
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Editar Perfil Pessoal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nome de Exibição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_name_input")
                    )

                    OutlinedTextField(
                        value = editNicknameInput,
                        onValueChange = { editNicknameInput = it },
                        label = { Text("Apelido / Cargo") },
                        placeholder = { Text("Ex: Gestor Financeiro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_nickname_input")
                    )

                    OutlinedTextField(
                        value = editWalletInput,
                        onValueChange = { editWalletInput = it },
                        label = { Text("Nome da Carteira") },
                        placeholder = { Text("Ex: Carteira Principal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_wallet_name_input")
                    )

                    OutlinedTextField(
                        value = editCurrencyInput,
                        onValueChange = { editCurrencyInput = it },
                        label = { Text("Moeda Padrão") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDateFormatInput,
                        onValueChange = { editDateFormatInput = it },
                        label = { Text("Formato de Data") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        themeViewModel.updateProfile(
                            name = editNameInput,
                            nickname = editNicknameInput,
                            wallet = editWalletInput,
                            currency = editCurrencyInput,
                            formatDate = editDateFormatInput
                        )
                        showEditProfileDialog = false
                        Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_profile_btn")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Export Password Dialog (AES-256-GCM Configuration) ---
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Configurar Criptografia do Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Recomendamos proteger o backup com senha usando criptografia AES-256-GCM de padrão militar:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exportUseEncryption,
                            onCheckedChange = { exportUseEncryption = it }
                        )
                        Text("Criptografar arquivo com senha", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (exportUseEncryption) {
                        OutlinedTextField(
                            value = exportPasswordInput,
                            onValueChange = { exportPasswordInput = it },
                            label = { Text("Senha de Recuperação") },
                            placeholder = { Text("Digite uma senha forte...") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("export_password_input")
                        )

                        Text(
                            text = "Atenção: Guarde esta senha. Ela será exigida para restaurar o backup em qualquer aparelho.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val password = if (exportUseEncryption && exportPasswordInput.isNotBlank()) exportPasswordInput.trim() else null
                        val json = financialViewModel.generateBackupJson(password = password)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        val fileName = if (password != null) "backup_financeiro_enc_$timestamp.json" else "backup_financeiro_$timestamp.json"

                        financialViewModel.recordBackup(
                            fileName = fileName,
                            fileSize = json.toByteArray(Charsets.UTF_8).size.toLong(),
                            isEncrypted = password != null,
                            status = "GERADO"
                        ) { recId ->
                            lastGeneratedRecordId = recId
                        }

                        showExportPasswordDialog = false

                        when (exportPendingAction) {
                            "SAVE" -> {
                                triggerDriveNoticeOrExecute {
                                    createDocumentLauncher.launch(fileName)
                                }
                            }
                            "SHARE" -> {
                                shareBackupFile(context, json, fileName)
                                lastGeneratedRecordId?.let { recId ->
                                    financialViewModel.updateBackupStatus(recId, "EXPORTADO")
                                    themeViewModel.updateLastExportedTime(System.currentTimeMillis())
                                }
                            }
                        }
                    },
                    enabled = !exportUseEncryption || exportPasswordInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_export_proceed_btn")
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Google Drive / Account Picker Guidance Dialog ---
    if (showDriveNoticeDialog) {
        AlertDialog(
            onDismissRequest = {
                showDriveNoticeDialog = false
                pendingDriveNoticeAction = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Escolha de Conta / Local",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "No próximo passo, toque no menu ☰ do seletor e escolha a conta do Google Drive onde está (ou onde quer guardar) o arquivo de backup.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowDriveNoticeAgain = !dontShowDriveNoticeAgain }
                    ) {
                        Checkbox(
                            checked = dontShowDriveNoticeAgain,
                            onCheckedChange = { dontShowDriveNoticeAgain = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Não mostrar novamente",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dontShowDriveNoticeAgain) {
                            themeViewModel.setSkipDriveNotice(true)
                        }
                        showDriveNoticeDialog = false
                        val act = pendingDriveNoticeAction
                        pendingDriveNoticeAction = null
                        act?.invoke()
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDriveNoticeDialog = false
                        pendingDriveNoticeAction = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Decryption Password Dialog ---
    if (showDecryptPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showDecryptPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ExpectedTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Protegido por Senha")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Este arquivo de backup está criptografado (AES-256-GCM). Digite a senha utilizada no momento da exportação:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = decryptPasswordInput,
                        onValueChange = { decryptPasswordInput = it },
                        label = { Text("Senha do Backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("decrypt_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val decrypted = BackupManager.decryptBackup(rawPendingJsonForDecryption, decryptPasswordInput.trim())
                            val parsed = BackupManager.parseBackupJson(decrypted)
                            pendingDecryptedJsonToRestore = decrypted
                            showDecryptPasswordDialog = false
                            restoreSummaryMessage = "Backup Descriptografado com Sucesso (AES-256-GCM):\n\n" +
                                    "• ${parsed.loans.size} Empréstimo(s)\n" +
                                    "• ${parsed.receivables.size} Venda(s) / Recebível(is)\n" +
                                    "• ${parsed.payables.size} Dívida(s)\n" +
                                    "• ${parsed.history.size} Histórico(s) de Pagamento\n\n" +
                                    "Atenção: Os dados atuais serão substituídos com segurança transacional."
                            showConfirmRestoreDialog = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "Senha incorreta ou integridade do arquivo violada: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = decryptPasswordInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_decrypt_btn")
                ) {
                    Text("Descriptografar e Validar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecryptPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Restore Confirmation Dialog (Transactional Rollback Safe) ---
    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmRestoreDialog = false
                pendingDecryptedJsonToRestore = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Restauração")
                }
            },
            text = {
                Text(
                    text = restoreSummaryMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        financialViewModel.restoreBackupTransactional(
                            jsonString = pendingDecryptedJsonToRestore,
                            password = null,
                            onSuccess = {
                                themeViewModel.updateLastBackupTime(System.currentTimeMillis())
                                Toast.makeText(context, "Backup restaurado com sucesso! Integridade verificada.", Toast.LENGTH_LONG).show()
                                showConfirmRestoreDialog = false
                                pendingDecryptedJsonToRestore = ""
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erro na restauração: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_file_restore_btn")
                ) {
                    Text("Restaurar Agora")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmRestoreDialog = false
                    pendingDecryptedJsonToRestore = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Configure PIN Dialog ---
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configurar PIN de Segurança") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Defina um PIN numérico (4 a 6 dígitos):", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("Novo PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { confirmPinInput = it },
                        label = { Text("Confirme o PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )

                    if (pinDialogError.isNotBlank()) {
                        Text(text = pinDialogError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinDialogError = "O PIN deve conter no mínimo 4 dígitos."
                        } else if (newPinInput != confirmPinInput) {
                            pinDialogError = "Os PINs digitados não coincidem."
                        } else {
                            themeViewModel.setAppPin(newPinInput)
                            Toast.makeText(context, "PIN de segurança configurado com sucesso!", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_pin_btn")
                ) {
                    Text("Salvar PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // --- App Diagnostics Dialog ---
    if (showDiagnosticsDialog) {
        val diag = financialViewModel.getDiagnostics()
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnóstico Técnico do BD")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Banco de Dados: Room SQLite v2", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Status da Conexão: Ativo & Íntegro", color = ExpectedTeal, style = MaterialTheme.typography.labelSmall)
                            Text("Espaço em Disco: ${String.format("%.2f KB", diag.dbFileSizeBytes / 1024.0)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Registros Cadastrados:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• Empréstimos: ${diag.totalLoans}", style = MaterialTheme.typography.labelSmall)
                            Text("• Vendas / Recebíveis: ${diag.totalReceivables}", style = MaterialTheme.typography.labelSmall)
                            Text("• Dívidas a Pagar: ${diag.totalPayables}", style = MaterialTheme.typography.labelSmall)
                            Text("• Histórico de Movimentações: ${diag.totalHistory}", style = MaterialTheme.typography.labelSmall)
                            Text("• Histórico de Backups: ${diag.totalBackupsRecorded}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Rotinas e Segurança:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• Criptografia: AES-256-GCM Nativo", style = MaterialTheme.typography.labelSmall)
                            Text("• Canal Diário 08:00: Ativo", style = MaterialTheme.typography.labelSmall)
                            Text("• Arquitetura: 100% Offline (Sem Servidor)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDiagnosticsDialog = false }) { Text("Fechar") }
            }
        )
    }

    // --- View / Copy Raw JSON Modal ---
    if (showExportTextDialog) {
        AlertDialog(
            onDismissRequest = { showExportTextDialog = false },
            title = { Text("Conteúdo do Backup JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Conteúdo JSON válido formatado:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    shareBackupFile(context, exportedJsonText, "backup_financeiro.json")
                    showExportTextDialog = false
                }) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compartilhar Arquivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportTextDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // --- Restore via Text Dialog ---
    if (showRestoreTextDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreTextDialog = false },
            title = { Text("Restaurar via Texto JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cole abaixo o texto JSON exportado:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("restore_json_input"),
                        placeholder = { Text("Cole o JSON aqui...") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = restoreJsonText.trim()
                        if (input.isNotBlank()) {
                            if (BackupManager.isBackupEncrypted(input)) {
                                rawPendingJsonForDecryption = input
                                showRestoreTextDialog = false
                                decryptPasswordInput = ""
                                showDecryptPasswordDialog = true
                            } else {
                                try {
                                    val parsed = BackupManager.parseBackupJson(input)
                                    pendingDecryptedJsonToRestore = input
                                    restoreSummaryMessage = "JSON válido (Não criptografado):\n\n" +
                                            "• ${parsed.loans.size} Empréstimo(s)\n" +
                                            "• ${parsed.receivables.size} Venda(s)\n" +
                                            "• ${parsed.payables.size} Dívida(s)\n" +
                                            "• ${parsed.history.size} Histórico(s)\n\n" +
                                            "Deseja substituir os dados atuais com segurança transacional?"
                                    showRestoreTextDialog = false
                                    showConfirmRestoreDialog = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "JSON inválido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_btn"),
                    enabled = restoreJsonText.isNotBlank()
                ) {
                    Text("Validar e Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreTextDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Danger Zone Step 1: Warning Dialog ---
    if (showClearWarningDialog) {
        AlertDialog(
            onDismissRequest = { showClearWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = OverdueRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Atenção: Ação Crítica")
                }
            },
            text = {
                Text(
                    text = "Você está prestes a excluir TODOS os empréstimos, recebimentos, dívidas e históricos salvos.\n\n" +
                            "Por segurança, o aplicativo criará automaticamente uma cópia de segurança prévia no aparelho antes de apagar.\n\n" +
                            "Deseja avançar para a confirmação final?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearWarningDialog = false
                        deleteConfirmationInput = ""
                        showClearFinalDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                    modifier = Modifier.testTag("btn_step1_clear_db")
                ) {
                    Text("Continuar para Confirmação...")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWarningDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Danger Zone Step 2: Final Verification Dialog ---
    if (showClearFinalDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearFinalDialog = false
                deleteConfirmationInput = ""
            },
            title = { Text("Confirmação Final de Exclusão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Para confirmar que deseja apagar absolutamente tudo, digite a palavra APAGAR no campo abaixo e confirme:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = deleteConfirmationInput,
                        onValueChange = { deleteConfirmationInput = it },
                        label = { Text("Digite APAGAR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("delete_confirmation_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        financialViewModel.clearAllDatabase {
                            Toast.makeText(context, "Banco de dados limpo com sucesso. Cópia de segurança prévia preservada.", Toast.LENGTH_LONG).show()
                            showClearFinalDialog = false
                            deleteConfirmationInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                    enabled = deleteConfirmationInput.trim().equals("APAGAR", ignoreCase = true),
                    modifier = Modifier.testTag("confirm_clear_db_btn")
                ) {
                    Text("Excluir Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearFinalDialog = false
                    deleteConfirmationInput = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Emergency Pre-Wipe Restore Dialog ---
    if (showEmergencyRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = ExpectedTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Cópia Pré-Limpeza")
                }
            },
            text = {
                Text("Deseja restaurar a cópia de segurança automática criada antes da última limpeza do banco de dados?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        financialViewModel.restoreSafetyBackup(
                            onSuccess = {
                                Toast.makeText(context, "Cópia de segurança pré-limpeza restaurada com sucesso!", Toast.LENGTH_LONG).show()
                                showEmergencyRestoreDialog = false
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Restaurar Cópia")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyRestoreDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ==========================================
// COMPONENTES DA TELA PRINCIPAL DE AJUSTES
// ==========================================

@Composable
private fun UserProfileHeaderCard(
    userName: String,
    userNickname: String,
    walletName: String,
    defaultCurrency: String,
    totalReceived: Double,
    totalPaid: Double,
    startDate: Long,
    onEditProfile: () -> Unit,
    onOpenProfileCategory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProfileCategory() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile presentation top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar circle with initials
                    val initials = remember(userName) {
                        userName.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercaseChar() }
                            .joinToString("")
                            .ifBlank { "CF" }
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = userNickname,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = walletName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("btn_edit_profile_header")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Perfil",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Real stats row from database
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stat 1: Total Recebido
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = ExpectedTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Total Recebido",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(totalReceived),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpectedTeal,
                        maxLines = 1
                    )
                }

                // Stat 2: Total Pago
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = PayableRose,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Total Pago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(totalPaid),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PayableRose,
                        maxLines = 1
                    )
                }

                // Stat 3: Início de uso
                Column(modifier = Modifier.weight(0.9f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Início do Uso",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = DateUtils.formatDate(startDate),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMenuItem(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_item_${category.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 13.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// CONTEÚDO DAS SUBTELAS INDEPENDENTES
// ==========================================

@Composable
private fun ProfileCategoryContent(
    userName: String,
    userNickname: String,
    walletName: String,
    defaultCurrency: String,
    dateFormat: String,
    startDate: Long,
    totalReceived: Double,
    totalPaid: Double,
    onEditClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Detailed Card
        SettingsCard(
            title = "Dados da Conta e Carteira",
            icon = Icons.Default.Person
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileInfoRow(label = "Nome de Exibição", value = userName)
                ProfileInfoRow(label = "Apelido / Cargo", value = userNickname)
                ProfileInfoRow(label = "Carteira Financeira", value = walletName)
                ProfileInfoRow(label = "Moeda Padrão", value = defaultCurrency)
                ProfileInfoRow(label = "Formato de Data", value = dateFormat)
                ProfileInfoRow(label = "Data de Início", value = DateUtils.formatDate(startDate))

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onEditClicked,
                    modifier = Modifier.fillMaxWidth().testTag("btn_edit_profile_subscreen")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar Informações do Perfil")
                }
            }
        }

        // Financial Consolidated Summary Card
        SettingsCard(
            title = "Resumo Consolidado do Perfil",
            icon = Icons.Default.AccountBalanceWallet,
            iconColor = ExpectedTeal
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Concretizado Recebido:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyFormatter.format(totalReceived),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ExpectedTeal
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Concretizado Pago:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        CurrencyFormatter.format(totalPaid),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PayableRose
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val netBalance = totalReceived - totalPaid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Saldo Real Consolidado:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        CurrencyFormatter.format(netBalance),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netBalance >= 0) ExpectedTeal else PayableRose
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun BackupCategoryContent(
    lastBackupTime: Long,
    lastExportedTime: Long,
    backupRecords: List<com.example.data.local.entity.BackupRecordEntity>,
    onSaveFile: () -> Unit,
    onShareFile: () -> Unit,
    onImportFile: () -> Unit,
    onRestoreText: () -> Unit,
    onViewRawJson: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Backup Actions Card
        SettingsCard(
            title = "Exportação e Cópia de Segurança",
            icon = Icons.Default.Backup
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (lastBackupTime > 0) {
                            "Último backup gerado em: ${DateUtils.formatDateTime(lastBackupTime)}"
                        } else {
                            "Nenhum backup registrado até o momento."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (lastExportedTime > 0) {
                        Text(
                            text = "Última exportação externa: ${DateUtils.formatDateTime(lastExportedTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpectedTeal
                        )
                    }
                }

                Text(
                    text = "Exportação de Arquivo JSON (com ou sem senha AES-256-GCM):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSaveFile,
                        modifier = Modifier.weight(1f).testTag("btn_export_backup_file")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar .json", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShareFile,
                        modifier = Modifier.weight(1f).testTag("btn_share_backup_file")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "Restauração Segura e Transacional:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportFile,
                        modifier = Modifier.weight(1f).testTag("btn_import_backup_file")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Selecionar .json", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRestoreText,
                        modifier = Modifier.weight(1f).testTag("btn_restore_text")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Colar Texto", fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = onViewRawJson,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver código JSON não criptografado", fontSize = 12.sp)
                }
            }
        }

        // Backup History Card
        SettingsCard(
            title = "Histórico de Auditoria de Backups",
            icon = Icons.Default.History,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Registro local de integridade com status de geração e exportação confirmada:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (backupRecords.isEmpty()) {
                    Text(
                        text = "Nenhum histórico de backup registrado até o momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        backupRecords.take(5).forEach { record ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.fileName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${DateUtils.formatDateTime(record.timestamp)} • ${String.format("%.1f KB", record.fileSize / 1024.0)} • v${record.backupVersion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = when (record.status) {
                                            "EXPORTADO" -> ExpectedTeal.copy(alpha = 0.2f)
                                            "GERADO" -> MaterialTheme.colorScheme.primaryContainer
                                            else -> OverdueRed.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (record.isEncrypted) "${record.status} (AES)" else record.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (record.status) {
                                                "EXPORTADO" -> ExpectedTeal
                                                "GERADO" -> MaterialTheme.colorScheme.primary
                                                else -> OverdueRed
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCategoryContent(
    isPrivacyMode: Boolean,
    onTogglePrivacyMode: () -> Unit,
    isPinConfigured: Boolean,
    onSetOrChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onLockNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsCard(
            title = "Privacidade e Bloqueio",
            icon = Icons.Default.Security,
            iconColor = ExpectedTeal
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Privacy Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ocultar Valores na Tela (Modo Privacidade)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Mascara saldos no dashboard (exibe R$ ••••••). Pode ser alternado pelo ícone de olho no cabeçalho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrivacyMode,
                        onCheckedChange = { onTogglePrivacyMode() },
                        modifier = Modifier.testTag("switch_privacy_mode")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // PIN Lock
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Bloqueio do Aplicativo por PIN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (isPinConfigured) "Proteção por PIN ativa. O aplicativo exige autenticação ao abrir."
                        else "Defina um PIN numérico para proteger o acesso aos seus dados financeiros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPinConfigured) {
                        Button(
                            onClick = onSetOrChangePin,
                            modifier = Modifier.weight(1f).testTag("btn_change_pin")
                        ) {
                            Text("Alterar PIN", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onRemovePin,
                            modifier = Modifier.weight(1f).testTag("btn_remove_pin")
                        ) {
                            Text("Remover PIN", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onSetOrChangePin,
                            modifier = Modifier.fillMaxWidth().testTag("btn_set_pin")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ativar Proteção por PIN")
                        }
                    }
                }

                if (isPinConfigured) {
                    OutlinedButton(
                        onClick = onLockNow,
                        modifier = Modifier.fillMaxWidth().testTag("btn_lock_app_now")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bloquear Aplicativo Agora")
                    }
                }
            }
        }

        // Compliance & Security Information
        SettingsCard(
            title = "Proteção de Dados & LGPD",
            icon = Icons.Default.CheckCircle,
            iconColor = ExpectedTeal
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• Armazenamento 100% Offline: Nenhum dado financeiro sai do seu celular.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Criptografia AES-256-GCM: Arquivos de exportação podem ser protegidos com chave militar.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Sem Servidores ou Rastreadores: Privacidade absoluta conforme a LGPD.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NotificationsCategoryContent(
    notifyPayables: Boolean,
    onToggleNotifyPayables: (Boolean) -> Unit,
    notifyReceivables: Boolean,
    onToggleNotifyReceivables: (Boolean) -> Unit,
    notificationTime: String,
    onTestDueNotification: () -> Unit,
    onTestBackupNotification: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsCard(
            title = "Lembretes Diários de Vencimentos",
            icon = Icons.Default.NotificationsActive,
            iconColor = MaterialTheme.colorScheme.primary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Lembretes programados diariamente às $notificationTime sobre contas a pagar, empréstimos e recebimentos que vencem no dia ou estão em atraso (ignora registros quitados).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle Payables
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lembrete de Contas a Pagar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Avisa sobre contas que vencem hoje ou estão atrasadas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyPayables,
                        onCheckedChange = onToggleNotifyPayables
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Toggle Receivables
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lembrete de Recebimentos & Empréstimos",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Avisa sobre valores e vendas a receber com prazo vencendo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyReceivables,
                        onCheckedChange = onToggleNotifyReceivables
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Testes Rápidos de Alertas:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestDueNotification,
                        modifier = Modifier.weight(1f).testTag("btn_test_notification")
                    ) {
                        Text("Testar Notificação (08h)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onTestBackupNotification,
                        modifier = Modifier.weight(1f).testTag("btn_test_backup_notification")
                    ) {
                        Text("Aviso Backup Pronto", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferencesCategoryContent(
    themeMode: String,
    onSetThemeMode: (String) -> Unit,
    defaultCurrency: String,
    dateFormat: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Theme Selector
        SettingsCard(
            title = "Aparência e Tema",
            icon = Icons.Default.Brightness4
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = themeMode == "DARK",
                        onClick = { onSetThemeMode("DARK") },
                        modifier = Modifier.testTag("theme_dark_radio")
                    )
                    Text("Modo Escuro (Recomendado / Fintech style)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = themeMode == "LIGHT",
                        onClick = { onSetThemeMode("LIGHT") },
                        modifier = Modifier.testTag("theme_light_radio")
                    )
                    Text("Modo Claro")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = themeMode == "SYSTEM",
                        onClick = { onSetThemeMode("SYSTEM") },
                        modifier = Modifier.testTag("theme_system_radio")
                    )
                    Text("Seguir Padrão do Sistema")
                }
            }
        }

        // Regional and Format Preferences
        SettingsCard(
            title = "Padrões Regionais e Exibição",
            icon = Icons.Default.Tune
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileInfoRow(label = "Moeda Padrão de Exibição", value = defaultCurrency)
                ProfileInfoRow(label = "Formato de Datas", value = dateFormat)
                ProfileInfoRow(label = "Idioma", value = "Português (Brasil)")
            }
        }
    }
}

@Composable
private fun DataStorageCategoryContent(
    totalLoans: Int,
    totalReceivables: Int,
    totalPayables: Int,
    totalHistory: Int,
    hasSafetyBackup: Boolean,
    onOpenDiagnostics: () -> Unit,
    onClearDatabase: () -> Unit,
    onEmergencyRestore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Database Metrics
        SettingsCard(
            title = "Métricas do Banco de Dados",
            icon = Icons.Default.Storage
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileInfoRow(label = "Empréstimos Cadastrados", value = "$totalLoans registros")
                ProfileInfoRow(label = "Vendas / Recebíveis", value = "$totalReceivables registros")
                ProfileInfoRow(label = "Dívidas a Pagar", value = "$totalPayables registros")
                ProfileInfoRow(label = "Histórico de Transações", value = "$totalHistory movimentações")

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_diagnostics")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Executar Diagnóstico Técnico")
                }
            }
        }

        // Danger Zone
        SettingsCard(
            title = "Zona de Perigo / Recuperação",
            icon = Icons.Default.DeleteForever,
            iconColor = PayableRose
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Exclusão total com criação automática de backup de segurança pré-limpeza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasSafetyBackup) {
                    OutlinedButton(
                        onClick = onEmergencyRestore,
                        modifier = Modifier.fillMaxWidth().testTag("btn_emergency_restore")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = ExpectedTeal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurar Cópia de Emergência Pré-Limpeza", color = ExpectedTeal)
                    }
                }

                Button(
                    onClick = onClearDatabase,
                    colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                    modifier = Modifier.fillMaxWidth().testTag("btn_clear_db")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpar Banco de Dados (2 Etapas)")
                }
            }
        }
    }
}

@Composable
private fun AboutCategoryContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsCard(
            title = "Sobre o Aplicativo",
            icon = Icons.Default.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Controle Financeiro Comercial",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Versão: 1.1 (v2 - Room SQLite com Migração Segura)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Arquitetura 100% Offline: Nenhum dado trafega por servidores externos. Todos os registros permanecem salvos com segurança criptográfica unicamente no seu dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Padrão de Segurança: Criptografia AES-256-GCM para exportações e backups.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpectedTeal
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

private fun shareBackupFile(context: Context, jsonText: String, fileName: String = "backup_financeiro.json") {
    try {
        val file = File(context.cacheDir, fileName)
        file.writeText(jsonText, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup Controle Financeiro (.json)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartilhar Backup (.json)")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao compartilhar arquivo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
