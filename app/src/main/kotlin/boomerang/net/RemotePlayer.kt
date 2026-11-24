package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer
import boomerang.game.PlayerLost

/** Server side: stands in for the player at the other end of a connection. Turns ConnectionLost and a
 *  reply of the wrong kind into PlayerLost. A failed send of a question surfaces as PlayerLost in await(),
 *  so selectCard and chooseScore always return; a failed send in show(event) throws PlayerLost at once.
 *  close() sends Bye and closes the connection. */
class RemotePlayer(private val connection: IConnection) : IPlayer, AutoCloseable {
    /** Sends the question now. The reply is read from the connection when the game awaits the answer. */
    override fun selectCard(request: CardRequest): IAnswer<Char> {
        val failure = trySend(AskCard(request))
        return IAnswer {
            val reply = replyAfter(failure)
            if (reply !is CardAnswer) throw PlayerLost(this)
            reply.site
        }
    }

    /** As selectCard, for a score. */
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> {
        val failure = trySend(AskChoice(request))
        return IAnswer {
            val reply = replyAfter(failure)
            if (reply !is ChoiceAnswer) throw PlayerLost(this)
            reply.index
        }
    }

    /** Sends the event to the client. */
    override fun show(event: GameEvent) {
        try {
            connection.send(Tell(event))
        } catch (lost: ConnectionLost) {
            throw PlayerLost(this, lost)
        }
    }

    /** Tells the client that the session is over, and closes the connection whether that worked or not. */
    override fun close() {
        try {
            connection.send(Bye)
        } finally {
            connection.close()
        }
    }

    /** Sends a question. A failure is not thrown here but returned, to be thrown when the answer is
     *  awaited: the game first puts the question to every player. Null if the question went out. */
    private fun trySend(question: ServerMessage): ConnectionLost? {
        try {
            connection.send(question)
            return null
        } catch (lost: ConnectionLost) {
            return lost
        }
    }

    /** The reply to a question. PlayerLost if the question could not be sent or no reply can come. */
    private fun replyAfter(sendFailure: ConnectionLost?): Message {
        if (sendFailure != null) throw PlayerLost(this, sendFailure)
        try {
            return connection.receive()
        } catch (lost: ConnectionLost) {
            throw PlayerLost(this, lost)
        }
    }
}
