package boomerang.game

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * FC13 on the rule itself (AC13.1, AC13.2, Q18): AlternatingPassingRule is asked for the directions of a
 * round, as Game asks it. No game is played here.
 */
class AlternatingPassingRuleTest {

    private val asRules6And9 = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)
    private val mirrored = Passing(hands = TO_PREVIOUS, catch = TO_NEXT)

    @Test
    fun `FC13 AC13_1 with three and four players the odd rounds pass as R6 and R9 say - hands to the next player, Catch card to the previous`() {
        for (n in 3..4) for (round in listOf(1, 3)) {
            assertEquals(asRules6And9, AlternatingPassingRule.passing(round, n), "$n players, round $round")
        }
    }

    @Test
    fun `FC13 AC13_1 with three and four players the even rounds are mirrored completely - hands to the previous player, Catch card to the next`() {
        for (n in 3..4) for (round in listOf(2, 4)) {
            assertEquals(mirrored, AlternatingPassingRule.passing(round, n), "$n players, round $round")
        }
    }

    @Test
    fun `FC13 AC13_1 rounds 1 to 4 alternate, starting as R6 and R9 say, and both directions change together`() {
        for (n in 3..4) {
            val rounds = (1..Game.ROUNDS).map { AlternatingPassingRule.passing(it, n) }

            assertEquals(listOf(asRules6And9, mirrored, asRules6And9, mirrored), rounds, "$n players")
            for (passing in rounds) assertEquals(false, passing.hands == passing.catch, "$n players: hands and Catch card travel the same way in $passing")
        }
    }

    @Test
    fun `FC13 AC13_1 in an even round a hand goes to the seat before and the Catch card to the seat after, round the table`() {
        // three players: seat 0 gives its hand to seat 2 and its last card to seat 1
        val three = AlternatingPassingRule.passing(round = 2, playerCount = 3)
        assertEquals(listOf(2, 0, 1), (0..2).map { three.hands.receiver(it, 3) }, "who receives the hand of seat 0, 1, 2")
        assertEquals(listOf(1, 2, 0), (0..2).map { three.catch.receiver(it, 3) }, "who receives the last card of seat 0, 1, 2")
        // four players: seat 0 gives its hand to seat 3 and its last card to seat 1
        val four = AlternatingPassingRule.passing(round = 4, playerCount = 4)
        assertEquals(listOf(3, 0, 1, 2), (0..3).map { four.hands.receiver(it, 4) }, "who receives the hand of seat 0, 1, 2, 3")
        assertEquals(listOf(1, 2, 3, 0), (0..3).map { four.catch.receiver(it, 4) }, "who receives the last card of seat 0, 1, 2, 3")
    }

    @Test
    fun `FC13 AC13_2 with two players every round passes as R6 and R9 say`() {
        for (round in 1..Game.ROUNDS) {
            assertEquals(asRules6And9, AlternatingPassingRule.passing(round, 2), "2 players, round $round")
            assertEquals(StandardPassingRule.passing(round, 2), AlternatingPassingRule.passing(round, 2), "2 players, round $round, against the standard rule")
        }
    }
}
