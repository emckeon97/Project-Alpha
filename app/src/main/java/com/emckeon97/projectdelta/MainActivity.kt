package com.emckeon97.projectdelta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.game.MusicManager
import com.emckeon97.projectdelta.ui.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Never play audio when the app isn't actively being used.
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> MusicManager.pause()
                Lifecycle.Event.ON_RESUME -> MusicManager.play(this)
                else -> {}
            }
        })
        AdManager.initialize(this)
        val characterManager = CharacterManager(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppNav(characterManager = characterManager)
            }
        }
    }
}
