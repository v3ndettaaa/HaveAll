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
import com.example.ui.Translations
import com.example.ui.UiState

@Composable
fun AdminPanel(
    viewModel: MainViewModel,
    channelsState: UiState<List<SupabaseChannel>>,
    subscriptionsState: UiState<List<SupabaseSubscription>>,
    language: AppLanguage,
    darkMode: Boolean
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    var channelInput by remember { mutableStateOf("") }
    var subUrlInput by remember { mutableStateOf("") }
    var subLabelInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Info banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(Translations.get("admin_desc", language),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(Translations.get("sync_interval", language),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primary)
                    }
                }
            }
        }

        // Channels section
        item { SectionLabel(Translations.get("monitored_list", language)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = channelInput,
                        onValueChange = { channelInput = it },
                        placeholder = { Text(Translations.get("chan_placeholder", language), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Tag, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (channelInput.isNotBlank()) {
                                viewModel.addCustomChannel(context, channelInput)
                                channelInput = ""
                            } else Toast.makeText(context, "Enter channel name", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Translations.get("add_chan", language))
                    }
                }
            }
        }

        when (channelsState) {
            is UiState.Loading -> item { CenteredLoader(primary) }
            is UiState.Error -> item { InlineError(channelsState.message) }
            is UiState.Success -> {
                if (channelsState.data.isEmpty()) {
                    item { EmptyLabel(Translations.get("no_channels", language)) }
                } else {
                    items(channelsState.data, key = { "ch_${it.id}" }) { ch ->
                        ChannelTile(ch, primary) { viewModel.deleteCustomChannel(context, ch.username) }
                    }
                }
            }
        }

        // Subscriptions section
        item { Spacer(Modifier.height(12.dp)) }
        item { SectionLabel(Translations.get("sub_list", language)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = subUrlInput,
                        onValueChange = { subUrlInput = it },
                        label = { Text(Translations.get("sub_url_label", language), fontSize = 12.sp) },
                        placeholder = { Text("https://...", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subLabelInput,
                        onValueChange = { subLabelInput = it },
                        label = { Text(Translations.get("sub_label_label", language), fontSize = 12.sp) },
                        placeholder = { Text(Translations.get("sub_label_placeholder", language), fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (subUrlInput.isNotBlank() && subLabelInput.isNotBlank()) {
                                viewModel.addSubscription(context, subUrlInput, subLabelInput)
                                subUrlInput = ""; subLabelInput = ""
                            } else Toast.makeText(context, "Fill both fields", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Translations.get("add_sub", language))
                    }
                }
            }
        }

        when (subscriptionsState) {
            is UiState.Loading -> item { CenteredLoader(primary) }
            is UiState.Error -> item { InlineError(subscriptionsState.message) }
            is UiState.Success -> {
                if (subscriptionsState.data.isEmpty()) {
                    item { EmptyLabel(Translations.get("no_subs", language)) }
                } else {
                    items(subscriptionsState.data, key = { "sub_${it.id}" }) { sub ->
                        SubTile(sub, primary) { viewModel.deleteSubscription(context, sub.url) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CenteredLoader(primary: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(22.dp), color = primary, strokeWidth = 2.dp)
    }
}

@Composable
private fun InlineError(message: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChannelTile(channel: SupabaseChannel, primary: androidx.compose.ui.graphics.Color, onDelete: () -> Unit) {
    ListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        headlineContent = {
            Text("@${channel.username}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        },
        supportingContent = {
            Text("ID #${channel.id}", fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(Icons.Default.RssFeed, null, tint = primary, modifier = Modifier.size(20.dp))
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Delete",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}

@Composable
private fun SubTile(sub: SupabaseSubscription, primary: androidx.compose.ui.graphics.Color, onDelete: () -> Unit) {
    ListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        headlineContent = {
            Text(sub.remarks ?: "Subscription", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(sub.url, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Icon(Icons.Default.Link, null, tint = primary, modifier = Modifier.size(20.dp))
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Delete",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
}
