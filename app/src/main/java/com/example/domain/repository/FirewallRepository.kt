package com.example.domain.repository

import com.example.domain.model.AccessType
import com.example.domain.model.AppRuleConfig
import com.example.domain.model.NetworkLog
import com.example.domain.model.RuleFilter
import kotlinx.coroutines.flow.Flow

interface FirewallRepository {
    fun getAllRuleFilters(): Flow<List<RuleFilter>>
    fun getFiltersForApp(pkgName: String): Flow<List<RuleFilter>>
    fun getAllAppRules(): Flow<List<AppRuleConfig>>
    fun getRecentLogs(): Flow<List<NetworkLog>>
    fun getBlockedCount(): Flow<Long>
    fun getAllowedCount(): Flow<Long>

    suspend fun saveRuleFilter(filter: RuleFilter)
    suspend fun deleteRuleFilter(filterId: Long)
    suspend fun updateAppRule(pkgName: String, appName: String, wifi: AccessType, mobile: AccessType)
    suspend fun loadDefaultRulesFromAssets(): Result<Unit>
    suspend fun importRulesFromJson(jsonString: String): Result<Int>
    suspend fun exportRulesToJson(): String
    suspend fun addLog(log: NetworkLog)
    suspend fun clearLogs()
    suspend fun scanInstalledApps()
}
