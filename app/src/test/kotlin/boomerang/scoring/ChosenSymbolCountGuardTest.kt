package boomerang.scoring

import boomerang.game.Card
import boomerang.game.Draft
import boomerang.game.History
import boomerang.game.Score
import boomerang.game.ScoringContext
import boomerang.game.Symbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * R10e, review finding L2. Added after the review of the finished code, tests first.
 *
 * ChosenSymbolCount looks up pointsByCount[number of cards]. The class does not know the cards of its
 * edition, so at construction it can only refuse a table with no entry at all. Whether the table is long
 * enough shows when a draft is scored: a count the table has no entry for is a fault in the edition's data
 * and must be reported as such, with the category and the count, not as an index error from inside a list.
 * It is not rounded down to the last entry: that would hand out points the edition never stated.
 */
class ChosenSymbolCountGuardTest {
    private val art = Symbol("Treasure", "Art")
    private val music = Symbol("Treasure", "Music")

    /** A draft of [count] cards that carry Art, and one that carries Music. */
    private fun context(count: Int): ScoringContext {
        val cards = List(count) { Card('a' + it, "Gallery $it", "North", 1, listOf(art)) } + Card('z', "Hall", "South", 2, listOf(music))
        return ScoringContext(0, listOf(Draft(cards)), History())
    }

    @Test
    fun `R10e L2 a points table with no entry is refused when the category is made`() {
        assertThrows(IllegalArgumentException::class.java) { ChosenSymbolCount("Treasures", listOf(art, music), emptyList()) }
    }

    @Test
    fun `R10e L2 a count the points table does not reach fails with the category and the count, not with an index error`() {
        val category = ChosenSymbolCount("Treasures", listOf(art, music), listOf(0, 2, 5)) // entries for 0, 1 and 2 cards

        val failure = assertThrows(RuntimeException::class.java) { category.scores(context(count = 3)) }

        assertTrue(failure is IllegalStateException || failure is IllegalArgumentException,
            "a fault in the edition's data is to be an IllegalStateException or IllegalArgumentException, not $failure")
        val message = failure.message.orEmpty()
        assertTrue("Treasures" in message && "3" in message, "the message does not name the category and the count: '$message'")
    }

    @Test
    fun `R10e L2 counts the points table reaches are scored as before, the last entry included`() {
        val category = ChosenSymbolCount("Treasures", listOf(art, music), listOf(0, 2, 5))

        assertEquals(listOf(Score(5, chosen = art), Score(2, chosen = music), Score(0)), category.scores(context(count = 2)))
        assertEquals(listOf(Score(2, chosen = art), Score(2, chosen = music), Score(0)), category.scores(context(count = 1)))
    }
}
