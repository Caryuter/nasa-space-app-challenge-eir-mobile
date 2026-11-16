package mx.edu.utez.eir_nasa_space_apps_challenge.data.auth

import android.content.Context
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.LoginResponseDto
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.SessionTokensDto

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val deviceIdManager: DeviceIdManager
) {
    fun isLoggedIn(): Boolean {
        return !sessionStorage.getAccessToken().isNullOrBlank()
    }

    fun getAccessToken(): String? = sessionStorage.getAccessToken()
    fun getRefreshToken(): String? = sessionStorage.getRefreshToken()
    fun getDeviceId(): String = deviceIdManager.getOrCreateDeviceId()
    fun getUserId(): String? = sessionStorage.getUserId()
    fun getAstronautId(): String? = sessionStorage.getAstronautId()
    fun getUsername(): String? = sessionStorage.getUsername()
    fun getDisplayName(): String? = sessionStorage.getDisplayName()
    fun getOnlineAuthenticatedAt(): String? = sessionStorage.getOnlineAuthenticatedAt()

    fun isAccessTokenExpired(bufferSeconds: Int = 30): Boolean {
        val acquiredAt = sessionStorage.getTokenAcquiredAt()
        if (acquiredAt == 0L) return true
        val expiresInMs = (sessionStorage.getExpiresIn() - bufferSeconds) * 1000L
        val elapsed = System.currentTimeMillis() - acquiredAt
        return elapsed >= expiresInMs
    }

    fun saveLoginSession(loginResponse: LoginResponseDto) {
        sessionStorage.saveSession(
            accessToken = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            tokenType = loginResponse.tokenType,
            expiresIn = loginResponse.expiresIn,
            onlineAuthenticatedAt = loginResponse.onlineAuthenticatedAt,
            userId = loginResponse.user.userId,
            astronautId = loginResponse.user.astronautId,
            username = loginResponse.user.username,
            displayName = loginResponse.user.displayName,
            missionId = loginResponse.user.missionId,
            profileVersion = loginResponse.user.profileVersion
        )
    }

    fun updateTokens(sessionTokens: SessionTokensDto) {
        sessionStorage.updateTokens(
            accessToken = sessionTokens.accessToken,
            refreshToken = sessionTokens.refreshToken,
            expiresIn = sessionTokens.expiresIn
        )
    }

    fun clearSession() {
        sessionStorage.clearSession()
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(
                    EncryptedSessionStorage(context.applicationContext),
                    DeviceIdManager.getInstance(context.applicationContext)
                )
                INSTANCE = instance
                instance
            }
        }

        fun createForTesting(storage: SessionStorage, deviceIdManager: DeviceIdManager): SessionManager {
            return SessionManager(storage, deviceIdManager)
        }
    }
}
