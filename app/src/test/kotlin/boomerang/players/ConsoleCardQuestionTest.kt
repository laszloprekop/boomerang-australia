package boomerang.players

import boomerang.australia.australia
import boomerang.game.Card
import boomerang.game.CardsShown
import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.game.GameStarted
import boomerang.game.Passing
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine
import boomerang.support.TestConsole
import boomerang.support.besidesSites
import boomerang.support.card
import boomerang.support.cards
import boomerang.support.markOf
import boomerang.support.namesSite
import boomerang.support.numbers
import boomerang.support.ownerOf
import boomerang.support.screenLines
import boomerang.support.timesNamed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * docs/console-screens.md, CS2 to CS9: the screen of a card question. Colour is off. S7 still holds, so no
 * test compares a sentence: each looks for the facts the criterion names and for the structure it names
 * (a header first, one line per region, one line per card, one row per category). Names of players have no
 * digits here and are no part of a card, so a name or a number found can only be the one looked for.
 */
class ConsoleCardQuestionTest {
    private val edition = australia()
    private val names = listOf("Alice", "Bruno", "Chloe")
    private val standard = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)

    /** A console that has seen the start of a game of three and of a round. It knows the cards of the
     *  edition (D2) unless the test says otherwise. */
    private fun console(
        seat: Int = 0, round: Int = 1, visited: String = "", passing: Passing = standard, withCards: Boolean = true,
    ): TestConsole = TestConsole(if (withCards) edition.cards else emptyList())
        .told(GameStarted(seat, 3, edition.name, names), RoundStarted(round, visited.toSet(), passing))

    /** The one line of the screen that holds the name of the card: a table has one card per line (CS4). */
    private fun lineOf(screen: String, card: Card): String {
        val lines = screen.screenLines().filter { card.name in it }
        assertEquals(1, lines.size, "${card.name} is to be on one line of: \n$screen")
        return lines.single()
    }

    // --- CS2 ------------------------------------------------------------------------------------------

    @Test
    fun `CS2 a card question starts with a header - the edition, the round, the number of the pick and the player's own name`() {
        // no cards given, so no map: what follows the header has no numbers of its own that could be mistaken
        val console = console(seat = 1, round = 4, withCards = false)

        // the header is looked for in the first two lines: the pick is the cards in the draft plus one
        val second = console.cardQuestion(hand = "EKWFJN", draft = "A").screenLines().take(2).joinToString("\n")
        val fifth = console.cardQuestion(hand = "EKW", draft = "ABCD").screenLines().take(2).joinToString("\n")

        assertTrue(edition.name.lowercase() in second.lowercase(), "the edition is missing in: $second")
        assertTrue("Bruno" in second, "the name of seat 1 is missing in: $second")
        assertTrue(second.numbers().containsAll(listOf(4, 2)) && 5 !in second.numbers(), "round 4, pick 2 is not in: $second")
        assertTrue(fifth.numbers().containsAll(listOf(4, 5)) && 2 !in fifth.numbers(), "round 4, pick 5 is not in: $fifth")
    }

    // --- CS3 ------------------------------------------------------------------------------------------

    private val sitesOfRegion: Map<String, List<Char>> = edition.cards.groupBy({ it.region }, { it.site })

    /** The rows of the map for a region, without the region's name: the lines that hold the full name of the
     *  region and name each of its sites once. (A card line or a legend holds the name and not the sites.) */
    private fun mapRows(screen: String, region: String): List<String> =
        screen.screenLines().filter { region in it }.map { it.replace(region, "") }
            .filter { row -> sitesOfRegion.getValue(region).all { row.timesNamed(it) == 1 } }

    private fun mapRow(screen: String, region: String): String {
        val rows = mapRows(screen, region)
        assertEquals(1, rows.size, "$region is to have one row with its sites ${sitesOfRegion[region]} in: \n$screen")
        return rows.single()
    }

    /** Round 2. Visited in round 1: A, B, C, E and all of Victoria. In the draft of this round: D and M.
     *  So Western Australia is completed by the draft and Victoria was complete before. */
    private fun mapScreen(): String =
        console(round = 2, visited = "ABCEUVWX").cardQuestion(hand = "FJNOP", draft = "DM")

    @Test
    fun `CS3 the map shows every region once and every site of the edition once`() {
        val screen = mapScreen()

        assertEquals(7, sitesOfRegion.size, "control: the regions of the edition")
        // one row per region, and in it every site of the region exactly once: 7 rows, 28 sites
        for (region in sitesOfRegion.keys) mapRow(screen, region)
    }

    @Test
    fun `CS3 a site visited earlier, a site in the draft of this round and a site not visited are marked in three different ways`() {
        val screen = mapScreen()
        fun marks(sites: String): Set<String> = sites.map { site -> mapRow(screen, card(site).region).markOf(site) }.toSet()

        val earlier = marks("ABCEUVWX")
        val drafted = marks("DM")
        val notVisited = marks("FGHIJKLNOPQRSTYZ*-")

        assertEquals(1, earlier.size, "sites visited earlier are marked alike: $earlier")
        assertEquals(1, drafted.size, "sites in the draft are marked alike: $drafted")
        assertTrue(earlier.single() != drafted.single(), "visited earlier and drafted now are marked alike: $earlier")
        assertTrue(earlier.single() !in notVisited && drafted.single() !in notVisited, "a site not visited is marked $notVisited, as a site the player has")
    }

    @Test
    fun `CS3 each region shows how many of its sites the player has, visited or in this draft, out of how many`() {
        val screen = mapScreen()

        // the only numbers of a row: the sites the player has, then the sites of the region
        val has = mapOf(
            "Western Australia" to 4, "Northern Territory" to 1, "Queensland" to 0, "South Australia" to 1,
            "New South Wales" to 0, "Victoria" to 4, "Tasmania" to 0,
        )
        for ((region, count) in has) assertEquals(listOf(count, 4), mapRow(screen, region).numbers(), region)
    }

    @Test
    fun `CS3 a region whose sites the player all has is marked as complete`() {
        val screen = mapScreen()
        /** What the row says besides the name, the sites with their marks and the two counts. */
        fun rest(region: String): String = mapRow(screen, region).besidesSites(sitesOfRegion.getValue(region))

        val incomplete = listOf("Northern Territory", "Queensland", "South Australia", "New South Wales", "Tasmania").map(::rest)

        for (complete in listOf("Western Australia", "Victoria")) {
            assertTrue(rest(complete) !in incomplete, "$complete is complete and its row says no more than a row that is not: '${rest(complete)}'")
        }
    }

    @Test
    fun `CS3 without cards given there is no map and nothing fails`() {
        val screen = console(round = 2, visited = "ABCEUVWX", withCards = false).cardQuestion(hand = "FJNOP", draft = "DM")

        for (region in sitesOfRegion.keys) assertEquals(emptyList<String>(), mapRows(screen, region), region)
        assertTrue("Kakadu National Park" in screen, "the question is still put: $screen")
    }

    // --- CS4 ------------------------------------------------------------------------------------------

    /** D8: the initials of a name of several words, else the first three letters in capitals. */
    private val regionCode = mapOf(
        "Western Australia" to "WA", "Northern Territory" to "NT", "Queensland" to "QUE", "South Australia" to "SA",
        "New South Wales" to "NSW", "Victoria" to "VIC", "Tasmania" to "TAS",
    )

    /** Four questions that put every card of the edition in a hand once, each with three cards in the draft
     *  that carry all three symbol kinds between them: the hand, the draft and the screen. */
    private fun everyCardOnce(): List<Triple<List<Card>, List<Card>, String>> {
        val hands = listOf("ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ*-")
        val drafts = listOf("HIJ", "OPQ", "VWX", "ABC")
        return hands.indices.map { Triple(cards(hands[it]), cards(drafts[it]), console().cardQuestion(hands[it], drafts[it])) }
    }

    @Test
    fun `CS4 the hand and the draft have one card per line, with site character, name, number and region code`() {
        for ((hand, draft, screen) in everyCardOnce()) for (card in hand + draft) {
            val line = lineOf(screen, card)

            assertTrue(line.namesSite(card.site), "the site character ${card.site} is missing in: $line")
            assertEquals(listOf(card.number), line.numbers(), "the number of the card is the only number in: $line")
            val code = regionCode.getValue(card.region)
            assertTrue(Regex("\\b$code\\b").containsMatchIn(line), "the region code $code is missing in: $line")
        }
    }

    @Test
    fun `CS4 the hand and the draft have one column per symbol kind, with a dash where a card has no symbol of that kind`() {
        val kinds = edition.cards.flatMap { it.symbols }.map { it.kind }.distinct()
        assertEquals(3, kinds.size, "control: the symbol kinds of the edition")

        for ((hand, draft, screen) in everyCardOnce()) for (table in listOf(hand, draft)) {
            val lines = table.associateWith { lineOf(screen, it) }
            // a column: the symbols of one kind start at the same place in every line of the table
            val start = kinds.associateWith { kind ->
                val starts = table.mapNotNull { card ->
                    val symbol = card.symbols.firstOrNull { it.kind == kind } ?: return@mapNotNull null
                    val line = lines.getValue(card)
                    line.indexOf(symbol.name, line.indexOf(card.name) + card.name.length)
                }.distinct()
                assertEquals(1, starts.size, "the $kind symbols of ${table.map { it.site }} do not start in one column: $starts in\n$screen")
                starts.single()
            }
            // a cell: from the start of its column to the start of the next one, or to the end of the line
            for (card in table) for (kind in kinds) {
                val line = lines.getValue(card)
                val end = start.values.filter { it > start.getValue(kind) }.minOrNull() ?: line.length
                val cell = line.padEnd(end).substring(start.getValue(kind), end).trim()
                assertEquals(card.symbols.firstOrNull { it.kind == kind }?.name ?: "-", cell, "the $kind of ${card.name} in: $line")
            }
            // the column heads: one line names every kind, in the order of the columns
            val heads = screen.screenLines().filter { line -> kinds.all { it in line } }
            assertTrue(heads.isNotEmpty(), "no line names the kinds $kinds as column heads in:\n$screen")
            assertEquals(kinds.sortedBy { start.getValue(it) }, kinds.sortedBy { heads.first().indexOf(it) }, "the heads are in another order than the columns")
        }
    }

    @Test
    fun `CS4 the full name of every region on the screen is still there, on a line with its code`() {
        for ((hand, draft, screen) in everyCardOnce()) for (region in (hand + draft).map { it.region }.distinct()) {
            val code = regionCode.getValue(region)
            assertTrue(screen.screenLines().any { region in it && Regex("\\b$code\\b").containsMatchIn(it) },
                "no line says that $code is $region in:\n$screen")
        }
    }

    // --- CS5 ------------------------------------------------------------------------------------------

    /** The lines that speak of the Throw card. "Throw card" is a word of the game, as "Round" is. */
    private fun throwLines(screen: String): List<String> = screen.screenLines().filter { "throw" in it.lowercase() }

    private fun names(line: String, card: Card): Boolean = card.name in line || line.namesSite(card.site)

    @Test
    fun `CS5 with a card in the draft the question says which card is the Throw card - the first one`() {
        // without the map, so that a site character alone can only be the Throw card's
        val console = console(withCards = false)

        val throwD = throwLines(console.cardQuestion(hand = "EJNOP", draft = "DM"))
        val throwM = throwLines(console.cardQuestion(hand = "EJNOP", draft = "MD"))

        assertTrue(throwD.any { names(it, card('D')) && !names(it, card('M')) }, "no line says that D is the Throw card: $throwD")
        assertTrue(throwM.any { names(it, card('M')) && !names(it, card('D')) }, "no line says that M is the Throw card: $throwM")
    }

    // --- CS6 ------------------------------------------------------------------------------------------

    @Test
    fun `CS6 after CardsShown the next card question shows every other player by name with the shown cards, and not the player's own row again`() {
        // the third pick: every player has shown one card. Without the map, so a site is named only where its card is shown.
        val console = console(withCards = false).told(CardsShown(listOf(cards("M"), cards("S"), cards("T"))))

        val lines = console.cardQuestion(hand = "EJNOP", draft = "DM").screenLines()

        for ((player, shown) in listOf("Bruno" to card('S'), "Chloe" to card('T'))) {
            assertTrue(lines.indices.any { names(lines[it], shown) && ownerOf(lines, it, names) == player },
                "${shown.name} is not shown as a card of $player in:\n${lines.joinToString("\n")}")
        }
        // the own shown card M is in the own draft and nowhere else
        assertEquals(1, lines.count { names(it, card('M')) }, "the player's own shown card is repeated in:\n${lines.joinToString("\n")}")
    }

    // --- CS7 ------------------------------------------------------------------------------------------

    @Test
    fun `CS7 from the second pick on the question names the player the hand came from - seat 0 of three receives from seat 2 with TO_NEXT and from seat 1 with TO_PREVIOUS`() {
        // the Catch card travels the same way in both rounds: only the direction of the hands differs
        val toNext = console(seat = 0, passing = Passing(hands = TO_NEXT, catch = TO_PREVIOUS))
            .cardQuestion(hand = "IJKLMN", draft = "A").screenLines()
        val toPrevious = console(seat = 0, passing = Passing(hands = TO_PREVIOUS, catch = TO_PREVIOUS))
            .cardQuestion(hand = "IJKLMN", draft = "A").screenLines()

        // what one screen says and the other does not is the origin of the hand: Chloe is seat 2, Bruno is seat 1
        val onlyToNext = toNext - toPrevious.toSet()
        val onlyToPrevious = toPrevious - toNext.toSet()
        assertTrue(onlyToNext.any { "Chloe" in it && "Bruno" !in it }, "hands TO_NEXT: no line says the hand came from Chloe: $onlyToNext")
        assertTrue(onlyToPrevious.any { "Bruno" in it && "Chloe" !in it }, "hands TO_PREVIOUS: no line says the hand came from Bruno: $onlyToPrevious")
    }

    // --- CS8 ------------------------------------------------------------------------------------------

    private val titles = listOf("Throw and catch", "Tourist sites", "Region bonus", "Collections", "Animals", "Activities")
    private val roundOne = listOf(3, 7, 0, 9, 8, 2)
    private val roundTwo = listOf(1, 5, 6, 14, 11, 4)

    private fun scored(round: Int, points: List<Int>) =
        RoundScored(round, titles.indices.map { ScoreLine(titles[it], Score(points[it])) }, List(3) { emptyList() })

    /** The numbers of the row of a category in the score sheet: the one line that holds the title and any
     *  number. (The heads of the card tables may hold the same word, and hold no number.) */
    private fun sheetRow(screen: String, title: String): List<Int> {
        val rows = screen.screenLines().filter { title in it && it.numbers().isNotEmpty() }
        assertEquals(1, rows.size, "$title is to have one row in:\n$screen")
        return rows.single().numbers()
    }

    @Test
    fun `CS8 after a scored round the card question shows a score sheet - a row per category with the points of the round and the row total`() {
        val screen = console().told(scored(1, roundOne), RoundStarted(2, "ABCDEFG".toSet())).cardQuestion(hand = "HIJKLMN")

        for (index in titles.indices) assertEquals(listOf(roundOne[index], roundOne[index]), sheetRow(screen, titles[index]), titles[index])
    }

    @Test
    fun `CS8 the score sheet has one column per round played and its rows are in the order the categories were received`() {
        val screen = console().told(scored(1, roundOne), RoundStarted(2, emptySet()), scored(2, roundTwo), RoundStarted(3, emptySet()))
            .cardQuestion(hand = "HIJKLMN")

        for (index in titles.indices) {
            assertEquals(listOf(roundOne[index], roundTwo[index], roundOne[index] + roundTwo[index]), sheetRow(screen, titles[index]), titles[index])
        }
        val rowAt = titles.map { title -> screen.screenLines().indexOfFirst { title in it && it.numbers().isNotEmpty() } }
        assertEquals(rowAt.sorted(), rowAt, "the rows are not in the order of $titles")
    }

    // --- CS9 ------------------------------------------------------------------------------------------

    @Test
    fun `CS9 a card question after a refusal says first which answer was refused, and then puts the question again`() {
        val console = console(round = 2, visited = "ABC")
        val question = console.cardQuestion(hand = "EJNOP", draft = "DM")

        val refused = console.cardQuestion(hand = "EJNOP", draft = "DM", refused = 'Z')

        // Z is a site of the map, so it is looked for only in what comes before the question
        assertTrue(refused.trimEnd().endsWith(question.trimEnd()), "the question does not follow the refusal unchanged:\n$refused")
        val first = refused.trimEnd().removeSuffix(question.trimEnd())
        assertTrue(first.namesSite('Z'), "the refused answer Z is not named first: '$first'")
    }
}
