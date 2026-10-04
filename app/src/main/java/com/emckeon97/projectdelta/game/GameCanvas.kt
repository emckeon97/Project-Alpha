package com.emckeon97.projectdelta.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.emckeon97.projectdelta.characters.ROSTER
import com.emckeon97.projectdelta.characters.drawCharacter
import com.emckeon97.projectdelta.characters.rememberSpriteBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

/**
 * Full-screen 60fps perspective-3D renderer for [GameEngine].
 * World units are projected (x, y, z) -> screen with a level camera at
 * height 5, z = -8:  d = z + 8; s = f / d;
 * screenX = cx + x * s;  screenY = horizonY + (H - y) * s.
 *
 * NOTE on focal length: the design spec lists f = canvasHeight * 1.15, but
 * with H = 5 and the camera at z = -8 that places the player plane (z = 0)
 * below the visible canvas, so the runner would never be seen. f = h * 0.62
 * keeps the identical formula and every other constant while framing the
 * player's feet at ~0.81h. Revisit if art direction wants a different frame.
 *
 * Owns the frame loop; calls [onGameOver] once when the run ends.
 * Swipe input is handled by the hosting GameScreen.
 */
@Composable
fun GameRenderer(
    engine: GameEngine,
    characterID: String,
    onGameOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var tick by remember { mutableLongStateOf(0L) }
    var gameOverFired by remember { mutableStateOf(false) }
    var speedU by remember { mutableFloatStateOf(8f) }
    var lastDist by remember { mutableFloatStateOf(0f) }
    // Animation juice (mirrors iOS Player): lane lean, land squash.
    var lean by remember { mutableFloatStateOf(0f) }
    var landT by remember { mutableFloatStateOf(1f) } // 1 = inactive
    var wasAirborne by remember { mutableStateOf(false) }
    val spriteBitmap = rememberSpriteBitmap(characterID)
    val charBasePx = with(density) { 130.dp.toPx() }
    // Background cameos: 3 random roster characters (never the player), matching iOS.
    val cameoIds = remember(characterID) {
        ROSTER.map { it.id }.filter { it != characterID }.shuffled().take(3)
    }
    val cameoSprite0 = rememberSpriteBitmap(cameoIds[0])
    val cameoSprite1 = rememberSpriteBitmap(cameoIds[1])
    val cameoSprite2 = rememberSpriteBitmap(cameoIds[2])

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
                    val d = engine.distance
                    if (dtMs > 0) {
                        speedU = ((d - lastDist) / (dtMs / 1000f)).coerceIn(0f, 30f)
                    }
                    lastDist = d
                    // lane-change lean: ease toward clamp(lateral * 0.15, ±0.3)
                    val dtSec = dtMs / 1000f
                    val lateral = GameEngine.LANE_X[engine.playerLane] - engine.playerX
                    val targetLean = (lateral * 0.15f).coerceIn(-0.3f, 0.3f)
                    lean += (targetLean - lean) * minOf(1f, dtSec * 10f)
                    // land-squash trigger on touchdown
                    val airborne = engine.playerY > 0.02f
                    if (wasAirborne && !airborne && !engine.isRolling) landT = 0f
                    wasAirborne = airborne
                    if (landT < 0.22f) landT += dtSec
                } else if (engine.gameOver && !gameOverFired) {
                    // Game over fires ONLY on a real collision — never on pause.
                    gameOverFired = true
                    onGameOver()
                }
                tick = now
            }
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val tSec = tick / 1_000_000_000f // redraw trigger + animation clock
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val horizonY = h * 0.42f
        val camH = 5f
        val camZ = -8f
        val f = h * 0.62f
        val sPlayer = f / 8f

        fun proj(x: Float, y: Float, z: Float): Offset? {
            val d = z - camZ
            if (d <= 0.5f) return null // behind (or inside) the camera
            val s = f / d
            return Offset(cx + x * s, horizonY + (camH - y) * s)
        }
        fun scaleAt(z: Float): Float = f / (z - camZ)

        // ---- sky: near-black navy, like an old cartoon reel at night ----
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF04060E), Color(0xFF0A0D1F)),
                endY = horizonY
            ),
            size = Size(w, horizonY)
        )
        // ---- stars (fixed seed: no flicker) ----
        val starRand = kotlin.random.Random(42)
        repeat(44) {
            val sx = starRand.nextFloat() * w
            val sy = starRand.nextFloat() * horizonY * 0.92f
            val sr = 1f + starRand.nextFloat() * 1.6f
            val tw = 0.35f + 0.65f * abs(sin(tSec * (1f + starRand.nextFloat() * 2f) + it))
            drawCircle(Color.White.copy(alpha = 0.75f * tw), sr, Offset(sx, sy))
        }
        // ---- moon + halo ----
        val moonX = w * 0.22f
        val moonY = h * 0.13f
        val moonR = h * 0.045f
        drawCircle(Color(0xFFF5EFE0).copy(alpha = 0.10f), moonR * 2.6f, Offset(moonX, moonY))
        drawCircle(Color(0xFFF5EFE0).copy(alpha = 0.16f), moonR * 1.7f, Offset(moonX, moonY))
        drawCircle(Color(0xFFF2EBD8), moonR, Offset(moonX, moonY))
        // ---- water: one dark plane under everything ----
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0A0F22), Color(0xFF04060D)),
                startY = horizonY
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(w, h - horizonY)
        )
        // ---- moonlight path shimmering on the water ----
        for (i in 0..7) {
            val fy = i / 7f
            val ry = horizonY + (h - horizonY) * (0.08f + 0.88f * fy * fy)
            val rw = (10f + 46f * fy) * (0.75f + 0.25f * sin(tSec * 2.2f + i * 1.7f))
            drawLine(
                Color(0xFFF5EFE0).copy(alpha = 0.10f * (1f - fy * 0.6f)),
                Offset(moonX - rw, ry), Offset(moonX + rw, ry),
                strokeWidth = 3f + 5f * fy
            )
        }
        // ---- scrolling moonlit wave dashes (2 rows per side, like iOS) ----
        val waveMod = engine.distance % 4f
        for (m in -1..17) {
            val zLine = m * 4f - waveMod
            if (zLine < -6f) continue
            for (wx in floatArrayOf(-11f, -7f, 7f, 11f)) {
                val wob = sin(wx * 12.9898f + m * 78.233f) * 0.7f
                val a = proj(wx + wob - 0.85f, -0.5f, zLine) ?: continue
                val b = proj(wx + wob + 0.85f, -0.5f, zLine) ?: continue
                drawLine(
                    Color.White.copy(alpha = 0.16f), a, b,
                    strokeWidth = (scaleAt(zLine) * 0.10f).coerceIn(1f, 8f),
                    cap = StrokeCap.Round
                )
            }
        }
        drawLine(
            Color(0xFF6A5ACD).copy(alpha = 0.25f),
            Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 2f
        )

        // ---- distant riverboat (ambient life, matches iOS) ----
        val boatX = 16f + sin(tSec * 0.1f) * 3f
        val boatZ = 80f
        val boatHullF = Color(0xFF1A2030); val boatHullT = Color(0xFF232B40); val boatHullS = Color(0xFF11141F)
        val boatCabF = Color(0xFF8E93A6); val boatCabT = Color(0xFFA6ACBF); val boatCabS = Color(0xFF6B7085)
        drawShadedBox(::proj, boatX, 0f, 1.5f, boatZ, 8f, 20f, boatHullF, boatHullT, boatHullS)
        drawShadedBox(::proj, boatX, 1.5f, 4f, boatZ, 6f, 14f, boatCabF, boatCabT, boatCabS)
        drawShadedBox(::proj, boatX - 1.5f, 4f, 7f, boatZ - 2f, 1f, 1f, boatHullF, boatHullT, boatHullS)

        // ---- wooden pier deck (perspective quad) ----
        // Near edge starts behind the camera plane so the bridge fills the
        // bottom of the screen (no black strip below the player).
        val slab = Path().apply {
            val a = proj(-3.6f, 0f, -6f); val b = proj(3.6f, 0f, -6f)
            val c = proj(3.6f, 0f, 64f); val dPt = proj(-3.6f, 0f, 64f)
            if (a != null && b != null && c != null && dPt != null) {
                moveTo(a.x, a.y); lineTo(b.x, b.y)
                lineTo(c.x, c.y); lineTo(dPt.x, dPt.y); close()
            }
        }
        drawPath(slab, Color(0xFF33291F))

        // ---- deck planks scrolling under the player ----
        val plankMod = engine.distance % 2f
        for (m in -3..33) {
            val zLine = m * 2f - plankMod
            if (zLine < -6f) continue
            val a = proj(-3.55f, 0f, zLine) ?: continue
            val b = proj(3.55f, 0f, zLine) ?: continue
            val sw = (scaleAt(zLine) * 0.45f).coerceIn(1.5f, 30f)
            drawLine(
                Color(0xFF5C4F45).copy(alpha = 0.85f), a, b,
                strokeWidth = sw, cap = StrokeCap.Butt
            )
        }

        // ---- pier railings ----
        val railF = Color(0xFF3D332B); val railT = Color(0xFF4A3F36); val railS = Color(0xFF2C251F)
        drawShadedBox(::proj, -3.62f, 1.02f, 1.22f, 29f, 0.16f, 70f, railF, railT, railS)
        drawShadedBox(::proj, 3.62f, 1.02f, 1.22f, 29f, 0.16f, 70f, railF, railT, railS)
        val postMod = engine.distance % 8f
        var pz = -4f - postMod
        while (pz < 64f) {
            if (pz > -6f) {
                drawShadedBox(::proj, -3.62f, 0f, 1.1f, pz, 0.16f, 0.16f, railF, railT, railS)
                drawShadedBox(::proj, 3.62f, 0f, 1.1f, pz, 0.16f, 0.16f, railF, railT, railS)
            }
            pz += 8f
        }

        // ---- lane dividers converging to the vanishing point ----
        for (dx in floatArrayOf(-3.4f, -1.1f, 1.1f, 3.4f)) {
            val a = proj(dx, 0f, -6f) ?: continue
            val b = proj(dx, 0f, 64f) ?: continue
            val edge = abs(dx) > 2f
            drawLine(
                Color(0xFFFFD54F).copy(alpha = if (edge) 0.15f else 0.25f),
                a, b, strokeWidth = 4f
            )
        }

        // ---- background cameos: 2 waving from the pier edges, 1 on the riverboat ----
        val cameoSprites = listOf(cameoSprite0, cameoSprite1, cameoSprite2)
        val cameoSpots = listOf(
            Triple(-3.3f, 0f, 16f),
            Triple(3.3f, 0f, 16f),
            Triple(boatX, 4.2f, boatZ)
        )
        for (i in cameoSpots.indices) {
            val (cx, cy, cz) = cameoSpots[i]
            val bobY = cy + sin(tSec * 2.8f + i * 2.1f) * 0.12f
            val p = proj(cx, bobY, cz) ?: continue
            val sizeMul = if (i == 2) 2.2f else 1f
            val size = charBasePx * 8f / (cz + 8f) * sizeMul
            drawCharacter(cameoIds[i], p.x, p.y, size, rolling = false, sprite = cameoSprites[i])
        }

        // ---- entities, far to near ----
        val items = ArrayList<DrawItem>(64)
        for (o in engine.obstacles) {
            if (o.lane !in 0..2) continue
            val lx = GameEngine.LANE_X[o.lane]
            val zKey = if (o.kind == ObstacleKind.TRAIN) o.z - o.depth else o.z - 0.5f
            items.add(ObItem(zKey, o, lx))
        }
        for (c in engine.coins) {
            if (!c.collected) items.add(CoinItem(c.z, c))
        }
        for (p in engine.powerUps) {
            if (!p.taken) items.add(PowItem(p.z, p))
        }
        items.add(PlayerItem(0f))
        items.sortByDescending { it.zKey }

        for (item in items) {
            when (item) {
                is ObItem -> drawObstacle(item.o, item.lx, tSec, ::proj, ::scaleAt)
                is CoinItem -> drawCoin(item.c, tSec, ::proj, ::scaleAt)
                is PowItem -> drawPowerUp(item.p, ::proj, ::scaleAt, textMeasurer, density)
                is PlayerItem -> {
                    // subtle running bob (mirrors iOS runPhase)
                    val bobY = if (!engine.isRolling && engine.playerY < 0.02f)
                        sin(tSec * 14f) * 0.05f else 0f
                    val feet = proj(engine.playerX, engine.playerY + bobY, 0f)
                    if (feet != null) {
                        val s = scaleAt(0f)
                        // jump stretch / land squash (never while rolling)
                        var sx = 1f
                        var sy = 1f
                        if (!engine.isRolling) {
                            if (engine.playerY > 0.02f) {
                                val js = (engine.playerY / GameEngine.JUMP_HEIGHT).coerceIn(0f, 1f)
                                sx = 1f - 0.07f * js
                                sy = 1f + 0.10f * js
                            } else if (landT < 0.22f) {
                                val k = 1f - landT / 0.22f
                                sx = 1f + 0.10f * k
                                sy = 1f - 0.16f * k
                            }
                        }
                        val pivot = Offset(feet.x, feet.y)
                        withTransform({
                            // iOS eulerAngles.z is CCW in y-up; Canvas y is down, so negate
                            rotate(degrees = -lean * 57.2958f, pivot = pivot)
                            scale(sx, sy, pivot = pivot)
                        }) {
                            drawCharacter(
                                id = characterID,
                                centerX = feet.x,
                                feetY = feet.y,
                                size = charBasePx * s / sPlayer,
                                rolling = engine.isRolling,
                                sprite = spriteBitmap
                            )
                        }
                        // shadow, fading as the jump rises
                        val g = proj(engine.playerX, 0f, 0f)
                        if (g != null) {
                            val fade = (1f - (engine.playerY / 4f).coerceIn(0f, 1f))
                            drawOval(
                                Color.Black.copy(alpha = 0.35f * fade),
                                Offset(g.x - 0.9f * s, g.y - 0.12f * s),
                                Size(1.8f * s, 0.24f * s)
                            )
                        }
                    }
                }
            }
        }

        // ---- speed streaks at the edges ----
        if (speedU > 13f) {
            val alpha = ((speedU - 13f) / 9f).coerceIn(0f, 1f) * 0.14f
            for (i in 0 until 10) {
                val sx = (if (i % 2 == 0) 0.04f else 0.96f) * w + (i % 3) * 14f
                val sy = ((i * 197f + tSec * 900f) % (h * 1.2f)) - h * 0.1f
                drawLine(
                    Color.White.copy(alpha = alpha),
                    Offset(sx, sy), Offset(sx, sy + 110f),
                    strokeWidth = 5f, cap = StrokeCap.Round
                )
            }
        }

        // ---- active power-up pips ----
        var pipX = 44f
        if (engine.magnetActive) {
            drawArc(
                Color(0xFFE53935), 180f, 180f, false,
                Offset(pipX - 18f, 66f), Size(36f, 36f),
                style = Stroke(12f, cap = StrokeCap.Butt)
            )
            pipX += 68f
        }
        if (engine.doubleScore) {
            drawCircle(Color(0xFFFFD54F), 24f, Offset(pipX, 84f))
            val fs = with(density) { 24.dp.toSp() }
            val label = textMeasurer.measure(
                "2x", style = TextStyle(fontSize = fs, color = Color(0xFF3E2723))
            )
            drawText(
                textMeasurer, "2x",
                topLeft = Offset(pipX - label.size.width / 2f, 84f - label.size.height / 2f),
                style = TextStyle(fontSize = fs, color = Color(0xFF3E2723))
            )
        }
    }
}

// ------------------------------------------------------------ draw items

private sealed interface DrawItem { val zKey: Float }
private data class ObItem(override val zKey: Float, val o: Obstacle, val lx: Float) : DrawItem
private data class CoinItem(override val zKey: Float, val c: Coin) : DrawItem
private data class PowItem(override val zKey: Float, val p: PowerUp) : DrawItem
private data class PlayerItem(override val zKey: Float) : DrawItem

private fun quadPath(a: Offset, b: Offset, c: Offset, d: Offset): Path =
    Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y)
        lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }

private fun lerpOff(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

/** Projects and draws a shaded 3D box. Returns the 8 projected corners, or null if skipped. */
private fun DrawScope.drawShadedBox(
    proj: (Float, Float, Float) -> Offset?,
    bcx: Float, y0: Float, y1: Float, zc: Float, w: Float, depth: Float,
    front: Color, top: Color, side: Color
): Array<Offset>? {
    val x0 = bcx - w / 2f
    val x1 = bcx + w / 2f
    val zN = zc - depth / 2f
    val zF = zc + depth / 2f
    val pts = listOf(
        proj(x0, y0, zN), proj(x1, y0, zN), proj(x1, y1, zN), proj(x0, y1, zN),
        proj(x0, y0, zF), proj(x1, y0, zF), proj(x1, y1, zF), proj(x0, y1, zF)
    ).filterNotNull()
    if (pts.size != 8) return null
    // side face toward the track center, then top, then front (nearest last)
    if (bcx < 0f) drawPath(quadPath(pts[1], pts[5], pts[6], pts[2]), side)
    else drawPath(quadPath(pts[0], pts[4], pts[7], pts[3]), side)
    drawPath(quadPath(pts[2], pts[6], pts[7], pts[3]), top)
    drawPath(quadPath(pts[0], pts[1], pts[2], pts[3]), front)
    return pts.toTypedArray()
}

/** Vertical stripes across a projected front face (corners q0..q3 = BL, BR, TR, TL). */
private fun DrawScope.stripeFrontFace(
    q: Array<Offset>, stripes: Int, color: Color
) {
    val q0 = q[0]; val q1 = q[1]; val q2 = q[2]; val q3 = q[3]
    var i = 0
    while (i < stripes) {
        val t0 = i / stripes.toFloat()
        val t1 = (i + 1) / stripes.toFloat()
        drawPath(
            quadPath(
                lerpOff(q0, q1, t0), lerpOff(q0, q1, t1),
                lerpOff(q3, q2, t1), lerpOff(q3, q2, t0)
            ),
            color
        )
        i += 2
    }
}

private fun DrawScope.drawObstacle(
    o: Obstacle,
    lx: Float,
    tSec: Float,
    proj: (Float, Float, Float) -> Offset?,
    scaleAt: (Float) -> Float
) {
    // soft contact shadow
    val sh = proj(lx, 0f, o.z)
    if (sh != null) {
        val ss = scaleAt(o.z)
        drawOval(
            Color.Black.copy(alpha = 0.28f),
            Offset(sh.x - 1.1f * ss, sh.y - 0.08f * ss),
            Size(2.2f * ss, 0.16f * ss)
        )
    }
    // 1930s steamboat-river palette (matches iOS)
    val woodF = Color(0xFF5C4F45); val woodT = Color(0xFF6B5D4F); val woodS = Color(0xFF463C33)
    val woodDarkF = Color(0xFF3D332B); val woodDarkT = Color(0xFF4A3F36); val woodDarkS = Color(0xFF2C251F)
    val hullF = Color(0xFF1C1C1E); val hullT = Color(0xFF2A2A2C); val hullS = Color(0xFF101012)
    val cabinF = Color(0xFFB5B0A6); val cabinT = Color(0xFFCFC9BC); val cabinS = Color(0xFF8E887A)
    val trimF = Color(0xFF7A7468); val trimT = Color(0xFF8E887A); val trimS = Color(0xFF5C574C)
    val stackF = Color(0xFF141414); val stackT = Color(0xFF1E1E1E); val stackS = Color(0xFF0A0A0A)
    val warm = Color(0xFFFFEBBF)
    val wheelRed = Color(0xFF9E2924)
    val smoke = Color(0xFFB0A89C).copy(alpha = 0.45f)
    when (o.kind) {
        // Rowboat drifting across the pier — jump it (top at y≈1.0).
        ObstacleKind.BARRIER -> {
            drawShadedBox(proj, lx, 0.15f, 0.85f, o.z, 2.6f, 1.2f, woodF, woodT, woodS) ?: return
            // tapered bow / stern
            drawShadedBox(proj, lx - 1.45f, 0.2f, 0.8f, o.z, 0.7f, 1.0f, woodF, woodT, woodS)
            drawShadedBox(proj, lx + 1.45f, 0.2f, 0.8f, o.z, 0.7f, 1.0f, woodF, woodT, woodS)
            // gunwale rim
            drawShadedBox(proj, lx, 0.84f, 0.96f, o.z, 2.75f, 1.3f, woodDarkF, woodDarkT, woodDarkS)
            // bench seats
            drawShadedBox(proj, lx - 0.55f, 0.7f, 0.78f, o.z, 0.28f, 1.05f, woodDarkF, woodDarkT, woodDarkS)
            drawShadedBox(proj, lx + 0.55f, 0.7f, 0.78f, o.z, 0.28f, 1.05f, woodDarkF, woodDarkT, woodDarkS)
            // oar resting across the boat
            drawShadedBox(proj, lx + 0.1f, 0.95f, 1.02f, o.z, 2.4f, 0.07f, woodDarkF, woodDarkT, woodDarkS)
        }
        // Low wooden footbridge — roll under it (beam spans y 1.7–2.1).
        ObstacleKind.OVERHEAD -> {
            drawShadedBox(proj, lx - 1.05f, 0f, 2.6f, o.z, 0.22f, 0.22f, woodF, woodT, woodS)
            drawShadedBox(proj, lx + 1.05f, 0f, 2.6f, o.z, 0.22f, 0.22f, woodF, woodT, woodS)
            val q = drawShadedBox(
                proj, lx, 1.72f, 2.08f, o.z, 2.35f, 0.9f, woodF, woodT, woodS
            ) ?: return
            stripeFrontFace(q, 5, woodDarkF) // plank seams
            // hanging lantern — a warm glow in the monochrome world
            drawShadedBox(proj, lx, 1.42f, 1.72f, o.z, 0.05f, 0.05f, woodDarkF, woodDarkT, woodDarkS)
            val lp = proj(lx, 1.38f, o.z)
            if (lp != null) {
                val ls = scaleAt(o.z)
                drawCircle(warm.copy(alpha = 0.25f), 0.34f * ls, lp)
                drawCircle(warm, 0.14f * ls, lp)
            }
        }
        // Paddle-wheeler steamboat blocking the lane — dodge! (~3.2 tall, 6 long).
        ObstacleKind.TRAIN -> {
            // z marks the far face; the boat extends `depth` toward the player
            val depth = o.depth
            val zc = o.z - depth / 2f
            drawShadedBox(proj, lx, 0f, 1.0f, zc, 2.0f, depth, hullF, hullT, hullS) ?: return
            // tapered bow at the near end
            drawShadedBox(proj, lx, 0f, 0.9f, zc - 2.4f, 1.4f, 1.2f, hullF, hullT, hullS)
            // stacked cabins
            val q = drawShadedBox(proj, lx, 1.0f, 1.9f, zc, 1.7f, 4.6f, cabinF, cabinT, cabinS)
            drawShadedBox(proj, lx, 1.9f, 2.02f, zc, 1.9f, 4.8f, trimF, trimT, trimS)
            drawShadedBox(proj, lx, 2.02f, 2.7f, zc, 1.4f, 3.4f, cabinF, cabinT, cabinS)
            drawShadedBox(proj, lx, 2.7f, 2.82f, zc, 1.6f, 3.6f, trimF, trimT, trimS)
            // lit cabin windows (warm dots in the monochrome world)
            if (q != null) {
                val q0 = q[0]; val q1 = q[1]; val q2 = q[2]; val q3 = q[3]
                fun facePt(fx: Float, fy: Float): Offset =
                    lerpOff(lerpOff(q0, q1, fx), lerpOff(q3, q2, fx), fy)
                fun window(fx0: Float, fx1: Float) {
                    drawPath(
                        quadPath(
                            facePt(fx0, 0.3f), facePt(fx1, 0.3f),
                            facePt(fx1, 0.62f), facePt(fx0, 0.62f)
                        ),
                        warm
                    )
                }
                window(0.14f, 0.30f); window(0.42f, 0.58f); window(0.70f, 0.86f)
            }
            // smokestacks with caps (rear)
            val stackZ = zc - 1.2f
            drawShadedBox(proj, lx - 0.4f, 2.82f, 4.1f, stackZ, 0.36f, 0.36f, stackF, stackT, stackS)
            drawShadedBox(proj, lx + 0.4f, 2.82f, 4.1f, stackZ, 0.36f, 0.36f, stackF, stackT, stackS)
            drawShadedBox(proj, lx - 0.4f, 4.1f, 4.25f, stackZ, 0.48f, 0.48f, stackF, stackT, stackS)
            drawShadedBox(proj, lx + 0.4f, 4.1f, 4.25f, stackZ, 0.48f, 0.48f, stackF, stackT, stackS)
            // drifting smoke puffs
            val sSmoke = scaleAt(stackZ)
            val puffX = floatArrayOf(lx - 0.4f, lx + 0.4f, lx)
            for (i in 0..2) {
                val px = puffX[i] + sin(tSec * 1.3f + i * 2.1f) * 0.18f
                val py = 4.55f + i * 0.38f
                val pp = proj(px, py, stackZ) ?: continue
                drawCircle(smoke, (0.30f + i * 0.13f) * sSmoke, pp)
            }
            // paddle wheel on the visible side (the one red accent)
            val side = if (lx < 0f) 1f else -1f
            val wp = proj(lx + side * 1.02f, 0.95f, zc)
            if (wp != null) {
                val ws = scaleAt(zc)
                val wr = 0.75f * ws
                drawCircle(wheelRed, wr, wp, style = Stroke(width = 0.13f * ws))
                for (i in 0..3) {
                    val a = i * PI.toFloat() / 4f
                    val ex = wp.x + cos(a) * wr
                    val ey = wp.y + sin(a) * wr
                    drawLine(wheelRed, wp, Offset(ex, ey), strokeWidth = 0.07f * ws)
                }
                drawCircle(wheelRed, 0.12f * ws, wp)
            }
        }
    }
}

private fun DrawScope.drawCoin(
    c: Coin,
    tSec: Float,
    proj: (Float, Float, Float) -> Offset?,
    scaleAt: (Float) -> Float
) {
    val bobY = c.y + sin(tSec * 3f + c.z * 0.7f) * 0.12f
    val p = proj(c.x, bobY, c.z) ?: return
    val s = scaleAt(c.z)
    val r = 0.42f * s
    val spin = abs(cos(tSec * 6f + c.x * 2f + c.z * 0.5f)).coerceAtLeast(0.15f)
    drawOval(Color(0xFFFFD54F), Offset(p.x - r * spin, p.y - r), Size(r * 2f * spin, r * 2f))
    drawOval(
        Color(0xFFFFF59D),
        Offset(p.x - r * spin * 0.45f, p.y - r * 0.55f),
        Size(r * 0.9f * spin, r * 0.9f)
    )
}

private fun DrawScope.drawPowerUp(
    p: PowerUp,
    proj: (Float, Float, Float) -> Offset?,
    scaleAt: (Float) -> Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density
) {
    val c = proj(p.x, 1.2f, p.z) ?: return
    val s = scaleAt(p.z)
    val r = 0.55f * s
    drawCircle(Color(0xFF2A1B4E), r, c)
    drawCircle(Color(0xFFFFD54F), r, c, style = Stroke(r * 0.12f))
    when (p.kind) {
        PowerUpKind.MAGNET -> {
            drawArc(
                Color(0xFFE53935), 180f, 180f, false,
                Offset(c.x - r * 0.5f, c.y - r * 0.5f), Size(r, r),
                style = Stroke(r * 0.36f, cap = StrokeCap.Butt)
            )
        }
        PowerUpKind.MULTIPLIER -> {
            val fs = with(density) { (r * 0.8f).toSp() }
            val style = TextStyle(fontSize = fs, color = Color(0xFFFFD54F))
            val label = textMeasurer.measure("2x", style = style)
            drawText(
                textMeasurer, "2x",
                topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f),
                style = style
            )
        }
    }
}
