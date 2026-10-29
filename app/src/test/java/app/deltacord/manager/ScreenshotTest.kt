package app.deltacord.manager

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.Composable
import app.deltacord.manager.domain.link.LinkBackups
import app.deltacord.manager.domain.link.LinkCheck
import app.deltacord.manager.domain.link.LinkPlugin
import app.deltacord.manager.domain.link.LinkStatus
import app.deltacord.manager.domain.link.LinkTheme
import app.deltacord.manager.installer.step.StepStatus
import app.deltacord.manager.ui.delta.DeltaTheme
import app.deltacord.manager.ui.screen.deltacord.DeltacordView
import app.deltacord.manager.ui.screen.deltacord.LinkState
import app.deltacord.manager.ui.screen.home.HomeAction
import app.deltacord.manager.ui.screen.home.HomeState
import app.deltacord.manager.ui.screen.home.MainMenu
import app.deltacord.manager.ui.screen.installer.InstallView
import app.deltacord.manager.ui.screen.installer.InstallerView
import app.deltacord.manager.ui.screen.installer.StepView
import app.deltacord.manager.ui.screen.setup.SetupPage
import app.deltacord.manager.ui.screen.setup.SetupView
import app.deltacord.manager.utils.DiscordVersion
import org.junit.Rule
import org.junit.Test

/** Pictures of each screen, so the look can be checked without a phone (see the Screenshots workflow). */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        maxPercentDifference = 0.1,
    )

    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name) { DeltaTheme(still = true) { content() } }

    private val none = {}

    @Composable
    private fun menu(state: HomeState) = MainMenu(state, none, none, none, none, none, none, none, none, none, none)

    @Test
    fun mainMenuUpdate() = shot("menu-update") {
        menu(HomeState("Deltacord", "351.11 - Stable", "352.4 - Stable", false, HomeAction.UPDATE, managerUpdate = null))
    }

    @Test
    fun mainMenuFresh() = shot("menu-fresh") {
        menu(HomeState("Deltacord", null, "352.4 - Stable", false, HomeAction.INSTALL, managerUpdate = "v1.9.0"))
    }

    @Test
    fun setupHello() = shot("setup-hello") {
        SetupView(SetupPage.HELLO, true, DiscordVersion.Type.STABLE, true, {}, none, none, {}, none, none, {})
    }

    @Test
    fun setupChannel() = shot("setup-channel") {
        SetupView(SetupPage.CHANNEL, true, DiscordVersion.Type.STABLE, true, {}, none, none, {}, none, none, {})
    }

    private fun steps(vararg s: Pair<String, StepStatus>) = s.map { StepView(it.first, it.second, if (it.second == StepStatus.ONGOING) 0.42f else null, 2300) }

    @Test
    fun installing() = shot("install-running") {
        InstallerView(
            InstallView(
                listOf(
                    "Download APKs" to steps("Downloading base apk" to StepStatus.SUCCESSFUL, "Downloading libraries apk" to StepStatus.ONGOING, "Downloading Deltacord" to StepStatus.QUEUED),
                    "Patching" to steps("Changing app icon" to StepStatus.QUEUED, "Injecting Deltacord" to StepStatus.QUEUED),
                    "Installing" to steps("Installing APKs" to StepStatus.QUEUED),
                ),
                completed = false,
                appName = "Deltacord",
            ),
            none, none, none, none, none,
        )
    }

    @Test
    fun installed() = shot("install-done") {
        InstallerView(
            InstallView(
                listOf(
                    "Download APKs" to steps("Downloading base apk" to StepStatus.SUCCESSFUL, "Downloading Deltacord" to StepStatus.SUCCESSFUL),
                    "Installing" to steps("Installing APKs" to StepStatus.SUCCESSFUL),
                ),
                completed = true,
                appName = "Deltacord",
            ),
            none, none, none, none, none,
        )
    }

    @Test
    fun installFailed() = shot("install-failed") {
        InstallerView(
            InstallView(
                listOf("Download APKs" to steps("Downloading base apk" to StepStatus.SUCCESSFUL, "Downloading Deltacord" to StepStatus.UNSUCCESSFUL)),
                completed = true,
                appName = "Deltacord",
            ),
            none, none, none, none, none,
        )
    }

    private val status = LinkStatus(
        at = System.currentTimeMillis(),
        bundle = "b027c7b-main",
        module = "DeltacordXposed 1.15.1",
        discord = "351.11 (351011)",
        checks = listOf(
            LinkCheck("Self-test", "Gift & apps buttons", "waiting for Discord to load the chat bar", "warn"),
            LinkCheck("Plugins", "Plugins", "16 on, 16 running", "ok"),
        ),
        plugins = listOf(
            LinkPlugin("deltacord.typewriter", "Typewriter", true, true, true),
            LinkPlugin("deltacord.messagelogger", "Message Logger", true, true, true),
            LinkPlugin("deltacord.freeemojis", "Free Emojis", false, true, false),
        ),
        themes = listOf(LinkTheme("deltarune", "Deltarune", true), LinkTheme("lightworld", "Light World", false)),
        backups = LinkBackups(System.currentTimeMillis() - 39 * 3_600_000L, 3),
    )

    @Test
    fun deltacordLinked() = shot("deltacord-linked") {
        DeltacordView(status, LinkState.LIVE, null, none, none, none, { _, _ -> }, {}, none, {})
    }

    @Test
    fun report() = shot("report") {
        app.deltacord.manager.ui.screen.deltacord.ReportView(
            app.deltacord.manager.domain.link.SavedReport(
                "Deltacord report (from the module)\n2026-10-29 08:41:00 -0600\n\n===== Versions =====\nDiscord: 351.11 (351011) · package app.deltacord\nModule: 1.18.0\nAndroid 16 (SDK 36) · samsung SM-S928U\n\n===== This launch =====\nRunning for 52s\nDeltacord b027c7b-main\nDeltacord finished starting: yes · plugins started: yes · Discord's screen ready: no\nLast steps: done: plugin deltacord.typewriter @10759ms > ready @10762ms",
                System.currentTimeMillis(),
            ),
            app.deltacord.manager.ui.screen.deltacord.ReportState.GOT,
            none, none, none, none,
        )
    }

    @Test
    fun deltacordNever() = shot("deltacord-never") {
        DeltacordView(null, LinkState.NEVER, null, none, none, none, { _, _ -> }, {}, none, {})
    }
}
