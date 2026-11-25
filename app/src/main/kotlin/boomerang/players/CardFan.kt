package boomerang.players

import boomerang.game.Card

/** D12: cards drawn as cards, side by side, each lying on the one to its left: of a card under another
 *  only the left part is seen, without its right edge, and the last card alone has its whole border.
 *  A card is filled with the colour of its region; its number stands in its top edge and its site
 *  character, which is what a person types, in its bottom edge. For a real terminal only; the tables of
 *  CardTable stay what a console without a frame prints. `known` as for CardTable: the cards that decide
 *  the regions and the symbol kinds (D8). */
internal class CardFan(known: List<Card>, private val paint: Paint) {
    private val regions = regionsOf(known)
    private val kinds = known.flatMap { it.symbols }.map { it.kind }.distinct()

    /** The lines of the fan, at most `width` characters wide. `marks`: a word for the top edge of the card
     *  at that place, as "Throw" and "Catch" of a revealed draft. No lines for no cards. */
    fun lines(cards: List<Card>, width: Int, marks: Map<Int, String> = emptyMap()): List<String> {
        if (cards.isEmpty()) return emptyList()
        // every card shows an edge and its text; the last one a second edge
        val textWidth = ((width - 1) / cards.size - 1).coerceIn(MIN_TEXT, MAX_TEXT)
        val all = cards.map { blocks(it, textWidth) }
        val heights = all.first().indices.map { block -> all.maxOf { it[block].size } }
        val rows = mutableListOf(row(cards, "╮") { place -> "╭" + topEdge(cards[place], marks[place], textWidth) })
        for (block in heights.indices) {
            for (line in 0 until heights[block]) {
                // the blocks take turns in shade, as the rows of a table do; the first one is bold
                rows += row(cards, "│", bold = block == 0, shaded = block % 2 == 1) { place ->
                    "│" + all[place][block].getOrElse(line) { "" }.padEnd(textWidth).take(textWidth)
                }
            }
        }
        rows += row(cards, "╯") { place -> "╰" + "[${cards[place].site}]".padEnd(textWidth, '─') }
        return rows
    }

    /** What stands where on a card, for a legend under a fan. */
    fun legend(): String = "number above / " + (listOf("region", "name") + kinds).joinToString(" / ") + " / what to type below"

    /** One line of the fan: a piece per card, each in the colour of its region, and the right edge of the
     *  last card. */
    private fun row(
        cards: List<Card>, lastEdge: String, bold: Boolean = false, shaded: Boolean = false, piece: (Int) -> String,
    ): String {
        val pieces = cards.indices.joinToString("") { paint.card(regions.indexOf(cards[it].region), piece(it), bold, shaded) }
        return pieces + paint.card(regions.indexOf(cards.last().region), lastEdge, bold = false, shaded = shaded)
    }

    /** The top edge of a card: its number, and a word behind it if there is one. */
    private fun topEdge(card: Card, mark: String?, textWidth: Int): String {
        val start = "─${card.number}─" + if (mark == null) "" else " $mark "
        return start.take(textWidth).padEnd(textWidth, '─')
    }

    /** What is written on a card, as blocks of lines: the region code; the name; then one block per symbol
     *  kind, with a dash for a kind the card does not have. A block is broken between words and has no
     *  more lines than its limit; what does not fit is left out. A block is drawn as high as the highest
     *  of its kind in the fan, so the same thing stands on the same line of every card. */
    private fun blocks(card: Card, textWidth: Int): List<List<String>> {
        val symbols = kinds.map { kind ->
            val names = card.symbols.filter { it.kind == kind }.map { it.name }
            wrap((if (names.isEmpty()) "-" else names.joinToString("+")).split(" "), " ", textWidth).take(SYMBOL_LINES)
        }
        return listOf(listOf(regionCode(card.region)), wrap(card.name.split(" "), " ", textWidth).take(NAME_LINES)) + symbols
    }

    companion object {
        /** The width of the text of one card: never less than a number and a word in the top edge need,
         *  and no more than reads well. */
        private const val MIN_TEXT = 9
        private const val MAX_TEXT = 18
        private const val NAME_LINES = 3
        private const val SYMBOL_LINES = 2
    }
}
