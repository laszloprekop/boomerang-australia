package boomerang.game

/** An edition: its cards, its categories in score sheet order, and its tie-break order.
 *  The constructor throws IllegalArgumentException unless site characters are distinct, category names
 *  are distinct and not blank, and every tie-breaker is one of `categories` (the same object). */
class Edition(
    val name: String, val cards: List<Card>,
    val categories: List<IScoringCategory>, val tieBreakers: List<IScoringCategory>,
) {
    init {
        val sites = cards.map { it.site }
        require(sites.distinct().size == sites.size) { "two cards of $name have the same site character" }

        val names = categories.map { it.name }
        require(names.none { it.isBlank() }) { "a category of $name has no name" }
        require(names.distinct().size == names.size) { "two categories of $name have the same name" }

        // === asks for the same object, not for an equal one
        require(tieBreakers.all { tieBreaker -> categories.any { it === tieBreaker } }) {
            "a tie-breaker of $name is not one of its categories"
        }
    }
}
