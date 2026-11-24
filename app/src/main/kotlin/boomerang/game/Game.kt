package boomerang.game

/** Rules 1, 3, 11 and 12: checks the player count, plays four rounds, keeps the History, names the winners.
 *  The constructor is to throw IllegalArgumentException naming PLAYERS if players.size is not in it (the one
 *  check of rule 1). Stub: the check is not implemented yet. */
class Game(
    private val edition: Edition,
    private val players: List<IPlayer>,
    private val shuffler: IShuffler,
    private val passingRule: IPassingRule = StandardPassingRule,
) {
    /** On PlayerLost: shows GameAborted to every other player, then rethrows it. */
    fun play(): GameResult = TODO("not implemented")

    companion object {
        /** The one definition of rule 1. */
        val PLAYERS: IntRange = 2..4
        const val ROUNDS: Int = 4
        const val HAND_SIZE: Int = 7
    }
}
