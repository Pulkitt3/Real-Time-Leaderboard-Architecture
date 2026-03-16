package app.gameleaderboard.leaderboard.domain

import app.gameleaderboard.core.model.Player
import app.gameleaderboard.core.model.ScoreUpdate
import app.gameleaderboard.leaderboard.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold

/**
 * Consumer module orchestrator.
 *
 * It consumes gameplay score events and emits full leaderboard state updates.
 * This module never generates scores itself.
 */
class LeaderboardEngine(
    private val players: List<Player>,
    private val rankingCalculator: RankingCalculator,
) {
    /**
     * Transforms a stream of score events into ranked leaderboard snapshots.
     */
    fun leaderboard(scoreUpdates: Flow<ScoreUpdate>): Flow<List<LeaderboardEntry>> {
        return scoreUpdates
            .runningFold(
                initial =
                    Snapshot(
                        scoresByPlayerId = players.associate { it.id to 0 },
                        updatedPlayerId = null,
                    ),
            ) { previous, update ->
                // Keep an immutable snapshot per event for deterministic state transitions.
                Snapshot(
                    scoresByPlayerId =
                        previous.scoresByPlayerId.toMutableMap().apply {
                            this[update.playerId] = update.newScore
                        },
                    updatedPlayerId = update.playerId,
                )
            }
            .map { snapshot ->
                rankingCalculator.calculate(
                    players = players,
                    scoresByPlayerId = snapshot.scoresByPlayerId,
                    updatedPlayerId = snapshot.updatedPlayerId,
                )
            }
            .onStart {
                emit(
                    rankingCalculator.calculate(
                        players = players,
                        scoresByPlayerId = players.associate { it.id to 0 },
                        updatedPlayerId = null,
                    ),
                )
            }
    }

    private data class Snapshot(
        val scoresByPlayerId: Map<String, Int>,
        val updatedPlayerId: String?,
    )
}
