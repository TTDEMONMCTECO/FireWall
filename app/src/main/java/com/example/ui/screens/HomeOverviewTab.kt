package com.example.ui.screens

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FirewallVpnService
import com.example.ui.theme.HighDensityError
import com.example.ui.theme.HighDensitySuccess
import com.example.ui.viewmodel.FirewallUiState

@Composable
fun HomeOverviewTab(
    uiState: FirewallUiState,
    onToggleVpn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            FirewallVpnService.startService(context)
        }
    }

    fun handleStartStop() {
        if (uiState.isVpnActive) {
            FirewallVpnService.stopService(context)
        } else {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnPrepareLauncher.launch(intent)
            } else {
                FirewallVpnService.startService(context)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Switcher Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vpn_status_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (uiState.isVpnActive) {
                                listOf(HighDensitySuccess.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface)
                            } else {
                                listOf(HighDensityError.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface)
                            }
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isVpnActive) HighDensitySuccess.copy(alpha = 0.2f)
                            else HighDensityError.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isVpnActive) Icons.Default.Shield else Icons.Default.Security,
                        contentDescription = "Status Shield",
                        tint = if (uiState.isVpnActive) HighDensitySuccess else HighDensityError,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.isVpnActive) "FIREWALL ACTIVE" else "FIREWALL STOPPED",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isVpnActive) HighDensitySuccess else HighDensityError
                    )
                    Text(
                        text = if (uiState.isVpnActive) "Protecting per-app connection & custom rules"
                        else "Traffic is currently unfiltered",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { handleStartStop() },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp)
                        .testTag("toggle_vpn_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isVpnActive) HighDensityError else HighDensitySuccess,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isVpnActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isVpnActive) "START / STOP: STOP" else "START / STOP: START",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Rules",
                value = "${uiState.totalRules}",
                subtitle = "${uiState.customRulesCount} Custom Filters",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary
            )

            StatCard(
                title = "Blocked Traffic",
                value = "${uiState.blockedCount}",
                subtitle = "Denied Connections",
                modifier = Modifier.weight(1f),
                color = HighDensityError
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Allowed Traffic",
                value = "${uiState.allowedCount}",
                subtitle = "Passed Connections",
                modifier = Modifier.weight(1f),
                color = HighDensitySuccess
            )

            StatCard(
                title = "Apps Filtered",
                value = "${uiState.appRules.size}",
                subtitle = "Package Rules",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Live Log Snapshot Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recent Traffic Snapshots",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.recentLogs.isEmpty()) {
                    Text(
                        text = "No recent activity yet. Start firewall to monitor packets.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.recentLogs.take(3).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.appName} (${log.destination}:${log.port})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${log.protocol} • ${log.pkgName}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = log.status.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.status.name == "ALLOW") HighDensitySuccess else HighDensityError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
