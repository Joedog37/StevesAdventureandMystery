package com.example.stevesadeventureandmystery.game.scenes

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.stevesadeventureandmystery.R // IMPORT YOUR R FILE
import com.example.stevesadeventureandmystery.game.core.BootStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Object for AssetKeys - useful if you load them dynamically later, for now mostly for conceptual reference
object AssetKeys {
    const val BOOT_MUSIC = "audio/music/space_explorers.mp3" // Example
    // If you had specific keys for drawables for dynamic loading, they'd go here.
    // For direct R.drawable usage, it's less critical here but good practice for other assets.
}

const val BOOT_SCENE_TAG = "BootScene" // Simplified tag

class BootScene(
    private val onComplete: () -> Unit,
    private val scope: CoroutineScope
) {
    private var currentStage: BootStage by mutableStateOf(BootStage.IDLE)

    // UI State Holders - public read, private write
    var warningTextContent by mutableStateOf("")
        private set
    var showWarningText by mutableStateOf(false)
        private set

    var logoDrawableRes by mutableIntStateOf(0) // 0 means no drawable
        private set
    var showLogoImage by mutableStateOf(false)
        private set

    var titleTextContent by mutableStateOf("")
        private set
    var showTitleText by mutableStateOf(false)
        private set

    var backgroundDrawableRes by mutableIntStateOf(0) // 0 means no drawable
    var backgroundAlpha by mutableFloatStateOf(0f) // For fade-in effect
        private set

    var showAudioPromptElements by mutableStateOf(false)
        private set
    var audioPromptTextContent by mutableStateOf("Enable Audio?")
        private set

    var showContinueButtonElements by mutableStateOf(false)
        private set
    var continueButtonTextContent by mutableStateOf("Continue")
        private set


    init {
        Log.d(BOOT_SCENE_TAG, "BootScene instance CREATED. Initial stage: $currentStage")
    }

    fun start() {
        Log.d(BOOT_SCENE_TAG, "start() - Current stage: $currentStage")
        if (currentStage != BootStage.IDLE && currentStage != BootStage.COMPLETED) {
            Log.w(BOOT_SCENE_TAG, "start() called but scene is not in IDLE or COMPLETED state. Resetting for new start.")
            // Reset all UI states if restarting mid-way is a valid scenario
            cleanupUI(fullReset = true) // fullReset to ensure all states are cleared
        }
        currentStage = BootStage.IDLE // Ensure it starts from IDLE
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "start() coroutine LAUNCHED.")
            currentStage = BootStage.LOADING_ASSETS
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            loadAssets()
        }
    }

    private fun loadAssets() {
        Log.d(BOOT_SCENE_TAG, "loadAssets() - Simulating asset loading...")
        scope.launch {
            // Simulate asset pre-loading if any (e.g., image paths, sound URIs)
            // For drawables, they are already part of the APK.
            // You might set your drawable resources here.
            logoDrawableRes = R.drawable.thunderhead_pictures_new_logo_processed // REPLACE with your actual logo
            backgroundDrawableRes = R.drawable.pexels_krisof_1252890 // REPLACE with your actual background
            titleTextContent = "Steve's Adventure!" // Set your game title

            delay(500) // Simulate asset loading time
            Log.d(BOOT_SCENE_TAG, "Assets loaded (simulated).")
            currentStage = BootStage.SHOWING_WARNING
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showWarning()
        }
    }

    private fun showWarning() {
        Log.d(BOOT_SCENE_TAG, "showWarning()")
        scope.launch {
            warningTextContent = "Photosensitivity Warning: This game may contain flashing lights." // Example warning
            showWarningText = true
            delay(2000) // Warning display time
            showWarningText = false // Hide warning
            delay(300) // Brief pause before next element

            currentStage = BootStage.SHOWING_LOGO
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showLogo()
        }
    }

    private fun showLogo() {
        Log.d(BOOT_SCENE_TAG, "showLogo()")
        scope.launch {
            if (logoDrawableRes != 0) {
                showLogoImage = true
                delay(2000) // Logo display time
                showLogoImage = false // Hide logo
                delay(300) // Brief pause
            } else {
                Log.w(BOOT_SCENE_TAG, "Logo drawable resource not set.")
            }

            currentStage = BootStage.FADING_IN_BACKGROUND
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            fadeInBackground()
        }
    }

    private fun fadeInBackground() {
        Log.d(BOOT_SCENE_TAG, "fadeInBackground()")
        scope.launch {
            if (backgroundDrawableRes != 0) {
                // Animate alpha from 0f to 1f over a duration
                val animationDuration = 1000L
                val steps = 20
                for (i in 0..steps) {
                    backgroundAlpha = i.toFloat() / steps
                    delay(animationDuration / steps)
                }
                backgroundAlpha = 1f // Ensure it's fully opaque
            } else {
                Log.w(BOOT_SCENE_TAG, "Background drawable resource not set.")
            }
            delay(500) // Time background is visible before title

            currentStage = BootStage.SHOWING_TITLE
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showTitle()
        }
    }

    private fun showTitle() {
        Log.d(BOOT_SCENE_TAG, "showTitle()")
        scope.launch {
            showTitleText = true
            delay(2000) // Title display time (it remains visible over the background)

            currentStage = BootStage.AWAITING_AUDIO_PROMPT
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showAudioPrompt()
        }
    }

    private fun showAudioPrompt() {
        Log.d(BOOT_SCENE_TAG, "showAudioPrompt()")
        scope.launch {
            // Title and background are expected to be visible here
            showAudioPromptElements = true
            // Stage is already AWAITING_AUDIO_PROMPT
            Log.d(BOOT_SCENE_TAG, "Now awaiting audio prompt input.")
        }
    }

    fun handleAudioPromptInput() {
        Log.d(BOOT_SCENE_TAG, "handleAudioPromptInput - Current stage: $currentStage")
        if (currentStage != BootStage.AWAITING_AUDIO_PROMPT) {
            Log.w(BOOT_SCENE_TAG, "Called but not in AWAITING_AUDIO_PROMPT stage.")
            return
        }
        scope.launch {
            showAudioPromptElements = false // Hide prompt
            Log.d(BOOT_SCENE_TAG, "Audio prompt input received. Simulating playing music: ${AssetKeys.BOOT_MUSIC}")
            delay(100) // Simulate audio start
            currentStage = BootStage.SHOWING_CONTINUE_BUTTON
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showContinueButton()
        }
    }

    private fun showContinueButton() {
        Log.d(BOOT_SCENE_TAG, "showContinueButton()")
        scope.launch {
            // Title and background are expected to be visible
            showContinueButtonElements = true
            currentStage = BootStage.AWAITING_CONTINUE_INPUT
            Log.d(BOOT_SCENE_TAG, "Now awaiting continue input.")
        }
    }

    fun handleContinueInput() {
        Log.d(BOOT_SCENE_TAG, "handleContinueInput - Current stage: $currentStage")
        if (currentStage != BootStage.AWAITING_CONTINUE_INPUT) {
            Log.w(BOOT_SCENE_TAG, "Called but not in AWAITING_CONTINUE_INPUT stage.")
            return
        }
        scope.launch {
            showContinueButtonElements = false // Hide continue button
            delay(100)
            currentStage = BootStage.COMPLETED
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            completeBootSequence()
        }
    }

    private fun completeBootSequence() {
        Log.d(BOOT_SCENE_TAG, "completeBootSequence()")
        scope.launch {
            cleanupUI() // Clean up all UI elements from BootScene
            Log.d(BOOT_SCENE_TAG, "Boot sequence complete! Notifying MainActivity.")
            delay(100) // Final delay
            onComplete()
        }
    }

    private fun cleanupUI(fullReset: Boolean = false) {
        Log.d(BOOT_SCENE_TAG, "cleanupUI() called. Full reset: $fullReset")
        showWarningText = false
        showLogoImage = false
        // Title and background might stay if transitioning to main menu that uses them,
        // or clear them too if main menu has a completely different look.
        // For a clean transition, let's hide them for now.
        showTitleText = false
        backgroundAlpha = 0f // Fade out background
        showAudioPromptElements = false
        showContinueButtonElements = false

        if (fullReset) {
            currentStage = BootStage.IDLE
            warningTextContent = ""
            logoDrawableRes = 0
            titleTextContent = ""
            backgroundDrawableRes = 0
            // Reset other content strings if necessary
        }
    }
}