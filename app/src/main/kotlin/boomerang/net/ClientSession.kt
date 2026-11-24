package boomerang.net

import boomerang.game.IPlayer

/** Client side: receives server messages, delivers each to the local player, sends the reply, until Bye. */
class ClientSession(private val connection: IConnection, private val player: IPlayer) {
    /** Returns after the message that closes the session. ConnectionLost if the server is gone before it. */
    fun run() {
        while (true) {
            val message = connection.receive() as? ServerMessage
                ?: throw ConnectionLost("the server sent a message that only a client sends")
            val reply = message.deliverTo(player)
            if (reply != null) connection.send(reply)
            if (message.closes) return
        }
    }
}
