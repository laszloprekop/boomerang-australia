package boomerang.game

import boomerang.support.TABLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

/** R3 on the production shuffler. The source of randomness is seeded, so nothing here is left to chance. */
class ShufflerTest {

    @Test
    fun `R3 AC3_1 after the shuffle the deck holds the same 28 cards, each exactly once`() {
        val shuffled = RandomShuffler(Random(2048)).shuffle(TABLE)

        assertEquals(28, shuffled.size)
        assertEquals(TABLE.toSet(), shuffled.toSet())
    }

    @Test
    fun `R3 AC3_2 the same state of the random source gives the same order`() {
        assertEquals(RandomShuffler(Random(7)).shuffle(TABLE), RandomShuffler(Random(7)).shuffle(TABLE))
    }

    @Test
    fun `R3 AC3_3 the order is not the fixed order of the card table`() {
        // one chance in 28! that a correct shuffle with this seed returns the table order
        assertNotEquals(TABLE, RandomShuffler(Random(2048)).shuffle(TABLE))
    }

    @Test
    fun `R3 the shuffle leaves the list it was given as it was`() {
        val given = TABLE.toList()

        RandomShuffler(Random(1)).shuffle(given)

        assertEquals(TABLE, given)
    }
}
