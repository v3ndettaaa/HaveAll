package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConfig
import com.example.data.SupabaseProxy
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    darkMode: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppLanguage.EN) }
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    val configsState by viewModel.configsState.collectAsState()
    val proxiesState by viewModel.proxiesState.collectAsState()
    val channelsState by viewModel.channelsState.collectAsState()
    val subsState by viewModel.subscriptionsState.collectAsState()
    val currentUrl by viewModel.supabaseUrl.collectAsState()
    val currentKey by viewModel.supabaseKey.collectAsState()

    var newUrl by remember { mutableStateOf(currentUrl) }
    var newKey by remember { mutableStateOf(currentKey) }
    LaunchedEffect(currentUrl, currentKey) { newUrl = currentUrl; newKey = currentKey }

    val accent = if (darkMode) Color(0xFF00E5FF) else Color(0xFF2979FF)
    val bg = if (darkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val layoutDir = remember(language) { if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Cloud, null, tint = accent, modifier = Modifier.size(26.dp))
                                        Text(Translations.get("app_title", language), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Text(Translations.get("subtitle", language), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { language = if (language == AppLanguage.EN) AppLanguage.FA else AppLanguage.EN },
                                        modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.1f)).testTag("lang_toggle")
                                    ) {
                                        Icon(Icons.Default.Language, null, tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { onDarkThemeToggle(!darkMode) },
                                        modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.1f)).testTag("theme_toggle")
                                    ) {
                                        Icon(if (darkMode) Icons.Default.Brightness5 else Icons.Default.Brightness2, null, tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { showSettings = true },
                                        modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.1f)).testTag("settings_button")
                                    ) {
                                        Icon(Icons.Default.Settings, null, tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.08f)).padding(3.dp)
                            ) {
                                val tabs = listOf(
                                    Translations.get("configs", language),
                                    Translations.get("proxies", language),
                                    Translations.get("admin_panel", language)
                                )
                                tabs.forEachIndexed { index, title ->
                                    val selected = selectedTab == index
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                                            .background(if (selected) accent else Color.Transparent)
                                            .clickable { selectedTab = index }.padding(vertical = 9.dp)
                                            .testTag("tab_$index"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp,
                            color = if (darkMode) Color(0xFF0A0F1D) else MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Translations.get("active_sync", language), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind { drawRect(Brush.linearGradient(listOf(bg, bg))) }
                            .padding(paddingValues)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (slideInHorizontally { if (targetState > initialState) it else -it } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally { if (targetState > initialState) -it else it } + fadeOut(tween(250)))
                                    .using(SizeTransform(clip = false))
                            },
                            label = "tab",
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            when (tab) {
                                0 -> ConfigsTab(viewModel, configsState, darkMode, accent, language)
                                1 -> ProxiesTab(viewModel, proxiesState, darkMode, accent, language)
                                2 -> AdminPanel(viewModel, channelsState, subsState, language, darkMode)
                            }
                        }
                    }

                    if (showSettings) {
                        AlertDialog(
                            onDismissRequest = { showSettings = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Backup, null, tint = accent)
                                    Text(Translations.get("db_setup", language))
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text(Translations.get("dialog_desc", language), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    OutlinedTextField(value = newUrl, onValueChange = { newUrl = it }, label = { Text("Supabase URL") },
                                        leadingIcon = { Icon(Icons.Default.Link, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = newKey, onValueChange = { newKey = it }, label = { Text("API Key") },
                                        leadingIcon = { Icon(Icons.Default.VpnKey, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            },
                            confirmButton = {
                                Button(onClick = { viewModel.initializeCredentials(newUrl, newKey); showSettings = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                    Text(Translations.get("apply", language), color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSettings = false }) { Text(Translations.get("cancel", language)) }
                            },
                            containerColor = if (darkMode) Color(0xFF131A2E) else MaterialTheme.colorScheme.surface
                        )
                    }
                }

                if (showSplash) {
                    SplashScreen(darkMode = darkMode, onFinished = { showSplash = false })
                }
            }
        }
    }
}

@Composable
private fun ConfigsTab(viewModel: MainViewModel, state: UiState<List<SupabaseConfig>>, darkMode: Boolean, accent: Color, language: AppLanguage) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val shouldLoadMore = remember {
        derivedStateOf {
            val total = scrollState.layoutInfo.totalItemsCount
            val last = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 3 && total > 0 && !viewModel.isConfigEndReached
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) viewModel.loadNextConfigsPage() }

    Column(modifier = Modifier.fillMaxSize()) {
        TipCard(Translations.get("configs_tip", language), Icons.Default.Info, accent, darkMode)

        when (state) {
            is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
            is UiState.Error -> ErrorView(state.message) { viewModel.refreshData() }
            is UiState.Success -> {
                val configs = state.data
                if (configs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Translations.get("empty_db", language), color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(state = scrollState, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(configs, key = { _, c -> c.id }) { index, config ->
                            AnimatedItem(index) {
                                ConfigItemCard(config, darkMode,
                                    onCopy = { viewModel.copyToClipboard(context, "Config", config.raw_content) },
                                    onImport = { viewModel.connectHiddifyConfig(context, config.raw_content) }
                                )
                            }
                        }
                        if (!viewModel.isConfigEndReached) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = accent, strokeWidth = 2.dp) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxiesTab(viewModel: MainViewModel, state: UiState<List<SupabaseProxy>>, darkMode: Boolean, accent: Color, language: AppLanguage) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val shouldLoadMore = remember {
        derivedStateOf {
            val total = scrollState.layoutInfo.totalItemsCount
            val last = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 3 && total > 0 && !viewModel.isProxyEndReached
        }
    }
    LaunchedEffect(shouldLoadMore.value) { if (shouldLoadMore.value) viewModel.loadNextProxiesPage() }

    Column(modifier = Modifier.fillMaxSize()) {
        TipCard(Translations.get("proxies_tip", language), Icons.Default.OfflineBolt, accent, darkMode)

        when (state) {
            is UiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
            is UiState.Error -> ErrorView(state.message) { viewModel.refreshData() }
            is UiState.Success -> {
                val proxies = state.data
                if (proxies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Translations.get("empty_db", language), color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(state = scrollState, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(proxies, key = { _, p -> p.id }) { index, proxy ->
                            AnimatedItem(index) {
                                ProxyItemCard(proxy, darkMode,
                                    onCopy = { viewModel.copyToClipboard(context, "Proxy", proxy.tg_link) },
                                    onConnect = { viewModel.connectProxy(context, proxy.tg_link) }
                                )
                            }
                        }
                        if (!viewModel.isProxyEndReached) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = accent, strokeWidth = 2.dp) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipCard(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, darkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = Color(0xFFFF5252), modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun AnimatedItem(index: Int, content: @Composable () -> Unit) {
    if (index > 5) {
        content()
    } else {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 30L); visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 5 },
            exit = fadeOut(tween(100))
        ) { content() }
    }
}
