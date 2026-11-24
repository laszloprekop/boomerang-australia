package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext

/** R10a: the absolute difference of the numbers on Throw and Catch card. */
class ThrowCatchDifference(override val name: String) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}
