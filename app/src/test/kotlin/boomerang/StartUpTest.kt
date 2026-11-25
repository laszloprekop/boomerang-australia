package boomerang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS20: what App.kt decides before a game starts. Both decisions are functions
 * of values, so nothing here needs a terminal, an environment variable or a socket.
 */
class StartUpTest {

    @Test
    fun `CS20 colour is on only with a console and without NO_COLOR`() {
        assertEquals(true, useColour(hasConsole = true, noColor = null), "a console, NO_COLOR not set")
        assertEquals(false, useColour(hasConsole = false, noColor = null), "no console: a file or a pipe")
        assertEquals(false, useColour(hasConsole = true, noColor = "1"), "a console, NO_COLOR set")
        assertEquals(false, useColour(hasConsole = false, noColor = "1"), "no console, NO_COLOR set")
    }

    @Test
    fun `CS20 an empty NO_COLOR counts as not set`() {
        assertEquals(true, useColour(hasConsole = true, noColor = ""), "a console, NO_COLOR empty")
        assertEquals(false, useColour(hasConsole = false, noColor = ""), "no console, NO_COLOR empty")
    }

    @Test
    fun `CS20 humans are named Player n and bots Bot n, n being the seat plus one, humans first`() {
        // the humans take the seats in the order they connect, before the bots
        assertEquals(listOf("Player 1", "Bot 2", "Bot 3"), playerNamesFor(ServerStart(humans = 1, bots = 2)))
        assertEquals(listOf("Player 1", "Player 2", "Player 3", "Bot 4"), playerNamesFor(ServerStart(humans = 3, bots = 1)))
        assertEquals(listOf("Player 1", "Player 2"), playerNamesFor(ServerStart(humans = 2, bots = 0)))
        assertEquals(listOf("Bot 1", "Bot 2", "Bot 3", "Bot 4"), playerNamesFor(ServerStart(humans = 0, bots = 4)))
    }

    @Test
    fun `CS20 the alternate argument does not change the names`() {
        assertEquals(listOf("Player 1", "Bot 2"), playerNamesFor(ServerStart(humans = 1, bots = 1, alternate = true)))
    }
}
