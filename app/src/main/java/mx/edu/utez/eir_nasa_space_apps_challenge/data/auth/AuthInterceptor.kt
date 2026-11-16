package mx.edu.utez.eir_nasa_space_apps_challenge.data.auth

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Public endpoints: no auth header required
        if (isPublicEndpoint(path)) {
            return chain.proceed(request)
        }

        val token = sessionManager.getAccessToken()
        val authenticatedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }

    private fun isPublicEndpoint(path: String): Boolean {
        return path.endsWith("/api/v1/health") ||
                path.endsWith("/api/v1/auth/login") ||
                path.endsWith("/api/v1/auth/refresh")
    }
}
