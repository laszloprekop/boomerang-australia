package boomerang.support

import boomerang.game.Draft
import boomerang.game.Edition
import boomerang.game.IScoringCategory
import boomerang.game.RoundRecord
import boomerang.game.Score
import boomerang.game.ScoringContext

/** A scoring category for tests: the scores come from the function it is given, and every call is recorded. */
class FixedCategory(
    override val name: String,
    private val answer: (ScoringContext) -> List<Score>,
) : IScoringCategory {
    val contexts = mutableListOf<ScoringContext>()

    override fun scores(context: ScoringContext): List<Score> {
        contexts += context
        return answer(context)
    }
}

/** A category without a choice: these points for seat 0, 1 and so on, in every round. */
fun points(name: String, vararg bySeat: Int) = FixedCategory(name) { listOf(Score(bySeat[it.seat])) }

/** An edition of the 28 cards of the table with the given categories; the tie-breakers default to none. */
fun editionOf(vararg categories: IScoringCategory, tieBreakers: List<IScoringCategory> = emptyList()) =
    Edition("Test edition", TABLE, categories.toList(), tieBreakers)

/** A finished round in which only the scores matter: one map per seat, empty drafts. */
fun roundOf(vararg scoresBySeat: Map<IScoringCategory, Score>) =
    RoundRecord(List(scoresBySeat.size) { Draft() }, scoresBySeat.toList())

/** A finished round in which only the drafts matter: one string of site characters per seat, no scores. */
fun roundOfDrafts(vararg draftsBySeat: String) =
    RoundRecord(draftsBySeat.map(::draft), List(draftsBySeat.size) { emptyMap() })
