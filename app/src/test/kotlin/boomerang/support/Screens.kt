package boomerang.support

import boomerang.game.Card
import boomerang.game.CardRequest
import boomerang.game.ChoiceRequest
import boomerang.game.GameEvent
import boomerang.players.ConsolePlayer

/*
 * Helpers for the tests of docs/console-screens.md. A screen is read as text: the tests look for facts
 * (a name, a number, a site character) and for structure (one line per card, one row per category), never
 * for a sentence. The few assumptions about layout that this needs are written at each helper.
 */

/** The character every escape sequence starts with (CS15). */
const val ESC = '\u001B'

/**
 * A ConsolePlayer whose screen is a string. The keyboard is empty: a question is printed when it is put,
 * and these tests never await the answer. A "screen" is everything one call prints (console-screens 3).
 */
class TestConsole(cards: List<Card> = emptyList(), colour: Boolean = false) {
    private val printed = StringBuilder()
    private val player = ConsolePlayer(
        readLine = { null }, print = { printed.append(it).append('\n') }, cards = cards, colour = colour,
    )

    /** Shows the events without looking at what they print: the console only has to remember them (D1). */
    fun told(vararg events: GameEvent): TestConsole {
        for (event in events) player.show(event)
        return this
    }

    /** The screen of one event. */
    fun screenOf(event: GameEvent): String = screen { player.show(event) }

    /** The screen of a card question; hand and draft as site characters of the test table. */
    fun cardQuestion(hand: String, draft: String = "", refused: Char? = null): String =
        screen { player.selectCard(CardRequest(cards(hand), draft(draft), refused)) }

    /** The screen of a choice question. */
    fun choiceQuestion(request: ChoiceRequest): String = screen { player.chooseScore(request) }

    private fun screen(action: () -> Unit): String {
        printed.setLength(0)
        action()
        return printed.toString()
    }
}

/** The lines of a screen that are not blank. */
fun String.screenLines(): List<String> = lines().filter { it.isNotBlank() }

/** Every whole number in the text, in the order written: "Round 2, pick 3 of 7" gives 2, 3, 7. */
fun String.numbers(): List<Int> = Regex("\\d+").findAll(this).map { it.value.toInt() }.toList()

/** The text without escape sequences (CS15): ESC, an opening bracket, digits and semicolons, one letter. */
fun String.withoutEscapes(): String = replace(Regex("$ESC\\[[0-9;]*[A-Za-z]"), "")

/** A site character standing alone: no letter or digit directly before or after it. So "[A]", "(A)", " A "
 *  and "A," name site A, and the A of "WA" or of "Alice" does not. */
private fun alone(site: Char) = Regex("(?<![A-Za-z0-9])${Regex.escape(site.toString())}(?![A-Za-z0-9])")

/** How often the text names the site: the site character standing alone. */
fun String.timesNamed(site: Char): Int = alone(site).findAll(this).count()

fun String.namesSite(site: Char): Boolean = timesNamed(site) > 0

/** How a site is marked where the text names it: the character before it and the one after it. "[A]" gives
 *  "[]", "(A)" gives "()", and a site between spaces or at an end of the text gives two spaces. */
fun String.markOf(site: Char): String {
    val padded = " $this "
    val at = alone(site).find(padded)?.range?.first ?: throw AssertionError("site $site is not named in: $this")
    return "${padded[at - 1]}${padded[at + 1]}"
}

/** The text without the named sites and their marks, without digits and without spaces: what else it says. */
fun String.besidesSites(sites: Collection<Char>): String {
    var rest = " $this "
    for (site in sites) {
        val at = alone(site).find(rest)?.range?.first ?: continue
        rest = rest.substring(0, at - 1) + " " + rest.substring(at + 2)
    }
    return rest.filterNot { it.isDigit() || it.isWhitespace() }
}

/** Of several names, the one in the nearest line at or above lines[index] that holds any of them; null if
 *  no such line. This is how a line is taken to belong to a player: the player is named first, on the same
 *  line or on a line above, and the next player's name ends the block. */
fun ownerOf(lines: List<String>, index: Int, names: List<String>): String? {
    for (at in index downTo 0) {
        val named = names.filter { it in lines[at] }
        if (named.isNotEmpty()) return named.last()
    }
    return null
}
