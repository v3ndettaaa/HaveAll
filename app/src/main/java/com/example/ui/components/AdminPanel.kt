package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseChannel
import com.example.data.SupabaseSubscription
import com.example.ui.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.Translations

@Composable
fun AdminPanel(
    viewModel: MainViewModel,
    channelsState: UiState<List<SupabaseChannel>>,
    subscriptionsState: UiState<List<SupabaseSubscription>>,
    language: AppLanguage,
    darkMode: Boolean
) {
    val context = LocalContext.current
    val accent = if (darkMode) Color(0xFF00E5FF) else Color(0xFF2979FF)
    var channelInput by remember { mutableStateOf("") }
    var subUrlInput by remember { mutableStateOf("") }
    var subLabelInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.get("admin_panel", language), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(Translations.get("admin_desc", language), fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(Translations.get("sync_interval", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }

        item {
            SectionHeader(Translations.get("monitored_list", language))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xB3151D35) else Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = channelInput,
                        onValueChange = { channelInput = it },
                        placeholder = { Text(Translations.get("chan_placeholder", language), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AddLink, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (channelInput.isNotBlank()) {
                                viewModel.addCustomChannel(context, channelInput)
                                channelInput = ""
                            } else {
                                Toast.makeText(context, "Enter channel name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("add_chan", language), color = Color.White)
                    }
                }
            }
        }

        when (channelsState) {
            is UiState.Loading -> item { LoadingItem(accent) }
            is UiState.Error -> item { ErrorItem(channelsState.message) }
            is UiState.Success -> {
                val channels = channelsState.data
                if (channels.isEmpty()) {
                    item { EmptyItem(Translations.get("no_channels", language)) }
                } else {
                    items(channels, key = { "ch_${it.id}" }) { ch ->
                        ChannelRow(ch, darkMode, accent) { viewModel.deleteCustomChannel(context, ch.username) }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(Translations.get("sub_list", language))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xB3151D35) else Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = subUrlInput,
                        onValueChange = { subUrlInput = it },
                        placeholder = { Text(Translations.get("sub_url_placeholder", language), fontSize = 12.sp) },
                        label = { Text(Translations.get("sub_url_label", language), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subLabelInput,
                        onValueChange = { subLabelInput = it },
                        placeholder = { Text(Translations.get("sub_label_placeholder", language), fontSize = 12.sp) },
                        label = { Text(Translations.get("sub_label_label", language), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (subUrlInput.isNotBlank() && subLabelInput.isNotBlank()) {
                                viewModel.addSubscription(context, subUrlInput, subLabelInput)
                                subUrlInput = ""
                                subLabelInput = ""
                            } else {
                                Toast.makeText(context, "Fill both fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("add_sub", language), color = Color.White)
                    }
                }
            }
        }

        when (subscriptionsState) {
            is UiState.Loading -> item { LoadingItem(accent) }
            is UiState.Error -> item { ErrorItem(subscriptionsState.message) }
            is UiState.Success -> {
                val subs = subscriptionsState.data
                if (subs.isEmpty()) {
                    item { EmptyItem(Translations.get("no_subs", language)) }
                } else {
                    items(subs, key = { "sub_${it.id}" }) { sub ->
                        SubRow(sub, darkMode, accent) { viewModel.deleteSubscription(context, sub.url) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = Color.Gray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun LoadingItem(accent: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = accent, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorItem(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 11.sp, color = Color.Red)
    }
}

@Composable
private fun EmptyItem(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun ChannelRow(channel: SupabaseChannel, darkMode: Boolean, accent: Color, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xB3151D35).copy(alpha = 0.6f) else Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RssFeed, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("@${channel.username}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("ID: #${channel.id}", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SubRow(sub: SupabaseSubscription, darkMode: Boolean, accent: Color, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = if (darkMode) Color(0xB3151D35).copy(alpha = 0.6f) else Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLink, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(sub.remarks ?: "Subscription", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub.url, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}
