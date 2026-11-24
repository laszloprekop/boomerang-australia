package boomerang.game

/** Puts the cards of a deck in the order of the next deal. Returns the same cards, each once. */
fun interface IShuffler { fun shuffle(cards: List<Card>): List<Card> }

/** The production shuffler: the order comes from the random source it is given. */
class RandomShuffler(private val random: kotlin.random.Random) : IShuffler {
    override fun shuffle(cards: List<Card>): List<Card> = TODO("not implemented")
}
