package com.mg.pillreminder.di

import android.util.Log
import com.mg.pillreminder.BuildConfig
import com.mg.pillreminder.domain.voice.VoiceIntentParser
import com.mg.pillreminder.voice.gemini.GeminiApi
import com.mg.pillreminder.voice.gemini.GeminiVoiceIntentParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideGeminiApi(): GeminiApi {
        // 사용자가 말하고 기다리는 중이라 오래 붙잡지 않고 실패로 보고 다시 말하게 한다.
        val client = OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(GeminiApi.BASE_URL)
            .client(client)
            .addConverterFactory(GeminiApi.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }

    @Provides
    fun provideVoiceIntentParser(api: GeminiApi): VoiceIntentParser =
        GeminiVoiceIntentParser(api, BuildConfig.GEMINI_API_KEY) { Log.w("GeminiVoiceIntent", "request failed", it) }
}
