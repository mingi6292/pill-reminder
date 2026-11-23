package com.mg.pillreminder.voice.gemini

import com.mg.pillreminder.domain.voice.VoiceIntent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class GeminiVoiceIntentParserTest {

    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GeminiApi.json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GeminiApi::class.java)
    private val parser = GeminiVoiceIntentParser(api, apiKey = "test-key")

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `모델이 낸 JSON을 VoiceIntent로 바꾼다`() = runTest {
        server.enqueue(completed("""{"type":"schedule_after","medicine_name":null,"minutes":240,"yes":null}"""))

        val intent = parser.parse("4시간 뒤에")

        assertEquals(VoiceIntent.ScheduleAfter(null, 240), intent)
    }

    @Test
    fun `키를 헤더로 보내고 문장은 저장하지 않게 요청한다`() = runTest {
        server.enqueue(completed("""{"type":"unknown","medicine_name":null,"minutes":null,"yes":null}"""))

        parser.parse("생리통약 먹었어")

        val request = server.takeRequest()
        assertEquals("/v1beta/interactions", request.path)
        assertEquals("test-key", request.getHeader("x-goog-api-key"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("생리통약 먹었어", body.getValue("input").jsonPrimitive.content)
        assertEquals(GeminiApi.MODEL, body.getValue("model").jsonPrimitive.content)
        assertFalse(body.getValue("store").jsonPrimitive.boolean)
    }

    @Test
    fun `서버 오류면 Unknown`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(VoiceIntent.Unknown, parser.parse("생리통약 먹었어"))
    }

    @Test
    fun `완료되지 않은 응답이면 Unknown`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"failed","steps":[]}"""))

        assertEquals(VoiceIntent.Unknown, parser.parse("생리통약 먹었어"))
    }

    @Test
    fun `키가 없으면 요청하지 않고 Unknown`() = runTest {
        val noKey = GeminiVoiceIntentParser(api, apiKey = "")

        assertEquals(VoiceIntent.Unknown, noKey.parse("생리통약 먹었어"))
        assertEquals(0, server.requestCount)
    }

    private fun completed(outputJson: String): MockResponse {
        val escaped = Json.encodeToString(outputJson)
        return MockResponse().setBody(
            """{"status":"completed","steps":[{"type":"model_output","content":[{"type":"text","text":$escaped}]}]}""",
        )
    }
}
