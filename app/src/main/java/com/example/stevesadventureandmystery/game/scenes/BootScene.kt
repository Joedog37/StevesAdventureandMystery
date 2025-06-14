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
    scope: CoroutineScope
) {
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
    val titleDisplayDurationMillis = 2500L
    val backgroundFadeAnimationSteps = 30
    val backgroundExtraFadeDurationMillis = 1000L

    private var mediaPlayer: MediaPlayer? = null
    private var musicJob: Job? = null
    var isMusicPlayingState by mutableStateOf(false)
    private var wasMusicPlayingBeforePause = false

    var backgroundMusicAssetPath by mutableStateOf<String?>(null)

    fun getMediaPlayerInstance(): MediaPlayer? {
        return mediaPlayer
    }

    fun relinquishMediaPlayerControl() {
        Log.d(BOOT_SCENE_TAG, "MediaPlayer control relinquished by BootScene.")
        musicJob?.cancel()
        mediaPlayer = null
        isMusicPlayingState = false
    }

    fun isBootSequenceActive(): Boolean {
        return currentStage != BootStage.IDLE &&
                currentStage != BootStage.COMPLETED &&
                currentStage != BootStage.ERROR
    }

    fun isBootSequenceCompletedAndBackgroundAvailable(): Boolean {
        return currentStage == BootStage.COMPLETED && backgroundDrawableRes != 0
    }

    fun start() {
        if (currentStage == BootStage.IDLE || currentStage == BootStage.COMPLETED || currentStage == BootStage.ERROR) {
            Log.d(BOOT_SCENE_TAG, "Boot sequence (re)starting.")
            resetState()
            currentStage = BootStage.LOADING_ASSETS
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
            loadAssets()
        } else {
            Log.w(BOOT_SCENE_TAG, "Boot sequence already in progress or not in a startable state: $currentStage")
        }
    }

    private fun resetState() {
        Log.d(BOOT_SCENE_TAG, "Resetting BootScene state.")
        sceneJob.cancelChildren()
        stopAndReleaseMusicPlayer()

        showWarningText = false
        showLogoImage = false
        showTitleText = false
        showContinueButtonElements = false
        backgroundAlpha = 0f
        logoDrawableRes = 0
        backgroundDrawableRes = 0
        currentErrorMessage = null
        currentStage = BootStage.IDLE
        wasMusicPlayingBeforePause = false
        Log.d(BOOT_SCENE_TAG, "BootScene state reset finished.")
    }

    private fun loadAssets() {
        Log.d(BOOT_SCENE_TAG, "loadAssets() - Loading assets...")
        sceneScope.launch {
            try {
                delay(initialAssetLoadDelayMillis)
                logoDrawableRes = R.drawable.thunderhead_pictures_new_logo_processed
                backgroundDrawableRes = R.drawable.pexels_krisof_1252890
                titleTextContent = "Steve's Space Adventure & Mystery"
                backgroundMusicAssetPath = "audio/music/space_explorers.mp3"

                delay(assetLoadingSimulatedDelayMillis)
                if (!isActive) return@launch
                Log.d(BOOT_SCENE_TAG, "Assets loaded.")
                currentStage = BootStage.SHOWING_WARNING
                Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage")
                showWarning()
            } catch (e: Exception) {
                if (isActive) {
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
            if (!isActive) return@launch
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
                showLogoImage = false
            } else {
                Log.w(BOOT_SCENE_TAG, "Logo drawable resource not set, skipping direct display.")
            }
            delay(defaultFadeDurationMillis / 3)
            if (!isActive) return@launch

            currentStage = BootStage.FADING_IN_BACKGROUND
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage. Initiating background fade.")
            fadeInBackgroundVisuals()
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
                Log.w(BOOT_SCENE_TAG, "Background drawable resource not set for fade. Setting alpha to 1f conceptually.")
                backgroundAlpha = 1f
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
                Log.d(BOOT_SCENE_TAG, "Resuming music from showTitleAndMusic as it was paused but should be playing.")
                resumeMusicPlayback()
            } else if (mediaPlayer?.isPlaying == true) {
                Log.d(BOOT_SCENE_TAG, "Music is already playing. No action needed in showTitleAndMusic.")
                isMusicPlayingState = true
            } else {
                Log.w(BOOT_SCENE_TAG, "MediaPlayer conditions not met to start/resume in showTitleAndMusic (Player: $mediaPlayer, Path: $backgroundMusicAssetPath, WasPlaying: $wasMusicPlayingBeforePause).")
            }

            delay(titleDisplayDurationMillis)
            if (!isActive) return@launch

            currentStage = BootStage.SHOWING_CONTINUE_BUTTON
            Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage (or preparing for AWAITING_CONTINUE_INPUT)")
            showContinueButton()
        }
    }

    private fun initializeAndPrepareMusic(assetPath: String) {
        if (!sceneScope.isActive) {
            Log.d(BOOT_SCENE_TAG, "Scene scope no longer active, not initializing music.")
            return
        }

        musicJob?.cancel()
        musicJob = sceneScope.launch {
            if (mediaPlayer != null) {
                Log.w(BOOT_SCENE_TAG, "MediaPlayer instance already exists in initializeAndPrepareMusic. Releasing before creating new.")
                stopAndReleaseMusicPlayer()
            }

            Log.d(BOOT_SCENE_TAG, "Initializing and preparing music from assets: $assetPath")
            mediaPlayer = MediaPlayer()
            var afd: AssetFileDescriptor? = null
            try {
                afd = context.assets.openFd(assetPath)
                mediaPlayer?.apply {
                    val validAfd = afd
                    setDataSource(validAfd.fileDescriptor, validAfd.startOffset, validAfd.length)
                    isLooping = true
                    setOnPreparedListener { mp ->
                        Log.d(BOOT_SCENE_TAG, "MediaPlayer prepared.")
                        if (sceneScope.isActive && mediaPlayer == mp) {
                            try {
                                mp.start()
                                isMusicPlayingState = true
                                wasMusicPlayingBeforePause = true
                                Log.d(BOOT_SCENE_TAG, "SUCCESS: Music started from assets.")
                            } catch (e: IllegalStateException) {
                                Log.e(BOOT_SCENE_TAG, "IllegalStateException on mp.start(): ${e.message}", e)
                                if (mediaPlayer == mp) stopAndReleaseMusicPlayer()
                                else mp.release()
                            }
                        } else {
                            Log.d(BOOT_SCENE_TAG, "MediaPlayer prepared, but scene/player no longer valid. Releasing this instance.")
                            mp.release()
                            if (mediaPlayer == mp) {
                                isMusicPlayingState = false
                                wasMusicPlayingBeforePause = false
                            }
                        }
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e(BOOT_SCENE_TAG, "MediaPlayer Error (assets): what: $what, extra: $extra")
                        if (mediaPlayer == mp) stopAndReleaseMusicPlayer()
                        else mp.release()
                        true
                    }
                    prepareAsync()
                    Log.d(BOOT_SCENE_TAG, "MediaPlayer prepareAsync() called.")
                }
            } catch (e: Exception) {
                Log.e(BOOT_SCENE_TAG, "Exception setting up MediaPlayer for '$assetPath': ${e.message}", e)
                stopAndReleaseMusicPlayer()
            } finally {
                try {
                    afd?.close()
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
        musicJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) {
                try {
                    it.pause()
                    isMusicPlayingState = false
                    Log.d(BOOT_SCENE_TAG, "BootScene music PAUSED.")
                } catch (e: IllegalStateException) {
                    Log.e(BOOT_SCENE_TAG, "IllegalStateException on mediaPlayer.pause(): ${e.message}", e)
                    stopAndReleaseMusicPlayer()
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
        mediaPlayer?.let { player ->
            if (!player.isPlaying && wasMusicPlayingBeforePause) {
                Log.d(BOOT_SCENE_TAG, "Attempting to RESUME BootScene music.")
                resumeMusicPlayback()
            } else if (player.isPlaying) {
                Log.d(BOOT_SCENE_TAG, "resumeMusic called, but music is already playing.")
                isMusicPlayingState = true
            } else {
                Log.d(BOOT_SCENE_TAG, "Conditions not met to resume BootScene music (Player exists but not playing OR wasMusicPlayingBeforePause is false).")
            }
        } ?: if (wasMusicPlayingBeforePause && !backgroundMusicAssetPath.isNullOrEmpty()) {
            Log.d(BOOT_SCENE_TAG, "MediaPlayer was null but was playing before pause. Re-initializing for resume.")
            initializeAndPrepareMusic(backgroundMusicAssetPath!!)
        } else {
            Log.d(BOOT_SCENE_TAG, "Conditions not met to resume BootScene music (Player is null and/or wasMusicPlayingBeforePause is false).")
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
                isMusicPlayingState = false
            }
        }
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(clampedVolume, clampedVolume)
            Log.d(BOOT_SCENE_TAG, "BootScene MediaPlayer volume set to: $clampedVolume")
        } catch (e: IllegalStateException) {
            Log.e(BOOT_SCENE_TAG, "Error setting volume on BootScene media player: ${e.message}")
        }
    }

    private fun stopAndReleaseMusicPlayer() {
        musicJob?.cancel()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                    Log.d(BOOT_SCENE_TAG, "MediaPlayer stopped in stopAndReleaseMusicPlayer.")
                }
                it.release()
                Log.d(BOOT_SCENE_TAG, "MediaPlayer released in stopAndReleaseMusicPlayer.")
            } catch (e: Exception) {
                Log.e(BOOT_SCENE_TAG, "Exception during stop/release of MediaPlayer: ${e.message}", e)
                try { if (mediaPlayer != null) it.release() } catch (ex: Exception) { Log.e(BOOT_SCENE_TAG, "Secondary release attempt failed", ex) }
            }
        }
        mediaPlayer = null
        isMusicPlayingState = false
        wasMusicPlayingBeforePause = false
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
            sceneScope.launch {
                if (!isActive) return@launch
                showContinueButtonElements = false
                currentStage = BootStage.COMPLETED
                Log.d(BOOT_SCENE_TAG, "Stage changed to: $currentStage. Boot sequence complete.")
                onComplete(true)
            }
        } else {
            Log.w(BOOT_SCENE_TAG, "Continue input received but not in AWAITING_CONTINUE_INPUT stage. Current stage: $currentStage")
        }
    }

    private fun handleError(errorMessage: String) {
        if (!sceneScope.isActive) {
            Log.w(BOOT_SCENE_TAG, "handleError called, but sceneScope is not active. Error: $errorMessage")
            return
        }

        Log.e(BOOT_SCENE_TAG, "BootScene Error: $errorMessage")
        currentErrorMessage = errorMessage
        currentStage = BootStage.ERROR

        sceneJob.cancelChildren()
        stopAndReleaseMusicPlayer()

        onComplete(false)
    }

    fun cleanup() {
        Log.d(BOOT_SCENE_TAG, "BootScene cleanup() called. Current stage: $currentStage")
        sceneJob.cancelChildren()
        sceneJob.cancel()

        if (mediaPlayer != null) {
            Log.d(BOOT_SCENE_TAG, "Cleaning up BootScene's MediaPlayer as it was not relinquished or reacquired.")
            stopAndReleaseMusicPlayer()
        } else {
            Log.d(BOOT_SCENE_TAG, "MediaPlayer was null in cleanup (likely relinquished or never initialized).")
        }
        currentStage = BootStage.IDLE
        Log.d(BOOT_SCENE_TAG, "BootScene cleanup finished. sceneScope.isActive: ${sceneScope.isActive}")
    }
}