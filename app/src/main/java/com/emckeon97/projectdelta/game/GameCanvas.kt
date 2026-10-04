package com.emckeon97.projectdelta.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emckeon97.projectdelta.characters.drawCharacter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

/**
 * Full-screen 60fps Canvas renderer for [GameEngine].
 * Owns the frame loop; calls [onGameOver] once when the run ends.
 * Swipe input is handled by the hosting GameScreen — the engine only
 * exposes moveLeft()/moveRight()/jump()/roll().
 */
@Composable
fun GameRenderer(
    engine: GameEngine,
    characterID: String,
    modifier: Modifier = Modifier,
    onGameOver: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    var tick by remember { mutableLongStateOf(0L) }
    var scrollPx by remember { mutableStateOf(0f) }
    var gameOverFired by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val spacingPx = min(wPx / 3.2f, with(density) { 132.dp.toPx() })
        val playerYPx = hPx * 0.80f

        LaunchedEffect(spacingPx, hPx) {
            engine.applyGeometry(spacingPx, playerYPx, hPx)
        }

        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last == 0L) last = now
                    val dtMs = ((now - last) / 1_000_000).coerceAtMost(50)
                    last = now
                    if (!engine.gameOver && !engine.paused) {
                        gameOverFired = false
                        engine.update(dtMs)
                        scrollPx = (scrollPx + engine.speed * (dtMs / 1000f)) % (spacingPx * 2f)
                    } else if (!gameOverFired) {
                        gameOverFired = true
                        onGameOver()
                    }
                    tick = now
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            tick // redraw every frame
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val spacing = engine.laneSpacing
            val pY = engine.playerY

            // ---- background: dark gradient ----
            drawRect(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1026), Color(0xFF141B3D), Color(0xFF0B1026))
                )
            )

            // ---- speed lines ----
            val lineGap = spacing * 1.4f
            var ly = -lineGap + (scrollPx % lineGap)
            while (ly < h + lineGap) {
                for (lx in listOf(cx - spacing * 1.5f, cx + spacing * 1.5f)) {
                    drawLine(
                        Color.White.copy(alpha = 0.06f),
                        Offset(lx, ly),
                        Offset(lx, ly + lineGap * 0.45f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
                ly += lineGap
            }

            // ---- lane strips + scrolling ties ----
            for (lane in 0..2) {
                val lx = cx + engine.laneX(lane)
                drawRect(
                    Color.White.copy(alpha = 0.03f),
                    Offset(lx - spacing * 0.46f, 0f),
                    Size(spacing * 0.92f, h)
                )
            }
            val tieGap = 130f
            var ty = -tieGap + (scrollPx % tieGap)
            while (ty < h + tieGap) {
                for (lane in 0..2) {
                    val lx = cx + engine.laneX(lane)
                    drawRoundRect(
                        Color.White.copy(alpha = 0.05f),
                        Offset(lx - spacing * 0.38f, ty),
                        Size(spacing * 0.76f, 14f),
                        CornerRadius(7f)
                    )
                }
                ty += tieGap
            }

            // ---- lane dividers (dashed, scrolling) ----
            val dashGap = 90f
            var dyDash = -dashGap + (scrollPx % dashGap)
            while (dyDash < h + dashGap) {
                for (dx in listOf(cx - spacing / 2f, cx + spacing / 2f)) {
                    drawLine(
                        Color(0xFFFFD54F).copy(alpha = 0.35f),
                        Offset(dx, dyDash),
                        Offset(dx, dyDash + dashGap * 0.5f),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }
                dyDash += dashGap
            }

            val nowSec = tick / 1_000_000_000f

            // ---- power-ups ----
            for (p in engine.powerups) {
                val px = cx + engine.laneX(p.lane)
                val bob = cos(nowSec * 4f + p.y * 0.01f) * 10f
                when (p.kind) {
                    PowerUpKind.MAGNET -> drawMagnet(px, p.y + bob, 64f)
                    PowerUpKind.MULTIPLIER -> {
                        drawCircle(Color(0xFFFFD54F), 40f, Offset(px, p.y + bob))
                        drawCircle(Color(0xFFB7860B), 40f, Offset(px, p.y + bob), style = Stroke(6f))
                        drawText(
                            textMeasurer, "2x",
                            topLeft = Offset(px - 26f, p.y + bob - 28f),
                            style = TextStyle(color = Color(0xFF3E2723), fontSize = 34.sp)
                        )
                    }
                }
            }

            // ---- coins (spinning) ----
            for (c in engine.coins) {
                if (c.collected) continue
                val spin = abs(cos(nowSec * 6f + c.y * 0.005f)).coerceIn(0.25f, 1f)
                withTransform({
                    scale(sx = spin, sy = 1f, pivot = Offset(c.x, c.y))
                }) {
                    drawCircle(Color(0xFFFFD54F), 30f, Offset(c.x, c.y))
                    drawCircle(Color(0xFFB7860B), 30f, Offset(c.x, c.y), style = Stroke(5f))
                    drawCircle(Color(0xFFFFF59D), 12f, Offset(c.x - 7f, c.y - 7f))
                }
            }

            // ---- obstacles ----
            for (o in engine.obstacles) {
                val ox = cx + engine.laneX(o.lane)
                val ow = spacing * 0.8f
                when (o.kind) {
                    ObstacleKind.BARRIER -> drawBarrier(ox, o.y, ow, GameEngine.BARRIER_H_PX)
                    ObstacleKind.OVERHEAD -> drawOverhead(
                        ox, o.y, ow,
                        GameEngine.OVERHEAD_TOP_PX, GameEngine.OVERHEAD_BOTTOM_PX
                    )
                    ObstacleKind.TRAIN -> drawTrain(ox, o.y, ow, GameEngine.TRAIN_H_PX)
                }
            }

            // ---- player ----
            val pSize = spacing * GameEngine.PLAYER_H_FRAC
            drawCharacter(
                id = characterID,
                centerX = cx + engine.playerX,
                feetY = pY - engine.jumpPx,
                size = pSize,
                rolling = engine.rolling
            )

            // ---- active power-up pips ----
            var pipX = 36f
            if (engine.magnetActive) {
                drawMagnet(pipX, 90f, 44f)
                pipX += 90f
            }
            if (engine.multiplierActive) {
                drawCircle(Color(0xFFFFD54F), 28f, Offset(pipX, 90f))
                drawText(
                    textMeasurer, "2x",
                    topLeft = Offset(pipX - 20f, 90f - 22f),
                    style = TextStyle(color = Color(0xFF3E2723), fontSize = 26.sp)
                )
            }
        }
    }
}

// ------------------------------------------------------------ pieces

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBarrier(
    cx: Float, baseY: Float, w: Float, h: Float
) {
    // striped hurdle
    drawRoundRect(
        Color(0xFFE53935),
        Offset(cx - w / 2f, baseY - h),
        Size(w, h),
        CornerRadius(14f)
    )
    val stripeW = 26f
    var x = cx - w / 2f
    val clip = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                cx - w / 2f, baseY - h, cx + w / 2f, baseY,
                androidx.compose.ui.geometry.CornerRadius(14f)
            )
        )
    }
    withTransform({ clipPath(clip) }) {
        while (x < cx + w / 2f) {
            drawLine(
                Color.White, Offset(x, baseY),
                Offset(x + stripeW, baseY - h),
                strokeWidth = 14f
            )
            x += stripeW * 2f
        }
    }
    // posts
    drawRoundRect(Color(0xFF616161), Offset(cx - w / 2f - 8f, baseY - h), Size(16f, h), CornerRadius(8f))
    drawRoundRect(Color(0xFF616161), Offset(cx + w / 2f - 8f, baseY - h), Size(16f, h), CornerRadius(8f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOverhead(
    cx: Float, baseY: Float, w: Float, topPx: Float, bottomPx: Float
) {
    val barTop = baseY - topPx
    val barH = topPx - bottomPx
    // poles
    drawRoundRect(Color(0xFF616161), Offset(cx - w / 2f - 10f, barTop), Size(20f, topPx), CornerRadius(10f))
    drawRoundRect(Color(0xFF616161), Offset(cx + w / 2f - 10f, barTop), Size(20f, topPx), CornerRadius(10f))
    // bar with hazard stripes
    drawRoundRect(Color(0xFFFFB300), Offset(cx - w / 2f, barTop), Size(w, barH), CornerRadius(12f))
    var x = cx - w / 2f
    while (x < cx + w / 2f) {
        drawLine(Color(0xFF212121), Offset(x, barTop + barH), Offset(x + 24f, barTop), strokeWidth = 12f)
        x += 48f
    }
    // warning lights
    drawCircle(Color(0xFFE53935), 12f, Offset(cx - w / 4f, barTop + barH / 2f))
    drawCircle(Color(0xFFE53935), 12f, Offset(cx + w / 4f, barTop + barH / 2f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrain(
    cx: Float, baseY: Float, w: Float, h: Float
) {
    val top = baseY - h
    // body
    drawRoundRect(
        Color(0xFF37474F),
        Offset(cx - w / 2f, top),
        Size(w, h),
        CornerRadius(28f)
    )
    drawRoundRect(
        Color(0xFF455A64),
        Offset(cx - w / 2f, top),
        Size(w, h * 0.32f),
        CornerRadius(28f)
    )
    // windshield
    drawRoundRect(
        Color(0xFFB3E5FC),
        Offset(cx - w * 0.36f, top + h * 0.06f),
        Size(w * 0.72f, h * 0.16f),
        CornerRadius(14f)
    )
    // windows
    val winY = top + h * 0.30f
    for (i in -1..1) {
        drawRoundRect(
            Color(0xFFB3E5FC).copy(alpha = 0.85f),
            Offset(cx + i * w * 0.26f - w * 0.09f, winY),
            Size(w * 0.18f, h * 0.12f),
            CornerRadius(10f)
        )
    }
    // stripe + headlights + grill
    drawRect(Color(0xFFFFD54F), Offset(cx - w / 2f, top + h * 0.48f), Size(w, h * 0.05f))
    drawCircle(Color(0xFFFFF59D), 14f, Offset(cx - w * 0.32f, baseY - h * 0.12f))
    drawCircle(Color(0xFFFFF59D), 14f, Offset(cx + w * 0.32f, baseY - h * 0.12f))
    drawRoundRect(Color(0xFF212121), Offset(cx - w * 0.2f, baseY - h * 0.10f), Size(w * 0.4f, h * 0.06f), CornerRadius(8f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagnet(
    cx: Float, cy: Float, s: Float
) {
    // horseshoe magnet: thick red arc + white tips
    drawArc(
        Color(0xFFE53935), 180f, 180f, false,
        Offset(cx - s / 2f, cy - s / 2f),
        Size(s, s),
        style = Stroke(s * 0.34f, cap = StrokeCap.Butt)
    )
    drawArc(
        Color.White, 180f, 34f, false,
        Offset(cx - s / 2f, cy - s / 2f),
        Size(s, s),
        style = Stroke(s * 0.34f, cap = StrokeCap.Butt)
    )
    drawArc(
        Color.White, 326f, 34f, false,
        Offset(cx - s / 2f, cy - s / 2f),
        Size(s, s),
        style = Stroke(s * 0.34f, cap = StrokeCap.Butt)
    )
}
