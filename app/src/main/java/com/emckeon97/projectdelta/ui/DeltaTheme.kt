package com.emckeon97.projectdelta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 1930s cartoon-marquee palette (mirrors the iOS DeltaTheme). */
object DeltaTheme {
    val ink = Color(0xFF0A0A0C)
    val cream = Color(0xFFF5EFE0)
    val gold = Color(0xFFD4A942)
    val red = Color(0xFFB03A2E)
}

/** A row of marquee chase lights — some lit, some dimmed. */
@Composable
fun MarqueeLights(count: Int = 18, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        repeat(count) { i ->
            val lit = i % 3 != 0
            Box(
                Modifier
                    .size(7.dp)
                    .background(
                        if (lit) DeltaTheme.gold else DeltaTheme.gold.copy(alpha = 0.22f),
                        CircleShape
                    )
            )
        }
    }
}
