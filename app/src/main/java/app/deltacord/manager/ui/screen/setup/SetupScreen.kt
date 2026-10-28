package app.deltacord.manager.ui.screen.setup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.Dialogue
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.SoulHeart
import app.deltacord.manager.ui.screen.home.HomeScreen
import app.deltacord.manager.utils.DiscordVersion
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.androidx.compose.get

/** The pages of the first setup. */
enum class SetupPage { HELLO, SEPARATE, CHANNEL, BACKGROUND, READY }

/** The first time the Manager opens: a short walk through, in the game's dialogue. */
class SetupScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val prefs: PreferenceManager = get()
        val context = LocalContext.current
        var page by rememberSaveable { mutableIntStateOf(0) }
        val pages = SetupPage.entries
        val next = { page = (page + 1).coerceAtMost(pages.lastIndex) }
        val finish = { install: Boolean ->
            prefs.setupDone = true
            navigator.replaceAll(HomeScreen(autoInstall = install))
        }

        SetupView(
            page = pages[page],
            sounds = prefs.sounds,
            channel = prefs.channel,
            needsBackground = !ignoringBatteryOptimizations(context),
            onSounds = { prefs.sounds = it },
            onNext = next,
            onBack = { page = (page - 1).coerceAtLeast(0) },
            onChannel = { prefs.channel = it; next() },
            onAllowBackground = {
                prefs.hasAskedForBatteryOpt = true
                requestBackground(context)
                next()
            },
            onSkipBackground = { prefs.hasAskedForBatteryOpt = true; next() },
            onFinish = finish,
        )
    }
}

@Composable
fun SetupView(
    page: SetupPage,
    sounds: Boolean,
    channel: DiscordVersion.Type,
    needsBackground: Boolean,
    onSounds: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onChannel: (DiscordVersion.Type) -> Unit,
    onAllowBackground: () -> Unit,
    onSkipBackground: () -> Unit,
    onFinish: (install: Boolean) -> Unit,
) {
    DeltaScreen(title = null, onBack = if (page != SetupPage.HELLO) onBack else null) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SoulHeart(pixel = 6.dp) }
        Spacer(Modifier.height(32.dp))

        when (page) {
            SetupPage.HELLO -> {
                Dialogue(listOf("Hello.", "This is the DELTACORD Manager.", "It puts Deltacord on your phone."))
                Spacer(Modifier.height(20.dp))
                MenuOption("NEXT", onNext, selected = true)
                MenuOption("SOUND", { onSounds(!sounds) }, trailing = if (sounds) "ON" else "OFF", sub = "Text blips and menu sounds")
            }

            SetupPage.SEPARATE -> {
                Dialogue(
                    listOf(
                        "Deltacord installs as its own app, next to Discord.",
                        "Your normal Discord isn't changed. You can keep both.",
                    )
                )
                Spacer(Modifier.height(20.dp))
                MenuOption("NEXT", onNext, selected = true)
            }

            SetupPage.CHANNEL -> {
                Dialogue(listOf("Which Discord should Deltacord be made from?"))
                Spacer(Modifier.height(20.dp))
                MenuOption(
                    "STABLE", { onChannel(DiscordVersion.Type.STABLE) },
                    selected = channel == DiscordVersion.Type.STABLE,
                    sub = "The normal Discord. Best for most people.",
                )
                MenuOption(
                    "BETA", { onChannel(DiscordVersion.Type.BETA) },
                    selected = channel == DiscordVersion.Type.BETA,
                    sub = "New things sooner, a little less steady.",
                )
                MenuOption(
                    "ALPHA", { onChannel(DiscordVersion.Type.ALPHA) },
                    selected = channel == DiscordVersion.Type.ALPHA,
                    sub = "The very newest. Things may break.",
                )
            }

            SetupPage.BACKGROUND -> {
                if (needsBackground) {
                    Dialogue(
                        listOf(
                            "Installing downloads over 100 MB.",
                            "Let the Manager keep going in the background, so it doesn't stop when the screen turns off?",
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    MenuOption("ALLOW", onAllowBackground, selected = true)
                    MenuOption("NOT NOW", onSkipBackground)
                } else {
                    Dialogue(listOf("Installing downloads over 100 MB.", "Wi-Fi is a good idea."))
                    Spacer(Modifier.height(20.dp))
                    MenuOption("NEXT", onNext, selected = true)
                }
            }

            SetupPage.READY -> {
                Dialogue(listOf("That's everything.", "Install Deltacord now?"))
                Spacer(Modifier.height(20.dp))
                MenuOption("INSTALL NOW", { onFinish(true) }, selected = true)
                MenuOption("LATER", { onFinish(false) })
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "${page.ordinal + 1} / ${SetupPage.entries.size}",
            style = DeltaType.small,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ignoringBatteryOptimizations(context: Context): Boolean = runCatching {
    (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
}.getOrDefault(true)

@SuppressLint("BatteryLife")
private fun requestBackground(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
