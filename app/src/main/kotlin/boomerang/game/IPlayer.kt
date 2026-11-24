package boomerang.game

import java.io.Serializable

/** An answer that is still to come. await() blocks until the player has answered; PlayerLost if none can come. */
fun interface IAnswer<out T> { fun await(): T }

/** `refused`: the answer just given to the same question that was not allowed (S1); null on the first asking. */
data class CardRequest(val hand: List<Card>, val draft: Draft, val refused: Char? = null) : Serializable

/** The question for one of several scores in a category. `refused` as in CardRequest, for an index. */
data class ChoiceRequest(
    val category: String, val scores: List<Score>, val draft: Draft, val refused: Int? = null,
) : Serializable

/** Something a player is told. A closed family: the seven events below. */
sealed interface GameEvent : Serializable
/** The game begins: the receiver's own seat, the number of players and the name of the edition. */
data class GameStarted(val seat: Int, val playerCount: Int, val edition: String) : GameEvent
/** A round begins: its number and the sites the receiver visited in earlier rounds. */
data class RoundStarted(val round: Int, val visitedSites: Set<Char>) : GameEvent   // round is 1 to 4
/** The cards every player has kept so far in the round, without the hidden Throw cards. */
data class CardsShown(val shown: List<List<Card>>) : GameEvent                     // by seat, Throw cards excluded
/** The round is drafted: the complete drafts of all players, Throw and Catch cards included. */
data class DraftsRevealed(val drafts: List<Draft>) : GameEvent                     // by seat, all seven cards
/** The round is scored: the receiver's own scores, and what is announced of every player. */
data class RoundScored(
    val round: Int,
    val own: List<ScoreLine>,               // the receiver's score in every category, in the edition's order
    val announced: List<List<ScoreLine>>,   // by seat: the lines of each player that carry an announcement
) : GameEvent
/** The game is finished: the final scores and the winners. */
data class GameOver(val result: GameResult) : GameEvent
/** The game ended early because the player of this seat was lost. */
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
