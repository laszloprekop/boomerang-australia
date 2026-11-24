package boomerang.game

import boomerang.support.TABLE
import boomerang.support.points
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * R4 and R1, review finding L3. Added after the review of the finished code, tests first.
 *
 * Every player is dealt Game.HAND_SIZE cards and a game has up to Game.PLAYERS.last players, so an edition
 * with fewer cards than that cannot be dealt. It is to be refused when it is made, not in the middle of
 * the first deal of a game of four.
 */
class EditionDeckSizeTest {
    private val category = points("Only", 0)
    private val needed = Game.PLAYERS.last * Game.HAND_SIZE

    @Test
    fun `R4 L3 an edition with too few cards for the largest game is refused`() {
        assertEquals(28, needed)
        assertThrows(IllegalArgumentException::class.java) { Edition("One short", TABLE.take(needed - 1), listOf(category), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { Edition("Two hands", TABLE.take(2 * Game.HAND_SIZE), listOf(category), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { Edition("Empty", emptyList(), listOf(category), emptyList()) }
    }

    @Test
    fun `R4 L3 an edition with exactly the cards the largest game needs is accepted, and so is a larger one`() {
        val extra = Card('+', "One more", "Elsewhere", 1, emptyList())

        assertEquals(needed, Edition("Exact", TABLE.take(needed), listOf(category), emptyList()).cards.size)
        assertEquals(needed + 1, Edition("Larger", TABLE + extra, listOf(category), emptyList()).cards.size)
        // and the check is in force: without this line the test would pass before the guard exists
        assertThrows(IllegalArgumentException::class.java) { Edition("One short", TABLE.take(needed - 1), listOf(category), emptyList()) }
    }
}
