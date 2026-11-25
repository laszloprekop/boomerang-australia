package boomerang.players

import boomerang.game.Card

/** How a console draws cards. Two ways exist: as a table, for any console (CS4), and as cards lying on
 *  each other, for a real terminal (D12). ConsoleScreens is handed one and does not know which. */
internal interface ICardView {
    /** A hand or a draft. `onScreen`: all cards of the screen these lines are part of. */
    fun cards(shown: List<Card>, onScreen: List<Card>): List<String>

    /** What stands once above all revealed drafts of a round; `all` are the cards of all of them. */
    fun revealedHead(all: List<Card>): List<String>

    /** One revealed draft. `marks`: a word for the card at that place ("Throw", "Catch"). */
    fun revealed(draft: List<Card>, all: List<Card>, marks: Map<Int, String>): List<String>
}

/** How a console draws the map: the lines under the heading, for the sites visited earlier and the sites
 *  drafted in this round. Two ways exist: a list of the regions (CS3) and a board (D13). */
internal interface IMapView {
    fun lines(visited: Set<Char>, drafted: Set<Char>): List<String>
}

/** Cards as a table (CS4, CS12). The columns fit the edition if the console knows its cards, else the
 *  cards on the screen. */
internal class CardTableView(private val known: List<Card>, private val paint: Paint) : ICardView {
    override fun cards(shown: List<Card>, onScreen: List<Card>): List<String> = table(onScreen).lines(shown)

    override fun revealedHead(all: List<Card>): List<String> =
        listOf(table(all).head(lead = " ".repeat(MARK_WIDTH), withRegion = false))

    /** The mark takes the place the region code has in a card question: a line has room for one of the two. */
    override fun revealed(draft: List<Card>, all: List<Card>, marks: Map<Int, String>): List<String> {
        val table = table(all)
        return draft.indices.map { place ->
            table.line(draft[place], lead = (marks[place] ?: "").padEnd(MARK_WIDTH), withRegion = false)
        }
    }

    private fun table(onScreen: List<Card>): CardTable = CardTable(known.ifEmpty { onScreen }, paint)

    companion object {
        /** The width of what stands in front of a card of a revealed draft: "Throw" or "Catch" and a space. */
        private const val MARK_WIDTH = 6
    }
}

/** Cards as a fan (D12), with what the lines of a card are below it. Nothing for no cards. */
internal class CardFanView(private val known: List<Card>, private val paint: Paint) : ICardView {
    override fun cards(shown: List<Card>, onScreen: List<Card>): List<String> = fan(shown, emptyMap())

    /** A fan needs no head: every card says what it is. */
    override fun revealedHead(all: List<Card>): List<String> = emptyList()

    override fun revealed(draft: List<Card>, all: List<Card>, marks: Map<Int, String>): List<String> = fan(draft, marks)

    private fun fan(shown: List<Card>, marks: Map<Int, String>): List<String> {
        if (shown.isEmpty()) return emptyList()
        val cardFan = CardFan(known.ifEmpty { shown }, paint)
        return cardFan.lines(shown, WIDTH - LEAD.length, marks).map { LEAD + it } + paint.dim(LEAD + cardFan.legend())
    }

    companion object {
        private const val LEAD = "  "
    }
}

/** The map as a list (CS3): one row per region with its sites, how many of them the player has, and
 *  whether that is all. */
internal class MapList(private val cards: List<Card>, private val paint: Paint) : IMapView {
    override fun lines(visited: Set<Char>, drafted: Set<Char>): List<String> {
        val regions = regionsOf(cards)
        val nameWidth = regions.maxOf { it.length }
        val codeWidth = regions.maxOf { regionCode(it).length }
        return regions.indices.map { index ->
            val region = regions[index]
            val label = regionCode(region).padEnd(codeWidth) + " " + region.padEnd(nameWidth)
            "  " + paint.region(index, label) + "  " + regionSites(index, region, visited, drafted)
        }
    }

    /** The sites of a region with their marks, how many of them the player has, and whether that is all. */
    private fun regionSites(index: Int, region: String, visited: Set<Char>, drafted: Set<Char>): String {
        val sites = cards.filter { it.region == region }.map { it.site }
        val marks = sites.joinToString(" ") { siteMark(index, it, visited, drafted) }
        val has = sites.count { it in visited || it in drafted }
        val complete = if (has == sites.size) "  " + paint.good("complete") else ""
        return "$marks  $has/${sites.size}$complete"
    }

    /** A site as the map shows it: in square brackets and the colour of its region if visited in an earlier
     *  round, in round ones and bold if drafted in this round, between spaces and dim if the player does
     *  not have it. `index` is the place of the region among the regions. */
    private fun siteMark(index: Int, site: Char, visited: Set<Char>, drafted: Set<Char>): String = when (site) {
        in visited -> paint.region(index, "[$site]")
        in drafted -> paint.now("($site)")
        else -> paint.dim(" $site ")
    }
}
