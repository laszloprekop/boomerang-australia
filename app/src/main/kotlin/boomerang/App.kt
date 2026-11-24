package boomerang

import boomerang.australia.australia
import boomerang.game.AlternatingPassingRule
import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Game
import boomerang.game.GameAborted
import boomerang.game.GameEvent
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.IAnswer
import boomerang.game.IPassingRule
import boomerang.game.IPlayer
import boomerang.game.PlayerLost
import boomerang.game.RandomShuffler
import boomerang.game.StandardPassingRule
import boomerang.net.ClientSession
import boomerang.net.ConnectionLost
import boomerang.net.RemotePlayer
import boomerang.net.SocketConnection
import boomerang.net.SocketListener
import boomerang.players.BotPlayer
import boomerang.players.ConsolePlayer
import kotlin.random.Random
import kotlin.system.exitProcess

/** What the command line asks for. */
sealed interface Start

/** Run a server for a game with this many humans (who connect) and bots. `alternate`: FC13 is asked for. */
data class ServerStart(val humans: Int, val bots: Int, val alternate: Boolean = false) : Start

/** Join the game of the server at this address. */
data class ClientStart(val host: String) : Start

/** States Game.PLAYERS and the two command forms. */
data class Usage(val message: String) : Start

/** S3: "<humans> <bots>", two whole numbers not below 0 whose sum is in Game.PLAYERS, is a server;
 *  a third argument, exactly the word of ALTERNATE, asks for FC13 (AC13.3); "<host>" is a client;
 *  anything else is Usage. */
fun parseArguments(args: Array<String>): Start {
    if (args.size == 1) return ClientStart(args[0])
    if (args.size != 2 && args.size != 3) return usage()
    val alternate = args.size == 3
    if (alternate && args[2] != ALTERNATE) return usage()
    val humans = args[0].toIntOrNull()
    val bots = args[1].toIntOrNull()
    if (humans == null || bots == null) return usage()
    if (humans < 0 || bots < 0) return usage()
    if (humans + bots !in Game.PLAYERS) return usage()
    return ServerStart(humans, bots, alternate)
}

/** AC13.3: the third argument that switches on the alternating passing direction. */
private const val ALTERNATE = "alternate"

/** The text for a command line that cannot be used. The range comes from the game, not from here. */
private fun usage(): Usage = Usage(
    "Start a server with: <humans> <bots>, two whole numbers that add up to " +
        "${Game.PLAYERS.first} to ${Game.PLAYERS.last} players; add the word $ALTERNATE to pass the other way " +
        "round in even rounds. Join a game with: <address of the server>.",
)

/** FC13, AC13.3 and AC13.4: the passing rule of the game this start asks for. */
internal fun passingRuleFor(start: ServerStart): IPassingRule =
    if (start.alternate) AlternatingPassingRule else StandardPassingRule

/** The composition root; reports every failure in words (S2). */
fun main(args: Array<String>) {
    try {
        when (val start = parseArguments(args)) {
            is ServerStart -> runServer(start)
            is ClientStart -> runClient(start)
            is Usage -> fail(start.message)
        }
    } catch (lost: PlayerLost) {
        reportSuppressed(lost)
        fail("A player can no longer be reached, so the game has ended${reason(lost)}.")
    } catch (lost: ConnectionLost) {
        fail("The game cannot go on: ${lost.message}.")
    }
}

/** Prints the sentence and ends the program with a status that says it failed. */
private fun fail(sentence: String): Nothing {
    println(sentence)
    exitProcess(1)
}

/** Why the player was lost, as the end of a sentence; empty if the failure carries no cause. */
private fun reason(lost: PlayerLost): String {
    val cause = lost.cause?.message
    return if (cause == null) "" else ": $cause"
}

/** A player lost while the others were told of the first loss is attached to it (D10): say so as well. */
private fun reportSuppressed(lost: PlayerLost) {
    repeat(lost.suppressed.size) {
        println("Another player could not be told that the game has ended.")
    }
}

/** The server: waits for the humans, adds the bots, plays one game, and closes every connection. */
private fun runServer(start: ServerStart) {
    val random = Random.Default
    SocketListener(PORT).use { listener ->
        val remotes = mutableListOf<RemotePlayer>()
        try {
            acceptHumans(listener, start.humans, remotes)
            val players: List<IPlayer> = remotes + List(start.bots) { BotPlayer(random) }
            val result = Game(australia(), players, RandomShuffler(random), passingRuleFor(start)).play()
            printResult(result)
        } finally {
            closeAll(remotes)
        }
    }
}

/** Waits for one client per human. They take the seats in the order they connect, before the bots. */
private fun acceptHumans(listener: SocketListener, humans: Int, remotes: MutableList<RemotePlayer>) {
    for (number in 1..humans) {
        println("Waiting for player $number of $humans on port ${listener.port}.")
        remotes += RemotePlayer(listener.accept())
    }
    println("All players are here. The game starts.")
}

/** What the server itself prints at the end: it hosts no player, so nobody else would see it there. */
private fun printResult(result: GameResult) {
    println("The game is over.")
    for (seat in result.totals.indices) {
        println("Player ${seat + 1} has ${result.totals[seat]} points.")
    }
    for (seat in result.winners) {
        println("Player ${seat + 1} wins.")
    }
}

/** Says Bye to every client and closes its connection. A client that is already gone cannot be told;
 *  that is printed, and the remaining clients are still closed. */
private fun closeAll(remotes: List<RemotePlayer>) {
    for (remote in remotes) {
        try {
            remote.close()
        } catch (lost: ConnectionLost) {
            println("A client was gone before the end: ${lost.message}.")
        }
    }
}

/** The client: one human at this console, played over the connection to the server. */
private fun runClient(start: ClientStart) {
    SocketConnection.open(start.host, PORT).use { connection ->
        val console = ConsolePlayer(readLine = { readlnOrNull() }, print = { println(it) })
        val player = EndWatcher(console)
        try {
            ClientSession(connection, player).run()
        } catch (lost: PlayerLost) {
            fail("The input has ended, so you have left the game.")
        }
        if (!player.sawTheEnd) fail("The server ended the session before the game was over.")
    }
}

/** Stands in front of the client's player and passes everything on unchanged. It only notes whether the
 *  player was shown how the game ended, so that a session that just stops is not taken for a finished game. */
private class EndWatcher(private val player: IPlayer) : IPlayer {
    /** True once GameOver or GameAborted has been shown. */
    var sawTheEnd = false
        private set

    override fun selectCard(request: CardRequest): IAnswer<Char> = player.selectCard(request)

    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> = player.chooseScore(request)

    override fun show(event: GameEvent) {
        if (event is GameOver || event is GameAborted) sawTheEnd = true
        player.show(event)
    }
}

/** Q11: the port of the original. */
const val PORT: Int = 2048
