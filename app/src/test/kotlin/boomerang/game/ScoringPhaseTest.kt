package boomerang.game

import boomerang.support.Answered
import boomerang.support.Asked
import boomerang.support.BUSHWALKING
import boomerang.support.Entry
import boomerang.support.FixedCategory
import boomerang.support.SWIMMING
import boomerang.support.ScriptedPlayer
import boomerang.support.Told
import boomerang.support.draft
import boomerang.support.points
import boomerang.support.roundOfDrafts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Rule 10 on ScoringPhase, with categories that are test doubles: who is asked, what is recorded. The
 * rules of the Australian categories are tested in boomerang.australia.
 */
class ScoringPhaseTest {
    private val drafts = listOf(draft("EFGHAIM"), draft("QRUVYZB"))
    private val journal = mutableListOf<Entry>()

    private fun players(vararg choices: List<Int>) =
        List(2) { ScriptedPlayer(it, choices = choices.getOrElse(it) { emptyList() }, journal = journal) }

    private val offered = listOf(Score(4, chosen = BUSHWALKING), Score(2, chosen = SWIMMING), Score(0))

    /** Seat 0 has three scores to choose from, seat 1 has one. */
    private val choice = FixedCategory("Activities") { if (it.seat == 0) offered else listOf(Score(0)) }

    @Test
    fun `R10 every player's score in every category is recorded, and a category without a choice asks nobody`() {
        val first = points("First", 3, 5)
        val second = points("Second", 0, 2)
        val players = players()

        val record = ScoringPhase(listOf(first, second), players).run(drafts, History())

        assertEquals(drafts, record.drafts)
        assertEquals(
            listOf(
                mapOf<IScoringCategory, Score>(first to Score(3), second to Score(0)),
                mapOf<IScoringCategory, Score>(first to Score(5), second to Score(2)),
            ),
            record.scores,
        )
        assertTrue(players.all { it.choiceRequests.isEmpty() && it.cardRequests.isEmpty() }, "nobody was asked anything")
    }

    @Test
    fun `R10 a category is given the seat, all drafts of the round and the history of the game`() {
        val category = points("Only", 1, 1)
        val history = History(listOf(roundOfDrafts("CDJKNOS", "TWXLP*-")))

        ScoringPhase(listOf(category), players()).run(drafts, history)

        assertEquals(setOf(0, 1), category.contexts.map { it.seat }.toSet())
        for (context in category.contexts) {
            assertEquals(drafts, context.drafts)
            assertSame(history, context.history)
        }
    }

    @Test
    fun `R10e AC10e_3 AC10e_7 a player with several possible scores is asked once, with the category, the scores with their points and all seven cards`() {
        val players = players(listOf(1))

        val record = ScoringPhase(listOf(points("First", 3, 5), choice), players).run(drafts, History())

        assertEquals(listOf(ChoiceRequest("Activities", offered, drafts[0])), players[0].choiceRequests)
        assertEquals(7, players[0].choiceRequests.single().draft.cards.size)
        assertEquals(emptyList<ChoiceRequest>(), players[1].choiceRequests, "seat 1 has one possible score and is not asked")
        assertEquals(Score(2, chosen = SWIMMING), record.scores[0][choice], "the one score seat 0 chose")
        assertEquals(Score(0), record.scores[1][choice])
    }

    @Test
    fun `R10e S1 a choice that is not offered is refused, nothing is recorded for it and the same player is asked again`() {
        val players = players(listOf(3, -1, 0))

        val record = ScoringPhase(listOf(choice), players).run(drafts, History())

        val requests = players[0].choiceRequests
        assertEquals(listOf(null, 3, -1), requests.map { it.refused })
        assertEquals(1, requests.map { it.copy(refused = null) }.toSet().size, "the question is the same all three times")
        assertEquals(Score(4, chosen = BUSHWALKING), record.scores[0][choice])
    }

    @Test
    fun `R10e all players who have a choice are asked before any answer is read, category by category`() {
        val both = FixedCategory("Both") { listOf(Score(1, chosen = BUSHWALKING), Score(0)) }
        val again = FixedCategory("Again") { listOf(Score(1, chosen = SWIMMING), Score(0)) }

        ScoringPhase(listOf(both, again), players()).run(drafts, History())

        val kinds = journal.joinToString("") { entry ->
            when (entry) { is Asked -> "A"; is Answered -> "R"; is Told -> "T" }
        }
        assertEquals("AARRAARR", kinds)
        assertEquals(listOf("Both", "Both", "Again", "Again"), journal.filterIsInstance<Asked>().map { (it.request as ChoiceRequest).category })
    }
}
