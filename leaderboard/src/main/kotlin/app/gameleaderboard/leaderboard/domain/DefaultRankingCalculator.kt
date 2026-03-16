package app.gameleaderboard.leaderboard.domain

import app.gameleaderboard.core.model.Player
import app.gameleaderboard.leaderboard.model.LeaderboardEntry

/**
 * Default ranking strategy:
 * - Sort by score descending
 * - Equal scores share same rank
 * - Next rank skips accordingly (competition ranking)
 */
class DefaultRankingCalculator : RankingCalculator {
    override fun calculate(
        players: List<Player>,
        scoresByPlayerId: Map<String, Int>,
        updatedPlayerId: String?,
    ): List<LeaderboardEntry> {
        val sorted =
            players
                .map { player ->
                    Candidate(
                        playerId = player.id,
                        username = player.username,
                        score = scoresByPlayerId[player.id] ?: 0,
                    )
                }
                .sortedWith(
                    compareByDescending<Candidate> { it.score }
                        .thenBy { it.username },
                )

        var lastScore: Int? = null
        var currentRank = 0
        var position = 0

        return sorted.map { candidate ->
            position += 1
            // Rank only advances when score changes; ties keep previous rank.
            if (candidate.score != lastScore) {
                currentRank = position
                lastScore = candidate.score
            }

            LeaderboardEntry(
                playerId = candidate.playerId,
                username = candidate.username,
                score = candidate.score,
                rank = currentRank,
                hasRecentUpdate = candidate.playerId == updatedPlayerId,
            )
        }
    }

    private data class Candidate(
        val playerId: String,
        val username: String,
        val score: Int,
    )
}
