package com.example.carry.network

import retrofit2.http.Body
import retrofit2.http.POST

interface CarryApiService {
    // Carry API Endpoints to be implemented in Step 4
    @POST("api/command")
    suspend fun sendCommand(@Body command: CarryCommandRequest): CarryCommandResponse
}

data class CarryCommandRequest(
    val command: String,
    val target: String?,
    val rawText: String
)

data class CarryCommandResponse(
    val success: Boolean,
    val message: String
)
