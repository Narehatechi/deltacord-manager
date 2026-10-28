package app.deltacord.manager.ui.screen.deltacord

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deltacord.manager.domain.link.DeltacordLink
import app.deltacord.manager.domain.link.LinkCheck
import app.deltacord.manager.domain.link.LinkStatus
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DeltaDialog
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.Dialogue
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.SectionLabel
import app.deltacord.manager.ui.delta.ToggleOption
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

/** Where the link with Deltacord stands. */
enum class LinkState { ASKING, LIVE, SAVED, NEVER }

/** A request Deltacord didn't answer (it isn't running): offered to be done by opening it. */
data class Unanswered(val action: String, val target: String?, val value: Boolean?, val what: String)

/** DELTACORD: its health, plugins, themes and backups, through the link with the app. */
class DeltacordScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val link: DeltacordLink = get()
        val status by link.status.collectAsState()
        val scope = rememberCoroutineScope()
        var state by remember { mutableStateOf(if (status == null) LinkState.ASKING else LinkState.SAVED) }
        var busy by remember { mutableStateOf<String?>(null) }
        var unanswered by remember { mutableStateOf<Unanswered?>(null) }
        var failure by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            state = if (link.request("status") != null) LinkState.LIVE
            else if (link.status.value != null) LinkState.SAVED else LinkState.NEVER
        }

        val send = { action: String, target: String?, value: Boolean?, what: String ->
            scope.launch {
                busy = what
                val answer = link.request(action, target, value, timeoutMs = if (action == "backup") 30_000 else 6000)
                busy = null
                when {
                    answer == null -> unanswered = Unanswered(action, target, value, what)
                    answer.error != null -> failure = answer.error
                    else -> state = LinkState.LIVE
                }
            }
            Unit
        }

        unanswered?.let { u ->
            DeltaDialog(
                lines = listOf("Deltacord didn't answer. It's probably closed.", "Open it to ${u.what}?"),
                options = listOf("OPEN DELTACORD" to { link.open(u.action, u.target, u.value) }, "NOT NOW" to {}),
                onDismiss = { unanswered = null },
            )
        }
        failure?.let { f ->
            DeltaDialog(lines = listOf("That didn't work.", f), options = listOf("OK" to {}), onDismiss = { failure = null })
        }

        DeltacordView(
            status = status,
            state = state,
            busy = busy,
            onBack = { navigator.pop() },
            onOpen = { link.open() },
            onRefresh = { send("status", null, null, "check on it") },
            onPlugin = { id, on -> send("plugin", id, on, if (on) "turn that plugin on" else "turn that plugin off") },
            onTheme = { id -> send("theme", id, null, "change the theme") },
            onBackup = { send("backup", null, null, "make a backup") },
            onSafeMode = { on -> send("safemode", null, on, if (on) "start Safe Mode" else "leave Safe Mode") },
        )
    }
}

@Composable
fun DeltacordView(
    status: LinkStatus?,
    state: LinkState,
    busy: String?,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onRefresh: () -> Unit,
    onPlugin: (String, Boolean) -> Unit,
    onTheme: (String?) -> Unit,
    onBackup: () -> Unit,
    onSafeMode: (Boolean) -> Unit,
) {
    val problems = status?.checks?.filter { it.status == "bad" || it.status == "warn" }.orEmpty()
    val lines = when {
        busy != null -> listOf("Asking Deltacord to $busy...")
        state == LinkState.ASKING -> listOf("Calling Deltacord...")
        status == null -> listOf(
            "No word from Deltacord yet.",
            "Open it once (it needs the newest version), then come back.",
        )
        problems.isNotEmpty() -> listOf("Deltacord ${status.bundle.take(7)} is running.", "${problems.size} thing${if (problems.size > 1) "s" else ""} to look at.")
        state == LinkState.LIVE -> listOf("Deltacord ${status.bundle.take(7)} is running.", "Everything looks good.")
        else -> listOf("Last heard from Deltacord ${ago(status.at)}.", "Open it for the newest news.")
    }

    DeltaScreen(title = "Deltacord", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Dialogue(lines)
        Spacer(Modifier.height(8.dp))
        MenuOption("OPEN DELTACORD", onOpen)
        if (status != null) MenuOption("CHECK AGAIN", onRefresh, enabled = busy == null)

        if (status == null) return@DeltaScreen

        SectionLabel("Health")
        if (problems.isEmpty()) Text("* Nothing wrong.", style = DeltaType.body, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        for (check in problems) CheckLine(check)
        CheckLine(LinkCheck("", "Deltacord", status.bundle, "info"))
        CheckLine(LinkCheck("", "Module", status.module, "info"))
        CheckLine(LinkCheck("", "Discord", status.discord, "info"))

        SectionLabel("Plugins")
        for (plugin in status.plugins.sortedWith(compareBy({ !it.on }, { it.name.lowercase() }))) {
            ToggleOption(
                plugin.name,
                plugin.on,
                { onPlugin(plugin.id, it) },
                sub = if (plugin.on && !plugin.running && !status.safeMode) "On, but not running" else null,
                enabled = busy == null,
            )
        }

        SectionLabel("Themes")
        MenuOption("NO THEME", { onTheme(null) }, selected = status.themes.none { it.on }, style = DeltaType.body, enabled = busy == null)
        for (theme in status.themes) {
            MenuOption(theme.name, { onTheme(theme.id) }, selected = theme.on, style = DeltaType.body, enabled = busy == null)
        }

        SectionLabel("Backups")
        Text(
            "* " + (status.backups.newest?.let { "Newest backup: ${ago(it)} (${status.backups.count} kept)." } ?: "No backups yet."),
            style = DeltaType.body,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        MenuOption("BACK UP NOW", onBackup, style = DeltaType.body, enabled = busy == null, sub = "Plugins, settings, themes and logs, kept on your phone")
        MenuOption("RESTORE...", onOpen, style = DeltaType.body, sub = "In Deltacord: Settings › Backup & restore")

        SectionLabel("If something's wrong")
        ToggleOption(
            "Safe Mode",
            status.safeMode,
            onSafeMode,
            sub = "Starts Deltacord with plugins off",
            enabled = busy == null,
        )
    }
}

@Composable
private fun CheckLine(check: LinkCheck) {
    val color = when (check.status) {
        "bad" -> Delta.Red
        "warn" -> Delta.Yellow
        else -> Delta.White
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(check.label, style = DeltaType.body.copy(color = color), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(check.value, style = DeltaType.small.copy(color = if (check.status == "info") Delta.Gray else color), modifier = Modifier.weight(1.2f))
    }
    check.hint?.let { Text(it, style = DeltaType.small, modifier = Modifier.padding(bottom = 6.dp)) }
}

private fun ago(ms: Long): String {
    if (ms <= 0) return "a while ago"
    val minutes = ((System.currentTimeMillis() - ms) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 48 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} days ago"
    }
}
