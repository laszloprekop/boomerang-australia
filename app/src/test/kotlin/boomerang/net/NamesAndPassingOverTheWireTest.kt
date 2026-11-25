package boomerang.net

import boomerang.australia.australia
import boomerang.game.AlternatingPassingRule
import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.game.Game
import boomerang.game.GameEvent
import boomerang.game.GameStarted
import boomerang.game.Passing
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine
import boomerang.support.FixedShuffler
import boomerang.support.ScriptedPlayer
import boomerang.support.SerializingWire
import boomerang.support.TABLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS18: the three new fields travel. Every message is written to bytes and read
 * back (SerializingWire), without a socket and without a thread, as in GameOverTheWireTest.
 */
class NamesAndPassingOverTheWireTest {
    private val names = listOf("Player 1", "Bot 2", "Bot 3")
    private val standard = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)
    private val mirrored = Passing(hands = TO_PREVIOUS, catch = TO_NEXT)

    @Test
    fun `CS18 GameStarted with names, RoundStarted with passing and a score with its explanation survive serialization`() {
        val client = ScriptedPlayer()
        val wire = SerializingWire(client)
        val events = listOf<GameEvent>(
            GameStarted(seat = 0, playerCount = 3, edition = "Boomerang Australia", names = names),
            RoundStarted(2, setOf('A', 'M'), mirrored),
            RoundScored(2, listOf(ScoreLine("Throw and catch", Score(2, explanation = "Throw D is 1, Catch P is 3"))), List(3) { emptyList() }),
        )

        for (event in events) wire.server.send(Tell(event))
        wire.server.send(Bye)
        wire.server.close()

        assertEquals(events, client.events, "what reached the client")
        assertEquals(names, (client.events[0] as GameStarted).names)
        assertEquals(mirrored, (client.events[1] as RoundStarted).passing)
        assertEquals("Throw D is 1, Catch P is 3", (client.events[2] as RoundScored).own.single().score.explanation)
        // control: the values did travel as bytes
        assertNotSame(events[0], client.events[0])
        assertTrue(wire.sessionEnded)
    }

    @Test
    fun `CS18 in a whole game over the wire every client is told the names and the passing of every round`() {
        val clients = List(3) { ScriptedPlayer(it) }
        val remotes = clients.map { RemotePlayer(SerializingWire(it).server) }

        Game(australia(), remotes, FixedShuffler(TABLE, TABLE.reversed()), AlternatingPassingRule, names).play()
        remotes.forEach { it.close() }

        for (client in clients) {
            assertEquals(names, client.eventsOf<GameStarted>().single().names, "the names told to the client of seat ${client.seat}")
            assertEquals(listOf(standard, mirrored, standard, mirrored), client.eventsOf<RoundStarted>().map { it.passing },
                "the passing told to the client of seat ${client.seat}, round by round")
        }
    }
}
