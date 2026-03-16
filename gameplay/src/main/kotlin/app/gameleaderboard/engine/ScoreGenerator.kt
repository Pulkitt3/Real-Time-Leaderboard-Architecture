package app.gameleaderboard.engine

import app.gameleaderboard.core.model.ScoreUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Gameplay module contract.
 *
 * How it is used:
 * - Producer implementation emits real-time [ScoreUpdate] events.
 * - Consumer module listens to [scoreUpdates] and builds leaderboard state.
 */
interface ScoreGenerator {
    /** Stream of gameplay score events. */
    val scoreUpdates: Flow<ScoreUpdate>

    /** Starts generating gameplay events. */
    fun start()

    /** Stops generating events but keeps resources available for restart. */
    fun stop()

    /** Final cleanup when owner is destroyed. */
    fun release()
}
