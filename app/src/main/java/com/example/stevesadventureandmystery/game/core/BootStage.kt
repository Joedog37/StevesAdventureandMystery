package com.example.stevesadventureandmystery.game.core

enum class BootStage {
    IDLE,
    LOADING_ASSETS,
    SHOWING_WARNING,
    SHOWING_LOGO,
    FADING_IN_BACKGROUND,
    SHOWING_TITLE, // Title and audio now happen under this logical stage or just before continue
    // AWAITING_AUDIO_PROMPT, // Kept commented as per your version
    SHOWING_CONTINUE_BUTTON,
    AWAITING_CONTINUE_INPUT,
    COMPLETED,
    ERROR // Recommended for handling issues
}