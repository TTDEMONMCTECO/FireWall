package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleFilterDao {
    @Query("SELECT * FROM rule_filters ORDER BY isCustom DESC, appName ASC")
    fun getAllRuleFilters(): Flow<List<RuleFilterEntity>>

    @Query("SELECT * FROM rule_filters WHERE pkg1Name = :pkgName OR pkg2Name = :pkgName")
    fun getFiltersForApp(pkgName: String): Flow<List<RuleFilterEntity>>

    @Query("SELECT COUNT(*) FROM rule_filters")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filters: List<RuleFilterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: RuleFilterEntity): Long

    @Update
    suspend fun update(filter: RuleFilterEntity)

    @Delete
    suspend fun delete(filter: RuleFilterEntity)

    @Query("DELETE FROM rule_filters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM rule_filters")
    suspend fun clearAll()
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllAppRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    suspend fun getAppRulesList(): List<AppRuleEntity>

    @Query("SELECT * FROM app_rules WHERE pkgName = :pkgName")
    suspend fun getAppRule(pkgName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appRules: List<AppRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appRule: AppRuleEntity)

    @Query("DELETE FROM app_rules")
    suspend fun clearAll()
}

@Dao
interface NetworkLogDao {
    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<NetworkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NetworkLogEntity)

    @Query("DELETE FROM network_logs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM network_logs WHERE status = 'deny'")
    fun getBlockedCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM network_logs WHERE status = 'allow'")
    fun getAllowedCount(): Flow<Long>
}
