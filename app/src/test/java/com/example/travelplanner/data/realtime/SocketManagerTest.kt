package com.example.travelplanner.data.realtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocketManagerTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `connect switches state to connected on open`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)

        manager.connect("ws://test")
        assertEquals(SocketConnectionState.Connecting, manager.state.value)
        transport.open()
        assertEquals(SocketConnectionState.Connected, manager.state.value)
    }

    @Test
    fun `disconnect switches state to disconnected`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)

        manager.connect("ws://test")
        transport.open()
        manager.disconnect()

        assertEquals(SocketConnectionState.Disconnected, manager.state.value)
    }

    @Test
    fun `send delegates to transport`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)

        manager.connect("ws://test")
        transport.open()
        val sent = manager.send("""{"ping":1}""")

        assertTrue(sent)
        assertEquals("""{"ping":1}""", transport.lastSentMessage)
    }

    @Test
    fun `onMessage handler receives parsed event`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)
        var received: SocketEvent? = null
        manager.onMessage { received = it }

        manager.connect("ws://test")
        transport.open()
        transport.push("""{"type":"trip","action":"updated","tripId":"t1","message":"ok","timestamp":1}""")

        assertEquals("trip", received?.type)
        assertEquals("t1", received?.tripId)
    }

    @Test
    fun `reconnect on failure updates state`() = runTest(dispatcher) {
        val first = ControlledTransport()
        val second = ControlledTransport()
        val queue = ArrayDeque<ControlledTransport>().apply {
            add(first)
            add(second)
        }
        val manager = SocketManager(
            retryDelayMillis = 2000,
            maxRetries = 1,
            dispatcher = dispatcher,
            transportFactory = { _: CoroutineScope ->
                queue.removeFirst()
            }
        )

        manager.connect("ws://test")
        first.open()
        first.fail(RuntimeException("boom"))
        advanceTimeBy(2000)
        runCurrent()
        second.open()
        assertEquals(SocketConnectionState.Connected, manager.state.value)
    }

    @Test
    fun `exhausted retries ends in disconnected`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = SocketManager(
            retryDelayMillis = 1000,
            maxRetries = 0,
            dispatcher = dispatcher,
            transportFactory = { transport }
        )

        manager.connect("ws://test")
        transport.open()
        transport.fail(RuntimeException("boom"))
        assertEquals(SocketConnectionState.Disconnected, manager.state.value)
    }

    @Test
    fun `manual disconnect prevents reconnect`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)

        manager.connect("ws://test")
        transport.open()
        manager.disconnect()
        transport.fail(RuntimeException("late"))

        assertEquals(SocketConnectionState.Disconnected, manager.state.value)
    }

    @Test
    fun `invalid json does not emit events`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)

        manager.connect("ws://test")
        transport.open()
        transport.push("not-json")

        assertTrue(manager.events.value.isEmpty())
    }

    @Test
    fun `event history is capped to 100`() = runTest(dispatcher) {
        val transport = ControlledTransport()
        val manager = createManager(transport)
        manager.connect("ws://test")
        transport.open()

        repeat(120) { i ->
            transport.push("""{"type":"trip","action":"updated","tripId":"$i","message":"m","timestamp":$i}""")
        }

        assertEquals(100, manager.events.value.size)
        assertEquals("20", manager.events.value.first().tripId)
    }

    private fun createManager(transport: ControlledTransport): SocketManager =
        SocketManager(
            retryDelayMillis = 1000,
            maxRetries = 1,
            dispatcher = dispatcher,
            transportFactory = { transport }
        )
}

private class ControlledTransport : SocketTransport {
    private var listener: SocketTransport.Listener? = null
    var lastSentMessage: String? = null
    override val isOpen: Boolean
        get() = open
    private var open = false

    override fun connect(url: String, listener: SocketTransport.Listener) {
        this.listener = listener
    }

    override fun disconnect(code: Int, reason: String) {
        open = false
    }

    override fun send(message: String): Boolean {
        lastSentMessage = message
        return open
    }

    fun open() {
        open = true
        listener?.onOpen()
    }

    fun push(text: String) {
        listener?.onMessage(text)
    }

    fun fail(error: Throwable) {
        open = false
        listener?.onFailure(error)
    }
}
