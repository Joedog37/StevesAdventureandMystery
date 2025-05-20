package com.example.stevesadeventureandmystery.game.scenes

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.stevesadeventureandmystery.game.core.BootStage // <--- ADD THIS IMPORT

// Placeholder for your AssetKeys if not defined elsewhere
object AssetKeys {
    const val BOOT_MUSIC = "audio/music/space_explorers.mp3" // Example
    // Add other asset keys if used by BootScene
}

const val BOOT_SCENE_TAG = "BootScene_DEBUG" // Unique tag for BootScene logs

class BootScene(
    private val onComplete: () -> Unit,
    private val scope: CoroutineScope // From Activity's lifecycleScope
) {
    private var currentStage: BootStage = BootStage.IDLE
    private val visibleElements = mutableSetOf<String>() // To track "visible" elements conceptually

    init {
        Log.d(BOOT_SCENE_TAG, "BootScene instance CREATED. Initial stage: $currentStage")
    }

    fun start() {
        Log.d(BOOT_SCENE_TAG, "start() method CALLED. Current stage: $currentStage")
        if (currentStage != BootStage.IDLE) {
            Log.w(BOOT_SCENE_TAG, "start() called but scene is not in IDLE state. Current: $currentStage. Resetting for new start.")
            // Optionally reset state here if re-start is possible, or just log and return.
            // For now, let's allow it to proceed to see if coroutine launches.
        }
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "start() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: Starting boot sequence...") // Using println as per original
            currentStage = BootStage.LOADING_ASSETS
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            loadAssets()
        }
        Log.d(BOOT_SCENE_TAG, "start() method finished launching coroutine.")
    }

    private fun loadAssets() {
        Log.d(BOOT_SCENE_TAG, "loadAssets() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "loadAssets() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [ASSETS] Loading assets...")
            println("$BOOT_SCENE_TAG: BootScene: [ASSETS] Simulating asset loading delay...")
            delay(500) // Simulate asset loading time
            println("$BOOT_SCENE_TAG: BootScene: [ASSETS] Assets loaded (simulated).")
            currentStage = BootStage.SHOWING_WARNING
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showWarning()
        }
        Log.d(BOOT_SCENE_TAG, "loadAssets() method finished launching coroutine.")
    }

    private fun showWarning() {
        Log.d(BOOT_SCENE_TAG, "showWarning() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "showWarning() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Showing: warningText (simulating fade-in)")
            visibleElements.add("warningText")
            delay(1000) // Simulate fade-in time
            println("$BOOT_SCENE_TAG: BootScene: warningText fully visible. Auto-advancing...")
            currentStage = BootStage.SHOWING_LOGO
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showLogo()
        }
        Log.d(BOOT_SCENE_TAG, "showWarning() method finished launching coroutine.")
    }

    private fun showLogo() {
        Log.d(BOOT_SCENE_TAG, "showLogo() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "showLogo() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Showing: gameLogo (simulating fade-in)")
            visibleElements.add("gameLogo")
            delay(1000)
            println("$BOOT_SCENE_TAG: BootScene: gameLogo fully visible. Auto-advancing...")
            currentStage = BootStage.FADING_IN_BACKGROUND
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            fadeInBackground()
        }
        Log.d(BOOT_SCENE_TAG, "showLogo() method finished launching coroutine.")
    }

    private fun fadeInBackground() {
        Log.d(BOOT_SCENE_TAG, "fadeInBackground() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "fadeInBackground() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Fading in: mainBackground (simulated)")
            visibleElements.add("mainBackground")
            delay(500)
            println("$BOOT_SCENE_TAG: BootScene: mainBackground fully visible. Auto-advancing...")
            currentStage = BootStage.SHOWING_TITLE
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showTitle()
        }
        Log.d(BOOT_SCENE_TAG, "fadeInBackground() method finished launching coroutine.")
    }

    private fun showTitle() {
        Log.d(BOOT_SCENE_TAG, "showTitle() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "showTitle() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Showing: gameTitle (simulating fade-in)")
            visibleElements.add("gameTitle")
            delay(500)
            println("$BOOT_SCENE_TAG: BootScene: gameTitle fully visible. Auto-advancing...")
            currentStage = BootStage.AWAITING_AUDIO_PROMPT
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showAudioPrompt()
        }
        Log.d(BOOT_SCENE_TAG, "showTitle() method finished launching coroutine.")
    }

    private fun showAudioPrompt() {
        Log.d(BOOT_SCENE_TAG, "showAudioPrompt() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "showAudioPrompt() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Showing: audioPromptText (simulating fade-in)")
            visibleElements.add("audioPromptText")
            delay(300)
            println("$BOOT_SCENE_TAG: BootScene: audioPromptText fully visible. Waiting for input...")
            // Stage is already AWAITING_AUDIO_PROMPT
        }
        Log.d(BOOT_SCENE_TAG, "showAudioPrompt() method finished launching coroutine.")
    }

    fun handleAudioPromptInput() {
        Log.d(BOOT_SCENE_TAG, "handleAudioPromptInput CALLED. Current stage: $currentStage")
        if (currentStage != BootStage.AWAITING_AUDIO_PROMPT) {
            Log.w(BOOT_SCENE_TAG, "handleAudioPromptInput: Called but not in AWAITING_AUDIO_PROMPT stage.")
            return
        }
        scope.launch { // Input handling can also be in a coroutine if it involves delays/async ops
            Log.d(BOOT_SCENE_TAG, "handleAudioPromptInput coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: Audio prompt input received.")
            visibleElements.remove("audioPromptText")
            println("$BOOT_SCENE_TAG: BootScene: [AUDIO] Playing ${AssetKeys.BOOT_MUSIC} (simulated)")
            delay(100) // Simulate audio start
            currentStage = BootStage.SHOWING_CONTINUE_BUTTON
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showContinueButton()
        }
        Log.d(BOOT_SCENE_TAG, "handleAudioPromptInput method finished launching coroutine.")
    }

    private fun showContinueButton() {
        Log.d(BOOT_SCENE_TAG, "showContinueButton() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "showContinueButton() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: [DISPLAY] Showing: continueButtonText (simulating fade-in)")
            visibleElements.add("continueButtonText")
            delay(300)
            println("$BOOT_SCENE_TAG: BootScene: continueButtonText fully visible. Waiting for input...")
            currentStage = BootStage.AWAITING_CONTINUE_INPUT
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
        }
        Log.d(BOOT_SCENE_TAG, "showContinueButton() method finished launching coroutine.")
    }

    fun handleContinueInput() {
        Log.d(BOOT_SCENE_TAG, "handleContinueInput CALLED. Current stage: $currentStage")
        if (currentStage != BootStage.AWAITING_CONTINUE_INPUT) {
            Log.w(BOOT_SCENE_TAG, "handleContinueInput: Called but not in AWAITING_CONTINUE_INPUT stage.")
            return
        }
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "handleContinueInput coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: Continue input received.")
            visibleElements.remove("continueButtonText")
            // Simulate any action for continue
            delay(100)
            currentStage = BootStage.COMPLETED
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            completeBootSequence()
        }
        Log.d(BOOT_SCENE_TAG, "handleContinueInput method finished launching coroutine.")
    }

    private fun completeBootSequence() {
        Log.d(BOOT_SCENE_TAG, "completeBootSequence() CALLED. Current stage: $currentStage")
        scope.launch {
            Log.d(BOOT_SCENE_TAG, "completeBootSequence() coroutine LAUNCHED.")
            println("$BOOT_SCENE_TAG: BootScene: Boot sequence complete!")
            delay(100) // Final delay
            onComplete() // Call the callback to notify MainActivity
        }
        Log.d(BOOT_SCENE_TAG, "completeBootSequence() method finished launching coroutine.")
    }
}