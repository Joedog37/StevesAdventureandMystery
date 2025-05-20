package com.example.stevesadeventureandmystery.game.core // Adjust if your package is different

enum class BootStage {
    SHOWING_WARNING,
    SHOWING_LOGO,
    FADING_IN_BACKGROUND,
    SHOWING_TITLE,
    AWAITING_AUDIO_PROMPT,
    SHOWING_CONTINUE_BUTTON,
    IDLE,
    LOADING_ASSETS,
    AWAITING_CONTINUE_INPUT,
    COMPLETED
}