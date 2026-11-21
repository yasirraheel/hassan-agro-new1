package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hassan_agro_prefs")

enum class ThemeAppearanceMode(val key: String, val label: String) {
    SYSTEM("SYSTEM", "System"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark");

    companion object {
        fun fromKey(key: String?): ThemeAppearanceMode =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: SYSTEM
    }
}

data class ServerSyncConfig(
    val serverBaseUrl: String = PreferencesStore.DEFAULT_SERVER_URL,
    val apiToken: String = "",
    val wsUrl: String = "",
    val deviceId: String = "",
    val autoSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L
) {
    val isConfigured: Boolean
        get() = serverBaseUrl.trim().startsWith("http://") || serverBaseUrl.trim().startsWith("https://")
}

data class AuthSession(
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val roleId: String = "",
    val roleName: String = "",
    val permissions: Set<String> = emptySet(),
    val allowedBusinessIds: Set<String> = emptySet(),
    val authToken: String = ""
) {
    fun hasPermission(permission: AppPermission): Boolean {
        if (!isLoggedIn) return false
        return permissions.contains(permission.key)
    }

    fun canAccessBusiness(businessId: String): Boolean {
        if (!isLoggedIn) return false
        return allowedBusinessIds.isEmpty() || allowedBusinessIds.contains(businessId)
    }
}

class PreferencesStore(private val context: Context) {
    companion object {
        const val DEFAULT_SERVER_URL = "https://ledger.hassanagro.com/"
    }

    private object Keys {
        val ACTIVE_BUSINESS_ID = stringPreferencesKey("active_business_id")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val API_TOKEN = stringPreferencesKey("api_token")
        val WS_URL = stringPreferencesKey("ws_url")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNC_TS = longPreferencesKey("last_sync_timestamp")

        // Server-authenticated admin session
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
        val AUTH_USER_NAME = stringPreferencesKey("auth_user_name")
        val AUTH_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val AUTH_ROLE_ID = stringPreferencesKey("auth_role_id")
        val AUTH_ROLE_NAME = stringPreferencesKey("auth_role_name")
        val AUTH_PERMISSIONS_JSON = stringPreferencesKey("auth_permissions_json")
        val AUTH_BUSINESS_IDS_JSON = stringPreferencesKey("auth_business_ids_json")
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
    }

    val appearanceModeFlow: Flow<ThemeAppearanceMode> = context.dataStore.data.map { prefs ->
        ThemeAppearanceMode.fromKey(prefs[Keys.APPEARANCE_MODE])
    }

    val activeBusinessIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_BUSINESS_ID] ?: AppDatabase.DEFAULT_BUSINESS_ID
    }

    val serverConfigFlow: Flow<ServerSyncConfig> = context.dataStore.data.map { prefs ->
        val rawUrl = prefs[Keys.SERVER_BASE_URL].orEmpty().trim()
        ServerSyncConfig(
            serverBaseUrl = rawUrl.ifBlank { DEFAULT_SERVER_URL },
            apiToken = prefs[Keys.API_TOKEN] ?: "",
            wsUrl = prefs[Keys.WS_URL] ?: "",
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            autoSyncEnabled = prefs[Keys.AUTO_SYNC] ?: true,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TS] ?: 0L
        )
    }

    val authSessionFlow: Flow<AuthSession> = context.dataStore.data.map { prefs ->
        val token = prefs[Keys.AUTH_TOKEN].orEmpty().trim()
        val userId = prefs[Keys.AUTH_USER_ID].orEmpty().trim()
        val loggedIn = token.isNotEmpty() && userId.isNotEmpty()
        AuthSession(
            isLoggedIn = loggedIn,
            userId = userId,
            userName = prefs[Keys.AUTH_USER_NAME] ?: "",
            userEmail = prefs[Keys.AUTH_USER_EMAIL] ?: "",
            roleId = prefs[Keys.AUTH_ROLE_ID] ?: "",
            roleName = prefs[Keys.AUTH_ROLE_NAME] ?: "",
            permissions = CustomizationJson.decodeList(prefs[Keys.AUTH_PERMISSIONS_JSON], emptyList()).toSet(),
            allowedBusinessIds = CustomizationJson.decodeList(prefs[Keys.AUTH_BUSINESS_IDS_JSON], emptyList()).toSet(),
            authToken = token
        )
    }

    suspend fun ensureDeviceId(): String {
        val existing = context.dataStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = "BM-${UUID.randomUUID().toString().take(8).uppercase()}"
        context.dataStore.edit { it[Keys.DEVICE_ID] = generated }
        return generated
    }

    suspend fun setActiveBusinessId(businessId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_BUSINESS_ID] = businessId }
    }

    suspend fun setAppearanceMode(mode: ThemeAppearanceMode) {
        context.dataStore.edit { it[Keys.APPEARANCE_MODE] = mode.key }
    }

    suspend fun saveServerUrlOnly(baseUrl: String) {
        val normalizedUrl = normalizeUrl(baseUrl)
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_BASE_URL] = normalizedUrl.ifBlank { DEFAULT_SERVER_URL }
        }
    }

    suspend fun saveServerConfig(
        baseUrl: String,
        apiToken: String,
        wsUrl: String,
        autoSyncEnabled: Boolean
    ) {
        val normalizedUrl = normalizeUrl(baseUrl)
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_BASE_URL] = normalizedUrl.ifBlank { DEFAULT_SERVER_URL }
            prefs[Keys.API_TOKEN] = apiToken.trim()
            prefs[Keys.WS_URL] = wsUrl.trim()
            prefs[Keys.AUTO_SYNC] = autoSyncEnabled
        }
    }

    suspend fun saveAuthSession(
        token: String,
        userId: String,
        userName: String,
        userEmail: String,
        roleId: String,
        roleName: String,
        permissions: List<String>,
        allowedBusinessIds: List<String>
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token.trim()
            prefs[Keys.AUTH_USER_ID] = userId.trim()
            prefs[Keys.AUTH_USER_NAME] = userName.trim()
            prefs[Keys.AUTH_USER_EMAIL] = userEmail.trim()
            prefs[Keys.AUTH_ROLE_ID] = roleId.trim()
            prefs[Keys.AUTH_ROLE_NAME] = roleName.trim()
            prefs[Keys.AUTH_PERMISSIONS_JSON] = CustomizationJson.encodeList(permissions)
            prefs[Keys.AUTH_BUSINESS_IDS_JSON] = CustomizationJson.encodeList(allowedBusinessIds)
            prefs[Keys.LAST_SYNC_TS] = 0L
        }
    }

    suspend fun updateAuthUser(
        userId: String,
        userName: String,
        userEmail: String,
        roleId: String,
        roleName: String,
        permissions: List<String>,
        allowedBusinessIds: List<String>
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_USER_ID] = userId.trim()
            prefs[Keys.AUTH_USER_NAME] = userName.trim()
            prefs[Keys.AUTH_USER_EMAIL] = userEmail.trim()
            prefs[Keys.AUTH_ROLE_ID] = roleId.trim()
            prefs[Keys.AUTH_ROLE_NAME] = roleName.trim()
            prefs[Keys.AUTH_PERMISSIONS_JSON] = CustomizationJson.encodeList(permissions)
            prefs[Keys.AUTH_BUSINESS_IDS_JSON] = CustomizationJson.encodeList(allowedBusinessIds)
        }
    }

    suspend fun addAllowedBusinessIdLocally(businessId: String) {
        context.dataStore.edit { prefs ->
            val current = CustomizationJson.decodeList(prefs[Keys.AUTH_BUSINESS_IDS_JSON], emptyList())
            if (current.isNotEmpty() && !current.contains(businessId)) {
                prefs[Keys.AUTH_BUSINESS_IDS_JSON] = CustomizationJson.encodeList(current + businessId)
            }
        }
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.AUTH_USER_ID)
            prefs.remove(Keys.AUTH_USER_NAME)
            prefs.remove(Keys.AUTH_USER_EMAIL)
            prefs.remove(Keys.AUTH_ROLE_ID)
            prefs.remove(Keys.AUTH_ROLE_NAME)
            prefs.remove(Keys.AUTH_PERMISSIONS_JSON)
            prefs.remove(Keys.AUTH_BUSINESS_IDS_JSON)
            prefs[Keys.LAST_SYNC_TS] = 0L
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TS] = timestamp }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isNotEmpty() && !trimmed.endsWith("/")) "$trimmed/" else trimmed
    }
}
