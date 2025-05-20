package com.example.stevesadeventureandmystery // Make sure this matches

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.stevesadeventureandmystery.game.scenes.BootScene // Ensure this path is correct
import com.example.stevesadeventureandmystery.ui.theme.StevesAdeventureAndMysteryTheme // Your Compose theme
import kotlinx.coroutines.launch

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var bootScene: BootScene? = null
    private var currentScreen by mutableStateOf("Booting") // Simple state for UI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity creating...")

        // Initialize BootScene
        bootScene = BootScene(
            onComplete = {
                // This will be called when BootScene finishes
                Log.d(TAG, "BootScene completed! Transitioning to Main Menu (conceptually).")
                // Update UI or navigate to a new Composable/Fragment/Activity for Main Menu
                currentScreen = "MainMenu"
                // TODO: Initialize and start MainMenuScene here
            },
            scope = lifecycleScope // Use the Activity's lifecycleScope for coroutines
        )
        Log.d(TAG, "onCreate: BootScene initialized.")

        setContent {
            StevesAdeventureAndMysteryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        currentScreen = currentScreen,
                        bootScene = bootScene
                    )
                }
            }
        }
        Log.d(TAG, "onCreate: setContent complete.")

        lifecycleScope.launch {
            // VITAL DEBUGGING STEP - START
            if (bootScene == null) {
                Log.e(TAG, "onCreate: bootScene is NULL right before trying to start it!")
            } else {
                Log.d(TAG, "onCreate: Launching coroutine to start BootScene (bootScene is NOT null).")
                bootScene?.start() // Safe call is still good
            }
            // VITAL DEBUGGING STEP - END
        }
        Log.d(TAG, "onCreate: BootScene start coroutine launched from MainActivity.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity resumed.")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Activity started.")
    }

    // We'll use buttons in our Composable for this.
    // These functions would typically be called from your Composables onClick handlers
    // via a ViewModel or by passing lambdas if the Composable is in the same file
    // and has access to these methods or the bootScene instance.
    // For now, GameScreen directly calls methods on the bootScene instance it receives.
}

// Simple Composable to show current state and provide test buttons
@Composable
fun GameScreen(currentScreen: String, bootScene: BootScene?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Current Screen: $currentScreen")
            Spacer(modifier = Modifier.height(20.dp))

            if (currentScreen == "Booting") {
                bootScene?.let { scene -> // Use 'scene' for clarity
                    Button(onClick = {
                        Log.d(
                            TAG,
                            "GameScreen: Audio Prompt Button Clicked. bootScene instance: $scene"
                        )
                        scene.handleAudioPromptInput()
                    }) {
                        Text("Simulate Audio Input")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(onClick = {
                        Log.d(TAG, "GameScreen: Continue Button Clicked. bootScene instance: $scene")
                        scene.handleContinueInput()
                    }) {
                        Text("Simulate Continue Input")
                    }
                } ?: run {
                    Log.e(
                        TAG,
                        "GameScreen: bootScene is NULL when trying to show buttons during Booting state."
                    )
                    Text("Error: BootScene not available. Check logs.")
                }
            }

            if (currentScreen == "MainMenu") {
                Text("Welcome to the Main Menu! (Placeholder)")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    StevesAdeventureAndMysteryTheme {
        GameScreen("Booting", null) // Preview with null BootScene
    }
}