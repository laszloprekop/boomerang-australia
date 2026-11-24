package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IPlayer
import java.io.Serializable

/** A two-way channel for messages to one peer. The only thing a transport has to provide. */
interface IConnection : AutoCloseable {
    /** ConnectionLost if the peer cannot be reached. */
    fun send(message: Message)

    /** Blocks; ConnectionLost if the peer is gone or sends what is not a Message. */
    fun receive(): Message
}

class ConnectionLost(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

sealed interface Message : Serializable

/** Server to client. Carries out itself on the client's local player; returns the reply to send, if any. */
sealed interface ServerMessage : Message {
    fun deliverTo(player: IPlayer): ClientMessage?
    val closes: Boolean get() = false
}

data class Tell(val event: GameEvent) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = TODO("not implemented")
}

data class AskCard(val request: CardRequest) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = TODO("not implemented")
}

data class AskChoice(val request: ChoiceRequest) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = TODO("not implemented")
}

/** The last message of a session: closes = true. */
data object Bye : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = TODO("not implemented")
    override val closes: Boolean get() = TODO("not implemented")
}

/** Client to server. */
sealed interface ClientMessage : Message
data class CardAnswer(val site: Char) : ClientMessage
data class ChoiceAnswer(val index: Int) : ClientMessage
