package app.deltacord.manager.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.deltacord.manager.ui.screen.home.HomeScreen
import app.deltacord.manager.ui.screen.installer.InstallerScreen
import app.deltacord.manager.domain.manager.InstallManager
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.ui.delta.DeltaTheme
import app.deltacord.manager.ui.screen.setup.SetupScreen
import org.koin.android.ext.android.get
import app.deltacord.manager.utils.DiscordVersion
import app.deltacord.manager.utils.Intents
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val version = intent.getStringExtra(Intents.Extras.VERSION)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.POST_NOTIFICATIONS"),
                0
            )
        }

        val prefs: PreferenceManager = get()
        val screen = when {
            intent.action == Intents.Actions.INSTALL && version != null ->
                InstallerScreen(DiscordVersion.fromVersionCode(version)!!)
            // The first time (and nothing installed yet): the guided setup.
            !prefs.setupDone && get<InstallManager>().current == null -> SetupScreen()
            else -> HomeScreen()
        }

        setContent {
            DeltaTheme(sounds = { prefs.sounds }, flashy = prefs.flashy) {
                Navigator(screen) {
                    FadeTransition(it)
                }
            }
        }
    }
}
