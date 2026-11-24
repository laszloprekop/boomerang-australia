package boomerang.australia

import boomerang.game.Card
import boomerang.game.Symbol
import boomerang.support.ALL_SITES
import boomerang.support.BUSHWALKING
import boomerang.support.EMUS
import boomerang.support.INDIGENOUS_CULTURE
import boomerang.support.KANGAROOS
import boomerang.support.KOALAS
import boomerang.support.LEAVES
import boomerang.support.PLATYPUSES
import boomerang.support.SHELLS
import boomerang.support.SIGHTSEEING
import boomerang.support.SOUVENIRS
import boomerang.support.SWIMMING
import boomerang.support.TABLE
import boomerang.support.WILDFLOWERS
import boomerang.support.WOMBATS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Rule 2: the deck of Boomerang Australia is the 28 cards of requirements section 2. */
class AustraliaCardsTest {

    @Test
    fun `R2 AC2_1 the deck has 28 cards and 28 distinct site characters, A to Z, star and dash`() {
        assertEquals(28, australiaCards.size)
        assertEquals(ALL_SITES.toSet(), australiaCards.map { it.site }.toSet())
        assertEquals(ALL_SITES, australiaCards.map { it.site }.joinToString(""), "the cards are in table order")
    }

    @Test
    fun `R2 AC2_2 every card equals its row in the table - name, site, region, number and symbols`() {
        fun row(card: Card) = listOf(card.site, card.name, card.region, card.number, card.symbols.toSet())

        assertEquals(28, australiaCards.size)
        for ((expected, actual) in TABLE zip australiaCards) assertEquals(row(expected), row(actual), "card ${expected.site}")
    }

    @Test
    fun `R2 AC2_3 there are 7 regions with exactly 4 sites each`() {
        val sitesByRegion = australiaCards.groupBy({ it.region }, { it.site })

        assertEquals(7, sitesByRegion.size)
        for ((region, sites) in sitesByRegion) assertEquals(4, sites.toSet().size, region)
    }

    @Test
    fun `R2 AC2_4 all four cards of a region carry the same number`() {
        val numbersByRegion = australiaCards.groupBy({ it.region }, { it.number }).mapValues { it.value.toSet() }

        assertEquals(
            mapOf(
                "Western Australia" to setOf(1), "Victoria" to setOf(2), "South Australia" to setOf(3),
                "Northern Territory" to setOf(4), "New South Wales" to setOf(5), "Queensland" to setOf(6),
                "Tasmania" to setOf(7),
            ),
            numbersByRegion,
        )
    }

    @Test
    fun `R2 AC2_5 every card carries exactly two symbols, of two different kinds out of Collection, Animal and Activity`() {
        assertEquals(28, australiaCards.size)
        for (card in australiaCards) {
            assertEquals(2, card.symbols.size, "symbols on card ${card.site}")
            val kinds = card.symbols.map { it.kind }.toSet()
            assertEquals(2, kinds.size, "kinds on card ${card.site}")
            assertEquals(emptySet<String>(), kinds - setOf("Collection", "Animal", "Activity"), "kinds on card ${card.site}")
        }
    }

    @Test
    fun `R2 AC2_6 the symbol counts over the deck`() {
        val counts: Map<Symbol, Int> = australiaCards.flatMap { it.symbols }.groupingBy { it }.eachCount()

        assertEquals(
            mapOf(
                LEAVES to 5, WILDFLOWERS to 4, SHELLS to 3, SOUVENIRS to 1,
                KANGAROOS to 5, EMUS to 5, WOMBATS to 4, KOALAS to 3, PLATYPUSES to 2,
                INDIGENOUS_CULTURE to 6, SIGHTSEEING to 6, BUSHWALKING to 6, SWIMMING to 6,
            ),
            counts,
        )
    }

    @Test
    fun `R2 the edition is played with exactly these cards and has the six score lines of the sheet`() {
        val edition = australia()

        assertEquals(australiaCards, edition.cards)
        assertEquals(6, edition.categories.size)
        assertEquals(6, edition.categories.map { it.name }.toSet().size, "six different names")
    }

    @Test
    fun `R12 the tie-break of the edition is its Throw and catch category`() {
        val edition = australia()

        assertEquals(listOf(edition.categories[0]), edition.tieBreakers)
    }
}
