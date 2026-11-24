package boomerang

sealed interface Start
data class ServerStart(val humans: Int, val bots: Int) : Start
data class ClientStart(val host: String) : Start

/** States Game.PLAYERS and the two command forms. */
data class Usage(val message: String) : Start

/** S3: "<humans> <bots>", two whole numbers not below 0 whose sum is in Game.PLAYERS, is a server;
 *  "<host>" is a client; anything else is Usage. */
fun parseArguments(args: Array<String>): Start = TODO("not implemented")

/** The composition root; reports every failure in words (S2). */
fun main(args: Array<String>): Unit = TODO("not implemented")

const val PORT: Int = 2048
