package boomerang.net

/** The transport of today: Java object streams over a TCP socket. */
class SocketConnection(socket: java.net.Socket) : IConnection {
    override fun send(message: Message): Unit = TODO("not implemented")
    override fun receive(): Message = TODO("not implemented")
    override fun close(): Unit = TODO("not implemented")

    companion object {
        /** The client's end. */
        fun open(host: String, port: Int): SocketConnection = TODO("not implemented")
    }
}

/** The server's end. Port 0 takes any free port; `port` is the one in use. */
class SocketListener(port: Int) : AutoCloseable {
    val port: Int get() = TODO("not implemented")
    fun accept(): IConnection = TODO("not implemented")
    override fun close(): Unit = TODO("not implemented")
}
