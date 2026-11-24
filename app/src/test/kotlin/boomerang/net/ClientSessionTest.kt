package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.GameEvent
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.support.BUSHWALKING
import boomerang.support.InMemoryConnection
import boomerang.support.ScriptedPlayer
import boomerang.support.cards
import boomerang.support.draft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * The client's end of a game, over a connection in memory. There is no thread: the server's messages are
 * all sent before the session runs, and the replies are read after it has returned.
 */
class ClientSessionTest {
    private val ends = InMemoryConnection.pair()
    private val server = ends.first
    private val client = ends.second
    private val player = ScriptedPlayer(cards = "K", choices = listOf(0))

    private val cardRequest = CardRequest(cards("EKW"), Draft())
    private val choiceRequest = ChoiceRequest("Activities", listOf(Score(4, chosen = BUSHWALKING), Score(0)), draft("DKMCGXY"))
    private val over = GameOver(GameResult(listOf(10, 12), listOf(emptyList(), emptyList()), listOf(1)))

    @Test
    fun `S3 the session delivers every message to the local player, sends each reply and ends with Bye`() {
        for (message in listOf(Tell(RoundStarted(1, emptySet())), AskCard(cardRequest), AskChoice(choiceRequest), Tell(over), Bye)) {
            server.send(message)
        }

        ClientSession(client, player).run()

        assertEquals(listOf<GameEvent>(RoundStarted(1, emptySet()), over), player.events)
        assertEquals(listOf(cardRequest), player.cardRequests)
        assertEquals(listOf(choiceRequest), player.choiceRequests)
        assertEquals(listOf<Message>(CardAnswer('K'), ChoiceAnswer(0)), client.sent, "the replies, in the order of the questions")
    }

    @Test
    fun `S3 the end of the game is known by the Bye message, not by the words of an event`() {
        // the result is told, and the game goes on: only Bye ends the session (requirements 6.3)
        for (message in listOf(Tell(over), AskCard(cardRequest), Bye, AskCard(cardRequest))) server.send(message)

        ClientSession(client, player).run()

        assertEquals(listOf(cardRequest), player.cardRequests, "the question after the result is asked, the one after Bye is not")
        assertEquals(listOf<Message>(AskCard(cardRequest)), server.sent, "what is left unread after Bye")
    }

    @Test
    fun `S2 a connection lost before Bye is a failure of the session, not a silent end`() {
        server.send(Tell(RoundStarted(1, emptySet())))

        assertThrows(ConnectionLost::class.java) { ClientSession(client, player).run() }

        assertEquals(listOf<GameEvent>(RoundStarted(1, emptySet())), player.events, "what came before the loss was delivered")
    }
}
