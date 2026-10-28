package app.deltacord.manager.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.deltacord.manager.network.dto.Commit
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.viewmodel.home.HomeViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/** Deltacord's recent changes, as dialogue lines. Tap one to see it on GitHub. */
class ChangesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: HomeViewModel = getScreenModel()
        val commits = viewModel.commits.collectAsLazyPagingItems()
        val uri = LocalUriHandler.current

        DeltaScreen(title = "Changes", onBack = { navigator.pop() }, scroll = false) {
            LazyColumn(Modifier.fillMaxSize()) {
                // Reading commits[i] is what loads the next page as you scroll.
                items(count = commits.itemCount, key = commits.itemKey { it.sha }) { i ->
                    commits[i]?.let { commit -> ChangeLine(commit) { uri.openUri(commit.url) } }
                }
                val loading = commits.loadState.refresh is LoadState.Loading || commits.loadState.append is LoadState.Loading
                val failed = commits.loadState.refresh is LoadState.Error || commits.loadState.append is LoadState.Error
                if (loading) item { Text("* Loading...", style = DeltaType.body, modifier = Modifier.padding(vertical = 12.dp)) }
                if (failed) item {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("* Couldn't load the changes.", style = DeltaType.body)
                        MenuOption("TRY AGAIN", { commits.retry() }, style = DeltaType.body)
                    }
                }
            }
        }
    }
}

/** "* fix: something" → a dialogue line with the kind of change in yellow. */
@Composable
private fun ChangeLine(commit: Commit, onClick: () -> Unit) {
    val first = commit.info.message.lineSequence().first()
    val kind = Regex("^(\\w+)(\\(.+?\\))?!?:\\s*").find(first)
    val label = when (kind?.groupValues?.get(1)) {
        "feat" -> "NEW"
        "fix" -> "FIX"
        "perf" -> "FAST"
        "docs" -> "DOCS"
        null -> null
        else -> null
    }
    val text = if (kind != null) first.substring(kind.range.last + 1) else first
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            "* " + text.replaceFirstChar { it.uppercase() },
            style = DeltaType.body,
        )
        Text(
            listOfNotNull(label, commit.info.committer.date.toString().take(10), commit.sha.take(7)).joinToString("  ·  "),
            style = DeltaType.small.copy(color = if (label == "NEW") Delta.Yellow else Delta.Gray),
            modifier = Modifier.padding(start = 18.dp, top = 2.dp),
        )
    }
}
