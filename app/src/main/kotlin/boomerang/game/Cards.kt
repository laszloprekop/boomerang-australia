package boomerang.game

import java.io.Serializable

/** One symbol on a card. The kind is data: a new kind needs no new type. */
data class Symbol(val kind: String, val name: String) : Serializable

/** One card of an edition: what is printed on it, nothing about how it scores. */
data class Card(
    val site: Char, val name: String, val region: String, val number: Int, val symbols: List<Symbol>,
) : Serializable

/** The cards one player has kept in a round, in the order kept: Throw card first, Catch card seventh. */
data class Draft(val cards: List<Card> = emptyList()) : Serializable {
    /** The first card; IllegalStateException if the draft is empty. */
    val throwCard: Card
        get() {
            check(cards.isNotEmpty()) { "an empty draft has no Throw card" }
            return cards.first()
        }

    /** The seventh card; IllegalStateException if fewer than seven. */
    val catchCard: Card
        get() {
            check(cards.size >= Game.HAND_SIZE) { "a draft of ${cards.size} cards has no Catch card" }
            return cards[Game.HAND_SIZE - 1]
        }

    /** Every card except the Throw card. */
    val shown: List<Card> get() = cards.drop(1)

    /** The site characters of the cards. */
    val sites: Set<Char> get() = cards.map { it.site }.toSet()

    /** Number of cards that carry the symbol. */
    fun count(symbol: Symbol): Int = cards.count { symbol in it.symbols }

    /** A new draft with the card added at the end; this draft stays as it is. */
    operator fun plus(card: Card): Draft = Draft(cards + card)
}
