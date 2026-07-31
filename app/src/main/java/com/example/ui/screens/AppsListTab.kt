package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AccessType
import com.example.domain.model.AppRuleConfig
import com.example.ui.components.AccessToggleChip
import com.example.ui.viewmodel.FirewallUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsListTab(
    uiState: FirewallUiState,
    onSearchQueryChanged: (String) -> Unit,
    onAppAccessChanged: (String, String, AccessType, AccessType) -> Unit,
    onAddCustomRuleForApp: (AppRuleConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field"),
            placeholder = { Text("Search apps or packages...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // App List
        if (uiState.appRules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching apps found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.appRules,
                    key = { it.pkgName }
                ) { appRule ->
                    AppRuleRowItem(
                        appRule = appRule,
                        onAppAccessChanged = onAppAccessChanged,
                        onAddCustomRule = { onAddCustomRuleForApp(appRule) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRuleRowItem(
    appRule: AppRuleConfig,
    onAppAccessChanged: (String, String, AccessType, AccessType) -> Unit,
    onAddCustomRule: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_rule_row_${appRule.pkgName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIconImage(
                            pkgName = appRule.pkgName,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appRule.appName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (appRule.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "System",
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = appRule.pkgName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onAddCustomRule) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Custom Rule",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccessToggleChip(
                    label = "Wi-Fi",
                    currentAccess = appRule.wifi,
                    onAccessChanged = { newWifi ->
                        onAppAccessChanged(appRule.pkgName, appRule.appName, newWifi, appRule.mobile)
                    }
                )

                AccessToggleChip(
                    label = "Mobile",
                    currentAccess = appRule.mobile,
                    onAccessChanged = { newMobile ->
                        onAppAccessChanged(appRule.pkgName, appRule.appName, appRule.wifi, newMobile)
                    }
                )

                if (appRule.customRuleCount > 0) {
                    Text(
                        text = "${appRule.customRuleCount} custom filters",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AppIconImage(
    pkgName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconDrawable by produceState<Drawable?>(initialValue = null, key1 = pkgName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(pkgName)
            } catch (e: Exception) {
                null
            }
        }
    }

    if (iconDrawable != null) {
        AsyncImage(
            model = iconDrawable,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
        )
    }
}
