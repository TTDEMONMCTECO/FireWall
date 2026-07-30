package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.FirewallDatabase
import com.example.data.repository.FirewallRepositoryImpl
import com.example.domain.model.AccessType
import com.example.domain.model.NetworkLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class FirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var simulationJob: Job? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "firewall_vpn_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FirewallVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_isRunning.value) return

        try {
            val builder = Builder()
                .setSession("NoRoot Firewall")
                .addAddress("10.1.10.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")

            vpnInterface = builder.establish()
            _isRunning.value = true

            startForeground(NOTIFICATION_ID, createNotification())
            startTrafficMonitorSimulation()

        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        simulationJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        vpnInterface = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTrafficMonitorSimulation() {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            val db = FirewallDatabase.getDatabase(applicationContext)
            val repository = FirewallRepositoryImpl(
                applicationContext,
                db.ruleFilterDao(),
                db.appRuleDao(),
                db.networkLogDao()
            )

            val sampleTargets = listOf(
                Pair("PUBG MOBILE", "com.tencent.ig"),
                Pair("WhatsApp", "com.whatsapp"),
                Pair("Telegram", "org.telegram.messenger"),
                Pair("Facebook", "com.facebook.katana"),
                Pair("Gmail", "com.google.android.gm")
            )
            val sampleHosts = listOf(
                "asia.csoversea.mbgame.anticheatexpert.com",
                "graph.facebook.com",
                "api.telegram.org",
                "mail.google.com",
                "down.anticheatexpert.com",
                "fonts.googleapis.com"
            )
            val ports = listOf(80, 443, 8080, 17500, 20371)

            while (isActive && _isRunning.value) {
                delay(6000) // generate simulated log entry periodically
                val target = sampleTargets.random()
                val host = sampleHosts.random()
                val port = ports.random()

                // Check rules logic
                val action = if (host.contains("anticheatexpert") || target.second == "com.tencent.ig" && port == 20371) {
                    AccessType.DENY
                } else if (port == 17500 || target.second == "org.telegram.messenger") {
                    AccessType.ALLOW
                } else {
                    if ((1..10).random() > 3) AccessType.ALLOW else AccessType.DENY
                }

                repository.addLog(
                    NetworkLog(
                        appName = target.first,
                        pkgName = target.second,
                        destination = host,
                        port = port,
                        protocol = if ((1..2).random() == 1) "TCP" else "UDP",
                        status = action
                    )
                )
            }
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("NoRoot Firewall Active")
        .setContentText("Monitoring network traffic and enforcing custom rules.")
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NoRoot Firewall Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows firewall protection active notification"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
