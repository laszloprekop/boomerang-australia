package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.PlayerLost
import boomerang.game.Score
import boomerang.support.BUSHWALKING
import boomerang.support.InMemoryConnection
import boomerang.support.cards
import boomerang.support.draft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The server's stand-in for a player at the other end of a connection, over a connection in memory. */
class RemotePlayerTest {
    private val ends = InMemoryConnection.pair()
    private val server = ends.first
    private val client = ends.second
    private val remote = RemotePlayer(server)

    private val cardRequest = CardRequest(cards("EKW"), draft("A"))
    private val choiceRequest = ChoiceRequest("Activities", listOf(Score(4, chosen = BUSHWALKING), Score(0)), draft("DKMCGXY"))

    @Test
    fun `S3 R7 a card question is sent at once and the answer is read from the connection when it is awaited`() {
        val answer = remote.selectCard(cardRequest)
        assertEquals(AskCard(cardRequest), client.receive(), "the question is out before the answer is awaited")

        client.send(CardAnswer('K'))

        assertEquals('K', answer.await())
    }

    @Test
    fun `S3 R10e a score question is sent at once and the answer is read from the connection when it is awaited`() {
        val answer = remote.chooseScore(choiceRequest)
        assertEquals(AskChoice(choiceRequest), client.receive())

        client.send(ChoiceAnswer(1))

        assertEquals(1, answer.await())
    }

    @Test
    fun `S3 R7 AC7_4 two remote players are both asked before either answer is read`() {
        val (otherServer, otherClient) = InMemoryConnection.pair()
        val other = RemotePlayer(otherServer)

        val first = remote.selectCard(cardRequest)
        val second = other.selectCard(cardRequest.copy(hand = cards("FLX")))
        assertEquals(AskCard(cardRequest), client.receive())
        assertEquals(AskCard(cardRequest.copy(hand = cards("FLX"))), otherClient.receive())
        // the second player answers first
        otherClient.send(CardAnswer('X'))
        client.send(CardAnswer('E'))

        assertEquals('E' to 'X', first.await() to second.await())
    }

    @Test
    fun `S3 an event is sent to the client as it is`() {
        val event = DraftsRevealed(listOf(draft("EFGHAIM"), draft("QRUVYZB")))

        remote.show(event)

        assertEquals(Tell(event), client.receive())
    }

    @Test
    fun `S3 closing a remote player says Bye and closes the connection`() {
        remote.close()

        assertEquals(listOf<Message>(Bye), server.sent)
        assertTrue(server.closed)
    }

    @Test
    fun `S2 a reply of the wrong kind makes the player lost`() {
        val card = remote.selectCard(cardRequest)
        client.send(ChoiceAnswer(0))
        assertSame(remote, assertThrows(PlayerLost::class.java) { card.await() }.player)

        val choice = remote.chooseScore(choiceRequest)
        client.send(CardAnswer('K'))
        assertSame(remote, assertThrows(PlayerLost::class.java) { choice.await() }.player)
    }

    @Test
    fun `S2 a connection lost while an answer is awaited makes the player lost`() {
        val answer = remote.selectCard(cardRequest)
        client.close()

        val lost = assertThrows(PlayerLost::class.java) { answer.await() }
        assertSame(remote, lost.player)
        assertTrue(lost.cause is ConnectionLost, "the cause is passed on: ${lost.cause}")
    }

    @Test
    fun `S2 a question that cannot be sent makes the player lost when the answer is collected, not before`() {
        client.close()

        // putting the question returns normally: the game has other players to ask first
        val card = remote.selectCard(cardRequest)
        val choice = remote.chooseScore(choiceRequest)

        val lostAtCard = assertThrows(PlayerLost::class.java) { card.await() }
        assertSame(remote, lostAtCard.player)
        assertTrue(lostAtCard.cause is ConnectionLost, "the cause is passed on: ${lostAtCard.cause}")
        assertSame(remote, assertThrows(PlayerLost::class.java) { choice.await() }.player)
    }

    @Test
    fun `S2 an event that cannot be sent makes the player lost at once`() {
        client.close()

        val lost = assertThrows(PlayerLost::class.java) { remote.show(DraftsRevealed(listOf(Draft()))) }

        assertSame(remote, lost.player)
        assertTrue(lost.cause is ConnectionLost, "the cause is passed on: ${lost.cause}")
    }
}
