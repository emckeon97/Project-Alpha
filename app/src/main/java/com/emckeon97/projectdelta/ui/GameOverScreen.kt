package com.emckeon97.projectdelta.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.game.GameEngine

/** Post-run screen as a classic end card: "THE END" with a gold double rule. */
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
            .background(DeltaTheme.ink)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(36.dp))
        // The end card.
        DoubleRule(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp))
        Text(
            text = "THE END",
            color = DeltaTheme.cream,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        DoubleRule(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp))

        if (isNewBest) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "★ NEW BEST! ★",
                color = DeltaTheme.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .background(DeltaTheme.gold, RoundedCornerShape(10.dp))
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        StatRow(label = "DISTANCE", value = "$score m")
        StatRow(label = "COINS", value = "\uD83E\uDE99 $coins")
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeltaTheme.gold,
                contentColor = DeltaTheme.ink,
                disabledContainerColor = DeltaTheme.gold.copy(alpha = 0.45f)
            )
        ) {
            Text(
                text = "\uD83D\uDCFA ENCORE — WATCH AD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "RUN AGAIN") {
            navController.navigate(Routes.game()) {
                popUpTo(Routes.MENU)
            }
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton(text = "MAIN MENU") {
            navController.popBackStack(Routes.MENU, inclusive = false)
        }
    }
}

@Composable
private fun DoubleRule(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(DeltaTheme.gold.copy(alpha = 0.8f))
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DeltaTheme.gold.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DeltaTheme.cream.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 3.sp
        )
        Text(
            text = value,
            color = DeltaTheme.cream,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, DeltaTheme.cream.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeltaTheme.cream)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}
