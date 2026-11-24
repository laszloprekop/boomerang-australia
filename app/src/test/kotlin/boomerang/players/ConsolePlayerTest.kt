package boomerang.players

import boomerang.game.CardRequest
import boomerang.game.CardsShown
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.GameAborted
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.GameStarted
import boomerang.game.PlayerLost
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine
import boomerang.support.BUSHWALKING
import boomerang.support.SWIMMING
import boomerang.support.cards
import boomerang.support.draft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A human at a console, with the keyboard and the screen replaced by two lambdas: no System.in, no
 * System.out. S7: the wording and the layout are free, so these tests only look for the information (a
 * name, a number) somewhere in the text.
 */
class ConsolePlayerTest {
    private val printed = StringBuilder()

    /** A console player who types these lines, and after them nothing more (end of input). */
    private fun typing(vararg lines: String): ConsolePlayer {
        val keyboard = ArrayDeque(lines.toList())
        return ConsolePlayer(readLine = { keyboard.removeFirstOrNull() }, print = { printed.append(it).append('\n') })
    }

    private fun assertPrinted(vararg expected: Any, text: String = printed.toString()) {
        for (part in expected) assertTrue(part.toString() in text, "'$part' is missing in: $text")
    }

    private val hand = cards("EKW")
    private val cardRequest = CardRequest(hand, draft("AB"))
    private val choiceRequest = ChoiceRequest(
        "Activities", listOf(Score(7, chosen = SWIMMING), Score(2, chosen = BUSHWALKING), Score(0)), draft("HLNSDKA"),
    )

    // --- questions --------------------------------------------------------------------------------------

    @Test
    fun `R7 S7 the question for a card is put at once and shows the hand and the player's own draft`() {
        typing("K").selectCard(cardRequest)

        // before the answer is awaited: every card of the hand with site, name, region, number and symbols
        assertPrinted('E', "Uluru", "Northern Territory", 4, "Emus", "Indigenous Culture")
        assertPrinted('K', "Daintree Rainforest", "Queensland", 6, "Souvenirs", "Bushwalking")
        assertPrinted('W', "Twelve Apostles", "Victoria", 2, "Shells", "Swimming")
        // AC5.4: the own draft, Throw card included
        assertPrinted("The Bungle Bungles", "The Pinnacles")
    }

    @Test
    fun `R7 the answer to a card question is the site character typed`() {
        assertEquals('K', typing("K").selectCard(cardRequest).await())
    }

    @Test
    fun `S1 a site character typed in lower case is passed on as that card's site`() {
        assertEquals('K', typing("k").selectCard(cardRequest).await().uppercaseChar())
    }

    @Test
    fun `S1 a line that is not one character is asked again at the console until one is`() {
        assertEquals('W', typing("", "EK", "W", "E").selectCard(cardRequest).await())
    }

    @Test
    fun `S1 a card question that follows a refusal names the refused answer`() {
        val text = describe(cardRequest.copy(refused = 'Q'))

        assertPrinted('Q', text = text.replace("Queensland", "")) // not the Q of the region on card K
        assertPrinted('E', 'K', 'W', text = text)                 // what is allowed: the cards of the hand
    }

    @Test
    fun `R10e AC10e_7 S7 the question for a score lists every possible score with its points`() {
        typing("1").chooseScore(choiceRequest)

        assertPrinted("Activities", "Swimming", 7, "Bushwalking", 2)
        // all seven cards of the draft the choice is about
        assertPrinted("King's Canyon", "Surfers Paradise", "Lake Eyre", "Bondi Beach", "Kalbarri National Park", "Daintree Rainforest", "The Bungle Bungles")
    }

    @Test
    fun `R10e the answer to a score question is an index into the scores offered`() {
        // the options are counted from 1: the first one is typed as "1"
        assertEquals(listOf(0, 1, 2), listOf("1", "2", "3").map { typing(it).chooseScore(choiceRequest).await() })
    }

    @Test
    fun `S1 the line 0 is not an option number and the question is repeated at the console`() {
        assertEquals(1, typing("0", "2", "3").chooseScore(choiceRequest).await())
    }

    @Test
    fun `S1 a line that is not a number is asked again at the console until one is`() {
        assertEquals(1, typing("", "swimming", "2", "1").chooseScore(choiceRequest).await())
    }

    @Test
    fun `S1 a score question that follows a refusal says so`() {
        assertTrue(describe(choiceRequest.copy(refused = 9)) != describe(choiceRequest), "the refusal changes nothing in the text")
    }

    // --- S2 -------------------------------------------------------------------------------------------

    @Test
    fun `S2 when the input ends a console player is lost, in a card question and in a score question`() {
        val player = typing()

        assertSame(player, assertThrows(PlayerLost::class.java) { player.selectCard(cardRequest).await() }.player)
        assertSame(player, assertThrows(PlayerLost::class.java) { player.chooseScore(choiceRequest).await() }.player)
        // also when it ends after lines that were no answer
        assertThrows(PlayerLost::class.java) { typing("", "EK").selectCard(cardRequest).await() }
    }

    // --- events: S7, the information each event gives --------------------------------------------------

    @Test
    fun `S7 every event is printed when it is shown`() {
        val player = typing()

        player.show(RoundStarted(3, setOf('A', 'M')))

        assertPrinted(describe(RoundStarted(3, setOf('A', 'M'))).trim())
        assertTrue(printed.isNotBlank())
    }

    @Test
    fun `S7 the start of the game tells the player the own player number, the number of players and the edition`() {
        // seat 2 is shown as player 3
        assertPrinted(3, 4, "Boomerang Australia", text = describe(GameStarted(seat = 2, playerCount = 4, edition = "Boomerang Australia")))
    }

    @Test
    fun `S7 the start of a round tells the round and the sites the player has visited`() {
        assertPrinted(2, 'K', 'W', '*', text = describe(RoundStarted(2, setOf('K', 'W', '*'))))
    }

    @Test
    fun `R7 AC7_3 S7 the shown cards of every player are printed`() {
        val text = describe(CardsShown(listOf(cards("EK"), cards("W"))))

        assertPrinted("Uluru", "Daintree Rainforest", "Twelve Apostles", "Emus", "Souvenirs", "Shells", text = text)
    }

    @Test
    fun `R5 AC5_6 R9 AC9_4 S7 the complete drafts of every player are printed, Throw and Catch cards included`() {
        val text = describe(DraftsRevealed(listOf(draft("EFGHAIM"), draft("QRUVYZB"))))

        assertPrinted("Uluru", "Barossa Valley", "Blue Mountains", "The Pinnacles", "Kakadu National Park", "Mount Wellington", text = text)
    }

    @Test
    fun `R11 AC11_1 S7 the score of a round is printed per category, with what is announced of every player`() {
        val own = listOf(ScoreLine("Throw and catch", Score(6)), ScoreLine("Collections", Score(14)), ScoreLine("Region bonus", Score(0)))
        val announced = listOf(emptyList(), listOf(ScoreLine("Region bonus", Score(3, announcement = "Tasmania"))))

        val text = describe(RoundScored(2, own, announced))

        assertPrinted("Throw and catch", 6, "Collections", 14, "Region bonus", "Tasmania", text = text)
    }

    @Test
    fun `R12 AC12_7 S7 the end of the game prints the final scores of all players and the winners`() {
        fun lines(throwCatch: Int, sites: Int) = listOf(ScoreLine("Throw and catch", Score(throwCatch)), ScoreLine("Tourist sites", Score(sites)))
        val one = GameResult(totals = listOf(31, 58, 47), lines = listOf(lines(11, 20), lines(33, 25), lines(26, 21)), winners = listOf(1))

        assertPrinted(31, 58, 47, "Throw and catch", 11, 33, 26, "Tourist sites", 20, 25, 21, text = describe(GameOver(one)))
        assertTrue(describe(GameOver(one)) != describe(GameOver(one.copy(winners = listOf(0, 1)))),
            "one winner and a shared win read the same")
    }

    @Test
    fun `S2 a game that ends because a player was lost says which player`() {
        // seat 4 does not exist in a game of four: the 5 can only be the lost player, shown as player 5
        assertPrinted(5, text = describe(GameAborted(lostSeat = 4)))
    }
}
