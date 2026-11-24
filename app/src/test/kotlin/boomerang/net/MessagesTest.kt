package boomerang.net

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.GameEvent
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.support.BUSHWALKING
import boomerang.support.ScriptedPlayer
import boomerang.support.cards
import boomerang.support.draft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** S3, requirements 6.3: what travels is typed, and a server message carries out itself on the local player. */
class MessagesTest {
    private val player = ScriptedPlayer(cards = "K", choices = listOf(1))
    private val cardRequest = CardRequest(cards("EKW"), Draft())
    private val choiceRequest = ChoiceRequest("Activities", listOf(Score(4, chosen = BUSHWALKING), Score(0)), draft("DKMCGXY"))

    @Test
    fun `S3 Tell shows its event to the local player and has no reply`() {
        val reply = Tell(RoundStarted(1, emptySet())).deliverTo(player)

        assertNull(reply)
        assertEquals(listOf<GameEvent>(RoundStarted(1, emptySet())), player.events)
    }

    @Test
    fun `S3 AskCard puts its question to the local player and replies with the card answered`() {
        val reply = AskCard(cardRequest).deliverTo(player)

        assertEquals(CardAnswer('K'), reply)
        assertEquals(listOf(cardRequest), player.cardRequests)
    }

    @Test
    fun `S3 AskChoice puts its question to the local player and replies with the index answered`() {
        val reply = AskChoice(choiceRequest).deliverTo(player)

        assertEquals(ChoiceAnswer(1), reply)
        assertEquals(listOf(choiceRequest), player.choiceRequests)
    }

    @Test
    fun `S3 Bye is the only message that closes the session, and it asks and tells the player nothing`() {
        assertTrue(Bye.closes)
        assertNull(Bye.deliverTo(player))
        assertTrue(player.events.isEmpty() && player.cardRequests.isEmpty() && player.choiceRequests.isEmpty())
        for (message in listOf(Tell(RoundStarted(1, emptySet())), AskCard(cardRequest), AskChoice(choiceRequest))) {
            assertFalse(message.closes, "$message")
        }
    }
}
