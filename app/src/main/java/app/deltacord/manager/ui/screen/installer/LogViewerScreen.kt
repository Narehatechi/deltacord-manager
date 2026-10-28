package app.deltacord.manager.ui.screen.installer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.installer.util.LogEntry
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.ToggleOption
import app.deltacord.manager.ui.viewmodel.installer.LogViewerViewModel
import app.deltacord.manager.ui.widgets.installer.LogLine
import app.deltacord.manager.utils.rememberFileSaveLauncher
import app.deltacord.manager.utils.thenIf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf

/** The install log, line by line. Tap a line for its time; long-press to copy it. */
class LogViewerScreen(
    val logs: List<LogEntry>
) : Screen {

    @Composable
    override fun Content() {
        val prefs: PreferenceManager = get()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel: LogViewerViewModel = getScreenModel {
            parametersOf(logs)
        }
        val saveFile = rememberFileSaveLauncher(content = viewModel.logsString)

        DeltaScreen(title = "Log", onBack = { navigator.pop() }, scroll = false) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MenuOption(
                    "SAVE",
                    { saveFile.launch("${BuildConfig.MOD_NAME}-Manager-${System.currentTimeMillis()}.log") },
                    style = DeltaType.body,
                    modifier = Modifier.weight(1f),
                )
                MenuOption("SHARE", { viewModel.shareLogs(context) }, style = DeltaType.body, modifier = Modifier.weight(1f))
                MenuOption("COPY", { viewModel.copyLogs() }, style = DeltaType.body, modifier = Modifier.weight(1f))
            }
            ToggleOption("Wrap long lines", prefs.logsLineWrap, { prefs.logsLineWrap = it })
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .thenIf(!prefs.logsLineWrap) {
                        horizontalScroll(rememberScrollState())
                    }
            ) {
                itemsIndexed(viewModel.logs) { i, log ->
                    LogLine(
                        log = log,
                        alternateBackground = i % 2 == 0 && prefs.logsAlternateBackground,
                        wrapText = prefs.logsLineWrap,
                        logPadding = viewModel.maxLogLength,
                        onLongClick = { viewModel.copyLog(log) }
                    )
                }
            }
        }
    }
}
