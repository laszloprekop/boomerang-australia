package boomerang.game

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.support.FixedShuffler
import boomerang.support.ScriptedPlayer
import boomerang.support.TABLE
import boomerang.support.editionOf
import boomerang.support.points
import boomerang.support.sites
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * FC13 in a whole game: AlternatingPassingRule is given to Game, and the seats and cards the players
 * actually hold are checked (AC13.1, AC13.2). Scripted players and a shuffler that deals the table order
 * in every round: no console, no socket, no thread, no chance. Scoring is a test double: FC13 does not
 * concern it.
 */
class AlternatingGameTest {

    private val edition = editionOf(points("One", 1, 2, 3, 4))

    /**
     * A game in which every round is dealt in table order (seat 0: A to G, seat 1: H to N, seat 2: O to U,
     * seat 3: V to the last card). scripts[seat] is the cards that seat selects, six per round; a seat
     * without a script selects the first card of each hand. Played when the object is made.
     */
    private inner class Played(n: Int, rule: IPassingRule, scripts: List<String> = List(n) { "" }) {
        val players = scripts.mapIndexed { seat, script -> ScriptedPlayer(seat, cards = script) }

        init {
            Game(edition, players, FixedShuffler(TABLE), rule).play()
        }

        /** The hand a seat held at a step (1 to 6) of a round (1 to 4), as site characters in table order. */
        fun hand(round: Int, seat: Int, step: Int): String =
            players[seat].cardRequests[6 * (round - 1) + step - 1].hand.sites().toList().sorted().joinToString("")

        /** The drafts of a round by seat, as the players were told them. */
        fun drafts(round: Int): List<String> = players[0].eventsOf<DraftsRevealed>()[round - 1].drafts.map { it.cards.sites() }

        /** The hands every seat held at the six steps of a round. */
        fun hands(round: Int): List<List<String>> = players.indices.map { seat -> (1..6).map { step -> hand(round, seat, step) } }
    }

    // What each seat selects in a round of three, written for the direction the hands travel in.
    private val whenHandsGoToNext = listOf("APJDSM", "HBQKET", "OICRLF")
    private val whenHandsGoToPrevious = listOf("AIQDLT", "HPCKSF", "OBJREM")

    /** The game of three under the rule: the scripts of rounds 1 to 4 follow the directions AC13.1 gives. */
    private fun gameOfThree() = Played(3, AlternatingPassingRule,
        scripts = (0..2).map { seat -> whenHandsGoToNext[seat] + whenHandsGoToPrevious[seat] + whenHandsGoToNext[seat] + whenHandsGoToPrevious[seat] })

    @Test
    fun `FC13 AC13_1 three players, odd rounds - each hand is held next by the following seat and the last card goes to the seat before`() {
        val game = gameOfThree()

        for (round in listOf(1, 3)) {
            // step 1: seat 0 keeps A, seat 1 keeps H, seat 2 keeps O; each rest travels one seat on, the last to the first
            assertEquals("BCDEFG", game.hand(round, seat = 1, step = 2), "round $round: what seat 0 passed on is held by seat 1")
            assertEquals("IJKLMN", game.hand(round, seat = 2, step = 2), "round $round: what seat 1 passed on is held by seat 2")
            assertEquals("PQRSTU", game.hand(round, seat = 0, step = 2), "round $round: what seat 2 passed on is held by seat 0")
            // step 6: seat 0 holds M and N and keeps M, seat 1 holds T and U and keeps T, seat 2 holds F and G and keeps F
            assertEquals(listOf("MN", "TU", "FG"), (0..2).map { game.hand(round, it, step = 6) }, "round $round, the last two cards by seat")
            // the card left over goes to the previous seat: U from seat 1 to seat 0, G from seat 2 to seat 1, N from seat 0 to seat 2
            assertEquals(listOf("APJDSMU", "HBQKETG", "OICRLFN"), game.drafts(round), "the drafts of round $round")
        }
    }

    @Test
    fun `FC13 AC13_1 three players, even rounds - each hand is held next by the seat before and the last card goes to the following seat`() {
        val game = gameOfThree()

        for (round in listOf(2, 4)) {
            // step 1: seat 0 keeps A, seat 1 keeps H, seat 2 keeps O; each rest travels one seat back, the first to the last
            assertEquals("IJKLMN", game.hand(round, seat = 0, step = 2), "round $round: what seat 1 passed on is held by seat 0")
            assertEquals("PQRSTU", game.hand(round, seat = 1, step = 2), "round $round: what seat 2 passed on is held by seat 1")
            assertEquals("BCDEFG", game.hand(round, seat = 2, step = 2), "round $round: what seat 0 passed on is held by seat 2")
            // step 6: seat 0 holds T and U and keeps T, seat 1 holds F and G and keeps F, seat 2 holds M and N and keeps M
            assertEquals(listOf("TU", "FG", "MN"), (0..2).map { game.hand(round, it, step = 6) }, "round $round, the last two cards by seat")
            // the card left over goes to the next seat: U from seat 0 to seat 1, G from seat 1 to seat 2, N from seat 2 to seat 0
            assertEquals(listOf("AIQDLTN", "HPCKSFU", "OBJREMG"), game.drafts(round), "the drafts of round $round")
        }
    }

    @Test
    fun `FC13 AC13_1 three players - the same deal ends in other drafts in an even round than in an odd round, and every card is still drafted once`() {
        val game = gameOfThree()

        assertNotEquals(game.drafts(1), game.drafts(2), "rounds 1 and 2 are dealt alike and passed differently")
        assertEquals(game.drafts(1), game.drafts(3), "rounds 1 and 3")
        assertEquals(game.drafts(2), game.drafts(4), "rounds 2 and 4")
        // the Catch card of each seat comes from the other neighbour: by seat, U G N in odd rounds and N U G in even rounds
        assertEquals(listOf("UGN", "NUG", "UGN", "NUG"), (1..4).map { round -> game.drafts(round).map { it.last() }.joinToString("") }, "the Catch cards by seat, round by round")
        for (round in 1..4) {
            assertEquals(TABLE.take(21).sites().toSet(), game.drafts(round).joinToString("").toSet(), "round $round: the cards drafted are the cards dealt")
            assertEquals(21, game.drafts(round).joinToString("").length, "round $round: cards drafted")
        }
    }

    @Test
    fun `FC13 AC13_1 three and four players - odd rounds run as a game passed by R6 and R9, even rounds as a game passed the other way round`() {
        for (n in 3..4) {
            // every player selects the first card of each hand, so a round depends on the deal and the directions only
            val game = Played(n, AlternatingPassingRule)
            val standard = Played(n, IPassingRule { _, _ -> Passing(hands = TO_NEXT, catch = TO_PREVIOUS) })
            val mirrored = Played(n, IPassingRule { _, _ -> Passing(hands = TO_PREVIOUS, catch = TO_NEXT) })

            for (round in 1..4) {
                val expected = if (round % 2 == 1) standard else mirrored
                assertEquals(expected.hands(round), game.hands(round), "$n players, round $round: the hands each seat held at steps 1 to 6")
                assertEquals(expected.drafts(round), game.drafts(round), "$n players, round $round: the drafts")
            }
            assertNotEquals(standard.drafts(2), mirrored.drafts(2), "$n players: the two directions give the same drafts, so the test shows nothing")
        }
    }

    @Test
    fun `FC13 AC13_2 two players - every round of a game with the rule runs as R6 and R9 say`() {
        val game = Played(2, AlternatingPassingRule)
        val standard = Played(2, StandardPassingRule)

        for (round in 1..4) {
            assertEquals(standard.hands(round), game.hands(round), "round $round: the hands each seat held at steps 1 to 6")
            assertEquals(standard.drafts(round), game.drafts(round), "round $round: the drafts")
            // the same deal every round: no round differs from the first
            assertEquals(game.hands(1), game.hands(round), "round $round against round 1")
            assertEquals(game.drafts(1), game.drafts(round), "round $round against round 1")
        }
        // seat 0 is dealt A to G and keeps A: seat 1 holds the rest at step 2
        assertEquals("BCDEFG", game.hand(round = 2, seat = 1, step = 2), "round 2: what seat 0 passed on is held by seat 1")
    }
}
