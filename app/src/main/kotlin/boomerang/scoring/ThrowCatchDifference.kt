package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext
import kotlin.math.abs

/** R10a: the absolute difference of the numbers on Throw and Catch card. */
class ThrowCatchDifference(override val name: String) : IScoringCategory {
    /** One score: the larger number minus the smaller. The explanation names both cards and their numbers. */
    override fun scores(context: ScoringContext): List<Score> {
        val thrown = context.draft.throwCard
        val caught = context.draft.catchCard
        val explanation = "Throw ${thrown.site} is ${thrown.number}, Catch ${caught.site} is ${caught.number}"
        return listOf(Score(abs(thrown.number - caught.number), explanation = explanation))
    }
}
