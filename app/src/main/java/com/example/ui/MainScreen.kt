package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

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

    val layoutDir = if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    val navItems = listOf(
        NavItem(Translations.get("configs", language), Icons.Default.Tune, Icons.Default.Tune),
        NavItem(Translations.get("proxies", language), Icons.Default.VpnKey, Icons.Default.VpnKey),
        NavItem(Translations.get("admin_panel", language), Icons.Default.AdminPanelSettings, Icons.Default.AdminPanelSettings)
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    Translations.get("app_title", language),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                )
                                Text(
                                    Translations.get("subtitle", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp))
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { language = if (language == AppLanguage.EN) AppLanguage.FA else AppLanguage.EN },
                                modifier = Modifier.testTag("lang_toggle")
                            ) {
                                Icon(Icons.Default.Language, "Language",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { onDarkThemeToggle(!darkMode) },
                                modifier = Modifier.testTag("theme_toggle")
                            ) {
                                Icon(
                                    if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    "Theme", tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(Icons.Default.Settings, "Settings",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        if (selectedTab == index) item.selectedIcon else item.icon,
                                        null
                                    )
                                },
                                label = { Text(item.label, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("tab_$index")
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (slideInHorizontally { if (targetState > initialState) it else -it } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally { if (targetState > initialState) -it else it } + fadeOut(tween(220)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "tab",
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) { tab ->
                    when (tab) {
                        0 -> ConfigsTab(viewModel, configsState, language)
                        1 -> ProxiesTab(viewModel, proxiesState, language)
                        2 -> AdminPanel(viewModel, channelsState, subsState, language, darkMode)
                        else -> {}
                    }
                }
            }

            // Settings dialog
            if (showSettings) {
                AlertDialog(
                    onDismissRequest = { showSettings = false },
                    icon = { Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text(Translations.get("db_setup", language)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(Translations.get("dialog_desc", language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = newUrl, onValueChange = { newUrl = it },
                                label = { Text("Supabase URL") },
                                leadingIcon = { Icon(Icons.Default.Link, null) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newKey, onValueChange = { newKey = it },
                                label = { Text("API Key") },
                                leadingIcon = { Icon(Icons.Default.Key, null) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.initializeCredentials(newUrl, newKey); showSettings = false },
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(Translations.get("apply", language)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettings = false }) {
                            Text(Translations.get("cancel", language))
                        }
                    }
                )
            }

            // Splash
            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut(tween(400))
            ) {
                SplashScreen(onFinished = { showSplash = false })
            }
        }
    }
}

@Composable
private fun ConfigsTab(viewModel: MainViewModel, state: UiState<List<SupabaseConfig>>, language: AppLanguage) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = scrollState.layoutInfo.totalItemsCount
            val last = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 3 && total > 0 && !viewModel.isConfigEndReached
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadNextConfigsPage() }

    when (state) {
        is UiState.Loading -> CenteredProgress(primary)
        is UiState.Error -> ErrorState(state.message) { viewModel.refreshData() }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyState(Translations.get("empty_db", language))
            } else {
                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { TipBanner(Translations.get("configs_tip", language), Icons.Default.Info, primary) }
                    itemsIndexed(state.data, key = { _, c -> c.id }) { index, config ->
                        AnimatedItem(index) {
                            ConfigItemCard(
                                config = config,
                                darkMode = false,
                                onCopy = { viewModel.copyToClipboard(context, "Config", config.raw_content) },
                                onImport = { viewModel.connectHiddifyConfig(context, config.raw_content) }
                            )
                        }
                    }
                    if (!viewModel.isConfigEndReached) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxiesTab(viewModel: MainViewModel, state: UiState<List<SupabaseProxy>>, language: AppLanguage) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = scrollState.layoutInfo.totalItemsCount
            val last = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= total - 3 && total > 0 && !viewModel.isProxyEndReached
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadNextProxiesPage() }

    when (state) {
        is UiState.Loading -> CenteredProgress(primary)
        is UiState.Error -> ErrorState(state.message) { viewModel.refreshData() }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                EmptyState(Translations.get("empty_db", language))
            } else {
                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { TipBanner(Translations.get("proxies_tip", language), Icons.Default.Bolt, primary) }
                    itemsIndexed(state.data, key = { _, p -> p.id }) { index, proxy ->
                        AnimatedItem(index) {
                            ProxyItemCard(
                                proxy = proxy,
                                darkMode = false,
                                onCopy = { viewModel.copyToClipboard(context, "Proxy", proxy.tg_link) },
                                onConnect = { viewModel.connectProxy(context, proxy.tg_link) }
                            )
                        }
                    }
                    if (!viewModel.isProxyEndReached) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = primary, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipBanner(text: String, icon: ImageVector, primary: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun CenteredProgress(primary: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = primary)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun AnimatedItem(index: Int, content: @Composable () -> Unit) {
    if (index > 5) {
        content()
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 40L); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 6 }
    ) { content() }
}
