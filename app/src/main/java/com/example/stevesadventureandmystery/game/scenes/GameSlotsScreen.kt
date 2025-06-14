package com.example.stevesadventureandmystery.game.scenes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
// Removed: import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
// Removed: import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
// Removed: import androidx.compose.material3.Icon
// Removed: import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Removed: import androidx.compose.material3.Icons // This was the problematic import
import androidx.compose.runtime.*
// Removed: import androidx.compose.runtime.getValue
// Removed: import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stevesadventureandmystery.game.data.SaveSlotInfo
import com.example.stevesadventureandmystery.ui.theme.StevesAdventureAndMysteryTheme

@Composable
fun GameSlotsScreen(
    saveSlots: List<SaveSlotInfo>,
    onSlotSelected: (slotInfo: SaveSlotInfo, isNewGame: Boolean) -> Unit,
    onBackPressed: () -> Unit
    // Removed onDeleteSlot parameter
) {
    // Removed showConfirmDeleteDialogForSlotId state variable

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Game Progress",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            if (saveSlots.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No saved progress yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        "Select an empty slot below to begin your adventure!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(saveSlots) { _, slotInfo ->
                        GameSlotItem(
                            slotInfo = slotInfo,
                            onSlotClicked = { isStartingNew ->
                                onSlotSelected(slotInfo, isStartingNew)
                            }
                            // Removed onDeleteClicked callback
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackPressed,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Back to Main Menu", fontSize = 16.sp)
            }
        }

        // Removed ConfirmDeleteDialog logic
    }
}

@Composable
fun GameSlotItem(
    slotInfo: SaveSlotInfo,
    onSlotClicked: (isNewGame: Boolean) -> Unit
    // Removed onDeleteClicked parameter
) {
    val isEmptySlot = slotInfo.isEmpty

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clickable {
                Log.d("GameSlotItem", "Slot ${slotInfo.slotId} clicked. isEmpty: $isEmptySlot")
                onSlotClicked(isEmptySlot)
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isEmptySlot) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Slot ${slotInfo.slotId}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isEmptySlot) {
                    Text(
                        "[ Empty Slot - Start New Game ]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        slotInfo.chapterName ?: "No Chapter Data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Saved: ${slotInfo.lastSavedTimestamp ?: "Not available"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            // Removed IconButton and associated Spacer
        }
    }
}

// The ConfirmDeleteDialog composable can be entirely removed from this file
// if it's not used elsewhere. For now, I'll comment it out.
/*
@Composable
fun ConfirmDeleteDialog(
    slotInfo: SaveSlotInfo,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Delete Saved Game?", color = Color.White) },
        text = {
            Text(
                "Are you sure you want to delete the saved progress in Slot ${slotInfo.slotId}?\n" +
                        "'${slotInfo.chapterName ?: "This save"}' will be lost permanently.",
                color = Color.LightGray
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismissRequest() // Dismiss after confirm
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    )
}
*/

// --- Previews ---
// Previews will also need to be updated to not pass the onDeleteSlot or onDeleteClicked parameters

@Preview(name="GameSlots - Empty", showBackground = true, backgroundColor = 0xFF000000, widthDp = 1280, heightDp = 800, device = "spec:orientation=landscape,width=411dp,height=891dp")
@Composable
fun GameSlotsScreenPreview_Empty() {
    StevesAdventureAndMysteryTheme {
        GameSlotsScreen(
            saveSlots = emptyList(),
            onSlotSelected = { _, _ -> Log.d("Preview", "Slot selected") },
            onBackPressed = { Log.d("Preview", "Back pressed") }
            // Removed onDeleteSlot
        )
    }
}

@Preview(name="GameSlots - Some Data", showBackground = true, backgroundColor = 0xFF000000, widthDp = 1280, heightDp = 800, device = "spec:orientation=landscape,width=411dp,height=891dp")
@Composable
fun GameSlotsScreenPreview_WithData() {
    val previewSlots = listOf(
        SaveSlotInfo(slotId = 1, isEmpty = false, chapterName = "Chapter 1: The Discovery", lastSavedTimestamp = "2023-10-26 10:00 AM", currentSceneId = "scene_01_intro", gameVersion = "1.0.0"),
        SaveSlotInfo(slotId = 2, isEmpty = true, chapterName = null, lastSavedTimestamp = null, currentSceneId = null, gameVersion = null),
        SaveSlotInfo(slotId = 3, isEmpty = false, chapterName = "Chapter 5: Nebula's Edge", lastSavedTimestamp = "2023-10-25 06:30 PM", currentSceneId = "scene_05_ship", gameVersion = "1.0.0"),
        SaveSlotInfo(slotId = 4, isEmpty = true, chapterName = null, lastSavedTimestamp = null, currentSceneId = null, gameVersion = null)
    )
    StevesAdventureAndMysteryTheme {
        GameSlotsScreen(
            saveSlots = previewSlots,
            onSlotSelected = { slot, isNew -> Log.d("Preview", "Slot ${slot.slotId} selected, isNew: $isNew") },
            onBackPressed = { Log.d("Preview", "Back pressed") }
            // Removed onDeleteSlot
        )
    }
}

@Preview(name="GameSlot Item - Empty", showBackground = true, backgroundColor = 0xFF1C1B1F, widthDp = 400)
@Composable
fun GameSlotItemPreview_Empty() {
    StevesAdventureAndMysteryTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GameSlotItem(
                slotInfo = SaveSlotInfo(slotId = 1, isEmpty = true),
                onSlotClicked = {}
                // Removed onDeleteClicked
            )
        }
    }
}

@Preview(name="GameSlot Item - Occupied", showBackground = true, backgroundColor = 0xFF1C1B1F, widthDp = 400)
@Composable
fun GameSlotItemPreview_Occupied() {
    StevesAdventureAndMysteryTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GameSlotItem(
                slotInfo = SaveSlotInfo(slotId = 2, isEmpty = false, "The Ancient Caves", "Yesterday evening", "cave_puzzle_01"),
                onSlotClicked = {}
                // Removed onDeleteClicked
            )
        }
    }
}