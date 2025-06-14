package com.example.stevesadventureandmystery.game.data

import com.google.gson.annotations.SerializedName // For potential JSON serialization later

data class SaveSlotInfo(
    @SerializedName("slot_id")
    val slotId: Int,

    @SerializedName("is_empty")
    var isEmpty: Boolean = true,

    @SerializedName("chapter_name")
    var chapterName: String? = null,

    @SerializedName("last_saved_timestamp")
    var lastSavedTimestamp: String? = null, // Could be Long for System.currentTimeMillis()

    @SerializedName("current_scene_id")
    var currentSceneId: String? = null, // Or whatever identifies player location

    @SerializedName("game_version")
    var gameVersion: String? = null,

    // Add any other fields you want to save per slot
    // e.g., var playerName: String? = null,
    // e.g., var playerHealth: Int? = null,
    // e.g., var thumbnailPath: String? = null
) {
    // Optional: Add a constructor for creating a truly empty slot if needed,
    // though default parameters often cover this.
    // constructor(slotId: Int) : this(slotId, true, null, null, null, null)
}