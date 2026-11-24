package boomerang.game

enum class Direction {
    TO_NEXT, TO_PREVIOUS;

    /** The seat that receives what `seat` passes on. */
    fun receiver(seat: Int, playerCount: Int): Int = TODO("not implemented")
}

/** The two directions of one round. */
data class Passing(val hands: Direction, val catch: Direction)

/** Decides which way the hands and the Catch card travel in a round (round is 1 to 4). */
fun interface IPassingRule { fun passing(round: Int, playerCount: Int): Passing }

/** Brief rules 6 and 9 with Q1: hands to the next player, Catch card to the previous, in every round (AC11.5). */
object StandardPassingRule : IPassingRule {
    override fun passing(round: Int, playerCount: Int): Passing = TODO("not implemented")
}
