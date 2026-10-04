package com.emckeon97.projectdelta.characters

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.emckeon97.projectdelta.R

/**
 * Bundled 2D sprite art (drawable-nodpi). Only these 9 ids have PNGs —
 * willie, betty and minnie have no drawables and always return null here.
 */
@DrawableRes
fun spriteResFor(id: String): Int? = when (id) {
    "felix" -> R.drawable.felix
    "popeye" -> R.drawable.popeye
    "oswald" -> R.drawable.oswald
    "koko" -> R.drawable.koko
    "bimbo" -> R.drawable.bimbo
    "pooh" -> R.drawable.pooh
    "olive" -> R.drawable.olive
    "bosko" -> R.drawable.bosko
    "pete" -> R.drawable.pete
    else -> null
}

fun hasSprite(id: String): Boolean = spriteResFor(id) != null

/**
 * Loads the sprite bitmap for [id], or null when the character has no
 * bundled art (falls back to the code-drawn primitive).
 */
@Composable
fun rememberSpriteBitmap(id: String): ImageBitmap? = when (id) {
    "felix" -> ImageBitmap.imageResource(R.drawable.felix)
    "popeye" -> ImageBitmap.imageResource(R.drawable.popeye)
    "oswald" -> ImageBitmap.imageResource(R.drawable.oswald)
    "koko" -> ImageBitmap.imageResource(R.drawable.koko)
    "bimbo" -> ImageBitmap.imageResource(R.drawable.bimbo)
    "pooh" -> ImageBitmap.imageResource(R.drawable.pooh)
    "olive" -> ImageBitmap.imageResource(R.drawable.olive)
    "bosko" -> ImageBitmap.imageResource(R.drawable.bosko)
    "pete" -> ImageBitmap.imageResource(R.drawable.pete)
    else -> null
}
