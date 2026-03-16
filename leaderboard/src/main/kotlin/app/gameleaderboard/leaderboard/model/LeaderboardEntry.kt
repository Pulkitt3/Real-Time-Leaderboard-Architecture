package app.gameleaderboard.leaderboard.model

data class LeaderboardEntry(
    val playerId: String,
    val username: String,
    val score: Int,
    val rank: Int,
    val hasRecentUpdate: Boolean,
)
