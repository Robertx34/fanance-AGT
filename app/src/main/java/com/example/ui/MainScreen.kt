package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FilterOption
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
    val searchQuery by financialViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by financialViewModel.selectedFilter.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var receivablesSubTab by remember { mutableIntStateOf(0) }
    var isNavigatedFromDashboard by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }

    var showQuickAddLoanDialog by remember { mutableStateOf(false) }

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
        else -> "Configurações" to "Ajustes e gerenciamento de dados"
    }

    val showBackButton = isNavigatedFromDashboard && (currentTab != 0)
    val showSearchAndFilter = currentTab in listOf(0, 1, 2)

    ControleFinanceiroTheme(themeMode = themeMode) {
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
                    }
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
                            onAddNewClick = { showQuickAddLoanDialog = true }
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
