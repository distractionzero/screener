package dev.codingcoffee.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.codingcoffee.screen.ui.ScreenRoot
import dev.codingcoffee.screen.ui.theme.ScreenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTheme {
                ScreenRoot()
            }
        }
    }
}
