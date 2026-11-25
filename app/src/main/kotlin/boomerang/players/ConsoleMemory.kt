package boomerang.players

import boomerang.game.Card
import boomerang.game.CardsShown
import boomerang.game.Direction
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.GameAborted
import boomerang.game.GameEvent
import boomerang.game.GameOver
import boomerang.game.GameStarted
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.ScoreLine

/** Console-screens D1: what one console has been shown so far. A screen with a map and a score sheet needs
 *  more than the one event or question that makes it appear. This is display state of one console, not
 *  game state: the game never reads it. */
internal class ConsoleMemory {
    /** True once GameStarted has been shown. Until then the console prints the plain texts of `describe` (D9). */
    var started = false
        private set

    /** The console's own seat. */
    var seat = 0
        private set

    /** The name of the edition. */
    var edition = ""
        private set

    /** The names of all players, by seat. */
    var names: List<String> = emptyList()
        private set

    /** The start of the round being played: its number, the sites visited before it, and the passing. */
    var round = RoundStarted(0, emptySet())
        private set

    /** The cards every player has shown in this round, by seat; empty until the first CardsShown of a round. */
    var shown: List<List<Card>> = emptyList()
        private set

    /** The console's own draft of the round last revealed. */
    var draft = Draft()
        private set

    /** The console's own score lines, one list per round scored. */
    val rounds = mutableListOf<List<ScoreLine>>()

    /** Keeps of an event what a later screen needs. */
    fun remember(event: GameEvent) {
        when (event) {
            is GameStarted -> rememberStart(event)
            is RoundStarted -> rememberRound(event)
            is CardsShown -> shown = event.shown
            is DraftsRevealed -> draft = event.drafts[seat]
            is RoundScored -> rounds += event.own
            is GameOver, is GameAborted -> Unit
        }
    }

    /** CS14: the name from GameStarted; "Player n" for a seat that has none. */
    fun name(seat: Int): String = names.getOrNull(seat) ?: playerName(seat)

    /** The seat whose cards reach the console's own seat when they travel in this direction. */
    fun giver(direction: Direction): Int = names.indices.first { direction.receiver(it, names.size) == seat }

    /** The seat that gets the cards of the console's own seat when they travel in this direction. */
    fun receiver(direction: Direction): Int = direction.receiver(seat, names.size)

    private fun rememberStart(event: GameStarted) {
        started = true
        seat = event.seat
        edition = event.edition
        names = event.names
    }

    /** A new round: nothing has been shown in it yet. */
    private fun rememberRound(event: RoundStarted) {
        round = event
        shown = emptyList()
    }
}
