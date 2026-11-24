import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

/*
 * Helpers for testing the original BoomerangAustralia.java without changing it.
 *
 * Three ways in, from the least to the most intrusive:
 *
 * 1. onConsole: runs code with System.in and System.out replaced, so the original's console player
 *    (Scanner on System.in, line 34; println, line 56) reads scripted answers and nothing waits on the
 *    keyboard.
 * 2. solo: one Player object built directly, its draft filled by the test, roundScore called directly.
 *    Reaches lines 140 to 277 (all scoring of one player) and 68 to 114.
 * 3. playRigged: a whole game through initGame (lines 293 to 424), using a subclass that replaces
 *    server() (the only place players are created, called on line 328 AFTER the first shuffle on line
 *    327) and the two I/O methods of Player. No line of the original is edited; the overridden methods
 *    are public and not final.
 */

/** One row of the card table in the exam brief (lines 86 to 114), spelled as in the brief. */
data class BriefCard(
    val letter: String, val name: String, val region: String, val number: Int,
    val collection: String, val animal: String, val activity: String,
)

val BRIEF_CARDS = listOf(
    BriefCard("A", "The Bungle Bungles", "Western Australia", 1, "Leaves", "", "Indigenous Culture"),
    BriefCard("B", "The Pinnacles", "Western Australia", 1, "", "Kangaroos", "Sightseeing"),
    BriefCard("C", "Margaret River", "Western Australia", 1, "Shells", "Kangaroos", ""),
    BriefCard("D", "Kalbarri National Park", "Western Australia", 1, "Wildflowers", "", "Bushwalking"),
    BriefCard("E", "Uluru", "Northern Territory", 4, "", "Emus", "Indigenous Culture"),
    BriefCard("F", "Kakadu National Park", "Northern Territory", 4, "", "Wombats", "Sightseeing"),
    BriefCard("G", "Nitmiluk National Park", "Northern Territory", 4, "Shells", "Platypuses", ""),
    BriefCard("H", "King's Canyon", "Northern Territory", 4, "", "Koalas", "Swimming"),
    BriefCard("I", "The Great Barrier Reef", "Queensland", 6, "Wildflowers", "", "Sightseeing"),
    BriefCard("J", "The Whitsundays", "Queensland", 6, "", "Kangaroos", "Indigenous Culture"),
    BriefCard("K", "Daintree Rainforest", "Queensland", 6, "Souvenirs", "", "Bushwalking"),
    BriefCard("L", "Surfers Paradise", "Queensland", 6, "Wildflowers", "", "Swimming"),
    BriefCard("M", "Barossa Valley", "South Australia", 3, "", "Koalas", "Bushwalking"),
    BriefCard("N", "Lake Eyre", "South Australia", 3, "", "Emus", "Swimming"),
    BriefCard("O", "Kangaroo Island", "South Australia", 3, "", "Kangaroos", "Bushwalking"),
    BriefCard("P", "Mount Gambier", "South Australia", 3, "Wildflowers", "", "Sightseeing"),
    BriefCard("Q", "Blue Mountains", "New South Whales", 5, "", "Wombats", "Indigenous Culture"),
    BriefCard("R", "Sydney Harbour", "New South Whales", 5, "", "Emus", "Sightseeing"),
    BriefCard("S", "Bondi Beach", "New South Whales", 5, "", "Wombats", "Swimming"),
    BriefCard("T", "Hunter Valley", "New South Whales", 5, "", "Emus", "Bushwalking"),
    BriefCard("U", "Melbourne", "Victoria", 2, "", "Wombats", "Bushwalking"),
    BriefCard("V", "The MCG", "Victoria", 2, "Leaves", "", "Indigenous Culture"),
    BriefCard("W", "Twelve Apostles", "Victoria", 2, "Shells", "", "Swimming"),
    BriefCard("X", "Royal Exhibition Building", "Victoria", 2, "Leaves", "Platypuses", ""),
    BriefCard("Y", "Salamanca Markets", "Tasmania", 7, "Leaves", "Emus", ""),
    BriefCard("Z", "Mount Wellington", "Tasmania", 7, "", "Koalas", "Sightseeing"),
    BriefCard("*", "Port Arthur", "Tasmania", 7, "Leaves", "", "Indigenous Culture"),
    BriefCard("-", "Richmond", "Tasmania", 7, "", "Kangaroos", "Swimming"),
)

const val ALL_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*-"

/** The keys the original uses in its per-round score maps (lines 161, 172, 185, 212). */
const val THROW_CATCH = "Throw and Catch score"
const val SITES = "Tourist sites score"
const val COLLECTIONS = "Collections score"
const val ANIMALS = "Animals score"

class Captured<T>(val value: T, val output: String)

/**
 * Runs [block] with System.in and System.out replaced. Whenever the code under test reads a line,
 * [reply] is given everything printed so far and returns the line to type, or null for end of input
 * (the original turns that into an empty answer, lines 64 to 65).
 */
fun <T> onConsole(reply: (transcript: String) -> String? = { null }, block: () -> T): Captured<T> {
    val realIn = System.`in`
    val realOut = System.out
    val printed = ByteArrayOutputStream()
    val keyboard = object : InputStream() {
        private var line = ByteArray(0)
        private var at = 0

        override fun read(): Int {
            if (at == line.size) {
                val next = reply(printed.toString()) ?: return -1
                line = (next + "\n").toByteArray()
                at = 0
            }
            return line[at++].toInt() and 0xff
        }

        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            val first = read()
            if (first < 0) return -1
            target[offset] = first.toByte()
            var count = 1
            while (count < length && at < line.size) target[offset + count++] = line[at++]
            return count
        }

        override fun available(): Int = line.size - at
    }
    System.setIn(keyboard)
    System.setOut(PrintStream(printed, true))
    try {
        val value = block()
        System.out.flush()
        return Captured(value, printed.toString())
    } finally {
        System.setIn(realIn)
        System.setOut(realOut)
    }
}

internal fun letters(cards: List<BoomerangAustralia.Card>): String = cards.joinToString("") { it.letter }

// ---------------------------------------------------------------------------------------------------
// 2. One player, scored directly
// ---------------------------------------------------------------------------------------------------

/** A game object that plays nothing (no arguments, lines 286 to 290) and one console player of it. */
internal class Solo(val game: BoomerangAustralia, val player: BoomerangAustralia.Player) {

    fun card(letter: Char): BoomerangAustralia.Card {
        val row = BRIEF_CARDS.first { it.letter == letter.toString() }
        return game.Card(row.name, row.letter, row.region, row.number, row.collection, row.animal, row.activity)
    }

    fun cards(letters: String): List<BoomerangAustralia.Card> = letters.map { card(it) }

    /** Makes [draft] (Throw first, Catch last) the player's seven cards and scores round [roundNr] (0 to 3). */
    fun score(roundNr: Int, draft: String): Int {
        require(draft.length == 7 && draft.toSet().size == 7) { "a draft is seven different cards: $draft" }
        player.draft.clear()
        player.draft.addAll(cards(draft))
        return player.roundScore(roundNr)
    }

    /** What the original stored for a category in a round. */
    fun category(roundNr: Int, name: String): Int = player.rScore[roundNr].getValue(name)
}

/**
 * Builds a console player (or a bot) and runs [block] on it. A console player answers each
 * "Want to keep X(..)" question with Y when [keep] says so for activity X, otherwise N.
 */
internal fun <T> solo(isBot: Boolean = false, keep: (activity: String) -> Boolean = { false }, block: Solo.() -> T): Captured<T> =
    onConsole(reply = { transcript ->
        val question = transcript.substringAfterLast("Want to keep ", "")
        if (question.isNotEmpty() && keep(question.substringBefore("("))) "Y" else "N"
    }) {
        val game = BoomerangAustralia(arrayOf<String>())
        Solo(game, game.Player(0, isBot, null, null, null)).block()
    }

// ---------------------------------------------------------------------------------------------------
// 3. A whole game with a fixed deal
// ---------------------------------------------------------------------------------------------------

const val STOPPED = "stopped by the test"

class StopGame : RuntimeException(STOPPED)

/**
 * A game of [seatCount] scripted players played by the original's own initGame.
 *
 * [plan] gives, per round and per seat, the seven cards the seat is to end up with, as letters: the
 * Throw card first, then the five cards selected in order, the Catch card last. Rounds without a plan
 * are played with whatever the original deals, each seat taking the first card of its hand.
 *
 * How the deal is fixed:
 * - Round 1: server() runs between the shuffle (line 327) and the deal (lines 330 to 332), so it
 *   reorders the public deck.
 * - Rounds 2 to 4: the original shuffles and deals with nothing in between (lines 403 to 410), so the
 *   hands are replaced at the first message of the new round, under a lock, before any seat has looked
 *   at its hand (the first thing addCardToDraft does is send a message, line 70).
 *
 * [typed] can replace what a seat types at a step. [keep] answers the activity questions (default: N).
 * The game is stopped after the round with index [lastRound] by throwing from the last message of that
 * round (line 400), which the original catches on line 421; with lastRound 3 it runs to its own end.
 */
internal class RiggedGame(
    val seatCount: Int,
    val plan: List<List<String>>,
    val lastRound: Int,
    val typed: (round: Int, seat: Int, step: Int, planned: String) -> String,
    val keep: (round: Int, seat: Int, activity: String) -> Boolean,
) : BoomerangAustralia(arrayOf<String>()) {

    private val lock = Any()
    val seats = mutableListOf<Seat>()

    /** The deck as the original shuffled it for round 1, before the test touched it. */
    var shuffled = ""

    /** dealt[round][seat]: the hands as the original dealt them (round 1: from the reordered deck). */
    val dealt = mutableListOf<List<String>>()

    /** Exceptions thrown by this harness itself. Must stay empty: the original swallows them otherwise. */
    val harnessErrors = mutableListOf<String>()

    /** What the original printed on the console: empty, or the message of the exception that ended the game (line 422). */
    var ending = ""

    inner class Seat(id: Int) : BoomerangAustralia.Player(id, false, null, null, null) {
        val inbox = mutableListOf<String>()

        /** handsSeen[round][step]: the hand this seat held when asked for a card. */
        val handsSeen = mutableListOf<MutableList<String>>()

        /** The seven cards per round, read when the original reports the draft (line 398). */
        val drafts = mutableListOf<String>()
        var round = 0
        var step = 0

        override fun sendMessage(message: Any?) {
            synchronized(lock) {
                val text = message.toString()
                var roundOver = false
                guarded {
                    inbox.add(text)
                    when {
                        text == "You haven't drafted any cards yet" && step == 0 -> startRound(round)
                        text.startsWith("********************************\nYour draft this round") ->
                            drafts.add(letters(draft))
                        text.startsWith("The following regions have now been completed") -> {
                            round++
                            step = 0
                            roundOver = true
                        }
                    }
                }
                if (roundOver && playerID == seatCount - 1 && round > lastRound && lastRound < 3) throw StopGame()
            }
        }

        override fun readMessage(): String = synchronized(lock) {
            guarded {
                val prompt = inbox.last()
                if (prompt.startsWith("Want to keep ")) {
                    val activity = prompt.removePrefix("Want to keep ").substringBefore("(")
                    if (keep(round, playerID, activity)) "Y" else "N"
                } else {
                    check(prompt == "Type the letter of the card to draft") { "unexpected prompt: $prompt" }
                    while (handsSeen.size <= round) handsSeen.add(mutableListOf())
                    handsSeen[round].add(letters(hand))
                    val planned = if (round < plan.size) plan[round][playerID][step].toString() else hand[0].letter
                    typed(round, playerID, step++, planned)
                }
            }
        }

        /** Messages to this seat about the cards of seat [subject] (line 374). */
        fun toldAbout(subject: Int): List<String> = inbox.filter { it.startsWith("\nPlayer $subject has drafted") }

        /** The region bonus the original reported to this seat per round (line 171). */
        fun regionPointsTold(): List<Int> = inbox.mapNotNull {
            Regex("new sites points and (\\d+) points for completing regions").find(it)?.groupValues?.get(1)?.toInt()
        }
    }

    private fun <T> guarded(body: () -> T): T =
        try {
            body()
        } catch (e: Throwable) {
            harnessErrors.add(e.toString())
            throw e
        }

    private fun startRound(round: Int) {
        if (dealt.size != round) return
        if (round in 1 until plan.size) {
            for (seat in seats) {
                seat.hand.clear()
                seat.hand.addAll(handFor(round, seat.playerID))
            }
        }
        dealt.add(seats.map { letters(it.hand) })
    }

    /**
     * The hand to deal to seat [h] so that every seat ends with its planned draft: the hand is held at
     * step s by seat (h+s) mod n, who takes one card; the card left after step 5 goes to the previous
     * seat, (h+4) mod n, as its Catch card (rules 6 and 9).
     */
    private fun handFor(round: Int, h: Int): List<Card> {
        val n = seatCount
        val wanted = (0..5).map { s -> plan[round][(h + s) % n][s] } + plan[round][(h + 4) % n][6]
        return wanted.map { letter -> arrayDeck.first { it.letter == letter.toString() } }
    }

    override fun server(numberPlayers: Int, numberOfBots: Int) {
        shuffled = letters(deck)
        repeat(seatCount) {
            val seat = Seat(it)
            seats.add(seat)
            players.add(seat)
        }
        if (plan.isNotEmpty()) {
            val top = (0 until seatCount).flatMap { handFor(0, it) }
            val rest = deck.filter { card -> top.none { it.letter == card.letter } }
            deck.clear()
            deck.addAll(top + rest)
        }
    }

    /** Fails unless the game went the way the test set it up: a guard against a broken harness. */
    fun assertRanAsPlanned() {
        assertEquals(emptyList<String>(), harnessErrors, "harness: exceptions in the scripted seats")
        assertEquals(if (lastRound < 3) STOPPED else "", ending, "harness: how the game ended")
        for (round in plan.indices) {
            if (round > lastRound) break
            for (seat in seats) {
                assertEquals(plan[round][seat.playerID], seat.drafts.getOrNull(round),
                    "harness: draft of seat ${seat.playerID} in round index $round")
            }
        }
    }
}

internal fun playRigged(
    seatCount: Int,
    plan: List<List<String>> = emptyList(),
    lastRound: Int = 0,
    typed: (round: Int, seat: Int, step: Int, planned: String) -> String = { _, _, _, planned -> planned },
    keep: (round: Int, seat: Int, activity: String) -> Boolean = { _, _, _ -> false },
): RiggedGame {
    for (round in plan) {
        require(round.size == seatCount && round.all { it.length == 7 }) { "one draft of seven per seat" }
        require(round.joinToString("").toSet().size == 7 * seatCount) { "a card is planned twice: $round" }
    }
    lateinit var game: RiggedGame
    val run = onConsole {
        game = RiggedGame(seatCount, plan, lastRound, typed, keep)
        game.initGame(seatCount, 0)
    }
    // The constructor without arguments prints three lines of usage (lines 287 to 289) before the game.
    game.ending = run.output.substringAfter("Client syntax: IP").trim()
    return game
}
