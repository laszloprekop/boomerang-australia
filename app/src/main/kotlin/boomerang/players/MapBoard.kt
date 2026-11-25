package boomerang.players

import boomerang.game.Card

/** D13: the map as a board, for a real terminal: every region a block in its colour, the blocks placed
 *  as the regions lie on the map of the edition, side by side over the whole width.
 *  `layout`: the rows of the board from top to bottom, each a list of region names from left to right.
 *  An empty name is open water. A name that stands in the same column of the row above goes on as the
 *  same block (a region that is higher than the others). The layout is data the console is handed; it
 *  knows no edition (D8). */
internal class MapBoard(
    private val cards: List<Card>, private val layout: List<List<String>>, private val paint: Paint,
) : IMapView {
    private val regions = regionsOf(cards)

    /** Whether the board can show these cards: there is a layout, and it has every region of the cards. */
    val usable: Boolean = layout.isNotEmpty() && layout.flatten().toSet().containsAll(regions)

    /** The board over the width of a screen, from the first column, as the header of a screen is. */
    override fun lines(visited: Set<Char>, drafted: Set<Char>): List<String> = lines(WIDTH, visited, drafted)

    /** The lines of the board, `width` characters wide: two lines per row of the layout. */
    fun lines(width: Int, visited: Set<Char>, drafted: Set<Char>): List<String> {
        val cell = width / layout.maxOf { it.size }
        val lines = mutableListOf<String>()
        for (row in layout.indices) {
            val names = layout[row]
            val goesOn = names.indices.map { names[it].isNotEmpty() && layout.getOrNull(row - 1)?.getOrNull(it) == names[it] }
            lines += names.indices.joinToString("") { if (goesOn[it]) blank(names[it], cell) else title(names[it], cell) }
            lines += names.indices.joinToString("") {
                if (goesOn[it]) blank(names[it], cell) else sites(names[it], cell, visited, drafted)
            }
        }
        return lines
    }

    /** The first line of a block: the code and the name of the region, bold. Open water is spaces. */
    private fun title(region: String, cell: Int): String {
        if (region.isEmpty()) return " ".repeat(cell)
        val text = " " + regionCode(region).padEnd(CODE_WIDTH) + " " + region
        return paint.card(regions.indexOf(region), text.padEnd(cell).take(cell), bold = true)
    }

    /** The second line of a block: the sites of the region with their marks, and at the right how many of
     *  them the player has, or that the region is complete. */
    private fun sites(region: String, cell: Int, visited: Set<Char>, drafted: Set<Char>): String {
        if (region.isEmpty()) return " ".repeat(cell)
        val index = regions.indexOf(region)
        val sites = cards.filter { it.region == region }.map { it.site }
        val has = sites.count { it in visited || it in drafted }
        val state = if (has == sites.size) "complete" else "$has/${sites.size}"
        val marks = sites.joinToString(paint.card(index, " ", bold = false)) { mark(index, it, visited, drafted) }
        val free = cell - 1 - (sites.size * MARK_WIDTH + sites.size - 1) - state.length - 1
        return paint.card(index, " ", bold = false) + marks +
            paint.card(index, " ".repeat(maxOf(1, free)) + state + " ", bold = has == sites.size)
    }

    /** A site on the board: stamped (dark, in brackets) if visited in an earlier round, on white and in
     *  round brackets if drafted in this round, plain on the colour of the region if not visited. */
    private fun mark(index: Int, site: Char, visited: Set<Char>, drafted: Set<Char>): String = when (site) {
        in visited -> paint.stamp(index, "[$site]")
        in drafted -> paint.fresh("($site)")
        else -> paint.card(index, " $site ", bold = false)
    }

    /** Two lines of a block that only goes on: the colour of the region, nothing written. */
    private fun blank(region: String, cell: Int): String = paint.card(regions.indexOf(region), " ".repeat(cell), bold = false)

    companion object {
        private const val CODE_WIDTH = 3
        private const val MARK_WIDTH = 3
    }
}
