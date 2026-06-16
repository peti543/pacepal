package com.cosmos.orbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OrbitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceBlack
                ) {
                    OrbitApp()
                }
            }
        }
    }
}

@Composable
fun OrbitApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "system") {
        composable("system") {
            SolarSystemScreen(
                onBodySelected = { id -> navController.navigate("body/$id") }
            )
        }
        composable(
            route = "body/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            BodyDetailScreen(
                body = OrbitData.byId(id),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
