package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    financialViewModel: FinancialViewModel,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val lastBackupTime by themeViewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastExportedTime by themeViewModel.lastExportedTime.collectAsStateWithLifecycle()
    val isPrivacyMode by themeViewModel.isPrivacyMode.collectAsStateWithLifecycle()
    val isPinConfigured by themeViewModel.isPinConfigured.collectAsStateWithLifecycle()
    val backupRecords by financialViewModel.backupRecords.collectAsStateWithLifecycle()
    val skipDriveNotice by themeViewModel.skipDriveNotice.collectAsStateWithLifecycle()

    // Drive Account / Storage notice dialog state
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

    // Export with Password Dialog state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var exportUseEncryption by remember { mutableStateOf(true) }
    var exportPendingAction by remember { mutableStateOf<String?>(null) } // "SAVE" or "SHARE" or "VIEW"

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

    // Export Document Launcher (Storage Access Framework: Google Drive, SD Card, Local)
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

    // Import Document Launcher (Select real .json file)
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. Theme Mode Section
        item {
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
                            onClick = { themeViewModel.setThemeMode("DARK") },
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
                            onClick = { themeViewModel.setThemeMode("LIGHT") },
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
                            onClick = { themeViewModel.setThemeMode("SYSTEM") },
                            modifier = Modifier.testTag("theme_system_radio")
                        )
                        Text("Seguir Padrão do Sistema")
                    }
                }
            }
        }

        // 2. Privacy & Security Section
        item {
            SettingsCard(
                title = "Segurança e Privacidade",
                icon = Icons.Default.Security,
                iconColor = ExpectedTeal
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            onCheckedChange = { themeViewModel.togglePrivacyMode() },
                            modifier = Modifier.testTag("switch_privacy_mode")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isPinConfigured) {
                            Button(
                                onClick = {
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinDialogError = ""
                                    showPinDialog = true
                                },
                                modifier = Modifier.weight(1f).testTag("btn_change_pin")
                            ) {
                                Text("Alterar PIN", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    themeViewModel.removeAppPin()
                                    Toast.makeText(context, "PIN de segurança removido.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("btn_remove_pin")
                            ) {
                                Text("Remover PIN", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinDialogError = ""
                                    showPinDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().testTag("btn_set_pin")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ativar Proteção por PIN")
                            }
                        }
                    }
                }
            }
        }

        // 3. Notifications & Due Reminders Section (08:00)
        item {
            SettingsCard(
                title = "Notificações e Lembretes (08:00)",
                icon = Icons.Default.NotificationsActive,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Lembretes programados diariamente às 08:00 sobre contas a pagar, empréstimos e recebimentos que vencem no dia ou estão em atraso (ignora registros quitados).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nota técnica: O horário exato de entrega pode sofrer pequenas variações pelas diretrizes de economia de bateria (Doze Mode) do Android.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        NotificationHelper.checkAndShowDueNotification(context, isTest = true)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_test_notification")
                        ) {
                            Text("Testar Notificação (08h)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    NotificationHelper.checkAndNotifyAssistedBackup(context, isTest = true)
                                }
                                Toast.makeText(context, "Lembrete de backup assistido gerado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_test_backup_notification")
                        ) {
                            Text("Aviso Backup Pronto", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. Secure Backup & Restore Section (AES-256-GCM + SAF)
        item {
            SettingsCard(
                title = "Backup e Restauração Segura",
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
                                text = "Última exportação externa confirmada: ${DateUtils.formatDateTime(lastExportedTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpectedTeal
                            )
                        }
                    }

                    Text(
                        text = "Exportação Segura (Criptografia AES-256-GCM / Sem Conta Google):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportPendingAction = "SAVE"
                                exportPasswordInput = ""
                                exportUseEncryption = true
                                showExportPasswordDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("btn_export_backup_file")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar .json", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                exportPendingAction = "SHARE"
                                exportPasswordInput = ""
                                exportUseEncryption = true
                                showExportPasswordDialog = true
                            },
                            modifier = Modifier.weight(1f).testTag("btn_share_backup_file")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Restauração de Dados (Transacional e Segura):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerDriveNoticeOrExecute {
                                    openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_import_backup_file")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Selecionar .json", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreTextDialog = true },
                            modifier = Modifier.weight(1f).testTag("btn_restore_text")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Colar Texto", fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = {
                            exportedJsonText = financialViewModel.generateBackupJson(password = null)
                            showExportTextDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver código JSON não criptografado", fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. Backup History & Audit Section (Section 5.1)
        item {
            SettingsCard(
                title = "Histórico de Backups e Auditoria",
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

        // 6. App Diagnostics Section (Section 5.2)
        item {
            SettingsCard(
                title = "Diagnóstico do Aplicativo",
                icon = Icons.Default.Info,
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Verifique o estado de saúde do banco de dados, armazenamento e agendamentos locais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showDiagnosticsDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_open_diagnostics")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Executar Diagnóstico Técnico")
                    }
                }
            }
        }

        // 7. Danger Zone & Emergency Safety Restore (Section 2.3 & 5.3)
        item {
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

                    if (financialViewModel.hasSafetyBackup()) {
                        OutlinedButton(
                            onClick = { showEmergencyRestoreDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_emergency_restore")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = ExpectedTeal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Cópia de Emergência Pré-Limpeza", color = ExpectedTeal)
                        }
                    }

                    Button(
                        onClick = { showClearWarningDialog = true },
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

        // 8. About Section (Commercial Offline Release)
        item {
            SettingsCard(
                title = "Sobre o Aplicativo",
                icon = Icons.Default.CheckCircle
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Controle Financeiro Comercial v1.0",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "100% Offline • Sem servidores externos • Criptografia AES-256-GCM • Privacidade Conforme LGPD",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                    Text("Diagnóstico do Aplicativo")
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
                            Text("Notificações e Rotinas:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• Canal Diário 08:00: Ativo (InexactRepeating)", style = MaterialTheme.typography.labelSmall)
                            Text("• Canal Backup Assistido: Ativo", style = MaterialTheme.typography.labelSmall)
                            Text("• Permissões: Verificadas", style = MaterialTheme.typography.labelSmall)
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
                    Text("Atenção: Ação Irreversível")
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

    // --- Danger Zone Step 2: Final Verification Dialog (Requires typing "APAGAR") ---
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
