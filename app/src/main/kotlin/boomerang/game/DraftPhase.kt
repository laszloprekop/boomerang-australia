package boomerang.game

/** Rules 4 to 9: deal, Throw card, pass and select, Catch card. Knows no edition and no scoring. */
class DraftPhase(private val players: List<IPlayer>) {
    /** Deals deck[0..6] to seat 0, deck[7..13] to seat 1 and so on, runs the draft and returns the complete
     *  drafts by seat. Hands travel in passing.hands, the last card of each hand in passing.catch. */
    fun run(deck: List<Card>, passing: Passing): List<Draft> = TODO("not implemented")
}
