package boomerang.scoring

import boomerang.game.IScoringCategory
import boomerang.game.Score
import boomerang.game.ScoringContext
import boomerang.game.Symbol

/** R10c: the sum of the symbol values on the draft, turned into points by `toPoints`. */
class SymbolSum(
    override val name: String, private val values: Map<Symbol, Int>, private val toPoints: (Int) -> Int,
) : IScoringCategory {
    /** One score: every card with a symbol adds the value of that symbol; `toPoints` turns the sum into points. */
    override fun scores(context: ScoringContext): List<Score> {
        val sum = values.entries.sumOf { (symbol, value) -> value * context.draft.count(symbol) }
        return listOf(Score(toPoints(sum)))
    }
}

/** R10d: for each symbol, its value for every complete pair on the draft. */
class SymbolPairs(override val name: String, private val values: Map<Symbol, Int>) : IScoringCategory {
    /** One score. The whole-number division by 2 gives the complete pairs: 3 cards are 1 pair, 5 are 2. */
    override fun scores(context: ScoringContext): List<Score> {
        val points = values.entries.sumOf { (symbol, value) -> value * (context.draft.count(symbol) / 2) }
        return listOf(Score(points))
    }
}

/** R10e: one score per symbol that is on the draft and not in history.chosen(seat, this), worth
 *  pointsByCount[number of cards], in the order of `symbols`, then "take none" with 0 points. */
class ChosenSymbolCount(
    override val name: String, private val symbols: List<Symbol>, private val pointsByCount: List<Int>,
) : IScoringCategory {
    /** Several scores, so the player chooses: the symbols still free, then "take none". */
    override fun scores(context: ScoringContext): List<Score> {
        val draft = context.draft
        val usedUp = context.history.chosen(context.seat, this)
        val free = symbols.filter { draft.count(it) > 0 && it !in usedUp }
        val offered = free.map { Score(pointsByCount[draft.count(it)], chosen = it) }
        return offered + Score(0)
    }
}
