package app.gameleaderboard.engine

import app.gameleaderboard.core.model.Player
import app.gameleaderboard.core.model.ScoreUpdate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Gameplay simulation that mimics backend match scoring.
 *
 * Behavior:
 * - Picks a random player at random intervals.
 * - Increases score monotonically.
 * - Emits [ScoreUpdate] events to downstream consumers.
 */
class RandomScoreGenerator(
    private val players: List<Player>,
    private val minIntervalMillis: Long = 500L,
    private val maxIntervalMillis: Long = 2_000L,
    private val minScoreIncrement: Int = 5,
    private val maxScoreIncrement: Int = 60,
    seed: Int = 7_147_245,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScoreGenerator {
    private val random = Random(seed)
    private val scores = players.associate { it.id to 0 }.toMutableMap()
    private val updates = MutableSharedFlow<ScoreUpdate>(extraBufferCapacity = 64)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var generatorJob: Job? = null

    override val scoreUpdates: Flow<ScoreUpdate> = updates.asSharedFlow()

    override fun start() {
        // Prevent duplicate generator loops.
        if (generatorJob?.isActive == true || players.isEmpty()) return
        generatorJob =
            scope.launch {
                while (isActive) {
                    val delayMillis = random.nextLong(from = minIntervalMillis, until = maxIntervalMillis + 1)
                    delay(delayMillis)

                    val player = players[random.nextInt(players.size)]
                    val currentScore = scores.getValue(player.id)
                    val increment = random.nextInt(from = minScoreIncrement, until = maxScoreIncrement + 1)
                    val newScore = currentScore + increment
                    scores[player.id] = newScore

                    updates.emit(
                        ScoreUpdate(
                            playerId = player.id,
                            newScore = newScore,
                            timestampMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            }
    }

    override fun stop() {
        // Stop loop without destroying the full scope.
        generatorJob?.cancel()
        generatorJob = null
    }

    override fun release() {
        // Full cleanup used when app owner is disposed.
        stop()
        scope.cancel()
    }
}
