package boomerang.game

import java.io.Serializable

/** A score a player can take in one category in one round. */
data class Score(
    val points: Int,
    val chosen: Symbol? = null,        // what the player uses up by taking this score (an activity, a treasure)
    val announcement: String? = null,  // told to all players, for example the name of a completed region
) : Serializable

/** What a category may look at when it scores one player for the round just drafted. */
class ScoringContext(val seat: Int, val drafts: List<Draft>, val history: History) {
    /** drafts[seat] */
    val draft: Draft get() = drafts[seat]

    /** The drafts of all other players, this round. */
    val otherDrafts: List<Draft> get() = drafts.filterIndexed { index, _ -> index != seat }

    /** One draft per player, so the number of drafts. */
    val playerCount: Int get() = drafts.size
}

/** One line of the score sheet with the rule that fills it. Stateless: the same context gives the same list. */
interface IScoringCategory {
    /** Display text only; never used to find anything. */
    val name: String

    /** The scores this player may take this round, never empty. One element: that is the score.
     *  Several: the player chooses one; they are listed in the edition's order, "take none" last. */
    fun scores(context: ScoringContext): List<Score>
}
