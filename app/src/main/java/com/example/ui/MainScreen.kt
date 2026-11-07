package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditLoanDialog
import com.example.ui.components.AppTopHeader
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PayablesScreen
import com.example.ui.screens.ReceivablesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ControleFinanceiroTheme
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.ThemeViewModel

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Início", Icons.Default.Home)
    object Receivables : NavItem("receivables", "Receber", Icons.Default.Payments)
    object Payables : NavItem("payables", "Pagar", Icons.Default.MoneyOff)
    object Settings : NavItem("settings", "Ajustes", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    financialViewModel: FinancialViewModel,
    themeViewModel: ThemeViewModel
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isPrivacyMode by themeViewModel.isPrivacyMode.collectAsStateWithLifecycle()
    val isPinConfigured by themeViewModel.isPinConfigured.collectAsStateWithLifecycle()
    val isAppUnlocked by themeViewModel.isAppUnlocked.collectAsStateWithLifecycle()

    val searchQuery by financialViewModel.searchQuery.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var receivablesSubTab by remember { mutableIntStateOf(0) }
    var isNavigatedFromDashboard by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var showQuickAddLoanDialog by remember { mutableStateOf(false) }

    // PIN Unlock state
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem.Dashboard,
        NavItem.Receivables,
        NavItem.Payables,
        NavItem.Settings
    )

    val (headerTitle, headerSubtitle) = when (currentTab) {
        0 -> "Dashboard" to "Visão unificada das suas finanças"
        1 -> if (receivablesSubTab == 0) {
            "Empréstimos" to "Gestão de empréstimos concedidos"
        } else {
            "Valores a Receber" to "Controle de receitas e vendas"
        }
        2 -> "Dívidas" to "Gestão de compromissos financeiros"
        else -> "Configurações" to "Ajustes, segurança e dados"
    }

    val showBackButton = isNavigatedFromDashboard && (currentTab != 0)
    val showSearchAndFilter = currentTab in listOf(0, 1, 2)

    ControleFinanceiroTheme(themeMode = themeMode) {
        if (isPinConfigured && !isAppUnlocked) {
            // Lock Screen overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Controle Financeiro Protegido",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Digite o seu PIN de segurança para desbloquear seus dados financeiros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                enteredPin = it
                                pinError = false
                            },
                            label = { Text("PIN de Acesso") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("pin_unlock_input")
                        )

                        if (pinError) {
                            Text(
                                text = "PIN incorreto. Tente novamente.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = {
                                val success = themeViewModel.verifyAndUnlock(enteredPin)
                                if (!success) {
                                    pinError = true
                                } else {
                                    enteredPin = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pin_unlock_button"),
                            enabled = enteredPin.isNotBlank()
                        ) {
                            Text("Desbloquear")
                        }
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    AppTopHeader(
                        title = headerTitle,
                        subtitle = headerSubtitle,
                        onBackClick = if (showBackButton) {
                            {
                                currentTab = 0
                                isNavigatedFromDashboard = false
                            }
                        } else null,
                        onSearchClick = if (showSearchAndFilter) {
                            { isSearchActive = !isSearchActive }
                        } else null,
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { financialViewModel.setSearchQuery(it) },
                        onCloseSearch = {
                            isSearchActive = false
                            financialViewModel.setSearchQuery("")
                        },
                        isPrivacyMode = isPrivacyMode,
                        onTogglePrivacy = { themeViewModel.togglePrivacyMode() }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val isSelected = currentTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    currentTab = index
                                    isNavigatedFromDashboard = false
                                    isSearchActive = false
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("nav_item_${item.route}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = currentTab, label = "screen_transition") { tabIndex ->
                        when (tabIndex) {
                            0 -> DashboardScreen(
                                viewModel = financialViewModel,
                                onNavigateToSales = {
                                    currentTab = 1
                                    receivablesSubTab = 1
                                    financialViewModel.setSearchQuery("Venda")
                                    isNavigatedFromDashboard = true
                                },
                                onNavigateToReceivables = {
                                    currentTab = 1
                                    receivablesSubTab = 1
                                    isNavigatedFromDashboard = true
                                },
                                onNavigateToPayables = {
                                    currentTab = 2
                                    isNavigatedFromDashboard = true
                                },
                                onNavigateToLoansThisMonth = {
                                    currentTab = 1
                                    receivablesSubTab = 0
                                    isNavigatedFromDashboard = true
                                },
                                onAddNewClick = { showQuickAddLoanDialog = true },
                                isPrivacyMode = isPrivacyMode
                            )

                            1 -> ReceivablesScreen(
                                viewModel = financialViewModel,
                                initialTab = receivablesSubTab,
                                onSubTabChange = { receivablesSubTab = it }
                            )

                            2 -> PayablesScreen(
                                viewModel = financialViewModel
                            )

                            3 -> SettingsScreen(
                                financialViewModel = financialViewModel,
                                themeViewModel = themeViewModel
                            )
                        }
                    }
                }

                if (showQuickAddLoanDialog) {
                    AddEditLoanDialog(
                        loanToEdit = null,
                        onDismiss = { showQuickAddLoanDialog = false },
                        onSave = { loan ->
                            financialViewModel.saveLoan(loan)
                            showQuickAddLoanDialog = false
                        }
                    )
                }
            }
        }
    }
}
