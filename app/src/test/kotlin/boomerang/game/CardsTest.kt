package boomerang.game

import boomerang.support.KANGAROOS
import boomerang.support.LEAVES
import boomerang.support.SOUVENIRS
import boomerang.support.card
import boomerang.support.cards
import boomerang.support.draft
import boomerang.support.roundOfDrafts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Draft and ScoringContext: the values the rules 5, 9 and 10 are stated on. */
class CardsTest {

    @Test
    fun `R5 AC5_2 the Throw card is the first card of a draft`() {
        assertEquals(card('E'), draft("EFGHAIM").throwCard)
        assertEquals(card('E'), draft("E").throwCard)
    }

    @Test
    fun `R5 an empty draft has no Throw card`() {
        assertThrows(IllegalStateException::class.java) { Draft().throwCard }
    }

    @Test
    fun `R9 AC9_2 the Catch card is the seventh card of a draft`() {
        assertEquals(card('M'), draft("EFGHAIM").catchCard)
    }

    @Test
    fun `R9 a draft of fewer than seven cards has no Catch card`() {
        assertThrows(IllegalStateException::class.java) { draft("EFGHAI").catchCard }
        assertThrows(IllegalStateException::class.java) { Draft().catchCard }
    }

    @Test
    fun `R7 AC7_3 the shown cards are all cards of a draft except the Throw card`() {
        assertEquals(cards("FGHAIM"), draft("EFGHAIM").shown)
        assertEquals(cards("F"), draft("EF").shown)
        assertEquals(emptyList<Card>(), draft("E").shown)
        assertEquals(emptyList<Card>(), Draft().shown)
    }

    @Test
    fun `R7 AC7_2 a selected card is appended to the draft and the draft it was added to is unchanged`() {
        val before = draft("EF")

        val after = before + card('G')

        assertEquals(draft("EFG"), after)
        assertEquals(draft("EF"), before)
    }

    @Test
    fun `R10b the sites of a draft are the site characters of its cards`() {
        assertEquals("EFGHAIM".toSet(), draft("EFGHAIM").sites)
        assertEquals(setOf('*', '-'), draft("*-").sites)
        assertEquals(emptySet<Char>(), Draft().sites)
    }

    @Test
    fun `R10 a draft counts the cards that carry a symbol`() {
        val draft = draft("BCJADIK") // Kangaroos on B, C and J; Leaves on A only; Souvenirs on K only

        assertEquals(3, draft.count(KANGAROOS))
        assertEquals(1, draft.count(LEAVES))
        assertEquals(1, draft.count(SOUVENIRS))
        assertEquals(0, draft("ADI").count(KANGAROOS))
    }

    @Test
    fun `R10 a scoring context gives the player's own draft, the other drafts and the player count`() {
        val drafts = listOf(draft("ABCDEFG"), draft("HIJKLMN"), draft("OPQRSTU"))
        val history = History(listOf(roundOfDrafts("VWXYZ*-", "ABCDEFG", "HIJKLMN")))

        val context = ScoringContext(1, drafts, history)

        assertEquals(draft("HIJKLMN"), context.draft)
        assertEquals(listOf(draft("ABCDEFG"), draft("OPQRSTU")), context.otherDrafts)
        assertEquals(3, context.playerCount)
    }
}
