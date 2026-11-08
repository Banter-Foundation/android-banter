package com.banter.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.banter.android.ui.navigation.BanterNavGraph
import com.banter.android.ui.theme.BanterColors
import com.banter.android.ui.theme.BanterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BanterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BanterColors.Background
                ) {
                    BanterNavGraph()
                }
            }
        }
    }
}
