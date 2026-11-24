package boomerang.players

import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.Game
import boomerang.game.GameStarted
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.support.BUSHWALKING
import boomerang.support.FixedShuffler
import boomerang.support.SWIMMING
import boomerang.support.TABLE
import boomerang.support.cards
import boomerang.support.draft
import boomerang.support.editionOf
import boomerang.support.points
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/** S4: a bot. Its random source is seeded, so every run of these tests sees the same selections. */
class BotPlayerTest {

    @Test
    fun `S4 a bot selects a card of its hand, whatever the size of the hand`() {
        for (seed in 0 until 50) for (hand in listOf("ABCDEFG", "HIJ", "YZ", "K")) {
            val answer = BotPlayer(Random(seed)).selectCard(CardRequest(cards(hand), Draft())).await()

            assertTrue(answer in hand, "seed $seed: '$answer' is not in the hand $hand")
        }
    }

    @Test
    fun `S4 a bot can select any card of its hand, the last one too`() {
        // the original never selected the last card of the hand (line 88)
        val selected = (0 until 300).map { seed -> BotPlayer(Random(seed)).selectCard(CardRequest(cards("ABCDEFG"), Draft())).await() }

        assertEquals("ABCDEFG".toSet(), selected.toSet())
    }

    @Test
    fun `S4 a bot takes the first score it is offered`() {
        val offered = listOf(Score(0, chosen = SWIMMING), Score(15, chosen = BUSHWALKING), Score(0))

        for (seed in 0 until 20) {
            assertEquals(0, BotPlayer(Random(seed)).chooseScore(ChoiceRequest("Activities", offered, draft("DKMOTUH"))).await(), "seed $seed")
        }
    }

    @Test
    fun `S4 a bot can be told anything and answers afterwards as before`() {
        val bot = BotPlayer(Random(1))

        bot.show(GameStarted(seat = 1, playerCount = 2, edition = "Boomerang Australia"))
        bot.show(RoundStarted(1, emptySet()))

        assertEquals('K', bot.selectCard(CardRequest(cards("K"), Draft())).await())
    }

    @Test
    fun `S3 S4 a game of bots only is played to its end (Q10)`() {
        val edition = editionOf(points("Only", 1, 2, 3, 4))
        val bots = List(4) { BotPlayer(Random(it)) }

        val result = Game(edition, bots, FixedShuffler(TABLE)).play()

        assertEquals(listOf(4, 8, 12, 16), result.totals)
        assertEquals(listOf(3), result.winners)
    }
}
