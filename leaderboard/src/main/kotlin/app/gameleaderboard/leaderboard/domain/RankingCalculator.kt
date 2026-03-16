package app.gameleaderboard.leaderboard.domain

import app.gameleaderboard.core.model.Player
import app.gameleaderboard.leaderboard.model.LeaderboardEntry

/**
 * Domain contract for converting raw scores into ranked leaderboard rows.
 *
 * This is used by [LeaderboardEngine] so ranking rules stay outside UI/ViewModel.
 */
interface RankingCalculator {
    /**
     * Builds sorted and ranked entries from current score snapshot.
     */
    fun calculate(
        players: List<Player>,
        scoresByPlayerId: Map<String, Int>,
        updatedPlayerId: String?,
    ): List<LeaderboardEntry>
}
