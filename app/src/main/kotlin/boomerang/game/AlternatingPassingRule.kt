package boomerang.game

/** FC13 with Q18: in even rounds of a game of three or four players, hands go to the previous player and
 *  the Catch card to the next; in odd rounds, and in every round of a game of two, as StandardPassingRule. */
object AlternatingPassingRule : IPassingRule {
    override fun passing(round: Int, playerCount: Int): Passing = TODO("not implemented")
}
