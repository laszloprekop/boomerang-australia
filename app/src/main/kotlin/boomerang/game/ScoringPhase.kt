package boomerang.game

/** Rule 10: for every category, asks the players who have a choice, and records every player's score. */
class ScoringPhase(private val categories: List<IScoringCategory>, private val players: List<IPlayer>) {
    /** Scores the drafts of one round, category by category, and returns the round as it goes into the History. */
    fun run(drafts: List<Draft>, history: History): RoundRecord {
        val scores = List(players.size) { mutableMapOf<IScoringCategory, Score>() }
        for (category in categories) {
            val taken = scoreCategory(category, drafts, history)
            for (seat in players.indices) {
                scores[seat][category] = taken[seat]
            }
        }
        return RoundRecord(drafts, scores)
    }

    /** The score of each seat in one category. A player whose list has several scores chooses one. */
    private fun scoreCategory(category: IScoringCategory, drafts: List<Draft>, history: History): List<Score> {
        val requests = mutableListOf<ChoiceRequest>()
        for (seat in players.indices) {
            val offered = category.scores(ScoringContext(seat, drafts, history))
            // the contract of a category: never an empty list
            check(offered.isNotEmpty()) { "${category.name} offered no score" }
            requests += ChoiceRequest(category.name, offered, drafts[seat])
        }
        val answers = askChoosers(requests)
        val taken = mutableListOf<Score>()
        for (seat in players.indices) {
            val answer = answers[seat]
            // no answer means the player was not asked: the list has one score, and that is the score
            taken += if (answer == null) requests[seat].scores.first() else awaitChoice(seat, answer, requests[seat])
        }
        return taken
    }

    /** Puts the question to every player who has a choice, before any answer is read. By seat; null where
     *  nothing was asked. */
    private fun askChoosers(requests: List<ChoiceRequest>): List<IAnswer<Int>?> {
        val answers = mutableListOf<IAnswer<Int>?>()
        for (seat in players.indices) {
            val hasChoice = requests[seat].scores.size > 1
            answers += if (hasChoice) players[seat].chooseScore(requests[seat]) else null
        }
        return answers
    }

    /** Waits for the answer of one player. An index outside the list changes nothing: the same player is
     *  asked again and told what was refused (S1). */
    private fun awaitChoice(seat: Int, answer: IAnswer<Int>, request: ChoiceRequest): Score {
        var index = answer.await()
        while (index !in request.scores.indices) {
            index = players[seat].chooseScore(request.copy(refused = index)).await()
        }
        return request.scores[index]
    }
}
