package com.example.stevesadeventureandmystery

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.stevesadeventureandmystery.game.scenes.BootScene
import com.example.stevesadeventureandmystery.ui.theme.StevesAdeventureAndMysteryTheme
import kotlinx.coroutines.launch

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var bootScene: BootScene? = null
    private var currentScreen by mutableStateOf("Booting")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity creating...")

        bootScene = BootScene(
            onComplete = {
                Log.d(TAG, "BootScene completed! Transitioning to Main Menu.")
                currentScreen = "MainMenu"
                // TODO: Initialize and start MainMenuScene here
            },
            scope = lifecycleScope
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

        lifecycleScope.launch {
            if (bootScene == null) {
                Log.e(TAG, "onCreate: bootScene is NULL right before trying to start it!")
            } else {
                Log.d(TAG, "onCreate: Launching coroutine to start BootScene.")
                bootScene?.start()
            }
        }
        Log.d(TAG, "onCreate: BootScene start coroutine launched.")
    }

    // Lifecycle methods (onResume, onStart) can remain as they are for logging.
}

@Composable
fun GameScreen(currentScreen: String, bootScene: BootScene?) {
    Box(modifier = Modifier.fillMaxSize()) {

        // --- Background Layer (visible during Booting and potentially MainMenu) ---
        if ((currentScreen == "Booting" && bootScene != null && bootScene.backgroundDrawableRes != 0) ||
            (currentScreen == "MainMenu" /* && mainMenuScene has similar background */)) { // Adapt for MainMenu later

            val alpha by animateFloatAsState(
                targetValue = if (currentScreen == "Booting" && bootScene != null) bootScene.backgroundAlpha else 0f, // Example: fade out if not booting
                label = "BackgroundAlpha"
            )

            if (bootScene?.backgroundDrawableRes != 0) { // Check drawable is set
                Image(
                    painter = painterResource(id = bootScene!!.backgroundDrawableRes),
                    contentDescription = "Main Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(alpha),
                    contentScale = ContentScale.Crop // Or FillBounds, Fit, etc.
                )
            }
        }


        // --- Foreground Elements (Text, Logo, Buttons) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add some padding around the content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            if (currentScreen == "Booting" && bootScene != null) {

                // Warning Text
                AnimatedVisibility(
                    visible = bootScene.showWarningText,
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    Text(
                        text = bootScene.warningTextContent,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Yellow, // Example color
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent background
                            .padding(16.dp)
                    )
                }

                //Spacer to prevent overlap if warning and logo could show close together
                if (bootScene.showWarningText && bootScene.showLogoImage) Spacer(Modifier.height(20.dp))


                // Logo Image
                AnimatedVisibility(
                    visible = bootScene.showLogoImage && bootScene.logoDrawableRes != 0,
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    Image(
                        painter = painterResource(id = bootScene.logoDrawableRes),
                        contentDescription = "Game Logo",
                        modifier = Modifier
                            .heightIn(max = 150.dp) // Max height for the logo
                            .fillMaxWidth(0.6f) // Max 60% of screen width
                    )
                }

                //Spacer if logo and title could show close together
                if (bootScene.showLogoImage && bootScene.showTitleText) Spacer(Modifier.height(20.dp))

                // Title Text (often shown on top of the background)
                AnimatedVisibility(
                    visible = bootScene.showTitleText,
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(700)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(700)) // If it should fade out
                ) {
                    Text(
                        text = bootScene.titleTextContent,
                        fontSize = 36.sp, // Larger for a title
                        color = MaterialTheme.colorScheme.onPrimaryContainer, // Choose a contrasting color
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 30.dp) // Space below title
                    )
                }

                //Spacer to push buttons down a bit if title is visible
                if(bootScene.showTitleText) Spacer(Modifier.height(50.dp))


                // Audio Prompt Elements
                AnimatedVisibility(visible = bootScene.showAudioPromptElements) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(bootScene.audioPromptTextContent, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Button(onClick = {
                            bootScene.handleAudioPromptInput()
                        }) {
                            Text("Yes, enable audio") // More descriptive
                        }
                        // Optionally add a "No" button or a skip
                        Button(onClick = {
                            // Handle "No audio" - could be same as Yes for this sim, or different logic
                            bootScene.handleAudioPromptInput() // For now, treat same as yes
                        }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("No, keep silent")
                        }
                    }
                }


                // Continue Button Elements
                AnimatedVisibility(visible = bootScene.showContinueButtonElements) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ready to Proceed?", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                        Button(onClick = {
                            bootScene.handleContinueInput()
                        }) {
                            Text(bootScene.continueButtonTextContent)
                        }
                    }
                }


            } else if (currentScreen == "MainMenu") {
                // Main Menu UI
                Text(
                    "Welcome to the Main Menu!",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                // TODO: Add Main Menu buttons and other UI elements here
            } else {
                // Fallback for unknown states or if bootScene is null during booting
                Text("Loading...", fontSize = 20.sp)
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000) // Dark background for preview
@Composable
fun DefaultPreview() {
    StevesAdeventureAndMysteryTheme {
        // You might want to create a mock BootScene for preview if it becomes complex
        // For now, testing different states of GameScreen:
        // GameScreen("Booting", bootScene = null) // Preview initial loading
        GameScreen("MainMenu", bootScene = null) // Preview MainMenu
    }
}