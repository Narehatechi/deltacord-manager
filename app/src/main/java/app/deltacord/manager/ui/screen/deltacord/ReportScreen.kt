package app.deltacord.manager.ui.screen.deltacord

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.domain.link.DeltacordLink
import app.deltacord.manager.domain.link.SavedReport
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DeltaDialog
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.Dialogue
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.PixelBox
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get

/** Where getting a report stands. */
enum class ReportState { ASKING, GOT, NO_ANSWER, OPENED }

/**
 * REPORT: everything about Deltacord on this phone, built by the Deltacord module (so it works even
 * when Deltacord is stuck on Discord's logo): versions, how far startup got, what Discord is doing,
 * the last Doctor report and Discord's recent log. Copy or share it to get a problem fixed.
 */
class ReportScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val link: DeltacordLink = get()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val report by link.report.collectAsState()
        var state by remember { mutableStateOf(ReportState.ASKING) }
        var askOpen by remember { mutableStateOf(false) }
        val startedAt = remember { System.currentTimeMillis() }

        val ask = {
            scope.launch {
                state = ReportState.ASKING
                state = if (link.requestReport() != null) ReportState.GOT else ReportState.NO_ANSWER
                if (state == ReportState.NO_ANSWER) askOpen = true
            }
            Unit
        }
        LaunchedEffect(Unit) { ask() }
        // A report that arrives later (after opening Deltacord) shows by itself.
        LaunchedEffect(report?.at) {
            if ((report?.at ?: 0) > startedAt && state != ReportState.GOT) state = ReportState.GOT
        }

        if (askOpen) {
            DeltaDialog(
                lines = listOf(
                    "Deltacord didn't answer. It's probably closed.",
                    "Open it? The report comes back here about 35 seconds later, even if Deltacord gets stuck.",
                ),
                options = listOf(
                    "OPEN DELTACORD" to { state = ReportState.OPENED; link.open("report") },
                    "NOT NOW" to {},
                ),
                onDismiss = { askOpen = false },
            )
        }

        ReportView(
            report = report,
            state = state,
            onBack = { navigator.pop() },
            onAgain = ask,
            onCopy = { report?.let { copy(context, it.text) } },
            onShare = { report?.let { share(context, it) } },
        )
    }
}

@Composable
fun ReportView(
    report: SavedReport?,
    state: ReportState,
    onBack: () -> Unit,
    onAgain: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val lines = when (state) {
        ReportState.ASKING -> listOf("Asking Deltacord for a report...")
        ReportState.OPENED -> listOf("Waiting for Deltacord.", "Come back here in about 35 seconds.")
        ReportState.NO_ANSWER -> if (report != null) listOf("Deltacord didn't answer.", "This is the last report it sent.") else listOf("Deltacord didn't answer.")
        ReportState.GOT -> listOf("Here's Deltacord's report.", "Copy or share it with whoever is fixing it.")
    }
    DeltaScreen(title = "Report", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Dialogue(lines)
        Spacer(Modifier.height(8.dp))
        if (report != null) {
            Row {
                MenuOption("COPY", onCopy, style = DeltaType.body, modifier = Modifier.weight(1f))
                MenuOption("SHARE", onShare, style = DeltaType.body, modifier = Modifier.weight(1f))
            }
        }
        MenuOption("GET A NEW ONE", onAgain, style = DeltaType.body, enabled = state != ReportState.ASKING)
        if (report != null) {
            Spacer(Modifier.height(12.dp))
            PixelBox(padding = 10.dp) {
                SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                    Text(
                        report.text.take(60_000),
                        style = DeltaType.small.copy(color = Delta.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                    )
                }
            }
            if (report.text.length > 60_000) {
                Text("* The rest is in COPY and SHARE.", style = DeltaType.small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Deltacord report", text.take(100_000)))
        Toast.makeText(context, "Report copied", Toast.LENGTH_SHORT).show()
    }
}

/** Shares the report as a file (it can be too big to share as text). */
private fun share(context: Context, report: SavedReport) {
    runCatching {
        val dir = context.filesDir.resolve("reports").apply { deleteRecursively(); mkdirs() }
        val file = dir.resolve("Deltacord-report-${report.at}.txt").apply { writeText(report.text) }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Share Deltacord's report",
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
