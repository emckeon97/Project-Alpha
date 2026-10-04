package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.characters.drawCharacter
import com.emckeon97.projectdelta.characters.spriteResFor
import com.emckeon97.projectdelta.game.MusicManager

/** 1930s movie-poster marquee menu. */
@Composable
fun MainMenuScreen(navController: NavController, characterManager: CharacterManager) {
    val context = LocalContext.current
    val coins by characterManager.coins.collectAsState()
    val highScore by characterManager.highScore.collectAsState()
    val selectedID by characterManager.selectedID.collectAsState()
    val selected = characterManager.characters.first { it.id == selectedID }
    val muted by MusicManager.isMuted.collectAsState()

    // Preload fullscreen ads so they're ready after a run.
    LaunchedEffect(Unit) {
        AdManager.loadInterstitial(context)
        AdManager.loadRewarded(context)
        MusicManager.play(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(DeltaTheme.ink)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NOW SHOWING",
                    color = DeltaTheme.cream.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 6.sp
                )
                MarqueeLights(modifier = Modifier.padding(vertical = 10.dp))
                Text(
                    text = "PROJECT DELTA",
                    color = DeltaTheme.cream,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "A STEAMBOAT CARTOON",
                    color = DeltaTheme.gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                // Selected-character preview: sprite art when bundled, primitive fallback.
                val spriteRes = spriteResFor(selected.id)
                if (spriteRes != null) {
                    Image(
                        painter = painterResource(id = spriteRes),
                        contentDescription = selected.name,
                        modifier = Modifier.size(110.dp).padding(top = 14.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Canvas(modifier = Modifier.size(110.dp).padding(top = 14.dp)) {
                        drawCharacter(
                            id = selected.id,
                            centerX = size.width / 2f,
                            feetY = size.height,
                            size = size.minDimension,
                            rolling = false
                        )
                    }
                }
                Text(
                    text = selected.name.uppercase(),
                    color = DeltaTheme.cream.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 18.dp)
                ) {
                    StatPill("\uD83E\uDE99 $coins")
                    StatPill("\u2605 $highScore m")
                }
                // Ticket-stub PLAY button.
                Button(
                    onClick = { navController.navigate(Routes.game()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeltaTheme.gold,
                        contentColor = DeltaTheme.ink
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 54.dp, vertical = 13.dp)
                    ) {
                        Text(
                            text = "★ ADMIT ONE ★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "PLAY",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 5.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { navController.navigate(Routes.CHARACTERS) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, DeltaTheme.cream.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeltaTheme.cream
                    )
                ) {
                    Text(
                        text = "MEET THE STARS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Billing block — the full roster, movie-poster style.
                Text(
                    text = characterManager.characters.joinToString("   •   ") { it.name.uppercase() },
                    color = DeltaTheme.cream.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                )
                Text(
                    text = "Music: \"The Entertainer\" by Kevin MacLeod (incompetech.com) · CC BY 4.0",
                    color = DeltaTheme.cream.copy(alpha = 0.35f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            AndroidView(
                factory = { ctx -> AdManager.bannerView(ctx) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        // Music mute toggle.
        Button(
            onClick = { MusicManager.toggleMute(context) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeltaTheme.cream.copy(alpha = 0.06f),
                contentColor = DeltaTheme.gold
            ),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(text = if (muted) "\uD83D\uDD07" else "\uD83D\uDD0A", fontSize = 20.sp)
        }
    }
}

@Composable
private fun StatPill(value: String) {
    Box(
        modifier = Modifier
            .border(1.dp, DeltaTheme.gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .background(DeltaTheme.cream.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            color = DeltaTheme.gold,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
    }
}
