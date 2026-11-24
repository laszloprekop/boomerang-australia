package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext
import kotlin.math.abs

/** R10a: the absolute difference of the numbers on Throw and Catch card. */
class ThrowCatchDifference(override val name: String) : IScoringCategory {
    /** One score: the larger number minus the smaller. */
    override fun scores(context: ScoringContext): List<Score> {
        val draft = context.draft
        return listOf(Score(abs(draft.throwCard.number - draft.catchCard.number)))
    }
}
