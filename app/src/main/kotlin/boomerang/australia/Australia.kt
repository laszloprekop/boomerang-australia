package boomerang.australia

import boomerang.game.Card
import boomerang.game.Edition
import boomerang.game.Symbol
import boomerang.scoring.ChosenSymbolCount
import boomerang.scoring.NewSites
import boomerang.scoring.Region
import boomerang.scoring.RegionBonus
import boomerang.scoring.SymbolPairs
import boomerang.scoring.SymbolSum
import boomerang.scoring.ThrowCatchDifference

/** The symbols of the edition, each declared once (LEAVES, KANGAROOS, SWIMMING and so on). */
object AustraliaSymbols {
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
}

private const val WESTERN_AUSTRALIA = "Western Australia"
private const val NORTHERN_TERRITORY = "Northern Territory"
private const val QUEENSLAND = "Queensland"
private const val SOUTH_AUSTRALIA = "South Australia"
private const val NEW_SOUTH_WALES = "New South Wales"
private const val VICTORIA = "Victoria"
private const val TASMANIA = "Tasmania"

/** Where the regions lie on the map of the score card, row by row from north to south and from west to
 *  east. Western Australia is as high as two rows; an empty name is the sea. For a console that draws
 *  the map as a board (console-screens D13); no rule reads it. */
val AUSTRALIA_MAP: List<List<String>> = listOf(
    listOf(WESTERN_AUSTRALIA, NORTHERN_TERRITORY, QUEENSLAND),
    listOf(WESTERN_AUSTRALIA, SOUTH_AUSTRALIA, NEW_SOUTH_WALES),
    listOf("", TASMANIA, VICTORIA),
)

/** The name of the edition in large letters beside the kangaroo warning sign of Australian roads, for a
 *  console that greets with a logo (console-screens D14). The sign is made of braille characters, each a
 *  cell of two by four dots: the dots are the face of the sign, the kangaroo is where there are none.
 *  Cells without a dot are spaces, which every font leaves empty.
 *  No rule reads it. */
val AUSTRALIA_LOGO: List<String> = listOf(
    "      ⢀⣴⣦⡀",
    "    ⢀⣴⣿⣿⣿⣿⣦⡀       ████   ███   ███  █   █ █████ ████   ███  █   █  ████",
    "  ⢀⣴⣿⣿⠟⠉⠉⠻⣿⢿⣦⡀     █   █ █   █ █   █ ██ ██ █     █   █ █   █ ██  █ █",
    "⢀⣴⢿⡿⠟⣁⡀   ⠈⣠⣬⣿⣦⡀   ████  █   █ █   █ █ █ █ ████  ████  █████ █ █ █ █  ██",
    "⠈⠻⣷⣶⣿⣿⣿⣶ ⣧⣾⣿⣿⣿⠟⠁   █   █ █   █ █   █ █   █ █     █  █  █   █ █  ██ █   █",
    "  ⠈⠻⣿⣿⣿⣿⣧⣭⣽⣻⠟⠁     ████   ███   ███  █   █ █████ █   █ █   █ █   █  ████",
    "    ⠈⠻⣿⣿⣿⣿⠟⠁       A  U  S  T  R  A  L  I  A",
    "      ⠈⠻⠟⠁",
)

/** Points for completing a region first (R10b). */
private const val REGION_BONUS = 3

/** The 28 cards of requirements section 2, in table order.
 *  `with(AustraliaSymbols)` lets the block write LEAVES for AustraliaSymbols.LEAVES. */
val australiaCards: List<Card> = with(AustraliaSymbols) {
    listOf(
        Card('A', "The Bungle Bungles", WESTERN_AUSTRALIA, 1, listOf(LEAVES, INDIGENOUS_CULTURE)),
        Card('B', "The Pinnacles", WESTERN_AUSTRALIA, 1, listOf(KANGAROOS, SIGHTSEEING)),
        Card('C', "Margaret River", WESTERN_AUSTRALIA, 1, listOf(SHELLS, KANGAROOS)),
        Card('D', "Kalbarri National Park", WESTERN_AUSTRALIA, 1, listOf(WILDFLOWERS, BUSHWALKING)),
        Card('E', "Uluru", NORTHERN_TERRITORY, 4, listOf(EMUS, INDIGENOUS_CULTURE)),
        Card('F', "Kakadu National Park", NORTHERN_TERRITORY, 4, listOf(WOMBATS, SIGHTSEEING)),
        Card('G', "Nitmiluk National Park", NORTHERN_TERRITORY, 4, listOf(SHELLS, PLATYPUSES)),
        Card('H', "King's Canyon", NORTHERN_TERRITORY, 4, listOf(KOALAS, SWIMMING)),
        Card('I', "The Great Barrier Reef", QUEENSLAND, 6, listOf(WILDFLOWERS, SIGHTSEEING)),
        Card('J', "The Whitsundays", QUEENSLAND, 6, listOf(KANGAROOS, INDIGENOUS_CULTURE)),
        Card('K', "Daintree Rainforest", QUEENSLAND, 6, listOf(SOUVENIRS, BUSHWALKING)),
        Card('L', "Surfers Paradise", QUEENSLAND, 6, listOf(WILDFLOWERS, SWIMMING)),
        Card('M', "Barossa Valley", SOUTH_AUSTRALIA, 3, listOf(KOALAS, BUSHWALKING)),
        Card('N', "Lake Eyre", SOUTH_AUSTRALIA, 3, listOf(EMUS, SWIMMING)),
        Card('O', "Kangaroo Island", SOUTH_AUSTRALIA, 3, listOf(KANGAROOS, BUSHWALKING)),
        Card('P', "Mount Gambier", SOUTH_AUSTRALIA, 3, listOf(WILDFLOWERS, SIGHTSEEING)),
        Card('Q', "Blue Mountains", NEW_SOUTH_WALES, 5, listOf(WOMBATS, INDIGENOUS_CULTURE)),
        Card('R', "Sydney Harbour", NEW_SOUTH_WALES, 5, listOf(EMUS, SIGHTSEEING)),
        Card('S', "Bondi Beach", NEW_SOUTH_WALES, 5, listOf(WOMBATS, SWIMMING)),
        Card('T', "Hunter Valley", NEW_SOUTH_WALES, 5, listOf(EMUS, BUSHWALKING)),
        Card('U', "Melbourne", VICTORIA, 2, listOf(WOMBATS, BUSHWALKING)),
        Card('V', "The MCG", VICTORIA, 2, listOf(LEAVES, INDIGENOUS_CULTURE)),
        Card('W', "Twelve Apostles", VICTORIA, 2, listOf(SHELLS, SWIMMING)),
        Card('X', "Royal Exhibition Building", VICTORIA, 2, listOf(LEAVES, PLATYPUSES)),
        Card('Y', "Salamanca Markets", TASMANIA, 7, listOf(LEAVES, EMUS)),
        Card('Z', "Mount Wellington", TASMANIA, 7, listOf(KOALAS, SIGHTSEEING)),
        Card('*', "Port Arthur", TASMANIA, 7, listOf(LEAVES, INDIGENOUS_CULTURE)),
        Card('-', "Richmond", TASMANIA, 7, listOf(KANGAROOS, SWIMMING)),
    )
}

/** Boomerang Australia: the cards; six categories (Throw and catch, Tourist sites, Region bonus, Collections,
 *  Animals, Activities); tie-break Throw and catch. Regions are derived from the cards, bonus 3 each.
 *  Activities in the order of brief rule 2: Swimming, Bushwalking, Indigenous Culture, Sightseeing. */
fun australia(): Edition = with(AustraliaSymbols) {
    val throwAndCatch = ThrowCatchDifference("Throw and catch")
    val categories = listOf(
        throwAndCatch,
        NewSites("Tourist sites"),
        RegionBonus("Region bonus", regions()),
        SymbolSum("Collections", mapOf(LEAVES to 1, WILDFLOWERS to 2, SHELLS to 3, SOUVENIRS to 5), ::collectionPoints),
        SymbolPairs("Animals", mapOf(KANGAROOS to 3, EMUS to 4, WOMBATS to 5, KOALAS to 7, PLATYPUSES to 9)),
        // the place in the list is the number of cards: 0 and 1 card give 0 points, 6 cards give 15
        ChosenSymbolCount(
            "Activities", listOf(SWIMMING, BUSHWALKING, INDIGENOUS_CULTURE, SIGHTSEEING), listOf(0, 0, 2, 4, 7, 10, 15),
        ),
    )
    Edition("Boomerang Australia", australiaCards, categories, tieBreakers = listOf(throwAndCatch))
}

/** One region per region name on the cards, with the sites of the cards that carry the name. */
private fun regions(): List<Region> =
    australiaCards.groupBy { it.region }.map { (name, cards) -> Region(name, cards.map { it.site }.toSet(), REGION_BONUS) }

/** R10c: a sum of 1 to 7 scores double, a sum above 7 scores the sum. */
private fun collectionPoints(sum: Int): Int = if (sum <= 7) sum * 2 else sum
