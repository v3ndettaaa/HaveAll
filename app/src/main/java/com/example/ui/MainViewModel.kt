package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel : ViewModel() {

    private val _supabaseUrl = MutableStateFlow("")
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseKey = MutableStateFlow("")
    val supabaseKey: StateFlow<String> = _supabaseKey.asStateFlow()

    private val _proxiesState = MutableStateFlow<UiState<List<SupabaseProxy>>>(UiState.Loading)
    val proxiesState: StateFlow<UiState<List<SupabaseProxy>>> = _proxiesState.asStateFlow()

    private val _configsState = MutableStateFlow<UiState<List<SupabaseConfig>>>(UiState.Loading)
    val configsState: StateFlow<UiState<List<SupabaseConfig>>> = _configsState.asStateFlow()

    private val _channelsState = MutableStateFlow<UiState<List<SupabaseChannel>>>(UiState.Loading)
    val channelsState: StateFlow<UiState<List<SupabaseChannel>>> = _channelsState.asStateFlow()

    private val _subscriptionsState = MutableStateFlow<UiState<List<SupabaseSubscription>>>(UiState.Loading)
    val subscriptionsState: StateFlow<UiState<List<SupabaseSubscription>>> = _subscriptionsState.asStateFlow()

    private var proxiesOffset = 0
    private var configsOffset = 0
    private val limit = 20

    private val loadedProxies = mutableListOf<SupabaseProxy>()
    private val loadedConfigs = mutableListOf<SupabaseConfig>()

    var isConfigEndReached = false
        private set
    var isProxyEndReached = false
        private set

    fun initializeCredentials(url: String, key: String) {
        _supabaseUrl.value = url.trim()
        _supabaseKey.value = key.trim()
        if (url.isNotEmpty() && key.isNotEmpty()) {
            refreshData()
        } else {
            val missingMsg = "Supabase credentials missing! Click settings to configure."
            _proxiesState.value = UiState.Error(missingMsg)
            _configsState.value = UiState.Error(missingMsg)
            _channelsState.value = UiState.Error(missingMsg)
            _subscriptionsState.value = UiState.Error(missingMsg)
        }
    }

    fun refreshData() {
        proxiesOffset = 0
        configsOffset = 0
        isConfigEndReached = false
        isProxyEndReached = false
        loadedProxies.clear()
        loadedConfigs.clear()
        loadNextProxiesPage()
        loadNextConfigsPage()
        loadMonitoredChannels()
        loadSubscriptions()
    }

    fun loadNextProxiesPage() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty() || isProxyEndReached) return

        viewModelScope.launch {
            if (proxiesOffset == 0) _proxiesState.value = UiState.Loading
            try {
                val api = RetrofitClient.createService(url)
                val response = api.getProxies(apiKey = key, authHeader = "Bearer $key", limit = limit, offset = proxiesOffset)
                if (response.isEmpty()) {
                    isProxyEndReached = true
                } else {
                    loadedProxies.addAll(response)
                    proxiesOffset += limit
                }
                _proxiesState.value = UiState.Success(loadedProxies.toList())
            } catch (e: Exception) {
                _proxiesState.value = UiState.Error("Failed to load proxies: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun loadNextConfigsPage() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty() || isConfigEndReached) return

        viewModelScope.launch {
            if (configsOffset == 0) _configsState.value = UiState.Loading
            try {
                val api = RetrofitClient.createService(url)
                val response = api.getConfigs(apiKey = key, authHeader = "Bearer $key", limit = limit, offset = configsOffset)
                if (response.isEmpty()) {
                    isConfigEndReached = true
                } else {
                    loadedConfigs.addAll(response)
                    configsOffset += limit
                }
                _configsState.value = UiState.Success(loadedConfigs.toList())
            } catch (e: Exception) {
                _configsState.value = UiState.Error("Failed to load configs: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun loadMonitoredChannels() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty()) return

        viewModelScope.launch {
            _channelsState.value = UiState.Loading
            try {
                val api = RetrofitClient.createService(url)
                val channels = api.getMonitoredChannels(apiKey = key, authHeader = "Bearer $key")
                _channelsState.value = UiState.Success(channels)
            } catch (e: Exception) {
                _channelsState.value = UiState.Error("Failed to load channels: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun addCustomChannel(context: Context, username: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        val cleanedName = username.replace("@", "").trim()
        if (url.isEmpty() || key.isEmpty() || cleanedName.isEmpty()) return

        viewModelScope.launch {
            try {
                val api = RetrofitClient.createService(url)
                api.addMonitoredChannel(apiKey = key, authHeader = "Bearer $key", request = AddChannelRequest(username = cleanedName))
                Toast.makeText(context, "Added @$cleanedName", Toast.LENGTH_SHORT).show()
                loadMonitoredChannels()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteCustomChannel(context: Context, username: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty() || username.isEmpty()) return

        viewModelScope.launch {
            try {
                val api = RetrofitClient.createService(url)
                api.deleteMonitoredChannel(apiKey = key, authHeader = "Bearer $key", username = "eq.$username")
                Toast.makeText(context, "Removed @$username", Toast.LENGTH_SHORT).show()
                loadMonitoredChannels()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to remove: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                loadMonitoredChannels()
            }
        }
    }

    fun loadSubscriptions() {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty()) return

        viewModelScope.launch {
            _subscriptionsState.value = UiState.Loading
            try {
                val api = RetrofitClient.createService(url)
                val subs = api.getSubscriptions(apiKey = key, authHeader = "Bearer $key")
                _subscriptionsState.value = UiState.Success(subs)
            } catch (e: Exception) {
                _subscriptionsState.value = UiState.Error("Failed to load subscriptions: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun addSubscription(context: Context, linkUrl: String, remarks: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty() || linkUrl.isEmpty()) return

        viewModelScope.launch {
            try {
                val api = RetrofitClient.createService(url)
                api.addSubscription(apiKey = key, authHeader = "Bearer $key", request = AddSubscriptionRequest(url = linkUrl, remarks = remarks))
                Toast.makeText(context, "Subscription added", Toast.LENGTH_SHORT).show()
                loadSubscriptions()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteSubscription(context: Context, linkUrl: String) {
        val url = _supabaseUrl.value
        val key = _supabaseKey.value
        if (url.isEmpty() || key.isEmpty() || linkUrl.isEmpty()) return

        viewModelScope.launch {
            try {
                val api = RetrofitClient.createService(url)
                api.deleteSubscription(apiKey = key, authHeader = "Bearer $key", url = "eq.$linkUrl")
                Toast.makeText(context, "Subscription removed", Toast.LENGTH_SHORT).show()
                loadSubscriptions()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to remove: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                loadSubscriptions()
            }
        }
    }

    fun connectProxy(context: Context, tgLink: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tgLink))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun connectHiddifyConfig(context: Context, rawConfig: String) {
        try {
            val uri = Uri.parse("hiddify://import/#$rawConfig")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Hiddify not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
    }
}
