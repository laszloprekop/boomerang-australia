package boomerang.net

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * S2, review finding M5. Added after the review of the finished code, with approval to open sockets here.
 *
 * S2 names two failures at start-up: the port is in use, and the server is not reachable. Each has to
 * reach the caller as ConnectionLost with the facts a person needs (the port; the host and the port), so
 * that main can report it in words. Both tests use the loopback interface and a port the system picks.
 * No thread: nothing here waits for a peer.
 */
class SocketFailuresTest {
    private val loopback = "127.0.0.1"

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `S2 M5 a second listener on a port that is in use fails with ConnectionLost that names the port`() {
        SocketListener(0).use { first ->
            val port = first.port

            val failure = assertThrows(ConnectionLost::class.java) { SocketListener(port).close() }

            assertTrue("$port" in failure.message.orEmpty(), "the message does not name port $port: '${failure.message}'")
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `S2 M5 opening a connection where no server listens fails with ConnectionLost that names host and port`() {
        // a port that was free a moment ago and has no listener now
        val port = SocketListener(0).use { it.port }

        val failure = assertThrows(ConnectionLost::class.java) { SocketConnection.open(loopback, port).close() }

        val message = failure.message.orEmpty()
        assertTrue(loopback in message && "$port" in message, "the message does not name $loopback and port $port: '$message'")
    }
}
