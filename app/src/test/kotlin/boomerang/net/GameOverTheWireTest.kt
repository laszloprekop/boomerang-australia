package boomerang.net

import boomerang.australia.australia
import boomerang.game.DraftsRevealed
import boomerang.game.Game
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.GameStarted
import boomerang.game.IPlayer
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.support.FixedShuffler
import boomerang.support.ScriptedPlayer
import boomerang.support.SerializingWire
import boomerang.support.TABLE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * S3, requirements 6.3: the game is played over a network, so everything the game asks and tells has to
 * survive being written to bytes and read back. Added after the tests were locked, with approval.
 *
 * The bug it guards against: DraftPhase dealt each hand as `deck.subList(...)`, a view that Java cannot
 * serialize, so the first card question to a remote player failed on a real socket. No test saw it,
 * because InMemoryConnection hands over the very object that was sent. Here every message goes through
 * ObjectOutputStream and ObjectInputStream (SerializingWire), without a socket and without a thread.
 */
class GameOverTheWireTest {

    /** Scripts for three players. Seat 1 takes the first activity offered in every round, so scores with a
     *  chosen symbol travel too; the others take none. All select the first card of the hand. */
    private fun scripted(): List<ScriptedPlayer> =
        List(3) { seat -> ScriptedPlayer(seat, choices = if (seat == 1) List(4) { 0 } else emptyList()) }

    private fun play(players: List<IPlayer>): GameResult = Game(australia(), players, FixedShuffler(TABLE, TABLE.reversed())).play()

    @Test
    fun `S3 a whole game of four rounds is played with every question, answer, event, score and result written to bytes and read back`() {
        // the same game with the players at the table: what the network must not change
        val local = scripted()
        val expected = play(local)

        val clients = scripted()
        val wires = clients.map { SerializingWire(it) }
        val remotes = wires.map { RemotePlayer(it.server) }

        val result = play(remotes)
        remotes.forEach { it.close() }

        assertEquals(expected, result, "the result of the game")
        for ((seat, client) in clients.withIndex()) {
            assertEquals(local[seat].cardRequests, client.cardRequests, "the 24 card questions that reached the client of seat $seat")
            assertEquals(local[seat].choiceRequests, client.choiceRequests, "the score questions that reached the client of seat $seat")
            assertEquals(local[seat].events, client.events, "the events that reached the client of seat $seat")
            assertEquals(24, client.cardRequests.size)
            assertEquals(listOf(1, 1, 4, 4, 4, 1), listOf(
                client.eventsOf<GameStarted>().size, client.eventsOf<GameOver>().size, client.eventsOf<RoundStarted>().size,
                client.eventsOf<DraftsRevealed>().size, client.eventsOf<RoundScored>().size, if (client.events.last() == GameOver(result)) 1 else 0,
            ), "what the client of seat $seat was told")
            assertTrue(wires[seat].sessionEnded, "the session of seat $seat ended with Bye")
        }
        assertEquals(4, clients[1].choiceRequests.size, "seat 1 was asked for an activity in every round")
        // control: the values did travel as bytes. A copy arrived, not the object the game holds.
        assertNotSame(result, (clients[0].events.last() as GameOver).result)
        assertTrue(wires.all { it.messages > 24 * 2 }, "messages written per player: ${wires.map { it.messages }}")
    }
}
