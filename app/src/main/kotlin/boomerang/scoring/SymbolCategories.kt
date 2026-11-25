package boomerang.scoring

import boomerang.game.Draft
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
        val draft = context.draft
        val sum = values.entries.sumOf { (symbol, value) -> value * draft.count(symbol) }
        val points = toPoints(sum)
        return listOf(Score(points, explanation = explain(draft, sum, points)))
    }

    /** Each symbol on the draft with its value once per card, then the sum, then what became of the sum. */
    private fun explain(draft: Draft, sum: Int, points: Int): String {
        val counted = values.entries.filter { (symbol, _) -> draft.count(symbol) > 0 }
        if (counted.isEmpty()) return NOTHING_TO_COUNT
        val parts = counted.joinToString(", ") { (symbol, value) -> symbol.name + " " + repeated(value, draft.count(symbol)) }
        return "$parts = $sum${outcome(sum, points)}"
    }

    /** The value written once per card: 2 on three cards is "2 + 2 + 2". */
    private fun repeated(value: Int, times: Int): String = List(times) { value }.joinToString(" + ")

    /** What `toPoints` did to the sum, as the end of the explanation: nothing, doubled it, or something else. */
    private fun outcome(sum: Int, points: Int): String = when (points) {
        sum -> ""
        sum * 2 -> ", doubled"
        else -> ", which gives $points"
    }
}

/** R10d: for each symbol, its value for every complete pair on the draft. */
class SymbolPairs(override val name: String, private val values: Map<Symbol, Int>) : IScoringCategory {
    /** One score. The whole-number division by 2 gives the complete pairs: 3 cards are 1 pair, 5 are 2. */
    override fun scores(context: ScoringContext): List<Score> {
        val draft = context.draft
        val points = values.entries.sumOf { (symbol, value) -> value * pairs(draft, symbol) }
        return listOf(Score(points, explanation = explain(draft)))
    }

    /** The complete pairs of the symbol on the draft. */
    private fun pairs(draft: Draft, symbol: Symbol): Int = draft.count(symbol) / 2

    /** Each symbol on the draft with its pairs and their points; a symbol with no pair is named as such,
     *  without its value. */
    private fun explain(draft: Draft): String {
        val held = values.entries.filter { (symbol, _) -> draft.count(symbol) > 0 }
        if (held.isEmpty()) return NOTHING_TO_COUNT
        return held.joinToString(", ") { (symbol, value) -> symbol.name + " " + pairText(pairs(draft, symbol), value) }
    }

    /** "no pair", "1 pair 3" or "2 pairs 8": the number of pairs and the points they give. */
    private fun pairText(pairs: Int, value: Int): String = when (pairs) {
        0 -> "no pair"
        1 -> "1 pair $value"
        else -> "$pairs pairs ${pairs * value}"
    }
}

/** The explanation of a symbol category when the draft carries none of its symbols. */
private const val NOTHING_TO_COUNT = "none on the draft"

/** R10e: one score per symbol that is on the draft and not in history.chosen(seat, this), worth
 *  pointsByCount[number of cards], in the order of `symbols`, then "take none" with 0 points. */
class ChosenSymbolCount(
    override val name: String, private val symbols: List<Symbol>, private val pointsByCount: List<Int>,
) : IScoringCategory {
    init {
        require(pointsByCount.isNotEmpty()) { "$name has a points table with no entry" }
    }

    /** Several scores, so the player chooses: the symbols still free, then "take none". No score has an
     *  explanation: it names its symbol, and the console counts the cards that carry it (console-screens D6). */
    override fun scores(context: ScoringContext): List<Score> {
        val draft = context.draft
        val usedUp = context.history.chosen(context.seat, this)
        val free = symbols.filter { draft.count(it) > 0 && it !in usedUp }
        val offered = free.map { Score(pointsFor(draft.count(it)), chosen = it) }
        return offered + Score(0)
    }

    /** The points for this many cards. A count the table has no entry for is a fault in the edition's
     *  data: it is reported with the category and the count, and not rounded to the last entry. */
    private fun pointsFor(count: Int): Int {
        check(count < pointsByCount.size) { "$name has no points for $count cards" }
        return pointsByCount[count]
    }
}
