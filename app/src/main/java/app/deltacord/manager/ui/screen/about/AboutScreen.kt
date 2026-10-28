package app.deltacord.manager.ui.screen.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.Dialogue
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.SectionLabel
import app.deltacord.manager.ui.screen.libraries.LibrariesScreen
import app.deltacord.manager.utils.Constants
import app.deltacord.manager.utils.showToast
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.androidx.compose.get

/** People who made what Deltacord is built on. */
private val THANKS = listOf(
    Triple("Palm", "Lead developer of Revenge", "palmdevs"),
    Triple("oSumAtrIX", "Developer of Revenge", "osumatrix"),
    Triple("Nexpid", "Developer of Revenge", "nexpid"),
    Triple("Pylix", "Past developer of Bunny", "pylixonly"),
    Triple("Fiery", "Past developer of the iOS tweak", "FieryFlames"),
    Triple("Maisy", "Past developer of Vendetta", "maisymoe"),
    Triple("Wing", "Past developer of Manager", "wingio"),
    Triple("Kasi", "Past developer of the Xposed module", "redstonekasi"),
    Triple("rushii", "The installer, zip library and patching", "rushiiMachine"),
    Triple("Xinto", "The preference manager", "X1nto"),
)

class AboutScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uri = LocalUriHandler.current
        val ctx = LocalContext.current
        val prefs: PreferenceManager = get()
        var taps by remember { mutableIntStateOf(0) }

        DeltaScreen(title = "About", onBack = { navigator.pop() }) {
            Spacer(Modifier.height(16.dp))
            Text("DELTACORD", style = DeltaType.logo, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(
                "MANAGER v${BuildConfig.VERSION_NAME}",
                style = DeltaType.small,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    // Ten taps: the developer options in CONFIG.
                    .clickable(
                        enabled = !prefs.isDeveloper,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        taps++
                        when (taps) {
                            3 -> ctx.showToast(R.string.msg_seven_left)
                            5 -> ctx.showToast(R.string.msg_five_left)
                            8 -> ctx.showToast(R.string.msg_two_left)
                            10 -> {
                                ctx.showToast(R.string.msg_unlocked)
                                prefs.isDeveloper = true
                            }
                        }
                    },
            )
            Spacer(Modifier.height(20.dp))
            Dialogue(listOf("Discord, with an Undertale and Deltarune heart."))

            Spacer(Modifier.height(12.dp))
            MenuOption("GITHUB", { uri.openUri(BuildConfig.ORG_LINK) })
            MenuOption("DISCORD SERVER", { uri.openUri(BuildConfig.INVITE_LINK) })

            SectionLabel("Team")
            for (member in Constants.TEAM_MEMBERS) {
                MenuOption(member.name, { uri.openUri("https://github.com/${member.username}") }, style = DeltaType.body, sub = member.role)
            }

            SectionLabel("Special thanks")
            for ((name, role, user) in THANKS) {
                MenuOption(name, { uri.openUri("https://github.com/$user") }, style = DeltaType.body, sub = role)
            }

            SectionLabel("More")
            MenuOption("OPEN SOURCE LICENSES", { navigator.push(LibrariesScreen()) }, style = DeltaType.body)
        }
    }
}
