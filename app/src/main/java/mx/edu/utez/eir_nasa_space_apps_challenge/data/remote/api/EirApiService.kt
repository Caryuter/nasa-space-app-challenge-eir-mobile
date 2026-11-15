package mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.api

import mx.edu.utez.eir_nasa_space_apps_challenge.data.remote.dtos.*
import retrofit2.Response
import retrofit2.http.*

interface EirApiService {
    @GET("/api/v1/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @GET("/api/v1/users/me")
    suspend fun getMe(): Response<AstronautProfileDto>

    @GET("/api/v1/users/me/bootstrap")
    suspend fun getBootstrapPage(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<BootstrapPageDto>

    @GET("/api/v1/users/me/alerts")
    suspend fun getMyAlerts(): Response<RiskAlertsResponseDto>

    @POST("/api/v1/sync/records")
    suspend fun pushRecords(
        @Body request: SyncBatchRequestDto
    ): Response<SyncBatchResponseDto>
}
