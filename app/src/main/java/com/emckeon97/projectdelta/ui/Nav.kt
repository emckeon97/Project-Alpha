package com.emckeon97.projectdelta.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.game.GameEngine

object Routes {
    const val MENU = "menu"
    const val CHARACTERS = "characters"
    private const val GAME_BASE = "game"
    private const val GAME_OVER_BASE = "gameOver"

    /** Game screen. [revive]=true resumes the current run instead of starting fresh. */
    fun game(revive: Boolean = false): String = "$GAME_BASE?revive=$revive"

    fun gameOver(score: Int, coins: Int): String = "$GAME_OVER_BASE/$score/$coins"
}

@Composable
fun AppNav(characterManager: CharacterManager) {
    val navController = rememberNavController()
    // Hoisted above the NavHost so the engine survives Game -> GameOver -> Game
    // navigation (needed for the rewarded-ad revive flow).
    val engine = remember { GameEngine() }

    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MainMenuScreen(navController = navController, characterManager = characterManager)
        }
        composable(Routes.CHARACTERS) {
            CharacterSelectScreen(navController = navController, characterManager = characterManager)
        }
        composable(
            route = "game?revive={revive}",
            arguments = listOf(navArgument("revive") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val revive = backStackEntry.arguments?.getBoolean("revive") ?: false
            GameScreen(
                navController = navController,
                characterManager = characterManager,
                engine = engine,
                revive = revive
            )
        }
        composable(
            route = "gameOver/{score}/{coins}",
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("coins") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val coins = backStackEntry.arguments?.getInt("coins") ?: 0
            GameOverScreen(
                navController = navController,
                characterManager = characterManager,
                engine = engine,
                score = score,
                coins = coins
            )
        }
    }
}
