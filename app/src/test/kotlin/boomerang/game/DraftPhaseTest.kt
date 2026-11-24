package boomerang.game

import boomerang.game.Direction.TO_NEXT
import boomerang.game.Direction.TO_PREVIOUS
import boomerang.support.Answered
import boomerang.support.Asked
import boomerang.support.Entry
import boomerang.support.ScriptedPlayer
import boomerang.support.TABLE
import boomerang.support.Told
import boomerang.support.card
import boomerang.support.cards
import boomerang.support.deckFor
import boomerang.support.draft
import boomerang.support.sites
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Rules 4 to 9 on DraftPhase: a fixed deck, scripted players, the standard passing. No console, no socket,
 * no thread, no shuffle. Unless a test says otherwise the deck is the card table in its own order (seat 0
 * is dealt A to G, seat 1 H to N, seat 2 O to U, seat 3 V to -) and every player selects the first card of
 * the hand.
 */
class DraftPhaseTest {
    private val standard = Passing(hands = TO_NEXT, catch = TO_PREVIOUS)

    /** One round of the draft, played when the object is made. */
    private inner class Round(val n: Int, scripts: Map<Int, String> = emptyMap(), val deck: List<Card> = TABLE) {
        val journal = mutableListOf<Entry>()
        val players = List(n) { ScriptedPlayer(it, cards = scripts[it] ?: "", journal = journal) }
        val drafts: List<Draft> = DraftPhase(players).run(deck, standard)
        val dealt: List<Card> get() = deck.take(7 * n)
    }

    /** A round in which every player ends with the planned draft (seven site characters per seat). */
    private fun planned(vararg plans: String): Round {
        val round = Round(plans.size, plans.withIndex().associate { (seat, plan) -> seat to plan.take(6) }, deckFor(plans.toList()))
        assertEquals(plans.toList(), round.drafts.map { it.cards.sites() }, "precondition: the drafts are the planned ones")
        return round
    }

    private val threeSeats = arrayOf("EFGHAIM", "QRUVYZB", "CDJKNOS")

    // --- Rule 4 -------------------------------------------------------------------------------------

    @Test
    fun `R4 AC4_1 AC4_2 each of 2, 3 and 4 players is dealt 7 cards, seat by seat from the deck, and no card twice`() {
        for (n in 2..4) {
            val round = Round(n)

            val hands = round.players.map { it.cardRequests.first().hand }
            assertEquals(List(n) { 7 }, hands.map { it.size }, "hand sizes with $n players")
            assertEquals(7 * n, hands.flatten().toSet().size, "different cards dealt with $n players")
            for (seat in 0 until n) {
                assertEquals(TABLE.subList(7 * seat, 7 * seat + 7).toSet(), hands[seat].toSet(), "$n players, hand of seat $seat")
            }
        }
    }

    @Test
    fun `R4 AC4_3 with 2, 3 and 4 players 14, 7 and 0 cards stay undealt, take no part and are shown to nobody`() {
        for (n in 2..4) {
            val round = Round(n)
            val undealt = TABLE.drop(7 * n).toSet()
            assertEquals(28 - 7 * n, undealt.size)

            for (player in round.players) {
                val seen = player.cardRequests.flatMap { it.hand + it.draft.cards } + player.events.flatMap { it.cards() }
                assertEquals(emptySet<Card>(), seen.toSet() intersect undealt, "$n players: undealt cards seen by seat ${player.seat}")
            }
            assertEquals(emptySet<Card>(), round.drafts.flatMap { it.cards }.toSet() intersect undealt, "$n players: undealt cards drafted")
        }
    }

    // --- Rule 5 -------------------------------------------------------------------------------------

    @Test
    fun `R5 AC5_1 S1 a Throw card that is not in the player's hand is refused and the same player is asked again`() {
        // seat 0 holds A to G and first answers N, a card of seat 1's hand, then selects C
        val round = Round(2, mapOf(0 to "NC"))
        val (first, second) = round.players

        val (asked, askedAgain) = first.cardRequests
        assertNull(asked.refused)
        assertEquals('N', askedAgain.refused)
        assertEquals(asked.hand, askedAgain.hand, "the refusal changes nothing in the hand")
        assertEquals(Draft(), askedAgain.draft, "the refusal changes nothing in the draft")
        assertEquals(card('C'), round.drafts[0].cards.first(), "the Throw card is the allowed answer")
        assertEquals(6, second.cardRequests.count { it.refused == null }, "the other player is asked once per step")
        assertTrue(second.cardRequests.none { it.refused != null }, "only the player who answered wrongly is asked again")
    }

    @Test
    fun `R5 AC5_2 after the selection the hand has 6 cards and the Throw card is the first card of the draft`() {
        for (n in 2..4) {
            val round = Round(n)

            for (player in round.players) {
                val thrown = player.steps[0].hand.first() // the script: the first card of the hand
                val next = round.players[(player.seat + 1) % n]
                assertEquals(listOf(thrown), player.steps[1].draft.cards, "$n players, draft of seat ${player.seat} after step 1")
                assertEquals(thrown, round.drafts[player.seat].cards.first(), "$n players, final draft of seat ${player.seat}")
                assertEquals(6, next.steps[1].hand.size, "$n players, the hand seat ${player.seat} passed on")
                assertFalse(thrown in next.steps[1].hand, "$n players, the Throw card of seat ${player.seat} was passed on")
            }
        }
    }

    @Test
    fun `R5 AC5_3 nothing asked of or shown to another player before the round ends identifies a Throw card`() {
        val round = planned(*threeSeats)

        // control: the second card of seat 0 (F) did reach seat 1, so the search below can find a card
        assertTrue(round.players[1].eventsOf<CardsShown>().any { card('F') in it.cards() })
        for (owner in round.players) {
            val thrown = card(threeSeats[owner.seat][0])
            for (other in round.players - owner) {
                val asked = other.cardRequests.flatMap { it.hand + it.draft.cards }
                val shown = other.eventsOf<CardsShown>().flatMap { it.cards() }
                assertFalse(thrown in asked, "seat ${other.seat} was asked with ${thrown.site}, the Throw card of seat ${owner.seat}")
                assertFalse(thrown in shown, "seat ${other.seat} was shown ${thrown.site}, the Throw card of seat ${owner.seat}")
                assertEquals(DraftsRevealed::class, other.events.first { thrown in it.cards() }::class,
                    "the first event that names the Throw card of seat ${owner.seat} to seat ${other.seat}")
            }
        }
    }

    @Test
    fun `R5 AC5_4 a player sees the own Throw card in every later question`() {
        val round = planned(*threeSeats)

        for (player in round.players) {
            val thrown = card(threeSeats[player.seat][0])
            assertEquals(6, player.steps.size)
            for (request in player.steps.drop(1)) assertEquals(thrown, request.draft.cards.first(), "seat ${player.seat}")
        }
    }

    @Test
    fun `R5 AC5_5 R6 AC6_3 R7 AC7_4 all players are asked before any answer is read, and nothing is shown or passed before the last answer of the step`() {
        for (n in 2..4) {
            val round = Round(n)

            // A: a question put, R: an answer read, T: an event told. Step 1 shows nothing (only Throw cards
            // were selected); steps 2 to 6 end with the shown cards; the round ends with the complete drafts.
            val kinds = round.journal.joinToString("") { entry ->
                when (entry) { is Asked -> "A"; is Answered -> "R"; is Told -> "T" }
            }
            val ask = "A".repeat(n) + "R".repeat(n)
            assertEquals(ask + (ask + "T".repeat(n)).repeat(5) + "T".repeat(n), kinds, "$n players")
            // every block of n is one entry per seat
            for (block in round.journal.chunked(n)) assertEquals((0 until n).toSet(), block.map { it.seat }.toSet(), "$n players")
        }
    }

    @Test
    fun `R5 AC5_6 when the round ends every player's Throw card is shown to all players`() {
        val round = planned(*threeSeats)

        for (player in round.players) {
            val revealed = player.events.last()
            assertEquals(DraftsRevealed(round.drafts), revealed, "the last event of the round for seat ${player.seat}")
            assertEquals(cards("EQC"), (revealed as DraftsRevealed).drafts.map { it.cards.first() })
        }
        // the assertion of the legacy-bug test: seat 1 is told E, the Throw card of seat 0
        assertTrue(round.players[1].events.any { card('E') in it.cards() },
            "no event for seat 1 in the whole round names E, the Throw card of seat 0")
    }

    // --- Rule 6 -------------------------------------------------------------------------------------

    @Test
    fun `R6 AC6_1 after each selection the rest of the hand goes to the next player, the last player's to the first`() {
        for (n in 2..4) {
            val round = Round(n)

            for (giver in round.players) {
                val receiver = round.players[(giver.seat + 1) % n]
                for (step in 1..5) {
                    val passedOn = giver.steps[step - 1].hand - round.drafts[giver.seat].cards[step - 1]
                    assertEquals(8 - step - 1, passedOn.size)
                    assertEquals(passedOn.toSet(), receiver.steps[step].hand.toSet(),
                        "$n players, step $step: what seat ${receiver.seat} holds after the pass from seat ${giver.seat}")
                }
            }
        }
    }

    @Test
    fun `R6 AC6_2 a pass neither loses nor duplicates a card`() {
        for (n in 2..4) {
            val round = Round(n)

            for (step in 0..5) {
                val hands = round.players.flatMap { it.steps[step].hand }
                val drafts = round.players.flatMap { it.steps[step].draft.cards }
                assertEquals(n * (7 - step), hands.size, "$n players, cards in hands at step ${step + 1}")
                assertEquals(n * step, drafts.size, "$n players, cards in drafts at step ${step + 1}")
                assertEquals(7 * n, (hands + drafts).toSet().size, "$n players, different cards at step ${step + 1}")
                assertEquals(round.dealt.toSet(), (hands + drafts).toSet(), "$n players, step ${step + 1}")
            }
        }
    }

    // --- Rule 7 -------------------------------------------------------------------------------------

    @Test
    fun `R7 AC7_1 S1 an answer that is no card of the received hand is refused, nothing changes and the same player is asked again`() {
        // step 3: seat 0 answers '?' (no site), then A (its own Throw card, no longer in the hand), then C
        val round = Round(2, mapOf(0 to "AI?AC"))
        val first = round.players[0]

        val third = first.cardRequests.drop(2).take(3)
        assertEquals(listOf(null, '?', 'A'), third.map { it.refused })
        assertEquals(1, third.map { it.hand }.toSet().size, "the hand is the same in all three questions")
        assertEquals(1, third.map { it.draft }.toSet().size, "the draft is the same in all three questions")
        assertEquals(cards("AIC"), round.drafts[0].cards.take(3))
        assertEquals(8, first.cardRequests.size, "six steps and two repeated questions")
    }

    @Test
    fun `R7 legacy bug - a player who answers with something that is not in the hand still selects one card in that step`() {
        // seat 0 holds A to G and answers N; the original drafted nothing and passed on all seven cards
        val round = Round(2, mapOf(0 to "NA"))

        val selected = round.players[0].steps[1].draft.cards.size
        val passedOn = round.players[1].steps[1].hand.size
        assertEquals(1 to 6, selected to passedOn, "cards selected, cards passed on")
    }

    @Test
    fun `R7 legacy bug - one wrong answer must not end the round for everyone`() {
        val round = Round(2, mapOf(0 to "A?"))

        assertEquals('?', round.players[0].cardRequests[2].refused)
        assertEquals(listOf(7, 7), round.drafts.map { it.cards.size }, "cards in the drafts at the end of the round")
        assertTrue(round.players.all { it.events.last() is DraftsRevealed }, "the round ran to its end")
    }

    @Test
    fun `R7 S1 a site character is accepted in lower case`() {
        // seat 0 holds A to G; seat 1 holds H to N
        val round = Round(2, mapOf(0 to "c", 1 to "k"))

        assertEquals(cards("CK"), round.drafts.map { it.cards.first() })
        assertTrue(round.players.all { player -> player.cardRequests.none { it.refused != null } }, "nothing was refused")
    }

    @Test
    fun `R7 AC7_2 the selected card leaves the hand and is appended to the draft`() {
        val round = planned(*threeSeats)

        for (player in round.players) {
            val plan = threeSeats[player.seat]
            for (step in 0..5) {
                assertEquals(cards(plan.take(step)), player.steps[step].draft.cards, "seat ${player.seat}: the draft before step ${step + 1}")
            }
            // a card once selected is in no hand any more
            for (step in 1..5) for (anyone in round.players) {
                assertFalse(card(plan[step - 1]) in anyone.steps[step].hand, "${plan[step - 1]} is still in a hand at step ${step + 1}")
            }
        }
    }

    @Test
    fun `R7 AC7_3 after each step from the second, every player is shown every player's shown cards, the Throw cards not among them`() {
        val round = planned(*threeSeats)

        for (viewer in round.players) {
            val reports = viewer.eventsOf<CardsShown>()
            assertEquals(5, reports.size, "one report per step after the first, for seat ${viewer.seat}")
            for (step in 2..6) {
                // the report after step s names the cards selected in steps 2 to s, and no later one
                val expected = threeSeats.map { cards(it.substring(1, step)) }
                assertEquals(CardsShown(expected), reports[step - 2], "seat ${viewer.seat} after step $step")
            }
        }
    }

    // --- Rule 8 -------------------------------------------------------------------------------------

    @Test
    fun `R8 AC8_1 a player selects from hands of 7, 6, 5, 4, 3 and 2 cards`() {
        for (n in 2..4) {
            val round = Round(n)

            for (player in round.players) {
                assertEquals(listOf(7, 6, 5, 4, 3, 2), player.cardRequests.map { it.hand.size }, "$n players, seat ${player.seat}")
            }
        }
    }

    @Test
    fun `R8 AC8_2 after the last selection every player has a draft of the Throw card and 5 shown cards`() {
        for (n in 2..4) {
            val round = Round(n)

            for (player in round.players) {
                val last = player.eventsOf<CardsShown>().last()
                assertEquals(List(n) { 5 }, last.shown.map { it.size }, "$n players: shown cards after the last selection")
                assertEquals(5, player.steps.last().draft.cards.size, "$n players: the draft before the last selection")
            }
        }
    }

    @Test
    fun `R8 AC8_3 a round has exactly 5 passes to the next player`() {
        for (n in 2..4) {
            val round = Round(n)

            // at step s a player holds what is left of the hand dealt to the seat s-1 places before
            for (player in round.players) {
                assertEquals(6, player.steps.size)
                for (step in 0..5) {
                    val dealtTo = Math.floorMod(player.seat - step, n)
                    val original = TABLE.subList(7 * dealtTo, 7 * dealtTo + 7)
                    assertTrue(original.containsAll(player.steps[step].hand),
                        "$n players, seat ${player.seat}, step ${step + 1}: ${player.steps[step].hand.sites()} is not from the hand dealt to seat $dealtTo")
                }
            }
        }
    }

    // --- Rule 9 -------------------------------------------------------------------------------------

    @Test
    fun `R9 AC9_1 the last card of each hand goes to the previous player, the first player's to the last`() {
        for (n in 2..4) {
            val round = Round(n)

            for (giver in round.players) {
                val leftOver = giver.steps[5].hand - round.drafts[giver.seat].cards[5]
                val receiver = Math.floorMod(giver.seat - 1, n)
                assertEquals(1, leftOver.size)
                assertEquals(leftOver.single(), round.drafts[receiver].cards.last(),
                    "$n players: the Catch card of seat $receiver is the last card of seat ${giver.seat}")
            }
        }
    }

    @Test
    fun `R9 AC9_2 every player ends the round with 7 cards, the Throw card first and the Catch card last`() {
        val round = planned(*threeSeats)

        assertEquals(listOf(draft("EFGHAIM"), draft("QRUVYZB"), draft("CDJKNOS")), round.drafts)
        for (n in 2..4) assertEquals(List(n) { 7 }, Round(n).drafts.map { it.cards.size }, "$n players")
    }

    @Test
    fun `R9 AC9_3 the drafts of all players together are exactly the cards dealt in the round`() {
        for (n in 2..4) {
            val round = Round(n)

            val drafted = round.drafts.flatMap { it.cards }
            assertEquals(7 * n, drafted.size, "$n players")
            assertEquals(round.dealt.toSet(), drafted.toSet(), "$n players")
        }
    }

    @Test
    fun `R9 AC9_4 the Catch card is shown to all players and nobody is asked about it`() {
        val round = planned(*threeSeats)

        for (player in round.players) {
            assertEquals(6, player.cardRequests.size, "questions to seat ${player.seat}: none for the Catch card")
            val revealed = player.eventsOf<DraftsRevealed>().single()
            assertEquals(cards("MBS"), revealed.drafts.map { it.cards.last() }, "Catch cards told to seat ${player.seat}")
        }
        // the assertion of the legacy-bug test: seat 1 is told M, the Catch card of seat 0, by an event
        assertTrue(round.players[1].events.any { card('M') in it.cards() },
            "nothing told to seat 1, apart from its own hands, names M, the Catch card of seat 0")
    }
}
