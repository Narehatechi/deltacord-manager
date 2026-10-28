package app.deltacord.manager.ui.screen.installer

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.installer.step.StepStatus
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DeltaDialog
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.Dialogue
import app.deltacord.manager.ui.delta.HpBar
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.SectionLabel
import app.deltacord.manager.ui.delta.SoulHeart
import app.deltacord.manager.ui.viewmodel.installer.InstallerViewModel
import app.deltacord.manager.utils.DiscordVersion
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf
import java.util.UUID

/** One step as the installer screen shows it. */
data class StepView(val name: String, val status: StepStatus, val progress: Float?, val durationMs: Int)

/** Everything the installer screen shows. */
data class InstallView(
    val groups: List<Pair<String, List<StepView>>>,
    val completed: Boolean,
    val appName: String,
) {
    val steps get() = groups.flatMap { it.second }
    val current get() = steps.firstOrNull { it.status == StepStatus.ONGOING }
    val failed get() = steps.firstOrNull { it.status == StepStatus.UNSUCCESSFUL }
    val success get() = completed && failed == null && steps.all { it.status == StepStatus.SUCCESSFUL }

    /** Finished steps, plus how far the current one is. */
    val progress: Float
        get() {
            if (steps.isEmpty()) return 0f
            val done = steps.count { it.status == StepStatus.SUCCESSFUL }
            return (done + (current?.progress ?: 0f)) / steps.size
        }
}

class InstallerScreen(val version: DiscordVersion) : Screen {

    override val key: ScreenKey = "Installer-${UUID.randomUUID()}"

    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        val activity = LocalContext.current as? ComponentActivity
        val prefs: PreferenceManager = get()
        val viewModel: InstallerViewModel = getScreenModel { parametersOf(version) }

        // Error messages from the install service.
        val intentListener: (Intent) -> Unit = remember {
            {
                val msg = it.getStringExtra("deltacord.extras.EXTRA_MESSAGE")
                if (msg?.isNotBlank() == true) viewModel.logError(msg)
            }
        }
        DisposableEffect(Unit) {
            activity?.addOnNewIntentListener(intentListener)
            onDispose { activity?.removeOnNewIntentListener(intentListener) }
        }

        val runner = viewModel.runner
        BackHandler(enabled = !runner.completed) { viewModel.openBackDialog() }

        if (viewModel.backDialogOpened) {
            DeltaDialog(
                lines = listOf("Leave now?", "Stopping halfway can leave broken downloads behind."),
                options = listOf(
                    "LEAVE" to { viewModel.cancelInstall(); nav.pop() },
                    "STAY" to {},
                ),
                onDismiss = { viewModel.closeBackDialog() },
            )
        }
        if (runner.downloadErrored) {
            DeltaDialog(
                lines = listOf("A download failed.", "Your connection may have dropped. Try again?"),
                options = listOf(
                    "TRY AGAIN" to { viewModel.cancelInstall(); nav.replace(InstallerScreen(version)) },
                    "NOT NOW" to {},
                ),
                onDismiss = { viewModel.dismissDownloadFailedDialog() },
            )
        }

        val view = InstallView(
            groups = viewModel.groupedSteps.map { (group, steps) ->
                stringResource(group.nameRes) to steps.map { StepView(stringResource(it.nameRes), it.status, it.progress, it.durationMs) }
            },
            completed = runner.completed,
            appName = prefs.appName,
        )

        InstallerView(
            view = view,
            onBack = { if (!runner.completed) viewModel.openBackDialog() else nav.pop() },
            onLaunch = viewModel::launchMod,
            onShareLogs = { activity?.let(viewModel::shareLogs) },
            onViewLogs = { nav.push(LogViewerScreen(runner.logger.logs.toImmutableList())) },
            onClearCache = viewModel::clearCache,
            onRetry = { nav.replace(InstallerScreen(version)) },
        )
    }
}

@Composable
fun InstallerView(
    view: InstallView,
    onBack: () -> Unit,
    onLaunch: () -> Unit,
    onShareLogs: () -> Unit,
    onViewLogs: () -> Unit,
    onClearCache: () -> Unit,
    onRetry: () -> Unit = {},
) {
    val lines = when {
        view.success -> listOf("${view.appName} is installed.", "You're filled with DETERMINATION.")
        view.failed != null -> listOf(
            "Something went wrong: ${view.failed!!.name.lowercase()}.",
            "Share the log so it can be fixed.",
        )
        view.completed -> listOf("The install stopped.")
        else -> listOf((view.current?.name ?: "Getting ready") + "...")
    }

    DeltaScreen(title = "Install", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Dialogue(lines)
        Spacer(Modifier.height(16.dp))
        HpBar(if (view.completed) (if (view.success) 1f else view.progress) else view.progress, label = "HP")

        for ((group, steps) in view.groups) {
            SectionLabel(group)
            for (step in steps) StepLine(step)
        }

        if (view.completed) {
            Spacer(Modifier.height(20.dp))
            if (view.success) MenuOption("LAUNCH", onLaunch, selected = true)
            else MenuOption("TRY AGAIN", onRetry, selected = true)
            MenuOption("SHARE LOG", onShareLogs)
            MenuOption("VIEW LOG", onViewLogs)
            MenuOption("CLEAR CACHE", onClearCache, sub = "Download everything again next time")
            MenuOption("BACK", onBack)
        }
    }
}

@Composable
private fun StepLine(step: StepView) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Box(Modifier.width(34.dp)) {
            when (step.status) {
                StepStatus.ONGOING -> SoulHeart(pixel = 2.dp)
                StepStatus.UNSUCCESSFUL -> SoulHeart(pixel = 2.dp, color = Delta.Gray)
                else -> {}
            }
        }
        Text(
            step.name,
            style = DeltaType.body.copy(
                color = when (step.status) {
                    StepStatus.ONGOING -> Delta.Yellow
                    StepStatus.UNSUCCESSFUL -> Delta.Red
                    StepStatus.SUCCESSFUL -> Delta.White
                    StepStatus.QUEUED -> Delta.Gray
                }
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            when (step.status) {
                StepStatus.QUEUED -> ""
                StepStatus.ONGOING -> step.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
                StepStatus.SUCCESSFUL -> "%.1fs".format(step.durationMs / 1000f)
                StepStatus.UNSUCCESSFUL -> "FAILED"
            },
            style = DeltaType.small.copy(color = if (step.status == StepStatus.UNSUCCESSFUL) Delta.Red else Delta.Gray),
        )
    }
}
