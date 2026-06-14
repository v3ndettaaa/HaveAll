package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseChannel
import com.example.data.SupabaseConfig
import com.example.data.SupabaseProxy
import com.example.data.SupabaseSubscription
import com.example.ui.theme.FarsiTypography
import com.example.ui.theme.Typography

// Supported Languages Enum
enum class AppLanguage { EN, FA }

// Translucent Glass Colors
val GlassBackgroundLight = Color(0x99FFFFFF)
val GlassBorderLight = Color(0x33000000)
val NeonCyan = Color(0xFF00E5FF)
val ElectricBlue = Color(0xFF2979FF)
val SpaceDarkBg = Color(0xFF0A0F1D)
val GlassBackgroundDark = Color(0xB3151D35)
val GlassBorderDark = Color(0x3300E5FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    darkMode: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var activeLanguage by remember { mutableStateOf(AppLanguage.EN) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Configs, 1: Proxies, 2: Channel Admin
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    val configsState by viewModel.configsState.collectAsState()
    val proxiesState by viewModel.proxiesState.collectAsState()
    val channelsState by viewModel.channelsState.collectAsState()

    val currentUrl by viewModel.supabaseUrl.collectAsState()
    val currentKey by viewModel.supabaseKey.collectAsState()

    var newUrl by remember { mutableStateOf(currentUrl) }
    var newKey by remember { mutableStateOf(currentKey) }

    LaunchedEffect(currentUrl, currentKey) {
        newUrl = currentUrl
        newKey = currentKey
    }

    // Translation Dictionary
    val translation = remember(activeLanguage) {
        mapOf(
            "app_title" to if (activeLanguage == AppLanguage.EN) "HaveAll" else "همه برای تو",
            "subtitle" to if (activeLanguage == AppLanguage.EN) "Ultra low-latency network tunnels" else "تونل‌های دور زدن فیلترینگ با سرعت بالا",
            "configs" to if (activeLanguage == AppLanguage.EN) "Tunnel Configs" else "کانفیگ‌های تونل",
            "proxies" to if (activeLanguage == AppLanguage.EN) "MTProto Proxies" else "پروکسی‌های تلگرام",
            "admin_panel" to if (activeLanguage == AppLanguage.EN) "Admin space" else "پنل مدیریت",
            "db_setup" to if (activeLanguage == AppLanguage.EN) "Database Setup" else "تنظیمات دیتابیس",
            "dialog_desc" to if (activeLanguage == AppLanguage.EN) "Configure your Supabase endpoints to sync nodes in real time:" else "اطلاعات پایگاه داده سوپابیس را برای همگام‌سازی پروکسی‌ها وارد کنید:",
            "apply" to if (activeLanguage == AppLanguage.EN) "Apply Settings" else "ذخیره تغییرات",
            "cancel" to if (activeLanguage == AppLanguage.EN) "Cancel" else "انصراف",
            "configs_tip" to if (activeLanguage == AppLanguage.EN) "Single click copy. Click 'Import' to add directly into Hiddify subscription container." else "با کلیک ساده کانفیگ کپی می‌شود. برای اتصال مستقیم روی دکمه ورود به نرم‌افزار کلیک کنید.",
            "proxies_tip" to if (activeLanguage == AppLanguage.EN) "Connect dynamic MTProto fast nodes instantly to bypass telegram app restriction." else "پروکسی‌های پرسرعت و پایدار تلگرام را کلیک کرده و فوراً فیلترینگ را دور بزنید.",
            "copy_btn" to if (activeLanguage == AppLanguage.EN) "Copy Config" else "کپی کانفیگ",
            "import_btn" to if (activeLanguage == AppLanguage.EN) "Import URL" else "ورود به هیدیفای",
            "conn_proxy" to if (activeLanguage == AppLanguage.EN) "Connect (TG)" else "اتصال پروکسی",
            "active_sync" to if (activeLanguage == AppLanguage.EN) "Supabase connection online" else "اتصال مستقیم به سوپابیس برقرار است",
            "admin_desc" to if (activeLanguage == AppLanguage.EN) "Admin channels management pool. Adding custom channels automatically populates the scraper database." else "مدیریت کانال‌های رصد پروکسی. افزودن هر کانال در کمتر از ۳۰ دقیقه پروکسی‌های آن را استخراج می‌کند.",
            "chan_placeholder" to if (activeLanguage == AppLanguage.EN) "Monitored channel name (e.g. proxyfree)" else "آیدی کانال بدون @ (مثلا proxyfree)",
            "add_chan" to if (activeLanguage == AppLanguage.EN) "Add Channel" else "افزودن کانال",
            "monitored_list" to if (activeLanguage == AppLanguage.EN) "DATABASE MONITORED POOL" else "لیست کانال‌های تلگرامی تحت نظارت",
            "sync_interval" to if (activeLanguage == AppLanguage.EN) "Scraper sync interval: Every 30 minutes" else "دوره زمانی همگام‌سازی ربات: هر ۳۰ دقیقه خودکار",
            "empty_db" to if (activeLanguage == AppLanguage.EN) "No records detected. Run scraper on bot first." else "دیتابیس خالی است. ابتدا گزینه Force Scrape را در ربات تلگرام اجرا کنید.",
            "server" to if (activeLanguage == AppLanguage.EN) "Server" else "سرور",
            "port" to if (activeLanguage == AppLanguage.EN) "Port" else "پورت",
            "secret" to if (activeLanguage == AppLanguage.EN) "Secret" else "سکرت"
        )
    }

    // Dynamic Linear Gradient background representing neon stars
    val animatedGradient = remember(darkMode) {
        Brush.linearGradient(
            colors = if (darkMode) listOf(Color(0xFF000000), Color(0xFF060914), Color(0xFF000000))
                     else listOf(Color(0xFFF3F6FD), Color(0xFFE9EFFB), Color(0xFFE4ECF8))
        )
    }

    val dynamicTypography = remember(activeLanguage) {
        if (activeLanguage == AppLanguage.FA) FarsiTypography else Typography
    }

    val layoutDirection = remember(activeLanguage) {
        if (activeLanguage == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = dynamicTypography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .statusBarsPadding()
            ) {
                // Header details UI structure
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Clouds logo",
                                tint = if (darkMode) NeonCyan else ElectricBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = translation["app_title"] ?: "HaveAll",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = translation["subtitle"] ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Floating Glass actions controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language switcher button (Globe Icon)
                        IconButton(
                            onClick = {
                                activeLanguage = if (activeLanguage == AppLanguage.EN) AppLanguage.FA else AppLanguage.EN
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (darkMode) GlassBackgroundDark else GlassBackgroundLight)
                                .border(1.dp, if (darkMode) GlassBorderDark else GlassBorderLight, CircleShape)
                                .testTag("lang_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Toggle language",
                                tint = if (darkMode) NeonCyan else ElectricBlue
                            )
                        }

                        // Theme Mode Switcher
                        IconButton(
                            onClick = { onDarkThemeToggle(!darkMode) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (darkMode) GlassBackgroundDark else GlassBackgroundLight)
                                .border(1.dp, if (darkMode) GlassBorderDark else GlassBorderLight, CircleShape)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (darkMode) Icons.Default.Brightness5 else Icons.Default.Brightness2,
                                contentDescription = "Toggle theme",
                                tint = if (darkMode) NeonCyan else ElectricBlue
                            )
                        }

                        // Settings Icon
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (darkMode) GlassBackgroundDark else GlassBackgroundLight)
                                .border(1.dp, if (darkMode) GlassBorderDark else GlassBorderLight, CircleShape)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Database setup config",
                                tint = if (darkMode) NeonCyan else ElectricBlue
                            )
                        }
                    }
                }

                // 3 Pager/Switcher tabs: Configs, Proxies, Custom Admin Channels Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (darkMode) Color(0x3300E5FF) else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    val tabs = listOf(
                        translation["configs"] ?: "Configs",
                        translation["proxies"] ?: "Proxies",
                        translation["admin_panel"] ?: "Admin"
                    )

                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val bg = if (isSelected) (if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary) else Color.Transparent
                        val tc = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp)
                                .testTag("tab_button_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tc,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                color = if (darkMode) SpaceDarkBg else MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sync check icon",
                        tint = if (darkMode) NeonCyan else ElectricBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = translation["active_sync"] ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(animatedGradient)
                }
                .padding(paddingValues)
        ) {
            // View sections based on selected index with high-end transition animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300))
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> ConfigsListSection(viewModel, configsState, translation, darkMode)
                    1 -> ProxiesListSection(viewModel, proxiesState, translation, darkMode)
                    2 -> AdminPanelSection(viewModel, channelsState, translation, darkMode)
                }
            }
        }

        // Setup Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Database icon setup",
                            tint = if (darkMode) NeonCyan else ElectricBlue
                        )
                        Text(text = translation["db_setup"] ?: "")
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = translation["dialog_desc"] ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            label = { Text("Supabase URL") },
                            placeholder = { Text("https://your-proj.supabase.co") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("supabase_url_input")
                        )

                        OutlinedTextField(
                            value = newKey,
                            onValueChange = { newKey = it },
                            label = { Text("API Key") },
                            placeholder = { Text("public-anon-key") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("supabase_key_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.initializeCredentials(newUrl, newKey)
                            showSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("apply_settings_button")
                    ) {
                        Text(translation["apply"] ?: "", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text(translation["cancel"] ?: "", color = MaterialTheme.colorScheme.secondary)
                    }
                },
                containerColor = if (darkMode) Color(0xFF131A2E) else MaterialTheme.colorScheme.surface
            )
        }
    }

        if (showSplash) {
            val scale by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = tween(700),
                label = "alpha"
            )
            
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val animatedPulseScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            val rotationDegrees by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (darkMode) SpaceDarkBg else Color(0xFFF3F6FD))
                    .clickable(enabled = false) {}
                    .testTag("splash_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer(
                        scaleX = scale * animatedPulseScale,
                        scaleY = scale * animatedPulseScale,
                        alpha = alpha
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(rotationZ = rotationDegrees)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(NeonCyan, Color(0xFF9061F9), NeonCyan)
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "HaveAll Logo",
                                tint = if (darkMode) NeonCyan else ElectricBlue,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Text(
                        text = "HAVEALL",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = if (darkMode) Color.White else Color(0xFF0A0F1D)
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = if (activeLanguage == AppLanguage.EN) "ALL TUNNEL CONFIGS FOR YOU" else "همه برای تو",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = if (darkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF0A0F1D).copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = if (darkMode) NeonCyan else ElectricBlue,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}
}
}

// Configs Section
@Composable
fun ConfigsListSection(
    viewModel: MainViewModel,
    state: UiState<List<SupabaseConfig>>,
    translation: Map<String, String>,
    darkMode: Boolean
) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 3 && totalItems > 0 && !viewModel.isConfigEndReached
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextConfigsPage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkMode) Color(0x1A00E5FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (darkMode) NeonCyan else ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = translation["configs_tip"] ?: "",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (darkMode) NeonCyan else ElectricBlue)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(state.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry Sync")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val configs = state.data
                if (configs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(translation["empty_db"] ?: "", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                    }
                    return
                }

                // Generates pages of 20 configs each automatically
                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = configs,
                        key = { _, config -> config.id }
                    ) { index, config ->
                        AnimatedItemWrapper(index = index) {
                            ConfigItemCard(
                                config = config,
                                translation = translation,
                                darkMode = darkMode,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("V2Ray Config", config.raw_content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Config copied!", Toast.LENGTH_SHORT).show()
                                },
                                onConnect = {
                                    viewModel.connectHiddifyConfig(context, config.raw_content)
                                }
                            )
                        }
                    }

                    if (!viewModel.isConfigEndReached) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (darkMode) NeonCyan else ElectricBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItemCard(
    config: SupabaseConfig,
    translation: Map<String, String>,
    darkMode: Boolean,
    onCopy: () -> Unit,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, if (darkMode) Color(0x3300E5FF) else Color(0x1F000000), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) GlassBackgroundDark else Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (darkMode) Color(0x1A00E5FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = config.type.uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) NeonCyan else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "ID: #${config.id}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Raw address
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (darkMode) Color(0xFF0F1524) else Color(0xFFF1F4FA))
                    .clickable { onCopy() }
                    .padding(10.dp)
            ) {
                Text(
                    text = config.raw_content,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            if (!config.remarks.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = config.remarks, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(translation["copy_btn"] ?: "Copy", fontSize = 11.sp)
                }

                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(translation["import_btn"] ?: "Import", fontSize = 11.sp)
                }
            }
        }
    }
}

// Proxies Section
@Composable
fun ProxiesListSection(
    viewModel: MainViewModel,
    state: UiState<List<SupabaseProxy>>,
    translation: Map<String, String>,
    darkMode: Boolean
) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 3 && totalItems > 0 && !viewModel.isProxyEndReached
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextProxiesPage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkMode) Color(0x1A00E5FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = null,
                    tint = if (darkMode) NeonCyan else ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = translation["proxies_tip"] ?: "",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (darkMode) NeonCyan else ElectricBlue)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(state.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry Sync")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val proxies = state.data
                if (proxies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(translation["empty_db"] ?: "", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                    }
                    return
                }

                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = proxies,
                        key = { _, proxy -> proxy.id }
                    ) { index, proxy ->
                        AnimatedItemWrapper(index = index) {
                            ProxyItemCard(
                                proxy = proxy,
                                translation = translation,
                                darkMode = darkMode,
                                onConnect = {
                                    viewModel.connectProxy(context, proxy.tg_link)
                                }
                            )
                        }
                    }

                    if (!viewModel.isProxyEndReached) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (darkMode) NeonCyan else ElectricBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyItemCard(
    proxy: SupabaseProxy,
    translation: Map<String, String>,
    darkMode: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, if (darkMode) Color(0x3300E5FF) else Color(0x1F000000), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) GlassBackgroundDark else Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapCalls,
                        contentDescription = "Proxy Type",
                        tint = if (darkMode) NeonCyan else ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MTProto Proxy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (darkMode) Color(0x1A00E5FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PID #${proxy.id}",
                        color = if (darkMode) NeonCyan else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (darkMode) Color(0xFF0F1524) else Color(0xFFF1F4FA))
                    .padding(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = translation["server"] ?: "Server", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(text = proxy.server, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = translation["port"] ?: "Port", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(text = proxy.port.toString(), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = translation["secret"] ?: "Secret", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Text(text = proxy.secret, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(translation["conn_proxy"] ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Custom Channels Admin panel
@Composable
fun AdminPanelSection(
    viewModel: MainViewModel,
    state: UiState<List<SupabaseChannel>>,
    translation: Map<String, String>,
    darkMode: Boolean
) {
    val context = LocalContext.current
    var inputChannelName by remember { mutableStateOf("") }

    val subsState by viewModel.subscriptionsState.collectAsState()
    var inputSubUrl by remember { mutableStateOf("") }
    var inputSubRemarks by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Info / Status Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkMode) Color(0x1A00E5FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = if (darkMode) NeonCyan else ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = translation["admin_panel"] ?: "Admin",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = translation["admin_desc"] ?: "Manage pools and directories dynamically.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = translation["sync_interval"] ?: "Sync Interval: Every 30 minutes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) NeonCyan else ElectricBlue
                    )
                }
            }
        }

        // 2. Add Monitored Channel Form
        item {
            Text(
                text = "REGISTER CHANNEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) GlassBackgroundDark else Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = inputChannelName,
                        onValueChange = { inputChannelName = it },
                        placeholder = { Text(text = translation["chan_placeholder"] ?: "bypassfilter (without @)") },
                        leadingIcon = { Icon(Icons.Default.AddLink, null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (darkMode) NeonCyan else ElectricBlue
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_channel_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (inputChannelName.isNotBlank()) {
                                viewModel.addCustomChannel(context, inputChannelName)
                                inputChannelName = ""
                            } else {
                                Toast.makeText(context, "Input cannot be empty!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("add_channel_submit")
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = translation["add_chan"] ?: "Add Channel")
                    }
                }
            }
        }

        // 3. Monitored Channels Display Header
        item {
            Text(
                text = translation["monitored_list"] ?: "Monitored Telegram Channels Pool",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }

        // 4. Monitored Channels List
        when (state) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = if (darkMode) NeonCyan else ElectricBlue, modifier = Modifier.size(24.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = state.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            is UiState.Success -> {
                val channels = state.data
                if (channels.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = "No custom channels added currently.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = channels,
                        key = { channel -> "ch_${channel.id}" }
                    ) { channel ->
                        ChannelItemCard(
                            channel = channel,
                            darkMode = darkMode,
                            onDelete = {
                                viewModel.deleteCustomChannel(context, channel.username)
                            }
                        )
                    }
                }
            }
        }

        // 5. Add VPN Subscription Link Form
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "REGISTER SUBSCRIPTION POOL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = if (darkMode) GlassBackgroundDark else Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = inputSubUrl,
                        onValueChange = { inputSubUrl = it },
                        placeholder = { Text(text = "https://raw.githubusercontent.com/...txt") },
                        label = { Text("Subscription Link URL") },
                        leadingIcon = { Icon(Icons.Default.AddLink, null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (darkMode) NeonCyan else ElectricBlue
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_input_url")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputSubRemarks,
                        onValueChange = { inputSubRemarks = it },
                        placeholder = { Text(text = "Russian Pools / Star Pools") },
                        label = { Text("Service Remarks / Label") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (darkMode) NeonCyan else ElectricBlue
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_input_remarks")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (inputSubUrl.isNotBlank() && inputSubRemarks.isNotBlank()) {
                                viewModel.addSubscription(context, inputSubUrl, inputSubRemarks)
                                inputSubUrl = ""
                                inputSubRemarks = ""
                            } else {
                                Toast.makeText(context, "URL and Remarks cannot be empty!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (darkMode) ElectricBlue else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_submit")
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Subscription")
                    }
                }
            }
        }

        // 6. Subscription Links Display Header
        item {
            Text(
                text = "Active Subscription Repositories",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }

        // 7. Subscription Links List
        when (val subState = subsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = if (darkMode) NeonCyan else ElectricBlue, modifier = Modifier.size(24.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = subState.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            is UiState.Success -> {
                val subs = subState.data
                if (subs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = "No external subscription links added yet.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = subs,
                        key = { sub -> "sub_${sub.id}" }
                    ) { sub ->
                        SubscriptionItemCard(
                            subscription = sub,
                            darkMode = darkMode,
                            onDelete = {
                                viewModel.deleteSubscription(context, sub.url)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionItemCard(
    subscription: SupabaseSubscription,
    darkMode: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) GlassBackgroundDark.copy(alpha = 0.6f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddLink,
                    contentDescription = null,
                    tint = if (darkMode) NeonCyan else ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = subscription.remarks ?: "Subscription URL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subscription.url,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_sub_${subscription.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChannelItemCard(
    channel: SupabaseChannel,
    darkMode: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) GlassBackgroundDark.copy(alpha = 0.6f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = null,
                    tint = if (darkMode) NeonCyan else ElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "@${channel.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "ID: #${channel.id}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_channel_${channel.username}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedItemWrapper(
    index: Int,
    content: @Composable () -> Unit
) {
    if (index > 5) {
        content()
    } else {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(index * 30L)
            visible = true
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it / 5 },
            exit = fadeOut(animationSpec = tween(100))
        ) {
            content()
        }
    }
}
