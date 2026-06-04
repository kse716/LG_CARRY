package com.example.carry.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class VoiceIntentRequest(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class VoiceIntentResponse(
    @Json(name = "text") val text: String,
    @Json(name = "label") val label: String,
    @Json(name = "confidence") val confidence: Double,
    @Json(name = "accepted") val accepted: Boolean,
    @Json(name = "intent") val intent: String,
    @Json(name = "targetTrayId") val targetTrayId: String?,
    @Json(name = "requiresConfirm") val requiresConfirm: Boolean = false,
    @Json(name = "confirmText") val confirmText: String? = null,
    @Json(name = "message") val message: String?
)

interface VoiceIntentApi {
    @POST("api/ai/voice-intent")
    suspend fun predictIntent(
        @Body request: VoiceIntentRequest
    ): Response<VoiceIntentResponse>
}
