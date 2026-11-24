package boomerang.game

import boomerang.support.BUSHWALKING
import boomerang.support.SWIMMING
import boomerang.support.editionOf
import boomerang.support.points
import boomerang.support.roundOf
import boomerang.support.roundOfDrafts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * History: the score sheet of a game. R12 (totals, winner, tie-break) and the memory that R10b, R10e and
 * R11 need (AC10b.3, AC10e.4, AC11.4). The categories are test doubles, so no scoring rule plays a part.
 */
class HistoryTest {
    private val throwCatch = points("Throw and catch")
    private val sites = points("Tourist sites")
    private val animals = points("Animals")
    private val edition = editionOf(throwCatch, sites, animals, tieBreakers = listOf(throwCatch))

    /** One round: for each seat the points in throwCatch, sites and animals. */
    private fun round(vararg bySeat: Triple<Int, Int, Int>) = roundOf(*bySeat.map { (t, s, a) ->
        mapOf<IScoringCategory, Score>(throwCatch to Score(t), sites to Score(s), animals to Score(a))
    }.toTypedArray())

    private fun historyOf(vararg rounds: RoundRecord) = rounds.fold(History()) { history, round -> history + round }

    @Test
    fun `R11 AC11_4 adding a round gives a new history and leaves the old one as it was`() {
        val empty = History()
        val first = round(Triple(1, 2, 3), Triple(4, 5, 6))

        val one = empty + first
        val two = one + round(Triple(0, 0, 0), Triple(0, 0, 0))

        assertEquals(0, empty.rounds.size)
        assertEquals(listOf(first), one.rounds)
        assertEquals(2, two.rounds.size)
    }

    @Test
    fun `R10b AC10b_3 AC11_4 visited sites are kept per player over all finished rounds`() {
        val history = historyOf(roundOfDrafts("ABCDEFG", "HIJKLMN"), roundOfDrafts("ABOPQRS", "HITUVWX"))

        assertEquals("ABCDEFGOPQRS".toSet(), history.visitedSites(0))
        assertEquals("HIJKLMNTUVWX".toSet(), history.visitedSites(1))
        assertEquals(emptySet<Char>(), History().visitedSites(0))
    }

    @Test
    fun `R10e AC10e_4 AC11_4 what a player has chosen in a category is kept per player and per category`() {
        val activities = points("Activities")
        val other = points("Other")
        val history = historyOf(
            roundOf(mapOf(activities to Score(4, chosen = BUSHWALKING)), mapOf(activities to Score(0))),
            roundOf(mapOf(activities to Score(0, chosen = SWIMMING)), mapOf(activities to Score(2, chosen = SWIMMING))),
        )

        assertEquals(setOf(BUSHWALKING, SWIMMING), history.chosen(0, activities))
        assertEquals(setOf(SWIMMING), history.chosen(1, activities))
        assertEquals(emptySet<Symbol>(), history.chosen(0, other))
        assertEquals(emptySet<Symbol>(), History().chosen(0, activities))
    }

    @Test
    fun `R11 AC11_4 the points of the last finished round are known, and there are none before round 1`() {
        val history = historyOf(round(Triple(1, 7, 3), Triple(6, 7, 0)), round(Triple(2, 0, 5), Triple(0, 1, 9)))

        assertNull(History().previousPoints(0, sites))
        assertEquals(0, history.previousPoints(0, sites))
        assertEquals(9, history.previousPoints(1, animals))
    }

    @Test
    fun `R12 AC12_2 AC12_3 the total is the sum of all categories over all rounds, Tourist sites included`() {
        val history = historyOf(
            round(Triple(3, 7, 8), Triple(0, 7, 0)), round(Triple(1, 2, 0), Triple(6, 5, 3)),
            round(Triple(0, 4, 5), Triple(2, 0, 9)), round(Triple(6, 1, 3), Triple(1, 3, 0)),
        )

        assertEquals(10 + 14 + 16, history.total(0))
        assertEquals(9 + 15 + 12, history.total(1))
        assertEquals(10, history.total(0, throwCatch))
        assertEquals(14, history.total(0, sites))
        assertEquals(15, history.total(1, sites))
        assertEquals(0, History().total(0))
    }

    @Test
    fun `R12 AC12_2 the result lists the totals by seat and the total per category in the edition's order`() {
        val history = historyOf(round(Triple(3, 7, 8), Triple(0, 7, 0)), round(Triple(1, 2, 0), Triple(6, 5, 3)))

        val result = history.result(edition)

        assertEquals(listOf(21, 21), result.totals)
        assertEquals(listOf("Throw and catch", "Tourist sites", "Animals"), result.lines[0].map { it.title })
        assertEquals(listOf(4, 9, 8), result.lines[0].map { it.score.points })
        assertEquals(listOf(6, 12, 3), result.lines[1].map { it.score.points })
    }

    @Test
    fun `R12 AC12_4 the player with the highest total is the only winner`() {
        // seat 1 has the highest total and the lowest Throw and catch: the tie-break plays no part
        val history = historyOf(round(Triple(6, 7, 0), Triple(0, 7, 9), Triple(5, 7, 0)))

        assertEquals(listOf(1), history.result(edition).winners)
    }

    @Test
    fun `R12 AC12_5 of two players tied on the total, the one with more Throw and catch points wins`() {
        // both 20 in total; Throw and catch 0 against 12, over two rounds
        val history = historyOf(round(Triple(0, 7, 5), Triple(6, 7, 0)), round(Triple(0, 0, 8), Triple(6, 0, 1)))

        val result = history.result(edition)

        assertEquals(listOf(20, 20), result.totals)
        assertEquals(listOf(1), result.winners)
    }

    @Test
    fun `R12 AC12_5 the tie-break is between the players tied on the highest total only`() {
        // seat 2 has by far the most Throw and catch points, and a lower total
        val history = historyOf(round(Triple(1, 7, 12), Triple(3, 7, 10), Triple(6, 7, 0)))

        assertEquals(listOf(1), history.result(edition).winners)
    }

    @Test
    fun `R12 AC12_6 players equal on the total and on Throw and catch share the win`() {
        val history = historyOf(round(Triple(3, 7, 5), Triple(3, 5, 7), Triple(3, 0, 0)))

        val result = history.result(edition)

        assertEquals(listOf(0, 1), result.winners)
    }

    @Test
    fun `R12 the tie-breakers of an edition are applied in their order`() {
        // tied on the total (15) and on the first tie-breaker (sites 7); the second decides for seat 0
        val twoBreakers = editionOf(throwCatch, sites, animals, tieBreakers = listOf(sites, throwCatch))
        val history = historyOf(round(Triple(5, 7, 3), Triple(2, 7, 6), Triple(6, 1, 8)))

        assertEquals(listOf(0), history.result(twoBreakers).winners)
    }
}
