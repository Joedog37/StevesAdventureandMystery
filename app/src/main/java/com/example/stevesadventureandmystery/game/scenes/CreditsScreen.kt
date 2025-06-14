package com.example.stevesadventureandmystery.game.scenes // Correct package

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    "Android Studio",    // <-- NEW TOOL
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

private const val TAG = "CreditsScreen"

@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ai_art_for_credits_scene_background),
            contentDescription = "Credits Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = true
            ) {
                itemsIndexed(fullGameCreditsList, key = { index, creditLine -> "credit-$index-$creditLine" }) { _, creditLine ->
                    val isTitle = creditLine == "Steve's Space Adventure Mystery"
                    val isSectionHeader = setOf(
                        "Credits", "Special Thanks", "Artwork", "Voice Acting",
                        "Music", "Credits Scene Background Image", "Tools Used", "Assets", "Disclaimer"
                    ).contains(creditLine) && creditLine.isNotBlank()

                    val textStyle = when {
                        isTitle -> TextStyle(fontSize = 28.sp, color = Color(0xFF40E0D0), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        isSectionHeader -> TextStyle(fontSize = 22.sp, color = Color(0xFF39FF14), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        else -> TextStyle(fontSize = 16.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 22.sp)
                    }

                    if (creditLine.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = creditLine,
                            style = textStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                Log.d(TAG, "BACK_BUTTON: Clicked.")
                onBack()
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            )
        ) {
            Text("Back", fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun CreditsScreenPreview() {
    StevesAdventureAndMysteryTheme(darkTheme = true) {
        CreditsScreen(onBack = { Log.d("CreditsPreview", "Back triggered from preview") })
    }
}