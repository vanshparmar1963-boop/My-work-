package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.AiChatScreen
import com.example.ui.GuideScreen
import com.example.ui.SandboxScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VibrantBg
import com.example.ui.theme.VibrantSecondary
import com.example.ui.theme.VibrantSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    var currentScreenTab by remember { mutableIntStateOf(0) } // 0 = Sandbox Arena, 1 = Guide Manual, 2 = AI Architect

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = VibrantSurface,
                contentColor = VibrantSecondary
            ) {
                NavigationBarItem(
                    selected = currentScreenTab == 0,
                    onClick = { currentScreenTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Sandbox Arena") },
                    label = { Text("Sandbox Arena", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VibrantSecondary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = VibrantSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentScreenTab == 1,
                    onClick = { currentScreenTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Code blueprints") },
                    label = { Text("Blueprints", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VibrantSecondary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = VibrantSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentScreenTab == 2,
                    onClick = { currentScreenTab = 2 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "AI Assistant chat") },
                    label = { Text("AI Architect", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VibrantSecondary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = VibrantSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = VibrantBg
        ) {
            when (currentScreenTab) {
                0 -> SandboxScreen()
                1 -> GuideScreen()
                2 -> AiChatScreen()
            }
        }
    }
}
