package boomerang.game

/** Puts the cards of a deck in the order of the next deal. Returns the same cards, each once. */
fun interface IShuffler { fun shuffle(cards: List<Card>): List<Card> }

/** The production shuffler: the order comes from the random source it is given. */
class RandomShuffler(private val random: kotlin.random.Random) : IShuffler {
    /** shuffled returns a new list, so the list given is left as it was. */
    override fun shuffle(cards: List<Card>): List<Card> = cards.shuffled(random)
}
