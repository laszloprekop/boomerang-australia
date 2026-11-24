package boomerang.australia

import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.History
import boomerang.game.IScoringCategory
import boomerang.game.RoundRecord
import boomerang.game.Score
import boomerang.game.ScoringContext
import boomerang.game.ScoringPhase
import boomerang.support.ACTIVITIES
import boomerang.support.ANIMALS
import boomerang.support.BUSHWALKING
import boomerang.support.COLLECTIONS
import boomerang.support.INDIGENOUS_CULTURE
import boomerang.support.REGION_BONUS
import boomerang.support.SIGHTSEEING
import boomerang.support.SWIMMING
import boomerang.support.ScriptedPlayer
import boomerang.support.THROW_CATCH
import boomerang.support.TOURIST_SITES
import boomerang.support.draft
import boomerang.support.roundOfDrafts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Rule 10 for Boomerang Australia: the categories of australia(), each asked with a hand-built context.
 * A draft is written as its seven site characters, Throw card first, Catch card last. The cards are those
 * of the test table, so a fault in the production card list does not show here.
 */
class AustraliaScoringTest {
    private val edition = australia()

    private fun category(index: Int): IScoringCategory = edition.categories[index]

    /** The scores seat [seat] may take in a category, for the drafts of this round and what went before. */
    private fun scores(index: Int, drafts: List<String>, seat: Int = 0, history: History = History()): List<Score> =
        category(index).scores(ScoringContext(seat, drafts.map(::draft), history))

    /** The points of a category that leaves no choice, for one player alone at the table. */
    private fun points(index: Int, draft: String, history: History = History()): Int {
        val scores = scores(index, listOf(draft), history = history)
        assertEquals(1, scores.size, "a category without a choice gives one score: $scores")
        return scores.single().points
    }

    private fun historyOf(vararg rounds: RoundRecord) = History(rounds.toList())

    // --- R10a -----------------------------------------------------------------------------------------

    @Test
    fun `R10a AC10a_1 Throw 4 and Catch 1 score 3, and Throw 1 and Catch 4 score 3`() {
        assertEquals(3, points(THROW_CATCH, "EBCDFGA"))
        assertEquals(3, points(THROW_CATCH, "ABCDFGE"))
    }

    @Test
    fun `R10a AC10a_2 equal numbers score 0`() {
        assertEquals(0, points(THROW_CATCH, "AEFGHIB"))
    }

    @Test
    fun `R10a AC10a_3 the numbers 1 and 7 score 6, the maximum`() {
        assertEquals(6, points(THROW_CATCH, "AEFGHIZ"))
        assertEquals(6, points(THROW_CATCH, "ZEFGHIA"))
    }

    @Test
    fun `R10a only the Throw and the Catch card count, not the cards between`() {
        // Throw B (1) and Catch A (1) around five cards of 7, 6 and 4
        assertEquals(0, points(THROW_CATCH, "BYZIJEA"))
    }

    // --- R10b: sites ----------------------------------------------------------------------------------

    @Test
    fun `R10b AC10b_1 seven sites never visited before score 7`() {
        assertEquals(7, points(TOURIST_SITES, "ABCDEFG"))
    }

    @Test
    fun `R10b AC10b_2 a site visited in an earlier round scores 0 when drafted again`() {
        val afterOne = historyOf(roundOfDrafts("ABCDEFG"))
        val afterTwo = historyOf(roundOfDrafts("ABCDEFG"), roundOfDrafts("ABCDHIJ"))

        assertEquals(3, points(TOURIST_SITES, "ABCDHIJ", afterOne))
        assertEquals(0, points(TOURIST_SITES, "ABCDHIJ", afterTwo))
        assertEquals(0, points(TOURIST_SITES, "JIHGFED", afterTwo))
    }

    @Test
    fun `R10b AC10b_3 the visited sites are kept per player`() {
        val history = historyOf(roundOfDrafts("ABCDEFG", "HIJKLMN"))

        // both players draft A, B, C, H, I, O and P in round 2: what the other visited does not count
        assertEquals(4, scores(TOURIST_SITES, listOf("ABCHIOP", "ABCHIOP"), seat = 0, history).single().points)
        assertEquals(5, scores(TOURIST_SITES, listOf("ABCHIOP", "ABCHIOP"), seat = 1, history).single().points)
    }

    // --- R10b: region bonus ---------------------------------------------------------------------------

    @Test
    fun `R10b AC10b_5 legacy bug - the first player to visit A, B, C and D scores 3 for Western Australia`() {
        val bonus = scores(REGION_BONUS, listOf("ABCDEIM", "QRUVYZF")).single()

        assertEquals(3, bonus.points, "region points of seat 0")
        assertTrue("Western Australia" in bonus.announcement.orEmpty(), "announced: ${bonus.announcement}")
    }

    @Test
    fun `R10b AC10b_5 every one of the seven regions gives its bonus of 3 to the first player to complete it`() {
        // four sites of the region and three cards of three other regions
        for ((draft, region) in listOf(
            "ABCDEIM" to "Western Australia", "EFGHAIM" to "Northern Territory", "IJKLAEM" to "Queensland",
            "MNOPAEI" to "South Australia", "QRSTAEI" to "New South Wales", "UVWXAEI" to "Victoria", "YZ*-AEI" to "Tasmania",
        )) {
            val bonus = scores(REGION_BONUS, listOf(draft)).single()

            assertEquals(3, bonus.points, region)
            assertTrue(region in bonus.announcement.orEmpty(), "$region announced: ${bonus.announcement}")
        }
    }

    @Test
    fun `R10b a round that completes no region scores 0 and announces nothing`() {
        assertEquals(listOf(Score(0)), scores(REGION_BONUS, listOf("ABCEFGI")))
    }

    @Test
    fun `R10b AC10b_4 legacy bug - a region completed by the Catch card scores its 3 points in that round`() {
        // H, the fourth site of the Northern Territory, is the seventh card
        assertEquals(3, points(REGION_BONUS, "EFGAIMH"))
        // and the Throw card counts as well
        assertEquals(3, points(REGION_BONUS, "HAIMEFG"))
    }

    @Test
    fun `R10b AC10b_4 a region is complete when its four sites are visited over earlier rounds and this one`() {
        val history = historyOf(roundOfDrafts("ABEIMQU"))

        assertEquals(0, points(REGION_BONUS, "ABEIMQU"), "A and B in round 1")
        assertEquals(3, points(REGION_BONUS, "CDJNRVY", history), "C and D in round 2")
    }

    @Test
    fun `R10b AC10b_6 all players who complete the same region in the same round score 3 each`() {
        // seat 0 has E and F and adds G and H; seat 1 has G and H and adds E and F
        val history = historyOf(roundOfDrafts("EFAIMQU", "GHBJNRV"))
        val drafts = listOf("GHCKOSW", "EFDLPTX")

        assertEquals(3, scores(REGION_BONUS, drafts, seat = 0, history).single().points)
        assertEquals(3, scores(REGION_BONUS, drafts, seat = 1, history).single().points)
    }

    @Test
    fun `R10b AC10b_7 a player who completes a region in a later round than another player scores no bonus for it`() {
        // seat 0 completed the Northern Territory in round 1; seat 1 completes it in round 2
        val history = historyOf(roundOfDrafts("EFGHAIM", "QRUVYZB"))
        val drafts = listOf("QRUVYZB", "EFGHAIM")

        assertEquals(listOf(Score(0)), scores(REGION_BONUS, drafts, seat = 1, history))
    }

    @Test
    fun `R10b AC10b_7 legacy bug - after a region was completed by a Catch card, a later player is not paid for it`() {
        val history = historyOf(roundOfDrafts("EFGAIMH", "QRUVYZB"))

        assertEquals(0, scores(REGION_BONUS, listOf("QRUVYZB", "EFGAIMH"), seat = 1, history).single().points)
    }

    @Test
    fun `R10b AC10b_8 a player who completes two regions in one round, both first, scores 6`() {
        // A, B, E and F in round 1; C, D, G and H in round 2
        val history = historyOf(roundOfDrafts("ABEFIJM"))

        val bonus = scores(REGION_BONUS, listOf("CDGHQRU"), history = history).single()

        assertEquals(6, bonus.points)
        assertTrue("Western Australia" in bonus.announcement.orEmpty(), "announced: ${bonus.announcement}")
        assertTrue("Northern Territory" in bonus.announcement.orEmpty(), "announced: ${bonus.announcement}")
    }

    @Test
    fun `R10b AC10b_9 a region bonus is scored at most once per player and region`() {
        val history = historyOf(roundOfDrafts("EFGHAIM"))

        assertEquals(3, points(REGION_BONUS, "EFGHAIM"), "round 1")
        assertEquals(listOf(Score(0)), scores(REGION_BONUS, listOf("EFGHAIM"), history = history), "the same sites again in round 2")
    }

    // --- R10c -----------------------------------------------------------------------------------------

    @Test
    fun `R10c AC10c_1 no collection symbol scores 0`() {
        assertEquals(0, points(COLLECTIONS, "BEFHJMN"))
    }

    @Test
    fun `R10c AC10c_2 legacy bug - three Leaves and one Shells sum to 6 and score 12`() {
        assertEquals(12, points(COLLECTIONS, "AVXCEFH"))
    }

    @Test
    fun `R10c AC10c_3 legacy bug - Souvenirs and Wildflowers sum to 7 and score 14`() {
        assertEquals(14, points(COLLECTIONS, "KDBEFHJ"))
    }

    @Test
    fun `R10c AC10c_4 legacy bug - Souvenirs and Shells sum to 8 and score 8`() {
        assertEquals(8, points(COLLECTIONS, "KCBEFHJ"))
    }

    @Test
    fun `R10c AC10c_5 legacy bug - two Wildflowers and Souvenirs sum to 9 and score 9`() {
        assertEquals(9, points(COLLECTIONS, "DIKBEFH"))
    }

    @Test
    fun `R10c Leaves alone score double, and the Throw and the Catch card count (Q5)`() {
        assertEquals(6, points(COLLECTIONS, "AVXBEFH"))
        // Leaves only on the Throw card A and the Catch card V
        assertEquals(4, points(COLLECTIONS, "ABEFHJV"))
    }

    // --- R10d -----------------------------------------------------------------------------------------

    @Test
    fun `R10d AC10d_1 one Kangaroos card scores 0`() {
        assertEquals(0, points(ANIMALS, "BADIKLP"))
    }

    @Test
    fun `R10d AC10d_2 two Kangaroos score 3`() {
        assertEquals(3, points(ANIMALS, "BCADIKL"))
    }

    @Test
    fun `R10d AC10d_3 legacy bug - three Kangaroos hold one pair and score 3`() {
        assertEquals(3, points(ANIMALS, "BCJADIK"))
    }

    @Test
    fun `R10d AC10d_4 legacy bug - four Kangaroos are two pairs and score 6`() {
        assertEquals(6, points(ANIMALS, "BCJOADI"))
    }

    @Test
    fun `R10d AC10d_4 legacy bug - five Kangaroos hold two pairs and score 6`() {
        assertEquals(6, points(ANIMALS, "BCJO-AD"))
    }

    @Test
    fun `R10d AC10d_5 a pair of Koalas and a pair of Kangaroos score 10`() {
        assertEquals(10, points(ANIMALS, "HMBCADI"))
    }

    @Test
    fun `R10d AC10d_6 pairs of Emus, Wombats and Platypuses score 4, 5 and 9, and a pair of Koalas 7`() {
        assertEquals(4, points(ANIMALS, "ENADIKL"), "Emus")
        assertEquals(5, points(ANIMALS, "FQADIKL"), "Wombats")
        assertEquals(9, points(ANIMALS, "GXADIKL"), "Platypuses")
        assertEquals(7, points(ANIMALS, "HMADIKL"), "Koalas")
        assertEquals(18, points(ANIMALS, "ENFQGXA"), "Emus, Wombats and Platypuses together")
    }

    // --- R10e -----------------------------------------------------------------------------------------

    private val takeNone = Score(0)

    /** History in which seat 0 took these scores in the Activities category, one per round. */
    private fun afterTaking(vararg taken: Score) =
        History(taken.map { RoundRecord(listOf(Draft()), listOf(mapOf(category(ACTIVITIES) to it))) })

    @Test
    fun `R10e AC10e_1 one to six cards of an activity are worth 0, 2, 4, 7, 10 and 15`() {
        val worth = (1..6).map { count ->
            // D, K, M, O, T and U are the six Bushwalking cards; the others have none
            val draft = "DKMOTU".take(count) + "ABCEFGH".take(7 - count)
            scores(ACTIVITIES, listOf(draft)).single { it.chosen == BUSHWALKING }.points
        }

        assertEquals(listOf(0, 2, 4, 7, 10, 15), worth)
    }

    @Test
    fun `R10e AC10e_2 AC10e_6 three Bushwalking cards are offered for 4, then taking none, and nothing else`() {
        // C, G, X and Y carry no activity
        assertEquals(listOf(Score(4, chosen = BUSHWALKING), takeNone), scores(ACTIVITIES, listOf("DKMCGXY")))
    }

    @Test
    fun `R10e AC10e_6 an activity with no card in the draft is not offered, one with a single card is offered for 0`() {
        // one card each of Bushwalking (D), Indigenous Culture (A) and Sightseeing (B), no Swimming
        assertEquals(
            listOf(Score(0, chosen = BUSHWALKING), Score(0, chosen = INDIGENOUS_CULTURE), Score(0, chosen = SIGHTSEEING), takeNone),
            scores(ACTIVITIES, listOf("DABCGXY")),
        )
    }

    @Test
    fun `R10e S4 activities are offered in the order Swimming, Bushwalking, Indigenous Culture, Sightseeing, with taking none last`() {
        // B and A come first in the draft: the order is the edition's, not the draft's
        val offered = scores(ACTIVITIES, listOf("BADHCGX"))

        assertEquals(listOf(SWIMMING, BUSHWALKING, INDIGENOUS_CULTURE, SIGHTSEEING, null), offered.map { it.chosen })
        assertEquals(takeNone, offered.last())
    }

    @Test
    fun `R10e AC10e_4 an activity scored in an earlier round is not offered again in the game`() {
        val history = afterTaking(Score(4, chosen = BUSHWALKING))

        assertEquals(listOf(takeNone), scores(ACTIVITIES, listOf("DKMCGXY"), history = history))
        assertEquals(
            listOf(SWIMMING, INDIGENOUS_CULTURE, SIGHTSEEING, null),
            scores(ACTIVITIES, listOf("BADHCGX"), history = history).map { it.chosen },
        )
    }

    @Test
    fun `R10e AC10e_6 an activity taken for 0 points with one card is used up all the same`() {
        val history = afterTaking(Score(0, chosen = BUSHWALKING))

        // six Bushwalking cards and nothing to take for them
        assertEquals(listOf(Score(0, chosen = INDIGENOUS_CULTURE), takeNone), scores(ACTIVITIES, listOf("DKMOTUA"), history = history))
    }

    @Test
    fun `R10e AC10e_5 taking none scores 0 and uses up no activity`() {
        val history = afterTaking(takeNone, takeNone)

        assertNull(takeNone.chosen)
        assertEquals(listOf(Score(4, chosen = BUSHWALKING), takeNone), scores(ACTIVITIES, listOf("DKMCGXY"), history = history))
    }

    @Test
    fun `R10e AC10e_4 what one player has used up is still offered to another`() {
        val history = History(listOf(RoundRecord(
            listOf(Draft(), Draft()),
            listOf(mapOf(category(ACTIVITIES) to Score(4, chosen = BUSHWALKING)), mapOf(category(ACTIVITIES) to takeNone)),
        )))
        val drafts = listOf("DKMCGXY", "OTUABEF")

        assertEquals(listOf(takeNone), scores(ACTIVITIES, drafts, seat = 0, history))
        assertTrue(Score(4, chosen = BUSHWALKING) in scores(ACTIVITIES, drafts, seat = 1, history), "Bushwalking is offered to seat 1")
    }

    // --- R10 as a whole: one round and then another through ScoringPhase ---------------------------

    @Test
    fun `R10 a round is scored in all six categories on the seven cards of each player`() {
        val players = List(2) { ScriptedPlayer(it) } // both take no activity
        val drafts = listOf(draft("BJFNVHA"), draft("EQMX*S-"))

        val record = ScoringPhase(edition.categories, players).run(drafts, History())

        // seat 0: Throw B and Catch A are both 1; 7 new sites; no region; two Leaves doubled; a pair of Kangaroos
        assertEquals(listOf(0, 7, 0, 4, 3, 0), edition.categories.map { record.scores[0].getValue(it).points })
        // seat 1: Throw E is 4 and Catch - is 7; 7 new sites; no region; two Leaves doubled; a pair of Wombats
        assertEquals(listOf(3, 7, 0, 4, 5, 0), edition.categories.map { record.scores[1].getValue(it).points })
    }

    @Test
    fun `R10e AC10e_3 AC10e_7 a player is asked one question per round, which lists the possible activities with their points`() {
        val player = ScriptedPlayer(0, choices = listOf(1))
        val round = draft("HNSDKMA") // three Swimming, three Bushwalking, one Indigenous Culture

        val record = ScoringPhase(edition.categories, listOf(player)).run(listOf(round), History())

        val offered = listOf(Score(4, chosen = SWIMMING), Score(4, chosen = BUSHWALKING), Score(0, chosen = INDIGENOUS_CULTURE), takeNone)
        assertEquals(listOf(ChoiceRequest(category(ACTIVITIES).name, offered, round)), player.choiceRequests)
        assertEquals(Score(4, chosen = BUSHWALKING), record.scores[0][category(ACTIVITIES)], "one activity is scored, the one chosen")
    }

    @Test
    fun `R10e AC10e_4 AC10e_5 over two rounds - a chosen activity is gone, and after taking none everything is still there`() {
        val round = draft("HNSDKMA")
        val chooser = ScriptedPlayer(0, choices = listOf(1, 0))
        val modest = ScriptedPlayer(1) // takes none, twice
        val players = listOf(chooser, modest)
        val drafts = listOf(round, round) // the category looks at a draft, not at where the cards came from

        val first = ScoringPhase(edition.categories, players).run(drafts, History())
        val second = ScoringPhase(edition.categories, players).run(drafts, History() + first)

        assertEquals(listOf(SWIMMING, BUSHWALKING, INDIGENOUS_CULTURE, null), chooser.choiceRequests[0].scores.map { it.chosen })
        assertEquals(listOf(SWIMMING, INDIGENOUS_CULTURE, null), chooser.choiceRequests[1].scores.map { it.chosen })
        assertEquals(Score(4, chosen = SWIMMING), second.scores[0][category(ACTIVITIES)])
        assertEquals(modest.choiceRequests[0], modest.choiceRequests[1], "taking none used up nothing")
        assertEquals(listOf(takeNone, takeNone), listOf(first, second).map { it.scores[1][category(ACTIVITIES)] })
    }
}
