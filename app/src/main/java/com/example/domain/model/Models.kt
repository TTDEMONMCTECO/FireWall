package com.example.domain.model

enum class AccessType {
    ALLOW,
    DENY,
    NONE;

    companion object {
        fun fromString(value: String): AccessType {
            return when (value.lowercase()) {
                "allow" -> ALLOW
                "deny" -> DENY
                else -> NONE
            }
        }

        fun AccessType.toRuleString(): String {
            return when (this) {
                ALLOW -> "allow"
                DENY -> "deny"
                NONE -> "none"
            }
        }
    }
}

data class RuleFilter(
    val id: Long = 0,
    val appName: String,
    val pkg1Name: String,
    val pkg2Name: String? = null,
    val wifi: AccessType,
    val mobile: AccessType,
    val port: Int = -1,
    val proto: String = "tcp",
    val server: String = "*",
    val serverStrType: String = "ip4",
    val isCustom: Boolean = false,
    val priority: Int = 0
)

data class AppRuleConfig(
    val pkgName: String,
    val appName: String,
    val wifi: AccessType = AccessType.NONE,
    val mobile: AccessType = AccessType.NONE,
    val isSystemApp: Boolean = false,
    val customRuleCount: Int = 0
)

data class NetworkLog(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val pkgName: String,
    val destination: String,
    val port: Int,
    val protocol: String,
    val status: AccessType
)

data class FirewallStats(
    val isActive: Boolean = false,
    val totalRules: Int = 0,
    val customRules: Int = 0,
    val totalBlocked: Long = 0,
    val totalAllowed: Long = 0
)
