package com.example.stevesadventureandmystery.game.core

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.stevesadventureandmystery.game.data.SaveSlotInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants for SharedPreferences
private const val GAME_PROGRESS_PREFS_NAME = "GameProgressPrefs"
private const val SAVE_SLOTS_PREFS_KEY = "game_save_slots_list"
const val MAX_SAVE_SLOTS = 4

class SaveLoadManager(context: Context) {

    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences(GAME_PROGRESS_PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory cache of save slots, loaded from SharedPreferences.
    // This is the source of truth after initialization.
    private var currentSaveSlots: MutableList<SaveSlotInfo> = mutableListOf()

    init {
        Log.d("SaveLoadManager", "Initializing...")
        loadSaveSlotsFromPreferences() // Load existing or initialize new ones
    }

    private fun loadSaveSlotsFromPreferences() {
        val json = sharedPreferences.getString(SAVE_SLOTS_PREFS_KEY, null)
        var successfullyLoaded = false
        if (json != null) {
            try {
                val type = object : TypeToken<List<SaveSlotInfo>>() {}.type // Expect List
                val slots: List<SaveSlotInfo>? = gson.fromJson(json, type)
                if (slots != null) {
                    currentSaveSlots = slots.toMutableList()
                    Log.d("SaveLoadManager", "Successfully loaded ${currentSaveSlots.size} slots from SharedPreferences.")
                    successfullyLoaded = true
                } else {
                    Log.w("SaveLoadManager", "Parsed JSON was null. Initializing empty slots.")
                }
            } catch (e: Exception) {
                Log.e("SaveLoadManager", "Error parsing save slots JSON from SharedPreferences. Initializing empty slots.", e)
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

    /**
     * Gets a copy of all current save slot information.
     */
    fun getAllSaveSlotInfo(): List<SaveSlotInfo> {
        return currentSaveSlots.map { it.copy() }
    }

    /**
     * Call this when a player chooses to start a new game in a specific slot.
     * This will mark the slot as no longer empty and set initial game data.
     */
    suspend fun startNewGameInSlot(slotId: Int, chapterName: String = "Chapter 1: The Beginning", sceneId: String = "intro_scene_01"): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (!slot.isEmpty) {
                    Log.w("SaveLoadManager", "Attempting to start new game in an already occupied slot $slotId. Overwriting.")
                }
                slot.isEmpty = false
                slot.chapterName = chapterName
                slot.currentSceneId = sceneId
                slot.lastSavedTimestamp = getCurrentFormattedTimestamp()
                slot.gameVersion = "1.0" // Using BuildConfig here

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "New game started and saved in slot $slotId: $chapterName")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to start new game: Slot ID $slotId not found.")
                false
            }
        }
    }

    /**
     * Loads the game data for a specific, non-empty slot.
     * For now, it just returns the SaveSlotInfo. In a real game, this would
     * deserialize the full game state.
     */
    suspend fun loadGameFromSlot(slotId: Int): SaveSlotInfo? {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null && !slot.isEmpty) {
                Log.i("SaveLoadManager", "Game data for slot $slotId prepared for loading: ${slot.chapterName}")
                // You will need to implement actual game state loading logic.
                slot.copy()
            } else {
                Log.w("SaveLoadManager", "Attempted to load from empty or non-existent slot $slotId.")
                null
            }
        }
    }

    /**
     * Updates the game data for an existing save slot.
     * This is what you'd call during gameplay to save progress.
     */
    @Suppress ("unused")
    suspend fun saveGameToSlot(slotId: Int, currentChapter: String, currentScene: String /*, other game data */): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (slot.isEmpty) {
                    Log.w("SaveLoadManager", "Saving to an initially empty slot $slotId. Marking as not empty.")
                    slot.isEmpty = false
                }
                slot.chapterName = currentChapter
                slot.currentSceneId = currentScene
                slot.lastSavedTimestamp = getCurrentFormattedTimestamp()
                // You will need to add parameters and logic for other game data.

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "Game progress saved to slot $slotId: $currentChapter")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to save game: Slot ID $slotId not found.")
                false
            }
        }
    }

    /**
     * Deletes the save data for a specific slot, marking it as empty.
     */
    suspend fun deleteSaveSlot(slotId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val slot = currentSaveSlots.find { it.slotId == slotId }
            if (slot != null) {
                if (slot.isEmpty) {
                    Log.w("SaveLoadManager", "Attempted to delete an already empty slot $slotId.")
                    return@withContext true
                }
                slot.isEmpty = true
                slot.chapterName = null
                slot.lastSavedTimestamp = null
                slot.currentSceneId = null
                slot.gameVersion = null
                // You will need to add logic to clear any other data.

                persistSaveSlotsToPreferences()
                Log.i("SaveLoadManager", "Save data deleted for slot $slotId.")
                true
            } else {
                Log.e("SaveLoadManager", "Failed to delete save: Slot ID $slotId not found.")
                false
            }
        }
    }

    private fun getCurrentFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // Explanatory comments about more complex save/load for full game state were here.
    // In a real game, each slot might point to a separate file or database entry
    // for the full game state, not just the metadata in SaveSlotInfo.
}