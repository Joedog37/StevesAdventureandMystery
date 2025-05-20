package com.example.stevesadventureandmystery

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
import com.example.stevesadventureandmystery.game.scenes.BootScene
import com.example.stevesadventureandmystery.ui.theme.StevesAdventureAndMysteryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

const val TAG = "MainActivity"
val PREVIEW_PLACEHOLDER_BACKGROUND = R.drawable.pexels_krisof_1252890

// Define Screens
enum class Screen {
    BOOT,
    MAIN_MENU,
    CREDITS,
    // GAMEPLAY // Future screen
}

open class MainActivity : ComponentActivity() {
    lateinit var bootScene: BootScene
    var currentScreen by mutableStateOf(Screen.BOOT)
        set // External read-only, internal mutable

    private var mainMenuMediaPlayer: MediaPlayer? = null
    private var isMusicHandedOverToMainMenu by mutableStateOf(false)
    private var mainMenuMusicShouldBePlaying by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called. isRecreating: ${savedInstanceState != null}, initialScreen: $currentScreen")
        hideSystemUI()

        bootScene = BootScene(
            context = applicationContext,
            onComplete = { wantsToTransitionToMainMenu ->
                runOnUiThread {
                    Log.d(TAG, "BootScene onComplete. Transition: $wantsToTransitionToMainMenu, Stage: ${bootScene.currentStage}")
                    if (wantsToTransitionToMainMenu && bootScene.currentStage == BootStage.COMPLETED) {
                        Log.d(TAG, "BootScene completed. Attempting transition to MainMenu screen.")
                        currentScreen = Screen.MAIN_MENU // Set current screen
                        mainMenuMusicShouldBePlaying = true

                        val handedOverPlayer = bootScene.getMediaPlayerInstance()
                        if (handedOverPlayer != null) {
                            bootScene.relinquishMediaPlayerControl()
                            mainMenuMediaPlayer = handedOverPlayer
                            isMusicHandedOverToMainMenu = true
                            Log.d(TAG, "MediaPlayer acquired from BootScene. Was playing: ${handedOverPlayer.isPlaying}")
                            if (!handedOverPlayer.isPlaying) {
                                try {
                                    Log.d(TAG, "Handed over MediaPlayer was not playing. Attempting to start for MainMenu.")
                                    mainMenuMediaPlayer?.start()
                                } catch (e: IllegalStateException) {
                                    Log.e(TAG, "Error starting handed over MediaPlayer for MainMenu: ${e.message}", e)
                                    releaseMainMenuMusic()
                                    if (bootScene.backgroundMusicAssetPath != null) {
                                        initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "BootScene completed but MediaPlayer instance was null.")
                            isMusicHandedOverToMainMenu = false
                            if (bootScene.backgroundMusicAssetPath != null) {
                                Log.d(TAG, "Initializing MainMenu music independently as it wasn't handed over.")
                                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                            }
                        }
                    } else if (bootScene.currentStage == BootStage.ERROR) {
                        Log.e(TAG, "BootScene finished with an error: ${bootScene.currentErrorMessage}")
                        currentScreen = Screen.BOOT // Or a dedicated Error Screen if you make one
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
        Log.d(TAG, "onCreate finished. currentScreen: $currentScreen, BootScene Stage: ${bootScene.currentStage}, MainMenuPlayer: $mainMenuMediaPlayer, MusicShouldPlay: $mainMenuMusicShouldBePlaying")
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Navigation methods
    open fun navigateToCredits() {
        Log.d(TAG, "navigateToCredits called. Current screen: $currentScreen")
        if (currentScreen == Screen.MAIN_MENU) {
            currentScreen = Screen.CREDITS
            // Music handling for credits screen (e.g., continue main menu music or stop/change it)
            // For now, let main menu music continue if playing.
        }
    }

    open fun navigateToMainMenu() {
        Log.d(TAG, "navigateToMainMenu called. Current screen: $currentScreen")
        if (currentScreen == Screen.CREDITS) { // Or from other screens later
            currentScreen = Screen.MAIN_MENU
            // Ensure main menu music logic is reapplied if it was stopped for credits
            if (mainMenuMusicShouldBePlaying && mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (mainMenuMusicShouldBePlaying && mainMenuMediaPlayer != null && !mainMenuMediaPlayer!!.isPlaying) {
                try {
                    mainMenuMediaPlayer?.start()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error restarting music on navigateToMainMenu: ${e.message}")
                }
            }
        }
    }


    private fun handleActivityLifecycle(savedInstanceState: Bundle?) {
        val isRecreating = savedInstanceState != null
        Log.d(TAG, "handleActivityLifecycle - isRecreating: $isRecreating, initial currentScreen: $currentScreen, initial BootStage: ${bootScene.currentStage}")
        if (isRecreating) {
            if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying && mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                Log.d(TAG, "Activity recreated, on MainMenu and music should play. Re-initializing MainMenu music.")
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage == BootStage.COMPLETED) {
                Log.d(TAG, "Activity recreated. BootScene was completed but currentScreen is BOOT. Triggering onComplete for BootScene.")
                bootScene.handleContinueInput() // This should trigger onComplete -> currentScreen = MAIN_MENU
            } else if (currentScreen == Screen.BOOT && bootScene.isBootSequenceActive()) {
                Log.d(TAG, "Activity recreated. BootScene is active. It will resume or restart its sequence.")
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage != BootStage.ERROR && bootScene.currentStage != BootStage.COMPLETED) {
                Log.d(TAG, "Activity recreated. BootScene not active/completed/error. Starting BootScene.")
                bootScene.start()
            }
            // Add similar logic for Screen.CREDITS if music needs to be handled on recreation
        } else { // Fresh start
            if (currentScreen == Screen.BOOT && bootScene.currentStage != BootStage.COMPLETED && bootScene.currentStage != BootStage.ERROR) {
                Log.d(TAG, "Fresh start. Starting BootScene.")
                bootScene.start()
            } else if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying && mainMenuMediaPlayer == null && bootScene.backgroundMusicAssetPath != null) {
                Log.d(TAG, "Fresh start, but currentScreen is MainMenu and music should play. Initializing music.")
                initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
            } else if (currentScreen == Screen.BOOT && bootScene.currentStage == BootStage.COMPLETED) {
                Log.d(TAG, "Fresh start, but BootScene is already COMPLETED. Triggering onComplete.")
                bootScene.handleContinueInput()
            }
        }
    }


    private fun initializeAndPlayMainMenuMusic(assetPath: String) {
        if (!mainMenuMusicShouldBePlaying && currentScreen == Screen.MAIN_MENU) {
            Log.d(TAG, "initializeAndPlayMainMenuMusic called, but mainMenuMusicShouldBePlaying is false. Not initializing.")
            return
        }
        if (mainMenuMediaPlayer != null) {
            Log.w(TAG, "initializeAndPlayMainMenuMusic: mainMenuMediaPlayer not null, releasing first.")
            releaseMainMenuMusic()
        }

        Log.d(TAG, "Initializing MainMenu music for MainActivity with asset: $assetPath")
        try {
            mainMenuMediaPlayer = MediaPlayer().apply {
                val afd = applicationContext.assets.openFd(assetPath)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setOnPreparedListener { mp ->
                    Log.d(TAG, "MainMenu music (MainActivity) prepared.")
                    if (currentScreen == Screen.MAIN_MENU && mainMenuMusicShouldBePlaying) {
                        try {
                            mp.start()
                            Log.d(TAG, "MainMenu music started.")
                        } catch (ise: IllegalStateException) {
                            Log.e(TAG, "IllegalStateException on MainMenu (MainActivity) mp.start(): ${ise.message}", ise)
                            releaseMainMenuMusic()
                        }
                    } else {
                        Log.d(TAG, "MainMenu music prepared, but conditions not met to start. currentScreen: $currentScreen, mainMenuMusicShouldBePlaying: $mainMenuMusicShouldBePlaying")
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MainMenu MediaPlayer (MainActivity) Error: what: $what, extra: $extra")
                    releaseMainMenuMusic()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MainMenu music (MainActivity): ${e.message}", e)
            releaseMainMenuMusic()
        }
    }

    private fun releaseMainMenuMusic() {
        mainMenuMediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
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

    open fun exitGameCleanup() {
        Log.d(TAG, "exitGameCleanup called. Releasing music and finishing activity.")
        mainMenuMusicShouldBePlaying = false
        releaseMainMenuMusic()
        finish()
    }


    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause. currentScreen: $currentScreen, isMusicHandedOver: $isMusicHandedOverToMainMenu, musicShouldPlay: $mainMenuMusicShouldBePlaying, player: $mainMenuMediaPlayer")

        if (::bootScene.isInitialized && bootScene.isBootSequenceActive() && !isMusicHandedOverToMainMenu && currentScreen == Screen.BOOT) {
            Log.d(TAG, "Pausing BootScene music.")
            bootScene.pauseMusic()
        }

        mainMenuMediaPlayer?.let {
            if (it.isPlaying) {
                try {
                    it.pause()
                    Log.d(TAG, "MainMenu music paused by MainActivity.")
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error pausing MainMenu music: ${e.message}", e)
                    releaseMainMenuMusic()
                }
            } else {
                Log.d(TAG, "onPause: MainMenu music player exists but was not playing.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        Log.d(TAG, "onResume. currentScreen: $currentScreen, isMusicHandedOver: $isMusicHandedOverToMainMenu, musicShouldPlay: $mainMenuMusicShouldBePlaying, player: $mainMenuMediaPlayer")

        if (::bootScene.isInitialized && bootScene.isBootSequenceActive() && !isMusicHandedOverToMainMenu && currentScreen == Screen.BOOT) {
            Log.d(TAG, "Resuming BootScene music.")
            bootScene.resumeMusic()
        }

        if ((currentScreen == Screen.MAIN_MENU || currentScreen == Screen.CREDITS) && mainMenuMusicShouldBePlaying) { // Music can play on Main Menu or Credits
            if (mainMenuMediaPlayer != null) {
                if (!mainMenuMediaPlayer!!.isPlaying) {
                    Log.d(TAG, "onResume: Attempting to resume MainMenu/Credits music as it's not playing but should.")
                    try {
                        mainMenuMediaPlayer?.start()
                        Log.d(TAG, "MainMenu/Credits music resumed by MainActivity.")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error resuming MainMenu/Credits music: ${e.message}", e)
                        if (bootScene.backgroundMusicAssetPath != null) {
                            Log.w(TAG, "MainMenu/Credits music resume failed. Re-initializing with path: ${bootScene.backgroundMusicAssetPath!!}")
                            releaseMainMenuMusic()
                            initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                        } else {
                            Log.e(TAG, "Cannot re-initialize MainMenu/Credits music on resume: backgroundMusicAssetPath is null.")
                            releaseMainMenuMusic()
                        }
                    }
                } else {
                    Log.d(TAG, "onResume: MainMenu/Credits music already playing.")
                }
            } else {
                if (bootScene.backgroundMusicAssetPath != null) {
                    Log.d(TAG, "onResume: MainMenu/Credits player is null but should be playing. Initializing music.")
                    initializeAndPlayMainMenuMusic(bootScene.backgroundMusicAssetPath!!)
                } else {
                    Log.w(TAG, "onResume: MainMenu/Credits player is null, should be playing, but no asset path to initialize.")
                }
            }
        } else if (mainMenuMediaPlayer != null && mainMenuMediaPlayer!!.isPlaying &&
            !((currentScreen == Screen.MAIN_MENU || currentScreen == Screen.CREDITS) && mainMenuMusicShouldBePlaying)) {
            Log.w(TAG, "onResume: MainMenu/Credits music playing but conditions not met. Pausing. currentScreen: $currentScreen, mainMenuMusicShouldBePlaying: $mainMenuMusicShouldBePlaying")
            try {
                mainMenuMediaPlayer?.pause()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error pausing MainMenu/Credits music in edge case: ${e.message}", e)
                releaseMainMenuMusic()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop. isFinishing: $isFinishing")
        if (isFinishing) {
            Log.d(TAG, "onStop: Activity is finishing. Ensuring cleanup.")
            performCleanup()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy. Performing final cleanup.")
        performCleanup()
        super.onDestroy()
    }

    private fun performCleanup() {
        Log.d(TAG, "Performing cleanup.")
        mainMenuMusicShouldBePlaying = false
        releaseMainMenuMusic()
        if (::bootScene.isInitialized) {
            bootScene.cleanup()
        }
    }
}


@Composable
fun GameScreen(bootScene: BootScene, mainActivity: MainActivity) { // Removed showMainMenu, using mainActivity.currentScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val currentEffectiveStage = bootScene.currentStage
        val currentScreen = mainActivity.currentScreen // Get current screen from activity

        val isBooting = currentScreen == Screen.BOOT &&
                currentEffectiveStage != BootStage.ERROR &&
                currentEffectiveStage != BootStage.COMPLETED &&
                bootScene.isBootSequenceActive()
        val displayError = currentScreen == Screen.BOOT && currentEffectiveStage == BootStage.ERROR

        // Background logic might need adjustment based on which screen is showing
        val showBootSceneBackgroundForBoot = (isBooting && currentEffectiveStage >= BootStage.FADING_IN_BACKGROUND)
        val showBootSceneBackgroundForMenuOrCredits = (currentScreen == Screen.MAIN_MENU || currentScreen == Screen.CREDITS) && bootScene.backgroundDrawableRes != 0


        if (bootScene.backgroundDrawableRes != 0 && (showBootSceneBackgroundForBoot || showBootSceneBackgroundForMenuOrCredits)) {
            val imageAlpha by animateFloatAsState(
                targetValue = if (showBootSceneBackgroundForMenuOrCredits || (isBooting && currentEffectiveStage >= BootStage.SHOWING_TITLE)) bootScene.backgroundAlpha else 0f,
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
                        isBooting && (currentEffectiveStage < BootStage.FADING_IN_BACKGROUND ||
                                (bootScene.backgroundAlpha < 0.01f && currentEffectiveStage == BootStage.FADING_IN_BACKGROUND)) -> Color.Black
                        (currentScreen == Screen.MAIN_MENU || currentScreen == Screen.CREDITS) && bootScene.backgroundDrawableRes == 0 -> Color.DarkGray // Fallback background
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (currentScreen) {
                Screen.BOOT -> {
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
                                    Button(onClick = { bootScene.handleContinueInput() }) { // This will trigger onComplete, then screen change
                                        Text(bootScene.continueButtonTextContent, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    } else { // Boot screen but not actively booting (e.g. idle or completed but not yet navigated by onComplete)
                        Box(contentAlignment = Alignment.Center) {
                            Text("Loading...", fontSize = 20.sp, color = Color.Gray)
                        }
                    }
                }
                Screen.MAIN_MENU -> {
                    MainMenuScreen(
                        onExitGame = { mainActivity.exitGameCleanup() },
                        onShowCredits = { mainActivity.navigateToCredits() }
                    )
                }
                Screen.CREDITS -> {
                    CreditsScreen(
                        onBack = { mainActivity.navigateToMainMenu() }
                    )
                }
                // Add other screens like Screen.GAMEPLAY later
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
                onDismissRequest() // Also dismiss after confirming
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = {
                onCancel()
                onDismissRequest() // Also dismiss after cancelling
            }) {
                Text("No")
            }
        }
    )
}

@Composable
fun MainMenuScreen(onExitGame: () -> Unit, onShowCredits: () -> Unit) {
    var showConfirmExitDialog by remember { mutableStateOf(false) } // State for dialog visibility

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Steve's Space Adventure Mystery",
            fontSize = 30.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = { Log.d("MainMenuScreen", "New Game Clicked (Action TBD)") }) {
            Text("New Game")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { Log.d("MainMenuScreen", "Settings Clicked (Action TBD)") }) {
            Text("Settings")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Log.d("MainMenuScreen", "Credits Clicked")
            onShowCredits()
        }) {
            Text("Credits")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Log.d("MainMenuScreen", "Exit Game Clicked - Showing confirmation")
            showConfirmExitDialog = true // Show the dialog
        }) {
            Text("Exit Game")
        }
    }

    if (showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismissRequest = {
                Log.d("MainMenuScreen", "Exit confirmation dialog dismissed (outside click/back)")
                showConfirmExitDialog = false
            },
            onConfirm = {
                Log.d("MainMenuScreen", "Exit Confirmed")
                onExitGame() // This will call mainActivity.exitGameCleanup()
                showConfirmExitDialog = false // Hide dialog after confirming
            },
            onCancel = {
                Log.d("MainMenuScreen", "Exit Cancelled")
                showConfirmExitDialog = false // Just hide the dialog
            }
        )
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.DarkGray) // You can set a specific background for credits
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Credits", fontSize = 24.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Game Design & Programming by: Your Name Here", color = Color.LightGray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Music: (Details about music if applicable)", color = Color.LightGray, textAlign = TextAlign.Center)
        // Add more credits...
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("Back to Menu")
        }
    }
}


// --- Previews ---
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
            config()
            if (initialStage >= BootStage.FADING_IN_BACKGROUND) {
                this.backgroundAlpha = 1f
                if (this.backgroundDrawableRes == 0) this.backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND
            }
            if (initialStage >= BootStage.SHOWING_TITLE && initialStage < BootStage.COMPLETED) this.showTitleText = true
            if (initialStage == BootStage.SHOWING_WARNING) this.showWarningText = true
            if (initialStage == BootStage.SHOWING_LOGO) this.showLogoImage = true
            if (initialStage == BootStage.AWAITING_CONTINUE_INPUT) {
                this.showTitleText = true
                this.showContinueButtonElements = true
            }
        }
    }
}

// Mock MainActivity for Previews that need it for screen state
@Composable
private fun rememberMockMainActivity(initialScreen: Screen = Screen.MAIN_MENU): MainActivity {
    val context = LocalContext.current
    return remember {
        // This is a simplified mock. It won't have fully functional BootScene or MediaPlayer.
        // It's primarily for providing the 'currentScreen' state for previews.
        object : MainActivity() {
            init {
                // Initialize BootScene minimally if needed by previews, though for menu/credits,
                // it's mostly about the background it might have set.
                super.bootScene = BootScene(context, {}, CoroutineScope(Dispatchers.Main)).apply{
                    backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND // Set a default background
                    backgroundAlpha = 1f
                }
                super.currentScreen = initialScreen
            }
            override fun navigateToCredits() { currentScreen = Screen.CREDITS }
            override fun navigateToMainMenu() { currentScreen = Screen.MAIN_MENU }
            override fun exitGameCleanup() { Log.d("Preview", "Exit Game") }
        }
    }
}


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
                logoDrawableRes = R.drawable.thunderhead_pictures_new_logo_processed // Ensure this exists
            },
            mainActivity = rememberMockMainActivity(initialScreen = Screen.BOOT)
        )
    }
}

@Preview(name = "Boot - Awaiting Continue", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewBootSceneAwaitingInput() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.AWAITING_CONTINUE_INPUT) {
                titleTextContent = "Steve's Grand Adventure!"
                continueButtonTextContent = "Press to Continue"
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
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED) { // Simulate boot completed state for background
                backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND // Or your actual menu background
                backgroundAlpha = 1f
            },
            mainActivity = rememberMockMainActivity(initialScreen = Screen.MAIN_MENU)
        )
    }
}


@Preview(name = "Credits Screen", showBackground = true, device = "spec:orientation=landscape,width=1280dp,height=800dp,dpi=240")
@Composable
fun PreviewCreditsScreen() {
    StevesAdventureAndMysteryTheme {
        GameScreen(
            bootScene = rememberMockBootScene(initialStage = BootStage.COMPLETED) { // Simulate boot completed state for background
                backgroundDrawableRes = PREVIEW_PLACEHOLDER_BACKGROUND // Or your actual menu background
                backgroundAlpha = 1f
            },
            mainActivity = rememberMockMainActivity(initialScreen = Screen.CREDITS)
        )
    }
}