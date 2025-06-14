package com.example.stevesadventureandmystery.game.core

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.stevesadventureandmystery.game.data.SaveSlotInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GAME_PROGRESS_PREFS_NAME = "GameProgressPrefs"
private const val SAVE_SLOTS_PREFS_KEY = "game_save_slots_list"
const val MAX_SAVE_SLOTS = 4

class SaveLoadManager(private val context: Context) {

    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(GAME_PROGRESS_PREFS_NAME, Context.MODE_PRIVATE)
    private var currentSaveSlots: MutableList<SaveSlotInfo> = mutableListOf()

    init {
        Log.d("SaveLoadManager", "Initializing...")
        loadSaveSlotsFromPreferences()
    }

    private fun loadSaveSlotsFromPreferences() {
        val json = sharedPreferences.getString(SAVE_SLOTS_PREFS_KEY, null)
        var successfullyLoaded = false
        if (json != null) {
            try {
                val type = object : TypeToken<List<SaveSlotInfo>>() {}.type
                val slots: List<SaveSlotInfo>? = gson.fromJson(json, type)
                if (slots != null) {
                    currentSaveSlots = slots.toMutableList()
                    Log.d("SaveLoadManager", "Successfully loaded ${currentSaveSlots.size} slots from SharedPreferences.")
                    successfullyLoaded = true
                } else {
                    Log.w("SaveLoadManager", "Parsed JSON was null. Initializing empty slots.")
                }
            } catch (e: Exception) {
                Log.e("SaveLoadManager", "Error parsing save slots JSON. Initializing empty slots.", e)
            }
        } else {
            Log.i("SaveLoadManager", "No save data found in SharedPreferences. Initializing empty slots.")
        }

        if (!successfullyLoaded || currentSaveSlots.size != MAX_SAVE_SLOTS) {
            Log.w("SaveLoadManager", "Slot count mismatch or load error. Re-initializing to $MAX_SAVE_SLOTS slots. Loaded: ${currentSaveSlots.size}")
            currentSaveSlots.clear()
            for (i in 1..MAX_SAVE_SLOTS) {
                currentSaveSlots.add(SaveSlotInfo(slotId = i, isEmpty = true))
            }
            persistSaveSlotsToPreferences()
            Log.i("SaveLoadManager", "Initialized and saved $MAX_SAVE_SLOTS empty slots.")
        }
    }

    private fun persistSaveSlotsToPreferences() {
        try {
            val json = gson.toJson(currentSaveSlots)
            sharedPreferences.edit {
                putString(SAVE_SLOTS_PREFS_KEY, json)
            }
            Log.d("SaveLoadManager", "Successfully persisted ${currentSaveSlots.size} save slots to SharedPreferences.")
        } catch (e: Exception) {
            Log.e("SaveLoadManager", "Error persisting save slots to SharedPreferences.", e)
        }
    }

    fun getAllSaveSlotInfo(): List<SaveSlotInfo> {
        return currentSaveSlots.map { it.copy() }
    }

    suspend fun startNewGameInSlot(slotId: Int, chapterName: String = "Chapter 1: The Beginning", sceneId: String = "intro_scene_01"): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (!slot.isEmpty) {
                    Log.w("SaveLoadManager", "Overwriting occupied slot $slotId.")
                }
                slot.isEmpty = false
                slot.chapterName = chapterName
                slot.currentSceneId = sceneId
                slot.lastSavedTimestamp = getCurrentFormattedTimestamp()
                slot.gameVersion = "1.0" // Consider using BuildConfig.VERSION_NAME

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "New game started in slot $slotId: $chapterName")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to start new game: Slot ID $slotId not found.")
                false
            }
        }
    }

    suspend fun loadGameFromSlot(slotId: Int): SaveSlotInfo? {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null && !slot.isEmpty) {
                Log.i("SaveLoadManager", "Loading game from slot $slotId: ${slot.chapterName}")
                slot.copy()
            } else {
                Log.w("SaveLoadManager", "Attempted to load from empty/non-existent slot $slotId.")
                null
            }
        }
    }

    @Suppress("unused")
    suspend fun saveGameToSlot(slotId: Int, currentChapter: String, currentScene: String): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (slot.isEmpty) {
                    Log.w("SaveLoadManager", "Saving to initially empty slot $slotId.")
                    slot.isEmpty = false
                }
                slot.chapterName = currentChapter
                slot.currentSceneId = currentScene
                slot.lastSavedTimestamp = getCurrentFormattedTimestamp()

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "Game progress saved to slot $slotId: $currentChapter")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to save game: Slot ID $slotId not found.")
                false
            }
        }
    }

    suspend fun deleteSaveSlot(slotId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (slot.isEmpty) {
                    Log.w("SaveLoadManager", "Slot $slotId is already empty.")
                    return@withContext true
                }
                slot.isEmpty = true
                slot.chapterName = null
                slot.lastSavedTimestamp = null
                slot.currentSceneId = null
                slot.gameVersion = null

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "Save data deleted for slot $slotId.")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to delete save: Slot ID $slotId not found.")
                false
            }
        }
    }

    /**
     * Clears all save data by marking all slots as empty and persisting.
     * Also attempts to delete any associated individual save files if they follow a known pattern.
     * Note: This current implementation primarily clears the SharedPreferences list.
     * Actual file deletion logic might need to be more robust depending on how full game states are stored.
     */
    suspend fun clearAllSaveData(): Boolean {
        return withContext(Dispatchers.IO) {
            var allSuccessful = true // Initialize to true
            Log.i("SaveLoadManager", "Attempting to clear all save data.")

            // Clear the in-memory list representation
            currentSaveSlots.forEach { slot ->
                slot.isEmpty = true
                slot.chapterName = null
                slot.lastSavedTimestamp = null
                slot.currentSceneId = null
                slot.gameVersion = null
            }
            persistSaveSlotsToPreferences() // Assuming this always succeeds or throws an exception for failure

            for (i in 0 until MAX_SAVE_SLOTS) {
                val oldStyleSaveFile = File(context.filesDir, "savegame_$i.json")
                if (oldStyleSaveFile.exists()) {
                    if (!oldStyleSaveFile.delete()) {
                        Log.e("SaveLoadManager", "Failed to delete legacy save file: ${oldStyleSaveFile.name}")
                        allSuccessful = false
                    } else {
                        Log.i("SaveLoadManager", "Deleted legacy save file: ${oldStyleSaveFile.name}")
                    }
                }
            }

            if (allSuccessful) {
                Log.i("SaveLoadManager", "All save data (including legacy files if present) cleared successfully.")
            } else {
                Log.w("SaveLoadManager", "Some parts of clearing all save data may have failed (e.g., legacy file deletions). SharedPreferences data was cleared.")
            }
            allSuccessful // Return overall success.
        }
    }

    private fun getCurrentFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}