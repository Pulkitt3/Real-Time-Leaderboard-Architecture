package app.gameleaderboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.gameleaderboard.presentation.LeaderboardRoute
import app.gameleaderboard.presentation.LeaderboardViewModel
import app.gameleaderboard.ui.theme.GameLeaderBoardTheme

/**
 * Android entry point.
 * It owns the screen-level ViewModel and renders the leaderboard UI.
 */
class MainActivity : ComponentActivity() {
    // ViewModel keeps real-time state across configuration changes.
    private val viewModel: LeaderboardViewModel by viewModels { LeaderboardViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameLeaderBoardTheme {
                LeaderboardRoute(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
