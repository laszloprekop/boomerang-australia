package boomerang.game

import boomerang.australia.australia
import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.players.BotPlayer
import boomerang.support.FixedCategory
import boomerang.support.FixedShuffler
import boomerang.support.REGION_BONUS
import boomerang.support.ScriptedPlayer
import boomerang.support.TABLE
import boomerang.support.THROW_CATCH
import boomerang.support.deckFor
import boomerang.support.editionOf
import boomerang.support.points
import boomerang.support.sites
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Rules 1, 3, 11 and 12 on Game, and the faults of the original that need a whole game to show. Scripted
 * players and a shuffler that returns the deck the test wants: no console, no socket, no thread, no chance.
 */
class GameTest {

    // --- The sequence of a game, with an edition whose two categories are test doubles ------------------

    private val one = points("One", 1, 2, 3, 4)
    private val two = FixedCategory("Two") {
        listOf(Score(10 * it.seat, announcement = if (it.seat == 1) "seat 1 did it" else null))
    }
    private val edition = editionOf(one, two)

    /** A game of n players who each select the first card of the hand; played when the object is made. */
    private inner class Plain(n: Int, vararg decks: List<Card> = arrayOf(TABLE), rule: IPassingRule? = null) {
        val players = List(n) { ScriptedPlayer(it) }
        val shuffler = FixedShuffler(*decks)
        val result: GameResult =
            if (rule == null) Game(edition, players, shuffler).play() else Game(edition, players, shuffler, rule).play()

        /** The drafts of the finished rounds, as the players were told them. */
        fun drafts(round: Int): List<Draft> = players[0].eventsOf<DraftsRevealed>()[round - 1].drafts
    }

    @Test
    fun `R1 AC1_1 games of 2, 3 and 4 players are accepted and have exactly that many players`() {
        for (n in 2..4) {
            val game = Plain(n)

            assertEquals(n, game.result.totals.size, "players in the result")
            for (player in game.players) {
                assertEquals(GameStarted(player.seat, n, "Test edition"), player.events.first(), "the first thing seat ${player.seat} is told")
                assertEquals(1, player.eventsOf<GameStarted>().size)
            }
        }
    }

    @Test
    fun `R1 AC1_2 games of 0, 1 and 5 players are refused with the allowed range, and no cards are dealt`() {
        for (n in listOf(0, 1, 5)) {
            val players = List(n) { ScriptedPlayer(it) }
            val shuffler = FixedShuffler(TABLE)

            val refusal = assertThrows(IllegalArgumentException::class.java, { Game(edition, players, shuffler) }, "$n players")

            val message = refusal.message.orEmpty()
            assertTrue("2" in message && "4" in message, "the message for $n players does not state the range 2 to 4: '$message'")
            assertEquals(0, shuffler.received.size, "shuffles for $n players")
            assertTrue(players.all { it.cardRequests.isEmpty() && it.events.isEmpty() }, "a player was asked or told something")
        }
    }

    @Test
    fun `R1 AC1_3 the count is of all players together, whatever their kind`() {
        fun mixed(humans: Int, bots: Int): List<IPlayer> =
            List(humans) { ScriptedPlayer(it) } + List(bots) { BotPlayer(Random(it)) }

        // the assertion of the legacy-bug test: bots and other players never add up to a game of five
        assertThrows(IllegalArgumentException::class.java) { Game(edition, mixed(1, 4), FixedShuffler(TABLE)) }
        assertThrows(IllegalArgumentException::class.java) { Game(edition, mixed(2, 3), FixedShuffler(TABLE)) }
        assertThrows(IllegalArgumentException::class.java) { Game(edition, mixed(0, 1), FixedShuffler(TABLE)) }
        for ((humans, bots) in listOf(1 to 1, 1 to 3, 3 to 1, 0 to 4, 4 to 0)) {
            assertEquals(humans + bots, Game(edition, mixed(humans, bots), FixedShuffler(TABLE)).play().totals.size, "$humans and $bots")
        }
    }

    @Test
    fun `R3 AC3_2 R4 the deal of every round is the order the shuffler returns`() {
        val decks = listOf(TABLE, TABLE.reversed(), TABLE.drop(7) + TABLE.take(7), TABLE.drop(14) + TABLE.take(14))

        val game = Plain(3, *decks.toTypedArray())

        for (round in 1..4) for (player in game.players) {
            val firstQuestion = player.cardRequests[6 * (round - 1)]
            val expected = decks[round - 1].subList(7 * player.seat, 7 * player.seat + 7)
            assertEquals(expected.toSet(), firstQuestion.hand.toSet(), "round $round, the hand dealt to seat ${player.seat}")
        }
    }

    @Test
    fun `R11 AC11_2 R3 every round starts from the complete deck of 28 cards, whatever was dealt before`() {
        for (n in 2..4) {
            val game = Plain(n)

            assertEquals(4, game.shuffler.received.size, "$n players: shuffles in a game")
            for (given in game.shuffler.received) {
                assertEquals(28, given.size, "$n players: cards given to the shuffler")
                assertEquals(TABLE.toSet(), given.toSet(), "$n players")
            }
        }
    }

    @Test
    fun `R11 AC11_1 each player is shown the own score per category for the round, and what is announced of every player`() {
        val game = Plain(3)

        for (player in game.players) {
            val own = listOf(
                ScoreLine("One", Score(player.seat + 1)),
                ScoreLine("Two", Score(10 * player.seat, announcement = if (player.seat == 1) "seat 1 did it" else null)),
            )
            val announced = listOf(emptyList(), listOf(ScoreLine("Two", Score(10, announcement = "seat 1 did it"))), emptyList())
            assertEquals((1..4).map { RoundScored(it, own, announced) }, player.eventsOf<RoundScored>(), "seat ${player.seat}")
        }
    }

    @Test
    fun `R11 R12 AC11_1 a round is drafted, revealed and scored before the next one starts, and the result comes last`() {
        val game = Plain(4)

        val round = listOf(RoundStarted::class) + List(5) { CardsShown::class } + DraftsRevealed::class + RoundScored::class
        val expected = listOf(GameStarted::class) + List(4) { round }.flatten() + GameOver::class
        for (player in game.players) assertEquals(expected, player.events.map { it::class }, "seat ${player.seat}")
    }

    @Test
    fun `R11 AC11_3 hands and drafts of the finished round are cleared`() {
        val game = Plain(3)

        for (player in game.players) for (round in 2..4) {
            val firstQuestion = player.cardRequests[6 * (round - 1)]
            assertEquals(7, firstQuestion.hand.size, "seat ${player.seat}, the hand at the start of round $round")
            assertEquals(Draft(), firstQuestion.draft, "seat ${player.seat}, the draft at the start of round $round")
        }
    }

    @Test
    fun `R11 AC11_4 visited sites are kept for the whole game, per player`() {
        val game = Plain(2, TABLE, TABLE.reversed(), TABLE.drop(7) + TABLE.take(7), TABLE)

        for (player in game.players) {
            val visited = mutableSetOf<Char>()
            val expected = (1..4).map { round ->
                RoundStarted(round, visited.toSet()).also { visited += game.drafts(round)[player.seat].cards.map { it.site } }
            }
            assertEquals(expected, player.eventsOf<RoundStarted>(), "seat ${player.seat}")
        }
        assertTrue(game.players[0].eventsOf<RoundStarted>().last().visitedSites.size > 7, "control: the rounds differ")
    }

    @Test
    fun `R11 AC11_4 the scores of earlier rounds are kept`() {
        val game = Plain(3)

        assertEquals(listOf(4 * 1, 4 * 12, 4 * 23), game.result.totals)
    }

    @Test
    fun `R11 AC11_5 seat order and passing direction are the same in every round`() {
        val asked = mutableListOf<Pair<Int, Int>>()
        val recording = IPassingRule { round, playerCount ->
            asked += round to playerCount
            Passing(hands = TO_NEXT, catch = TO_PREVIOUS)
        }

        val game = Plain(3, rule = recording)
        val byDefault = Plain(3)

        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3, 4 to 3), asked, "the passing rule is asked once per round")
        for (player in game.players) {
            val rounds = player.cardRequests.chunked(6)
            assertEquals(4, rounds.size)
            // the same deck every round: the same hands reach the same seat in the same order
            for (round in rounds) assertEquals(rounds[0], round, "seat ${player.seat}")
        }
        // without a rule given, a game passes as rules 6 and 9 say: the same drafts as with the rule above
        for (round in 1..4) assertEquals(game.drafts(1), byDefault.drafts(round), "round $round")
    }

    /**
     * A game of three in which every round is dealt in table order (seat 0: A to G, seat 1: H to N, seat 2:
     * O to U) and every player selects the scripted cards, six per round. Played when the object is made.
     */
    private inner class Passed(scripts: List<String>, rule: IPassingRule? = null) {
        val players = scripts.mapIndexed { seat, script -> ScriptedPlayer(seat, cards = script.repeat(4)) }
        val result: GameResult =
            if (rule == null) Game(edition, players, FixedShuffler(TABLE)).play() else Game(edition, players, FixedShuffler(TABLE), rule).play()

        /** The hand a seat held at a step (1 to 6) of round 1, as site characters in table order. */
        fun hand(seat: Int, step: Int): String = players[seat].cardRequests[step - 1].hand.sites().toList().sorted().joinToString("")

        /** The drafts of each round, as the players were told them. */
        val drafts: List<List<String>> = players[0].eventsOf<DraftsRevealed>().map { event -> event.drafts.map { it.cards.sites() } }
    }

    @Test
    fun `R6 AC6_1 R9 AC9_1 a game without a rule given passes hands to the next seat and the Catch card to the previous seat`() {
        val game = Passed(listOf("APJDSM", "HBQKET", "OICRLF"))

        // step 1: seat 0 keeps A, seat 1 keeps H, seat 2 keeps O; each rest travels one seat on, the last to the first
        assertEquals("BCDEFG", game.hand(seat = 1, step = 2), "what seat 0 passed on is held by seat 1")
        assertEquals("IJKLMN", game.hand(seat = 2, step = 2), "what seat 1 passed on is held by seat 2")
        assertEquals("PQRSTU", game.hand(seat = 0, step = 2), "what seat 2 passed on is held by seat 0")
        // step 6: seat 0 holds M and N and keeps M, seat 1 holds T and U and keeps T, seat 2 holds F and G and keeps F
        assertEquals(listOf("MN", "TU", "FG"), (0..2).map { game.hand(it, step = 6) })
        // the card left over goes to the previous seat: U from seat 1 to seat 0, G from seat 2 to seat 1, N from seat 0 to seat 2
        assertEquals(List(4) { listOf("APJDSMU", "HBQKETG", "OICRLFN") }, game.drafts, "the drafts of the four rounds")
    }

    @Test
    fun `R6 R9 the game passes hands and the Catch card as its passing rule says - a rule that reverses both directions`() {
        val reversed = IPassingRule { _, _ -> Passing(hands = TO_PREVIOUS, catch = TO_NEXT) }

        val game = Passed(listOf("AIQDLT", "HPCKSF", "OBJREM"), reversed)

        // step 1: seat 0 keeps A, seat 1 keeps H, seat 2 keeps O; each rest travels one seat back, the first to the last
        assertEquals("IJKLMN", game.hand(seat = 0, step = 2), "what seat 1 passed on is held by seat 0")
        assertEquals("PQRSTU", game.hand(seat = 1, step = 2), "what seat 2 passed on is held by seat 1")
        assertEquals("BCDEFG", game.hand(seat = 2, step = 2), "what seat 0 passed on is held by seat 2")
        // step 6: seat 0 holds T and U and keeps T, seat 1 holds F and G and keeps F, seat 2 holds M and N and keeps M
        assertEquals(listOf("TU", "FG", "MN"), (0..2).map { game.hand(it, step = 6) })
        // the card left over goes to the next seat: U from seat 0 to seat 1, G from seat 1 to seat 2, N from seat 2 to seat 0
        assertEquals(List(4) { listOf("AIQDLTN", "HPCKSFU", "OBJREMG") }, game.drafts, "the drafts of the four rounds")
    }

    @Test
    fun `R12 AC12_1 exactly four rounds are played and there is no fifth deal`() {
        for (n in 2..4) {
            val game = Plain(n)

            assertEquals(4, game.shuffler.received.size, "$n players: deals")
            for (player in game.players) {
                assertEquals(4 * 6, player.cardRequests.size, "$n players: cards selected by seat ${player.seat}")
                assertEquals(listOf(1, 2, 3, 4), player.eventsOf<RoundStarted>().map { it.round })
                assertEquals(listOf(1, 2, 3, 4), player.eventsOf<RoundScored>().map { it.round })
            }
        }
    }

    @Test
    fun `R12 AC12_4 AC12_7 all players are told the final scores and the winner`() {
        val game = Plain(4)

        assertEquals(listOf(4, 48, 92, 136), game.result.totals)
        assertEquals(listOf(3), game.result.winners)
        for (player in game.players) assertEquals(GameOver(game.result), player.events.last(), "the last thing seat ${player.seat} is told")
    }

    // --- Whole games of Boomerang Australia with planned drafts --------------------------------------

    /** A game in which every player ends every round with the planned draft and takes no activity. */
    private inner class Planned(vararg val rounds: List<String>) {
        val players = List(rounds[0].size) { seat ->
            ScriptedPlayer(seat, cards = rounds.joinToString("") { it[seat].take(6) })
        }
        val result: GameResult =
            Game(australia(), players, FixedShuffler(*rounds.map(::deckFor).toTypedArray())).play()

        init {
            for (player in players) {
                assertEquals(rounds.toList(), player.eventsOf<DraftsRevealed>().map { event -> event.drafts.map { it.cards.sites() } },
                    "precondition: the drafts are the planned ones")
            }
        }

        fun scored(round: Int, seat: Int): RoundScored = players[seat].eventsOf<RoundScored>()[round - 1]
        fun points(seat: Int): List<Int> = result.lines[seat].map { it.score.points }
    }

    @Test
    fun `R12 AC12_2 AC12_3 legacy bug - the final total is the sum of all categories, Tourist sites included`() {
        // Per round seat 0 scores 7 (Throw and catch 0, two Leaves doubled, a pair of Kangaroos) and seat 1
        // scores 12 (Throw and catch 3, two Leaves doubled, a pair of Wombats). Both visit 7 sites in round
        // 1 and none after. No region, no activity. The original gave 28 and 48: it left out the sites.
        val game = Planned(*Array(4) { listOf("BJFNVHA", "EQMX*S-") })

        assertEquals(listOf(0, 7, 0, 16, 12, 0), game.points(0), "seat 0 by category")
        assertEquals(listOf(12, 7, 0, 16, 20, 0), game.points(1), "seat 1 by category")
        assertEquals(listOf(35, 55), game.result.totals)
        assertEquals(listOf(1), game.result.winners)
        for (player in game.players) assertEquals(GameOver(game.result), player.events.last())
    }

    @Test
    fun `R12 AC12_5 legacy bug - of two players tied on the total, the one with more Throw and catch points wins`() {
        // Per round both score 12: seat 0 from two animal pairs (3 + 5) and Leaves (4) with Throw and catch
        // 0; seat 1 from one pair (5), Leaves (4) and Throw and catch 3. Both visit 7 sites in round 1.
        val game = Planned(*Array(4) { listOf("BJFUVHA", "EQMX*S-") })

        assertEquals(listOf(55, 55), game.result.totals, "precondition: a tie on the total")
        assertEquals(0 to 12, game.points(0)[THROW_CATCH] to game.points(1)[THROW_CATCH], "precondition: Throw and catch totals")
        assertEquals(listOf(1), game.result.winners)
    }

    @Test
    fun `R10b AC10b_4 AC11_1 legacy bug - a region completed by the Catch card scores its 3 points in that round, and all are told`() {
        // seat 0 selects E, F and G and is caught H: the Northern Territory is complete with the Catch card
        val game = Planned(listOf("EFGAIMH", "QRUVYZB"), listOf("QRUVYZB", "CDJKNOS"), listOf("QRUVYZB", "CDJKNOS"), listOf("QRUVYZB", "CDJKNOS"))

        val bonus = game.scored(1, 0).own[REGION_BONUS].score
        assertEquals(3, bonus.points, "region points told to seat 0 after round 1")
        assertTrue("Northern Territory" in bonus.announcement.orEmpty(), "announced: ${bonus.announcement}")
        for (seat in 0..1) {
            val announced = game.scored(1, seat).announced
            assertEquals(listOf(game.scored(1, 0).own[REGION_BONUS]), announced[0], "what seat $seat is told about seat 0")
            assertEquals(emptyList<ScoreLine>(), announced[1], "what seat $seat is told about seat 1")
        }
        assertEquals(3, game.points(0)[REGION_BONUS])
    }

    @Test
    fun `R10b AC12_2 legacy bug - a region completed by the Catch card in round 4 counts in the final total`() {
        // Rounds 1 to 3 keep seat 0 out of the Northern Territory (E to H) and complete no region.
        val quiet = listOf("QRUVYZB", "CDJKNOS")
        val game = Planned(quiet, quiet, quiet, listOf("EFGAIMH", "QRUVYZB"))

        assertEquals(listOf(0, 0, 0, 3), (1..4).map { game.scored(it, 0).own[REGION_BONUS].score.points }, "region points of seat 0 by round")
        assertEquals(3, game.points(0)[REGION_BONUS], "region bonus of seat 0 at the end of the game")
        assertEquals(game.points(0).sum(), game.result.totals[0], "the total holds the bonus")
        // by hand: Throw and catch 4+4+4+0 (Q is 5, B is 1; E and H are 4), sites 7+0+0+7, the bonus 3,
        // collections 4+4+4+12 (two Leaves doubled; Shells, Leaves and Wildflowers doubled), animals
        // 9+9+9+7 (a pair of Wombats and a pair of Emus; a pair of Koalas), no activity
        assertEquals(listOf(12, 14, 3, 24, 34, 0), game.points(0))
    }

    // --- S2: a lost player ----------------------------------------------------------------------------

    private fun table(n: Int) = List(n) { ScriptedPlayer(it) }

    @Test
    fun `S2 a player lost during the game ends it for everyone - the others are told who, and the failure is passed on`() {
        val players = table(3)
        players[1].lostAtCardRequest = 9 // round 2, third selection

        val failure = assertThrows(PlayerLost::class.java) { Game(edition, players, FixedShuffler(TABLE)).play() }

        assertSame(players[1], failure.player)
        for (other in listOf(players[0], players[2])) {
            assertEquals(GameAborted(lostSeat = 1), other.events.last(), "the last thing seat ${other.seat} is told")
            assertTrue(other.eventsOf<GameOver>().isEmpty(), "no result after a lost player")
            assertEquals(listOf(1), other.eventsOf<RoundScored>().map { it.round }, "rounds scored before the loss")
        }
        assertTrue(players[1].eventsOf<GameAborted>().isEmpty(), "the lost player is not told")
    }

    @Test
    fun `S2 a player who cannot be reached with an event is lost in the same way`() {
        val players = table(3)
        players[2].unreachableFor = { it is RoundScored }

        val failure = assertThrows(PlayerLost::class.java) { Game(edition, players, FixedShuffler(TABLE)).play() }

        assertSame(players[2], failure.player)
        for (other in players.take(2)) assertEquals(GameAborted(lostSeat = 2), other.events.last(), "seat ${other.seat}")
    }

    @Test
    fun `S2 a second player lost while the others are told is not dropped, and the rest are still told`() {
        val players = table(3)
        players[1].lostAtCardRequest = 2
        players[0].unreachableFor = { it is GameAborted }

        val failure = assertThrows(PlayerLost::class.java) { Game(edition, players, FixedShuffler(TABLE)).play() }

        assertSame(players[1], failure.player, "the first loss is the one passed on")
        assertTrue(failure.suppressed.any { it is PlayerLost && it.player === players[0] }, "the second loss is attached: ${failure.suppressed.toList()}")
        assertEquals(GameAborted(lostSeat = 1), players[2].events.last(), "seat 2 is still told")
    }
}
