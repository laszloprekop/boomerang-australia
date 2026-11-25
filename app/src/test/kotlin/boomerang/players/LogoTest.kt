package boomerang.players

import boomerang.australia.AUSTRALIA_LOGO
import boomerang.game.GameStarted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** D14: the logo of the edition above the first line of a game. */
class LogoTest {
    private val started = GameStarted(edition = "Boomerang Australia", seat = 0, playerCount = 2)

    private fun printed(terminal: TerminalLook): List<String> {
        val out = mutableListOf<String>()
        ConsolePlayer(readLine = { null }, print = { out += it }, terminal = terminal).show(started)
        return out.single().split("\n")
    }

    @Test
    fun `D14 the logo of Australia is 5 to 10 lines and fits a screen`() {
        assertTrue(AUSTRALIA_LOGO.size in 5..10)
        assertTrue(AUSTRALIA_LOGO.all { it.length <= WIDTH && it.isNotBlank() })
    }

    @Test
    fun `D14 a console given a logo prints it above the first line of the game`() {
        val lines = printed(TerminalLook(logo = AUSTRALIA_LOGO))

        assertEquals(AUSTRALIA_LOGO, lines.take(AUSTRALIA_LOGO.size))
        assertEquals("Boomerang Australia: you are player 1 of 2.", lines.last())
    }

    @Test
    fun `D14 with a frame the logo and the first line are in one box, without a logo nothing changes`() {
        val framed = printed(TerminalLook(frame = TerminalFrame(rows = 40), logo = AUSTRALIA_LOGO))

        assertTrue(framed.first().startsWith("╭") && framed.last().startsWith("╰"))
        assertTrue(framed.any { AUSTRALIA_LOGO.first() in it } && framed.any { "you are player 1 of 2" in it })
        assertEquals(listOf("Boomerang Australia: you are player 1 of 2."), printed(TerminalLook(frame = TerminalFrame(rows = 40))))
    }
}
