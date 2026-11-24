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
    val throwCard: Card get() = TODO("not implemented")

    /** The seventh card; IllegalStateException if fewer than seven. */
    val catchCard: Card get() = TODO("not implemented")

    /** Every card except the Throw card. */
    val shown: List<Card> get() = TODO("not implemented")

    val sites: Set<Char> get() = TODO("not implemented")

    /** Number of cards that carry the symbol. */
    fun count(symbol: Symbol): Int = TODO("not implemented")

    operator fun plus(card: Card): Draft = TODO("not implemented")
}
