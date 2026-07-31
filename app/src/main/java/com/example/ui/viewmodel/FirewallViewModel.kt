package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AccessType
import com.example.domain.model.AppRuleConfig
import com.example.domain.model.NetworkLog
import com.example.domain.model.RuleFilter
import com.example.domain.repository.FirewallRepository
import com.example.service.FirewallVpnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FirewallUiState(
    val isVpnActive: Boolean = false,
    val totalRules: Int = 0,
    val customRulesCount: Int = 0,
    val blockedCount: Long = 0,
    val allowedCount: Long = 0,
    val appRules: List<AppRuleConfig> = emptyList(),
    val filterRules: List<RuleFilter> = emptyList(),
    val recentLogs: List<NetworkLog> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class FirewallViewModel(
    private val repository: FirewallRepository,
    private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    val isVpnActive: StateFlow<Boolean> = FirewallVpnService.isRunning

    val uiState: StateFlow<FirewallUiState> = combine(
        combine(isVpnActive, repository.getAllAppRules(), repository.getAllRuleFilters()) { active, appRules, filterRules ->
            Triple(active, appRules, filterRules)
        },
        combine(repository.getRecentLogs(), repository.getBlockedCount(), repository.getAllowedCount()) { logs, blocked, allowed ->
            Triple(logs, blocked, allowed)
        },
        _searchQuery
    ) { flow1, flow2, query ->
        val (active, appRules, filterRules) = flow1
        val (logs, blocked, allowed) = flow2

        val filteredApps = if (query.isBlank()) {
            appRules
        } else {
            appRules.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.pkgName.contains(query, ignoreCase = true)
            }
        }

        val filteredFilters = if (query.isBlank()) {
            filterRules
        } else {
            filterRules.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.pkg1Name.contains(query, ignoreCase = true) ||
                it.server.contains(query, ignoreCase = true)
            }
        }

        FirewallUiState(
            isVpnActive = active,
            totalRules = filterRules.size,
            customRulesCount = filterRules.count { it.isCustom },
            blockedCount = blocked,
            allowedCount = allowed,
            appRules = filteredApps,
            filterRules = filteredFilters,
            recentLogs = logs,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FirewallUiState()
    )

    init {
        viewModelScope.launch {
            try {
                repository.loadDefaultRulesFromAssets()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                repository.scanInstalledApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateAppAccess(pkgName: String, appName: String, wifi: AccessType, mobile: AccessType) {
        viewModelScope.launch {
            repository.updateAppRule(pkgName, appName, wifi, mobile)
            if (isVpnActive.value) {
                FirewallVpnService.reloadRules(context)
            }
        }
    }

    fun saveCustomRule(
        id: Long = 0,
        appName: String,
        pkgName: String,
        wifi: AccessType,
        mobile: AccessType,
        port: Int,
        server: String,
        proto: String
    ) {
        viewModelScope.launch {
            val filter = RuleFilter(
                id = id,
                appName = appName,
                pkg1Name = pkgName,
                wifi = wifi,
                mobile = mobile,
                port = port,
                server = server.ifBlank { "*" },
                proto = proto,
                serverStrType = if (server.contains(".")) "host" else "ip4",
                isCustom = true
            )
            repository.saveRuleFilter(filter)
            if (isVpnActive.value) {
                FirewallVpnService.reloadRules(context)
            }
            _snackbarMessage.value = "Custom rule saved for $appName"
        }
    }

    fun deleteRuleFilter(filterId: Long) {
        viewModelScope.launch {
            repository.deleteRuleFilter(filterId)
            if (isVpnActive.value) {
                FirewallVpnService.reloadRules(context)
            }
            _snackbarMessage.value = "Rule deleted"
        }
    }

    fun importRulesFromJson(jsonString: String) {
        viewModelScope.launch {
            val result = repository.importRulesFromJson(jsonString)
            result.onSuccess { count ->
                if (isVpnActive.value) {
                    FirewallVpnService.reloadRules(context)
                }
                _snackbarMessage.value = "Successfully imported $count rules!"
            }.onFailure {
                _snackbarMessage.value = "Failed to parse JSON: ${it.localizedMessage}"
            }
        }
    }

    fun exportRulesToJson(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportRulesToJson()
            onExportReady(json)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _snackbarMessage.value = "Network logs cleared"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val repository: FirewallRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FirewallViewModel(repository, context.applicationContext) as T
        }
    }
}
