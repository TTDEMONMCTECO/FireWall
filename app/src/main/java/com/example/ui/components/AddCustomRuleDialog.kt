package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AccessType

@Composable
fun AddCustomRuleDialog(
    initialAppName: String = "",
    initialPkgName: String = "",
    onDismiss: () -> Unit,
    onSaveRule: (
        appName: String,
        pkgName: String,
        wifi: AccessType,
        mobile: AccessType,
        port: Int,
        server: String,
        proto: String
    ) -> Unit
) {
    var appName by remember { mutableStateOf(initialAppName.ifBlank { "Custom App" }) }
    var pkgName by remember { mutableStateOf(initialPkgName.ifBlank { "com.example.app" }) }
    var server by remember { mutableStateOf("*") }
    var portText by remember { mutableStateOf("-1") }
    var selectedProto by remember { mutableStateOf("tcp") }
    var wifiAccess by remember { mutableStateOf(AccessType.DENY) }
    var mobileAccess by remember { mutableStateOf(AccessType.DENY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Custom Firewall Rule",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_app_name_input")
                )

                OutlinedTextField(
                    value = pkgName,
                    onValueChange = { pkgName = it },
                    label = { Text("Package Name (e.g. com.tencent.ig)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_pkg_name_input")
                )

                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Host/IP Server (* for all or domain)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_server_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port (-1 for all)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("rule_port_input")
                    )

                    OutlinedTextField(
                        value = selectedProto,
                        onValueChange = { selectedProto = it },
                        label = { Text("Proto (tcp/udp)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("rule_proto_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccessToggleChip(
                        label = "Wi-Fi",
                        currentAccess = wifiAccess,
                        onAccessChanged = { wifiAccess = it }
                    )

                    AccessToggleChip(
                        label = "Mobile",
                        currentAccess = mobileAccess,
                        onAccessChanged = { mobileAccess = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = portText.toIntOrNull() ?: -1
                    onSaveRule(
                        appName,
                        pkgName,
                        wifiAccess,
                        mobileAccess,
                        p,
                        server,
                        selectedProto.lowercase()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_rule_button")
            ) {
                Text("Save Filter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
