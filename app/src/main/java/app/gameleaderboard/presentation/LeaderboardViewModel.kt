package app.gameleaderboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.gameleaderboard.core.model.Player
import app.gameleaderboard.engine.RandomScoreGenerator
import app.gameleaderboard.engine.ScoreGenerator
import app.gameleaderboard.leaderboard.domain.DefaultRankingCalculator
import app.gameleaderboard.leaderboard.domain.LeaderboardEngine
import app.gameleaderboard.leaderboard.model.LeaderboardEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Presentation coordinator for the leaderboard screen.
 *
 * How it is used:
 * - Starts score generation from the gameplay module.
 * - Subscribes leaderboard domain flow from the consumer module.
 * - Exposes UI state as StateFlow for Compose.
 */
class LeaderboardViewModel(
    private val scoreGenerator: ScoreGenerator,
    leaderboardEngine: LeaderboardEngine
) : ViewModel() {

    // Converts cold flow to hot state so UI can observe current leaderboard at any time.
    val leaderboard: StateFlow<List<LeaderboardEntry>> = leaderboardEngine
        .leaderboard(scoreGenerator.scoreUpdates)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    init {
        // Starts emitting gameplay score events.
        scoreGenerator.start()
    }

    override fun onCleared() {
        // Cancels generator scope to avoid leaks when screen owner is destroyed.
        scoreGenerator.release()
        super.onCleared()
    }

    companion object {
        /**
         * Simple manual wiring for interview readability.
         * In production this can be replaced by DI (e.g., Hilt).
         */
        fun factory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val players = demoPlayers()
                    val generator = RandomScoreGenerator(players = players)
                    val engine = LeaderboardEngine(
                        players = players,
                        rankingCalculator = DefaultRankingCalculator()
                    )

                    @Suppress("UNCHECKED_CAST")
                    return LeaderboardViewModel(
                        scoreGenerator = generator,
                        leaderboardEngine = engine
                    ) as T
                }
            }
        }

        private fun demoPlayers(): List<Player> {
            return listOf(
                Player(id = "p1", username = "ApexNinja"),
                Player(id = "p2", username = "BlazeRider"),
                Player(id = "p3", username = "CyberMonk"),
                Player(id = "p4", username = "DeltaWolf"),
                Player(id = "p5", username = "EchoShade"),
                Player(id = "p6", username = "FrostByte"),
                Player(id = "p7", username = "GhostSpark"),
                Player(id = "p8", username = "HyperNova")
            )
        }
    }
}
