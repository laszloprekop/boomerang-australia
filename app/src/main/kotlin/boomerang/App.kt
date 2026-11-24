package boomerang

import boomerang.australia.australia
import boomerang.game.Game
import boomerang.game.GameResult
import boomerang.game.IPlayer
import boomerang.game.PlayerLost
import boomerang.game.RandomShuffler
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

/** Run a server for a game with this many humans (who connect) and bots. */
data class ServerStart(val humans: Int, val bots: Int) : Start

/** Join the game of the server at this address. */
data class ClientStart(val host: String) : Start

/** States Game.PLAYERS and the two command forms. */
data class Usage(val message: String) : Start

/** S3: "<humans> <bots>", two whole numbers not below 0 whose sum is in Game.PLAYERS, is a server;
 *  "<host>" is a client; anything else is Usage. */
fun parseArguments(args: Array<String>): Start {
    if (args.size == 1) return ClientStart(args[0])
    if (args.size != 2) return usage()
    val humans = args[0].toIntOrNull()
    val bots = args[1].toIntOrNull()
    if (humans == null || bots == null) return usage()
    if (humans < 0 || bots < 0) return usage()
    if (humans + bots !in Game.PLAYERS) return usage()
    return ServerStart(humans, bots)
}

/** The text for a command line that cannot be used. The range comes from the game, not from here. */
private fun usage(): Usage = Usage(
    "Start a server with: <humans> <bots>, two whole numbers that add up to " +
        "${Game.PLAYERS.first} to ${Game.PLAYERS.last} players. Join a game with: <address of the server>.",
)

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
        fail("A player can no longer be reached, so the game has ended.")
    } catch (lost: ConnectionLost) {
        fail("The game cannot go on: ${lost.message}.")
    }
}

/** Prints the sentence and ends the program with a status that says it failed. */
private fun fail(sentence: String): Nothing {
    println(sentence)
    exitProcess(1)
}

/** A player lost while the others were told of the first loss is attached to it (D10): say so as well. */
private fun reportSuppressed(lost: PlayerLost) {
    for (other in lost.suppressed) {
        println("Another player could not be told: $other")
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
            val result = Game(australia(), players, RandomShuffler(random)).play()
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
    println("The game is over. Totals by player: ${result.totals}. Winning player: ${result.winners.map { it + 1 }}.")
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
        val player = ConsolePlayer(readLine = { readlnOrNull() }, print = { println(it) })
        ClientSession(connection, player).run()
    }
}

/** Q11: the port of the original. */
const val PORT: Int = 2048
