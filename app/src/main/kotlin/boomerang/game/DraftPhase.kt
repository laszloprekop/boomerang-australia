package boomerang.game

/** Rules 4 to 9: deal, Throw card, pass and select, Catch card. Knows no edition and no scoring. */
class DraftPhase(private val players: List<IPlayer>) {
    /** A player selects one card less than the hand holds: the last card is not chosen, it is the Catch card. */
    private val lastStep = Game.HAND_SIZE - 1

    /** Deals deck[0..6] to seat 0, deck[7..13] to seat 1 and so on, runs the draft and returns the complete
     *  drafts by seat. Hands travel in passing.hands, the last card of each hand in passing.catch. */
    fun run(deck: List<Card>, passing: Passing): List<Draft> {
        var hands = deal(deck)
        var drafts = List(players.size) { Draft() }
        for (step in 1..lastStep) {
            val selected = selectCards(hands, drafts)
            hands = withoutSelected(hands, selected)
            drafts = withSelected(drafts, selected)
            // rule 6: the hands travel while they hold more than one card, so not after the last step
            if (step < lastStep) hands = pass(hands, passing.hands)
            // step 1 selected the Throw cards, which stay hidden, so there is nothing to show yet
            if (step > 1) showToAll(CardsShown(drafts.map { it.shown }))
        }
        drafts = withSelected(drafts, catchCards(hands, passing.catch))
        showToAll(DraftsRevealed(drafts))
        return drafts
    }

    /** Rule 4: seven cards per seat, taken from the deck in order. The cards after them are not used. */
    private fun deal(deck: List<Card>): List<List<Card>> {
        val hands = mutableListOf<List<Card>>()
        for (seat in players.indices) {
            val first = seat * Game.HAND_SIZE
            // toList makes a list of its own: a hand is sent to a player, and a view into the deck cannot be
            hands += deck.subList(first, first + Game.HAND_SIZE).toList()
        }
        return hands
    }

    /** One step of rules 5 and 7. Every player is asked before any answer is read, so nobody can see
     *  another player's selection before making their own. Returns the selected card of each seat. */
    private fun selectCards(hands: List<List<Card>>, drafts: List<Draft>): List<Card> {
        val answers = mutableListOf<IAnswer<Char>>()
        for (seat in players.indices) {
            answers += players[seat].selectCard(CardRequest(hands[seat], drafts[seat]))
        }
        val selected = mutableListOf<Card>()
        for (seat in players.indices) {
            selected += awaitCard(seat, answers[seat], hands[seat], drafts[seat])
        }
        return selected
    }

    /** Waits for the answer of one player. An answer that is no card of the hand changes nothing: the
     *  same player is asked again and told what was refused (S1). */
    private fun awaitCard(seat: Int, answer: IAnswer<Char>, hand: List<Card>, draft: Draft): Card {
        var site = answer.await()
        var card = cardWithSite(hand, site)
        while (card == null) {
            site = players[seat].selectCard(CardRequest(hand, draft, refused = site)).await()
            card = cardWithSite(hand, site)
        }
        return card
    }

    /** The card of the hand with this site character, in upper or lower case; null if there is none. */
    private fun cardWithSite(hand: List<Card>, site: Char): Card? =
        hand.firstOrNull { it.site.equals(site, ignoreCase = true) }

    /** Every hand without the card its holder selected. */
    private fun withoutSelected(hands: List<List<Card>>, selected: List<Card>): List<List<Card>> =
        players.indices.map { seat -> hands[seat] - selected[seat] }

    /** Every draft with one more card at its end: the card of the same seat. */
    private fun withSelected(drafts: List<Draft>, cards: List<Card>): List<Draft> =
        players.indices.map { seat -> drafts[seat] + cards[seat] }

    /** Rule 6: what each seat holds goes to the seat the direction names. Returns the hands by receiver. */
    private fun pass(hands: List<List<Card>>, direction: Direction): List<List<Card>> {
        val received = MutableList(players.size) { emptyList<Card>() }
        for (seat in players.indices) {
            received[direction.receiver(seat, players.size)] = hands[seat]
        }
        return received
    }

    /** Rule 9: every hand is down to one card, and it travels like a hand. Returns the Catch card of each seat. */
    private fun catchCards(hands: List<List<Card>>, direction: Direction): List<Card> =
        pass(hands, direction).map { it.single() }

    /** Tells every player the same thing. */
    private fun showToAll(event: GameEvent) {
        for (player in players) player.show(event)
    }
}
