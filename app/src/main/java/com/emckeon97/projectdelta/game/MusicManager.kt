package com.emckeon97.projectdelta.game

import android.content.Context
import android.media.MediaPlayer
import com.emckeon97.projectdelta.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Background music: "The Entertainer" (Kevin MacLeod, CC-BY 4.0).
 * Loops forever at low volume via MediaPlayer; every call is a safe
 * no-op if the raw resource is missing. Mute persists in SharedPreferences
 * under "delta.musicMuted" (same key as iOS).
 */
object MusicManager {
    private const val PREFS = "projectdelta"
    private const val KEY_MUTED = "delta.musicMuted"

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    @Volatile
    private var player: MediaPlayer? = null

    @Volatile
    private var prefsLoaded = false

    private fun ensurePrefs(context: Context) {
        if (prefsLoaded) return
        _isMuted.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MUTED, false)
        prefsLoaded = true
    }

    /** Starts the loop (or resumes after unmute). Safe to call repeatedly. */
    fun play(context: Context) {
        ensurePrefs(context)
        if (_isMuted.value) return
        val app = context.applicationContext
        var p = player
        if (p == null) {
            p = try {
                MediaPlayer.create(app, R.raw.delta_ragtime)?.apply {
                    isLooping = true
                    setVolume(0.45f, 0.45f)
                }
            } catch (_: Exception) {
                null
            }
            player = p
        }
        try {
            p?.start()
        } catch (_: Exception) {
            // no-op: music stays silent
        }
    }

    fun toggleMute(context: Context) {
        ensurePrefs(context)
        val muted = !_isMuted.value
        _isMuted.value = muted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MUTED, muted)
            .apply()
        if (muted) {
            try {
                player?.pause()
            } catch (_: Exception) {
                // no-op
            }
        } else {
            play(context)
        }
    }
}
