package com.emckeon97.projectdelta.characters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

private val ink = Color(0xFF161616)
private val paper = Color(0xFFFFFFFF)
private val toonRed = Color(0xFFE53935)
private val gold = Color(0xFFF2B234)
private val denim = Color(0xFF3D6DB5)
private val tan = Color(0xFFD9A066)
private val brown = Color(0xFF6D4C41)

/**
 * Rubber-hose cartoon rendering of the public-domain roster.
 * All likenesses are original code-drawn interpretations.
 *
 * Characters with bundled sprite art ([sprite] != null) draw the bitmap;
 * willie/betty/minnie have no PNGs and keep the primitive drawing below.
 *
 * @param centerX horizontal center of the character (px)
 * @param feetY y of the character's feet / ground contact (px)
 * @param size total character height (px)
 * @param rolling when true, draws a compact tumbling ball instead
 * @param rollSpin rotation of the tumbling ball, in degrees
 */
fun DrawScope.drawCharacter(
    id: String,
    centerX: Float,
    feetY: Float,
    size: Float,
    rolling: Boolean,
    rollSpin: Float = 0f,
    sprite: ImageBitmap? = null
) {
    if (rolling) {
        drawRollBall(id, centerX, feetY, size, spin = rollSpin)
        return
    }
    if (sprite != null) {
        drawSpriteCharacter(sprite, centerX, feetY, size)
        return
    }
    when (id) {
        "willie" -> drawWillie(centerX, feetY, size)
        "felix" -> drawFelix(centerX, feetY, size)
        "oswald" -> drawOswald(centerX, feetY, size)
        "popeye" -> drawPopeye(centerX, feetY, size)
        "pooh" -> drawPooh(centerX, feetY, size)
        "betty" -> drawBetty(centerX, feetY, size)
        else -> drawWillie(centerX, feetY, size)
    }
}

/** Draws a bundled sprite bitmap, ~[size] px tall, aspect-preserved, feet at [feetY]. */
private fun DrawScope.drawSpriteCharacter(
    sprite: ImageBitmap,
    centerX: Float,
    feetY: Float,
    size: Float
) {
    val h = size
    val w = h * sprite.width / sprite.height
    drawImage(
        image = sprite,
        dstOffset = IntOffset((centerX - w / 2f).roundToInt(), (feetY - h).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt())
    )
}

// ---------------------------------------------------------------- helpers

private fun DrawScope.limb(
    from: Offset,
    to: Offset,
    width: Float,
    color: Color = ink
) {
    drawLine(color, from, to, width, StrokeCap.Round)
}

private fun DrawScope.rrect(
    color: Color,
    cx: Float,
    top: Float,
    w: Float,
    h: Float,
    r: Float
) {
    drawRoundRect(color, Offset(cx - w / 2f, top), Size(w, h), androidx.compose.ui.geometry.CornerRadius(r))
}

private fun DrawScope.oval(color: Color, cx: Float, cy: Float, w: Float, h: Float, style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill) {
    drawOval(color, Offset(cx - w / 2f, cy - h / 2f), Size(w, h), style = style)
}

private fun DrawScope.pieEye(cx: Float, cy: Float, r: Float) {
    // white of the eye
    drawCircle(paper, r, Offset(cx, cy))
    drawCircle(ink, r, Offset(cx, cy), style = Stroke(r * 0.22f))
    // pie-cut pupil: black wedge
    drawArc(
        ink, 20f, 110f, true,
        Offset(cx - r * 0.45f, cy - r * 0.45f),
        Size(r * 0.9f, r * 0.9f)
    )
}

private fun DrawScope.smile(cx: Float, cy: Float, w: Float, color: Color = ink, width: Float = 6f) {
    drawArc(
        color, 15f, 150f, false,
        Offset(cx - w / 2f, cy - w / 4f),
        Size(w, w / 2f),
        style = Stroke(width, cap = StrokeCap.Round)
    )
}

// ------------------------------------------------------- rolling ball

private fun DrawScope.drawRollBall(id: String, cx: Float, feetY: Float, size: Float, spin: Float = 0f) {
    val r = size * 0.30f
    val cy = feetY - r
    val base = when (id) {
        "felix" -> ink
        "pooh" -> gold
        "betty" -> toonRed
        "popeye" -> paper
        "oswald" -> Color(0xFF2B2B2B)
        else -> ink
    }
    // motion streaks (static speed lines)
    limb(Offset(cx - r * 1.6f, cy - r * 0.4f), Offset(cx - r * 1.1f, cy - r * 0.4f), r * 0.16f, paper.copy(alpha = 0.5f))
    limb(Offset(cx - r * 1.7f, cy + r * 0.3f), Offset(cx - r * 1.1f, cy + r * 0.3f), r * 0.16f, paper.copy(alpha = 0.5f))
    // the ball itself tumbles around its own center
    withTransform({ rotate(spin, pivot = Offset(cx, cy)) }) {
        drawCircle(base, r, Offset(cx, cy))
        drawCircle(ink, r, Offset(cx, cy), style = Stroke(r * 0.12f))
        // signature accent so the ball still reads as the character
        when (id) {
            "willie", "felix", "oswald" -> {
                drawCircle(paper, r * 0.42f, Offset(cx, cy - r * 0.1f))
                pieEye(cx - r * 0.16f, cy - r * 0.14f, r * 0.16f)
                pieEye(cx + r * 0.16f, cy - r * 0.14f, r * 0.16f)
            }
            "popeye" -> {
                drawCircle(paper, r * 0.5f, Offset(cx, cy))
                rrect(paper, cx, cy - r * 1.05f, r * 0.9f, r * 0.34f, r * 0.12f)
            }
            "pooh" -> {
                drawCircle(toonRed, r * 0.55f, Offset(cx, cy + r * 0.25f))
            }
            "betty" -> {
                drawCircle(ink, r * 0.62f, Offset(cx, cy - r * 0.28f))
                drawCircle(paper, r * 0.4f, Offset(cx, cy + r * 0.05f))
            }
        }
    }
}

// ------------------------------------------------------------- willie

private fun DrawScope.drawWillie(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.035f
    // shoes
    rrect(tan, cx - s * 0.13f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    rrect(tan, cx + s * 0.13f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    // legs
    limb(Offset(cx - s * 0.09f, feetY - s * 0.26f), Offset(cx - s * 0.13f, feetY - s * 0.10f), lw)
    limb(Offset(cx + s * 0.09f, feetY - s * 0.26f), Offset(cx + s * 0.13f, feetY - s * 0.10f), lw)
    // red shorts
    rrect(toonRed, cx, feetY - s * 0.42f, s * 0.36f, s * 0.18f, s * 0.07f)
    drawLine(paper, Offset(cx - s * 0.18f, feetY - s * 0.33f), Offset(cx + s * 0.18f, feetY - s * 0.33f), s * 0.02f)
    // torso
    rrect(ink, cx, feetY - s * 0.62f, s * 0.32f, s * 0.22f, s * 0.10f)
    oval(paper, cx, feetY - s * 0.55f, s * 0.14f, s * 0.16f) // chest patch
    // arms + gloves
    limb(Offset(cx - s * 0.16f, feetY - s * 0.56f), Offset(cx - s * 0.30f, feetY - s * 0.42f), lw)
    limb(Offset(cx + s * 0.16f, feetY - s * 0.56f), Offset(cx + s * 0.30f, feetY - s * 0.42f), lw)
    drawCircle(paper, s * 0.075f, Offset(cx - s * 0.31f, feetY - s * 0.40f))
    drawCircle(paper, s * 0.075f, Offset(cx + s * 0.31f, feetY - s * 0.40f))
    // ears
    drawCircle(ink, s * 0.115f, Offset(cx - s * 0.17f, feetY - s * 0.90f))
    drawCircle(ink, s * 0.115f, Offset(cx + s * 0.17f, feetY - s * 0.90f))
    // head
    oval(ink, cx, feetY - s * 0.74f, s * 0.44f, s * 0.38f)
    // face
    oval(paper, cx, feetY - s * 0.68f, s * 0.32f, s * 0.26f)
    // pie eyes
    pieEye(cx - s * 0.085f, feetY - s * 0.76f, s * 0.062f)
    pieEye(cx + s * 0.085f, feetY - s * 0.76f, s * 0.062f)
    // snout + nose + smile
    oval(paper, cx, feetY - s * 0.635f, s * 0.20f, s * 0.13f)
    oval(ink, cx, feetY - s * 0.665f, s * 0.10f, s * 0.07f)
    smile(cx, feetY - s * 0.60f, s * 0.16f, ink, s * 0.022f)
}

// ------------------------------------------------------------- felix

private fun DrawScope.drawFelix(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.035f
    // tail (curved)
    val tail = Path().apply {
        moveTo(cx + s * 0.14f, feetY - s * 0.30f)
        quadraticTo(cx + s * 0.42f, feetY - s * 0.34f, cx + s * 0.38f, feetY - s * 0.58f)
    }
    drawPath(tail, ink, style = Stroke(lw * 1.6f, cap = StrokeCap.Round))
    // feet
    oval(ink, cx - s * 0.11f, feetY - s * 0.05f, s * 0.20f, s * 0.10f)
    oval(ink, cx + s * 0.11f, feetY - s * 0.05f, s * 0.20f, s * 0.10f)
    // body
    rrect(ink, cx, feetY - s * 0.52f, s * 0.34f, s * 0.44f, s * 0.15f)
    // arms
    limb(Offset(cx - s * 0.17f, feetY - s * 0.44f), Offset(cx - s * 0.28f, feetY - s * 0.30f), lw)
    limb(Offset(cx + s * 0.17f, feetY - s * 0.44f), Offset(cx + s * 0.28f, feetY - s * 0.30f), lw)
    // pointy ears
    val earL = Path().apply {
        moveTo(cx - s * 0.20f, feetY - s * 0.78f)
        lineTo(cx - s * 0.24f, feetY - s * 1.00f)
        lineTo(cx - s * 0.06f, feetY - s * 0.84f)
        close()
    }
    val earR = Path().apply {
        moveTo(cx + s * 0.20f, feetY - s * 0.78f)
        lineTo(cx + s * 0.24f, feetY - s * 1.00f)
        lineTo(cx + s * 0.06f, feetY - s * 0.84f)
        close()
    }
    drawPath(earL, ink)
    drawPath(earR, ink)
    // head
    drawCircle(ink, s * 0.21f, Offset(cx, feetY - s * 0.72f))
    // huge white eyes
    oval(paper, cx - s * 0.09f, feetY - s * 0.76f, s * 0.13f, s * 0.17f)
    oval(paper, cx + s * 0.09f, feetY - s * 0.76f, s * 0.13f, s * 0.17f)
    drawCircle(ink, s * 0.032f, Offset(cx - s * 0.09f, feetY - s * 0.73f))
    drawCircle(ink, s * 0.032f, Offset(cx + s * 0.09f, feetY - s * 0.73f))
    // nose + wide grin
    oval(ink, cx, feetY - s * 0.655f, s * 0.06f, s * 0.045f)
    drawArc(
        paper, 10f, 160f, false,
        Offset(cx - s * 0.14f, feetY - s * 0.68f),
        Size(s * 0.28f, s * 0.14f),
        style = Stroke(s * 0.03f, cap = StrokeCap.Round)
    )
}

// ------------------------------------------------------------- oswald

private fun DrawScope.drawOswald(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.035f
    // long floppy ears
    oval(ink, cx - s * 0.16f, feetY - s * 0.94f, s * 0.13f, s * 0.30f)
    oval(ink, cx + s * 0.16f, feetY - s * 0.94f, s * 0.13f, s * 0.30f)
    // feet
    rrect(ink, cx - s * 0.12f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    rrect(ink, cx + s * 0.12f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    // legs
    limb(Offset(cx - s * 0.08f, feetY - s * 0.24f), Offset(cx - s * 0.12f, feetY - s * 0.10f), lw)
    limb(Offset(cx + s * 0.08f, feetY - s * 0.24f), Offset(cx + s * 0.12f, feetY - s * 0.10f), lw)
    // body + blue shorts
    rrect(ink, cx, feetY - s * 0.58f, s * 0.32f, s * 0.36f, s * 0.14f)
    rrect(denim, cx, feetY - s * 0.40f, s * 0.34f, s * 0.18f, s * 0.07f)
    // arms
    limb(Offset(cx - s * 0.16f, feetY - s * 0.52f), Offset(cx - s * 0.28f, feetY - s * 0.38f), lw)
    limb(Offset(cx + s * 0.16f, feetY - s * 0.52f), Offset(cx + s * 0.28f, feetY - s * 0.38f), lw)
    drawCircle(paper, s * 0.07f, Offset(cx - s * 0.29f, feetY - s * 0.36f))
    drawCircle(paper, s * 0.07f, Offset(cx + s * 0.29f, feetY - s * 0.36f))
    // head
    oval(ink, cx, feetY - s * 0.72f, s * 0.40f, s * 0.34f)
    oval(paper, cx, feetY - s * 0.66f, s * 0.28f, s * 0.22f)
    // pie eyes
    pieEye(cx - s * 0.08f, feetY - s * 0.74f, s * 0.058f)
    pieEye(cx + s * 0.08f, feetY - s * 0.74f, s * 0.058f)
    oval(ink, cx, feetY - s * 0.63f, s * 0.09f, s * 0.06f)
    smile(cx, feetY - s * 0.585f, s * 0.15f, ink, s * 0.022f)
}

// ------------------------------------------------------------- popeye

private fun DrawScope.drawPopeye(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.035f
    // shoes
    rrect(ink, cx - s * 0.12f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    rrect(ink, cx + s * 0.12f, feetY - s * 0.10f, s * 0.20f, s * 0.10f, s * 0.05f)
    // denim legs
    limb(Offset(cx - s * 0.08f, feetY - s * 0.28f), Offset(cx - s * 0.12f, feetY - s * 0.10f), lw * 1.4f, denim)
    limb(Offset(cx + s * 0.08f, feetY - s * 0.28f), Offset(cx + s * 0.12f, feetY - s * 0.10f), lw * 1.4f, denim)
    // black shirt torso
    rrect(ink, cx, feetY - s * 0.60f, s * 0.36f, s * 0.34f, s * 0.12f)
    // huge forearms
    limb(Offset(cx - s * 0.18f, feetY - s * 0.52f), Offset(cx - s * 0.26f, feetY - s * 0.40f), lw, paper)
    limb(Offset(cx + s * 0.18f, feetY - s * 0.52f), Offset(cx + s * 0.26f, feetY - s * 0.40f), lw, paper)
    drawCircle(paper, s * 0.115f, Offset(cx - s * 0.27f, feetY - s * 0.36f))
    drawCircle(paper, s * 0.115f, Offset(cx + s * 0.27f, feetY - s * 0.36f))
    drawCircle(ink, s * 0.115f, Offset(cx - s * 0.27f, feetY - s * 0.36f), style = Stroke(s * 0.02f))
    drawCircle(ink, s * 0.115f, Offset(cx + s * 0.27f, feetY - s * 0.36f), style = Stroke(s * 0.02f))
    // anchor tattoo dot on left forearm
    drawCircle(denim, s * 0.03f, Offset(cx - s * 0.27f, feetY - s * 0.36f))
    // head: big chin/jaw
    oval(paper, cx, feetY - s * 0.74f, s * 0.38f, s * 0.34f)
    drawCircle(ink, s * 0.19f, Offset(cx, feetY - s * 0.74f), style = Stroke(s * 0.025f))
    oval(paper, cx, feetY - s * 0.63f, s * 0.26f, s * 0.18f) // jaw
    // squinty eye + nose
    drawCircle(ink, s * 0.045f, Offset(cx - s * 0.06f, feetY - s * 0.78f))
    drawLine(ink, Offset(cx + s * 0.02f, feetY - s * 0.79f), Offset(cx + s * 0.12f, feetY - s * 0.77f), s * 0.025f, StrokeCap.Round)
    oval(ink, cx + s * 0.02f, feetY - s * 0.71f, s * 0.09f, s * 0.11f) // big nose
    // corncob pipe
    limb(Offset(cx - s * 0.10f, feetY - s * 0.60f), Offset(cx - s * 0.20f, feetY - s * 0.58f), s * 0.03f, brown)
    drawCircle(brown, s * 0.035f, Offset(cx - s * 0.21f, feetY - s * 0.575f))
    // sailor cap
    rrect(paper, cx, feetY - s * 1.02f, s * 0.34f, s * 0.12f, s * 0.05f)
    drawCircle(paper, s * 0.17f, Offset(cx, feetY - s * 1.00f), style = Stroke(s * 0.025f))
    rrect(ink, cx, feetY - s * 0.93f, s * 0.38f, s * 0.045f, s * 0.02f) // cap brim
}

// ------------------------------------------------------------- pooh

private fun DrawScope.drawPooh(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.04f
    // ears
    drawCircle(gold, s * 0.085f, Offset(cx - s * 0.16f, feetY - s * 0.88f))
    drawCircle(gold, s * 0.085f, Offset(cx + s * 0.16f, feetY - s * 0.88f))
    drawCircle(ink, s * 0.085f, Offset(cx - s * 0.16f, feetY - s * 0.88f), style = Stroke(s * 0.02f))
    drawCircle(ink, s * 0.085f, Offset(cx + s * 0.16f, feetY - s * 0.88f), style = Stroke(s * 0.02f))
    // feet
    oval(gold, cx - s * 0.12f, feetY - s * 0.05f, s * 0.22f, s * 0.10f)
    oval(gold, cx + s * 0.12f, feetY - s * 0.05f, s * 0.22f, s * 0.10f)
    // big round body
    drawCircle(gold, s * 0.26f, Offset(cx, feetY - s * 0.36f))
    drawCircle(ink, s * 0.26f, Offset(cx, feetY - s * 0.36f), style = Stroke(s * 0.025f))
    // red shirt band
    drawArc(
        toonRed, 200f, 140f, false,
        Offset(cx - s * 0.26f, feetY - s * 0.62f),
        Size(s * 0.52f, s * 0.52f),
        style = Stroke(s * 0.13f, cap = StrokeCap.Butt)
    )
    // stubby arms
    limb(Offset(cx - s * 0.24f, feetY - s * 0.42f), Offset(cx - s * 0.32f, feetY - s * 0.28f), lw, gold)
    limb(Offset(cx + s * 0.24f, feetY - s * 0.42f), Offset(cx + s * 0.32f, feetY - s * 0.28f), lw, gold)
    // head
    drawCircle(gold, s * 0.21f, Offset(cx, feetY - s * 0.72f))
    drawCircle(ink, s * 0.21f, Offset(cx, feetY - s * 0.72f), style = Stroke(s * 0.025f))
    // snout
    oval(Color(0xFFF7DC8A), cx, feetY - s * 0.66f, s * 0.20f, s * 0.14f)
    oval(ink, cx, feetY - s * 0.69f, s * 0.07f, s * 0.05f)
    // sleepy eyes
    drawLine(ink, Offset(cx - s * 0.13f, feetY - s * 0.76f), Offset(cx - s * 0.05f, feetY - s * 0.76f), s * 0.028f, StrokeCap.Round)
    drawLine(ink, Offset(cx + s * 0.05f, feetY - s * 0.76f), Offset(cx + s * 0.13f, feetY - s * 0.76f), s * 0.028f, StrokeCap.Round)
    smile(cx, feetY - s * 0.60f, s * 0.13f, ink, s * 0.022f)
    // honey pot in hand
    rrect(brown, cx + s * 0.33f, feetY - s * 0.36f, s * 0.14f, s * 0.12f, s * 0.03f)
    oval(gold, cx + s * 0.33f, feetY - s * 0.36f, s * 0.14f, s * 0.05f)
}

// ------------------------------------------------------------- betty

private fun DrawScope.drawBetty(cx: Float, feetY: Float, s: Float) {
    val lw = s * 0.03f
    // legs
    limb(Offset(cx - s * 0.07f, feetY - s * 0.30f), Offset(cx - s * 0.09f, feetY - s * 0.06f), lw, paper)
    limb(Offset(cx + s * 0.07f, feetY - s * 0.30f), Offset(cx + s * 0.09f, feetY - s * 0.06f), lw, paper)
    // heels
    rrect(ink, cx - s * 0.09f, feetY - s * 0.07f, s * 0.12f, s * 0.07f, s * 0.03f)
    rrect(ink, cx + s * 0.09f, feetY - s * 0.07f, s * 0.12f, s * 0.07f, s * 0.03f)
    // red dress (trapezoid)
    val dress = Path().apply {
        moveTo(cx - s * 0.13f, feetY - s * 0.58f)
        lineTo(cx + s * 0.13f, feetY - s * 0.58f)
        lineTo(cx + s * 0.22f, feetY - s * 0.28f)
        lineTo(cx - s * 0.22f, feetY - s * 0.28f)
        close()
    }
    drawPath(dress, toonRed)
    // arms
    limb(Offset(cx - s * 0.12f, feetY - s * 0.54f), Offset(cx - s * 0.22f, feetY - s * 0.40f), lw, paper)
    limb(Offset(cx + s * 0.12f, feetY - s * 0.54f), Offset(cx + s * 0.22f, feetY - s * 0.40f), lw, paper)
    // bob haircut (big black bob behind head)
    drawCircle(ink, s * 0.24f, Offset(cx, feetY - s * 0.76f))
    oval(ink, cx - s * 0.20f, feetY - s * 0.68f, s * 0.14f, s * 0.22f) // side curl L
    oval(ink, cx + s * 0.20f, feetY - s * 0.68f, s * 0.14f, s * 0.22f) // side curl R
    // face
    drawCircle(paper, s * 0.16f, Offset(cx, feetY - s * 0.74f))
    // big eyes with lashes
    drawCircle(paper, s * 0.055f, Offset(cx - s * 0.07f, feetY - s * 0.76f))
    drawCircle(paper, s * 0.055f, Offset(cx + s * 0.07f, feetY - s * 0.76f))
    drawCircle(ink, s * 0.055f, Offset(cx - s * 0.07f, feetY - s * 0.76f), style = Stroke(s * 0.018f))
    drawCircle(ink, s * 0.055f, Offset(cx + s * 0.07f, feetY - s * 0.76f), style = Stroke(s * 0.018f))
    drawCircle(ink, s * 0.022f, Offset(cx - s * 0.07f, feetY - s * 0.755f))
    drawCircle(ink, s * 0.022f, Offset(cx + s * 0.07f, feetY - s * 0.755f))
    // lashes
    drawLine(ink, Offset(cx - s * 0.115f, feetY - s * 0.80f), Offset(cx - s * 0.15f, feetY - s * 0.83f), s * 0.014f, StrokeCap.Round)
    drawLine(ink, Offset(cx + s * 0.115f, feetY - s * 0.80f), Offset(cx + s * 0.15f, feetY - s * 0.83f), s * 0.014f, StrokeCap.Round)
    // beauty mark + red lips
    drawCircle(ink, s * 0.012f, Offset(cx + s * 0.10f, feetY - s * 0.68f))
    oval(toonRed, cx, feetY - s * 0.655f, s * 0.09f, s * 0.05f)
    // hoop earrings
    drawCircle(gold, s * 0.035f, Offset(cx - s * 0.19f, feetY - s * 0.66f), style = Stroke(s * 0.014f))
    drawCircle(gold, s * 0.035f, Offset(cx + s * 0.19f, feetY - s * 0.66f), style = Stroke(s * 0.014f))
    // garter line on dress
    drawLine(paper, Offset(cx - s * 0.20f, feetY - s * 0.33f), Offset(cx + s * 0.20f, feetY - s * 0.33f), s * 0.018f)
}

@Suppress("unused")
private fun minF(a: Float, b: Float): Float = min(a, b)
