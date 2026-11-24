import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * What the original gets right. These tests PASS against BoomerangAustralia.java. They also prove the
 * helpers of LegacyHarness.kt: every failing test in LegacyBugsTest uses a helper that a test here
 * has shown to work on a case with a known, correct outcome.
 */
@Timeout(60)
class LegacyRulesHoldTest {

    // --- Rule 1 -------------------------------------------------------------------------------------

    @Test
    fun `rule 1 - one console player and one to three bots give a game of two to four players`() {
        // The real constructor with an empty keyboard: the game starts and then dies on the first
        // empty answer (see rule 7 in LegacyBugsTest), which is enough to count the players.
        for (bots in 1..3) {
            val run = onConsole { BoomerangAustralia(arrayOf("1", "$bots")) }
            assertEquals(1 + bots, run.value.players.size, "players for arguments 1 $bots")
        }
    }

    @Test
    fun `rule 1 - totals of 1 and 5 and a wrong number of arguments are refused with the allowed range`() {
        for (args in listOf(arrayOf("1", "0"), arrayOf("0", "1"), arrayOf("1", "4"), arrayOf("0", "5"), arrayOf("1", "1", "1"), arrayOf())) {
            val run = onConsole { BoomerangAustralia(args) }
            assertEquals(0, run.value.players.size, "players for ${args.toList()}")
            assertTrue(run.output.contains("2-4 players"), "message for ${args.toList()}: ${run.output}")
            assertEquals(null, run.value.deck, "no deck for ${args.toList()}")
        }
    }

    // --- Rules 2 to 4 -------------------------------------------------------------------------------

    @Test
    fun `rule 2 - the deck is the 28 cards of the brief`() {
        val game = playRigged(2)
        game.assertRanAsPlanned()

        val original = game.arrayDeck.map {
            BriefCard(it.letter, it.name, it.region, it.number, it.Collections, it.Animals, it.Activities)
        }
        assertEquals(BRIEF_CARDS, original)
    }

    @Test
    fun `rule 3 - the shuffled deck holds the same 28 cards in an order that is not the table order`() {
        val orders = (1..3).map { playRigged(2).shuffled }

        for (order in orders) assertEquals(ALL_LETTERS.toList().sorted(), order.toList().sorted())
        assertTrue(orders.any { it != ALL_LETTERS }, "three shuffles in table order: $orders")
        assertTrue(orders.toSet().size > 1, "three identical shuffles: $orders")
    }

    @Test
    fun `rule 4 - each of 2, 3 and 4 players is dealt 7 cards and no card twice`() {
        for (n in 2..4) {
            val game = playRigged(n)
            game.assertRanAsPlanned()

            val hands = game.dealt[0]
            assertEquals(List(n) { 7 }, hands.map { it.length }, "hand sizes with $n players")
            assertEquals(7 * n, hands.joinToString("").toSet().size, "different cards dealt with $n players")
            assertEquals(28 - 7 * n, game.deck.size, "cards left undealt with $n players")
        }
    }

    // --- Rules 5 to 9: the draft ---------------------------------------------------------------------

    private val threeSeats = listOf(listOf("EFGHAIM", "QRUVYZB", "CDJKNOS"))

    @Test
    fun `rule 5 - during the round no other player is told the Throw card, its owner can see it`() {
        val game = playRigged(3, threeSeats)
        game.assertRanAsPlanned()

        for (owner in game.seats) {
            val throwCard = "[${threeSeats[0][owner.playerID][0]}]"
            assertTrue(owner.inbox.any { it.contains("Your current draft") && it.contains(throwCard) })
            for (other in game.seats - owner) {
                assertFalse(other.inbox.any { it.contains(throwCard) }, "seat ${other.playerID} was told $throwCard")
            }
        }
    }

    @Test
    fun `rule 6 - after each selection the rest of the hand goes to the next player, the last to the first`() {
        for (n in 2..4) {
            val game = playRigged(n)
            game.assertRanAsPlanned()

            for (seat in game.seats) {
                val giver = game.seats[(seat.playerID - 1 + n) % n]
                for (step in 1..5) {
                    val passedOn = giver.handsSeen[0][step - 1].replace(giver.drafts[0][step - 1].toString(), "")
                    assertEquals(passedOn, seat.handsSeen[0][step], "$n players, seat ${seat.playerID}, step $step")
                }
            }
        }
    }

    @Test
    fun `rule 7 - each card selected after the Throw card is shown to all players`() {
        val game = playRigged(3, threeSeats)
        game.assertRanAsPlanned()

        for (viewer in game.seats) for (subject in game.seats) {
            val told = viewer.toldAbout(subject.playerID)
            assertEquals(5, told.size, "one report per step after the first")
            for (step in 1..5) {
                // the report after step s names the cards selected in steps 1 to s, and no later one
                val expected = threeSeats[0][subject.playerID].substring(1, step + 1)
                val named = Regex("\\[(.)] \\(").findAll(told[step - 1]).joinToString("") { it.groupValues[1] }
                assertEquals(expected, named, "seat ${viewer.playerID} about seat ${subject.playerID} after step $step")
            }
        }
    }

    @Test
    fun `rule 8 - a player selects from hands of 7, 6, 5, 4, 3 and 2 cards`() {
        for (n in 2..4) {
            val game = playRigged(n)
            game.assertRanAsPlanned()

            for (seat in game.seats) assertEquals(listOf(7, 6, 5, 4, 3, 2), seat.handsSeen[0].map { it.length })
        }
    }

    @Test
    fun `rule 9 - the last card goes to the previous player and is the seventh card of that draft`() {
        for (n in 2..4) {
            val game = playRigged(n)
            game.assertRanAsPlanned()

            for (seat in game.seats) {
                val next = game.seats[(seat.playerID + 1) % n]
                val leftOver = next.handsSeen[0][5].replace(next.drafts[0][5].toString(), "")
                assertEquals(1, leftOver.length)
                assertEquals(7, seat.drafts[0].length)
                assertEquals(leftOver, seat.drafts[0].substring(6), "$n players, Catch card of seat ${seat.playerID}")
            }
            assertEquals(game.dealt[0].joinToString("").toSet(), game.seats.joinToString("") { it.drafts[0] }.toSet())
        }
    }

    // --- Rule 10: scoring ----------------------------------------------------------------------------

    @Test
    fun `rule 10a - the absolute difference of the numbers on the Throw and the Catch card`() {
        // E is 4, A and B are 1, Y and Z are 7
        assertEquals(3, solo { score(0, "EBCDFGA"); category(0, THROW_CATCH) }.value)
        assertEquals(3, solo { score(0, "ABCDFGE"); category(0, THROW_CATCH) }.value)
        assertEquals(0, solo { score(0, "AEFGHIB"); category(0, THROW_CATCH) }.value)
        assertEquals(6, solo { score(0, "AEFGHIZ"); category(0, THROW_CATCH) }.value)
    }

    @Test
    fun `rule 10b - one point per site not visited in an earlier round`() {
        val run = solo {
            score(0, "ABCDEFG")
            score(1, "ABCDHIJ")
            score(2, "ABCDHIJ")
            listOf(category(0, SITES), category(1, SITES), category(2, SITES))
        }
        assertEquals(listOf(7, 3, 0), run.value)
    }

    @Test
    fun `rule 10b - the first to complete a region scores 3, a player completing it in a later round nothing`() {
        // Seat 0 selects E, F, G and H (Northern Territory) in round 1; seat 1 does the same in round 2.
        val game = playRigged(2, listOf(listOf("EFGHAIM", "QRUVYZB"), listOf("QRUVYZB", "EFGHAIM")), lastRound = 1)
        game.assertRanAsPlanned()

        assertEquals(listOf(3, 0), game.seats[0].regionPointsTold())
        assertEquals(listOf(0, 0), game.seats[1].regionPointsTold())
        assertEquals(listOf("Northern Territory"), game.seats[0].region)
        assertEquals(emptyList<String>(), game.seats[1].region)
    }

    @Test
    fun `rule 10b - checkRegionComplete itself counts the four sites of a region over earlier rounds and the draft`() {
        val run = solo {
            score(0, "ABEFIJM")
            player.draft.clear()
            player.draft.addAll(cards("CDGKLNO"))
            listOf(player.checkRegionComplete("Western Australia"), player.checkRegionComplete("Northern Territory"))
        }
        assertEquals(listOf(true, false), run.value)
    }

    @Test
    fun `rule 10c control - no collection scores 0 and Leaves alone score double (the cases the original gets right)`() {
        assertEquals(0, solo { score(0, "BEFHJMN"); category(0, COLLECTIONS) }.value)
        assertEquals(6, solo { score(0, "AVXBEFH"); category(0, COLLECTIONS) }.value)
    }

    @Test
    fun `rule 10d - exactly one pair of an animal scores its value`() {
        assertEquals(0, solo { score(0, "BADIKLP"); category(0, ANIMALS) }.value, "one Kangaroos")
        assertEquals(3, solo { score(0, "BCADIKL"); category(0, ANIMALS) }.value, "two Kangaroos")
        assertEquals(10, solo { score(0, "HMBCADI"); category(0, ANIMALS) }.value, "Koalas and Kangaroos")
        assertEquals(18, solo { score(0, "ENFQGXA"); category(0, ANIMALS) }.value, "Emus, Wombats, Platypuses")
    }

    @Test
    fun `rule 10e - 1 to 6 cards of the chosen activity score 0, 2, 4, 7, 10, 15`() {
        val scores = (1..6).map { count ->
            val draft = "DKMOTU".take(count) + "ABCEFGH".take(7 - count)
            solo(keep = { it == "Bushwalking" }) {
                score(0, draft)
                player.activitiesScore["Bushwalking"]
            }.value
        }
        assertEquals(listOf<Int?>(0, 2, 4, 7, 10, 15), scores)
    }

    @Test
    fun `rule 10e - one activity per round, each once per game, and choosing none uses up nothing`() {
        val greedy = solo(keep = { true }) {
            score(0, "DKMOTUA")
            val afterFirst = player.activitiesScore.size
            score(1, "DKMOTUA")
            afterFirst to player.activitiesScore.size
        }
        assertEquals(1 to 2, greedy.value)
        // every activity is offered at most once over the two rounds, although the answer was always Y
        for (activity in listOf("Indigenous Culture", "Bushwalking", "Swimming", "Sightseeing")) {
            assertTrue(Regex("Want to keep $activity").findAll(greedy.output).count() <= 1, activity)
        }

        val modest = solo { score(0, "DKMOTUA"); player.activitiesScore.size }
        assertEquals(0, modest.value)
        assertEquals(4, Regex("Want to keep ").findAll(modest.output).count())
    }

    // --- Rules 11 and 12 -----------------------------------------------------------------------------

    // Per round, as the original counts and as the rules count: seat 0 scores 7 (Throw and catch 0,
    // Leaves 2 doubled, a pair of Kangaroos), seat 1 scores 12 (Throw and catch 3, Leaves 2 doubled, a
    // pair of Wombats). No Wildflowers, Shells or Souvenirs, no animal three times, no region completed.
    private val seat1Ahead = List(4) { listOf("BJFNVHA", "EQMX*S-") }

    @Test
    fun `rules 11 and 12 - four rounds, each with a new deal of 7, sites kept, the highest total wins and all are told`() {
        val game = playRigged(2, seat1Ahead, lastRound = 3)
        game.assertRanAsPlanned()

        assertEquals(4, game.dealt.size)
        for (seat in game.seats) {
            assertEquals(4, seat.drafts.size, "rounds scored")
            assertEquals(listOf(7, 0, 0, 0), seat.rScore.map { it[SITES] }, "sites are remembered")
            assertTrue(seat.inbox.last().startsWith("The winner is player: 1 with 48 points"), seat.inbox.last())
        }
        assertEquals(28, game.seats[0].finalScore)
        assertEquals(48, game.seats[1].finalScore)
    }

    @Test
    fun `rule 12 - the unchanged program plays a whole game of one console player and one bot to a winner`() {
        // No subclass here: the real constructor, the real server(), a scripted keyboard.
        val run = onConsole(reply = { transcript ->
            if (transcript.lastIndexOf("Want to keep ") > transcript.lastIndexOf("Type the letter")) "N"
            else Regex("\\[(.)] \\(").find(transcript.substringAfterLast("Your current hand: \n"))!!.groupValues[1]
        }) { BoomerangAustralia(arrayOf("1", "1")) }

        assertEquals(2, run.value.players.size)
        assertEquals(4, Regex("Your draft this round").findAll(run.output).count())
        assertTrue(run.output.trim().lines().last().startsWith("The winner is player: "), run.output.takeLast(200))
    }
}
