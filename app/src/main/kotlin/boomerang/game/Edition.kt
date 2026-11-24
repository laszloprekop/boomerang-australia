package boomerang.game

/** An edition: its cards, its categories in score sheet order, and its tie-break order.
 *  The constructor throws IllegalArgumentException unless site characters are distinct, category names
 *  are distinct and not blank, and every tie-breaker is one of `categories` (the same object).
 *  Stub: the check is not implemented yet. */
class Edition(
    val name: String, val cards: List<Card>,
    val categories: List<IScoringCategory>, val tieBreakers: List<IScoringCategory>,
)
