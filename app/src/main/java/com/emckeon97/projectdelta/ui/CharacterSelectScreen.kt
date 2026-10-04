package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.emckeon97.projectdelta.characters.drawCharacter
import com.emckeon97.projectdelta.characters.CharacterManager

@Composable
fun CharacterSelectScreen(navController: NavController, characterManager: CharacterManager) {
    val coins by characterManager.coins.collectAsState()
    val selectedID by characterManager.selectedID.collectAsState()
    // Observed so the grid recomposes on unlock/select.
    val unlockedIDs by characterManager.unlockedIDs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "CHOOSE YOUR TOON",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "\uD83E\uDE99 $coins",
            color = Color(0xFFFFD54F),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

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
                        containerColor = if (isSelected) Color(0xFF3A3A3A) else Color(0xFF1E1E1E)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(modifier = Modifier.size(96.dp)) {
                            drawCharacter(
                                id = character.id,
                                centerX = size.width / 2f,
                                feetY = size.height,
                                size = size.minDimension,
                                rolling = false
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = character.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = character.tagline,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.height(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        when {
                            isSelected -> Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("SELECTED") }

                            isUnlocked -> Button(
                                onClick = { characterManager.select(character) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("SELECT") }

                            else -> Button(
                                onClick = { characterManager.unlock(character) },
                                enabled = coins >= character.price,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("\uD83E\uDE99 ${character.price}") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("BACK") }
    }
}
