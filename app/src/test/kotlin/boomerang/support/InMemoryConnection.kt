package boomerang.support

import boomerang.net.ConnectionLost
import boomerang.net.IConnection
import boomerang.net.Message

/**
 * One end of a connection that lives in memory: what this end sends, the other end receives, in order.
 * There is no thread, so receive cannot wait: with nothing to receive it throws ConnectionLost, as a real
 * connection does when the peer is gone. After close() on either end both ends throw ConnectionLost.
 */
class InMemoryConnection private constructor(
    private val inbox: ArrayDeque<Message>,
    private val outbox: ArrayDeque<Message>,
    private val state: State,
) : IConnection {
    private class State { var closed = false }

    val closed: Boolean get() = state.closed

    /** What this end has sent and the other end has not yet received. */
    val sent: List<Message> get() = outbox.toList()

    override fun send(message: Message) {
        if (state.closed) throw ConnectionLost("the connection is closed")
        outbox.addLast(message)
    }

    override fun receive(): Message {
        if (state.closed) throw ConnectionLost("the connection is closed")
        return inbox.removeFirstOrNull() ?: throw ConnectionLost("nothing to receive: the peer is gone")
    }

    override fun close() {
        state.closed = true
    }

    companion object {
        /** The two ends of one connection: first the server's, then the client's. */
        fun pair(): Pair<InMemoryConnection, InMemoryConnection> {
            val toClient = ArrayDeque<Message>()
            val toServer = ArrayDeque<Message>()
            val state = State()
            return InMemoryConnection(toServer, toClient, state) to InMemoryConnection(toClient, toServer, state)
        }
    }
}
