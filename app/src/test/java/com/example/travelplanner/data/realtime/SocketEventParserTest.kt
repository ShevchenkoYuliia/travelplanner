package com.example.travelplanner.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SocketEventParserTest {
    private val parser = SocketEventParser()

    @Test
    fun `parse maps all explicit fields`() {
        val event = parser.parse("""{"type":"trip","action":"updated","tripId":"1","message":"ok","timestamp":100}""")
        assertEquals("trip", event.type)
        assertEquals("updated", event.action)
        assertEquals("1", event.tripId)
        assertEquals(null, event.userId)
        assertEquals("ok", event.message)
        assertEquals(100L, event.timestamp)
    }

    @Test
    fun `parse maps user id`() {
        val event = parser.parse("""{"type":"user","action":"created","userId":"42","message":"ok"}""")
        assertEquals("user", event.type)
        assertEquals("created", event.action)
        assertEquals("42", event.userId)
        assertEquals(null, event.tripId)
    }

    @Test
    fun `parse defaults unknown type when blank`() {
        val event = parser.parse("""{"type":"","action":"updated"}""")
        assertEquals("unknown", event.type)
    }

    @Test
    fun `parse defaults unknown action when missing`() {
        val event = parser.parse("""{"type":"trip"}""")
        assertEquals("unknown", event.action)
    }

    @Test
    fun `parse keeps nullable trip id`() {
        val event = parser.parse("""{"type":"trip","action":"updated"}""")
        assertEquals(null, event.tripId)
    }

    @Test
    fun `parse defaults message to empty`() {
        val event = parser.parse("""{"type":"trip","action":"updated"}""")
        assertEquals("", event.message)
    }

    @Test
    fun `parse generates timestamp when absent`() {
        val before = System.currentTimeMillis()
        val event = parser.parse("""{"type":"trip","action":"updated"}""")
        val after = System.currentTimeMillis()
        assert(event.timestamp in before..after)
    }

    @Test
    fun `parse accepts additional unknown fields`() {
        val event = parser.parse("""{"type":"trip","action":"created","foo":"bar"}""")
        assertNotNull(event)
        assertEquals("trip", event.type)
    }
}
