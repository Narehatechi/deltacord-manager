package app.deltacord.manager.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DarkFountain
import app.deltacord.manager.ui.delta.DeltaDialog
import app.deltacord.manager.ui.delta.FlashyLogo
import app.deltacord.manager.ui.delta.LocalFlashy
import app.deltacord.manager.ui.delta.entrance
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.HpBar
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.PixelBox
import app.deltacord.manager.ui.delta.SoulHeart
import app.deltacord.manager.ui.screen.about.AboutScreen
import app.deltacord.manager.ui.screen.deltacord.DeltacordScreen
import app.deltacord.manager.ui.screen.installer.InstallerScreen
import app.deltacord.manager.ui.screen.settings.SettingsScreen
import app.deltacord.manager.ui.viewmodel.home.HomeViewModel
import app.deltacord.manager.ui.widgets.dialog.BatteryOptimizationDialog
import app.deltacord.manager.utils.DiscordVersion
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.androidx.compose.get

/** Everything the main menu shows (kept apart from where it comes from, so it can be previewed). */
data class HomeState(
    val appName: String,
    val installed: String?,
    val latest: String?,
    val targetPinned: Boolean,
    val action: HomeAction,
    val managerUpdate: String?,
    val updating: Boolean = false,
    val updateProgress: Float? = null,
)

enum class HomeAction(val label: String, val enabled: Boolean) {
    LOADING("LOADING...", false),
    INSTALL("INSTALL", true),
    UPDATE("UPDATE", true),
    REINSTALL("REINSTALL", true),
    DOWNGRADE("DOWNGRADE", true),
    NO_DOWNGRADE("NEWER INSTALLED", false),
}

/** [autoInstall]: start installing as soon as Discord's newest version is known (from the setup). */
class HomeScreen(private val autoInstall: Boolean = false) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val prefs: PreferenceManager = get()
        val viewModel: HomeViewModel = getScreenModel()

        val current = remember(viewModel.installManager.current) {
            DiscordVersion.fromVersionCode(viewModel.installManager.current?.versionCode.toString())
        }
        val latest = remember(prefs.discordVersion, viewModel.discordVersions, prefs.channel) {
            when {
                prefs.discordVersion.isBlank() -> viewModel.discordVersions?.get(prefs.channel)
                else -> DiscordVersion.fromVersionCode(prefs.discordVersion)
            }
        }
        val action = when {
            latest == null -> HomeAction.LOADING
            current == null -> HomeAction.INSTALL
            current == latest -> HomeAction.REINSTALL
            latest > current -> HomeAction.UPDATE
            prefs.allowDowngrade -> HomeAction.DOWNGRADE
            else -> HomeAction.NO_DOWNGRADE
        }
        var progress by remember { mutableStateOf<Float?>(null) }
        var autoStarted by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(latest) {
            if (autoInstall && !autoStarted && latest != null && current == null) {
                autoStarted = true
                navigator.push(InstallerScreen(latest))
            }
        }
        val state = HomeState(
            appName = prefs.appName,
            installed = current?.toString(),
            latest = latest?.toString(),
            targetPinned = prefs.discordVersion.isNotBlank(),
            action = action,
            managerUpdate = viewModel.release?.versionName?.takeIf { viewModel.showUpdateDialog && !BuildConfig.DEBUG },
            updating = viewModel.isUpdating,
            updateProgress = progress,
        )

        BatteryOptimizationDialog()

        // Check for a newer Manager whenever it comes back on screen: Android keeps the app in
        // memory, so checking only on start missed releases that came out while it was open.
        @Suppress("DEPRECATION")
        val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
        androidx.compose.runtime.DisposableEffect(lifecycle) {
            var first = true
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    if (!first) viewModel.checkForUpdate()
                    first = false
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }

        MainMenu(
            state = state,
            onInstall = { latest?.let { navigator.push(InstallerScreen(it)) } },
            onLaunch = viewModel::launchMod,
            onAppInfo = viewModel::launchModInfo,
            onUninstall = viewModel::uninstallMod,
            onDeltacord = { navigator.push(DeltacordScreen()) },
            onChanges = { navigator.push(ChangesScreen()) },
            onConfig = { navigator.push(SettingsScreen()) },
            onAbout = { navigator.push(AboutScreen()) },
            onRefresh = { viewModel.getDiscordVersions(); viewModel.refreshCommits(); viewModel.checkForUpdate() },
            onUpdateManager = { viewModel.downloadAndInstallUpdate { progress = it } },
        )
    }
}

/** The main menu, like the game's title screen: the logo, the save file, and what to do. */
@Composable
fun MainMenu(
    state: HomeState,
    onInstall: () -> Unit,
    onLaunch: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDeltacord: () -> Unit,
    onChanges: () -> Unit,
    onConfig: () -> Unit,
    onAbout: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateManager: () -> Unit,
) {
    var options by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    val installed = state.installed != null

    if (options) {
        DeltaDialog(
            lines = listOf("What will you do with ${state.appName}?"),
            options = listOf(
                "APP INFO" to onAppInfo,
                "UNINSTALL" to { confirmUninstall = true },
                "NEVERMIND" to {},
            ),
            onDismiss = { options = false },
        )
    }
    if (confirmUninstall) {
        DeltaDialog(
            lines = listOf(
                "This removes ${state.appName} from your phone.",
                "Your normal Discord app isn't touched.",
            ),
            options = listOf("UNINSTALL" to onUninstall, "KEEP IT" to {}),
            onDismiss = { confirmUninstall = false },
        )
    }

    DeltaScreen(title = null, onBack = null, strong = true) {
        // The logo over the Dark Fountain, like the game's site.
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            DarkFountain(Modifier.fillMaxWidth().height(190.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlashyLogo("DELTACORD", DeltaType.logo)
                Text(
                    "MANAGER  v${BuildConfig.VERSION_NAME}",
                    style = DeltaType.small.copy(
                        letterSpacing = DeltaType.title.letterSpacing,
                        color = Delta.White,
                        shadow = androidx.compose.ui.graphics.Shadow(Delta.Black, blurRadius = 8f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).entrance(10),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Box(Modifier.entrance(0)) { SaveFile(state) }

        if (state.managerUpdate != null) {
            Spacer(Modifier.height(16.dp))
            PixelBox(borderColor = Delta.Yellow) {
                Text("* A new Manager is here: ${state.managerUpdate}.", style = DeltaType.body)
                if (state.updating) {
                    Spacer(Modifier.height(10.dp))
                    HpBar(state.updateProgress, label = "GET")
                } else {
                    MenuOption("UPDATE THE MANAGER", onUpdateManager, style = DeltaType.body)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        MenuOption(
            modifier = Modifier.entrance(1),
            text = state.action.label,
            onClick = onInstall,
            selected = state.action.enabled,
            enabled = state.action.enabled,
            sub = when (state.action) {
                HomeAction.INSTALL -> "Download Discord and add Deltacord to it"
                HomeAction.UPDATE -> "Discord ${state.latest} is out"
                HomeAction.REINSTALL -> "Install the same version again"
                HomeAction.DOWNGRADE -> "Go back to Discord ${state.latest}"
                HomeAction.NO_DOWNGRADE -> "A newer Discord is installed (CONFIG to allow going back)"
                HomeAction.LOADING -> "Checking Discord's versions..."
            },
        )
        MenuOption("LAUNCH", onLaunch, enabled = installed, modifier = Modifier.entrance(2))
        MenuOption("DELTACORD", onDeltacord, enabled = installed, sub = "Health check, plugins, themes, backups, report", modifier = Modifier.entrance(3))
        MenuOption("CHANGES", onChanges, sub = "What's new in Deltacord", modifier = Modifier.entrance(4))
        MenuOption("CONFIG", onConfig, modifier = Modifier.entrance(5))
        if (installed) MenuOption("OPTIONS", { options = true }, sub = "App info, uninstall", modifier = Modifier.entrance(6))
        MenuOption("ABOUT", onAbout, modifier = Modifier.entrance(7))
        MenuOption("REFRESH", onRefresh, style = DeltaType.body, color = Delta.Gray, modifier = Modifier.entrance(8))
        // Room to see the scene at the bottom (it's drawn behind the menu).
        if (LocalFlashy.current) Spacer(Modifier.height(110.dp))
    }
}

/** The "save file": which Discord is installed and what's newest, like the game's file select. */
@Composable
private fun SaveFile(state: HomeState) {
    PixelBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoulHeart(pixel = 3.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(state.appName.uppercase(), style = DeltaType.title)
                Text(
                    when {
                        state.installed == null -> "Not installed yet"
                        state.latest == null -> "Discord ${state.installed}"
                        state.installed == state.latest -> "Discord ${state.installed} · up to date"
                        else -> "Discord ${state.installed}"
                    },
                    style = DeltaType.body.copy(color = if (state.installed == null) Delta.Gray else Delta.White),
                )
            }
        }
        if (state.latest != null && state.installed != state.latest) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.targetPinned) "TARGET" else "NEWEST", style = DeltaType.small)
                Text("Discord ${state.latest}", style = DeltaType.small.copy(color = Delta.Yellow))
            }
        }
    }
}
