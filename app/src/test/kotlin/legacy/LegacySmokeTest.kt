import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LegacySmokeTest {
    @Test
    fun `no arguments starts no game`() {
        val game = BoomerangAustralia(arrayOf())

        assertEquals(0, game.players.size)
    }
}
