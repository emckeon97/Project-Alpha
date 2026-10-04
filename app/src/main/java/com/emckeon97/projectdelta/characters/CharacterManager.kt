package com.emckeon97.projectdelta.characters

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns coins, unlocks, selection, and high score. SharedPreferences-backed
 * with the same keys as the iOS build so progress stays consistent.
 */
class CharacterManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _coins = MutableStateFlow(prefs.getInt(KEY_COINS, 0))
    val coins: StateFlow<Int> = _coins

    private val _selectedID = MutableStateFlow(prefs.getString(KEY_SELECTED, "willie") ?: "willie")
    val selectedID: StateFlow<String> = _selectedID

    private val _unlockedIDs =
        MutableStateFlow(prefs.getStringSet(KEY_UNLOCKED, setOf("willie")) ?: setOf("willie"))
    val unlockedIDs: StateFlow<Set<String>> = _unlockedIDs

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_HIGHSCORE, 0))
    val highScore: StateFlow<Int> = _highScore

    val characters: List<GameCharacter> get() = ROSTER

    val selectedCharacter: GameCharacter
        get() = characters.firstOrNull { it.id == _selectedID.value } ?: characters.first()

    fun isUnlocked(c: GameCharacter): Boolean = _unlockedIDs.value.contains(c.id)

    /** Returns false if not enough coins. */
    fun unlock(c: GameCharacter): Boolean {
        if (isUnlocked(c)) return true
        if (_coins.value < c.price) return false
        _coins.value -= c.price
        _unlockedIDs.value = _unlockedIDs.value + c.id
        persist()
        return true
    }

    fun select(c: GameCharacter) {
        if (!isUnlocked(c)) return
        _selectedID.value = c.id
        persist()
    }

    fun addCoins(n: Int) {
        if (n <= 0) return
        _coins.value += n
        persist()
    }

    fun recordScore(s: Int) {
        if (s > _highScore.value) {
            _highScore.value = s
            persist()
        }
    }

    private fun persist() {
        prefs.edit()
            .putInt(KEY_COINS, _coins.value)
            .putString(KEY_SELECTED, _selectedID.value)
            .putStringSet(KEY_UNLOCKED, _unlockedIDs.value)
            .putInt(KEY_HIGHSCORE, _highScore.value)
            .apply()
    }

    companion object {
        private const val PREFS = "projectdelta"
        private const val KEY_COINS = "delta.coins"
        private const val KEY_SELECTED = "delta.selected"
        private const val KEY_UNLOCKED = "delta.unlocked"
        private const val KEY_HIGHSCORE = "delta.highscore"
    }
}
