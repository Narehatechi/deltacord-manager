package app.deltacord.manager.ui.screen.libraries

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.viewmodel.libraries.LibrariesViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/** The open source libraries the Manager uses, and their licenses. Tap one to open its page. */
class LibrariesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: LibrariesViewModel = getScreenModel()
        val uri = LocalUriHandler.current

        DeltaScreen(title = "Licenses", onBack = { navigator.pop() }, scroll = false) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(viewModel.libraries.libraries, key = { it.uniqueId }) { library ->
                    val link = library.website ?: library.licenses.firstOrNull()?.url
                    MenuOption(
                        text = library.name,
                        onClick = { link?.let { runCatching { uri.openUri(it) } } },
                        style = DeltaType.body,
                        sub = listOfNotNull(
                            library.artifactVersion,
                            library.licenses.joinToString { it.name }.takeIf { it.isNotBlank() },
                        ).joinToString("  ·  "),
                    )
                }
            }
        }
    }
}
