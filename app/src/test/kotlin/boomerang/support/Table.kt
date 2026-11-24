package boomerang.support

import boomerang.game.Card
import boomerang.game.CardsShown
import boomerang.game.Draft
import boomerang.game.DraftsRevealed
import boomerang.game.GameEvent
import boomerang.game.Symbol

/*
 * The card table of requirements section 2, typed in a second time for the tests. It is the expected value
 * of the R2 tests, and it gives the tests of the draft and of the scoring cards that do not depend on the
 * production card list. The kinds are spelled as in AC2.5: Collection, Animal, Activity.
 */

val LEAVES = Symbol("Collection", "Leaves")
val WILDFLOWERS = Symbol("Collection", "Wildflowers")
val SHELLS = Symbol("Collection", "Shells")
val SOUVENIRS = Symbol("Collection", "Souvenirs")
val KANGAROOS = Symbol("Animal", "Kangaroos")
val EMUS = Symbol("Animal", "Emus")
val WOMBATS = Symbol("Animal", "Wombats")
val KOALAS = Symbol("Animal", "Koalas")
val PLATYPUSES = Symbol("Animal", "Platypuses")
val INDIGENOUS_CULTURE = Symbol("Activity", "Indigenous Culture")
val SIGHTSEEING = Symbol("Activity", "Sightseeing")
val BUSHWALKING = Symbol("Activity", "Bushwalking")
val SWIMMING = Symbol("Activity", "Swimming")

private const val WA = "Western Australia"
private const val NT = "Northern Territory"
private const val QLD = "Queensland"
private const val SA = "South Australia"
private const val NSW = "New South Wales"
private const val VIC = "Victoria"
private const val TAS = "Tasmania"

val TABLE: List<Card> = listOf(
    Card('A', "The Bungle Bungles", WA, 1, listOf(LEAVES, INDIGENOUS_CULTURE)),
    Card('B', "The Pinnacles", WA, 1, listOf(KANGAROOS, SIGHTSEEING)),
    Card('C', "Margaret River", WA, 1, listOf(SHELLS, KANGAROOS)),
    Card('D', "Kalbarri National Park", WA, 1, listOf(WILDFLOWERS, BUSHWALKING)),
    Card('E', "Uluru", NT, 4, listOf(EMUS, INDIGENOUS_CULTURE)),
    Card('F', "Kakadu National Park", NT, 4, listOf(WOMBATS, SIGHTSEEING)),
    Card('G', "Nitmiluk National Park", NT, 4, listOf(SHELLS, PLATYPUSES)),
    Card('H', "King's Canyon", NT, 4, listOf(KOALAS, SWIMMING)),
    Card('I', "The Great Barrier Reef", QLD, 6, listOf(WILDFLOWERS, SIGHTSEEING)),
    Card('J', "The Whitsundays", QLD, 6, listOf(KANGAROOS, INDIGENOUS_CULTURE)),
    Card('K', "Daintree Rainforest", QLD, 6, listOf(SOUVENIRS, BUSHWALKING)),
    Card('L', "Surfers Paradise", QLD, 6, listOf(WILDFLOWERS, SWIMMING)),
    Card('M', "Barossa Valley", SA, 3, listOf(KOALAS, BUSHWALKING)),
    Card('N', "Lake Eyre", SA, 3, listOf(EMUS, SWIMMING)),
    Card('O', "Kangaroo Island", SA, 3, listOf(KANGAROOS, BUSHWALKING)),
    Card('P', "Mount Gambier", SA, 3, listOf(WILDFLOWERS, SIGHTSEEING)),
    Card('Q', "Blue Mountains", NSW, 5, listOf(WOMBATS, INDIGENOUS_CULTURE)),
    Card('R', "Sydney Harbour", NSW, 5, listOf(EMUS, SIGHTSEEING)),
    Card('S', "Bondi Beach", NSW, 5, listOf(WOMBATS, SWIMMING)),
    Card('T', "Hunter Valley", NSW, 5, listOf(EMUS, BUSHWALKING)),
    Card('U', "Melbourne", VIC, 2, listOf(WOMBATS, BUSHWALKING)),
    Card('V', "The MCG", VIC, 2, listOf(LEAVES, INDIGENOUS_CULTURE)),
    Card('W', "Twelve Apostles", VIC, 2, listOf(SHELLS, SWIMMING)),
    Card('X', "Royal Exhibition Building", VIC, 2, listOf(LEAVES, PLATYPUSES)),
    Card('Y', "Salamanca Markets", TAS, 7, listOf(LEAVES, EMUS)),
    Card('Z', "Mount Wellington", TAS, 7, listOf(KOALAS, SIGHTSEEING)),
    Card('*', "Port Arthur", TAS, 7, listOf(LEAVES, INDIGENOUS_CULTURE)),
    Card('-', "Richmond", TAS, 7, listOf(KANGAROOS, SWIMMING)),
)

const val ALL_SITES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*-"

fun card(site: Char): Card = TABLE.first { it.site == site }

/** The cards with these site characters, in the order written. */
fun cards(sites: String): List<Card> = sites.map(::card)

fun draft(sites: String): Draft = Draft(cards(sites))

/** The site characters of cards, as a string: easier to read in a failure message than a list of cards. */
fun List<Card>.sites(): String = map { it.site }.joinToString("")

/** Every card an event lets its receiver see. */
fun GameEvent.cards(): List<Card> = when (this) {
    is CardsShown -> shown.flatten()
    is DraftsRevealed -> drafts.flatMap { it.cards }
    else -> emptyList()
}

/**
 * The deck that leads to the planned drafts under the standard passing (hands to the next player, Catch
 * card to the previous), when every player selects the first six cards of the plan in order; the seventh
 * card of a plan arrives as the Catch card. plans[seat] is seven site characters. The cards no plan names
 * follow at the end, so the deck is always the complete 28.
 *
 * At step s (1 to 6) seat i holds the hand first dealt to seat i-(s-1). The Catch card of seat i is what
 * seat i+1 has left after step 6, which is from the hand first dealt to seat i-4. All counted modulo n.
 */
fun deckFor(plans: List<String>): List<Card> {
    val n = plans.size
    val hands = List(n) { mutableListOf<Card>() }
    for (seat in 0 until n) for (k in 0 until 7) {
        val dealtTo = if (k < 6) Math.floorMod(seat - k, n) else Math.floorMod(seat - 4, n)
        hands[dealtTo] += card(plans[seat][k])
    }
    val dealt = hands.flatten()
    check(dealt.toSet().size == 7 * n) { "a card is planned twice: $plans" }
    return dealt + TABLE.filterNot { it in dealt }
}
