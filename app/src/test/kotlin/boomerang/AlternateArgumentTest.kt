package boomerang

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.game.Game
import boomerang.game.Passing
import boomerang.game.StandardPassingRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * FC13 at start-up (AC13.3, AC13.4): the third argument, and the passing rule a parsed start leads to.
 * Both are functions from a value to a value, so nothing here starts a server, opens a port or prints.
 */
class AlternateArgumentTest {
    private fun parse(vararg args: String): Start = parseArguments(arrayOf(*args))

    /** The argument list is refused with the usage text, which states the allowed range (AC1.2). */
    private fun assertRefused(vararg args: String) {
        val start = parse(*args)
        assertTrue(start is Usage, "${args.toList()} was not refused: $start")
        val message = (start as Usage).message
        assertTrue("${Game.PLAYERS.first}" in message && "${Game.PLAYERS.last}" in message,
            "the message for ${args.toList()} does not state the range 2 to 4: '$message'")
    }

    /** The directions a rule gives for rounds 1 to 4 of a game of this many players. */
    private fun rounds(start: ServerStart): List<Passing> {
        val rule = passingRuleFor(start)
        return (1..Game.ROUNDS).map { rule.passing(it, start.humans + start.bots) }
    }

    private val asRules6And9 = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)
    private val mirrored = Passing(hands = TO_PREVIOUS, catch = TO_NEXT)

    @Test
    fun `FC13 AC13_3 a third argument alternate after humans and bots starts a server that asks for the rule`() {
        assertEquals(ServerStart(humans = 2, bots = 1, alternate = true), parse("2", "1", "alternate"))
        assertEquals(ServerStart(humans = 1, bots = 3, alternate = true), parse("1", "3", "alternate"))
        assertEquals(ServerStart(humans = 0, bots = 4, alternate = true), parse("0", "4", "alternate"))
        // a game of two may be started with the rule; AC13.2 says how it then passes
        assertEquals(ServerStart(humans = 1, bots = 1, alternate = true), parse("1", "1", "alternate"))
    }

    @Test
    fun `FC13 AC13_4 without a third argument the server does not ask for the rule`() {
        val start = parse("2", "1")

        assertEquals(ServerStart(humans = 2, bots = 1, alternate = false), start)
        assertEquals(false, (start as ServerStart).alternate)
        assertEquals(ServerStart(humans = 2, bots = 1), start, "a start written as before FC13 is the start without the rule")
    }

    @Test
    fun `FC13 AC13_3 any other third argument is refused with the usage text`() {
        assertRefused("2", "1", "something")
        assertRefused("2", "1", "alternating")
        assertRefused("2", "1", "")
        assertRefused("2", "1", "1")
        // the word is `alternate` as AC13.3 writes it; another spelling is another argument
        assertRefused("2", "1", "ALTERNATE")
    }

    @Test
    fun `FC13 AC13_3 a fourth argument is refused with the usage text`() {
        assertRefused("2", "1", "alternate", "alternate")
        assertRefused("2", "1", "alternate", "1")
        assertRefused("2", "1", "something", "alternate")
    }

    @Test
    fun `FC13 AC13_3 alternate is the third argument only, and it does not lift the check of rule 1`() {
        assertRefused("alternate", "2", "1")
        assertRefused("2", "alternate", "1")
        assertRefused("2", "alternate")
        assertRefused("0", "1", "alternate")
        assertRefused("3", "2", "alternate")
        assertRefused("-1", "3", "alternate")
        assertRefused("two", "1", "alternate")
        for (humans in 0..5) for (bots in 0..5) {
            val start = parse("$humans", "$bots", "alternate")
            if (humans + bots in Game.PLAYERS) assertEquals(ServerStart(humans, bots, alternate = true), start, "$humans humans and $bots bots")
            else assertTrue(start is Usage, "$humans humans and $bots bots: $start")
        }
    }

    @Test
    fun `FC13 AC13_3 the usage text of any refused command line names the word alternate`() {
        val refused = listOf(
            emptyList(), listOf("0", "0"), listOf("two", "bots"), listOf("2", "1", "something"),
            listOf("2", "1", "ALTERNATE"), listOf("2", "1", "alternate", "1"),
        )
        for (args in refused) {
            val start = parseArguments(args.toTypedArray())
            assertTrue(start is Usage, "$args was not refused: $start")
            val message = (start as Usage).message
            assertTrue(Regex("\\balternate\\b").containsMatchIn(message), "the usage text for $args does not name the option alternate: '$message'")
        }
    }

    @Test
    fun `FC13 AC13_3 AC13_1 a start that asks for the rule gets a game that alternates`() {
        assertEquals(listOf(asRules6And9, mirrored, asRules6And9, mirrored), rounds(ServerStart(humans = 2, bots = 1, alternate = true)), "three players")
        assertEquals(listOf(asRules6And9, mirrored, asRules6And9, mirrored), rounds(ServerStart(humans = 1, bots = 3, alternate = true)), "four players")
        assertEquals(listOf(asRules6And9, mirrored, asRules6And9, mirrored), rounds(parse("2", "1", "alternate") as ServerStart), "from the command line")
    }

    @Test
    fun `FC13 AC13_3 AC13_2 a start of two players that asks for the rule gets a game that passes as R6 and R9 say`() {
        assertEquals(List(4) { asRules6And9 }, rounds(ServerStart(humans = 1, bots = 1, alternate = true)))
    }

    @Test
    fun `FC13 AC13_4 a start that does not ask for the rule gets the standard rule, as before`() {
        for ((humans, bots) in listOf(1 to 1, 2 to 1, 1 to 3)) {
            assertEquals(List(4) { asRules6And9 }, rounds(ServerStart(humans, bots)), "$humans humans and $bots bots")
            assertTrue(passingRuleFor(ServerStart(humans, bots)) === StandardPassingRule, "$humans humans and $bots bots: not the rule Game uses by default")
        }
        assertTrue(passingRuleFor(parse("2", "1") as ServerStart) === StandardPassingRule, "from the command line")
    }
}
