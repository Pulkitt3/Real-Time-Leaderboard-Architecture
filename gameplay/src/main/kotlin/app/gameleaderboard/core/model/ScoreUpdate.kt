package app.gameleaderboard.core.model

data class ScoreUpdate(
    val playerId: String,
    val newScore: Int,
    val timestampMillis: Long,
)
