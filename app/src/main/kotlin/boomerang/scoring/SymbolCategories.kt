package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext
import boomerang.game.Symbol

/** R10c: the sum of the symbol values on the draft, turned into points by `toPoints`. */
class SymbolSum(
    override val name: String, private val values: Map<Symbol, Int>, private val toPoints: (Int) -> Int,
) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}

/** R10d: for each symbol, its value for every complete pair on the draft. */
class SymbolPairs(override val name: String, private val values: Map<Symbol, Int>) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}

/** R10e: one score per symbol that is on the draft and not in history.chosen(seat, this), worth
 *  pointsByCount[number of cards], in the order of `symbols`, then "take none" with 0 points. */
class ChosenSymbolCount(
    override val name: String, private val symbols: List<Symbol>, private val pointsByCount: List<Int>,
) : IScoringCategory {
    override fun scores(context: ScoringContext): List<Score> = TODO("not implemented")
}
