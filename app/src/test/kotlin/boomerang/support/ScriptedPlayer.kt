package boomerang.support

import boomerang.game.Card
import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.game.IAnswer
import boomerang.game.IPlayer
import boomerang.game.IShuffler
import boomerang.game.PlayerLost

/** What happened at the table, in the order it happened, over all players that share one journal. */
sealed interface Entry { val seat: Int }
data class Asked(override val seat: Int, val request: Any) : Entry
data class Answered(override val seat: Int, val answer: Any) : Entry
data class Told(override val seat: Int, val event: GameEvent) : Entry

/**
 * A player that answers from a script and records everything it is asked and told. No console, no socket,
 * no thread: an answer is taken from the script when the game awaits it.
 *
 * [cards] are the site characters to answer with, one per question, a repeated question included. When
 * they run out the player selects the first card of the hand. [choices] likewise; when they run out the
 * player takes the last score offered, which by the contract of IScoringCategory is "take none".
 */
class ScriptedPlayer(
    val seat: Int = 0,
    cards: String = "",
    choices: List<Int> = emptyList(),
    private val journal: MutableList<Entry> = mutableListOf(),
) : IPlayer {
    private val cardScript = ArrayDeque(cards.toList())
    private val choiceScript = ArrayDeque(choices)

    val cardRequests = mutableListOf<CardRequest>()
    val choiceRequests = mutableListOf<ChoiceRequest>()
    val events = mutableListOf<GameEvent>()

    /** The player is lost when the answer to this card question (counted from 1) is awaited. */
    var lostAtCardRequest: Int? = null

    /** The player cannot be reached with these events: show throws PlayerLost. */
    var unreachableFor: (GameEvent) -> Boolean = { false }

    /** The questions that were not a repetition after a refusal: one per step of the draft. */
    val steps: List<CardRequest> get() = cardRequests.filter { it.refused == null }

    inline fun <reified E : GameEvent> eventsOf(): List<E> = events.filterIsInstance<E>()

    override fun selectCard(request: CardRequest): IAnswer<Char> {
        cardRequests += request
        journal += Asked(seat, request)
        val number = cardRequests.size
        return IAnswer {
            if (number == lostAtCardRequest) throw PlayerLost(this)
            val site = cardScript.removeFirstOrNull() ?: request.hand.first().site
            journal += Answered(seat, site)
            site
        }
    }

    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> {
        choiceRequests += request
        journal += Asked(seat, request)
        return IAnswer {
            val index = choiceScript.removeFirstOrNull() ?: request.scores.lastIndex
            journal += Answered(seat, index)
            index
        }
    }

    override fun show(event: GameEvent) {
        if (unreachableFor(event)) throw PlayerLost(this)
        events += event
        journal += Told(seat, event)
    }
}

/** A shuffler that returns the decks it was given, one per round (the last one again when they run out),
 *  and records what it was asked to shuffle. */
class FixedShuffler(private vararg val decks: List<Card>) : IShuffler {
    val received = mutableListOf<List<Card>>()

    override fun shuffle(cards: List<Card>): List<Card> {
        received += cards
        return decks[minOf(received.size, decks.size) - 1]
    }
}
