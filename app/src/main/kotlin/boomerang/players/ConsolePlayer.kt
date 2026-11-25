package boomerang.players

import boomerang.game.Card
import boomerang.game.CardRequest
import boomerang.game.CardsShown
import boomerang.game.ChoiceRequest
import boomerang.game.DraftsRevealed
import boomerang.game.GameAborted
import boomerang.game.GameEvent
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.GameStarted
import boomerang.game.IAnswer
import boomerang.game.IPlayer
import boomerang.game.PlayerLost
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine

/** A human at a text console. Prints every event and question; repeats a question locally until a line can
 *  be read as an answer (one character; a number). A typed option number counts from 1; "0" is not an
 *  answer and the question is repeated locally. Throws PlayerLost when the input ends.
 *  `cards`: the cards of the edition, for the map (D2). `colour`: whether to print escape sequences (D3).
 *  Once it has been shown GameStarted it prints the screens of docs/console-screens.md and remembers what
 *  they need (D1); until then it prints the plain texts of `describe` (D9).
 *  `frame`: for a real terminal, a box around every screen, and a page of its own for the first screen
 *  after an answer (D11); without one, the screens are printed as they are.
 *  `map`: where the regions of the edition lie, row by row, for the map as a board (D13); used only
 *  with a frame. */
class ConsolePlayer(
    private val readLine: () -> String?, private val print: (String) -> Unit,
    cards: List<Card> = emptyList(), colour: Boolean = false, private val frame: TerminalFrame? = null,
    map: List<List<String>> = emptyList(),
) : IPlayer {
    private val memory = ConsoleMemory()
    private val screens = ConsoleScreens(cards, Paint(colour), fan = frame != null, map = map)

    /** D11: whether a line was typed since the last screen. Only then has the person seen all that is on
     *  the terminal, and the next screen may take a page of its own. */
    private var answered = false

    /** Prints the question at once; the line is read when the game awaits the answer. */
    override fun selectCard(request: CardRequest): IAnswer<Char> {
        print(if (memory.started) framed(screens.cardQuestion(request, memory), promptLines = 1) else describe(request))
        return IAnswer { readSite() }
    }

    /** As selectCard. The person types 1 for the first option, and the game counts from 0. */
    override fun chooseScore(request: ChoiceRequest): IAnswer<Int> {
        print(if (memory.started) framed(screens.choiceQuestion(request), promptLines = 1) else describe(request))
        return IAnswer { readOptionNumber() - 1 }
    }

    /** Remembers the event and prints its screen. GameStarted itself is still printed as `describe` gives
     *  it: the game had not started when it arrived. An event with an empty screen prints nothing. */
    override fun show(event: GameEvent) {
        val startedBefore = memory.started
        memory.remember(event)
        val text = if (startedBefore) screens.of(event, memory) else describe(event)
        if (text.isNotEmpty()) print(if (startedBefore) framed(text) else text)
    }

    /** D11: the screen in the frame, if there is one; on a new page if a line was typed since the last screen. */
    private fun framed(screen: String, promptLines: Int = 0): String {
        if (frame == null) return screen
        val newPage = answered
        answered = false
        return frame.around(screen, newPage, promptLines)
    }

    /** Reads lines until one is a single character. Whether it is a card of the hand is for the game to say. */
    private fun readSite(): Char {
        var line = readAnswerLine()
        while (line.length != 1) {
            print("Type one character: the letter or sign of a card.")
            line = readAnswerLine()
        }
        return line[0]
    }

    /** Reads lines until one is a number from 1 up. Whether that option exists is for the game to say. */
    private fun readOptionNumber(): Int {
        var number = readAnswerLine().toIntOrNull()
        while (number == null || number < 1) {
            print("Type a number, 1 or higher.")
            number = readAnswerLine().toIntOrNull()
        }
        return number
    }

    /** The next line typed, without spaces around it. No line means the input has ended: the player is lost. */
    private fun readAnswerLine(): String {
        val line = readLine() ?: throw PlayerLost(this)
        answered = true
        return line.trim()
    }
}

/** The plain text of an event, from the event alone: what a console prints that has not been shown the
 *  start of a game (D9). */
internal fun describe(event: GameEvent): String = when (event) {
    is GameStarted -> "${event.edition}: you are player ${event.seat + 1} of ${event.playerCount}."
    is RoundStarted -> "Round ${event.round}. Sites you have visited: ${siteList(event.visitedSites)}"
    is CardsShown -> describeCards("Cards shown so far:", event.shown)
    is DraftsRevealed ->
        describeCards("The drafts of the round, Throw card first and Catch card last:", event.drafts.map { it.cards })
    is RoundScored -> describeScores(event)
    is GameOver -> describeResult(event.result)
    is GameAborted -> "${playerName(event.lostSeat)} was lost. The game is over."
}

/** The question for a card: what was refused (if anything), the own draft, the hand, and what to type. */
internal fun describe(request: CardRequest): String {
    val lines = mutableListOf<String>()
    if (request.refused != null) lines += "${request.refused} is not a card of your hand."
    lines += "Your draft:"
    lines += cardLines(request.draft.cards)
    lines += "Your hand:"
    lines += cardLines(request.hand)
    lines += "Type the letter or sign of the card to keep:"
    return lines.joinToString("\n")
}

/** The question for a score: what was refused (if anything), the own draft, the numbered options, and what to type. */
internal fun describe(request: ChoiceRequest): String {
    val lines = mutableListOf<String>()
    if (request.refused != null) lines += "Option ${request.refused + 1} is not offered."
    lines += "Your draft:"
    lines += cardLines(request.draft.cards)
    lines += "${request.category}, choose one:"
    for (index in request.scores.indices) {
        lines += "  ${index + 1}  ${optionText(request.scores[index])}"
    }
    lines += "Type the number of your choice:"
    return lines.joinToString("\n")
}

/** A seat as people count: seat 0 is player 1. Also the name of a seat that GameStarted gave none (CS14). */
internal fun playerName(seat: Int): String = "Player ${seat + 1}"

/** Site characters in a fixed order with spaces between; "none" for an empty set. */
private fun siteList(sites: Set<Char>): String =
    if (sites.isEmpty()) "none" else sites.sorted().joinToString(" ")

/** One card on one line: site, name, region, number and symbols. */
private fun cardLine(card: Card): String {
    val symbols = card.symbols.joinToString(", ") { it.name }
    return "${card.site}  ${card.name}, ${card.region}, number ${card.number}, $symbols"
}

/** One indented line per card; one line saying so if there are none. */
private fun cardLines(cards: List<Card>): List<String> =
    if (cards.isEmpty()) listOf("  (no cards)") else cards.map { "  " + cardLine(it) }

/** A heading, then for every seat the player and that player's cards. */
private fun describeCards(heading: String, cardsBySeat: List<List<Card>>): String {
    val lines = mutableListOf(heading)
    for (seat in cardsBySeat.indices) {
        lines += "${playerName(seat)}:"
        lines += cardLines(cardsBySeat[seat])
    }
    return lines.joinToString("\n")
}

/** A score a player may take: what it uses up, or "nothing", with its points. */
private fun optionText(score: Score): String {
    val what = score.chosen?.name ?: "nothing"
    return "$what: ${score.points} points"
}

/** A score line: the category, the points, and the announcement if there is one. */
private fun scoreText(line: ScoreLine): String {
    val announcement = line.score.announcement
    val points = "${line.title}: ${line.score.points}"
    return if (announcement == null) points else "$points ($announcement)"
}

/** The receiver's own lines of the round, then what is announced of every player. */
private fun describeScores(event: RoundScored): String {
    val lines = mutableListOf("Your score in round ${event.round}:")
    for (line in event.own) {
        lines += "  " + scoreText(line)
    }
    for (seat in event.announced.indices) {
        for (line in event.announced[seat]) {
            lines += "${playerName(seat)}, ${scoreText(line)}"
        }
    }
    return lines.joinToString("\n")
}

/** The final total of every player with its parts, then the winner or winners. */
private fun describeResult(result: GameResult): String {
    val lines = mutableListOf("The game is over.")
    for (seat in result.totals.indices) {
        val parts = result.lines[seat].joinToString(", ") { scoreText(it) }
        lines += "${playerName(seat)}: ${result.totals[seat]} points ($parts)"
    }
    lines += winnersText(result.winners)
    return lines.joinToString("\n")
}

/** One winner, or the players who share the win. */
private fun winnersText(winners: List<Int>): String {
    val numbers = winners.map { it + 1 }
    return if (numbers.size == 1) "The winner is player ${numbers[0]}."
    else "The win is shared by players ${numbers.joinToString(" and ")}."
}
