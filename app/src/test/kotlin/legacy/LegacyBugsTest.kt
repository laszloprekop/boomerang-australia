import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * Where the original breaks rules 1 to 12. Every test states the rule and FAILS against
 * BoomerangAustralia.java. They are excluded from `./gradlew test` and run with `./gradlew legacyBugs`.
 *
 * Each test first checks that its set-up worked (assertRanAsPlanned, or a precondition), so that the
 * one assertion that fails is the one about the rule. Line numbers refer to BoomerangAustralia.java.
 */
@Tag("legacy-bug")
@Timeout(60)
class LegacyBugsTest {

    // --- Rule 1 -------------------------------------------------------------------------------------

    @Test
    fun `rule 1 - 0 players and 4 bots must not become a game of five players`() {
        // Line 282 accepts the total 4; line 443 then adds a console player on top of the 4 bots.
        val run = onConsole { BoomerangAustralia(arrayOf("0", "4")) }

        assertTrue(run.value.players.size <= 4,
            "players in the game: ${run.value.players.size}; the program printed: ${run.output.trim()}")
    }

    @Test
    fun `rule 1 - a negative number of players is refused`() {
        // Line 282 only checks the sum: -2 + 4 = 2. Lines 443 to 447 then make 1 + 4 players.
        val run = onConsole { BoomerangAustralia(arrayOf("-2", "4")) }

        assertEquals(0, run.value.players.size, "players in the game")
    }

    @Test
    fun `rule 1 - arguments that are not numbers are refused with a message`() {
        // Integer.valueOf throws on line 282, the constructor passes it on and main swallows it (line 464).
        val run = onConsole { BoomerangAustralia.main(arrayOf("two", "bots")) }

        assertTrue(run.output.contains("2-4 players"), "the program printed: '${run.output}'")
    }

    // --- Rules 5 and 9: who sees the Throw and the Catch card ---------------------------------------

    private val threeSeats = listOf(listOf("EFGHAIM", "QRUVYZB", "CDJKNOS"))

    @Test
    fun `rule 5 - when the round has ended the Throw card is revealed to the other players`() {
        val game = playRigged(3, threeSeats)
        game.assertRanAsPlanned()
        val other = game.seats[1]
        // control: the second card of seat 0 (F) did reach seat 1, so the search below works
        assertTrue(other.inbox.any { it.contains("[F]") })

        // Lines 369 to 377 always skip index 0 of the draft; line 398 shows the draft to its owner only.
        assertTrue(other.inbox.any { it.contains("[E]") },
            "no message to seat 1 in the whole round names [E], the Throw card of seat 0")
    }

    @Test
    fun `rule 9 - the Catch card is added to the cards shown to the other players`() {
        val game = playRigged(3, threeSeats)
        game.assertRanAsPlanned()
        // Seat 1 has held M itself on the way round, so its own hand displays do not count.
        val toldToSeat1 = game.seats[1].inbox.filterNot { it.startsWith("\nYour current hand") }
        assertTrue(toldToSeat1.any { it.contains("[I]") }, "control: the sixth card of seat 0")

        // The Catch card joins the draft on line 396, after the last report of lines 369 to 377.
        assertTrue(toldToSeat1.any { it.contains("[M]") },
            "nothing told to seat 1, apart from its own hands, names [M], the Catch card of seat 0")
    }

    // --- Rule 7 -------------------------------------------------------------------------------------

    @Test
    fun `rule 7 - a player who types something that is not in the hand still selects one card in that step`() {
        val run = solo {
            val neighbour = game.Player(1, true, null, null, null)
            player.hand.addAll(cards("ABCDEFG"))
            player.addCardToDraft(neighbour) // the scripted keyboard answers "N", and N is not in the hand
            player.draft.size to neighbour.nextHand.size
        }

        // Lines 90 to 94 find no card and say nothing; lines 95 to 96 pass all seven cards on.
        assertEquals(1 to 6, run.value, "cards selected, cards passed on")
    }

    @Test
    fun `rule 7 - one wrong input must not end the game for everyone`() {
        val game = playRigged(2, typed = { _, seat, step, planned -> if (seat == 0 && step == 2) "?" else planned })
        assertEquals(emptyList<String>(), game.harnessErrors)

        // Seat 0 ends with 6 cards, so draft.get(6) on line 159 throws; line 421 catches it outside the
        // round loop and the game is over. Nobody is scored and no winner is announced.
        assertEquals(STOPPED, game.ending, "round 1 was never scored; the game ended with")
    }

    // --- Rule 10b -----------------------------------------------------------------------------------

    @Test
    fun `rule 10b - the first player to visit A, B, C and D scores 3 for Western Australia`() {
        val game = playRigged(2, listOf(listOf("ABCDEIM", "QRUVYZF")))
        game.assertRanAsPlanned()

        // Line 323 spells the region "Wester Australia"; the cards say "Western Australia" (295 to 298).
        assertEquals(listOf(3), game.seats[0].regionPointsTold(), "region points told to seat 0")
    }

    @Test
    fun `rule 10b - a region completed by the Catch card scores its 3 points in that round`() {
        // As the passing Northern Territory test in LegacyRulesHoldTest, but H arrives as the Catch card.
        val game = playRigged(2, listOf(listOf("EFGAIMH", "QRUVYZB")))
        game.assertRanAsPlanned()

        // Regions are checked on lines 381 to 393; the Catch card joins the draft on line 396.
        assertEquals(listOf(3), game.seats[0].regionPointsTold(), "region points told to seat 0")
    }

    @Test
    fun `rule 10b - a region completed by the Catch card in round 4 counts in the final total`() {
        // Rounds 1 to 3 keep seat 0 out of the Northern Territory (E to H) and complete no region.
        val quiet = listOf("QRUVYZB", "CDJKNOS")
        val game = playRigged(2, listOf(quiet, quiet, quiet, listOf("EFGAIMH", "QRUVYZB")), lastRound = 3)
        game.assertRanAsPlanned()
        assertEquals(setOf("E", "F", "G", "H"), game.seats[0].sites.filterValues { it == "Northern Territory" }.keys,
            "precondition: seat 0 has visited all of the Northern Territory")

        assertEquals(listOf("Northern Territory"), game.seats[0].region, "regions of seat 0 at the end of the game")
    }

    // --- Rule 10c -----------------------------------------------------------------------------------

    @Test
    fun `rule 10c - three Leaves and one Shells sum to 6 and score 12`() {
        // Lines 178 to 183 count cards per collection and double the count; the values 1, 2, 3, 5 are
        // never used, and the test nr<8 (line 180) is on a card count that cannot exceed 5.
        assertEquals(12, solo { score(0, "AVXCEFH"); category(0, COLLECTIONS) }.value)
    }

    @Test
    fun `rule 10c - Souvenirs and Wildflowers sum to 7 and score 14`() {
        assertEquals(14, solo { score(0, "KDBEFHJ"); category(0, COLLECTIONS) }.value)
    }

    @Test
    fun `rule 10c - Souvenirs and Shells sum to 8 and score 8`() {
        assertEquals(8, solo { score(0, "KCBEFHJ"); category(0, COLLECTIONS) }.value)
    }

    @Test
    fun `rule 10c - two Wildflowers and Souvenirs sum to 9 and score 9`() {
        assertEquals(9, solo { score(0, "DIKBEFH"); category(0, COLLECTIONS) }.value)
    }

    // --- Rule 10d -----------------------------------------------------------------------------------

    @Test
    fun `rule 10d - three Kangaroos hold one pair and score 3`() {
        // Lines 193 to 208 score only a frequency of exactly 2.
        assertEquals(3, solo { score(0, "BCJADIK"); category(0, ANIMALS) }.value)
    }

    @Test
    fun `rule 10d - four Kangaroos are two pairs and score 6`() {
        assertEquals(6, solo { score(0, "BCJOADI"); category(0, ANIMALS) }.value)
    }

    @Test
    fun `rule 10d - five Kangaroos hold two pairs and score 6`() {
        assertEquals(6, solo { score(0, "BCJO-AD"); category(0, ANIMALS) }.value)
    }

    // --- Rule 12 ------------------------------------------------------------------------------------

    @Test
    fun `rule 12 - the final total is the sum of all categories, Tourist sites included`() {
        val run = solo {
            score(0, "ABEFIJM")
            score(1, "CDGHKLN")
            score(2, "OPQRUVY")
            val total = score(3, "STWXZ*-")
            // the original's own numbers per category, so that its other scoring faults play no part
            val byCategory = listOf(THROW_CATCH, SITES, COLLECTIONS, ANIMALS).associateWith { name -> (0..3).sumOf { category(it, name) } }
            Triple(total, byCategory, player.activitiesScore.values.sum() + 3 * player.region.size)
        }
        val (total, byCategory, activitiesAndRegions) = run.value
        assertEquals(28, byCategory[SITES], "precondition: 28 different sites over four rounds")
        assertEquals(0, activitiesAndRegions, "precondition: no activity scored, no region bonus")

        // Lines 270 and 272 add totalT, totalC, totalA, totalAct and the region bonuses, not totalTo.
        assertEquals(byCategory.values.sum(), total, "total for the categories $byCategory")
    }

    @Test
    fun `rule 12a - of two players tied on the total, the one with more Throw and catch points wins`() {
        // Per round both score 12, by the original's count and by the rules: seat 0 from two animal
        // pairs (3 + 5) and Leaves (4) with Throw and catch 0; seat 1 from one pair (5), Leaves (4) and
        // Throw and catch 3. Both visit 7 sites in round 1 and none after.
        val game = playRigged(2, List(4) { listOf("BJFUVHA", "EQMX*S-") }, lastRound = 3)
        game.assertRanAsPlanned()
        val (first, second) = game.seats
        assertEquals(first.finalScore, second.finalScore, "precondition: a tie on the total")
        assertEquals(0 to 12, first.rScore.sumOf { it.getValue(THROW_CATCH) } to second.rScore.sumOf { it.getValue(THROW_CATCH) },
            "precondition: Throw and catch totals")

        // Lines 412 to 417 keep the first player unless a later one has strictly more.
        assertTrue(first.inbox.last().startsWith("The winner is player: 1 "), first.inbox.last())
    }
}
