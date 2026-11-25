package boomerang.players

import boomerang.australia.australia
import boomerang.support.ESC
import boomerang.support.withoutEscapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** D12: hands and drafts as cards lying on each other, for a real terminal. */
class CardFanTest {
    private val cards = australia().cards
    private fun hand(sites: String) = sites.map { site -> cards.first { it.site == site } }

    @Test
    fun `D12 seven cards fit the width, and every line of a fan is as long as the others`() {
        val lines = CardFan(cards, Paint(false)).lines(hand("XJGQPZD"), width = 78)

        assertEquals(1, lines.map { it.length }.toSet().size, lines.joinToString("\n"))
        assertTrue(lines.first().length <= 78, "the fan is ${lines.first().length} wide")
    }

    @Test
    fun `D12 only the last card has a right edge, every card a left one`() {
        val lines = CardFan(cards, Paint(false)).lines(hand("XJG"), width = 78)

        assertEquals(3, lines.first().count { it == '╭' })
        assertEquals(1, lines.first().count { it == '╮' })
        assertEquals(1, lines.last().count { it == '╯' })
        assertTrue(lines.drop(1).dropLast(1).all { line -> line.count { it == '│' } == 4 }, lines.joinToString("\n"))
    }

    @Test
    fun `D12 every card has its number in its top edge, its region code first, and what to type in its bottom edge`() {
        val lines = CardFan(cards, Paint(false)).lines(hand("XJGQPZD"), width = 78)

        for (card in hand("XJGQPZD")) {
            assertTrue("╭─${card.number}─" in lines.first(), lines.first())
            assertTrue("│${regionCode(card.region)} " in lines[1], lines[1])
            assertTrue("╰[${card.site}]─" in lines.last(), lines.last())
        }
    }

    @Test
    fun `D12 the blocks of a card take turns in shade`() {
        val lines = CardFan(cards, Paint(true)).lines(hand("HW"), width = 78)
        val grounds = lines.map { Regex("48;2;[0-9;]+").find(it)!!.value }

        // top edge, region, name, three symbol kinds, bottom edge: the name and the second kind are shaded
        assertEquals(listOf(true, true, false, true, false, true, true), grounds.map { it == grounds.first() })
    }

    @Test
    fun `D12 fewer cards are wider, and then nothing of a name or a symbol is cut`() {
        val shown = hand("ITAEL")
        val text = CardFan(cards, Paint(false)).lines(shown, width = 78).joinToString("\n")

        for (word in shown.flatMap { it.name.split(" ") + it.symbols.flatMap { symbol -> symbol.name.split(" ") } }) {
            assertTrue(word in text, "$word is missing in:\n$text")
        }
    }

    @Test
    fun `D12 a fan is no higher than its cards need`() {
        val lines = CardFan(cards, Paint(false)).lines(hand("HW"), width = 78)

        // two edges, the first line, a name of one line, and one line for each of the three symbol kinds
        assertEquals(7, lines.size, lines.joinToString("\n"))
    }

    @Test
    fun `D12 a mark stands in the top edge of its card`() {
        val top = CardFan(cards, Paint(false)).lines(hand("XJG"), width = 78, marks = mapOf(0 to "Throw", 2 to "Catch")).first()

        assertTrue(top.startsWith("╭─2─ Throw ") && "─ Catch " in top, top)
    }

    @Test
    fun `D12 colour adds escape sequences and nothing else, and no cards give no lines`() {
        val plain = CardFan(cards, Paint(false)).lines(hand("XJG"), width = 78)
        val coloured = CardFan(cards, Paint(true)).lines(hand("XJG"), width = 78)

        assertTrue(coloured.all { ESC in it })
        assertEquals(plain, coloured.map { it.withoutEscapes() })
        assertFalse(plain.any { ESC in it })
        assertEquals(emptyList<String>(), CardFan(cards, Paint(false)).lines(emptyList(), width = 78))
    }

    @Test
    fun `D12 a console told to shows the hand as cards, any other shows the table`() {
        fun screen(terminal: TerminalLook): String {
            val printed = StringBuilder()
            val player = ConsolePlayer(readLine = { null }, print = { printed.append(it).append('\n') }, cards = cards, terminal = terminal)
            player.show(boomerang.game.GameStarted(edition = "Boomerang Australia", seat = 0, playerCount = 2))
            player.show(boomerang.game.RoundStarted(round = 1, visitedSites = emptySet()))
            player.selectCard(boomerang.game.CardRequest(hand("XJGQPZD"), boomerang.game.Draft()))
            return printed.toString()
        }

        assertTrue("╰[X]─" in screen(TerminalLook(cardFan = true)))
        assertFalse("╰[X]─" in screen(TerminalLook()))
        assertFalse("╰[X]─" in screen(TerminalLook(frame = TerminalFrame(rows = 40))))
    }
}
