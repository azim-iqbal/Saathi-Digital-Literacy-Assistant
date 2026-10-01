package com.saathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.saathi.ui.SaathiApp
import com.saathi.ui.LaunchReveal
import com.saathi.ui.Preferences
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

open class MainActivity : ComponentActivity() {
    private val launchReveal = LaunchReveal()
    companion object {
        const val EXTRA_LAUNCH_TASK = "launch_task"
        const val PREFERENCES_NAME = "saathi_preferences"
        const val LANGUAGE_PREFERENCE = "guidance_language"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            launchReveal.show(provider, savedInstanceState != null || Preferences(this).reducedMotion)
        }
        enableEdgeToEdge()
        setContent { SaathiApp() }
    }
    override fun onStop() {
        launchReveal.finish()
        super.onStop()
    }
    override fun onDestroy() {
        launchReveal.finish()
        super.onDestroy()
    }
}
