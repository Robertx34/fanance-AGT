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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    var showRestoreTextDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    var showExportTextDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    // Backup Confirmation Dialog State
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var pendingBackupJson by remember { mutableStateOf("") }
    var restoreSummaryMessage by remember { mutableStateOf("") }

    // Two-step Danger Zone Dialog State
    var showClearWarningDialog by remember { mutableStateOf(false) }
    var showClearFinalDialog by remember { mutableStateOf(false) }
    var deleteConfirmationInput by remember { mutableStateOf("") }

    // Notification Permission Launcher for Android 13+
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

    // Export Document Launcher (Save real .json file)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = financialViewModel.generateBackupJson()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                themeViewModel.updateLastBackupTime(System.currentTimeMillis())
                Toast.makeText(context, "Arquivo .json salvo com sucesso!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao salvar arquivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                    try {
                        val parsed = BackupManager.parseBackupJson(content)
                        pendingBackupJson = content
                        restoreSummaryMessage = "Arquivo JSON válido!\n\nDados identificados:\n" +
                                "• ${parsed.loans.size} Empréstimo(s)\n" +
                                "• ${parsed.receivables.size} Venda(s) / Recebível(is)\n" +
                                "• ${parsed.payables.size} Dívida(s) / Conta(s) a Pagar\n" +
                                "• ${parsed.history.size} Registro(s) de Histórico\n\n" +
                                "Atenção: Os dados atuais serão substituídos pelos dados deste arquivo."
                        showConfirmRestoreDialog = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "O arquivo selecionado não contém um JSON de backup válido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao ler arquivo selecionado: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Theme Mode Section
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

        // Daily Due Notification Section (08:00)
        item {
            SettingsCard(
                title = "Notificações de Vencimento",
                icon = Icons.Default.NotificationsActive,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Lembretes diários programados automaticamente às 08:00 para alertar sobre contas a pagar, empréstimos e recebimentos vencendo no dia ou em atraso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_notification")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testar Notificação das 08:00 Agora")
                    }
                }
            }
        }

        // Backup & Restore Section (Real .json file handling)
        item {
            SettingsCard(
                title = "Backup e Restauração",
                icon = Icons.Default.Backup
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lastBackupTime > 0) {
                            "Último backup realizado em: ${DateUtils.formatDateTime(lastBackupTime)}"
                        } else {
                            "Nenhum backup registrado até o momento."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Export Actions
                    Text(
                        text = "Exportar Dados:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                createDocumentLauncher.launch("backup_financeiro_$timestamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_backup_file")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar .json", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val json = financialViewModel.generateBackupJson()
                                themeViewModel.updateLastBackupTime(System.currentTimeMillis())
                                shareBackupFile(context, json)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_share_backup_file")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartilhar", fontSize = 12.sp)
                        }
                    }

                    // Import Actions
                    Text(
                        text = "Restaurar Dados:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_backup_file")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Selecionar .json", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreTextDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_text")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Colar Texto", fontSize = 12.sp)
                        }
                    }

                    // Secondary helper: view/copy JSON raw text
                    TextButton(
                        onClick = {
                            exportedJsonText = financialViewModel.generateBackupJson()
                            showExportTextDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver código JSON de backup", fontSize = 12.sp)
                    }
                }
            }
        }

        // Reset / Danger Zone (Safe two-step verification)
        item {
            SettingsCard(
                title = "Zona de Perigo / Banco de Dados",
                icon = Icons.Default.DeleteForever,
                iconColor = PayableRose
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exclui permanentemente todos os lançamentos de empréstimos, recebimentos, dívidas e históricos do aplicativo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showClearWarningDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OverdueRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_clear_db")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Limpar Banco de Dados")
                    }
                }
            }
        }

        // About / App Info
        item {
            SettingsCard(
                title = "Sobre o Aplicativo",
                icon = Icons.Default.Info
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Controle Financeiro v1.0",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "100% Offline • Sem APIs • Room Database • Jetpack Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // --- File Import Confirmation Dialog ---
    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmRestoreDialog = false
                pendingBackupJson = ""
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
                        financialViewModel.restoreBackupJson(
                            jsonString = pendingBackupJson,
                            onSuccess = {
                                themeViewModel.updateLastBackupTime(System.currentTimeMillis())
                                Toast.makeText(context, "Backup restaurado com sucesso!", Toast.LENGTH_LONG).show()
                                showConfirmRestoreDialog = false
                                pendingBackupJson = ""
                            },
                            onError = { err ->
                                Toast.makeText(context, "Erro ao restaurar: $err", Toast.LENGTH_LONG).show()
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
                    pendingBackupJson = ""
                }) {
                    Text("Cancelar")
                }
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
                    shareBackupFile(context, exportedJsonText)
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
                        if (restoreJsonText.isNotBlank()) {
                            try {
                                val parsed = BackupManager.parseBackupJson(restoreJsonText.trim())
                                pendingBackupJson = restoreJsonText.trim()
                                restoreSummaryMessage = "JSON válido!\n\nDados identificados:\n" +
                                        "• ${parsed.loans.size} Empréstimo(s)\n" +
                                        "• ${parsed.receivables.size} Venda(s)\n" +
                                        "• ${parsed.payables.size} Dívida(s)\n" +
                                        "• ${parsed.history.size} Histórico(s)\n\n" +
                                        "Deseja substituir os dados atuais por este backup?"
                                showRestoreTextDialog = false
                                showConfirmRestoreDialog = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "JSON inválido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                    text = "Você está prestes a excluir TODOS os empréstimos, recebimentos, dívidas e históricos salvos no dispositivo.\n\n" +
                            "Esta operação é permanente e não poderá ser desfeita.\n\n" +
                            "Deseja realmente avançar para a confirmação final?",
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
                    Text("Continuar...")
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
                        text = "Para confirmar a exclusão permanente de todos os dados, digite a palavra APAGAR no campo abaixo:",
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
                            Toast.makeText(context, "Banco de dados limpo com sucesso.", Toast.LENGTH_SHORT).show()
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

private fun shareBackupFile(context: Context, jsonText: String) {
    try {
        val file = File(context.cacheDir, "backup_controle_financeiro.json")
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
