package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppRuleConfig
import com.example.ui.components.AddCustomRuleDialog
import com.example.ui.screens.*
import com.example.ui.viewmodel.FirewallViewModel
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Settings

enum class FirewallTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    APPS("Apps", Icons.Default.Apps),
    FILTERS("Filters", Icons.Default.Tune),
    LOGS("Logs", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FirewallViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(FirewallTab.HOME) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var preselectedAppForRule by remember { mutableStateOf<AppRuleConfig?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "NoRoot Firewall",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isVpnActive) "Status: Protected" else "Status: Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isVpnActive) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                FirewallTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                FirewallTab.HOME -> {
                    HomeOverviewTab(
                        uiState = uiState,
                        onToggleVpn = {}
                    )
                }
                FirewallTab.APPS -> {
                    AppsListTab(
                        uiState = uiState,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onAppAccessChanged = { pkg, name, wifi, mobile ->
                            viewModel.updateAppAccess(pkg, name, wifi, mobile)
                        },
                        onAddCustomRuleForApp = { appConfig ->
                            preselectedAppForRule = appConfig
                            showAddRuleDialog = true
                        }
                    )
                }
                FirewallTab.FILTERS -> {
                    CustomRulesTab(
                        uiState = uiState,
                        onAddNewRuleClick = {
                            preselectedAppForRule = null
                            showAddRuleDialog = true
                        },
                        onDeleteRule = { viewModel.deleteRuleFilter(it) }
                    )
                }
                FirewallTab.LOGS -> {
                    TrafficLogsTab(
                        uiState = uiState,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
                FirewallTab.SETTINGS -> {
                    SettingsImportExportTab(
                        uiState = uiState,
                        onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                        onImportJson = { json -> viewModel.importRulesFromJson(json) },
                        onExportJson = { callback -> viewModel.exportRulesToJson(callback) },
                        onResetDefaultRules = {
                            coroutineScope.launch {
                                // reload default rules
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddCustomRuleDialog(
            initialAppName = preselectedAppForRule?.appName ?: "",
            initialPkgName = preselectedAppForRule?.pkgName ?: "",
            onDismiss = {
                showAddRuleDialog = false
                preselectedAppForRule = null
            },
            onSaveRule = { appName, pkgName, wifi, mobile, port, server, proto ->
                viewModel.saveCustomRule(
                    appName = appName,
                    pkgName = pkgName,
                    wifi = wifi,
                    mobile = mobile,
                    port = port,
                    server = server,
                    proto = proto
                )
            }
        )
    }
}
