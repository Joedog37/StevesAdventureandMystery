package com.example.stevesadventureandmystery

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.stevesadventureandmystery.game.core.BootStage
import com.example.stevesadventureandmystery.game.core.SaveLoadManager // Added for SaveLoadManager
import com.example.stevesadventureandmystery.game.data.SaveSlotInfo // Added for SaveSlotInfo
import com.example.stevesadventureandmystery.game.scenes.BootScene
import com.example.stevesadventureandmystery.game.scenes.CreditsScreen
import com.example.stevesadventureandmystery.game.scenes.GameSlotsScreen // Added for GameSlotsScreen Composable
import com.example.stevesadventureandmystery.ui.theme.StevesAdventureAndMysteryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch // Added for launching coroutines
import java.io.IOException

const val TAG = "MainActivity"
val PREVIEW_PLACEHOLDER_BACKGROUND = R.drawable.pexels_krisof_1252890

enum class Screen {
    BOOT,
    MAIN_MENU,
    GAME_SLOTS,
    CREDITS,
}

open class MainActivity : ComponentActivity() {
    lateinit var bootScene: BootScene
    var currentScreen by mutableStateOf(Screen.BOOT)

    private var mainMenuMediaPlayer: MediaPlayer? = null
    private var isMusicHandedOverToMainMenu by mutableStateOf(false)
    private var mainMenuMusicShouldBePlaying by mutableStateOf(false)

    private var creditsMediaPlayer: MediaPlayer? = null
    private var creditsMusicShouldBePlaying by mutableStateOf(false)
    private val creditsMusicAssetPath = "audio/music/journeys_end.mp3" // <<< UPDATE THIS PATH IF NEEDED

    // ++ New for Save/Load ++
    private lateinit var saveLoadManager: SaveLoadManager
    var saveSlotsState by mutableStateOf<List<SaveSlotInfo>>(emptyList())
    // ++ End New for Save/Load ++

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called. isRecreating: ${savedInstanceState != null}, initialScreen: $currentScreen")
        hideSystemUI()

        // ++ Initialize SaveLoadManager ++
        saveLoadManager = SaveLoadManager(applicationContext)
        loadSaveSlots() // Load initial save slots
        // ++ End Initialize SaveLoadManager ++

        bootScene = BootScene(
            context = applicationContext,
            onComplete = { wantsToTransitionToMainMenu ->
                runOnUiThread {
                    Log.d(TAG, "BootScene onComplete. Transition: $wantsToTransitionToMainMenu, Stage: ${bootScene.currentStage}")
                    if (wantsToTransitionToMainMenu && bootScene.currentStage == BootStage.COMPLETED) {
                        currentScreen = Screen.MAIN_MENU
                        mainMenuMusicShouldBePlaying = true
                        val handedOverPlayer = bootScene.getMediaPlayerInstance()
                        if (handedOverPlayer != null) {
                            bootScene.relinquishMediaPlayerControl()
                            mainMenuMediaPlayer = handedOverPlayer
                            isMusicHandedOverToMainMenu = true
                            if (!handedOverPlayer.isPlaying) {
                                try {
                                    mainMenuMediaPlayer?.start()
                                } catch (e: IllegalStateException) {
                                    Log.e(TAG, "Error starting handed over main menu music: ${e.message}", e)
                                    releaseMainMenuMusic()
                                    if (bootScene.backgroundMusicAssetPath != null) {
                                        initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                                    }
                                }
                            }
                        } else {
                            isMusicHandedOverToMainMenu = false
                            if (bootScene.backgroundMusicAssetPath != null) {
                                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                            }
                        }
                    } else if (bootScene.currentStage == BootStage.ERROR) {
                        Log.e(TAG, "BootScene finished with an error: ${bootScene.currentErrorMessage}")
                        currentScreen = Screen.BOOT
                        releaseMainMenuMusic()
                        isMusicHandedOverToMainMenu = false
                        mainMenuMusicShouldBePlaying = false
                    } else {
                        Log.w(TAG, "BootScene onComplete with unexpected state: ${bootScene.currentStage}. Not transitioning.")
                        releaseMainMenuMusic()
                        isMusicHandedOverToMainMenu = false
                        mainMenuMusicShouldBePlaying = false
                    }
                }
            },
            scope = lifecycleScope
        )

        setContent {
            StevesAdventureAndMysteryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(bootScene = bootScene, mainActivity = this)
                }
            }
        }
        handleActivityLifecycle(savedInstanceState)
        Log.d(TAG, "onCreate finished. currentScreen: $currentScreen, BootScene Stage: ${bootScene.currentStage}, MainMenuPlayer: $mainMenuMediaPlayer, MusicShouldPlay: $mainMenuMusicShouldBePlaying, CreditsPlayer: $creditsMediaPlayer, CreditsMusicShouldPlay: $creditsMusicShouldBePlaying")
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    open fun navigateToCredits() {
        Log.d(TAG, "navigateToCredits called. Current screen: $currentScreen")
        if (currentScreen == Screen.MAIN_MENU) {
            if (mainMenuMusicShouldBePlaying && mainMenuMediaPlayer?.isPlaying == true) {
                mainMenuMediaPlayer?.pause()
                Log.d(TAG, "Main menu music PAUSED for credits transition.")
            }
            currentScreen = Screen.CREDITS
            creditsMusicShouldBePlaying = true
            initializeAndPlayCreditsMusic()
        }
    }

    open fun navigateToMainMenu() {
        Log.d(TAG, "navigateToMainMenu called. Current screen: $currentScreen")
        if (currentScreen == Screen.CREDITS || currentScreen == Screen.GAME_SLOTS) { // Modified condition
            if (currentScreen == Screen.CREDITS) {
                creditsMusicShouldBePlaying = false
                releaseCreditsMusic()
                Log.d(TAG, "Credits music released for main menu transition.")
            }
            // If coming from GameSlots, main menu music might have been paused.

            currentScreen = Screen.MAIN_MENU
            mainMenuMusicShouldBePlaying = true // Set this before attempting to play

            // Existing logic for main menu music...
            if (mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                Log.d(TAG, "navigateToMainMenu: Main menu player null, re-initializing.")
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (mainMenuMediaPlayer != null && !mainMenuMediaPlayer!!.isPlaying) {
                Log.d(TAG, "navigateToMainMenu: Main menu player not playing, attempting to start.")
                try {
                    mainMenuMediaPlayer?.start()
                    Log.d(TAG, "Restarted MainMenu music on returning to Main Menu.")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error restarting main menu music on navigateToMainMenu: ${e.message}", e)
                    releaseMainMenuMusic()
                    if (bootScene.backgroundMusicAssetPath != null) {
                        initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                    }
                }
            } else if (mainMenuMediaPlayer?.isPlaying == true) {
                Log.d(TAG, "navigateToMainMenu: Main menu music already playing.")
            }
        }
    }

    // ++ New Navigation Function for Game Slots ++
    open fun navigateToGameSlots() {
        Log.d(TAG, "navigateToGameSlots called. Current screen: $currentScreen")
        if (currentScreen == Screen.MAIN_MENU) {
            // Optionally pause main menu music if it's playing
            if (mainMenuMusicShouldBePlaying && mainMenuMediaPlayer?.isPlaying == true) {
                mainMenuMediaPlayer?.pause()
                Log.d(TAG, "Main menu music PAUSED for game slots transition.")
            }
            currentScreen = Screen.GAME_SLOTS
            loadSaveSlots() // Ensure slots are fresh when navigating to the screen
        }
    }
    // ++ End New Navigation Function for Game Slots ++

    // ++ New function to load/refresh save slots ++
    private fun loadSaveSlots() {
        // Since getAllSaveSlotInfo is not suspend, we can call it directly.
        // If it were suspend, you'd launch a coroutine.
        saveSlotsState = saveLoadManager.getAllSaveSlotInfo()
        Log.d(TAG, "Save slots loaded/refreshed: ${saveSlotsState.size} slots.")
    }
    // ++ End new function ++

    // ++ New Functions for Game Slot Actions ++
    open fun onGameSlotSelected(slotInfo: SaveSlotInfo, isNewGame: Boolean) {
        lifecycleScope.launch { // Use lifecycleScope for coroutines
            if (isNewGame) {
                Log.d(TAG, "Starting new game in slot: ${slotInfo.slotId}")
                val success = saveLoadManager.startNewGameInSlot(slotInfo.slotId)
                if (success) {
                    loadSaveSlots() // Refresh the slots view
                    Log.i(TAG, "New game started. Slot ${slotInfo.slotId} is now active. Transition to game...")
                    // For now, let's just go back to Main Menu as a placeholder
                    navigateToMainMenu() // Or directly set currentScreen = Screen.MAIN_MENU and handle music
                } else {
                    Log.e(TAG, "Failed to start new game in slot ${slotInfo.slotId}")
                }
            } else {
                Log.d(TAG, "Loading game from slot: ${slotInfo.slotId}")
                val loadedData = saveLoadManager.loadGameFromSlot(slotInfo.slotId)
                if (loadedData != null) {
                    Log.i(TAG, "Game loaded from slot ${slotInfo.slotId}: ${loadedData.chapterName}. Transition to game...")
                    // For now, let's just go back to Main Menu as a placeholder
                    navigateToMainMenu() // Or directly set currentScreen = Screen.MAIN_MENU and handle music
                } else {
                    Log.e(TAG, "Failed to load game from slot ${slotInfo.slotId}")
                }
            }
        }
    }

    open fun onGameSlotDelete(slotId: Int) {
        lifecycleScope.launch {
            Log.d(TAG, "Deleting game slot: $slotId")
            val success = saveLoadManager.deleteSaveSlot(slotId)
            if (success) {
                loadSaveSlots() // Refresh the UI
                Log.i(TAG, "Slot $slotId deleted.")
            } else {
                Log.e(TAG, "Failed to delete slot $slotId")
            }
        }
    }
    // ++ End New Functions for Game Slot Actions ++

    private fun handleActivityLifecycle(savedInstanceState: Bundle?) {
        val isRecreating = savedInstanceState != null
        Log.d(TAG, "handleActivityLifecycle - isRecreating: $isRecreating, initial currentScreen: $currentScreen, initial BootStage: ${bootScene.currentStage}")
        if (isRecreating) {
            if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying && mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage == BootStage.COMPLETED) {
                if (::bootScene.isInitialized) bootScene.handleContinueInput() else Log.e(TAG, "BootScene not init in handleActivityLifecycle for COMPLETED state")
            } else if (currentScreen == Screen.BOOT && bootScene.isBootSequenceActive()) {
                // BootScene manages its own resumption if active
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage != BootStage.ERROR && bootScene.currentStage != BootStage.COMPLETED) {
                if (::bootScene.isInitialized) bootScene.start() else Log.e(TAG, "BootScene not init in handleActivityLifecycle for start state")
            } else if (currentScreen == Screen.CREDITS && creditsMusicShouldBePlaying) {
                if (creditsMediaPlayer == null) {
                    initializeAndPlayCreditsMusic()
                } else if (!creditsMediaPlayer!!.isPlaying) {
                    try {
                        creditsMediaPlayer?.start()
                        Log.d(TAG, "Credits music started on recreate.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting music for credits on recreate: ${e.message}", e)
                        releaseCreditsMusic()
                        initializeAndPlayCreditsMusic() // attempt reinit
                    }
                }
            }
            // Add handling for GameSlotsScreen if needed, e.g., refreshing slot data
            if (currentScreen == Screen.GAME_SLOTS) {
                loadSaveSlots()
            }
        } else { // Fresh start
            if (currentScreen == Screen.BOOT && bootScene.currentStage != BootStage.COMPLETED && bootScene.currentStage != BootStage.ERROR) {
                if (::bootScene.isInitialized) bootScene.start() else Log.e(TAG, "BootScene not init in handleActivityLifecycle for fresh start")
            } else if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying && mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage == BootStage.COMPLETED) {
                if (::bootScene.isInitialized) bootScene.handleContinueInput() else Log.e(TAG, "BootScene not init in handleActivityLifecycle for fresh COMPLETED state")
            }
        }
    }

    private fun initializeAndPlayMainMenuMusic(assetPath: String) {
        if (!mainMenuMusicShouldBePlaying && currentScreen == Screen.MAIN_MENU) {
            Log.d(TAG, "initializeAndPlayMainMenuMusic called, but mainMenuMusicShouldBePlaying is false for current screen or not on Main Menu. Not initializing.")
            return
        }
        if (mainMenuMediaPlayer != null) {
            releaseMainMenuMusic()
        }
        Log.d(TAG, "Initializing MainMenu music for MainActivity with asset: $assetPath")
        var afd: AssetFileDescriptor? = null
        try {
            mainMenuMediaPlayer = MediaPlayer()
            afd = applicationContext.assets.openFd(assetPath)
            mainMenuMediaPlayer?.apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MainMenu music (MainActivity) prepared.")
                    if (mainMenuMusicShouldBePlaying && currentScreen == Screen.MAIN_MENU) {
                        try {
                            mp.start()
                            Log.d(TAG, "MainMenu music started.")
                        } catch (ise: IllegalStateException) {
                            Log.e(TAG, "IllegalStateException on MainMenu (MainActivity) mp.start(): ${ise.message}", ise)
                            releaseMainMenuMusic()
                        }
                    } else {
                        Log.d(TAG, "MainMenu music prepared, but conditions not met to start. currentScreen: $currentScreen, mainMenuMusicShouldBePlaying: $mainMenuMusicShouldBePlaying. Releasing player.")
                        mp.release()
                        if (mainMenuMediaPlayer == mp) mainMenuMediaPlayer = null
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MainMenu MediaPlayer (MainActivity) Error: what: $what, extra: $extra")
                    releaseMainMenuMusic()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MainMenu music (MainActivity): ${e.message}", e)
            releaseMainMenuMusic()
        } finally {
            try { afd?.close() } catch (ioe: IOException) { Log.e(TAG, "Error closing AFD for main menu: ${ioe.message}")}
        }
    }

    private fun releaseMainMenuMusic() {
        mainMenuMediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
                Log.d(TAG, "MainMenu MediaPlayer released by MainActivity.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during releaseMainMenuMusic: ${e.message}")
                try { if (!it.isPlaying) it.release() } catch (ex: Exception) { Log.e(TAG, "Nested release attempt failed: ${ex.message}") }
            }
        }
        mainMenuMediaPlayer = null
        isMusicHandedOverToMainMenu = false
    }

    private fun initializeAndPlayCreditsMusic() {
        if (!creditsMusicShouldBePlaying) {
            Log.d(TAG, "initializeAndPlayCreditsMusic: creditsMusicShouldBePlaying is false, not starting.")
            return
        }
        if (creditsMediaPlayer != null) {
            Log.d(TAG, "initializeAndPlayCreditsMusic: Player already exists, releasing first.")
            releaseCreditsMusic()
        }

        Log.d(TAG, "Initializing Credits music with asset: $creditsMusicAssetPath")
        var afd: AssetFileDescriptor? = null
        try {
            creditsMediaPlayer = MediaPlayer()
            afd = applicationContext.assets.openFd(creditsMusicAssetPath)
            creditsMediaPlayer?.apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setOnPreparedListener { mp ->
                    Log.d(TAG, "Credits MediaPlayer prepared.")
                    if (creditsMusicShouldBePlaying && currentScreen == Screen.CREDITS) {
                        try {
                            mp.start()
                            Log.d(TAG, "Credits music started successfully.")
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Error starting credits music post-prepare: ${e.message}", e)
                            releaseCreditsMusic()
                        }
                    } else {
                        Log.d(TAG, "Credits MediaPlayer prepared, but conditions no longer met. Releasing player instance.")
                        mp.release()
                        if (creditsMediaPlayer == mp) creditsMediaPlayer = null
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Credits MediaPlayer Error: what $what, extra $extra")
                    releaseCreditsMusic()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize/setDS for credits music: ${e.message}", e)
            releaseCreditsMusic()
        } finally {
            try { afd?.close() } catch (ioe: IOException) { Log.e(TAG, "Error closing AFD for credits music: ${ioe.message}")}
        }
    }

    private fun releaseCreditsMusic() {
        creditsMediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
                Log.d(TAG, "Credits MediaPlayer released.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception releasing credits MediaPlayer: ${e.message}")
                try { if (!it.isPlaying) it.release() } catch (ex: Exception) { Log.e(TAG, "Nested credits release attempt failed: ${ex.message}") }
            }
        }
        creditsMediaPlayer = null
    }

    open fun exitGameCleanup() {
        Log.d(TAG, "exitGameCleanup called. Releasing music and finishing activity.")
        mainMenuMusicShouldBePlaying = false
        creditsMusicShouldBePlaying = false
        releaseMainMenuMusic()
        releaseCreditsMusic()
        finish()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause. currentScreen: $currentScreen, isMusicHandedOver: $isMusicHandedOverToMainMenu, musicShouldPlay: $mainMenuMusicShouldBePlaying, player: $mainMenuMediaPlayer, creditsPlayer: $creditsMediaPlayer, creditsMusicShouldPlay: $creditsMusicShouldBePlaying")

        if (::bootScene.isInitialized && bootScene.isBootSequenceActive() && !isMusicHandedOverToMainMenu && currentScreen == Screen.BOOT) {
            bootScene.pauseMusic()
        }
        mainMenuMediaPlayer?.let {
            if (it.isPlaying) try {
                it.pause()
                Log.d(TAG, "MainMenu music paused by MainActivity.")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error pausing MainMenu music: ${e.message}", e)
                releaseMainMenuMusic()
            }
        }
        creditsMediaPlayer?.let {
            if (it.isPlaying) try {
                it.pause()
                Log.d(TAG, "Credits music paused.")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error pausing Credits music: ${e.message}", e)
                releaseCreditsMusic()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        Log.d(TAG, "onResume. currentScreen: $currentScreen, isMusicHandedOver: $isMusicHandedOverToMainMenu, musicShouldPlay: $mainMenuMusicShouldBePlaying, player: $mainMenuMediaPlayer, creditsPlayer: $creditsMediaPlayer, creditsMusicShouldPlay: $creditsMusicShouldBePlaying")

        if (::bootScene.isInitialized && bootScene.isBootSequenceActive() && !isMusicHandedOverToMainMenu && currentScreen == Screen.BOOT) {
            bootScene.resumeMusic()
        }

        if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying) {
            if (mainMenuMediaPlayer != null) {
                if (!mainMenuMediaPlayer!!.isPlaying) {
                    try {
                        mainMenuMediaPlayer?.start()
                        Log.d(TAG, "MainMenu music resumed by MainActivity.")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error resuming MainMenu music: ${e.message}", e)
                        releaseMainMenuMusic()
                        if (bootScene.backgroundMusicAssetPath != null) {
                            initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                        }
                    }
                }
            } else {
                if (bootScene.backgroundMusicAssetPath != null) {
                    Log.d(TAG, "onResume: MainMenu music should be playing but player is null. Initializing.")
                    initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                }
            }
        } else if (currentScreen == Screen.CREDITS && creditsMusicShouldBePlaying) {
            if (creditsMediaPlayer != null) {
                if (!creditsMediaPlayer!!.isPlaying) {
                    try {
                        creditsMediaPlayer?.start()
                        Log.d(TAG, "Credits music resumed.")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error resuming Credits music: ${e.message}", e)
                        releaseCreditsMusic()
                        initializeAndPlayCreditsMusic()
                    }
                }
            } else {
                Log.d(TAG, "onResume: Credits music should be playing but player is null. Initializing.")
                initializeAndPlayCreditsMusic()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop. isFinishing: $isFinishing")
        if (isFinishing) {
            performCleanup()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called.")
        performCleanup()
        super.onDestroy()
    }

    private fun performCleanup() {
        Log.d(TAG, "Performing cleanup.")
        mainMenuMusicShouldBePlaying = false
        creditsMusicShouldBePlaying = false
        releaseMainMenuMusic()
        releaseCreditsMusic()
        if (::bootScene.isInitialized) {
            bootScene.cleanup()
        }
    }
} // End of MainActivity Class

@Composable
fun GameScreen(bootScene: BootScene, mainActivity: MainActivity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val currentEffectiveStage = bootScene.currentStage
        val currentScreen = mainActivity.currentScreen // Make sure to use the activity's currentScreen

        val isBooting = currentScreen == Screen.BOOT &&
                currentEffectiveStage != BootStage.ERROR &&
                currentEffectiveStage != BootStage.COMPLETED &&
                bootScene.isBootSequenceActive()
        val displayError = currentScreen == Screen.BOOT && currentEffectiveStage == BootStage.ERROR

        val showBootSceneBackground = bootScene.backgroundDrawableRes != 0 &&
                ( (isBooting && currentEffectiveStage >= BootStage.FADING_IN_BACKGROUND) ||
                        ( (currentScreen == Screen.MAIN_MENU || currentScreen == Screen.GAME_SLOTS) && // Show background for GameSlots too
                                bootScene.isBootSequenceCompletedAndBackgroundAvailable() )
                        )


        if (showBootSceneBackground) {
            val imageAlpha by animateFloatAsState(
                targetValue = if (currentScreen == Screen.MAIN_MENU || currentScreen == Screen.GAME_SLOTS || (isBooting && currentEffectiveStage >= BootStage.SHOWING_TITLE)) bootScene.backgroundAlpha else 0f,
                animationSpec = tween(durationMillis = bootScene.defaultFadeDurationMillis.toInt()),
                label = "BackgroundImageAlpha"
            )
            Image(
                painter = painterResource(id = bootScene.backgroundDrawableRes),
                contentDescription = "Main Background",
                modifier = Modifier.fillMaxSize().alpha(imageAlpha),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = when {
                        displayError -> Color.Black
                        currentScreen == Screen.CREDITS -> Color.Transparent // Or a specific credits background
                        currentScreen == Screen.GAME_SLOTS -> Color.Black.copy(alpha = 0.3f) // Dimmed background for slots screen
                        !showBootSceneBackground && currentScreen == Screen.MAIN_MENU -> Color.DarkGray
                        isBooting && (currentEffectiveStage < BootStage.FADING_IN_BACKGROUND ||
                                (bootScene.backgroundAlpha < 0.01f && currentEffectiveStage == BootStage.FADING_IN_BACKGROUND)) -> Color.Black
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (currentScreen) {
                Screen.BOOT -> {
                    // ... (Your existing Boot screen UI logic - kept as is)
                    if (displayError) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "An Error Occurred:\n${bootScene.currentErrorMessage ?: "Unknown error during boot."}",
                                fontSize = 18.sp, color = Color.Red, textAlign = TextAlign.Center
                            )
                        }
                    } else if (isBooting) {
                        val fadeMillis = bootScene.defaultFadeDurationMillis.toInt()
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedVisibility(
                                visible = bootScene.showWarningText && currentEffectiveStage == BootStage.SHOWING_WARNING,
                                enter = fadeIn(animationSpec = tween(fadeMillis)),
                                exit = fadeOut(animationSpec = tween(fadeMillis / 2))
                            ) {
                                Text(
                                    bootScene.warningTextContent, fontSize = 16.sp, textAlign = TextAlign.Center,
                                    color = Color.LightGray, modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp).fillMaxWidth()
                                )
                            }
                            AnimatedVisibility(
                                visible = bootScene.showLogoImage && bootScene.logoDrawableRes != 0 && currentEffectiveStage == BootStage.SHOWING_LOGO,
                                enter = fadeIn(animationSpec = tween(fadeMillis)),
                                exit = fadeOut(animationSpec = tween(fadeMillis / 2))
                            ) {
                                if (bootScene.logoDrawableRes != 0) {
                                    Image(
                                        painter = painterResource(id = bootScene.logoDrawableRes), contentDescription = "Game Logo",
                                        modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 20.dp), contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = bootScene.showTitleText && currentEffectiveStage >= BootStage.SHOWING_TITLE && currentEffectiveStage < BootStage.COMPLETED,
                                enter = fadeIn(animationSpec = tween(fadeMillis)),
                                exit = fadeOut(animationSpec = tween(fadeMillis / 2))
                            ) {
                                Text(
                                    bootScene.titleTextContent, fontSize = 32.sp, color = Color.White,
                                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 20.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = bootScene.showContinueButtonElements && currentEffectiveStage == BootStage.AWAITING_CONTINUE_INPUT,
                                enter = fadeIn(animationSpec = tween(fadeMillis / 2)),
                                exit = fadeOut(animationSpec = tween(fadeMillis / 2))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 30.dp)) {
                                    Button(onClick = { bootScene.handleContinueInput() }) {
                                        Text(bootScene.continueButtonTextContent, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center) { /* Potentially loading or idle before transition */ }
                    }
                }
                Screen.MAIN_MENU -> {
                    MainMenuScreen(
                        onExitGame = { mainActivity.exitGameCleanup() },
                        onShowCredits = { mainActivity.navigateToCredits() },
                        onShowGameSlots = { mainActivity.navigateToGameSlots() } // ++ Added this callback
                    )
                }
                Screen.CREDITS -> {
                    CreditsScreen(
                        onBack = { mainActivity.navigateToMainMenu() }
                    )
                }
                // ++ New Case for Game Slots Screen ++
                Screen.GAME_SLOTS -> {
                    // Assuming GameSlotsScreen is defined elsewhere (e.g., game.scenes.GameSlotsScreen.kt)
                    // If not, you'd define its Composable function here or import it.
                    // For this to compile, you need GameSlotsScreen composable and SaveSlotInfo data class.
                    // Make sure the necessary imports are at the top of MainActivity.kt
                    // import com.example.stevesadventureandmystery.game.scenes.GameSlotsScreen
                    // import com.example.stevesadventureandmystery.game.data.SaveSlotInfo

                    GameSlotsScreen( // This assumes GameSlotsScreen is ready to be used
                        saveSlots = mainActivity.saveSlotsState,
                        onSlotSelected = { slotInfo, isNewGame ->
                            mainActivity.onGameSlotSelected(slotInfo, isNewGame)
                        },
                        onBackPressed = { mainActivity.navigateToMainMenu() },
                        )
                }
                // ++ End New Case ++
            }
        }
    }
}

@Composable
fun ConfirmExitDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Confirm Exit") },
        text = { Text("Are you sure you want to exit the game?") },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismissRequest() // Dismiss after action
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = {
                onCancel()
                onDismissRequest() // Dismiss after action
            }) {
                Text("No")
            }
        }
    )
}

@Composable
fun MainMenuScreen(
    onExitGame: () -> Unit,
    onShowCredits: () -> Unit,
    onShowGameSlots: () -> Unit // ++ New callback for navigating to game slots
) {
    var showConfirmExitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Steve's Space Adventure Mystery", // Your game title
            fontSize = 30.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // ++ Button to navigate to Game Slots Screen ++
        Button(onClick = {
            Log.d("MainMenuScreen", "Continue/Load Game Clicked - Navigating to Game Slots")
            onShowGameSlots()
        }) {
            Text("Continue / Load Game") // Or "Play Game", "Load/Save"
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Your existing "Start Game" button might be replaced or repurposed by the above.
        // If "Start Game" means "Start a New Game directly", you'd need different logic.
        // For now, I'm assuming "Continue / Load Game" is the primary way to proceed.

        Button(onClick = { Log.d("MainMenuScreen", "Settings Clicked (Action TBD)") }) {
            Text("Settings")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Log.d("MainMenuScreen", "Credits Clicked - Navigating")
            onShowCredits()
        }) {
            Text("Credits")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Log.d("MainMenuScreen", "Exit Game Clicked - Showing confirmation")
            showConfirmExitDialog = true
        }) {
            Text("Exit Game")
        }
    }

    if (showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismissRequest = {
                Log.d("MainMenuScreen", "Exit confirmation dialog dismissed")
                showConfirmExitDialog = false
            },
            onConfirm = {
                Log.d("MainMenuScreen", "Exit Confirmed")
                onExitGame()
                // showConfirmExitDialog = false // Not strictly needed as activity may finish
            },
            onCancel = {
                Log.d("MainMenuScreen", "Exit Cancelled")
                showConfirmExitDialog = false
            }
        )
    }
}

@Composable
private fun rememberMockBootScene(
    context: android.content.Context = LocalContext.current,
    initialStage: BootStage = BootStage.IDLE,
    config: BootScene.() -> Unit = {}
): BootScene {
    val previewScope = remember { CoroutineScope(Dispatchers.Main) }
    return remember {
        BootScene(
            context = context,
            onComplete = { _ -> Log.d("PreviewBootScene", "onComplete called") },
            scope = previewScope
        ).apply {
            this.currentStage = initialStage
            config() // Apply custom configurations

            // Default sensible values for preview if not overridden
            if (this.backgroundDrawableRes == 0 && initialStage >= BootStage.FADING_IN_BACKGROUND) {
                this.backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND // Ensure placeholder if needed
            }
            if (initialStage >= BootStage.FADING_IN_BACKGROUND) {
                this.backgroundAlpha = 1f // Visible background
            }
            if (initialStage >= BootStage.SHOWING_TITLE && initialStage < BootStage.COMPLETED) this.showTitleText = true
            if (initialStage == BootStage.SHOWING_WARNING) this.showWarningText = true
            if (initialStage == BootStage.SHOWING_LOGO) this.showLogoImage = true
            if (initialStage == BootStage.AWAITING_CONTINUE_INPUT) {
                this.showTitleText = true // Title usually visible here
                this.showContinueButtonElements = true
            }
            if (initialStage == BootStage.COMPLETED) {
                this.backgroundAlpha = 1f
                if (this.backgroundDrawableRes == 0) this.backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND
            }
        }
    }
}

@Composable
private fun rememberMockMainActivity(initialScreen: Screen = Screen.MAIN_MENU): MainActivity {
    val context = LocalContext.current
    return remember {
        object : MainActivity() {
            // ++ Mock SaveLoadManager and slots for preview ++
            private val mockSaveLoadManager = SaveLoadManager(context)
            init {
                super.bootScene = BootScene(context, {}, CoroutineScope(Dispatchers.Main)).apply{
                    backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND
                    backgroundAlpha = 1f
                    currentStage = if (initialScreen == Screen.MAIN_MENU || initialScreen == Screen.CREDITS || initialScreen == Screen.GAME_SLOTS) {
                        BootStage.COMPLETED
                    } else {
                        BootStage.IDLE
                    }
                }
                super.currentScreen = initialScreen
                // ++ Initialize saveSlotsState for preview ++
                super.saveSlotsState = mockSaveLoadManager.getAllSaveSlotInfo().ifEmpty {
                    // Provide some sample slots if preferences are empty for the preview
                    listOf(
                        SaveSlotInfo(1, true, null, null), // Slot 1: Empty
                        SaveSlotInfo(2, false, "Chapter 1: The Beginning", "2023-10-26 10:00 AM"), // Slot 2: Has data
                        SaveSlotInfo(3, true, null, null)  // Slot 3: Empty
                    )
                }
            }
            override fun navigateToCredits() { currentScreen = Screen.CREDITS }
            override fun navigateToMainMenu() { currentScreen = Screen.MAIN_MENU }
            override fun navigateToGameSlots() { currentScreen = Screen.GAME_SLOTS } // ++ Mock nav ++
            override fun exitGameCleanup() { Log.d("Preview", "Exit Game Action Triggered") }

            // ++ Mock slot actions for preview if needed ++
            override fun onGameSlotSelected(slotInfo: SaveSlotInfo, isNewGame: Boolean) {
                Log.d("Preview", "Slot selected: ${slotInfo.slotId}, isNew: $isNewGame")
                // Simulate saving/loading for preview by updating the slot or navigating
                if (isNewGame && slotInfo.isEmpty) {
                    val updatedSlots = saveSlotsState.map {
                        if (it.slotId == slotInfo.slotId) {
                            it.copy(isEmpty = false, chapterName = "New Game Started (Preview)", lastSavedTimestamp = "Just Now")
                        } else it
                    }
                    saveSlotsState = updatedSlots
                }
                currentScreen = Screen.MAIN_MENU // Simulate going back or to game
            }
            override fun onGameSlotDelete(slotId: Int) {
                Log.d("Preview", "Slot delete: $slotId")
                // Simulate deletion for preview
                val updatedSlots = saveSlotsState.map {
                    if (it.slotId == slotId) it.copy(isEmpty = true, chapterName = null, lastSavedTimestamp = null)
                    else it
                }
                saveSlotsState = updatedSlots
            }
        }
    }
}


// -------- Previews --------

@Preview(name = "Boot - Warning", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneWarning() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.SHOWING_WARNING),
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}

@Preview(name = "Boot - Logo", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneLogo() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.SHOWING_LOGO) {
                // Assuming you have a placeholder or actual logo in drawables
                // If not, this preview might not show a logo, or you can use a generic icon
                // For example, using a built-in icon if you don't have a specific one yet:
                // logoDrawableRes = android.R.drawable.sym_def_app_icon
                // Or better, add a placeholder to your project:
                logoDrawableRes = R.drawable.ic_launcher_foreground // Replace if you have a specific game logo
            },
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}

@Preview(name = "Boot - Title", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneTitle() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.SHOWING_TITLE),
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}

@Preview(name = "Boot - Awaiting Continue", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneAwaitingInput() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.AWAITING_CONTINUE_INPUT),
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}

@Preview(name = "Boot - Error", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneError() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.ERROR) {
                currentErrorMessage = "Preview: A critical error occurred during boot."
            },
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}


@Preview(name = "Main Menu Screen", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewMainMenuScreen() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED),
            mainActivity = rememberMockMainActivity(initialScreen = Screen.MAIN_MENU)
        )
    }
}

@Preview(name = "Credits Screen", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewCreditsScreen() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED),
            mainActivity = rememberMockMainActivity(initialScreen = Screen.CREDITS)
        )
    }
}

@Preview(name = "Game Slots Screen - Default Mock Data", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewGameSlotsScreenWithData() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED),
            // rememberMockMainActivity already provides sample data for GAME_SLOTS
            mainActivity = rememberMockMainActivity(initialScreen = Screen.GAME_SLOTS)
        )
    }
}

@Preview(name = "Game Slots Screen - All Empty", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewGameSlotsScreenAllEmpty() {
    StevesAdventureAndMysteryTheme {
        val mockMainActivity = rememberMockMainActivity(initialScreen = Screen.GAME_SLOTS)
        // Override the saveSlotsState for this specific preview case
        mockMainActivity.saveSlotsState = listOf(
            SaveSlotInfo(1, true, null, null),
            SaveSlotInfo(2, true, null, null),
            SaveSlotInfo(3, true, null, null)
        )
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED),
            mainActivity = mockMainActivity
        )
    }
}

// You might also want a preview for the ConfirmExitDialog if it's not easily triggered
// via the MainMenuScreen preview.
@Preview(name = "Confirm Exit Dialog", showBackground = true)
@Composable
fun PreviewConfirmExitDialog() {
    StevesAdventureAndMysteryTheme {
        // Provide a simple background for the dialog to be visible
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            ConfirmExitDialog(
                onDismissRequest = { Log.d("Preview", "Dismiss Dialog") },
                onConfirm = { Log.d("Preview", "Confirm Exit") },
                onCancel = { Log.d("Preview", "Cancel Exit") }
            )
        }
    }
}