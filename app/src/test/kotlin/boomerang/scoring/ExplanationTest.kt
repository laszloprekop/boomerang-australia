package boomerang.scoring

import boomerang.australia.australia
import boomerang.game.History
import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext
import boomerang.game.Symbol
import boomerang.support.ACTIVITIES
import boomerang.support.BUSHWALKING
import boomerang.support.EMUS
import boomerang.support.INDIGENOUS_CULTURE
import boomerang.support.KANGAROOS
import boomerang.support.KOALAS
import boomerang.support.LEAVES
import boomerang.support.PLATYPUSES
import boomerang.support.REGION_BONUS
import boomerang.support.SHELLS
import boomerang.support.SIGHTSEEING
import boomerang.support.SOUVENIRS
import boomerang.support.SWIMMING
import boomerang.support.WILDFLOWERS
import boomerang.support.WOMBATS
import boomerang.support.draft
import boomerang.support.namesSite
import boomerang.support.numbers
import boomerang.support.roundOfDrafts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS19 (D6): a scoring class says in one line where its points come from. The
 * class with a choice does not: its score names the symbol, and the console counts the cards.
 * Each class is made here with the values of the test table and asked with a hand-built draft, written as
 * its site characters, Throw card first. The sentence is free: the tests look for the facts the table of
 * CS19 names (a site character standing alone, the name of a symbol, a number).
 */
class ExplanationTest {

    /** The scores of a category for one player alone at the table, with what went before. */
    private fun scores(category: IScoringCategory, sites: String, history: History = History()): List<Score> =
        category.scores(ScoringContext(0, listOf(draft(sites)), history))

    /** The explanation of a category that leaves no choice; a failure if there is none. */
    private fun explanation(category: IScoringCategory, sites: String, history: History = History()): String {
        val score = scores(category, sites, history).single()
        return score.explanation ?: throw AssertionError("${category.name} gives no explanation for $sites: $score")
    }

    private fun assertNames(text: String, vararg symbols: Symbol) {
        for (symbol in symbols) assertTrue(symbol.name in text, "${symbol.name} is missing in: $text")
    }

    private fun assertNumbers(text: String, vararg numbers: Int) {
        assertTrue(text.numbers().containsAll(numbers.toList()), "the numbers ${numbers.toList()} are not all in: $text")
    }

    // --- ThrowCatchDifference -------------------------------------------------------------------------

    @Test
    fun `CS19 ThrowCatchDifference explains with the site and the number of the Throw card and of the Catch card`() {
        val category = ThrowCatchDifference("Throw and catch")

        // Throw E is 4 and Catch Z is 7: the points are 3, and no card between is E, Z, a 4 or a 7
        val text = explanation(category, "EBCDIJZ")
        // the other way round, with other cards: Throw Y is 7 and Catch U is 2
        val other = explanation(category, "YBCDIJU")

        assertTrue(text.namesSite('E') && text.namesSite('Z'), "the sites E and Z are not both in: $text")
        assertNumbers(text, 4, 7)
        assertTrue(other.namesSite('Y') && other.namesSite('U') && !other.namesSite('E'), "the sites Y and U are not both in: $other")
        assertNumbers(other, 7, 2)
    }

    // --- NewSites -------------------------------------------------------------------------------------

    @Test
    fun `CS19 NewSites explains with the sites that are new`() {
        val history = History(listOf(roundOfDrafts("BCDEFGH")))

        // B, C, D and E were visited in round 1: K, L and M are new
        val text = explanation(NewSites("Tourist sites"), "BCDEKLM", history)

        for (site in "KLM") assertTrue(text.namesSite(site), "the new site $site is missing in: $text")
        for (site in "BCDE") assertTrue(!text.namesSite(site), "the site $site was visited before and is named in: $text")
    }

    @Test
    fun `CS19 NewSites explains that no site is new`() {
        val history = History(listOf(roundOfDrafts("BCDEFGH")))

        val text = explanation(NewSites("Tourist sites"), "HGFEDCB", history)

        assertTrue(text.isNotBlank(), "an empty explanation")
        for (site in "BCDEFGH") assertTrue(!text.namesSite(site), "no site is new and $site is named in: $text")
    }

    // --- RegionBonus ----------------------------------------------------------------------------------

    @Test
    fun `CS19 RegionBonus explains with nothing more than the announcement, and with nothing when the bonus is 0`() {
        val category = RegionBonus("Region bonus", listOf(Region("West", "ABCD".toSet(), 3), Region("North", "EFGH".toSet(), 3)))

        val won = scores(category, "ABCDEIM").single()
        val none = scores(category, "ABCEFIM").single()

        assertNotNull(won.announcement, "control: the region is announced")
        assertEquals(won.announcement, won.explanation, "the explanation of a bonus of ${won.points}")
        assertEquals(0, none.points, "control: no region is complete")
        assertNull(none.explanation, "the explanation of no bonus")
    }

    // --- SymbolSum ------------------------------------------------------------------------------------

    private val collections = SymbolSum("Collections", mapOf(LEAVES to 1, WILDFLOWERS to 2, SHELLS to 3, SOUVENIRS to 5)) { sum ->
        if (sum <= 7) sum * 2 else sum
    }

    @Test
    fun `CS19 SymbolSum explains with each symbol counted and its value, and with the sum`() {
        // Wildflowers on D (2) and Shells on C (3): the sum is 5. The other cards carry no collection.
        val text = explanation(collections, "DCBEFHJ")

        assertNames(text, WILDFLOWERS, SHELLS)
        assertTrue(LEAVES.name !in text && SOUVENIRS.name !in text, "a symbol that is not on the draft is named in: $text")
        assertNumbers(text, 2, 3, 5)
    }

    @Test
    fun `CS19 SymbolSum explains whether the sum was doubled`() {
        // 2 + 3 = 5 is turned into 10 points; with the Souvenirs of K, 2 + 3 + 5 = 10 stays 10
        val doubled = explanation(collections, "DCBEFHJ")
        val asItIs = explanation(collections, "DCKBEFH")

        // "doubled" is the word of the criterion; any form of it will do (doubled, double, doubling)
        assertTrue("doubl" in doubled.lowercase(), "a sum of 5 gives 10 points, and the explanation does not say it was doubled: $doubled")
        assertTrue("doubl" !in asItIs.lowercase(), "a sum of 10 gives 10 points, and the explanation says it was doubled: $asItIs")
        assertNumbers(asItIs, 2, 3, 5, 10)
    }

    // --- SymbolPairs ----------------------------------------------------------------------------------

    @Test
    fun `CS19 SymbolPairs explains with each symbol, its number of pairs and the points, and names a symbol held once as no pair`() {
        val category = SymbolPairs("Animals", mapOf(KANGAROOS to 3, EMUS to 4, WOMBATS to 5, KOALAS to 7, PLATYPUSES to 9))

        // Emus on E, N, R and T: 2 pairs, 8 points. Kangaroos on B and J: 1 pair, 3 points. Koalas on M only.
        val text = explanation(category, "ENRTBJM")

        assertNames(text, EMUS, KANGAROOS, KOALAS)
        assertTrue(WOMBATS.name !in text && PLATYPUSES.name !in text, "an animal that is not on the draft is named in: $text")
        assertNumbers(text, 2, 8, 1, 3)
        // Koalas score nothing: their 7 is not among the points
        assertTrue(7 !in text.numbers(), "Koalas are held once, and the explanation holds their value 7: $text")
    }

    // --- ChosenSymbolCount ----------------------------------------------------------------------------

    private val activities = ChosenSymbolCount(
        "Activities", listOf(SWIMMING, BUSHWALKING, INDIGENOUS_CULTURE, SIGHTSEEING), listOf(0, 0, 2, 4, 7, 10, 15),
    )

    @Test
    fun `CS19 ChosenSymbolCount gives no explanation - a score with a choice says what it is by its symbol`() {
        // Swimming on H, L and N: 3 cards, 4 points. Bushwalking on D: 1 card, 0 points. C, G and X carry no activity.
        val offered = scores(activities, "HLNDCGX")

        // whole values, as the locked tests of the activities compare them: nothing else rides on a score
        assertEquals(listOf(Score(4, chosen = SWIMMING), Score(0, chosen = BUSHWALKING), Score(0)), offered)
        for (score in offered) assertNull(score.explanation, "$score")
    }

    // --- all of them-----------------------------------------------------------------------------------

    @Test
    fun `CS19 every score of every category of the edition has an explanation, a region bonus of 0 and the activities excepted`() {
        val categories = australia().categories

        // two drafts that complete no region; between them they offer every activity and the option to take none
        for ((index, category) in categories.withIndex()) for (score in scores(category, "AEIMQUY") + scores(category, "HDJBCGX")) {
            if (index == REGION_BONUS || index == ACTIVITIES) assertNull(score.explanation, "${category.name}: $score")
            else assertTrue(!score.explanation.isNullOrBlank(), "${category.name} gives no explanation with $score")
        }
    }
}
