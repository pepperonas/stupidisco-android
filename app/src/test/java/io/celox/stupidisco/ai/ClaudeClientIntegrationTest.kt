package io.celox.stupidisco.ai

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class ClaudeClientIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ClaudeClient

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = ClaudeClient(
            apiKey = "test-api-key",
            client = okHttpClient,
            baseUrl = server.url("/v1/messages").toString()
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun streamAnswer_parsesContentBlockDelta() = runTest {
        val sseBody = buildString {
            appendLine("event: content_block_delta")
            appendLine("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}")
            appendLine()
            appendLine("event: content_block_delta")
            appendLine("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\" World\"}}")
            appendLine()
            appendLine("event: message_stop")
            appendLine("data: {}")
            appendLine()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        val tokens = mutableListOf<String>()
        var completed = false

        client.streamAnswer(
            question = "Test question",
            onToken = { tokens.add(it) },
            onComplete = { completed = true },
            onError = { throw AssertionError("Unexpected error: $it") }
        )

        assertEquals(listOf("Hello", " World"), tokens)
        assertTrue(completed)
    }

    @Test
    fun streamAnswer_handlesMessageStopWithoutDelta() = runTest {
        val sseBody = buildString {
            appendLine("event: message_stop")
            appendLine("data: {}")
            appendLine()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        var completed = false
        client.streamAnswer(
            question = "Test",
            onToken = {},
            onComplete = { completed = true },
            onError = { throw AssertionError("Unexpected error: $it") }
        )

        assertTrue(completed)
    }

    @Test
    fun streamAnswer_handlesErrorEvent() = runTest {
        val sseBody = buildString {
            appendLine("event: error")
            appendLine("data: {\"error\":{\"type\":\"rate_limit_error\",\"message\":\"Rate limited\"}}")
            appendLine()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        var errorMsg = ""
        client.streamAnswer(
            question = "Test",
            onToken = {},
            onComplete = {},
            onError = { errorMsg = it }
        )

        assertTrue("Expected error containing 'Rate limited', got: '$errorMsg'",
            errorMsg.contains("Rate limited") || errorMsg.contains("Claude"))
    }

    @Test
    fun streamAnswer_handlesHttpError() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\":{\"message\":\"Too many requests\"}}")
        )

        var errorMsg = ""
        client.streamAnswer(
            question = "Test",
            onToken = {},
            onComplete = {},
            onError = { errorMsg = it }
        )

        assertTrue(errorMsg.contains("429"))
    }

    @Test
    fun streamAnswer_sendsCorrectHeaders() = runTest {
        val sseBody = buildString {
            appendLine("event: message_stop")
            appendLine("data: {}")
            appendLine()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        client.streamAnswer(
            question = "Test",
            onToken = {},
            onComplete = {},
            onError = {}
        )

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("test-api-key", request.getHeader("x-api-key"))
        assertEquals("2023-06-01", request.getHeader("anthropic-version"))
        assertTrue("Content-Type should be application/json",
            request.getHeader("Content-Type")!!.startsWith("application/json"))
    }

    @Test
    fun streamAnswer_completesWhenStreamEndsWithoutMessageStop() = runTest {
        val sseBody = buildString {
            appendLine("event: content_block_delta")
            appendLine("data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Partial\"}}")
            appendLine()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        val tokens = mutableListOf<String>()
        var completed = false

        client.streamAnswer(
            question = "Test",
            onToken = { tokens.add(it) },
            onComplete = { completed = true },
            onError = {}
        )

        assertEquals(listOf("Partial"), tokens)
        assertTrue("Should complete even without message_stop", completed)
    }
}
