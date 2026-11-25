package boomerang.game

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.support.FixedShuffler
import boomerang.support.ScriptedPlayer
import boomerang.support.TABLE
import boomerang.support.editionOf
import boomerang.support.points
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS16 and CS17: what Game passes on so that a console can name players and say
 * who passes to whom. Scripted players, a shuffler that deals the table order and a scoring double, as in
 * GameTest. The game does not know what a name means: it only hands the names on (D4).
 */
class GameNamesAndPassingTest {
    private val edition = editionOf(points("One", 1, 2, 3, 4))
    private val standard = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)
    private val mirrored = Passing(hands = TO_PREVIOUS, catch = TO_NEXT)

    private fun table(n: Int) = List(n) { ScriptedPlayer(it) }

    /** The passing every seat was told at the start of each round; the same for all seats, or the test fails. */
    private fun passingByRound(players: List<ScriptedPlayer>): List<Passing> {
        val bySeat = players.map { player -> player.eventsOf<RoundStarted>().map { it.passing } }
        assertEquals(1, bySeat.distinct().size, "the seats were told different directions: $bySeat")
        return bySeat.first()
    }

    // --- CS16 -----------------------------------------------------------------------------------------

    @Test
    fun `CS16 the game sends every player a GameStarted with the names it was given`() {
        val players = table(3)
        val names = listOf("Player 1", "Bot 2", "Bot 3")

        Game(edition, players, FixedShuffler(TABLE), names = names).play()

        for (player in players) {
            assertEquals(GameStarted(player.seat, 3, "Test edition", names), player.events.first(), "the first thing seat ${player.seat} is told")
        }
    }

    @Test
    fun `CS16 without names the players are Player 1 to Player n`() {
        for (n in 2..4) {
            val players = table(n)

            Game(edition, players, FixedShuffler(TABLE)).play()

            for (player in players) {
                assertEquals(List(n) { "Player ${it + 1}" }, player.eventsOf<GameStarted>().single().names, "$n players, seat ${player.seat}")
            }
        }
    }

    @Test
    fun `CS16 names of another length than the players are refused, and nobody is told or asked anything`() {
        for (names in listOf(emptyList(), listOf("Ann", "Bea"), listOf("Ann", "Bea", "Cy", "Di"))) {
            val players = table(3)
            val shuffler = FixedShuffler(TABLE)

            assertThrows(IllegalArgumentException::class.java, { Game(edition, players, shuffler, names = names) }, "${names.size} names for 3 players")

            assertEquals(0, shuffler.received.size, "shuffles with ${names.size} names")
            assertTrue(players.all { it.events.isEmpty() && it.cardRequests.isEmpty() }, "a player was told or asked something")
        }
    }

    // --- CS17 -----------------------------------------------------------------------------------------

    @Test
    fun `CS17 RoundStarted carries what the passing rule answered for that round`() {
        // a rule no real game has: another answer in every round, so a round cannot carry that of another
        val answers = listOf(standard, mirrored, Passing(hands = TO_NEXT, catch = TO_NEXT), Passing(hands = TO_PREVIOUS, catch = TO_PREVIOUS))
        val players = table(3)

        Game(edition, players, FixedShuffler(TABLE), IPassingRule { round, _ -> answers[round - 1] }).play()

        assertEquals(answers, passingByRound(players))
        for (player in players) assertEquals(listOf(1, 2, 3, 4), player.eventsOf<RoundStarted>().map { it.round })
    }

    @Test
    fun `CS17 with AlternatingPassingRule and three players rounds 2 and 4 carry the mirrored passing`() {
        val players = table(3)

        Game(edition, players, FixedShuffler(TABLE), AlternatingPassingRule).play()

        assertEquals(listOf(standard, mirrored, standard, mirrored), passingByRound(players))
    }

    @Test
    fun `CS17 without a rule given every round carries the standard passing`() {
        val players = table(4)

        Game(edition, players, FixedShuffler(TABLE)).play()

        assertEquals(List(4) { standard }, passingByRound(players))
    }
}
