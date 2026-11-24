package boomerang.game

/** FC13 with Q18: in even rounds of a game of three or four players, hands go to the previous player and
 *  the Catch card to the next; in odd rounds, and in every round of a game of two, as StandardPassingRule. */
object AlternatingPassingRule : IPassingRule {
    /** The mirrored directions for an even round at a table of more than two, else the standard ones. */
    override fun passing(round: Int, playerCount: Int): Passing {
        val evenRound = round % 2 == 0
        if (evenRound && playerCount > 2) {
            return Passing(hands = Direction.TO_PREVIOUS, catch = Direction.TO_NEXT)
        }
        return StandardPassingRule.passing(round, playerCount)
    }
}
