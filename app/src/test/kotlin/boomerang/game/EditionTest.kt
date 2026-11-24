package boomerang.game

import boomerang.support.TABLE
import boomerang.support.points
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** What an Edition refuses to be built from (architecture section 3, D5): the ground R2 and R12 stand on. */
class EditionTest {
    private val first = points("First", 0)
    private val second = points("Second", 0)

    @Test
    fun `R2 AC2_1 an edition with the same site character on two cards is refused`() {
        val twice = TABLE + TABLE.first().copy(name = "A second card with site A")

        assertThrows(IllegalArgumentException::class.java) { Edition("X", twice, listOf(first), listOf(first)) }
    }

    @Test
    fun `R10 an edition with two categories of the same name is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            Edition("X", TABLE, listOf(first, points("First", 0)), emptyList())
        }
    }

    @Test
    fun `R10 an edition with a category without a name is refused`() {
        assertThrows(IllegalArgumentException::class.java) { Edition("X", TABLE, listOf(first, points(" ", 0)), emptyList()) }
    }

    @Test
    fun `R12 a tie-breaker that is not one of the edition's own category objects is refused`() {
        assertThrows(IllegalArgumentException::class.java) { Edition("X", TABLE, listOf(first), listOf(second)) }
        // the same name is not enough: it has to be the same object
        assertThrows(IllegalArgumentException::class.java) { Edition("X", TABLE, listOf(first), listOf(points("First", 0))) }
    }

    @Test
    fun `R12 a correct edition is accepted and keeps its cards, categories and tie-break order`() {
        val edition = Edition("X", TABLE, listOf(first, second), listOf(second, first))

        assertEquals(TABLE, edition.cards)
        assertEquals(listOf(first, second), edition.categories)
        assertEquals(listOf(second, first), edition.tieBreakers)
        // and the checks above are in force: without this line the test would pass on an empty constructor
        assertThrows(IllegalArgumentException::class.java) { Edition("X", TABLE, listOf(first), listOf(second)) }
    }
}
