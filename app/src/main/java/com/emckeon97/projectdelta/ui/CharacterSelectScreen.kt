package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emckeon97.projectdelta.characters.GameCharacter
import com.emckeon97.projectdelta.characters.drawCharacter
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.characters.spriteResFor

/** Character roster grid: marquee title, cream-and-gold cells on dark. */
@Composable
fun CharacterSelectScreen(navController: NavController, characterManager: CharacterManager) {
    val coins by characterManager.coins.collectAsState()
    val selectedID by characterManager.selectedID.collectAsState()
    // Observed so the grid recomposes on unlock/select.
    val unlockedIDs by characterManager.unlockedIDs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeltaTheme.ink)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DeltaTheme.cream.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeltaTheme.cream),
                contentPadding = PaddingValues(10.dp)
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Text(
                text = "\uD83E\uDE99 $coins",
                color = DeltaTheme.gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
        MarqueeLights(count = 14, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))
        Text(
            text = "CHOOSE YOUR STAR",
            color = DeltaTheme.cream,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(characterManager.characters, key = { it.id }) { character ->
                val isSelected = character.id == selectedID
                val isUnlocked = unlockedIDs.contains(character.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            DeltaTheme.gold.copy(alpha = 0.12f)
                        else
                            DeltaTheme.cream.copy(alpha = 0.03f)
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) DeltaTheme.gold else DeltaTheme.cream.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CharacterThumbnail(character = character)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = character.name.uppercase(),
                            color = DeltaTheme.cream,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = character.tagline,
                            color = DeltaTheme.cream.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.height(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        when {
                            isSelected -> Text(
                                text = "★ STARRING ★",
                                color = DeltaTheme.gold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )

                            isUnlocked -> Button(
                                onClick = { characterManager.select(character) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeltaTheme.gold,
                                    contentColor = DeltaTheme.ink
                                )
                            ) {
                                Text(
                                    text = "SELECT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Serif,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }

                            else -> {
                                val affordable = coins >= character.price
                                Button(
                                    onClick = { characterManager.unlock(character) },
                                    enabled = affordable,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeltaTheme.gold,
                                        contentColor = DeltaTheme.ink,
                                        disabledContainerColor = DeltaTheme.gold.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Text(
                                        text = "\uD83E\uDE99 ${character.price}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Serif,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(2.dp, DeltaTheme.cream.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeltaTheme.cream)
        ) {
            Text(
                text = "BACK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/** Bundled sprite art when available, primitive Canvas fallback otherwise. */
@Composable
private fun CharacterThumbnail(character: GameCharacter) {
    val spriteRes = spriteResFor(character.id)
    if (spriteRes != null) {
        Image(
            painter = painterResource(id = spriteRes),
            contentDescription = character.name,
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Canvas(modifier = Modifier.size(96.dp)) {
            drawCharacter(
                id = character.id,
                centerX = size.width / 2f,
                feetY = size.height,
                size = size.minDimension,
                rolling = false
            )
        }
    }
}
