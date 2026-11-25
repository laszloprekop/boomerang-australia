package boomerang.players

import boomerang.game.Card

/** D12: cards drawn as cards, side by side, each lying on the one to its left: of a card under another
 *  only the left part is seen, without its right edge, and the last card alone has its whole border.
 *  A card is filled with the colour of its region. For a real terminal only; the tables of CardTable stay
 *  what a console without a frame prints. `known` as for CardTable: the cards that decide the regions
 *  and the symbol kinds (D8). */
internal class CardFan(known: List<Card>, private val paint: Paint) {
    private val regions = regionsOf(known)
    private val kinds = known.flatMap { it.symbols }.map { it.kind }.distinct()

    /** The lines of the fan, at most `width` characters wide. `marks`: a word for the top edge of the card
     *  at that place, as "Throw" and "Catch" of a revealed draft. No lines for no cards. */
    fun lines(cards: List<Card>, width: Int, marks: Map<Int, String> = emptyMap()): List<String> {
        if (cards.isEmpty()) return emptyList()
        // every card shows an edge and its text; the last one a second edge
        val textWidth = ((width - 1) / cards.size - 1).coerceIn(MIN_TEXT, MAX_TEXT)
        val faces = faces(cards, textWidth)
        val rows = mutableListOf(row(cards) { place -> "╭" + topEdge(marks[place], textWidth) } + corner(cards, "╮"))
        for (line in faces.first().indices) {
            val bold = line == 0
            rows += row(cards, bold) { place -> "│" + faces[place][line].padEnd(textWidth).take(textWidth) } +
                corner(cards, "│")
        }
        rows += row(cards) { "╰" + "─".repeat(textWidth) } + corner(cards, "╯")
        return rows
    }

    /** The names of the symbol kinds in the order of the lines of a card, for a legend under a fan. */
    fun legend(): String = (listOf("site, region, number", "name") + kinds).joinToString(" / ")

    /** One line of the fan: a piece per card, each in the colour of its region. */
    private fun row(cards: List<Card>, bold: Boolean = false, piece: (Int) -> String): String =
        cards.indices.joinToString("") { paint.card(regions.indexOf(cards[it].region), piece(it), bold) }

    /** The right edge of the last card, in its colour. */
    private fun corner(cards: List<Card>, edge: String): String =
        paint.card(regions.indexOf(cards.last().region), edge, bold = false)

    /** The top edge of a card, with a word in it if there is one. */
    private fun topEdge(mark: String?, textWidth: Int): String =
        if (mark == null) "─".repeat(textWidth) else (" $mark ").take(textWidth).padEnd(textWidth, '─')

    /** What is written on a card, as blocks of lines: site, region code and number; the name; then one
     *  block per symbol kind, with a dash for a kind the card does not have. A block is broken between
     *  words and has no more lines than its limit; what does not fit is left out. */
    private fun blocks(card: Card, textWidth: Int): List<List<String>> {
        val left = "[${card.site}] ${regionCode(card.region)}"
        val number = card.number.toString()
        val first = left.padEnd(textWidth - number.length).take(textWidth - number.length) + number
        val symbols = kinds.map { kind ->
            val names = card.symbols.filter { it.kind == kind }.map { it.name }
            wrap((if (names.isEmpty()) "-" else names.joinToString("+")).split(" "), " ", textWidth).take(SYMBOL_LINES)
        }
        return listOf(listOf(first), wrap(card.name.split(" "), " ", textWidth).take(NAME_LINES)) + symbols
    }

    /** The lines of every card of a fan. A block is as high as the highest of its kind among these cards,
     *  so all cards are equally high and the same thing stands on the same line of each. */
    private fun faces(cards: List<Card>, textWidth: Int): List<List<String>> {
        val all = cards.map { blocks(it, textWidth) }
        val heights = all.first().indices.map { block -> all.maxOf { it[block].size } }
        return all.map { card -> card.indices.flatMap { block -> List(heights[block]) { card[block].getOrElse(it) { "" } } } }
    }

    companion object {
        /** The width of the text of one card: never less than a site, a code and a number need, and no
         *  more than reads well. */
        private const val MIN_TEXT = 9
        private const val MAX_TEXT = 18
        private const val NAME_LINES = 3
        private const val SYMBOL_LINES = 2
    }
}
