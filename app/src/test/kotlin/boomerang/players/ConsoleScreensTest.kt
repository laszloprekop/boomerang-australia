package boomerang.players

import boomerang.australia.australia
import boomerang.game.CardsShown
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.GameAborted
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.GameStarted
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine
import boomerang.support.BUSHWALKING
import boomerang.support.ESC
import boomerang.support.INDIGENOUS_CULTURE
import boomerang.support.SWIMMING
import boomerang.support.TABLE
import boomerang.support.TestConsole
import boomerang.support.cards
import boomerang.support.draft
import boomerang.support.numbers
import boomerang.support.ownerOf
import boomerang.support.screenLines
import boomerang.support.withoutEscapes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS1 and CS8 to CS15: the width of every screen, the screens of the end of a
 * round and of the game, names and colour. As in ConsoleCardQuestionTest: facts and structure, no sentence.
 * A console is always shown GameStarted first, because a console that has been shown nothing else prints
 * an event as `describe` does (D9).
 */
class ConsoleScreensTest {
    private val edition = australia()
    private val names = listOf("Alice", "Bruno", "Chloe")
    private val titles = listOf("Throw and catch", "Tourist sites", "Region bonus", "Collections", "Animals", "Activities")

    /** A console of a game of three named players that has seen the start of the game and of round 1. */
    private fun console(seat: Int = 0, colour: Boolean = false): TestConsole =
        TestConsole(edition.cards, colour).told(GameStarted(seat, 3, edition.name, names), RoundStarted(1, emptySet()))

    /** The one line that fits; a failure that says what was looked for if there is none or more than one. */
    private fun theLine(screen: String, what: String, fits: (String) -> Boolean): String {
        val found = screen.screenLines().filter(fits)
        assertEquals(1, found.size, "$what is to be on one line of:\n$screen")
        return found.single()
    }

    private fun at(screen: String, line: String): Int = screen.screenLines().indexOf(line)

    // --- CS1 ------------------------------------------------------------------------------------------

    private val four = listOf("Player 1", "Player 2", "Bot 3", "Bot 4")

    /** The score lines of a round as the six scoring classes may explain them, the long ones included. */
    private val explained = listOf(
        ScoreLine("Throw and catch", Score(6, explanation = "Throw A is 1, Catch Z is 7")),
        ScoreLine("Tourist sites", Score(7, explanation = "7 new sites: A B C D E F G")),
        ScoreLine("Region bonus", Score(3, announcement = "Western Australia", explanation = "Western Australia")),
        ScoreLine("Collections", Score(12, explanation = "Leaves 1 + 1, Wildflowers 2, Shells 3, Souvenirs 5 = 12")),
        ScoreLine("Animals", Score(12, explanation = "Kangaroos 1 pair 3, Emus 1 pair 4, Wombats 1 pair 5, Koalas no pair")),
        ScoreLine("Activities", Score(4, chosen = INDIGENOUS_CULTURE, explanation = "Indigenous Culture, 3 cards")),
    )

    private fun scoredOfFour(round: Int) =
        RoundScored(round, explained, listOf(listOf(explained[2]), emptyList(), listOf(explained[2]), emptyList()))

    private fun consoleOfFour(): TestConsole =
        TestConsole(edition.cards).told(GameStarted(0, 4, edition.name, four), RoundStarted(1, emptySet()))

    private fun assertWidth(what: String, screen: String) {
        assertTrue(screen.isNotBlank(), "$what is empty")
        assertEquals(emptyList<String>(), screen.lines().filter { it.length > 80 }, "lines of more than 80 characters in $what")
    }

    @Test
    fun `CS1 no line of a card question is longer than 80 characters, for every card of the edition in hand and draft and for four players`() {
        // round 2 of four players: a map with visited sites, a score sheet, and five shown cards of each of the others
        val console = consoleOfFour().told(
            scoredOfFour(1), RoundStarted(2, "ABCEUVW".toSet()),
            CardsShown(listOf(cards("BCDEF"), cards("IJKLM"), cards("PQRST"), cards("WXYZ*"))),
        )
        val sevens = listOf("ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ*-")

        // every card is in a hand once and in a draft at least once, as the first card and as a later one
        for (index in sevens.indices) {
            val other = sevens[(index + 1) % 4]
            assertWidth("the question with hand ${sevens[index]}", console.cardQuestion(sevens[index], other.take(6)))
            assertWidth("the question with draft ${other.drop(1)}", console.cardQuestion(sevens[index], other.drop(1)))
        }
    }

    @Test
    fun `CS1 no line of the screens at the end of a round is longer than 80 characters, with all 28 cards revealed and long explanations`() {
        val console = consoleOfFour()
        val choice = ChoiceRequest("Activities", listOf(
            Score(4, chosen = INDIGENOUS_CULTURE, explanation = "Indigenous Culture, 3 cards"),
            Score(2, chosen = SWIMMING, explanation = "Swimming, 2 cards"),
            Score(0, chosen = BUSHWALKING, explanation = "Bushwalking, 1 card"),
            Score(0, explanation = "no activity is used up"),
        ), draft("AEJQHLD"))

        assertWidth("the drafts", console.screenOf(DraftsRevealed(TABLE.chunked(7).map { Draft(it) })))
        assertWidth("the choice question", console.choiceQuestion(choice))
        assertWidth("the refused choice question", console.choiceQuestion(choice.copy(refused = 8)))
        assertWidth("the round score", console.screenOf(scoredOfFour(1)))
    }

    @Test
    fun `CS1 no line of the final screen is longer than 80 characters, for four players and four rounds`() {
        val console = consoleOfFour().told(
            scoredOfFour(1), RoundStarted(2, emptySet()), scoredOfFour(2), RoundStarted(3, emptySet()), scoredOfFour(3),
            RoundStarted(4, emptySet()), scoredOfFour(4),
        )
        val totals = explained.map { ScoreLine(it.title, Score(4 * it.score.points)) }

        assertWidth("the final screen", console.screenOf(GameOver(GameResult(listOf(176, 176, 176, 176), List(4) { totals }, listOf(0, 1, 2, 3)))))
    }

    // --- CS8 ------------------------------------------------------------------------------------------

    private val pointsByRound = listOf(listOf(3, 7, 0, 9, 8, 2), listOf(1, 5, 6, 14, 11, 4), listOf(0, 4, 3, 10, 12, 7), listOf(5, 6, 0, 16, 3, 10))

    private fun scored(round: Int) = RoundScored(
        round, titles.indices.map { ScoreLine(titles[it], Score(pointsByRound[round - 1][it])) }, List(3) { emptyList() },
    )

    @Test
    fun `CS8 the final screen shows the score sheet - a row per category with every round and the row total, the round totals and the grand total`() {
        val console = console().told(scored(1), RoundStarted(2, emptySet()), scored(2), RoundStarted(3, emptySet()), scored(3), RoundStarted(4, emptySet()), scored(4))
        val byCategory = titles.indices.map { category -> pointsByRound.map { it[category] } }
        val own = byCategory.map { it.sum() }
        assertEquals(146, own.sum(), "control: the grand total")
        val result = GameResult(
            totals = listOf(146, 120, 133), winners = listOf(0),
            lines = listOf(own, listOf(20, 20, 20, 20, 20, 20), listOf(23, 22, 22, 22, 22, 22)).map { points -> titles.indices.map { ScoreLine(titles[it], Score(points[it])) } },
        )

        val screen = console.screenOf(GameOver(result))

        // the only numbers of a row: the points of rounds 1 to 4, then their sum
        for (index in titles.indices) {
            val row = theLine(screen, "the row of ${titles[index]}") { titles[index] in it && it.numbers().isNotEmpty() }
            assertEquals(byCategory[index] + own[index], row.numbers(), titles[index])
        }
        val roundTotals = pointsByRound.map { it.sum() }
        assertTrue(screen.screenLines().any { it.numbers() == roundTotals + 146 }, "no row holds the round totals $roundTotals and the grand total 146 in:\n$screen")
    }

    // --- CS9 and CS10 ---------------------------------------------------------------------------------

    /** Swimming is on H, L, N and S, Bushwalking on D, K and M. */
    private val choice = ChoiceRequest("Activities", listOf(
        Score(7, chosen = SWIMMING, explanation = "Swimming, 4 cards"),
        Score(4, chosen = BUSHWALKING, explanation = "Bushwalking, 3 cards"),
        Score(0, explanation = "no activity is used up"),
    ), draft("HLNSDKM"))

    @Test
    fun `CS9 a choice question after a refusal says which number was not offered`() {
        val console = console()

        // the game counts from 0 and the person from 1: index 8 was typed as 9. No card and no option holds a 9.
        val refused = console.choiceQuestion(choice.copy(refused = 8))

        assertTrue(9 in refused.numbers(), "the refused number 9 is missing in:\n$refused")
        assertTrue(9 !in console.choiceQuestion(choice).numbers(), "control: a 9 without a refusal")
    }

    @Test
    fun `CS10 a choice question lists the options numbered from 1, each with what it uses up, its points and its explanation`() {
        val lines = console().choiceQuestion(choice).screenLines()

        // an option starts on the line that begins with its number, and ends where the next one starts
        val startOf = (1..3).map { number ->
            val starts = lines.indices.filter { Regex("^\\s*$number\\D").containsMatchIn(lines[it]) }
            assertEquals(1, starts.size, "option $number is to start one line of:\n${lines.joinToString("\n")}")
            starts.single()
        }
        assertEquals(startOf.sorted(), startOf, "the options are not in the order offered")
        for (index in 0..2) {
            val score = choice.scores[index]
            val option = lines.subList(startOf[index], startOf.getOrElse(index + 1) { lines.size }).joinToString("\n")
            assertTrue((score.chosen?.name ?: "") in option, "what option ${index + 1} uses up is missing in: $option")
            assertTrue(score.explanation!! in option, "the explanation '${score.explanation}' is missing in: $option")
            // after the number of the option and the number of cards in the explanation: the points
            assertTrue(score.points in option.numbers().drop(1), "the points ${score.points} are missing in: $option")
        }
    }

    @Test
    fun `CS10 an option without an explanation shows how many cards of the draft carry its symbol`() {
        // as the game sends them: Swimming is on H, L, N and S (4 cards), Bushwalking on D, K and M (3 cards)
        val plain = ChoiceRequest("Activities", listOf(Score(15, chosen = SWIMMING), Score(10, chosen = BUSHWALKING), Score(0)), draft("HLNSDKM"))

        val lines = console().choiceQuestion(plain).screenLines()

        val swimming = lines.single { Regex("^\\s*1\\D").containsMatchIn(it) }
        val bushwalking = lines.single { Regex("^\\s*2\\D").containsMatchIn(it) }
        assertEquals(listOf(1, 4, 15), swimming.numbers(), "option, cards, points in: $swimming")
        assertEquals(listOf(2, 3, 10), bushwalking.numbers(), "option, cards, points in: $bushwalking")
    }

    // --- CS11 -----------------------------------------------------------------------------------------

    @Test
    fun `CS11 RoundScored prints a line per category with title, explanation and points, then the total of the round, then what is announced of the others by name`() {
        val own = listOf(
            ScoreLine("Throw and catch", Score(2, explanation = "Throw D is 1, Catch P is 3")),
            ScoreLine("Tourist sites", Score(7, explanation = "7 new sites: D H J M N O P")),
            ScoreLine("Region bonus", Score(0)),
            ScoreLine("Collections", Score(12, explanation = "Shells 3 + 3 = 6, doubled")),
            ScoreLine("Animals", Score(10, explanation = "Koalas 1 pair 7, Kangaroos 1 pair 3, Emus no pair")),
            ScoreLine("Activities", Score(4, chosen = SWIMMING, explanation = "Swimming, 3 cards")),
        )
        val announced = listOf(emptyList(), listOf(ScoreLine("Region bonus", Score(3, announcement = "Tasmania", explanation = "Tasmania"))), emptyList())

        val screen = console().screenOf(RoundScored(1, own, announced))

        // the own lines: the title without the name of a player
        val categoryAt = own.map { line ->
            val printed = theLine(screen, "the own line of ${line.title}") { text -> line.title in text && names.none { it in text } }
            val explanation = line.score.explanation.orEmpty()
            assertTrue(explanation in printed, "the explanation '$explanation' is missing in: $printed")
            assertEquals(line.score.points, printed.replace(explanation, "").numbers().lastOrNull(), "the points of ${line.title} in: $printed")
            at(screen, printed)
        }
        assertEquals(categoryAt.sorted(), categoryAt, "the categories are not in the order received")
        // 35 is the sum, and no category and no explanation holds a 35
        val total = theLine(screen, "the total of the round, 35,") { 35 in it.numbers() }
        assertTrue(at(screen, total) > categoryAt.last(), "the total comes before a category in:\n$screen")
        val others = theLine(screen, "the region Bruno completed") { "Bruno" in it && "Tasmania" in it }
        assertTrue(at(screen, others) > at(screen, total), "what is announced comes before the total in:\n$screen")
    }

    // --- CS12 -----------------------------------------------------------------------------------------

    @Test
    fun `CS12 DraftsRevealed prints every player by name with all seven cards, and marks the Throw card and the Catch card`() {
        val drafts = listOf("EFGHAIM", "QRUVYZB", "JKLNOPS")

        val screen = console().screenOf(DraftsRevealed(drafts.map(::draft)))

        val lines = screen.screenLines()
        for (seat in drafts.indices) for ((place, card) in cards(drafts[seat]).withIndex()) {
            val line = theLine(screen, card.name) { card.name in it }
            assertEquals(names[seat], ownerOf(lines, lines.indexOf(line), names), "the player ${card.name} is listed under")
            // "Throw" and "Catch" are words of the game: the first card carries the one, the seventh the other
            assertEquals(place == 0, "throw" in line.lowercase(), "marked as Throw card: $line")
            assertEquals(place == 6, "catch" in line.lowercase(), "marked as Catch card: $line")
        }
    }

    // --- CS13 -----------------------------------------------------------------------------------------

    private fun over(totals: List<Int>, winners: List<Int>) =
        GameOver(GameResult(totals, totals.map { listOf(ScoreLine("Throw and catch", Score(it))) }, winners))

    /** The line of the result for a seat: the one that holds the name and the total. */
    private fun resultLine(screen: String, seat: Int, total: Int): String =
        theLine(screen, "${names[seat]} with $total") { names[seat] in it && total in it.numbers() }

    /** What the line of the result says besides the name, the numbers and the spaces. */
    private fun marked(screen: String, seat: Int, total: Int): String =
        resultLine(screen, seat, total).replace(names[seat], "").filterNot { it.isDigit() || it.isWhitespace() }

    @Test
    fun `CS13 GameOver prints the players ranked by total, highest first, each with name and total, and marks the receiver as you`() {
        val screen = console(seat = 2).screenOf(over(totals = listOf(98, 121, 114), winners = listOf(1)))

        val ranked = listOf(resultLine(screen, 1, 121), resultLine(screen, 2, 114), resultLine(screen, 0, 98)).map { at(screen, it) }
        assertEquals(ranked.sorted(), ranked, "Bruno 121, Chloe 114, Alice 98 are not in this order in:\n$screen")
        val you = Regex("\\byou\\b", RegexOption.IGNORE_CASE)
        assertTrue(you.containsMatchIn(resultLine(screen, 2, 114)), "seat 2 is the receiver and is not marked as you in:\n$screen")
        assertTrue(!you.containsMatchIn(resultLine(screen, 0, 98)) && !you.containsMatchIn(resultLine(screen, 1, 121)), "another player is marked as you in:\n$screen")
    }

    @Test
    fun `CS13 every winner is marked in the result`() {
        val one = console(seat = 0).screenOf(over(totals = listOf(98, 121, 114), winners = listOf(1)))
        val shared = console(seat = 0).screenOf(over(totals = listOf(98, 121, 121), winners = listOf(1, 2)))

        // Bruno and Chloe are not the receiver: their lines differ only in what marks a winner
        val notAWinner = marked(one, seat = 2, total = 114)
        assertTrue(marked(one, seat = 1, total = 121) != notAWinner, "the winner is not marked in:\n$one")
        assertTrue(marked(shared, seat = 1, total = 121) != notAWinner && marked(shared, seat = 2, total = 121) != notAWinner,
            "a winner of the shared win is not marked in:\n$shared")
    }

    @Test
    fun `CS13 one winner and a shared win read differently`() {
        // the same totals: Bruno wins the tie-break, or the win is shared with Chloe
        val one = console(seat = 0).screenOf(over(totals = listOf(98, 121, 121), winners = listOf(1)))
        val shared = console(seat = 0).screenOf(over(totals = listOf(98, 121, 121), winners = listOf(1, 2)))
        resultLine(one, 1, 121) // Bruno is in the result by name, in both

        // more than Chloe's line differs: a winner alone is not told as one of two winners
        fun withoutChloe(screen: String) = screen.screenLines().filterNot { "Chloe" in it }
        assertTrue(withoutChloe(one) != withoutChloe(shared), "only the line of the second winner tells a shared win from a single one:\n$shared")
    }

    // --- CS14 -----------------------------------------------------------------------------------------

    @Test
    fun `CS14 a lost player is named with the name from GameStarted`() {
        val screen = console().screenOf(GameAborted(lostSeat = 1))

        assertTrue("Bruno" in screen && "Player 2" !in screen, "seat 1 is Bruno in: $screen")
    }

    @Test
    fun `CS14 a console that never saw GameStarted says Player n`() {
        val console = TestConsole().told(RoundStarted(1, emptySet()))

        val screen = console.screenOf(DraftsRevealed(listOf(draft("EFGHAIM"), draft("QRUVYZB"))))

        assertTrue("Player 1" in screen && "Player 2" in screen, "the players of seat 0 and 1 are not named in:\n$screen")
    }

    // --- CS15 -----------------------------------------------------------------------------------------

    /** The screens of a game of three up to its result, by what they are. The same for every console. */
    private fun screensOfAGame(colour: Boolean): Map<String, String> {
        val console = console(colour = colour)
        val screens = linkedMapOf<String, String>()
        screens["the first card question"] = console.cardQuestion("ABCDEFG")
        console.told(CardsShown(listOf(cards("M"), cards("S"), cards("T"))))
        screens["the third card question"] = console.cardQuestion("EJNOP", "DM")
        screens["the refused card question"] = console.cardQuestion("EJNOP", "DM", refused = 'Z')
        screens["the drafts"] = console.screenOf(DraftsRevealed(listOf(draft("EFGHAIM"), draft("QRUVYZB"), draft("JKLNOPS"))))
        screens["the choice question"] = console.choiceQuestion(choice)
        screens["the round score"] = console.screenOf(scored(1))
        console.told(RoundStarted(2, "EFGHAIM".toSet()))
        screens["a card question of round 2"] = console.cardQuestion("JKLNOP", "B")
        screens["the result"] = console.screenOf(over(totals = listOf(98, 121, 114), winners = listOf(1)))
        return screens
    }

    @Test
    fun `CS15 with colour on every screen holds escape sequences, and without them it is the text of colour off`() {
        val plain = screensOfAGame(colour = false)
        val coloured = screensOfAGame(colour = true)

        for ((what, screen) in coloured) {
            assertTrue(ESC in screen, "$what has no escape sequence with colour on")
            assertEquals(plain[what], screen.withoutEscapes(), "$what, with the escape sequences removed")
        }
    }

    @Test
    fun `CS15 with colour off no screen holds an escape sequence`() {
        for ((what, screen) in screensOfAGame(colour = false)) {
            assertTrue(screen.isNotBlank(), "$what is empty")
            assertTrue(ESC !in screen, "$what holds an escape sequence with colour off")
        }
    }
}
