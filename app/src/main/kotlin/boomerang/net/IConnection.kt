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

/** Thrown by a connection whose peer cannot be reached, or could not be reached in the first place. */
class ConnectionLost(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Everything that travels over a connection. */
sealed interface Message : Serializable

/** Server to client. Carries out itself on the client's local player; returns the reply to send, if any. */
sealed interface ServerMessage : Message {
    /** Does on the local player what the message stands for; the reply to send back, or null for none. */
    fun deliverTo(player: IPlayer): ClientMessage?

    /** True for the message that ends the session. */
    val closes: Boolean get() = false
}

/** Carries an event: the player is shown it, and there is no reply. */
data class Tell(val event: GameEvent) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? {
        player.show(event)
        return null
    }
}

/** Carries the question for a card: the reply is the site character the player answers. */
data class AskCard(val request: CardRequest) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = CardAnswer(player.selectCard(request).await())
}

/** Carries the question for a score: the reply is the index the player answers. */
data class AskChoice(val request: ChoiceRequest) : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = ChoiceAnswer(player.chooseScore(request).await())
}

/** The last message of a session: closes = true. */
data object Bye : ServerMessage {
    override fun deliverTo(player: IPlayer): ClientMessage? = null
    override val closes: Boolean get() = true
}

/** Client to server. */
sealed interface ClientMessage : Message

/** The answer to AskCard. */
data class CardAnswer(val site: Char) : ClientMessage

/** The answer to AskChoice. */
data class ChoiceAnswer(val index: Int) : ClientMessage
