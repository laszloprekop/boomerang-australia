package boomerang.players

import boomerang.game.Card
import boomerang.game.CardRequest
import boomerang.game.CardsShown
import boomerang.game.ChoiceRequest
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.Game
import boomerang.game.GameAborted
import boomerang.game.GameEvent
import boomerang.game.GameOver
import boomerang.game.GameResult
import boomerang.game.GameStarted
import boomerang.game.RoundScored
import boomerang.game.RoundStarted
import boomerang.game.Score
import boomerang.game.ScoreLine

/** The screens of docs/console-screens.md: one text per question and per event, at most WIDTH characters
 *  wide, for a console that has seen the start of the game. `cards` are the cards of the edition (D2); with
 *  none given, the map is left out. Every screen starts with an empty line that sets it off from the last.
 *  `cardView`, `mapView`: how cards and the map are drawn. The screens do not know which way they were
 *  handed; without any, a table (CS4) and a list (CS3). */
internal class ConsoleScreens(
    private val cards: List<Card>, private val paint: Paint,
    private val cardView: ICardView = CardTableView(cards, paint),
    private val mapView: IMapView = MapList(cards, paint),
) {

    /** The screen of an event; empty if the event is only remembered. */
    fun of(event: GameEvent, memory: ConsoleMemory): String = when (event) {
        is GameStarted -> describe(event)
        is RoundStarted -> roundStart(memory)
        // nothing now: the next card question shows these cards, and after the last pick DraftsRevealed does
        is CardsShown -> ""
        is DraftsRevealed -> drafts(event, memory)
        is RoundScored -> roundScore(event, memory)
        is GameOver -> result(event.result, memory)
        is GameAborted -> "${memory.name(event.lostSeat)} was lost. The game is over."
    }

    /** 4.1 and 4.2: what was refused (if anything), the header, the map, the own draft, the cards of the
     *  others, the score sheet, the hand, and what to type. */
    fun cardQuestion(request: CardRequest, memory: ConsoleMemory): String {
        val draft = request.draft
        val onScreen = request.hand + draft.cards
        val pick = "Round ${memory.round.round}, pick ${draft.cards.size + 1} of ${Game.HAND_SIZE - 1}"
        val lines = mutableListOf("")
        if (request.refused != null) {
            lines += paint.warning("${request.refused} is not in your hand.") + " Nothing was drafted and no cards were passed."
            lines += ""
        }
        lines += header(pick, memory)
        lines += mapLines(memory.round.visitedSites, draft.sites)
        lines += draftLines(draft, onScreen)
        lines += othersLines(memory)
        lines += sheetLines("SCORE SHEET", memory.rounds, withRoundTotals = false)
        lines += paint.dim(rule('-'))
        lines += handLines(request, memory, onScreen)
        lines += ""
        lines += paint.prompt("Type the letter or sign of the card to keep (${request.hand.joinToString(" ") { it.site.toString() }}):")
        return lines.joinToString("\n")
    }

    /** 4.3: what was refused (if anything), the category, the own draft, the numbered options, what to type. */
    fun choiceQuestion(request: ChoiceRequest): String {
        val lines = mutableListOf("")
        if (request.refused != null) {
            lines += paint.warning("Option ${request.refused + 1} is not offered.") + " Nothing was chosen."
            lines += ""
        }
        lines += paint.bold(request.category.uppercase()) + paint.dim("   choose one for the cards of your draft")
        lines += cardView.cards(request.draft.cards, request.draft.cards)
        lines += optionLines(request)
        lines += paint.prompt("Type the number of your choice:")
        return lines.joinToString("\n")
    }

    // --- the parts of a card question -------------------------------------------------------------------

    /** CS2: the edition, where the game is, and the player's own name, with a line under them. */
    private fun header(middle: String, memory: ConsoleMemory): List<String> {
        val left = memory.edition.uppercase()
        val right = "You are ${memory.name(memory.seat)}"
        val between = headerLine(left, middle, right).removePrefix(left).removeSuffix(right)
        return listOf(paint.bold(left) + between + paint.dim(right), paint.dim(rule('=')))
    }

    /** CS3: the legend, then the map as this console draws it. Nothing without the cards of the edition (D2). */
    private fun mapLines(visited: Set<Char>, drafted: Set<Char>): List<String> {
        if (cards.isEmpty()) return emptyList()
        val legend = paint.bold("MAP") + paint.dim("   [x] visited earlier   (x) drafted this round    x  not visited")
        return listOf(legend) + mapView.lines(visited, drafted) + ""
    }

    /** CS4, CS5: the own draft, and which of its cards is the Throw card. */
    private fun draftLines(draft: Draft, onScreen: List<Card>): List<String> {
        val heading = paint.bold("YOUR DRAFT THIS ROUND") + paint.dim("   ${draft.cards.size} of ${Game.HAND_SIZE} cards")
        if (draft.cards.isEmpty()) return listOf(heading, "")
        val throwCard = paint.dim("  Your Throw card is ${draft.throwCard.site}, the first you kept: hidden until the round ends.")
        return listOf(heading) + cardView.cards(draft.cards, onScreen) + throwCard + ""
    }

    /** CS6: every other player by name with the cards shown so far in this round. Nothing before the first
     *  CardsShown of a round. The player's own cards are in the draft above and are not repeated. */
    private fun othersLines(memory: ConsoleMemory): List<String> {
        if (memory.shown.isEmpty()) return emptyList()
        val lines = mutableListOf(paint.bold("THE OTHERS THIS ROUND") + paint.dim("   their Throw cards are hidden"))
        val table = tableFor(memory.shown.flatten())
        val nameWidth = memory.shown.indices.maxOf { memory.name(it).length }
        for (seat in memory.shown.indices) {
            if (seat == memory.seat) continue
            val lead = "  " + memory.name(seat).padEnd(nameWidth) + "  "
            val shownCards = memory.shown[seat].map { "[${it.site}] ${it.name}" }
            // laid out as plain text first, so that the colour codes do not count as width
            lines += behind(lead, wrap(shownCards, "  ", WIDTH - lead.length)).map { line ->
                memory.shown[seat].fold(line) { text, card -> text.replace("[${card.site}] ", table.site(card) + " ") }
            }
        }
        lines += ""
        return lines
    }

    /** CS4, CS7: the hand, and where it came from: dealt at the first pick, later passed on by
     *  the player the passing of the round names. Without a map, the region codes are spelled out below. */
    private fun handLines(request: CardRequest, memory: ConsoleMemory, onScreen: List<Card>): List<String> {
        val firstPick = request.draft.cards.isEmpty()
        val giver = memory.name(memory.giver(memory.round.passing.hands))
        val origin = if (firstPick) "dealt to you" else "passed to you by $giver"
        val heading = paint.bold("YOUR HAND") + paint.dim("   ${counted(request.hand.size, "card")}, $origin")
        return listOf(heading) + cardView.cards(request.hand, onScreen) + regionLegend(onScreen)
    }

    /** CS4: the full name of every region of these cards beside its code. Not needed under a map, whose
     *  rows say the same. */
    private fun regionLegend(onScreen: List<Card>): List<String> {
        if (cards.isNotEmpty()) return emptyList()
        val lead = "  Regions: "
        val named = regionsOf(onScreen).map { "${regionCode(it)} $it" }
        return behind(lead, wrap(named, ", ", WIDTH - lead.length)).map { paint.dim(it) }
    }

    /** CS8: the score sheet, a row per category in the order received, a column per round played and the
     *  row total; on request a last row with the total of every round and the grand total. Nothing before
     *  the first round is scored. */
    private fun sheetLines(heading: String, rounds: List<List<ScoreLine>>, withRoundTotals: Boolean): List<String> {
        if (rounds.isEmpty()) return emptyList()
        val titles = rounds.first().map { it.title }
        val titleWidth = (titles + ROUND_TOTAL).maxOf { it.length }
        val lines = mutableListOf(paint.bold(heading))
        lines += paint.dim(sheetRow("", titleWidth, rounds.indices.map { "R${it + 1}" }, "Total"))
        for (row in titles.indices) {
            val points = rounds.map { it[row].score.points }
            lines += sheetRow(titles[row], titleWidth, points.map { it.toString() }, points.sum().toString())
        }
        if (withRoundTotals) {
            val totals = rounds.map { round -> round.sumOf { it.score.points } }
            lines += paint.bold(sheetRow(ROUND_TOTAL, titleWidth, totals.map { it.toString() }, totals.sum().toString()))
        }
        lines += ""
        return lines
    }

    /** One row of the score sheet: the title, a cell per round, the total. All cells end in a straight line. */
    private fun sheetRow(title: String, titleWidth: Int, cells: List<String>, total: String): String =
        "  " + title.padEnd(titleWidth) + cells.joinToString("") { it.padStart(ROUND_WIDTH) } + total.padStart(TOTAL_WIDTH)

    // --- the parts of a choice question -----------------------------------------------------------------

    /** CS10: the options numbered from 1, each with what it uses up, why it is worth what it is, and its points. */
    private fun optionLines(request: ChoiceRequest): List<String> {
        val whats = request.scores.map { it.chosen?.name ?: "nothing" }
        val reasons = request.scores.map { optionReason(it, request.draft) }
        val whatWidth = whats.maxOf { it.length }
        val reasonWidth = reasons.maxOf { it.length }
        return request.scores.indices.map { index ->
            val points = request.scores[index].points
            val pointsText = points.toString().padStart(POINTS_WIDTH) + if (points == 1) " point" else " points"
            "  ${index + 1}  " + whats[index].padEnd(whatWidth) + "  " + reasons[index].padEnd(reasonWidth) + pointsText
        }
    }

    /** Why an option is worth what it is: its explanation; without one, how many cards of the draft carry
     *  the symbol it uses up (D6); without a symbol, nothing. */
    private fun optionReason(score: Score, draft: Draft): String {
        val symbol = score.chosen
        if (score.explanation != null) return score.explanation
        if (symbol != null) return counted(draft.count(symbol), "card")
        return ""
    }

    /** Why a score of the round is what it is: its explanation; without one, the symbol it used up and how
     *  many cards of the draft carry it (D6); without a symbol, what it announces, or nothing. */
    private fun scoreReason(score: Score, draft: Draft): String {
        val symbol = score.chosen
        if (score.explanation != null) return score.explanation
        if (symbol != null) return symbol.name + ", " + counted(draft.count(symbol), "card")
        return score.announcement ?: ""
    }

    // --- the screens of events --------------------------------------------------------------------------

    /** The start of a round: who is at the table, and who passes to whom (D5). */
    private fun roundStart(memory: ConsoleMemory): String {
        val passing = memory.round.passing
        val lead = "  At the table: "
        val table = memory.names.indices.map { nameWithYou(it, memory) }
        val lines = mutableListOf("")
        lines += paint.bold("ROUND ${memory.round.round} OF ${Game.ROUNDS}")
        lines += behind(lead, wrap(table, ", ", WIDTH - lead.length))
        lines += "  You pass your hands on to ${memory.name(memory.receiver(passing.hands))}."
        lines += "  Your last card, the Catch card, comes from ${memory.name(memory.giver(passing.catch))}."
        return lines.joinToString("\n")
    }

    /** CS12: every player by name with all seven cards; the first is marked "Throw", the seventh "Catch". */
    private fun drafts(event: DraftsRevealed, memory: ConsoleMemory): String {
        val all = event.drafts.flatMap { it.cards }
        val lines = mutableListOf("")
        lines += paint.bold("THE DRAFTS OF ROUND ${memory.round.round}")
        lines += cardView.revealedHead(all)
        for (seat in event.drafts.indices) {
            lines += "  " + paint.bold(nameWithYou(seat, memory))
            lines += cardView.revealed(event.drafts[seat].cards, all, DRAFT_MARKS)
        }
        return lines.joinToString("\n")
    }

    /** CS11: one line per category with title, reason and points; the total of the round; then what is
     *  announced of the other players, by name. */
    private fun roundScore(event: RoundScored, memory: ConsoleMemory): String {
        val titleWidth = (event.own.maxOfOrNull { it.title.length } ?: 0) + 2
        val lines = mutableListOf("")
        lines += paint.bold("ROUND ${event.round} SCORE")
        for (line in event.own) {
            lines += scoreLines(line, titleWidth, memory.draft)
        }
        lines += paint.dim("-".repeat(POINTS_WIDTH + 2).padStart(WIDTH))
        lines += paint.bold(pointsLine("  Round ${event.round} total", event.own.sumOf { it.score.points }))
        for (seat in event.announced.indices) {
            if (seat == memory.seat) continue
            for (line in event.announced[seat]) {
                val text = "${memory.name(seat)}: ${line.title} ${line.score.points} (${line.score.announcement})"
                lines += behind("  ", wrap(text.split(" "), " ", WIDTH - 2))
            }
        }
        return lines.joinToString("\n")
    }

    /** One score of the round: title, reason and points on one line; a reason too long for it goes on in
     *  lines of its own below, under the reason. */
    private fun scoreLines(line: ScoreLine, titleWidth: Int, draft: Draft): List<String> {
        val lead = "  " + line.title.padEnd(titleWidth)
        val reasonWidth = WIDTH - lead.length - POINTS_WIDTH - 1
        val reason = wrap(scoreReason(line.score, draft).split(" "), " ", reasonWidth)
        val first = pointsLine(lead + (reason.firstOrNull() ?: ""), line.score.points)
        return listOf(first) + reason.drop(1).map { " ".repeat(lead.length) + it }
    }

    /** A text at the left and points at the right end of a line of the whole width. */
    private fun pointsLine(text: String, points: Int): String =
        text.padEnd(WIDTH - POINTS_WIDTH) + points.toString().padStart(POINTS_WIDTH)

    /** 4.4, CS8, CS13: the header, the own score sheet with the round totals, and the players ranked by
     *  total, highest first, with the receiver and every winner marked. */
    private fun result(result: GameResult, memory: ConsoleMemory): String {
        val lines = mutableListOf("")
        lines += header("Final score", memory)
        lines += sheetLines("YOUR SCORE SHEET", memory.rounds, withRoundTotals = true)
        lines += paint.bold("RESULT")
        val ranked = ranked(result)
        val nameWidth = ranked.maxOf { nameWithYou(it, memory).length }
        for (place in ranked.indices) {
            val seat = ranked[place]
            val total = result.totals[seat].toString().padStart(TOTAL_WIDTH)
            val line = "  ${place + 1}  " + nameWithYou(seat, memory).padEnd(nameWidth) + total + winnerMark(seat, result.winners)
            lines += if (seat in result.winners) paint.good(line) else line
        }
        return lines.joinToString("\n")
    }

    /** The seats by total, highest first; of two with the same total a winner comes first. */
    private fun ranked(result: GameResult): List<Int> =
        result.totals.indices.sortedWith(compareByDescending<Int> { result.totals[it] }.thenByDescending { it in result.winners })

    /** What stands behind a winner: a winner alone and one of several read differently (CS13). */
    private fun winnerMark(seat: Int, winners: List<Int>): String {
        if (seat !in winners) return ""
        return "   " + if (winners.size == 1) "winner" else "shares the win"
    }

    // --- shared -----------------------------------------------------------------------------------------

    /** The name of a seat, with "(you)" behind the console's own. */
    private fun nameWithYou(seat: Int, memory: ConsoleMemory): String =
        if (seat == memory.seat) "${memory.name(seat)} (you)" else memory.name(seat)

    /** A card table for a screen. Its columns fit the edition if the console knows the cards of the
     *  edition, else the cards on this screen. */
    private fun tableFor(onScreen: List<Card>): CardTable = CardTable(cards.ifEmpty { onScreen }, paint)

    companion object {
        /** The title of the last row of the final score sheet. */
        private const val ROUND_TOTAL = "Round total"

        /** The width of a round column and of the total column of the score sheet. */
        private const val ROUND_WIDTH = 6
        private const val TOTAL_WIDTH = 8

        /** The width points are written in, at the right end of a line. */
        private const val POINTS_WIDTH = 4

        /** The cards of a revealed draft that have a name: the first kept and the last received. */
        private val DRAFT_MARKS = mapOf(0 to "Throw", Game.HAND_SIZE - 1 to "Catch")
    }
}
