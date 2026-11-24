package boomerang.support

import boomerang.game.IPlayer
import boomerang.net.ClientSession
import boomerang.net.ConnectionLost
import boomerang.net.IConnection
import boomerang.net.Message
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * A server and a client joined by a connection that carries bytes, as a socket does: every message is
 * written with an ObjectOutputStream when it is sent and read back with an ObjectInputStream when it is
 * received, so the receiver gets a copy and never the sender's object. A value that Java cannot serialize
 * fails the test at the send that carries it. No socket and no thread.
 *
 * The client's end is a real ClientSession around [player]. Without a thread it cannot wait for the next
 * message, so the wire runs it in turns: whenever the server wants a reply that is not there yet, the
 * session runs and handles everything the server has sent so far; when nothing is left it is stopped by an
 * exception of this file (Paused) and started again at the next turn. That is sound because the session
 * keeps nothing between two messages. The session itself only ever sees messages the server really sent.
 */
class SerializingWire(val player: IPlayer) {
    private class Paused : RuntimeException()

    private val toClient = ArrayDeque<ByteArray>()
    private val toServer = ArrayDeque<ByteArray>()

    /** How many messages were written to bytes, in both directions. */
    var messages = 0
        private set

    /** True once the client's session has returned, which it does after Bye. */
    var sessionEnded = false
        private set

    /** The server's end: give it to a RemotePlayer. */
    val server: IConnection = object : IConnection {
        override fun send(message: Message) = toClient.addLast(toBytes(message))

        override fun receive(): Message {
            if (toServer.isEmpty()) runClient()
            val bytes = toServer.removeFirstOrNull() ?: throw ConnectionLost("the client sent no reply")
            return fromBytes(bytes)
        }

        override fun close() = runClient() // what is still on its way reaches the client
    }

    private val client: IConnection = object : IConnection {
        override fun send(message: Message) = toServer.addLast(toBytes(message))
        override fun receive(): Message = fromBytes(toClient.removeFirstOrNull() ?: throw Paused())
        override fun close() = Unit
    }

    private val session = ClientSession(client, player)

    /** One turn of the client: the session handles all messages that have arrived. */
    private fun runClient() {
        if (sessionEnded) return
        try {
            session.run()
            sessionEnded = true
        } catch (pause: Paused) {
            // nothing more has arrived; the next turn goes on from here
        }
    }

    private fun toBytes(message: Message): ByteArray {
        val bytes = ByteArrayOutputStream()
        try {
            ObjectOutputStream(bytes).use { it.writeObject(message) }
        } catch (failure: NotSerializableException) {
            throw AssertionError("this message cannot travel, it holds a ${failure.message} that is not serializable: $message", failure)
        }
        messages++
        return bytes.toByteArray()
    }

    private fun fromBytes(bytes: ByteArray): Message = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as Message }
}
