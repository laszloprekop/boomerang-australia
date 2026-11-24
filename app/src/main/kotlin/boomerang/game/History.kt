package boomerang.game

import java.io.Serializable

/** One line of a score sheet as it is shown to a person. */
data class ScoreLine(val title: String, val score: Score) : Serializable

/** Everything one finished round leaves behind. Both lists are by seat. */
data class RoundRecord(val drafts: List<Draft>, val scores: List<Map<IScoringCategory, Score>>)

/** Totals by seat, the total per category by seat in the edition's order, and the winning seats (several on a shared win). */
data class GameResult(
    val totals: List<Int>, val lines: List<List<ScoreLine>>, val winners: List<Int>,
) : Serializable

/** The finished rounds of a game: the single place where score state lives. Immutable. */
class History(val rounds: List<RoundRecord> = emptyList()) {
    /** A new history with the round added at the end; this history stays as it is. */
    operator fun plus(round: RoundRecord): History = History(rounds + round)

    /** Sites on the drafts of all finished rounds. */
    fun visitedSites(seat: Int): Set<Char> = rounds.flatMap { it.drafts[seat].sites }.toSet()

    /** What the player has used up in that category. */
    fun chosen(seat: Int, category: IScoringCategory): Set<Symbol> =
        rounds.mapNotNull { it.scores[seat][category]?.chosen }.toSet()

    /** Points in the last finished round; null in round 1. */
    fun previousPoints(seat: Int, category: IScoringCategory): Int? {
        val last = rounds.lastOrNull() ?: return null
        return points(last, seat, category)
    }

    /** The player's points in all categories over all finished rounds. */
    fun total(seat: Int): Int = rounds.sumOf { round -> round.scores[seat].values.sumOf { it.points } }

    /** The player's points in one category over all finished rounds. */
    fun total(seat: Int, category: IScoringCategory): Int = rounds.sumOf { points(it, seat, category) }

    /** Highest total, then each of edition.tieBreakers in order (R12). */
    fun result(edition: Edition): GameResult {
        check(rounds.isNotEmpty()) { "no round is finished, so there is no result" }
        val seats = rounds.first().scores.indices.toList()
        val totals = seats.map { total(it) }
        val lines = seats.map { seat -> edition.categories.map { ScoreLine(it.name, Score(total(seat, it))) } }
        return GameResult(totals, lines, winners(seats, edition.tieBreakers))
    }

    /** The seats with the highest total; while several are left, each tie-breaker in turn keeps its best. */
    private fun winners(seats: List<Int>, tieBreakers: List<IScoringCategory>): List<Int> {
        var leaders = best(seats) { total(it) }
        for (tieBreaker in tieBreakers) {
            leaders = best(leaders) { total(it, tieBreaker) }
        }
        return leaders
    }

    /** Those of the seats that have the most points; `seats` is not empty. */
    private fun best(seats: List<Int>, pointsOf: (Int) -> Int): List<Int> {
        val highest = seats.maxOf(pointsOf)
        return seats.filter { pointsOf(it) == highest }
    }

    /** The player's points in one category in one round; 0 if the round has no score for it. */
    private fun points(round: RoundRecord, seat: Int, category: IScoringCategory): Int =
        round.scores[seat][category]?.points ?: 0
}
