package boomerang.net

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket

/** The transport of today: Java object streams over a TCP socket. */
class SocketConnection(socket: java.net.Socket) : IConnection {
    private val socket: java.net.Socket = socket
    private val output: ObjectOutputStream
    private val input: ObjectInputStream

    init {
        try {
            // The output stream first: making it sends a header that the input stream of the peer waits for.
            output = ObjectOutputStream(socket.getOutputStream())
            input = ObjectInputStream(socket.getInputStream())
        } catch (failure: IOException) {
            socket.close()
            throw ConnectionLost("the connection could not be set up", failure)
        }
    }

    /** Writes the message and pushes it out at once, so the peer does not wait for a full buffer. */
    override fun send(message: Message) {
        try {
            output.writeObject(message)
            output.flush()
        } catch (failure: IOException) {
            throw ConnectionLost("the message could not be sent", failure)
        }
    }

    /** Waits for the next object from the peer. */
    override fun receive(): Message {
        try {
            return input.readObject() as? Message ?: throw ConnectionLost("the peer sent something that is not a message")
        } catch (failure: IOException) {
            throw ConnectionLost("the peer is gone", failure)
        } catch (failure: ClassNotFoundException) {
            throw ConnectionLost("the peer sent something that is not a message", failure)
        }
    }

    /** Closes the socket, and with it both streams. */
    override fun close() {
        socket.close()
    }

    companion object {
        /** The client's end. ConnectionLost if no server answers there. */
        fun open(host: String, port: Int): SocketConnection {
            val socket = try {
                java.net.Socket(host, port)
            } catch (failure: IOException) {
                throw ConnectionLost("no server answers at $host on port $port", failure)
            }
            return SocketConnection(socket)
        }
    }
}

/** The server's end. Port 0 takes any free port; `port` is the one in use.
 *  The constructor throws ConnectionLost if the port cannot be opened, for example because it is in use. */
class SocketListener(port: Int) : AutoCloseable {
    private val server: ServerSocket = try {
        ServerSocket(port)
    } catch (failure: IOException) {
        throw ConnectionLost("port $port cannot be opened, perhaps a server is already running", failure)
    }

    /** The port the listener really has. */
    val port: Int get() = server.localPort

    /** Waits for the next client and returns the connection to it. */
    fun accept(): IConnection {
        val socket = try {
            server.accept()
        } catch (failure: IOException) {
            throw ConnectionLost("no client could be accepted", failure)
        }
        return SocketConnection(socket)
    }

    /** Stops listening. Connections already accepted stay open. */
    override fun close() {
        server.close()
    }
}
