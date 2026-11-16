package mx.edu.utez.eir_nasa_space_apps_challenge.data.auth

import com.google.gson.Gson
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.RefreshRequestDto
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.SessionTokensDto
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val baseUrlProvider: () -> String,
    private val refreshClient: OkHttpClient = OkHttpClient.Builder().build()
) : Authenticator {

    private val refreshLock = Any()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        // Do not authenticate refresh or login requests to avoid loops
        if (path.endsWith("/api/v1/auth/refresh") || path.endsWith("/api/v1/auth/login")) {
            return null
        }

        // Avoid infinite renewal loops
        if (responseCount(response) >= 2) {
            return null
        }

        synchronized(refreshLock) {
            val currentToken = sessionManager.getAccessToken()
            val requestHeader = response.request.header("Authorization")
            val currentBearer = if (currentToken != null) "Bearer $currentToken" else null

            // If another concurrent thread has already refreshed the token,
            // retry this request immediately with the new token
            if (currentBearer != null && currentBearer != requestHeader) {
                return response.request.newBuilder()
                    .header("Authorization", currentBearer)
                    .build()
            }

            val refreshToken = sessionManager.getRefreshToken()
            val deviceId = sessionManager.getDeviceId()

            if (refreshToken.isNullOrBlank() || deviceId.isBlank()) {
                sessionManager.clearSession()
                return null
            }

            // Perform refresh request synchronously
            val refreshPayload = gson.toJson(
                RefreshRequestDto(
                    refreshToken = refreshToken,
                    deviceId = deviceId
                )
            )

            val refreshUrl = baseUrlProvider().trimEnd('/') + "/api/v1/auth/refresh"
            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(refreshPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val refreshResponse = try {
                refreshClient.newCall(refreshRequest).execute()
            } catch (_: Exception) {
                // Network error during refresh: do not clear session, return null to propagate error
                return null
            }

            refreshResponse.use { resp ->
                if (resp.isSuccessful) {
                    val bodyString = resp.body?.string() ?: return null
                    val tokens = try {
                        gson.fromJson(bodyString, SessionTokensDto::class.java)
                    } catch (_: Exception) {
                        return null
                    }

                    sessionManager.updateTokens(tokens)

                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokens.accessToken}")
                        .build()
                } else {
                    // Revoked or expired refresh token (e.g. 401)
                    sessionManager.clearSession()
                    return null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
