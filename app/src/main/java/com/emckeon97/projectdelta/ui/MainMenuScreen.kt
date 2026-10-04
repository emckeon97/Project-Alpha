package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.drawCharacter
import com.emckeon97.projectdelta.characters.CharacterManager

@Composable
fun MainMenuScreen(navController: NavController, characterManager: CharacterManager) {
    val context = LocalContext.current
    val coins by characterManager.coins.collectAsState()
    val highScore by characterManager.highScore.collectAsState()
    val selectedID by characterManager.selectedID.collectAsState()
    val selected = characterManager.characters.first { it.id == selectedID }

    // Preload fullscreen ads so they're ready after a run.
    LaunchedEffect(Unit) {
        AdManager.loadInterstitial(context)
        AdManager.loadRewarded(context)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCharacter(
                    id = selected.id,
                    centerX = size.width / 2f,
                    feetY = size.height,
                    size = size.minDimension,
                    rolling = false
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "PROJECT DELTA",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "starring public-domain toons",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "\uD83E\uDE99 $coins     \u2605 $highScore m",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate(Routes.game()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("\u25B6 PLAY", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { navController.navigate(Routes.CHARACTERS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CHARACTERS", fontSize = 18.sp)
            }
        }
        AndroidView(
            factory = { ctx -> AdManager.bannerView(ctx) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
