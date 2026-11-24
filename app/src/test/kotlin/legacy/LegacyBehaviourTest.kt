import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

/**
 * Characterisation: what the original DOES where the rules are silent or a question is still open
 * (Q3, Q6, Q9 of docs/requirements-questions.md, and rows of section 7 of docs/requirements.md that
 * are not about rules 1 to 12). These tests PASS against BoomerangAustralia.java. They record
 * behaviour; they do not say the behaviour is wanted.
 */
@Timeout(60)
class LegacyBehaviourTest {

    // --- Section 7, row R1 ---------------------------------------------------------------------------

    @Test
    fun `R1 row - the server adds a console player of its own, so 0 2 is a game of three`() {
        assertEquals(3, onConsole { BoomerangAustralia(arrayOf("0", "2")) }.value.players.size)
    }

    @Test
    fun `R1 row - a negative count passes the check when the sum is 2 to 4, so -1 3 is a game of four`() {
        assertEquals(4, onConsole { BoomerangAustralia(arrayOf("-1", "3")) }.value.players.size)
    }

    @Test
    fun `R1 row - a game of five runs out of cards in the deal and ends with only an exception text`() {
        val run = onConsole { BoomerangAustralia(arrayOf("0", "4")) }

        assertEquals(5, run.value.players.size)
        assertEquals("Index 0 out of bounds for length 0", run.output.trim())
    }

    // --- Q3: Throw and Catch cards -------------------------------------------------------------------

    @Test
    fun `Q3 - over a whole game no player is ever told another player's Throw or Catch card`() {
        val plan = List(4) { listOf("BJFNVHA", "EQMX*S-") }
        val game = playRigged(2, plan, lastRound = 3)
        game.assertRanAsPlanned()

        val (first, second) = game.seats
        // B and E are Throw cards: they never leave their owner, so they appear nowhere in the other inbox.
        assertFalse(second.inbox.any { it.contains("[B]") })
        assertFalse(first.inbox.any { it.contains("[E]") })
        // A and - are Catch cards: the other player held them in hand, but is never told who got them.
        assertFalse(second.toldAbout(0).any { it.contains("[A]") })
        assertFalse(first.toldAbout(1).any { it.contains("[-]") })
        // What others are told about a player is always "Player n has drafted" with cards 2 to 6 at most.
        assertEquals(20, second.toldAbout(0).size)
        assertTrue(second.toldAbout(0).all { told -> Regex("\\[(.)] \\(").findAll(told).all { it.groupValues[1] in "JFNVH" } })
    }

    // --- Q6: activities without cards ----------------------------------------------------------------

    @Test
    fun `Q6 - an activity with no card is offered, scores 0 when accepted and is used up for the game`() {
        val run = solo(keep = { it == "Swimming" }) {
            score(0, "ABCDEFG") // no Swimming card
            val afterRound1 = player.activitiesScore.toMap()
            score(1, "HLNSW-A") // six Swimming cards, worth 15
            afterRound1 to player.activitiesScore.toMap()
        }

        assertTrue(run.output.contains("Want to keep Swimming(0) [0 points]? (Y/N)"))
        assertEquals(mapOf("Swimming" to 0), run.value.first)
        assertEquals(mapOf("Swimming" to 0), run.value.second)
        assertEquals(1, Regex("Want to keep Swimming").findAll(run.output).count(), "not offered again in round 2")
    }

    @Test
    fun `Q7 - activities are asked one at a time in the order of a HashMap, not in the order of the source`() {
        val run = solo { score(0, "ABCDEFG") }

        val asked = Regex("Want to keep ([A-Za-z ]+)\\(").findAll(run.output).map { it.groupValues[1] }.toList()
        // Observed on the JDK 21 toolchain. The order follows String.hashCode and the HashMap of line 217;
        // the source lists them as Indigenous Culture, Bushwalking, Swimming, Sightseeing (line 215).
        assertEquals(listOf("Indigenous Culture", "Swimming", "Sightseeing", "Bushwalking"), asked)
    }

    // --- Q9: bots ------------------------------------------------------------------------------------

    @Test
    fun `Q9 - a bot drafts any card of its hand except the last one`() {
        val picked = solo(isBot = true) {
            val neighbour = game.Player(1, true, null, null, null)
            (1..700).map {
                player.hand.clear()
                player.draft.clear()
                player.hand.addAll(cards("ABCDEFG"))
                player.addCardToDraft(neighbour)
                player.draft.single().letter
            }.toSet()
        }.value

        // nextInt(hand.size()-1) on line 88: indexes 0 to 5 of 7. Uniform choice would miss G in 700
        // tries with a probability of (6/7)^700, about 10^-47.
        assertEquals(setOf("A", "B", "C", "D", "E", "F"), picked)
    }

    @Test
    fun `Q9 - from a hand of two a bot always takes the first card, and a hand of one makes it throw`() {
        solo(isBot = true) {
            val neighbour = game.Player(1, true, null, null, null)
            repeat(50) {
                player.hand.clear()
                player.draft.clear()
                player.hand.addAll(cards("AB"))
                player.addCardToDraft(neighbour)
                assertEquals("A", player.draft.single().letter)
            }
            player.hand.clear()
            player.hand.addAll(cards("A"))
            assertThrows(IllegalArgumentException::class.java) { player.addCardToDraft(neighbour) } // nextInt(0)
        }
    }

    @Test
    fun `Q9 - a bot says Y to the first activity asked, also with no card of it, and so wastes activities`() {
        val used = solo(isBot = true) {
            // six Bushwalking cards and one card without an activity, three rounds running
            repeat(3) { score(it, "DKMOTUC") }
            player.activitiesScore.toMap()
        }.value

        // The bot goes down the HashMap order of the Q7 test, one activity per round, whatever it holds:
        // three activities with 0 cards are used up, and Bushwalking (worth 15 here) waits for round 4.
        assertEquals(mapOf("Indigenous Culture" to 0, "Swimming" to 0, "Sightseeing" to 0), used)
    }

    // --- Section 7, row R10b: the late region bonus --------------------------------------------------

    @Test
    fun `R10b row - a region completed by the Catch card is paid one round late`() {
        val game = playRigged(2, listOf(listOf("EFGAIMH", "QRUVYZB"), listOf("BCJKNOS", "ADILPTW")), lastRound = 1)
        game.assertRanAsPlanned()

        assertEquals(listOf(0, 3), game.seats[0].regionPointsTold())
    }

    @Test
    fun `R10b row - and then a player completing the region a round later is paid as well`() {
        // Seat 0 completes Northern Territory in round 1 (by the Catch card), seat 1 in round 2.
        val game = playRigged(2, listOf(listOf("EFGAIMH", "QRUVYZB"), listOf("QRUVYZB", "EFGHAIM")), lastRound = 1)
        game.assertRanAsPlanned()

        assertEquals(listOf(0, 3), game.seats[0].regionPointsTold())
        assertEquals(listOf(0, 3), game.seats[1].regionPointsTold())
    }

    // --- Section 7, row R7: where the game dies ------------------------------------------------------

    @Test
    fun `R7 row - a player who never answers ends the game at the first report of drafted cards`() {
        val game = playRigged(2, typed = { _, seat, _, planned -> if (seat == 0) "" else planned })

        // draft.subList(1, 0) on line 374, caught on line 421
        assertEquals("fromIndex(1) > toIndex(0)", game.ending)
        assertEquals(listOf(7, 7), game.seats[1].handsSeen[0].map { it.length }, "seat 1 is handed 7 cards again")
    }

    // --- Rule 12 / AC12.1 ----------------------------------------------------------------------------

    @Test
    fun `R12 - after the fourth round the original shuffles and deals a fifth time before naming the winner`() {
        val game = playRigged(2, lastRound = 3)
        game.assertRanAsPlanned()

        // Lines 403 to 410 run at the end of every pass of the round loop, the last one included.
        assertEquals(listOf(7, 7), game.seats.map { it.hand.size })
        assertEquals(14, game.deck.size)
        assertEquals(4, game.seats[0].drafts.size)
    }

    // --- Section 7, row S2: swallowed exceptions -----------------------------------------------------

    @Test
    fun `S2 row - a console player whose input has ended answers with an empty string (line 64)`() {
        val run = onConsole { BoomerangAustralia(arrayOf<String>()).Player(0, false, null, null, null).readMessage() }

        assertEquals("", run.value)
    }

    @Test
    fun `S2 row - an online player works over in-memory object streams, and stream failures vanish (lines 54, 62)`() {
        onConsole {
            val game = BoomerangAustralia(arrayOf<String>())

            // control: with working streams a message goes out and an answer comes in
            val wire = ByteArrayOutputStream()
            val answer = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject("K") }
            val online = game.Player(1, false, null, ObjectInputStream(ByteArrayInputStream(answer.toByteArray())), ObjectOutputStream(wire))
            online.online = true // the constructor derives this from the Socket; the field is public
            online.sendMessage("hello")
            assertEquals("hello", ObjectInputStream(ByteArrayInputStream(wire.toByteArray())).readObject())
            assertEquals("K", online.readMessage())

            // the stream is now at its end: readObject throws EOFException, readMessage returns ""
            assertEquals("", online.readMessage())

            // a connection that fails on every write after the stream header: sendMessage returns normally
            var broken = false
            val failing = object : OutputStream() {
                override fun write(b: Int) { if (broken) throw IOException("connection lost") }
            }
            val lost = game.Player(2, false, null, null, ObjectOutputStream(failing))
            lost.online = true
            broken = true
            lost.sendMessage("x".repeat(5000))
        }
    }
}
