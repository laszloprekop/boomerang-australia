package boomerang.game

import java.io.Serializable

/** An answer that is still to come. await() blocks until the player has answered; PlayerLost if none can come. */
fun interface IAnswer<out T> { fun await(): T }

/** `refused`: the answer just given to the same question that was not allowed (S1); null on the first asking. */
data class CardRequest(val hand: List<Card>, val draft: Draft, val refused: Char? = null) : Serializable

data class ChoiceRequest(
    val category: String, val scores: List<Score>, val draft: Draft, val refused: Int? = null,
) : Serializable

sealed interface GameEvent : Serializable
data class GameStarted(val seat: Int, val playerCount: Int, val edition: String) : GameEvent
data class RoundStarted(val round: Int, val visitedSites: Set<Char>) : GameEvent   // round is 1 to 4
data class CardsShown(val shown: List<List<Card>>) : GameEvent                     // by seat, Throw cards excluded
data class DraftsRevealed(val drafts: List<Draft>) : GameEvent                     // by seat, all seven cards
data class RoundScored(
    val round: Int,
    val own: List<ScoreLine>,               // the receiver's score in every category, in the edition's order
    val announced: List<List<ScoreLine>>,   // by seat: the lines of each player that carry an announcement
) : GameEvent
data class GameOver(val result: GameResult) : GameEvent
data class GameAborted(val lostSeat: Int) : GameEvent

/** One seat at the table as the rules see it: something that can be asked and told. */
interface IPlayer {
    /** Puts the question to the player and returns at once. The answer is the site character of a card of request.hand. */
    fun selectCard(request: CardRequest): IAnswer<Char>

    /** As selectCard. The answer is an index into request.scores. */
    fun chooseScore(request: ChoiceRequest): IAnswer<Int>

    /** Tells the player something. Never asks. PlayerLost if the player cannot be reached. */
    fun show(event: GameEvent)
}

/** Thrown by an IPlayer that can no longer be reached. */
class PlayerLost(val player: IPlayer, cause: Throwable? = null) : RuntimeException(cause)
