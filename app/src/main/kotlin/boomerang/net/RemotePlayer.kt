package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer

/** Server side: stands in for the player at the other end of a connection. Turns ConnectionLost and a
 *  reply of the wrong kind into PlayerLost. close() sends Bye and closes the connection. */
class RemotePlayer(private val connection: IConnection) : IPlayer, AutoCloseable {
    override fun selectCard(request: CardRequest): IAnswer<Char> = TODO("not implemented")
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> = TODO("not implemented")
    override fun show(event: GameEvent): Unit = TODO("not implemented")
    override fun close(): Unit = TODO("not implemented")
}
