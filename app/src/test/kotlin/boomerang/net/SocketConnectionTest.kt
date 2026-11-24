package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.DraftsRevealed
import boomerang.support.cards
import boomerang.support.draft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * The one test that opens a socket (architecture D13): SocketConnection and SocketListener on the loopback
 * interface, on a port the system picks. It is also the one test that starts a thread, for the server's
 * end: accept and the first receive wait for the client.
 */
class SocketConnectionTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun `S3 messages with the game's own values cross a socket both ways, and a closed peer is reported as ConnectionLost`() {
        val question = AskCard(CardRequest(cards("EKW"), draft("AB")))
        val event = Tell(DraftsRevealed(listOf(draft("EFGHAIM"), draft("QRUVYZB"))))
        var atServer: Message? = null
        var serverFailure: Throwable? = null

        SocketListener(0).use { listener ->
            assertTrue(listener.port > 0, "port 0 asks for any free port: ${listener.port}")
            val serverEnd = Thread {
                try {
                    listener.accept().use { connection ->
                        connection.send(question)
                        atServer = connection.receive()
                        connection.send(event)
                    } // closes the server's end
                } catch (failure: Throwable) {
                    serverFailure = failure
                }
            }
            serverEnd.start()
            try {
                SocketConnection.open("127.0.0.1", listener.port).use { client ->
                    assertEquals(question, client.receive())
                    client.send(CardAnswer('K'))
                    assertEquals(event, client.receive())
                    // S2: the peer is gone; no silent end and no bare IOException
                    assertThrows(ConnectionLost::class.java) { client.receive() }
                }
            } finally {
                serverEnd.join(10_000)
            }
        }

        assertNull(serverFailure, "the server's end failed")
        assertEquals(CardAnswer('K'), atServer)
    }
}
