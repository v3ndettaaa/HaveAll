package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class SupabaseConfig(
    val id: Long = 0,
    val type: String = "vmess",
    val raw_content: String = "",
    val remarks: String? = null,
    val created_at: String = ""
)

data class SupabaseProxy(
    val id: Long = 0,
    val server: String = "",
    val port: Int = 0,
    val secret: String = "",
    val tg_link: String = "",
    val created_at: String = ""
)

data class SupabaseChannel(
    val id: Long = 0,
    val username: String = "",
    val created_at: String = ""
)

data class SupabaseSubscription(
    val id: Long = 0,
    val url: String = "",
    val remarks: String? = null,
    val created_at: String = ""
)

data class AddChannelRequest(val username: String)
data class AddSubscriptionRequest(val url: String, val remarks: String)

interface SupabaseApi {
    @GET("rest/v1/configs")
    suspend fun getConfigs(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseConfig>

    @GET("rest/v1/proxies")
    suspend fun getProxies(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseProxy>

    @GET("rest/v1/monitored_channels")
    suspend fun getMonitoredChannels(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "username.asc"
    ): List<SupabaseChannel>

    @POST("rest/v1/monitored_channels")
    suspend fun addMonitoredChannel(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: AddChannelRequest
    ): List<SupabaseChannel>

    @DELETE("rest/v1/monitored_channels")
    suspend fun deleteMonitoredChannel(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("username") username: String
    )

    @GET("rest/v1/subscriptions")
    suspend fun getSubscriptions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "remarks.asc"
    ): List<SupabaseSubscription>

    @POST("rest/v1/subscriptions")
    suspend fun addSubscription(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: AddSubscriptionRequest
    ): List<SupabaseSubscription>

    @DELETE("rest/v1/subscriptions")
    suspend fun deleteSubscription(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("url") url: String
    )
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val sharedClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var currentBaseUrl: String? = null
    @Volatile
    private var currentApi: SupabaseApi? = null

    fun createService(baseUrl: String): SupabaseApi {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        currentApi?.let { api ->
            if (currentBaseUrl == formattedUrl) return api
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = sharedClient.newBuilder()
            .addInterceptor(logging)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)

        currentBaseUrl = formattedUrl
        currentApi = api
        return api
    }
}
