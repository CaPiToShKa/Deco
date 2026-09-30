package com.example.decosocio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.decosocio.ui.navigation.AppRoot
import com.example.decosocio.ui.theme.DecoTheme

/**
 * AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales()
 * can switch between pt-PT and English on every supported Android version.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DecoTheme {
                AppRoot()
            }
        }
    }
}
