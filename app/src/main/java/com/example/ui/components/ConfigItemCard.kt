package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConfig

private val protocolColors = mapOf(
    "vmess" to 0xFF6366F1, "vless" to 0xFF0EA5E9, "trojan" to 0xFFEC4899,
    "shadowsocks" to 0xFFF59E0B, "hysteria" to 0xFF10B981, "tuic" to 0xFF8B5CF6
)

@Composable
fun ConfigItemCard(config: SupabaseConfig, darkMode: Boolean, onCopy: () -> Unit, onImport: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val protoHex = protocolColors[config.type.lowercase()] ?: 0xFF6366F1
    val protoColor = androidx.compose.ui.graphics.Color(protoHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(protoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        config.type.uppercase(),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = protoColor, letterSpacing = 1.sp
                    )
                }
                if (!config.remarks.isNullOrEmpty()) {
                    Text(
                        config.remarks, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                } else {
                    Text("#${config.id}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { onCopy() }
                    .padding(12.dp)
            ) {
                Text(
                    config.raw_content,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 9.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Copy", fontSize = 12.sp)
                }
                Button(
                    onClick = onImport,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 9.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(5.dp))
                    Text("Hiddify", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
