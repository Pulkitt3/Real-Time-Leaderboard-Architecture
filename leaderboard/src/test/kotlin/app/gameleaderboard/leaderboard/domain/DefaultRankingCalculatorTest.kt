package app.gameleaderboard.leaderboard.domain

import app.gameleaderboard.core.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRankingCalculatorTest {
    private val players =
        listOf(
            Player(id = "p1", username = "Alpha"),
            Player(id = "p2", username = "Bravo"),
            Player(id = "p3", username = "Charlie"),
            Player(id = "p4", username = "Delta"),
        )

    private val calculator = DefaultRankingCalculator()

    @Test
    fun `assigns same rank for ties and skips next rank`() {
        val result =
            calculator.calculate(
                players = players,
                scoresByPlayerId =
                    mapOf(
                        "p1" to 120,
                        "p2" to 90,
                        "p3" to 90,
                        "p4" to 40,
                    ),
                updatedPlayerId = "p3",
            )

        assertEquals(listOf(1, 2, 2, 4), result.map { it.rank })
        assertTrue(result.first { it.playerId == "p3" }.hasRecentUpdate)
        assertFalse(result.first { it.playerId == "p2" }.hasRecentUpdate)
    }

    @Test
    fun `sorts by score descending then username for stable ordering`() {
        val result =
            calculator.calculate(
                players = players,
                scoresByPlayerId =
                    mapOf(
                        "p1" to 50,
                        "p2" to 50,
                        "p3" to 50,
                        "p4" to 50,
                    ),
                updatedPlayerId = null,
            )

        assertEquals(
            listOf("Alpha", "Bravo", "Charlie", "Delta"),
            result.map { it.username },
        )
        assertEquals(listOf(1, 1, 1, 1), result.map { it.rank })
    }

    @Test
    fun `defaults missing player scores to zero`() {
        val result =
            calculator.calculate(
                players = players,
                scoresByPlayerId =
                    mapOf(
                        "p1" to 30,
                        "p2" to 10,
                    ),
                updatedPlayerId = null,
            )

        assertEquals(30, result.first { it.playerId == "p1" }.score)
        assertEquals(10, result.first { it.playerId == "p2" }.score)
        assertEquals(0, result.first { it.playerId == "p3" }.score)
        assertEquals(0, result.first { it.playerId == "p4" }.score)
    }

    @Test
    fun `does not mark recent update when id is unknown`() {
        val result =
            calculator.calculate(
                players = players,
                scoresByPlayerId =
                    mapOf(
                        "p1" to 120,
                        "p2" to 90,
                        "p3" to 60,
                        "p4" to 40,
                    ),
                updatedPlayerId = "unknown",
            )

        assertTrue(result.none { it.hasRecentUpdate })
    }
}
