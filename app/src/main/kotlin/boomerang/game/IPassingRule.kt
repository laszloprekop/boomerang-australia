package boomerang.game

import java.io.Serializable

/** A way to pass cards round the table: to the next seat or to the previous one. */
enum class Direction {
    TO_NEXT, TO_PREVIOUS;

    /** The seat that receives what `seat` passes on. */
    fun receiver(seat: Int, playerCount: Int): Int = when (this) {
        TO_NEXT -> (seat + 1) % playerCount
        // playerCount is added so that seat 0 gives the last seat and not -1
        TO_PREVIOUS -> (seat - 1 + playerCount) % playerCount
    }
}

/** The two directions of one round. Serializable: it travels in RoundStarted (console-screens D5). */
data class Passing(val hands: Direction, val catch: Direction) : Serializable

/** Decides which way the hands and the Catch card travel in a round (round is 1 to 4). */
fun interface IPassingRule { fun passing(round: Int, playerCount: Int): Passing }

/** Brief rules 6 and 9 with Q1: hands to the next player, Catch card to the previous, in every round (AC11.5). */
object StandardPassingRule : IPassingRule {
    override fun passing(round: Int, playerCount: Int): Passing =
        Passing(hands = Direction.TO_NEXT, catch = Direction.TO_PREVIOUS)
}
