package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api

import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import retrofit2.Response
import retrofit2.http.*

interface EirApiService {
    @GET("/api/v1/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @POST("/api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<LoginResponseDto>

    @POST("/api/v1/auth/refresh")
    suspend fun refreshSession(
        @Body request: RefreshRequestDto
    ): Response<SessionTokensDto>

    @POST("/api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("/api/v1/users/me")
    suspend fun getMe(): Response<AstronautProfileDto>

    @GET("/api/v1/users/me/bootstrap")
    suspend fun getBootstrapPage(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<BootstrapPageDto>

    @GET("/api/v1/users/me/health-records")
    suspend fun getMyHealthRecords(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<HealthPageDto>

    @GET("/api/v1/users/me/wellbeing-checkins")
    suspend fun getMyWellbeingCheckins(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<WellbeingPageDto>

    @GET("/api/v1/users/me/alerts")
    suspend fun getMyAlerts(): Response<RiskAlertsResponseDto>

    @POST("/api/v1/sync/records")
    suspend fun pushRecords(
        @Body request: SyncBatchRequestDto
    ): Response<SyncBatchResponseDto>
}
