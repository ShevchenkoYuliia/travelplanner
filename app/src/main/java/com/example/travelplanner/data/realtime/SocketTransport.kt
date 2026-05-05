package com.example.travelplanner.data.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface SocketTransport {
    fun connect(url: String, listener: Listener)
    fun disconnect(code: Int = 1000, reason: String = "Normal closure")
    fun send(message: String): Boolean
    val isOpen: Boolean

    interface Listener {
        fun onOpen()
        fun onMessage(text: String)
        fun onClosed()
        fun onFailure(error: Throwable)
    }
}

class OkHttpSocketTransport(
    private val client: OkHttpClient = OkHttpClient()
) : SocketTransport {
    private var socket: WebSocket? = null
    private var open = false

    override val isOpen: Boolean
        get() = open

    override fun connect(url: String, listener: SocketTransport.Listener) {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    open = true
                    listener.onOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    listener.onMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    open = false
                    listener.onClosed()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    open = false
                    listener.onFailure(t)
                }
            }
        )
    }

    override fun disconnect(code: Int, reason: String) {
        socket?.close(code, reason)
        socket = null
        open = false
    }

    override fun send(message: String): Boolean = socket?.send(message) == true
}

class FakeSocketTransport(
    private val scope: CoroutineScope,
    private val intervalMillis: Long = 4_000L
) : SocketTransport {
    private var listener: SocketTransport.Listener? = null
    private var tickerJob: Job? = null
    private var open = false

    override val isOpen: Boolean
        get() = open

    override fun connect(url: String, listener: SocketTransport.Listener) {
        this.listener = listener
        open = true
        listener.onOpen()
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && open) {
                delay(intervalMillis)
                val message = """{"type":"trip","action":"updated","tripId":"demo-trip","message":"Mock WS event","timestamp":${System.currentTimeMillis()}}"""
                listener.onMessage(message)
            }
        }
    }

    override fun disconnect(code: Int, reason: String) {
        open = false
        tickerJob?.cancel()
        tickerJob = null
        listener?.onClosed()
    }

    override fun send(message: String): Boolean = open
}
