package boomerang.players

import boomerang.australia.AUSTRALIA_MAP
import boomerang.australia.australia
import boomerang.support.ESC
import boomerang.support.withoutEscapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** D13: the map as a board, for a real terminal. */
class MapBoardTest {
    private val cards = australia().cards
    private fun board(colour: Boolean = false) = MapBoard(cards, AUSTRALIA_MAP, Paint(colour))

    @Test
    fun `D13 the layout of Australia names every region of its cards`() {
        assertTrue(board().usable)
        assertEquals(cards.map { it.region }.toSet(), AUSTRALIA_MAP.flatten().filter { it.isNotEmpty() }.toSet())
    }

    @Test
    fun `D13 the board fills the width, two lines per row of the layout`() {
        val lines = board().lines(width = 78, visited = emptySet(), drafted = emptySet())

        assertEquals(2 * AUSTRALIA_MAP.size, lines.size)
        assertEquals(setOf(78), lines.map { it.length }.toSet(), lines.joinToString("\n"))
    }

    @Test
    fun `D13 every region is named once with its code, and every site stands once`() {
        val text = board().lines(78, emptySet(), emptySet()).joinToString("\n")

        for (region in cards.map { it.region }.distinct()) {
            assertEquals(1, Regex(Regex.escape("${regionCode(region).padEnd(3)} $region")).findAll(text).count(), region)
        }
        for (card in cards) {
            assertEquals(1, Regex(" ${Regex.escape(card.site.toString())} ").findAll(text).count(), "site ${card.site} in:\n$text")
        }
    }

    @Test
    fun `D13 regions lie as on the map - west left of north, north above south, east at the right`() {
        val lines = board().lines(78, emptySet(), emptySet())

        assertTrue(lines[0].indexOf("Western Australia") < lines[0].indexOf("Northern Territory"))
        assertTrue(lines[0].indexOf("Northern Territory") < lines[0].indexOf("Queensland"))
        assertEquals(lines[0].indexOf("Northern Territory"), lines[2].indexOf("South Australia"))
        assertEquals(lines[0].indexOf("QUE"), lines[4].indexOf("VIC"))
        assertTrue(lines[2].take(26).isBlank(), "Western Australia goes on as an empty block: ${lines[2]}")
    }

    @Test
    fun `D13 a visited site, a drafted site and the count of a region are marked, a whole region is complete`() {
        val lines = board().lines(78, visited = setOf('A', 'B', 'C', 'E'), drafted = setOf('D', 'F'))

        assertTrue("[A] [B] [C] (D)" in lines[1] && "complete" in lines[1], lines[1])
        assertTrue("[E] (F)  G   H " in lines[1] && "2/4" in lines[1], lines[1])
    }

    @Test
    fun `D13 colour adds escape sequences and nothing else`() {
        val plain = board().lines(78, setOf('A'), setOf('M'))
        val coloured = board(colour = true).lines(78, setOf('A'), setOf('M'))

        assertEquals(plain, coloured.map { it.withoutEscapes() })
        assertTrue(coloured.all { ESC in it })
    }

    @Test
    fun `D13 a layout that misses a region is not used, and a console without a frame keeps the list`() {
        assertFalse(MapBoard(cards, listOf(listOf("Western Australia")), Paint(false)).usable)
        assertFalse(MapBoard(cards, emptyList(), Paint(false)).usable)

        fun screen(frame: TerminalFrame?): String {
            val printed = StringBuilder()
            val player = ConsolePlayer(
                readLine = { null }, print = { printed.append(it).append('\n') }, cards = cards, frame = frame, map = AUSTRALIA_MAP,
            )
            player.show(boomerang.game.GameStarted(edition = "Boomerang Australia", seat = 0, playerCount = 2))
            player.show(boomerang.game.RoundStarted(round = 1, visitedSites = emptySet()))
            player.selectCard(boomerang.game.CardRequest(cards.take(7), boomerang.game.Draft()))
            return printed.toString()
        }
        val sameLine = Regex("Western Australia.*Northern Territory.*Queensland")

        assertTrue(sameLine.containsMatchIn(screen(TerminalFrame(rows = 40))))
        assertFalse(sameLine.containsMatchIn(screen(null)))
    }
}
