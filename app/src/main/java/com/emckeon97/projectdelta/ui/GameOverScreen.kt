package com.emckeon97.projectdelta.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.game.GameEngine

@Composable
fun GameOverScreen(
    navController: NavController,
    characterManager: CharacterManager,
    engine: GameEngine,
    score: Int,
    coins: Int
) {
    val context = LocalContext.current
    val activity = context as Activity
    val highScore by characterManager.highScore.collectAsState()
    // Freeze the "new best" verdict before recordScore() runs below.
    val isNewBest = remember { score > 0 && score > characterManager.highScore.value }
    val rewardedReady by AdManager.isRewardedReady.collectAsState()

    LaunchedEffect(Unit) {
        characterManager.addCoins(coins)
        characterManager.recordScore(score)
        AdManager.loadInterstitial(context)
        AdManager.loadRewarded(context)
        AdManager.gameOverOccurred(activity)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WIPED OUT!",
            color = Color(0xFFFF5252),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black
        )
        if (isNewBest) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "\u2605 NEW BEST \u2605",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(text = "Score: $score m", color = Color.White, fontSize = 24.sp)
        Text(
            text = "Coins: \uD83E\uDE99 $coins",
            color = Color(0xFFFFD54F),
            fontSize = 20.sp
        )
        Text(text = "Best: $highScore m", color = Color.Gray, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                AdManager.showRewarded(activity) {
                    navController.navigate(Routes.game(revive = true)) {
                        popUpTo(Routes.MENU)
                    }
                }
            },
            enabled = rewardedReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("\uD83D\uDCFA REVIVE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                navController.navigate(Routes.game()) {
                    popUpTo(Routes.MENU)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RUN AGAIN", fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { navController.popBackStack(Routes.MENU, inclusive = false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("MENU", fontSize = 18.sp)
        }
    }
}
