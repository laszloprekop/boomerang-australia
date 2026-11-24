package boomerang.game

/** Rule 10: for every category, asks the players who have a choice, and records every player's score. */
class ScoringPhase(private val categories: List<IScoringCategory>, private val players: List<IPlayer>) {
    fun run(drafts: List<Draft>, history: History): RoundRecord = TODO("not implemented")
}
