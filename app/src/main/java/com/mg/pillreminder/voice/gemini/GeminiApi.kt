package com.mg.pillreminder.voice.gemini

import kotlinx.serialization.json.Json
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/** Gemini Interactions API. 공식 문서의 JSON 출력 예시가 이 API를 쓴다. */
interface GeminiApi {
    // 응답 형식이 바뀌지 않게 문서 예시와 같은 리비전에 고정한다.
    @Headers("Api-Revision: 2026-05-20")
    @POST("v1beta/interactions")
    suspend fun createInteraction(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: InteractionRequest,
    ): InteractionResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val MODEL = "gemini-3.5-flash-lite"

        // store=false, mime_type 같은 기본값도 보내야 해서 encodeDefaults를 켠다.
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
