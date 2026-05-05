package com.example.travelplanner.data.realtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocketManager(
    private val parser: SocketEventParser = SocketEventParser(),
    private val retryDelayMillis: Long = 2_000L,
    private val maxRetries: Int = Int.MAX_VALUE,
    private val maxPendingMessageRetries: Int = 5,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val transportFactory: (CoroutineScope) -> SocketTransport
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var transport: SocketTransport = transportFactory(scope)
    private var socketUrl: String? = null
    private var retries = 0
    private var isManualDisconnect = false
    private var consumers = 0
    private val handlers = LinkedHashMap<Int, (SocketEvent) -> Unit>()
    private var handlerId = 0

    private val _state = MutableStateFlow(SocketConnectionState.Disconnected)
    val state: StateFlow<SocketConnectionState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<SocketEvent>>(emptyList())
    val events: StateFlow<List<SocketEvent>> = _events.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<PendingMessage>>(emptyList())
    val pendingMessages: StateFlow<List<PendingMessage>> = _pendingMessages.asStateFlow()

    fun connect(url: String) {
        consumers += 1
        if (socketUrl == url && _state.value != SocketConnectionState.Disconnected) {
            return
        }
        if (_state.value != SocketConnectionState.Disconnected) {
            transport.disconnect()
            transport = transportFactory(scope)
        }
        socketUrl = url
        isManualDisconnect = false
        retries = 0
        _state.value = SocketConnectionState.Connecting
        transport.connect(url, transportListener)
    }

    fun disconnect() {
        if (consumers > 0) {
            consumers -= 1
        }
        if (consumers > 0) return
        isManualDisconnect = true
        retries = 0
        transport.disconnect()
        _state.value = SocketConnectionState.Disconnected
    }

    fun send(message: String): Boolean {
        if (_state.value == SocketConnectionState.Connected && transport.isOpen) {
            return if (transport.send(message)) {
                true
            } else {
                addToPendingMessages(message)
                false
            }
        } else {
            addToPendingMessages(message)
            return false
        }
    }

    private fun addToPendingMessages(message: String) {
        val pending = PendingMessage(message = message)
        _pendingMessages.value = _pendingMessages.value + pending
    }

    private fun updatePendingMessage(id: String, status: PendingSyncStatus, errorMessage: String? = null) {
        _pendingMessages.value = _pendingMessages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    status = status,
                    errorMessage = errorMessage,
                    retryCount = msg.retryCount + 1
                )
            } else {
                msg
            }
        }
    }

    private fun removePendingMessage(id: String) {
        _pendingMessages.value = _pendingMessages.value.filter { it.id != id }
    }

    fun onMessage(handler: (SocketEvent) -> Unit): () -> Unit {
        val id = handlerId++
        handlers[id] = handler
        return { handlers.remove(id) }
    }

    private val transportListener = object : SocketTransport.Listener {
        override fun onOpen() {
            retries = 0
            _state.value = SocketConnectionState.Connected
            syncPendingMessages()
        }

        override fun onMessage(text: String) {
            runCatching { parser.parse(text) }
                .onSuccess { event ->
                    _events.value = (_events.value + event).takeLast(100)
                    handlers.values.forEach { it(event) }
                }
        }

        override fun onClosed() {
            handleReconnectIfNeeded()
        }

        override fun onFailure(error: Throwable) {
            handleReconnectIfNeeded()
        }
    }

    private fun handleReconnectIfNeeded() {
        if (isManualDisconnect || consumers == 0) {
            _state.value = SocketConnectionState.Disconnected
            return
        }
        val url = socketUrl
        if (url == null || retries >= maxRetries) {
            _state.value = SocketConnectionState.Disconnected
            return
        }
        retries += 1
        _state.value = SocketConnectionState.Reconnecting
        scope.launch {
            kotlinx.coroutines.delay(retryDelayMillis)
            transport.disconnect()
            transport = transportFactory(scope)
            _state.value = SocketConnectionState.Connecting
            transport.connect(url, transportListener)
        }
    }

    private fun syncPendingMessages() {
        val toSync = _pendingMessages.value
            .filter { it.status == PendingSyncStatus.PendingSync || it.status == PendingSyncStatus.Failed }
            .filter { it.retryCount < maxPendingMessageRetries }

        scope.launch {
            toSync.forEach { pending ->
                if (transport.isOpen && _state.value == SocketConnectionState.Connected) {
                    if (transport.send(pending.message)) {
                        updatePendingMessage(pending.id, PendingSyncStatus.Sent)
                        scope.launch {
                            kotlinx.coroutines.delay(5000)
                            removePendingMessage(pending.id)
                        }
                    } else {
                        updatePendingMessage(
                            pending.id,
                            PendingSyncStatus.Failed,
                            "Помилка при надсиланні на сервер"
                        )
                    }
                }
            }
        }
    }
}
