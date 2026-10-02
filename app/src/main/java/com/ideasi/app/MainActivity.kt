package com.ideasi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
    IdeasITheme {
        var currentScreen by remember { mutableStateOf("splash") }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000)
            currentScreen = "selectApps"
        }

        when (currentScreen) {
            "splash" -> SplashScreen()
            "selectApps" -> SelectAppsScreen(onNext = { currentScreen = "setTime" })
            "setTime" -> SetTimeScreen(onDone = { currentScreen = "dashboard" })
            "dashboard" -> Text("Dashboard coming in the next stage")
        }
    }
        }
    }
}
            
        
        
    


@Composable
fun IdeasITheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "IDEAS I",
                fontSize = 32.sp
            )
        }
    }
}
