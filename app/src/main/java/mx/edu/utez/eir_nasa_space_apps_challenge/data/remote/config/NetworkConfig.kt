package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.config

import android.content.Context
import mx.edu.utez.eir_nasa_space_apps_challenge.data.auth.*
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.EirApiService
import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api.MockEirApiInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

enum class NetworkEnvironment {
    REAL,
    MOCK
}

data class NetworkConfiguration(
    val environment: NetworkEnvironment = NetworkEnvironment.REAL,
    val baseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.example.org"
    }
}

class EirNetworkModule private constructor(context: Context) {
    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences("eir_network_config_prefs", Context.MODE_PRIVATE)

    var currentConfig: NetworkConfiguration = loadConfig()
        private set

    val deviceIdManager: DeviceIdManager = DeviceIdManager.getInstance(appContext)
    val sessionManager: SessionManager = SessionManager.getInstance(appContext)

    @Volatile
    private var cachedOkHttpClient: OkHttpClient? = null

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    @Volatile
    private var cachedApiService: EirApiService? = null

    private fun loadConfig(): NetworkConfiguration {
        val envStr = prefs.getString(KEY_ENV, NetworkEnvironment.REAL.name) ?: NetworkEnvironment.REAL.name
        val env = try {
            NetworkEnvironment.valueOf(envStr)
        } catch (_: Exception) {
            NetworkEnvironment.REAL
        }
        val url = prefs.getString(KEY_BASE_URL, NetworkConfiguration.DEFAULT_BASE_URL)
            ?: NetworkConfiguration.DEFAULT_BASE_URL
        return NetworkConfiguration(environment = env, baseUrl = url)
    }

    @Synchronized
    fun updateConfiguration(environment: NetworkEnvironment, baseUrl: String) {
        currentConfig = NetworkConfiguration(environment = environment, baseUrl = baseUrl.trimEnd('/') + "/")
        prefs.edit()
            .putString(KEY_ENV, currentConfig.environment.name)
            .putString(KEY_BASE_URL, currentConfig.baseUrl)
            .apply()

        // Invalidate cached clients so new config takes effect
        cachedOkHttpClient = null
        cachedRetrofit = null
        cachedApiService = null
    }

    @Synchronized
    fun getOkHttpClient(): OkHttpClient {
        return cachedOkHttpClient ?: buildOkHttpClient().also { cachedOkHttpClient = it }
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        builder.addInterceptor(loggingInterceptor)

        if (currentConfig.environment == NetworkEnvironment.REAL) {
            // REAL environment:
            // 1. Add AuthInterceptor to attach Authorization header
            builder.addInterceptor(AuthInterceptor(sessionManager))

            // 2. Add TokenAuthenticator for automatic 401 token refresh with concurrency lock
            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            builder.authenticator(
                TokenAuthenticator(
                    sessionManager = sessionManager,
                    baseUrlProvider = { currentConfig.baseUrl },
                    refreshClient = refreshClient
                )
            )

            // CRITICAL (P01): In REAL mode, MockEirApiInterceptor is NEVER added!
            // No mock fallback on HTTP errors or connection failure.
        } else {
            // MOCK environment:
            // MockEirApiInterceptor handles requests locally with simulated OpenAPI responses
            builder.addInterceptor(MockEirApiInterceptor())
        }

        return builder.build()
    }

    @Synchronized
    fun getRetrofit(): Retrofit {
        return cachedRetrofit ?: Retrofit.Builder()
            .baseUrl(currentConfig.baseUrl)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build().also { cachedRetrofit = it }
    }

    @Synchronized
    fun getApiService(): EirApiService {
        return cachedApiService ?: getRetrofit().create(EirApiService::class.java).also {
            cachedApiService = it
        }
    }

    companion object {
        private const val KEY_ENV = "network_environment"
        private const val KEY_BASE_URL = "network_base_url"

        @Volatile
        private var INSTANCE: EirNetworkModule? = null

        fun getInstance(context: Context): EirNetworkModule {
            return INSTANCE ?: synchronized(this) {
                val instance = EirNetworkModule(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getApiService(context: Context): EirApiService {
            return getInstance(context).getApiService()
        }

        fun getSessionManager(context: Context): SessionManager {
            return getInstance(context).sessionManager
        }

        fun getDeviceIdManager(context: Context): DeviceIdManager {
            return getInstance(context).deviceIdManager
        }
    }
}
