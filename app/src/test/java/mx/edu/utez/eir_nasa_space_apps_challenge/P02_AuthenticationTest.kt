package mx.edu.utez.eir_nasa_space_apps_challenge

import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicInteger

class P02_AuthenticationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionStorage: InMemorySessionStorage
    private lateinit var sessionManager: SessionManager
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sessionStorage = InMemorySessionStorage()
        val deviceIdManager = DeviceIdManager.createForTesting("TEST-DEVICE-001")
        sessionManager = SessionManager.createForTesting(sessionStorage, deviceIdManager)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authInterceptor_addsBearerTokenToProtectedEndpoints() {
        sessionStorage.saveSession(
            accessToken = "initial_access_token",
            refreshToken = "initial_refresh_token",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T12:00:00Z",
            userId = "usr_001",
            astronautId = "ASTRO-01",
            username = "test_user",
            displayName = "Test User",
            missionId = "MISSION-1",
            profileVersion = 1
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(AstronautProfileDto("usr_001", "ASTRO-01", "test_user", "Test User", "MISSION-1", 1)))
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        val response = runBlocking {
            apiService.getMe()
        }

        assertTrue(response.isSuccessful)
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer initial_access_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun authInterceptor_doesNotAddBearerTokenToPublicEndpoints() {
        sessionStorage.saveSession(
            accessToken = "secret_token",
            refreshToken = "secret_refresh",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T12:00:00Z",
            userId = "usr_001",
            astronautId = "ASTRO-01",
            username = "test_user",
            displayName = "Test User",
            missionId = "MISSION-1",
            profileVersion = 1
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(HealthResponseDto("ok")))
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        runBlocking {
            apiService.getHealth()
        }

        val recordedRequest = mockWebServer.takeRequest()
        assertNull("Health endpoint must not receive Authorization header", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun tokenAuthenticator_transparentlyRenewsTokenOn401() {
        sessionStorage.saveSession(
            accessToken = "expired_token",
            refreshToken = "valid_refresh_token",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T10:00:00Z",
            userId = "usr_001",
            astronautId = "ASTRO-01",
            username = "test_user",
            displayName = "Test User",
            missionId = "MISSION-1",
            profileVersion = 1
        )

        val baseUrl = mockWebServer.url("/").toString()

        // 1. First request fails with 401
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"unauthorized","message":"Token expired"}""")
        )

        // 2. Refresh endpoint succeeds with rotated tokens
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    gson.toJson(
                        SessionTokensDto(
                            accessToken = "new_rotated_access_token",
                            refreshToken = "new_rotated_refresh_token",
                            tokenType = "Bearer",
                            expiresIn = 900
                        )
                    )
                )
        )

        // 3. Retried request with new token succeeds
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(AstronautProfileDto("usr_001", "ASTRO-01", "test_user", "Test User", "MISSION-1", 1)))
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(sessionManager, { baseUrl }))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        val response = runBlocking {
            apiService.getMe()
        }

        assertTrue(response.isSuccessful)
        assertEquals("new_rotated_access_token", sessionManager.getAccessToken())
        assertEquals("new_rotated_refresh_token", sessionManager.getRefreshToken())

        // Ensure online_authenticated_at is NOT overwritten by token rotation
        assertEquals("2026-11-15T10:00:00Z", sessionManager.getOnlineAuthenticatedAt())
    }

    @Test
    fun tokenAuthenticator_avoidsInfiniteLoopWhenRefreshFails() {
        sessionStorage.saveSession(
            accessToken = "expired_token",
            refreshToken = "revoked_refresh_token",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T10:00:00Z",
            userId = "usr_001",
            astronautId = "ASTRO-01",
            username = "test_user",
            displayName = "Test User",
            missionId = "MISSION-1",
            profileVersion = 1
        )

        val baseUrl = mockWebServer.url("/").toString()

        // 1. Protected call fails with 401
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"unauthorized","message":"Token expired"}""")
        )

        // 2. Refresh endpoint also fails with 401 (revoked session)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"unauthorized","message":"Refresh token revoked"}""")
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(sessionManager, { baseUrl }))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        val response = runBlocking {
            apiService.getMe()
        }

        assertEquals(401, response.code())
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getAccessToken())
    }

    @Test
    fun tokenAuthenticator_controlsConcurrentRefreshRequests() {
        sessionStorage.saveSession(
            accessToken = "old_token",
            refreshToken = "refresh_token_concurrent",
            tokenType = "Bearer",
            expiresIn = 900,
            onlineAuthenticatedAt = "2026-11-15T10:00:00Z",
            userId = "usr_001",
            astronautId = "ASTRO-01",
            username = "test_user",
            displayName = "Test User",
            missionId = "MISSION-1",
            profileVersion = 1
        )

        val refreshCallCount = AtomicInteger(0)

        mockWebServer.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                val path = request.path ?: ""
                val authHeader = request.getHeader("Authorization")

                return when {
                    path.endsWith("/api/v1/auth/refresh") -> {
                        refreshCallCount.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            gson.toJson(SessionTokensDto("new_shared_token", "new_shared_refresh", "Bearer", 900))
                        )
                    }
                    path.endsWith("/api/v1/users/me") -> {
                        if (authHeader == "Bearer old_token") {
                            MockResponse().setResponseCode(401).setBody("""{"code":"unauthorized"}""")
                        } else if (authHeader == "Bearer new_shared_token") {
                            MockResponse().setResponseCode(200).setBody(
                                gson.toJson(AstronautProfileDto("usr_001", "ASTRO-01", "test_user", "Test User", "MISSION-1", 1))
                            )
                        } else {
                            MockResponse().setResponseCode(401).setBody("""{"code":"unauthorized"}""")
                        }
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val baseUrl = mockWebServer.url("/").toString()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(sessionManager, { baseUrl }))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        runBlocking {
            val call1 = async { apiService.getMe() }
            val call2 = async { apiService.getMe() }
            val res1 = call1.await()
            val res2 = call2.await()

            assertTrue(res1.isSuccessful)
            assertTrue(res2.isSuccessful)
        }

        assertEquals(1, refreshCallCount.get())
        assertEquals("new_shared_token", sessionManager.getAccessToken())
    }
}
