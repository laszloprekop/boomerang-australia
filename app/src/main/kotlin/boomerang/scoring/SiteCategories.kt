package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext

/** R10b, first half: one point per site on the draft that is not in history.visitedSites(seat). */
class NewSites(override val name: String) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}

/** A group of sites with the bonus for completing it first. */
data class Region(val name: String, val sites: Set<Char>, val bonus: Int)

/** R10b, second half: the bonus of every region this player completes this round and no player had complete
 *  after an earlier round. Announces the region names. */
class RegionBonus(override val name: String, private val regions: List<Region>) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}
