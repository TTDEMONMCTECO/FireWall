package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AccessType
import com.example.ui.theme.HighDensityError
import com.example.ui.theme.HighDensitySuccess

@Composable
fun AccessToggleChip(
    label: String,
    currentAccess: AccessType,
    onAccessChanged: (AccessType) -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, icon, text) = when (currentAccess) {
        AccessType.ALLOW -> Triple(HighDensitySuccess.copy(alpha = 0.15f), Icons.Default.Check, "Allow")
        AccessType.DENY -> Triple(HighDensityError.copy(alpha = 0.15f), Icons.Default.Close, "Deny")
        AccessType.NONE -> Triple(MaterialTheme.colorScheme.surfaceVariant, Icons.Default.Remove, "Default")
    }

    val tintColor = when (currentAccess) {
        AccessType.ALLOW -> HighDensitySuccess
        AccessType.DENY -> HighDensityError
        AccessType.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable {
                val next = when (currentAccess) {
                    AccessType.NONE -> AccessType.ALLOW
                    AccessType.ALLOW -> AccessType.DENY
                    AccessType.DENY -> AccessType.NONE
                }
                onAccessChanged(next)
            },
        color = bgColor,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tintColor
            )
        }
    }
}
