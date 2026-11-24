package boomerang.net

import boomerang.game.IPlayer

/** Client side: receives server messages, delivers each to the local player, sends the reply, until Bye. */
class ClientSession(private val connection: IConnection, private val player: IPlayer) {
    fun run(): Unit = TODO("not implemented")
}
