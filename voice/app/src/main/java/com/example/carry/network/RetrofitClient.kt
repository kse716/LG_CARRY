package com.example.carry.network

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://example.com/" // Placeholder service URL

    // PC 로컬 IP: 192.168.219.168 (실제 스마트폰 연결 테스트용)
    // 에뮬레이터 테스트용: "http://10.0.2.2:5000/"
    private const val LOCAL_API_BASE_URL = "http://192.168.219.168:5000/"

    val carryApi: CarryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CarryApiService::class.java)
    }

    val voiceIntentApi: VoiceIntentApi by lazy {
        Retrofit.Builder()
            .baseUrl(LOCAL_API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(VoiceIntentApi::class.java)
    }
}
