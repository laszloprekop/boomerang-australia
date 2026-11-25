package boomerang.players

/** What a console may do at a real terminal and not when its text goes into a pipe or a file. Three
 *  choices, each on its own; with none made, a console prints the plain screens of console-screens.md.
 *  `frame`: a box around every screen, and pages (D11). `cardFan`: hands and drafts as cards lying on each
 *  other and not as tables (D12). `map`: where the regions of the edition lie, row by row from the top, an
 *  empty name for open water; with it the map is a board and not a list (D13). */
class TerminalLook(
    val frame: TerminalFrame? = null, val cardFan: Boolean = false, val map: List<List<String>> = emptyList(),
)
