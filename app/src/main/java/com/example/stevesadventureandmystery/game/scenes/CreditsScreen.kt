package com.example.stevesadventureandmystery.game.scenes

import androidx.compose.animation.AnimatedVisibility // Kept for potential future use
import androidx.compose.animation.core.tween // Kept for potential future use
import androidx.compose.animation.fadeIn // Kept for potential future use
import androidx.compose.animation.fadeOut // Kept for potential future use
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn // <-- Added for scrolling
import androidx.compose.foundation.lazy.items // <-- Added for LazyColumn items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stevesadventureandmystery.R
import com.example.stevesadventureandmystery.ui.theme.StevesAdventureAndMysteryTheme
// Optional: import kotlinx.coroutines.delay
// Optional: import androidx.compose.runtime.LaunchedEffect

// Your full list of credits
val fullGameCreditsList: List<String> = listOf(
    "Steve's Space Adventure Mystery",
    "",
    "Story by Joseph Rice",
    "This is the prequel to the \"My Story In Space\" book series",
    "Available in online stores now",
    "",
    "Credits",
    "",
    "Game Design and Development",
    "Joseph Rice",
    "",
    "Special Thanks",
    "The Phaser.js Community",
    "",
    "Artwork",
    "Milky Way Galaxy during nighttime",
    "Photo by Hristo Fidanov from Pexels",
    "https://www.pexels.com/photo/milky-way-galaxy-during-nighttime-1252890/",
    "",
    "Galaxy Image",
    "Image by Lumina Obscura from Pixabay",
    "",
    "Voice Acting",
    "AI-generated voice by ElevenLabs",
    "",
    "Voice by Joseph Rice",
    "Voice changed by Eleven Labs.io AI Andrew J Griffin",
    "",
    "\"What's your age?\" - Andrew J Griffin",
    "\"What's your name?\" - Andrew J Griffin",
    "",
    "Music",
    "Track: \"Journey's End\"",
    "Composed by: Suno AI (https://suno.ai), Version 3.5",
    "",
    "Track: \"Space Explorers\"",
    "Composed by: Kevin MacLeod (incompetech.com)",
    "Licensed under Creative Commons: By Attribution 4.0 License",
    "http://creativecommons.org/licenses/by/4.0/",
    "",
    "Credits Scene Background Image",
    "Image Title: Galactic End Credits",
    "Image Description: A captivating AI-generated image representing the vastness of space, featuring a beautiful array of stars and galaxies.",
    "Created by: Microsoft Copilot",
    "Source: Generated using advanced AI tools",
    "",
    "Tools Used",
    "Phaser",
    "Android Studio", // Added Android Studio and Jetpack Compose here
    "Jetpack Compose",
    "",
    "Assets",
    "UI Elements by lemongreen",
    "Licensed under Creative Commons Attribution v4.0 International",
    "Source: Itch.io",
    "",
    "Disclaimer",
    "All characters, names, and events in this game are fictional.",
    "Any resemblance to real persons, living or dead, or actual events is purely coincidental.",
    "© 2024 Joseph Rice. All rights reserved."
)

@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Set to true to show credits immediately.
    // If you want the fade-in animation, keep it false and uncomment LaunchedEffect
    var showCreditsText by remember { mutableStateOf(true) }

    // Optional: Use LaunchedEffect to make credits text appear after a delay
    // LaunchedEffect(key1 = true) {
    //     delay(500) // Wait for 0.5 seconds
    //     showCreditsText = true
    // }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ai_art_for_credits_scene_background),
            contentDescription = "Credits Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Changed to Crop for better background scaling
        )

        // This Column will contain the scrollable credits list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, bottom = 80.dp, start = 16.dp, end = 16.dp), // Added more padding for top and bottom to avoid overlap with button
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AnimatedVisibility can still be used if you want the fade-in
            AnimatedVisibility(
                visible = showCreditsText,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000))
            ) {
                // LazyColumn to display the list of credits and make it scrollable
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(), // Allow LazyColumn to take available height
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Space between credit lines
                ) {
                    // Iterate through your full credits list
                    items(fullGameCreditsList) { creditLine ->
                        val isTitle = creditLine == "Steve's Space Adventure Mystery"
                        val isSectionHeader = setOf(
                            "Credits", "Special Thanks", "Artwork", "Voice Acting",
                            "Music", "Credits Scene Background Image", "Tools Used", "Assets", "Disclaimer"
                        ).contains(creditLine) && creditLine.isNotBlank()

                        val textStyle = when {
                            isTitle -> TextStyle(
                                fontSize = 28.sp, // Slightly smaller to fit more
                                color = Color(0xFF40E0D0), // Turquoise
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            isSectionHeader -> TextStyle(
                                fontSize = 22.sp, // Slightly smaller
                                color = Color(0xFF39FF14), // Bright Green
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            creditLine.isBlank() -> TextStyle(fontSize = 6.sp) // Smaller spacer for blank lines
                            else -> TextStyle(
                                fontSize = 16.sp, // Standard text size
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                        if (creditLine.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp)) // Explicit spacer for blank lines
                        } else {
                            Text(
                                text = creditLine,
                                style = textStyle,
                                modifier = Modifier.padding(horizontal = 16.dp) // Padding for each text line
                            )
                        }
                    }
                }
            }
        }

        // "Back to Main Menu" Button - Aligned to the bottom of the outer Box
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 64.dp, vertical = 24.dp), // Adjust padding for better placement
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.5f), // Slightly visible background
                contentColor = Color.White
            )
        ) {
            Text(
                "Back to Main Menu",
                fontSize = 20.sp // Adjusted size
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 360, device = "spec:orientation=landscape,width=411dp,height=891dp")
@Composable
fun CreditsScreenPreview() {
    StevesAdventureAndMysteryTheme(darkTheme = true) { // Assuming dark theme for credits
        CreditsScreen(onBack = { /* Preview: Back triggered */ })
    }
}