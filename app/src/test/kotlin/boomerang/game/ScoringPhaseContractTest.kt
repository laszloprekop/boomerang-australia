package boomerang.game

import boomerang.support.FixedCategory
import boomerang.support.ScriptedPlayer
import boomerang.support.draft
import boomerang.support.points
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * R10, review finding L4. Added after the review of the finished code, tests first.
 *
 * The contract of IScoringCategory says the list of scores is never empty. A category that breaks it is a
 * programming error in an edition, and the scoring phase is to say which category it was, not to fail
 * with a bare "list is empty".
 */
class ScoringPhaseContractTest {
    private val drafts = listOf(draft("EFGHAIM"), draft("QRUVYZB"))
    private val players = List(2) { ScriptedPlayer(it) }

    private fun assertNamed(failure: IllegalStateException, name: String) =
        assertTrue(name in failure.message.orEmpty(), "the message does not name the category: '${failure.message}'")

    @Test
    fun `R10 L4 a category that offers no score at all stops the scoring with a failure that names the category`() {
        val hollow = FixedCategory("Hollow") { emptyList() }

        val failure = assertThrows(IllegalStateException::class.java) {
            ScoringPhase(listOf(points("Sound", 1, 2), hollow), players).run(drafts, History())
        }

        assertNamed(failure, "Hollow")
    }

    @Test
    fun `R10 L4 a category that offers no score to one player only is reported in the same way`() {
        val halfHollow = FixedCategory("Half hollow") { if (it.seat == 1) emptyList() else listOf(Score(3)) }

        val failure = assertThrows(IllegalStateException::class.java) {
            ScoringPhase(listOf(halfHollow), players).run(drafts, History())
        }

        assertNamed(failure, "Half hollow")
    }
}
