package boomerang.game

/** An edition: its cards, its categories in score sheet order, and its tie-break order.
 *  The constructor throws IllegalArgumentException unless site characters are distinct, category names
 *  are distinct and not blank, every tie-breaker is one of `categories` (the same object), and there are
 *  enough cards to deal a hand to every player of the largest game. */
class Edition(
    val name: String, val cards: List<Card>,
    val categories: List<IScoringCategory>, val tieBreakers: List<IScoringCategory>,
) {
    init {
        val sites = cards.map { it.site }
        require(sites.distinct().size == sites.size) { "two cards of $name have the same site character" }

        val needed = Game.PLAYERS.last * Game.HAND_SIZE
        require(cards.size >= needed) { "$name has ${cards.size} cards, and a game can need $needed" }

        val names = categories.map { it.name }
        require(names.none { it.isBlank() }) { "a category of $name has no name" }
        require(names.distinct().size == names.size) { "two categories of $name have the same name" }

        // === asks for the same object, not for an equal one
        require(tieBreakers.all { tieBreaker -> categories.any { it === tieBreaker } }) {
            "a tie-breaker of $name is not one of its categories"
        }
    }
}
