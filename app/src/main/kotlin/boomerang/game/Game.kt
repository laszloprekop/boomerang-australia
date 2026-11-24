package boomerang.game

/** Rules 1, 3, 11 and 12: checks the player count, plays four rounds, keeps the History, names the winners.
 *  The constructor throws IllegalArgumentException naming PLAYERS if players.size is not in it (the one
 *  check of rule 1). */
class Game(
    private val edition: Edition,
    private val players: List<IPlayer>,
    private val shuffler: IShuffler,
    private val passingRule: IPassingRule = StandardPassingRule,
) {
    init {
        require(players.size in PLAYERS) {
            "a game has ${PLAYERS.first} to ${PLAYERS.last} players, not ${players.size}"
        }
    }

    /** On PlayerLost: shows GameAborted to every other player, then rethrows it. */
    fun play(): GameResult {
        try {
            return playAllRounds()
        } catch (lost: PlayerLost) {
            tellOthers(lost)
            throw lost
        }
    }

    /** The whole game: the start, four rounds, the result (R12). */
    private fun playAllRounds(): GameResult {
        showStart()
        var history = History()
        for (round in 1..ROUNDS) {
            history = history + playRound(round, history)
        }
        val result = history.result(edition)
        showToAll(GameOver(result))
        return result
    }

    /** One round (R11): a fresh shuffle of all cards, the draft, the scoring, and the scores shown. */
    private fun playRound(round: Int, history: History): RoundRecord {
        showRoundStart(round, history)
        val deck = shuffler.shuffle(edition.cards)
        val passing = passingRule.passing(round, players.size)
        val drafts = DraftPhase(players).run(deck, passing)
        val record = ScoringPhase(edition.categories, players).run(drafts, history)
        showScores(round, record)
        return record
    }

    /** Tells every player the own seat, the number of players and the edition. */
    private fun showStart() {
        for (seat in players.indices) {
            players[seat].show(GameStarted(seat, players.size, edition.name))
        }
    }

    /** Tells every player the round number and the sites that player visited in earlier rounds. */
    private fun showRoundStart(round: Int, history: History) {
        for (seat in players.indices) {
            players[seat].show(RoundStarted(round, history.visitedSites(seat)))
        }
    }

    /** AC11.1: every player is told the own score lines, and of all players the lines with an announcement. */
    private fun showScores(round: Int, record: RoundRecord) {
        val lines = players.indices.map { seat -> scoreLines(record, seat) }
        val announced = lines.map { ownLines -> ownLines.filter { it.score.announcement != null } }
        for (seat in players.indices) {
            players[seat].show(RoundScored(round, lines[seat], announced))
        }
    }

    /** The scores of one player in the round, one line per category, in the edition's order. */
    private fun scoreLines(record: RoundRecord, seat: Int): List<ScoreLine> =
        edition.categories.map { category -> ScoreLine(category.name, record.scores[seat].getValue(category)) }

    /** Tells every player the same thing. */
    private fun showToAll(event: GameEvent) {
        for (player in players) player.show(event)
    }

    /** S2: tells every player except the lost one that the game is over. A player who cannot be told is
     *  lost as well; that second loss is attached to the first, so it is not dropped. */
    private fun tellOthers(lost: PlayerLost) {
        val lostSeat = players.indexOf(lost.player)
        for (seat in players.indices) {
            if (seat == lostSeat) continue
            try {
                players[seat].show(GameAborted(lostSeat))
            } catch (second: PlayerLost) {
                lost.addSuppressed(second)
            }
        }
    }

    companion object {
        /** The one definition of rule 1. */
        val PLAYERS: IntRange = 2..4

        /** Rule 12: a game is four rounds. */
        const val ROUNDS: Int = 4

        /** Rule 4: seven cards are dealt to each player. */
        const val HAND_SIZE: Int = 7
    }
}
