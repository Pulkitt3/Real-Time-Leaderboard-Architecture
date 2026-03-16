package app.gameleaderboard.leaderboard.domain

import app.gameleaderboard.core.model.Player
import app.gameleaderboard.core.model.ScoreUpdate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardEngineTest {
    private val players =
        listOf(
            Player(id = "p1", username = "Alpha"),
            Player(id = "p2", username = "Bravo"),
            Player(id = "p3", username = "Charlie"),
        )

    private val engine =
        LeaderboardEngine(
            players = players,
            rankingCalculator = DefaultRankingCalculator(),
        )

    @Test
    fun `emits initial leaderboard when no updates are present`() =
        runBlocking {
            val initial = engine.leaderboard(emptyFlow()).take(1).toList().single()

            assertEquals(3, initial.size)
            assertTrue(initial.all { it.score == 0 })
            assertTrue(initial.all { it.rank == 1 })
            assertTrue(initial.none { it.hasRecentUpdate })
        }

    @Test
    fun `applies score updates and marks latest updated player`() =
        runBlocking {
            val updates =
                flowOf(
                    ScoreUpdate(playerId = "p2", newScore = 40, timestampMillis = 1L),
                    ScoreUpdate(playerId = "p1", newScore = 80, timestampMillis = 2L),
                )

            val emissions = engine.leaderboard(updates).take(4).toList()
            val afterFirstUpdate = emissions[2]
            val afterSecondUpdate = emissions[3]

            assertEquals("p2", afterFirstUpdate.first().playerId)
            assertTrue(afterFirstUpdate.first().hasRecentUpdate)
            assertFalse(afterFirstUpdate.first { it.playerId == "p1" }.hasRecentUpdate)

            assertEquals("p1", afterSecondUpdate.first().playerId)
            assertEquals(80, afterSecondUpdate.first().score)
            assertTrue(afterSecondUpdate.first().hasRecentUpdate)
            assertFalse(afterSecondUpdate.first { it.playerId == "p2" }.hasRecentUpdate)
        }
}
