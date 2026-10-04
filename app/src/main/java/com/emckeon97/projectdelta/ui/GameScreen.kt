package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.game.GameEngine
import com.emckeon97.projectdelta.game.GameRenderer
import kotlin.math.abs

/** Sepia HUD pill: cream serif text on dark translucent, gold hairline. */
@Composable
private fun HudPill(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, DeltaTheme.gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = DeltaTheme.cream,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
    }
}

/** Minimum drag distance (px) before a swipe registers. */
private const val SWIPE_THRESHOLD = 60f

/** First-run how-to-play card. */
@Composable
private fun HowToPlayCard(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .border(2.dp, DeltaTheme.gold, RoundedCornerShape(4.dp))
                .background(DeltaTheme.ink)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HOW TO PLAY",
                color = DeltaTheme.gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HowToRow("\u2190 \u2192", "Swipe sideways to change lanes")
            HowToRow("\u2191", "Swipe up to leap the rowboats")
            HowToRow("\u2193", "Swipe down to roll under the footbridges")
            HowToRow("\uD83D\uDEA2", "Dodge the paddle-wheelers!")
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeltaTheme.gold,
                    contentColor = DeltaTheme.ink
                )
            ) {
                Text(
                    text = "ROLL FILM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun HowToRow(glyph: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            color = DeltaTheme.gold,
            fontSize = 22.sp,
            modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = text,
            color = DeltaTheme.cream.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif
        )
    }
}

/** Silent-film title card shown when a new reel (level) begins. */
@Composable
private fun ReelTitleCard(reel: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .border(2.dp, DeltaTheme.gold, RoundedCornerShape(4.dp))
                .padding(3.dp)
                .border(1.dp, DeltaTheme.gold.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REEL $reel",
                color = DeltaTheme.gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = GameEngine.reelTitle(reel),
                color = DeltaTheme.cream,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = GameEngine.reelBlurb(reel),
                color = DeltaTheme.cream.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun GameScreen(
    navController: NavController,
    characterManager: CharacterManager,
    engine: GameEngine,
    revive: Boolean
) {
    val characterID by characterManager.selectedID.collectAsState()
    var paused by remember { mutableStateOf(false) }
    // Gate the renderer until the run state is (re)initialized, so it never
    // observes a stale gameOver=true from a previous run.
    var ready by remember { mutableStateOf(false) }

    // HUD mirrors of the engine's plain-Kotlin fields.
    var hudScore by remember { mutableIntStateOf(0) }
    var hudCoins by remember { mutableIntStateOf(0) }
    // First-run how-to-play.
    var showHowTo by remember { mutableStateOf(!characterManager.sawHowTo) }
    // Silent-film reel title card.
    var reelCard by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(reelCard) {
        if (reelCard != null) {
            kotlinx.coroutines.delay(2600)
            reelCard = null
        }
    }

    LaunchedEffect(revive) {
        engine.paused = false
        if (revive) engine.revive() else engine.reset()
        if (showHowTo) engine.paused = true
        ready = true
    }
    // Lightweight per-frame read of the engine's score/coins for the HUD.
    // (The renderer owns the real frame loop; this only copies two Ints.)
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        while (true) {
            withFrameNanos {
                hudScore = engine.score
                hudCoins = engine.coinsCollected
                if (engine.reelChanged) {
                    engine.reelChanged = false
                    reelCard = engine.reel
                }
            }
        }
    }
    // Never leave the simulation running when this screen goes away.
    DisposableEffect(Unit) {
        onDispose { engine.paused = true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragCancel = { total = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                    onDragEnd = {
                        val (x, y) = total
                        if (abs(x) > abs(y)) {
                            if (x > SWIPE_THRESHOLD) engine.moveRight()
                            else if (x < -SWIPE_THRESHOLD) engine.moveLeft()
                        } else {
                            // In Compose, -y is up.
                            if (y < -SWIPE_THRESHOLD) engine.jump()
                            else if (y > SWIPE_THRESHOLD) engine.roll()
                        }
                        total = Offset.Zero
                    }
                )
            }
    ) {
        if (ready) {
            GameRenderer(
                engine = engine,
                characterID = characterID,
                modifier = Modifier.fillMaxSize(),
                onGameOver = {
                    navController.navigate(
                        Routes.gameOver(engine.score, engine.coinsCollected)
                    ) {
                        popUpTo(Routes.MENU)
                    }
                }
            )
        }

        // HUD overlay — sepia pills (cream on dark, gold hairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudPill(text = "$hudScore m")
            Row(verticalAlignment = Alignment.CenterVertically) {
                HudPill(text = "\uD83E\uDE99 $hudCoins")
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        paused = true
                        engine.paused = true
                    },
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Text("\u23F8", fontSize = 18.sp)
                }
            }
        }

        // Silent-film reel title card (auto-dismisses; doesn't block swipes)
        reelCard?.let { ReelTitleCard(reel = it) }

        // First-run how-to-play (pauses the engine until dismissed)
        if (showHowTo) {
            HowToPlayCard(onDismiss = {
                characterManager.sawHowTo = true
                showHowTo = false
                engine.paused = false
            })
        }

        if (paused) {
            AlertDialog(                onDismissRequest = { /* force an explicit choice */ },
                title = { Text("Paused") },
                text = { Text("Take a breather.") },
                confirmButton = {
                    TextButton(onClick = {
                        paused = false
                        engine.paused = false
                    }) { Text("RESUME") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        navController.popBackStack(Routes.MENU, inclusive = false)
                    }) { Text("QUIT") }
                }
            )
        }
    }
}
