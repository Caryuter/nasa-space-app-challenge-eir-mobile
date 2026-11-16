package mx.edu.utez.eir_nasa_space_apps_challenge.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SessionStorage {
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        expiresIn: Int,
        onlineAuthenticatedAt: String,
        userId: String,
        astronautId: String,
        username: String,
        displayName: String,
        missionId: String,
        profileVersion: Int
    )

    fun updateTokens(accessToken: String, refreshToken: String, expiresIn: Int)
    fun clearSession()

    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getTokenType(): String
    fun getExpiresIn(): Int
    fun getTokenAcquiredAt(): Long
    fun getOnlineAuthenticatedAt(): String?
    fun getUserId(): String?
    fun getAstronautId(): String?
    fun getUsername(): String?
    fun getDisplayName(): String?
    fun getMissionId(): String?
    fun getProfileVersion(): Int
}

class EncryptedSessionStorage(context: Context) : SessionStorage {
    private val prefs: SharedPreferences = createEncryptedOrFallbackPrefs(context)

    private fun createEncryptedOrFallbackPrefs(ctx: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(ctx.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                ctx.applicationContext,
                "eir_secure_session_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            // Fallback for JVM unit tests or unsupported hardware keystore
            ctx.applicationContext.getSharedPreferences("eir_session_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        expiresIn: Int,
        onlineAuthenticatedAt: String,
        userId: String,
        astronautId: String,
        username: String,
        displayName: String,
        missionId: String,
        profileVersion: Int
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_TOKEN_TYPE, tokenType)
            .putInt(KEY_EXPIRES_IN, expiresIn)
            .putLong(KEY_TOKEN_ACQUIRED_AT, System.currentTimeMillis())
            .putString(KEY_ONLINE_AUTHENTICATED_AT, onlineAuthenticatedAt)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ASTRONAUT_ID, astronautId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_MISSION_ID, missionId)
            .putInt(KEY_PROFILE_VERSION, profileVersion)
            .apply()
    }

    override fun updateTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putInt(KEY_EXPIRES_IN, expiresIn)
            .putLong(KEY_TOKEN_ACQUIRED_AT, System.currentTimeMillis())
            // Note: online_authenticated_at is deliberately NOT updated on refresh,
            // respecting project requirements: "La renovación de tokens no debe interpretarse como
            // una nueva autenticación online que amplíe automáticamente la ventana offline."
            .apply()
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_EXPIRES_IN)
            .remove(KEY_TOKEN_ACQUIRED_AT)
            .remove(KEY_ONLINE_AUTHENTICATED_AT)
            .remove(KEY_USER_ID)
            .remove(KEY_ASTRONAUT_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_MISSION_ID)
            .remove(KEY_PROFILE_VERSION)
            .apply()
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    override fun getTokenType(): String = prefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
    override fun getExpiresIn(): Int = prefs.getInt(KEY_EXPIRES_IN, 900)
    override fun getTokenAcquiredAt(): Long = prefs.getLong(KEY_TOKEN_ACQUIRED_AT, 0L)
    override fun getOnlineAuthenticatedAt(): String? = prefs.getString(KEY_ONLINE_AUTHENTICATED_AT, null)
    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    override fun getAstronautId(): String? = prefs.getString(KEY_ASTRONAUT_ID, null)
    override fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    override fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)
    override fun getMissionId(): String? = prefs.getString(KEY_MISSION_ID, null)
    override fun getProfileVersion(): Int = prefs.getInt(KEY_PROFILE_VERSION, 1)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_TOKEN_ACQUIRED_AT = "token_acquired_at"
        private const val KEY_ONLINE_AUTHENTICATED_AT = "online_authenticated_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ASTRONAUT_ID = "astronaut_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_MISSION_ID = "mission_id"
        private const val KEY_PROFILE_VERSION = "profile_version"
    }
}

class InMemorySessionStorage : SessionStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var tokenType: String = "Bearer"
    private var expiresIn: Int = 900
    private var tokenAcquiredAt: Long = 0L
    private var onlineAuthenticatedAt: String? = null
    private var userId: String? = null
    private var astronautId: String? = null
    private var username: String? = null
    private var displayName: String? = null
    private var missionId: String? = null
    private var profileVersion: Int = 1

    override fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        expiresIn: Int,
        onlineAuthenticatedAt: String,
        userId: String,
        astronautId: String,
        username: String,
        displayName: String,
        missionId: String,
        profileVersion: Int
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.tokenType = tokenType
        this.expiresIn = expiresIn
        this.tokenAcquiredAt = System.currentTimeMillis()
        this.onlineAuthenticatedAt = onlineAuthenticatedAt
        this.userId = userId
        this.astronautId = astronautId
        this.username = username
        this.displayName = displayName
        this.missionId = missionId
        this.profileVersion = profileVersion
    }

    override fun updateTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresIn = expiresIn
        this.tokenAcquiredAt = System.currentTimeMillis()
    }

    override fun clearSession() {
        accessToken = null
        refreshToken = null
        onlineAuthenticatedAt = null
        userId = null
        astronautId = null
        username = null
        displayName = null
        missionId = null
        tokenAcquiredAt = 0L
    }

    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken
    override fun getTokenType(): String = tokenType
    override fun getExpiresIn(): Int = expiresIn
    override fun getTokenAcquiredAt(): Long = tokenAcquiredAt
    override fun getOnlineAuthenticatedAt(): String? = onlineAuthenticatedAt
    override fun getUserId(): String? = userId
    override fun getAstronautId(): String? = astronautId
    override fun getUsername(): String? = username
    override fun getDisplayName(): String? = displayName
    override fun getMissionId(): String? = missionId
    override fun getProfileVersion(): Int = profileVersion
}
