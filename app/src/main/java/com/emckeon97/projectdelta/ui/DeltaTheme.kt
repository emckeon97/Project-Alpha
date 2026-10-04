package com.emckeon97.projectdelta.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** A row of marquee chase lights, animated in classic chase sequence. */
@Composable
fun MarqueeLights(count: Int = 18, modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "marquee").animateFloat(
        initialValue = 0f,
        targetValue = 2.999f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chase"
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        repeat(count) { i ->
            val lit = (i + phase.toInt()) % 3 != 0
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
