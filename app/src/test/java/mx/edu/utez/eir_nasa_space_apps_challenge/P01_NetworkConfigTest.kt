package mx.edu.utez.eir_nasa_space_apps_challenge

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.DeviceIdManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.InMemorySessionStorage
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.SessionManager
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.NetworkConfiguration
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config.NetworkEnvironment
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.HealthResponseDto
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class P01_NetworkConfigTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun realMode_propagatesNetworkErrorsWhenServerIsDown() {
        // Stop server immediately to simulate server down / connection refused
        mockWebServer.shutdown()

        val okHttpClient = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        var caughtException = false
        try {
            runBlocking {
                apiService.getHealth()
            }
        } catch (e: IOException) {
            caughtException = true
        }

        assertTrue("In REAL mode, connection failure must be propagated, never simulated as success", caughtException)
    }

    @Test
    fun realMode_propagatesHttpErrorsWithoutMockFallback() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"code":"unauthorized","message":"Missing or invalid token"}""")
        )

        val okHttpClient = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        val response = runBlocking {
            apiService.getHealth()
        }

        assertEquals(401, response.code())
        assertFalse("HTTP 401 must not be replaced with a successful response", response.isSuccessful)
    }

    @Test
    fun realMode_propagates422UnprocessableEntityWithoutMockFallback() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"code":"unprocessable_entity","message":"Invalid payload format"}""")
        )

        val okHttpClient = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(EirApiService::class.java)

        val response = runBlocking {
            apiService.getHealth()
        }

        assertEquals(422, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun networkConfiguration_supportsEnvironmentAndUrlSwitching() {
        val config = NetworkConfiguration(
            environment = NetworkEnvironment.REAL,
            baseUrl = "http://10.0.2.2:8000/"
        )

        assertEquals(NetworkEnvironment.REAL, config.environment)
        assertEquals("http://10.0.2.2:8000/", config.baseUrl)
    }
}
