package boomerang

import boomerang.game.Game
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The command line (R1, S2, S3): parseArguments is a function from the arguments to a value, so nothing
 * here starts a server, opens a port or prints.
 */
class AppTest {
    private fun parse(vararg args: String): Start = parseArguments(arrayOf(*args))

    /** AC1.2: the argument list is refused with a message that states the allowed range. */
    private fun assertRefused(vararg args: String) {
        val start = parse(*args)
        assertTrue(start is Usage, "${args.toList()} was not refused: $start")
        val message = (start as Usage).message
        assertTrue("${Game.PLAYERS.first}" in message && "${Game.PLAYERS.last}" in message,
            "the message for ${args.toList()} does not state the range 2 to 4: '$message'")
    }

    @Test
    fun `R1 AC1_1 S3 one to four humans and bots that add up to 2, 3 or 4 start a server`() {
        assertEquals(ServerStart(humans = 1, bots = 1), parse("1", "1"))
        assertEquals(ServerStart(humans = 1, bots = 2), parse("1", "2"))
        assertEquals(ServerStart(humans = 1, bots = 3), parse("1", "3"))
        assertEquals(ServerStart(humans = 2, bots = 0), parse("2", "0"))
        assertEquals(ServerStart(humans = 4, bots = 0), parse("4", "0"))
    }

    @Test
    fun `R1 AC1_2 totals of 0, 1 and 5 are refused with the allowed range`() {
        assertRefused("0", "0")
        assertRefused("1", "0")
        assertRefused("0", "1")
        assertRefused("1", "4")
        assertRefused("0", "5")
        assertRefused("5", "0")
    }

    @Test
    fun `R1 AC1_3 the check is on the total of humans and bots, whatever the mix`() {
        for (humans in 0..5) for (bots in 0..5) {
            val start = parse("$humans", "$bots")
            if (humans + bots in 2..4) assertEquals(ServerStart(humans, bots), start, "$humans humans and $bots bots")
            else assertTrue(start is Usage, "$humans humans and $bots bots: $start")
        }
    }

    @Test
    fun `R1 legacy bug - 0 players and 4 bots must not become a game of five players`() {
        // The original added a player of its own to the four bots. Here the server hosts no player (Q10).
        val start = parse("0", "4")

        assertEquals(ServerStart(humans = 0, bots = 4), start)
        assertTrue((start as ServerStart).humans + start.bots <= 4, "players in the game: ${start.humans + start.bots}")
    }

    @Test
    fun `R1 AC1_2 legacy bug - a negative number of players is refused`() {
        assertRefused("-2", "4") // the sum is 2
        assertRefused("-1", "3")
        assertRefused("4", "-1")
        assertRefused("-2", "-2")
    }

    @Test
    fun `R1 AC1_2 S2 legacy bug - arguments that are not numbers are refused with a message`() {
        assertRefused("two", "bots")
        assertRefused("2", "bots")
        assertRefused("two", "2")
        assertRefused("1.5", "1")
    }

    @Test
    fun `S2 S3 no arguments and more than two arguments are refused with a message`() {
        assertRefused()
        assertRefused("1", "1", "1")
    }

    @Test
    fun `S3 one argument is the address of the server to join, and the port is 2048 (Q11)`() {
        assertEquals(ClientStart(host = "localhost"), parse("localhost"))
        assertEquals(ClientStart(host = "192.168.0.7"), parse("192.168.0.7"))
        assertEquals(2048, PORT)
    }
}
