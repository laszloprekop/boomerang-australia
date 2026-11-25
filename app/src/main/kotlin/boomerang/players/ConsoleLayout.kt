package boomerang.players

import boomerang.game.Card
import kotlin.math.max

/** CS1: no line of a screen is longer than this. */
internal const val WIDTH = 80

/** Colour for a console (D3, CS15). Every function gets text that is already laid out and adds only escape
 *  sequences around it, so that the text without them is the text of a console without colour. */
internal class Paint(private val on: Boolean) {
    /** For headings: bold and white. */
    fun bold(text: String): String = sgr("1;" + ink(WHITE), text)

    /** For what explains and what is not the player's: legends, column heads, lines, sites not visited. */
    fun dim(text: String): String = sgr(ink(GREY), text)

    /** For what is of this round and must be found at once: a site drafted now. */
    fun now(text: String): String = sgr("1;" + ink(WHITE), text)

    /** For good news: a region that is complete, a winner. */
    fun good(text: String): String = sgr(ink(GREEN), text)

    /** For an answer that was refused. */
    fun warning(text: String): String = sgr("1;" + ink(RED), text)

    /** For the line that says what to type. */
    fun prompt(text: String): String = sgr(ink(BLUE), text)

    /** A region: dark, bold text on the colour of the region; `index` is the place of the region in the
     *  order the cards name them (D8). */
    fun region(index: Int, text: String): String =
        sgr("1;" + ink(DARK) + ";" + ground(REGION_COLOURS[index % REGION_COLOURS.size]), text)

    /** A piece of a card (D12): dark text on the colour of its region, bold on request. `shaded`: on a
     *  darker shade of that colour, for every second block of a card. */
    fun card(index: Int, text: String, bold: Boolean, shaded: Boolean = false): String {
        val colour = REGION_COLOURS[index % REGION_COLOURS.size]
        val behind = if (shaded) colour.split(";").joinToString(";") { (it.toInt() * SHADE / 100).toString() } else colour
        return sgr((if (bold) "1;" else "") + ink(DARK) + ";" + ground(behind), text)
    }

    /** A line of the logo of an edition (D14): bold, in the yellow of a road sign. */
    fun logo(text: String): String = sgr("1;" + ink(SIGN_YELLOW), text)

    /** A site that was visited, on the board of the map (D13): the colour of its region on dark, bold. */
    fun stamp(index: Int, text: String): String =
        sgr("1;" + ink(REGION_COLOURS[index % REGION_COLOURS.size]) + ";" + ground(DARK), text)

    /** A site drafted in this round, on the board of the map (D13): dark on white, bold. */
    fun fresh(text: String): String = sgr("1;" + ink(DARK) + ";" + ground(WHITE), text)

    /** The text between an ANSI "select graphic rendition" code and the code that resets it; with colour
     *  off, or with no text, the text as it is. */
    private fun sgr(code: String, text: String): String =
        if (on && text.isNotEmpty()) "$ESCAPE[${code}m$text$ESCAPE[0m" else text

    /** The code that sets the colour of the text, and the one that sets the colour behind it: 24-bit
     *  colour, as red;green;blue (D10). */
    private fun ink(rgb: String): String = "38;2;$rgb"
    private fun ground(rgb: String): String = "48;2;$rgb"
}

/** The character that starts an escape sequence. */
private const val ESCAPE = '\u001B'

/** D8, D10: seven background colours, given to the regions in order of first appearance: coral, orange,
 *  yellow, green, cyan, violet, lavender. Each as red;green;blue. */
private val REGION_COLOURS = listOf(
    "240;144;124", "240;180;106", "236;217;102", "159;214;122", "95;211;220", "199;164;242", "169;164;245",
)

/** D10: the other colours of a screen, each as red;green;blue. */
/** D12: how light a shaded block of a card is, in hundredths of the colour of its region. */
private const val SHADE = 93

private const val WHITE = "255;255;255"
private const val SIGN_YELLOW = "255;205;0"
private const val GREY = "139;145;155"
private const val DARK = "20;22;26"
private const val GREEN = "143;220;159"
private const val RED = "255;157;146"
private const val BLUE = "138;180;248"

/** D8: a short code for a region, made from its name: the initials of a name of several words (WA), else
 *  its first three letters in capitals (QUE). */
internal fun regionCode(region: String): String {
    val words = region.split(" ")
    if (words.size == 1) return region.take(3).uppercase()
    return words.joinToString("") { it.take(1) }.uppercase()
}

/** The regions of the cards, each once, in the order the cards name them. */
internal fun regionsOf(cards: List<Card>): List<String> = cards.map { it.region }.distinct()

/** A number with its word: "1 card", "3 cards". */
internal fun counted(count: Int, word: String): String = if (count == 1) "1 $word" else "$count ${word}s"

/** Packs the items into lines of at most `width` characters, in order, with the separator between two
 *  items of a line. An item is never split, so one that is too long has a line of its own. */
internal fun wrap(items: List<String>, separator: String, width: Int): List<String> {
    val lines = mutableListOf<String>()
    var line = ""
    for (item in items) {
        val longer = if (line.isEmpty()) item else line + separator + item
        if (line.isEmpty() || longer.length <= width) {
            line = longer
        } else {
            lines += line
            line = item
        }
    }
    if (line.isNotEmpty()) lines += line
    return lines
}

/** The texts one per line behind a lead: the first line starts with the lead, the others with as many spaces. */
internal fun behind(lead: String, texts: List<String>): List<String> =
    texts.mapIndexed { index, text -> (if (index == 0) lead else " ".repeat(lead.length)) + text }

/** A line across the whole width, made of one character. */
internal fun rule(character: Char): String = character.toString().repeat(WIDTH)

/** Whether a line of a screen only separates two parts of it: an empty line, or a rule as `rule` makes
 *  it. `line` is the line as it is seen, without escape sequences. A frame draws its dividers there (D11). */
internal fun separates(line: String): Boolean = line.isBlank() || line == rule('-') || line == rule('=')

/** Three texts on one line of the whole width: one at the left, one in the middle, one at the right. */
internal fun headerLine(left: String, middle: String, right: String): String {
    val free = WIDTH - left.length - middle.length - right.length
    val before = max(1, free / 2)
    val after = max(1, free - before)
    return left + " ".repeat(before) + middle + " ".repeat(after) + right
}

/** CS4: card tables with one card per line. The columns are made wide enough for every one of the known
 *  cards, and there is one column per symbol kind those cards carry (D8: nothing here names an edition). */
internal class CardTable(known: List<Card>, private val paint: Paint) {
    private val regions = regionsOf(known)
    private val kinds = known.flatMap { it.symbols }.map { it.kind }.distinct()
    private val nameWidth = max(NAME_HEAD.length, known.maxOfOrNull { it.name.length } ?: 0)
    private val numberWidth = max(NUMBER_HEAD.length, known.maxOfOrNull { it.number.toString().length } ?: 0)
    private val codeWidth = max(REGION_HEAD.length, regions.maxOfOrNull { regionCode(it).length } ?: 0)
    private val kindWidths = kinds.map { kind -> max(kind.length, known.maxOfOrNull { cell(it, kind).length } ?: 0) }

    /** The line of column heads. `lead` is what a card line starts with, here as spaces. */
    fun head(lead: String = LEAD, withRegion: Boolean = true): String {
        val site = " ".repeat(SITE_WIDTH)
        val region = if (withRegion) " " + REGION_HEAD.padEnd(codeWidth) else ""
        return paint.dim(lead + site + " " + NAME_HEAD.padEnd(nameWidth) + " " + NUMBER_HEAD.padStart(numberWidth) +
            region + " " + padded(kinds))
    }

    /** One card on one line: the lead, the site character in brackets and the region code, both in the
     *  colour of the region, the name, the number, and one cell per symbol kind. */
    fun line(card: Card, lead: String = LEAD, withRegion: Boolean = true): String {
        val region = if (withRegion) " " + regionCell(card.region) else ""
        return lead + site(card) + " " + card.name.padEnd(nameWidth) + " " +
            card.number.toString().padStart(numberWidth) + region + " " + padded(kinds.map { cell(card, it) })
    }

    /** The head and one line per card, as a card question shows a hand or a draft. */
    fun lines(cards: List<Card>): List<String> = listOf(head()) + cards.map { line(it) }

    /** The site character of a card in brackets, in the colour of its region. */
    fun site(card: Card): String = paint.region(regions.indexOf(card.region), "[${card.site}]")

    /** The code of the region in the colour of the region, then spaces up to the width of its column. */
    private fun regionCell(region: String): String {
        val code = regionCode(region)
        return paint.region(regions.indexOf(region), code) + " ".repeat(codeWidth - code.length)
    }

    /** What a card has of one symbol kind: the name of the symbol, or a dash if it has none. */
    private fun cell(card: Card, kind: String): String {
        val names = card.symbols.filter { it.kind == kind }.map { it.name }
        return if (names.isEmpty()) "-" else names.joinToString("+")
    }

    /** One text per symbol column, each as wide as its column, with a space between; nothing after the last. */
    private fun padded(texts: List<String>): String =
        texts.indices.joinToString(" ") { texts[it].padEnd(kindWidths[it]) }.trimEnd()

    companion object {
        /** What a card line starts with when nothing else is asked for. */
        const val LEAD = "  "

        /** The site character and its two brackets. */
        private const val SITE_WIDTH = 3
        private const val NAME_HEAD = "Site"
        private const val NUMBER_HEAD = "No"
        private const val REGION_HEAD = "Reg"
    }
}
