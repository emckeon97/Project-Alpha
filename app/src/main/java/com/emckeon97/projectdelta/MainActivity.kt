package com.emckeon97.projectdelta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.emckeon97.projectdelta.ads.AdManager
import com.emckeon97.projectdelta.characters.CharacterManager
import com.emckeon97.projectdelta.ui.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(this)
        val characterManager = CharacterManager(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppNav(characterManager = characterManager)
            }
        }
    }
}
