package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RuleFilterEntity::class, AppRuleEntity::class, NetworkLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FirewallDatabase : RoomDatabase() {
    abstract fun ruleFilterDao(): RuleFilterDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun networkLogDao(): NetworkLogDao

    companion object {
        @Volatile
        private var INSTANCE: FirewallDatabase? = null

        fun getDatabase(context: Context): FirewallDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FirewallDatabase::class.java,
                    "noroot_firewall_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
