package com.example.stevesadventureandmystery.game.scenes

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.stevesadventureandmystery.R
import com.example.stevesadventureandmystery.game.core.BootStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

const val BOOT_SCENE_TAG = "BootScene"

class BootScene(
    private val context: Context,
    private val onComplete: (transitionToMainMenu: Boolean) -> Unit,
    scope: CoroutineScope // Changed: Removed 'private val'
) {
    // sceneJob is a child of the passed 'scope'. If 'scope' is lifecycleScope from an Activity/ViewModel,
    // its cancellation will propagate here.
    private val sceneJob = SupervisorJob(scope.coroutineContext[Job])
    private val sceneScope = CoroutineScope(scope.coroutineContext + sceneJob)

    var currentStage by mutableStateOf(BootStage.IDLE)
    var currentErrorMessage by mutableStateOf<String?>(null)

    var warningTextContent by mutableStateOf("Warning: This game contains flashing lights and may not be suitable for individuals with photosensitive epilepsy. Player discretion is advised.")
    var logoDrawableRes by mutableIntStateOf(0)
    var backgroundDrawableRes by mutableIntStateOf(0)
    var titleTextContent by mutableStateOf("Game Title")
    var continueButtonTextContent by mutableStateOf("Continue")

    var showWarningText by mutableStateOf(false)
    var showLogoImage by mutableStateOf(false)
    var showTitleText by mutableStateOf(false)
    var showContinueButtonElements by mutableStateOf(false)

    var backgroundAlpha by mutableFloatStateOf(0f)
    val defaultFadeDurationMillis = 1000L
    val assetLoadingSimulatedDelayMillis = 1000L
    val initialAssetLoadDelayMillis = 500L
    val warningDisplayDurationMillis = 3500L
    val logoDisplayDurationMillis = 2000L
    val titleDisplayDurationMillis = 2500L // Duration title is shown *before* continue button appears
    val backgroundFadeAnimationSteps = 30
    val backgroundExtraFadeDurationMillis = 1000L

    private var mediaPlayer: MediaPlayer? = null
    private var musicJob: Job? = null // Job specifically for the music loading/starting coroutine
    var isMusicPlayingState by mutableStateOf(false) // For Jetpack Compose UI to react
    private var wasMusicPlayingBeforePause = false // Internal flag for pause/resume logic

    var backgroundMusicAssetPath by mutableStateOf<String?>(null)

    fun getMediaPlayerInstance(): MediaPlayer? {
        return mediaPlayer
    }

    fun relinquishMediaPlayerControl() {
        Log.d(BOOT_SCENE_TAG, "MediaPlayer control relinquished.")
        mediaPlayer = null
        isMusicPlayingState = false // Reflect that this instance is no longer managing it
        musicJob?.cancel() // Cancel any pending music operations from BootScene
    }

    fun isBootSequenceActive(): Boolean {
        return currentStage != BootStage.IDLE &&
                currentStage != BootStage.COMPLETED &&
                currentStage != BootStage.ERROR
    }

    // In the start() method of BootScene.kt
    fun start() {
        if (currentStage == BootStage.IDLE || currentStage == BootStage.COMPLETED || currentStage == BootStage.ERROR) {
            Log.d(BOOT_SCENE_TAG, "Boot sequence started.")
            // resetState(keepMusicPlaying = false) // Old call
            resetState() // Updated call
            currentStage = BootStage.LOADING_ASSETS
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            loadAssets()
        } else {
            Log.w(BOOT_SCENE_TAG, "Boot sequence already in progress or not in a startable state: $currentStage")
        }
    }

    private fun resetState() {
        Log.d(BOOT_SCENE_TAG, "Resetting BootScene state.")
        sceneJob.cancelChildren() // Cancel all non-music coroutines specific to this scene's progression

        stopAndReleaseMusicPlayer() // Music is always stopped and released on reset

        showWarningText = false
        showLogoImage = false
        showTitleText = false
        showContinueButtonElements = false
        backgroundAlpha = 0f
        logoDrawableRes = 0
        backgroundDrawableRes = 0
        currentErrorMessage = null
        currentStage = BootStage.IDLE
        wasMusicPlayingBeforePause = false // Reset this flag too
    }

    private fun loadAssets() {
        Log.d(BOOT_SCENE_TAG, "loadAssets() - Loading assets...")
        sceneScope.launch {
            try {
                delay(initialAssetLoadDelayMillis) // Simulate initial non-UI work
                logoDrawableRes = R.drawable.thunderhead_pictures_new_logo_processed
                backgroundDrawableRes = R.drawable.pexels_krisof_1252890
                titleTextContent = "Steve's Space Adventure & Mystery"
                backgroundMusicAssetPath = "audio/music/space_explorers.mp3"

                delay(assetLoadingSimulatedDelayMillis) // Simulate asset loading
                Log.d(BOOT_SCENE_TAG, "Assets loaded.")
                currentStage = BootStage.SHOWING_WARNING
                Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
                showWarning()
            } catch (e: Exception) {
                if (isActive) { // Check if coroutine is still active before handling error
                    Log.e(BOOT_SCENE_TAG, "Error loading assets: ${e.message}", e)
                    handleError("Failed to load game assets. Details: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    private fun showWarning() {
        Log.d(BOOT_SCENE_TAG, "showWarning()")
        sceneScope.launch {
            showWarningText = true
            delay(warningDisplayDurationMillis)
            if (!isActive) return@launch // Exit if coroutine or scene was cancelled
            showWarningText = false
            delay(defaultFadeDurationMillis / 2)
            if (!isActive) return@launch

            currentStage = BootStage.SHOWING_LOGO
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showLogo()
        }
    }

    private fun showLogo() {
        Log.d(BOOT_SCENE_TAG, "showLogo()")
        sceneScope.launch {
            if (logoDrawableRes != 0) {
                showLogoImage = true
                delay(logoDisplayDurationMillis)
                if (!isActive) return@launch
                showLogoImage = false // Start fading out logo or hide it before background fades in
            } else {
                Log.w(BOOT_SCENE_TAG, "Logo drawable resource not set, skipping direct display.")
            }

            // Start background fade-in concurrently or just after logo starts showing/hiding
            currentStage = BootStage.FADING_IN_BACKGROUND
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage. Initiating background fade.")
            fadeInBackgroundVisuals() // Call directly, it launches its own coroutine if needed

            // Removed postLogoDelay here, let fadeInBackgroundVisuals and showTitleAndMusic manage timing
        }
    }


    private fun fadeInBackgroundVisuals() {
        Log.d(BOOT_SCENE_TAG, "fadeInBackgroundVisuals()")
        sceneScope.launch {
            if (backgroundDrawableRes != 0) {
                val animationDuration = defaultFadeDurationMillis + backgroundExtraFadeDurationMillis
                for (i in 0..backgroundFadeAnimationSteps) {
                    if (!isActive) return@launch
                    backgroundAlpha = i.toFloat() / backgroundFadeAnimationSteps
                    delay(animationDuration / backgroundFadeAnimationSteps)
                }
                backgroundAlpha = 1f
                Log.d(BOOT_SCENE_TAG, "Background image fade-in complete. Alpha: $backgroundAlpha")
            } else {
                Log.w(BOOT_SCENE_TAG, "Background drawable resource not set. Setting alpha to 1f conceptually.")
                backgroundAlpha = 1f // Show content on top of it
            }

            if (!isActive) return@launch
            currentStage = BootStage.SHOWING_TITLE
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showTitleAndMusic()
        }
    }

    private fun showTitleAndMusic() {
        Log.d(BOOT_SCENE_TAG, "showTitleAndMusic()")
        sceneScope.launch {
            showTitleText = true
            Log.d(BOOT_SCENE_TAG, "Title visibility set to true.")

            if (mediaPlayer == null && !backgroundMusicAssetPath.isNullOrEmpty()) {
                initializeAndPrepareMusic(backgroundMusicAssetPath!!)
            } else if (mediaPlayer != null && !mediaPlayer!!.isPlaying && wasMusicPlayingBeforePause) {
                // If player exists, wasn't playing (likely paused), and should be playing
                Log.d(BOOT_SCENE_TAG, "Resuming music from showTitleAndMusic after it was paused.")
                resumeMusicPlayback() // Try to resume
            } else if (mediaPlayer?.isPlaying == true) {
                Log.d(BOOT_SCENE_TAG, "Music is already playing. No action needed in showTitleAndMusic.")
                isMusicPlayingState = true
            } else {
                Log.w(BOOT_SCENE_TAG, "MediaPlayer conditions not met to start/resume in showTitleAndMusic (Player: $mediaPlayer, Path: $backgroundMusicAssetPath).")
            }

            delay(titleDisplayDurationMillis) // How long title is shown before button
            if (!isActive) return@launch

            currentStage = BootStage.SHOWING_CONTINUE_BUTTON
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            showContinueButton()
        }
    }

    // In BootScene.kt

    private fun initializeAndPrepareMusic(assetPath: String) {
        if (!sceneScope.isActive) {
            Log.d(BOOT_SCENE_TAG, "Scene scope no longer active, not initializing music.")
            return
        }

        musicJob?.cancel() // Cancel any previous music job
        musicJob = sceneScope.launch {
            if (mediaPlayer != null) {
                Log.w(BOOT_SCENE_TAG, "MediaPlayer instance already exists. Releasing before creating new.")
                stopAndReleaseMusicPlayer()
            }

            Log.d(BOOT_SCENE_TAG, "Initializing and preparing music from assets: $assetPath")
            mediaPlayer = MediaPlayer()
            var afd: AssetFileDescriptor? = null
            try {
                afd = context.assets.openFd(assetPath) // Assign afd here
                mediaPlayer?.apply {
                    // Since openFd throws an exception if the file is not found,
                    // afd will be non-null here if we reach this point.
                    // However, to be absolutely safe and satisfy linters without `!!`,
                    // you can use a local non-null variable.
                    val validAfd = afd

                    setDataSource(validAfd.fileDescriptor, validAfd.startOffset, validAfd.length)
                    isLooping = true
                    setOnPreparedListener { mp ->
                        Log.d(BOOT_SCENE_TAG, "MediaPlayer prepared.")
                        if (sceneScope.isActive) {
                            try {
                                mp.start()
                                isMusicPlayingState = true
                                wasMusicPlayingBeforePause = true
                                Log.d(BOOT_SCENE_TAG, "SUCCESS: Music started from assets.")
                            } catch (e: IllegalStateException) {
                                Log.e(BOOT_SCENE_TAG, "IllegalStateException on mp.start(): ${e.message}", e)
                                isMusicPlayingState = false
                                wasMusicPlayingBeforePause = false
                                stopAndReleaseMusicPlayer()
                            }
                        } else {
                            Log.d(BOOT_SCENE_TAG, "MediaPlayer prepared, but scene no longer active. Not starting.")
                            isMusicPlayingState = false
                            wasMusicPlayingBeforePause = false
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(BOOT_SCENE_TAG, "MediaPlayer Error (assets): what: $what, extra: $extra")
                        isMusicPlayingState = false
                        wasMusicPlayingBeforePause = false
                        stopAndReleaseMusicPlayer()
                        true
                    }
                    prepareAsync()
                    Log.d(BOOT_SCENE_TAG, "MediaPlayer prepareAsync() called.")
                }
            } catch (e: IOException) {
                Log.e(BOOT_SCENE_TAG, "IOException preparing music from '$assetPath': ${e.message}", e)
                isMusicPlayingState = false
                wasMusicPlayingBeforePause = false
                stopAndReleaseMusicPlayer()
            } catch (e: IllegalStateException) {
                Log.e(BOOT_SCENE_TAG, "IllegalStateException setting up MediaPlayer for '$assetPath': ${e.message}", e)
                isMusicPlayingState = false
                wasMusicPlayingBeforePause = false
                stopAndReleaseMusicPlayer()
            } finally {
                try {
                    afd?.close() // Safely close afd here
                } catch (e: IOException) {
                    Log.e(BOOT_SCENE_TAG, "Error closing AssetFileDescriptor: ${e.message}", e)
                }
            }
        }
    }

    fun pauseMusic() {
        if (!sceneScope.isActive && mediaPlayer == null) {
            Log.d(BOOT_SCENE_TAG, "Scene not active or no media player to pause.")
            return
        }
        musicJob?.cancel() // Cancel any pending start attempts
        mediaPlayer?.let {
            if (it.isPlaying) {
                try {
                    it.pause()
                    isMusicPlayingState = false
                    // wasMusicPlayingBeforePause is already true if it was playing
                    Log.d(BOOT_SCENE_TAG, "BootScene music PAUSED.")
                } catch (e: IllegalStateException) {
                    Log.e(BOOT_SCENE_TAG, "IllegalStateException on mediaPlayer.pause(): ${e.message}", e)
                    stopAndReleaseMusicPlayer() // If pause fails, release
                }
            } else {
                Log.d(BOOT_SCENE_TAG, "pauseMusic called, but player was not playing.")
            }
        }
    }

    fun resumeMusic() {
        if (!sceneScope.isActive) {
            Log.d(BOOT_SCENE_TAG, "Scene not active, not resuming music.")
            return
        }
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying && wasMusicPlayingBeforePause) {
            Log.d(BOOT_SCENE_TAG, "Attempting to RESUME BootScene music.")
            resumeMusicPlayback()
        } else if (mediaPlayer?.isPlaying == true) {
            Log.d(BOOT_SCENE_TAG, "resumeMusic called, but music is already playing.")
            isMusicPlayingState = true // Ensure state is correct
        } else if (mediaPlayer == null && wasMusicPlayingBeforePause && !backgroundMusicAssetPath.isNullOrEmpty()) {
            Log.d(BOOT_SCENE_TAG, "MediaPlayer was null but was playing before pause. Re-initializing for resume.")
            initializeAndPrepareMusic(backgroundMusicAssetPath!!)
        }
        else {
            Log.d(BOOT_SCENE_TAG, "Conditions not met to resume BootScene music (Player: $mediaPlayer, wasPlayingBeforePause: $wasMusicPlayingBeforePause).")
        }
    }

    private fun resumeMusicPlayback() {
        mediaPlayer?.let {
            try {
                it.start()
                isMusicPlayingState = true
                Log.d(BOOT_SCENE_TAG, "BootScene music RESUMED successfully.")
            } catch (e: IllegalStateException) {
                Log.e(BOOT_SCENE_TAG, "IllegalStateException on mediaPlayer.start() during resume: ${e.message}", e)
                // If resume fails, it might be in a bad state.
                // Consider re-preparing or releasing. For now, just log and it remains paused.
                isMusicPlayingState = false
                // wasMusicPlayingBeforePause remains true, so next onResume might try again or re-init.
            }
        }
    }


    private fun stopAndReleaseMusicPlayer() {
        musicJob?.cancel()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                    Log.d(BOOT_SCENE_TAG, "MediaPlayer stopped.")
                }
                it.release()
                Log.d(BOOT_SCENE_TAG, "MediaPlayer released.")
            } catch (e: IllegalStateException) {
                Log.e(BOOT_SCENE_TAG, "IllegalStateException during stop/release of MediaPlayer: ${e.message}")
                try { it.release() } catch (ex: Exception) { Log.e(BOOT_SCENE_TAG, "Secondary release attempt failed", ex) }
            } catch (e: Exception) { // Catch any other exception during release
                Log.e(BOOT_SCENE_TAG, "Exception during stop/release of MediaPlayer: ${e.message}", e)
            }
        }
        mediaPlayer = null
        isMusicPlayingState = false
        wasMusicPlayingBeforePause = false // Crucial: reset this when fully stopped/released
    }

    private fun showContinueButton() {
        Log.d(BOOT_SCENE_TAG, "showContinueButton()")
        sceneScope.launch {
            if (!isActive) return@launch
            showContinueButtonElements = true
            currentStage = BootStage.AWAITING_CONTINUE_INPUT
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage. Waiting for user input.")
        }
    }

    fun handleContinueInput() {
        if (currentStage == BootStage.AWAITING_CONTINUE_INPUT) {
            Log.d(BOOT_SCENE_TAG, "Continue input received.")
            // Don't launch a new coroutine here if you want to ensure cleanup happens
            // after onComplete is fully processed by MainActivity.
            // The sceneJob.cancelChildren() in resetState or cleanup will handle stopping sequences.

            showContinueButtonElements = false
            currentStage = BootStage.COMPLETED
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage. Boot sequence complete.")
            // Music is NOT stopped here by default. MainActivity will decide.
            onComplete(true)
        } else {
            Log.w(BOOT_SCENE_TAG, "Continue input received but not in AWAITING_CONTINUE_INPUT stage. Current stage: $currentStage")
        }
    }

    private fun handleError(errorMessage: String) {
        if (!sceneScope.isActive) return // Avoid handling error if scope is already cancelled

        Log.e(BOOT_SCENE_TAG, "BootScene Error: $errorMessage")
        currentErrorMessage = errorMessage
        currentStage = BootStage.ERROR
        stopAndReleaseMusicPlayer() // Stop and release music on error
        sceneJob.cancelChildren() // Cancel other ongoing scene tasks
        onComplete(false)
    }

    fun cleanup() {
        Log.d(BOOT_SCENE_TAG, "BootScene cleanup() called.")
        // Cancel all coroutines launched within sceneScope.
        // This includes scene progression, animations, and the musicJob if it's running.
        sceneJob.cancelChildren() // Cancels children of SupervisorJob
        sceneJob.cancel()         // Cancels the SupervisorJob itself

        // If mediaPlayer is not null here, it means BootScene is still responsible for it
        // (i.e., it wasn't relinquished or was reacquired).
        if (mediaPlayer != null) {
            Log.d(BOOT_SCENE_TAG, "Cleaning up BootScene's MediaPlayer as it was not (or no longer) relinquished.")
            stopAndReleaseMusicPlayer()
        } else {
            Log.d(BOOT_SCENE_TAG, "MediaPlayer was null (likely relinquished or never initialized), not attempting to release in BootScene cleanup.")
        }
        Log.d(BOOT_SCENE_TAG, "BootScene cleanup finished.")
    }
}