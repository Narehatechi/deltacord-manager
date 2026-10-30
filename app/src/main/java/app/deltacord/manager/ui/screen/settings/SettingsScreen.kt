package app.deltacord.manager.ui.screen.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.InstallManager
import app.deltacord.manager.domain.manager.InstallMethod
import app.deltacord.manager.domain.manager.Mirror
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.domain.manager.UpdateCheckerDuration
import app.deltacord.manager.ui.delta.CycleOption
import app.deltacord.manager.ui.delta.Delta
import app.deltacord.manager.ui.delta.DeltaDialog
import app.deltacord.manager.ui.delta.DeltaScreen
import app.deltacord.manager.ui.delta.DeltaTextField
import app.deltacord.manager.ui.delta.DeltaType
import app.deltacord.manager.ui.delta.MenuOption
import app.deltacord.manager.ui.delta.SectionLabel
import app.deltacord.manager.ui.delta.ToggleOption
import app.deltacord.manager.ui.viewmodel.settings.AdvancedSettingsViewModel
import app.deltacord.manager.utils.CustomIcon
import app.deltacord.manager.utils.DiscordVersion
import app.deltacord.manager.utils.showToast
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.androidx.compose.get
import java.io.File

/** CONFIG: every setting on one page, in the game's menu style. */
class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val ctx = LocalContext.current
        val prefs: PreferenceManager = get()
        val installManager: InstallManager = get()
        val viewModel: AdvancedSettingsViewModel = getScreenModel()

        // (Preferences are Compose state, so the page follows them by itself.)
        val changed = {}

        var confirmClear by remember { mutableStateOf(false) }
        if (confirmClear) {
            DeltaDialog(
                lines = listOf("Delete the downloaded Discord files?", "The next install downloads them again."),
                options = listOf("CLEAR" to { viewModel.clearCache() }, "KEEP" to {}),
                onDismiss = { confirmClear = false },
            )
        }

        DeltaScreen(title = "Config", onBack = { navigator.pop() }) {
            SectionLabel("Manager")
            ToggleOption(
                "Sound",
                prefs.sounds,
                { prefs.sounds = it },
                sub = "Text blips and menu sounds",
            )
            ToggleOption(
                "Flashy effects",
                prefs.flashy,
                { prefs.flashy = it },
                sub = "The moving dark world, sparkles and fountain. Turn off to save battery",
            )

            SectionLabel("Deltacord app")
            var name by remember { mutableStateOf(prefs.appName) }
            DeltaTextField(
                label = "Name",
                value = name,
                onChange = { name = it; if (it.isNotBlank()) prefs.appName = it.trim() },
                hint = "What it's called on your home screen (applies on the next install)",
                error = name.isBlank(),
            )
            ToggleOption(
                "Deltacord icon",
                prefs.patchIcon,
                { prefs.patchIcon = it; changed() },
                sub = "Use Deltacord's icon instead of Discord's",
            )
            if (prefs.patchIcon) IconOptions(prefs)

            SectionLabel("Discord")
            CycleOption(
                "Discord version",
                prefs.channel,
                DiscordVersion.Type.entries.toList(),
                { ctx.getString(it.labelRes) },
                { prefs.channel = it; changed() },
                sub = "Stable is best for most people",
            )
            CycleOption(
                "Check for updates",
                prefs.updateDuration,
                UpdateCheckerDuration.entries.toList(),
                { ctx.getString(it.labelRes) },
                { prefs.updateDuration = it; viewModel.updateCheckerDuration(it); changed() },
            )
            CycleOption(
                "Download from",
                prefs.mirror,
                Mirror.entries.toList(),
                { it.baseUrl.toUri().authority ?: it.baseUrl },
                { prefs.mirror = it; changed() },
                sub = "Try another if downloads fail",
            )
            CycleOption(
                "Install with",
                prefs.installMethod,
                InstallMethod.entries.toList(),
                { if (it == InstallMethod.SHIZUKU) "Shizuku" else "Android" },
                { viewModel.setInstallMethod(it); changed() },
                sub = "Shizuku installs without asking each time",
            )
            ToggleOption(
                "Tidy old downloads",
                prefs.autoClearCache,
                { prefs.autoClearCache = it; changed() },
                sub = "Delete old Discord files when a new version comes out",
            )
            MenuOption("CLEAR DOWNLOADS", { confirmClear = true }, style = DeltaType.body)

            if (prefs.isDeveloper) {
                SectionLabel("Developer")
                DeveloperOptions(prefs, installManager, changed)
            }
        }
    }
}

/** The icon's background colour and your own picture. */
@Composable
private fun IconOptions(prefs: PreferenceManager) {
    val ctx = LocalContext.current
    var colorText by remember { mutableStateOf(prefs.iconColor) }
    var iconVersion by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            CustomIcon.save(ctx, uri, prefs.customIconFile)
            iconVersion++
        } catch (_: Throwable) {
            ctx.showToast(R.string.settings_custom_icon_failed)
        }
    }
    val parsedColor = CustomIcon.parseColor(colorText)

    DeltaTextField(
        label = "Icon colour",
        value = colorText,
        onChange = {
            colorText = it
            if (CustomIcon.parseColor(it) != null) prefs.iconColor = it.trim()
        },
        hint = if (parsedColor == null) "Use a colour like #000000" else "The colour behind the icon",
        error = parsedColor == null,
    )

    val hasCustomIcon = remember(iconVersion) { prefs.customIconFile.exists() }
    val preview = remember(iconVersion) {
        if (hasCustomIcon) BitmapFactory.decodeFile(prefs.customIconFile.path)?.asImageBitmap() else null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MenuOption(
            if (hasCustomIcon) "CHANGE PICTURE" else "USE MY PICTURE",
            { picker.launch("image/*") },
            style = DeltaType.body,
            sub = if (hasCustomIcon) "Your picture (applies on the next install)" else "Your own picture as the icon",
            modifier = Modifier.weight(1f),
        )
        if (preview != null) {
            Spacer(Modifier.width(12.dp))
            Image(
                bitmap = preview,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(parsedColor ?: android.graphics.Color.BLACK))
                    .border(2.dp, Delta.White, RectangleShape),
            )
        }
    }
    if (hasCustomIcon) {
        MenuOption("REMOVE PICTURE", { prefs.customIconFile.delete(); iconVersion++ }, style = DeltaType.body)
    }
}

@Composable
private fun DeveloperOptions(prefs: PreferenceManager, installManager: InstallManager, changed: () -> Unit) {
    var packageName by remember { mutableStateOf(prefs.packageName) }
    DeltaTextField(
        label = "Package name",
        value = packageName,
        onChange = {
            packageName = it
            prefs.packageName = it
            installManager.getInstalled()
        },
    )

    var version by remember { mutableStateOf(prefs.discordVersion) }
    val versionError = version.isNotBlank() && DiscordVersion.fromVersionCode(version) == null
    DeltaTextField(
        label = "Discord version code",
        value = version,
        onChange = {
            version = it
            if (it.isBlank() || DiscordVersion.fromVersionCode(it) != null) prefs.discordVersion = it
        },
        hint = when {
            versionError -> "That isn't a Discord version code"
            version.isNotBlank() -> DiscordVersion.fromVersionCode(version).toString()
            else -> "Empty: the newest"
        },
        error = versionError,
    )

    ToggleOption("Debuggable", prefs.debuggable, { prefs.debuggable = it; changed() }, sub = "Lets you debug the installed app")
    ToggleOption("Allow downgrades", prefs.allowDowngrade, { prefs.allowDowngrade = it; changed() }, sub = "Install an older Discord over a newer one")

    var moduleLocation by remember { mutableStateOf(prefs.moduleLocation.absolutePath) }
    DeltaTextField(
        label = "Module file",
        value = moduleLocation,
        onChange = {
            moduleLocation = it
            prefs.moduleLocation = File(it)
        },
        hint = "Where the Deltacord module APK is kept",
    )
    MenuOption(
        "RESET MODULE FILE",
        {
            prefs.moduleLocation = prefs.DEFAULT_MODULE_LOCATION
            moduleLocation = prefs.DEFAULT_MODULE_LOCATION.absolutePath
        },
        style = DeltaType.body,
    )
    Text(
        "* Only you can see this section.",
        style = DeltaType.small,
        modifier = Modifier.padding(top = 8.dp),
    )
}
