package com.example.stevesadventureandmystery.game.scenes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stevesadventureandmystery.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainActivity: MainActivity,
    backgroundImageResId: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundImageResId),
            contentDescription = "Settings Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Game Settings",
                fontSize = 30.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(30.dp))

            SettingSliderItem(
                label = "Music Volume",
                value = mainActivity.musicVolumeState,
                onValueChange = { mainActivity.onMusicVolumeChange(it) }
            )

            SettingSliderItem(
                label = "Sound Effects Volume",
                value = mainActivity.sfxVolumeState,
                onValueChange = { mainActivity.onSfxVolumeChange(it) }
            )

            SettingSliderItem(
                label = "Voice Volume",
                value = mainActivity.voiceVolumeState,
                onValueChange = { mainActivity.onVoiceVolumeChange(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Data Management",
                fontSize = 20.sp, // You might want to adjust the style
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    mainActivity.requestClearAllSaveData()
                },
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Clear All Save Data", color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes the "Back to Main Menu" button to the bottom

            Button(
                onClick = { mainActivity.navigateToMainMenu() },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Back to Main Menu")
            }
        }
    }
}

@Composable
private fun SettingSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Text(label, color = Color.White)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}