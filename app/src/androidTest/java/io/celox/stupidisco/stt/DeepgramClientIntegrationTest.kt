package io.celox.stupidisco.stt

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeepgramClientIntegrationTest {

    private lateinit var server: MockWebServer
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun connect_establishesWebSocketConnection() {
        val connectedLatch = CountDownLatch(1)

        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                connectedLatch.countDown()
            }
        }))

        val client = DeepgramClient(
            apiKey = "test-key",
            onPartialTranscript = {},
            onFinalTranscript = {},
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("WebSocket should connect", connectedLatch.await(5, TimeUnit.SECONDS))
        client.disconnect()
    }

    @Test
    fun onMessage_parsesFinalTranscript() {
        val transcriptLatch = CountDownLatch(1)
        var receivedTranscript = ""

        val serverListener = object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                val json = """
                    {
                        "channel": {
                            "alternatives": [{
                                "transcript": "Hallo Welt"
                            }]
                        },
                        "is_final": true
                    }
                """.trimIndent()
                webSocket.send(json)
            }
        }

        server.enqueue(MockResponse().withWebSocketUpgrade(serverListener))

        val client = DeepgramClient(
            apiKey = "test-key",
            onPartialTranscript = {},
            onFinalTranscript = {
                receivedTranscript = it
                transcriptLatch.countDown()
            },
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("Should receive final transcript", transcriptLatch.await(5, TimeUnit.SECONDS))
        assertEquals("Hallo Welt", receivedTranscript)
        client.disconnect()
    }

    @Test
    fun onMessage_parsesPartialTranscript() {
        val partialLatch = CountDownLatch(1)
        var receivedPartial = ""

        val serverListener = object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                val json = """
                    {
                        "channel": {
                            "alternatives": [{
                                "transcript": "Hallo"
                            }]
                        },
                        "is_final": false
                    }
                """.trimIndent()
                webSocket.send(json)
            }
        }

        server.enqueue(MockResponse().withWebSocketUpgrade(serverListener))

        val client = DeepgramClient(
            apiKey = "test-key",
            onPartialTranscript = {
                receivedPartial = it
                partialLatch.countDown()
            },
            onFinalTranscript = {},
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("Should receive partial transcript", partialLatch.await(5, TimeUnit.SECONDS))
        assertEquals("Hallo", receivedPartial)
        client.disconnect()
    }

    @Test
    fun onMessage_ignoresBlankTranscript() {
        val openLatch = CountDownLatch(1)
        var transcriptReceived = false

        val serverListener = object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                val json = """
                    {
                        "channel": {
                            "alternatives": [{
                                "transcript": ""
                            }]
                        },
                        "is_final": true
                    }
                """.trimIndent()
                webSocket.send(json)
                openLatch.countDown()
            }
        }

        server.enqueue(MockResponse().withWebSocketUpgrade(serverListener))

        val client = DeepgramClient(
            apiKey = "test-key",
            onPartialTranscript = { transcriptReceived = true },
            onFinalTranscript = { transcriptReceived = true },
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("Server should open connection", openLatch.await(5, TimeUnit.SECONDS))
        // Small delay to ensure message would have been processed
        Thread.sleep(500)
        assertTrue("Blank transcripts should be ignored", !transcriptReceived)
        client.disconnect()
    }

    @Test
    fun onMessage_ignoresMessageWithoutChannel() {
        val openLatch = CountDownLatch(1)
        var transcriptReceived = false

        val serverListener = object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                webSocket.send("""{"type":"Metadata","request_id":"abc"}""")
                openLatch.countDown()
            }
        }

        server.enqueue(MockResponse().withWebSocketUpgrade(serverListener))

        val client = DeepgramClient(
            apiKey = "test-key",
            onPartialTranscript = { transcriptReceived = true },
            onFinalTranscript = { transcriptReceived = true },
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("Server should open connection", openLatch.await(5, TimeUnit.SECONDS))
        Thread.sleep(500)
        assertTrue("Messages without channel should be ignored", !transcriptReceived)
        client.disconnect()
    }

    @Test
    fun connect_sendsAuthorizationHeader() {
        val headerLatch = CountDownLatch(1)

        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                headerLatch.countDown()
            }
        }))

        val client = DeepgramClient(
            apiKey = "my-secret-key",
            onPartialTranscript = {},
            onFinalTranscript = {},
            onError = {},
            client = okHttpClient,
            baseUrl = server.url("/v1/listen").toString().replace("http://", "ws://")
        )

        client.connect()
        assertTrue("Should connect", headerLatch.await(5, TimeUnit.SECONDS))

        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("Token my-secret-key", request.getHeader("Authorization"))
        client.disconnect()
    }
}
