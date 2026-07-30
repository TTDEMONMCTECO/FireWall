package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rule_filters")
data class RuleFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val pkg1Name: String,
    val pkg2Name: String? = null,
    val wifi: String,
    val mobile: String,
    val port: Int,
    val priority: Int,
    val proto: String,
    val server: String,
    val serverStrType: String,
    val isCustom: Boolean
)

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val pkgName: String,
    val appName: String,
    val wifi: String,
    val mobile: String,
    val isSystemApp: Boolean
)

@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appName: String,
    val pkgName: String,
    val destination: String,
    val port: Int,
    val protocol: String,
    val status: String
)
