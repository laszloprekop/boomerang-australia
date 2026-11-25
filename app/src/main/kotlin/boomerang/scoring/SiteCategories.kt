package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext

/** R10b, first half: one point per site on the draft that is not in history.visitedSites(seat). */
class NewSites(override val name: String) : IScoringCategory {
    /** One score: the number of sites of this round that the player had not visited before. */
    override fun scores(context: ScoringContext): List<Score> {
        val newSites = context.draft.sites - context.history.visitedSites(context.seat)
        return listOf(Score(newSites.size, explanation = explain(newSites)))
    }

    /** The new sites in the order drafted, with their number in front; or that there is none. */
    private fun explain(newSites: Set<Char>): String {
        if (newSites.isEmpty()) return "no new site"
        val word = if (newSites.size == 1) "new site" else "new sites"
        return "${newSites.size} $word: ${newSites.joinToString(" ")}"
    }
}

/** A group of sites with the bonus for completing it first. */
data class Region(val name: String, val sites: Set<Char>, val bonus: Int)

/** R10b, second half: the bonus of every region this player completes this round and no player had complete
 *  after an earlier round. Announces the region names. */
class RegionBonus(override val name: String, private val regions: List<Region>) : IScoringCategory {
    /** One score: the sum of the bonuses won this round, with the names of the regions as the announcement.
     *  The explanation says the same as the announcement; a bonus of 0 has neither. */
    override fun scores(context: ScoringContext): List<Score> {
        val won = regions.filter { completeNow(it, context) && !completeBefore(it, context) }
        if (won.isEmpty()) return listOf(Score(0))
        val names = won.joinToString(", ") { it.name }
        return listOf(Score(won.sumOf { it.bonus }, announcement = names, explanation = names))
    }

    /** True if the player has visited every site of the region, counting earlier rounds and this draft. */
    private fun completeNow(region: Region, context: ScoringContext): Boolean {
        val visited = context.history.visitedSites(context.seat) + context.draft.sites
        return visited.containsAll(region.sites)
    }

    /** True if any player, this one included, had every site of the region after an earlier round. */
    private fun completeBefore(region: Region, context: ScoringContext): Boolean =
        (0 until context.playerCount).any { seat -> context.history.visitedSites(seat).containsAll(region.sites) }
}
