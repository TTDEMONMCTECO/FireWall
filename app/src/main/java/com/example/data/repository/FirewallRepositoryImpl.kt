package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.local.*
import com.example.data.parser.FirewallJsonData
import com.example.data.parser.JsonRuleParser
import com.example.domain.model.AccessType
import com.example.domain.model.AppRuleConfig
import com.example.domain.model.NetworkLog
import com.example.domain.model.RuleFilter
import com.example.domain.repository.FirewallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FirewallRepositoryImpl(
    private val context: Context,
    private val ruleFilterDao: RuleFilterDao,
    private val appRuleDao: AppRuleDao,
    private val networkLogDao: NetworkLogDao
) : FirewallRepository {

    override fun getAllRuleFilters(): Flow<List<RuleFilter>> {
        return ruleFilterDao.getAllRuleFilters().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFiltersForApp(pkgName: String): Flow<List<RuleFilter>> {
        return ruleFilterDao.getFiltersForApp(pkgName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllAppRules(): Flow<List<AppRuleConfig>> {
        return combine(
            appRuleDao.getAllAppRules(),
            ruleFilterDao.getAllRuleFilters()
        ) { appEntities, filters ->
            val filterCountMap = filters.groupBy { it.pkg1Name }
                .mapValues { entry -> entry.value.count { it.isCustom } }

            appEntities.map { entity ->
                AppRuleConfig(
                    pkgName = entity.pkgName,
                    appName = entity.appName,
                    wifi = AccessType.fromString(entity.wifi),
                    mobile = AccessType.fromString(entity.mobile),
                    isSystemApp = entity.isSystemApp,
                    customRuleCount = filterCountMap[entity.pkgName] ?: 0
                )
            }
        }
    }

    override fun getRecentLogs(): Flow<List<NetworkLog>> {
        return networkLogDao.getRecentLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBlockedCount(): Flow<Long> = networkLogDao.getBlockedCount()

    override fun getAllowedCount(): Flow<Long> = networkLogDao.getAllowedCount()

    override suspend fun saveRuleFilter(filter: RuleFilter) {
        withContext(Dispatchers.IO) {
            val entity = filter.toEntity()
            if (filter.id == 0L) {
                ruleFilterDao.insert(entity)
            } else {
                ruleFilterDao.update(entity)
            }
        }
    }

    override suspend fun deleteRuleFilter(filterId: Long) {
        withContext(Dispatchers.IO) {
            ruleFilterDao.deleteById(filterId)
        }
    }

    override suspend fun updateAppRule(
        pkgName: String,
        appName: String,
        wifi: AccessType,
        mobile: AccessType
    ) {
        withContext(Dispatchers.IO) {
            val existing = appRuleDao.getAppRule(pkgName)
            val isSystem = existing?.isSystemApp ?: false
            appRuleDao.insert(
                AppRuleEntity(
                    pkgName = pkgName,
                    appName = appName,
                    wifi = wifi.name.lowercase(),
                    mobile = mobile.name.lowercase(),
                    isSystemApp = isSystem
                )
            )
        }
    }

    override suspend fun loadDefaultRulesFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (ruleFilterDao.getCount() == 0) {
                val jsonString = context.assets.open("default_rules.json").bufferedReader().use { it.readText() }
                val parsed = JsonRuleParser.parseJsonString(jsonString)

                val filterEntities = parsed.filters.map { it.toEntity() }
                ruleFilterDao.insertAll(filterEntities)

                val appEntities = parsed.apps.map {
                    AppRuleEntity(
                        pkgName = it.pkgName,
                        appName = it.appName,
                        wifi = it.wifi.name.lowercase(),
                        mobile = it.mobile.name.lowercase(),
                        isSystemApp = it.isSystemApp
                    )
                }
                appRuleDao.insertAll(appEntities)
            }
        }
    }

    override suspend fun importRulesFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = JsonRuleParser.parseJsonString(jsonString)
            
            val filterEntities = parsed.filters.map { it.toEntity() }
            ruleFilterDao.insertAll(filterEntities)

            val appEntities = parsed.apps.map {
                AppRuleEntity(
                    pkgName = it.pkgName,
                    appName = it.appName,
                    wifi = it.wifi.name.lowercase(),
                    mobile = it.mobile.name.lowercase(),
                    isSystemApp = it.isSystemApp
                )
            }
            appRuleDao.insertAll(appEntities)

            parsed.filters.size
        }
    }

    override suspend fun exportRulesToJson(): String = withContext(Dispatchers.IO) {
        val filtersFlow = ruleFilterDao.getAllRuleFilters()
        val appsFlow = appRuleDao.getAllAppRules()

        var filtersList: List<RuleFilterEntity> = emptyList()
        var appsList: List<AppRuleEntity> = emptyList()

        // Sync fetch for quick export
        val db = FirewallDatabase.getDatabase(context)
        filtersList = db.ruleFilterDao().let {
            // direct DB query
            val list = mutableListOf<RuleFilterEntity>()
            // load from DB directly
            db.query("SELECT * FROM rule_filters", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        RuleFilterEntity(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            appName = cursor.getString(cursor.getColumnIndexOrThrow("appName")),
                            pkg1Name = cursor.getString(cursor.getColumnIndexOrThrow("pkg1Name")),
                            pkg2Name = cursor.getString(cursor.getColumnIndexOrThrow("pkg2Name")),
                            wifi = cursor.getString(cursor.getColumnIndexOrThrow("wifi")),
                            mobile = cursor.getString(cursor.getColumnIndexOrThrow("mobile")),
                            port = cursor.getInt(cursor.getColumnIndexOrThrow("port")),
                            priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority")),
                            proto = cursor.getString(cursor.getColumnIndexOrThrow("proto")),
                            server = cursor.getString(cursor.getColumnIndexOrThrow("server")),
                            serverStrType = cursor.getString(cursor.getColumnIndexOrThrow("serverStrType")),
                            isCustom = cursor.getInt(cursor.getColumnIndexOrThrow("isCustom")) == 1
                        )
                    )
                }
            }
            list
        }

        appsList = db.query("SELECT * FROM app_rules", null).use { cursor ->
            val list = mutableListOf<AppRuleEntity>()
            while (cursor.moveToNext()) {
                list.add(
                    AppRuleEntity(
                        pkgName = cursor.getString(cursor.getColumnIndexOrThrow("pkgName")),
                        appName = cursor.getString(cursor.getColumnIndexOrThrow("appName")),
                        wifi = cursor.getString(cursor.getColumnIndexOrThrow("wifi")),
                        mobile = cursor.getString(cursor.getColumnIndexOrThrow("mobile")),
                        isSystemApp = cursor.getInt(cursor.getColumnIndexOrThrow("isSystemApp")) == 1
                    )
                )
            }
            list
        }

        val data = FirewallJsonData(
            apps = appsList.map {
                AppRuleConfig(
                    pkgName = it.pkgName,
                    appName = it.appName,
                    wifi = AccessType.fromString(it.wifi),
                    mobile = AccessType.fromString(it.mobile),
                    isSystemApp = it.isSystemApp
                )
            },
            filters = filtersList.map { it.toDomain() }
        )

        JsonRuleParser.toJsonString(data)
    }

    override suspend fun addLog(log: NetworkLog) {
        withContext(Dispatchers.IO) {
            networkLogDao.insertLog(log.toEntity())
        }
    }

    override suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            networkLogDao.clearLogs()
        }
    }

    override suspend fun scanInstalledApps() {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val installedPackages = pm.getInstalledPackages(0)

                val appEntities = installedPackages.mapNotNull { pkg ->
                    val pkgName = pkg.packageName ?: return@mapNotNull null
                    val appName = try {
                        pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkgName
                    } catch (e: Exception) {
                        pkgName
                    }
                    val isSystem = try {
                        (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                    } catch (e: Exception) {
                        false
                    }

                    val existing = appRuleDao.getAppRule(pkgName)
                    if (existing == null) {
                        AppRuleEntity(
                            pkgName = pkgName,
                            appName = appName,
                            wifi = "none",
                            mobile = "none",
                            isSystemApp = isSystem
                        )
                    } else {
                        null
                    }
                }

                if (appEntities.isNotEmpty()) {
                    appRuleDao.insertAll(appEntities)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Mapping Extensions
    private fun RuleFilterEntity.toDomain() = RuleFilter(
        id = id,
        appName = appName,
        pkg1Name = pkg1Name,
        pkg2Name = pkg2Name,
        wifi = AccessType.fromString(wifi),
        mobile = AccessType.fromString(mobile),
        port = port,
        priority = priority,
        proto = proto,
        server = server,
        serverStrType = serverStrType,
        isCustom = isCustom
    )

    private fun RuleFilter.toEntity() = RuleFilterEntity(
        id = id,
        appName = appName,
        pkg1Name = pkg1Name,
        pkg2Name = pkg2Name,
        wifi = wifi.name.lowercase(),
        mobile = mobile.name.lowercase(),
        port = port,
        priority = priority,
        proto = proto,
        server = server,
        serverStrType = serverStrType,
        isCustom = isCustom
    )

    private fun NetworkLogEntity.toDomain() = NetworkLog(
        id = id,
        timestamp = timestamp,
        appName = appName,
        pkgName = pkgName,
        destination = destination,
        port = port,
        protocol = protocol,
        status = AccessType.fromString(status)
    )

    private fun NetworkLog.toEntity() = NetworkLogEntity(
        id = id,
        timestamp = timestamp,
        appName = appName,
        pkgName = pkgName,
        destination = destination,
        port = port,
        protocol = protocol,
        status = status.name.lowercase()
    )
}
