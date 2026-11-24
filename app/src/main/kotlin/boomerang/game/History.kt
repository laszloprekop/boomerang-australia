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
    operator fun plus(round: RoundRecord): History = TODO("not implemented")

    /** Sites on the drafts of all finished rounds. */
    fun visitedSites(seat: Int): Set<Char> = TODO("not implemented")

    /** What the player has used up in that category. */
    fun chosen(seat: Int, category: IScoringCategory): Set<Symbol> = TODO("not implemented")

    /** Points in the last finished round; null in round 1. */
    fun previousPoints(seat: Int, category: IScoringCategory): Int? = TODO("not implemented")

    fun total(seat: Int): Int = TODO("not implemented")

    fun total(seat: Int, category: IScoringCategory): Int = TODO("not implemented")

    /** Highest total, then each of edition.tieBreakers in order (R12). */
    fun result(edition: Edition): GameResult = TODO("not implemented")
}
