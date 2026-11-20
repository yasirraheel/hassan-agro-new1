package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CustomServerApi {
    @GET("api/v1/health")
    suspend fun health(): HealthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): retrofit2.Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun me(): retrofit2.Response<MeResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("api/v1/sync/push")
    suspend fun pushSync(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun pullSync(@Query("since") since: Long): SyncPullResponse
}

object CustomServerClientFactory {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun buildOkHttpClient(apiToken: String, deviceId: String): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Device-Id", deviceId)
            if (apiToken.isNotBlank()) {
                builder.header("Authorization", "Bearer $apiToken")
            }
            chain.proceed(builder.build())
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun createApi(baseUrl: String, apiToken: String, deviceId: String): CustomServerApi {
        val sanitized = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        val client = buildOkHttpClient(apiToken, deviceId)
        return Retrofit.Builder()
            .baseUrl(sanitized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CustomServerApi::class.java)
    }

    fun openRealtimeSyncSocket(
        wsUrl: String,
        apiToken: String,
        deviceId: String,
        onSyncSignal: () -> Unit,
        onStatusChanged: (Boolean) -> Unit
    ): WebSocket? {
        val trimmed = wsUrl.trim()
        if (!trimmed.startsWith("ws://") && !trimmed.startsWith("wss://")) return null
        val client = buildOkHttpClient(apiToken, deviceId)
        val request = Request.Builder().url(trimmed).build()
        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatusChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onSyncSignal()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onStatusChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStatusChanged(false)
            }
        })
    }
}
