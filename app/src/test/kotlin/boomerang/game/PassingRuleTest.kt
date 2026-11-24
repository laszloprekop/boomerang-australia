package boomerang.game

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Who receives what a seat passes on, and the default passing rule (R6, R9, AC11.5). FC13 is not tested. */
class PassingRuleTest {

    @Test
    fun `R6 AC6_1 TO_NEXT gives to the following seat and the last seat gives to the first`() {
        for (n in Game.PLAYERS) {
            for (seat in 0 until n - 1) assertEquals(seat + 1, TO_NEXT.receiver(seat, n), "$n players, seat $seat")
            assertEquals(0, TO_NEXT.receiver(n - 1, n), "$n players, the last seat")
        }
    }

    @Test
    fun `R9 AC9_1 TO_PREVIOUS gives to the preceding seat and the first seat gives to the last`() {
        for (n in Game.PLAYERS) {
            for (seat in 1 until n) assertEquals(seat - 1, TO_PREVIOUS.receiver(seat, n), "$n players, seat $seat")
            assertEquals(n - 1, TO_PREVIOUS.receiver(0, n), "$n players, the first seat")
        }
    }

    @Test
    fun `R6 R9 AC11_5 the standard rule passes hands to the next player and the Catch card to the previous, in every round`() {
        for (n in Game.PLAYERS) for (round in 1..Game.ROUNDS) {
            assertEquals(Passing(hands = TO_NEXT, catch = TO_PREVIOUS), StandardPassingRule.passing(round, n),
                "$n players, round $round")
        }
    }
}
