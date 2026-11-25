package boomerang.players

import boomerang.support.ESC
import boomerang.support.withoutEscapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** D11: the frame a real terminal gets around a screen. */
class TerminalFrameTest {
    private val rule = "-".repeat(WIDTH)
    private val screen = listOf("", "HEAD", "=".repeat(WIDTH), "MAP", "  a row", "", rule, "HAND", "", "Type:").joinToString("\n")

    @Test
    fun `D11 a screen stands in a box with rounded corners, every row as wide as the others`() {
        val rows = TerminalFrame(rows = 40).around(screen, newPage = false).split("\n")

        assertTrue(rows.first().startsWith("╭") && rows.first().endsWith("╮"), rows.first())
        assertTrue(rows.last().startsWith("╰") && rows.last().endsWith("╯"), rows.last())
        assertEquals(setOf(WIDTH + 4), rows.map { it.length }.toSet())
    }

    @Test
    fun `D11 empty lines and lines across the width become one divider each, none at the start or the end`() {
        val rows = TerminalFrame(rows = 40).around(screen, newPage = false, promptLines = 1).split("\n")
        val kinds = rows.map { it.first() }.joinToString("")

        assertEquals("╭│├││├│╰T", kinds)
        assertFalse(rows.any { rule in it }, "a line of the screen is still there as text")
    }

    @Test
    fun `D11 the lines that say what to type stay below the box`() {
        val rows = TerminalFrame(rows = 40).around(screen, newPage = false, promptLines = 1).split("\n")

        assertEquals("Type:", rows.last())
        assertTrue(rows[rows.size - 2].startsWith("╰"))
    }

    @Test
    fun `D11 a new page moves the terminal up by its height and puts the cursor at the top`() {
        val framed = TerminalFrame(rows = 40).around(screen, newPage = true)

        assertTrue(framed.startsWith("\n".repeat(40) + "$ESC[H╭"), framed.take(60))
        assertFalse(ESC in TerminalFrame(rows = 40).around(screen, newPage = false))
    }

    @Test
    fun `D11 colour in a line does not count as width, and the box itself is coloured only with colour on`() {
        val coloured = "$ESC[1mHEAD$ESC[0m"
        val rows = TerminalFrame(rows = 40, colour = true).around(coloured, newPage = false).split("\n")

        assertEquals(setOf(WIDTH + 4), rows.map { it.withoutEscapes().length }.toSet())
        assertTrue(ESC in rows.first())
    }

    @Test
    fun `D11 a console without a frame prints its screens as before, and with one only after the game started`() {
        val printed = mutableListOf<String>()
        val player = ConsolePlayer(readLine = { null }, print = { printed += it }, terminal = TerminalLook(frame = TerminalFrame(rows = 40)))

        player.show(boomerang.game.GameAborted(lostSeat = 0))

        assertFalse(printed.single().startsWith("╭"), "a text from before the start of a game was framed")
    }
}
