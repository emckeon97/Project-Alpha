package com.emckeon97.projectdelta.characters

/**
 * Public-domain cartoon roster. All likenesses are original code-drawn
 * interpretations — no copyrighted assets.
 */
data class GameCharacter(
    val id: String,
    val name: String,
    val price: Int,
    val tagline: String
)

val ROSTER: List<GameCharacter> = listOf(
    GameCharacter(
        id = "willie",
        name = "Steamboat Willie",
        price = 0,
        tagline = "The 1928 original. Toots a mean whistle."
    ),
    GameCharacter(
        id = "felix",
        name = "Felix the Cat",
        price = 500,
        tagline = "A bag of tricks since 1919."
    ),
    GameCharacter(
        id = "oswald",
        name = "Oswald the Lucky Rabbit",
        price = 1000,
        tagline = "The lucky rabbit, back on track."
    ),
    GameCharacter(
        id = "popeye",
        name = "Popeye the Sailor",
        price = 2500,
        tagline = "Spinach-powered and shipshape."
    ),
    GameCharacter(
        id = "pooh",
        name = "Winnie the Pooh",
        price = 5000,
        tagline = "A bear of very little brain, much honey."
    ),
    GameCharacter(
        id = "betty",
        name = "Betty Boop",
        price = 10000,
        tagline = "Boop-oop-a-doop since 1930."
    )
)
