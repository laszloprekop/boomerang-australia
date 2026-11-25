package boomerang.players

/** A frame for a real terminal (D11): a screen of ConsoleScreens inside a box with rounded corners, its
 *  parts as sections of the box, and on request on a page of its own. A page is "faked": the terminal is
 *  moved up by its own height, so the old screen is out of sight but still there to scroll back to.
 *  Nothing here knows a rule or an event; it reads only the lines of a screen.
 *  `rows`: the height of the terminal in lines. `colour`: whether the box is drawn dim (D3). */
class TerminalFrame(private val rows: Int, colour: Boolean = false) {
    private val paint = Paint(colour)

    /** The screen in its box. `newPage`: first move everything on the terminal up and out of sight.
     *  `promptLines`: so many lines at the end stay below the box; they say what to type. */
    fun around(screen: String, newPage: Boolean, promptLines: Int = 0): String {
        val lines = screen.split("\n")
        val inside = sections(lines.dropLast(promptLines))
        val below = lines.takeLast(promptLines)
        val box = listOf(edge('╭', '╮')) + inside.map { row(it) } + edge('╰', '╯')
        return (if (newPage) pageBreak() else "") + (box + below).joinToString("\n")
    }

    /** The lines of a box: an empty line and a line across the whole width both become one divider. No
     *  divider at the start, at the end, or directly after another. */
    private fun sections(lines: List<String>): List<String?> {
        val out = mutableListOf<String?>()
        for (line in lines) {
            if (isDivider(line)) {
                if (out.isNotEmpty() && out.last() != null) out += null
            } else {
                out += line
            }
        }
        while (out.isNotEmpty() && out.last() == null) out.removeLast()
        return out
    }

    /** An empty line, or a line of the whole width made of one of the two line characters. */
    private fun isDivider(line: String): Boolean {
        val text = visible(line)
        return text.isBlank() || (text.length == WIDTH && (text.all { it == '-' } || text.all { it == '=' }))
    }

    /** One row of the box: a line of the screen between the two sides, or a divider for null. */
    private fun row(line: String?): String {
        if (line == null) return edge('├', '┤')
        val side = paint.dim("│")
        return side + " " + line + " ".repeat(maxOf(0, WIDTH - visible(line).length)) + " " + side
    }

    /** A line across the box between two corner or joint characters. */
    private fun edge(left: Char, right: Char): String = paint.dim(left + "─".repeat(WIDTH + 2) + right)

    /** As many new lines as the terminal is high, which moves all that is on it into what can be scrolled
     *  back to; then the cursor to the top left corner. */
    private fun pageBreak(): String = "\n".repeat(rows) + "$ESCAPE_CHARACTER[H"

    /** A line as it is seen: without escape sequences. */
    private fun visible(line: String): String = line.replace(ESCAPE_SEQUENCE, "")

    companion object {
        private const val ESCAPE_CHARACTER = '\u001B'
        private val ESCAPE_SEQUENCE = Regex("$ESCAPE_CHARACTER\\[[0-9;]*[A-Za-z]")
    }
}
