package app.deltacord.manager.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import app.deltacord.manager.ui.components.settings.SettingsItem
import app.deltacord.manager.utils.CustomIcon
import app.deltacord.manager.utils.showToast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.ui.components.settings.SettingsItemChoice
import app.deltacord.manager.ui.components.settings.SettingsSwitch
import app.deltacord.manager.ui.components.settings.SettingsTextField
import app.deltacord.manager.utils.DimenUtils
import org.koin.androidx.compose.get

class CustomizationSettings: Screen {

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Content() {
        val ctx = LocalContext.current
        val prefs: PreferenceManager = get()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        Scaffold(
            topBar = { TitleBar(scrollBehavior) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = DimenUtils.navBarPadding)
            ) {
                SettingsTextField(
                    label = stringResource(R.string.settings_app_name),
                    pref = prefs.appName,
                    onPrefChange = {
                        prefs.appName = it
                    }
                )

                SettingsSwitch(
                    label = stringResource(R.string.settings_app_icon),
                    secondaryLabel = stringResource(R.string.settings_app_icon_description),
                    pref = prefs.patchIcon,
                    onPrefChange = {
                        prefs.patchIcon = it
                    }
                )

                if (prefs.patchIcon) {
                    CustomIconSettings(prefs)
                }

                SettingsItemChoice(
                    label = stringResource(R.string.settings_channel),
                    pref = prefs.channel,
                    labelFactory = {
                        ctx.getString(it.labelRes)
                    },
                    onPrefChange = {
                        prefs.channel = it
                    }
                )
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun TitleBar(
        scrollBehavior: TopAppBarScrollBehavior
    ) {
        val navigator = LocalNavigator.currentOrThrow

        LargeTopAppBar(
            title = {
                Text(stringResource(R.string.settings_customization))
            },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }

}

@Composable
private fun CustomIconSettings(prefs: PreferenceManager) {
    val ctx = LocalContext.current
    var colorText by remember { mutableStateOf(prefs.iconColor) }
    // Bumped whenever the icon file changes so the preview reloads.
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

    SettingsTextField(
        label = stringResource(R.string.settings_icon_color),
        pref = colorText,
        error = parsedColor == null,
        supportingText = stringResource(
            if (parsedColor == null) R.string.settings_icon_color_invalid else R.string.settings_icon_color_hint
        ),
        onPrefChange = {
            colorText = it
            if (CustomIcon.parseColor(it) != null) prefs.iconColor = it.trim()
        }
    )

    val hasCustomIcon = remember(iconVersion) { prefs.customIconFile.exists() }
    val preview = remember(iconVersion) {
        if (hasCustomIcon) BitmapFactory.decodeFile(prefs.customIconFile.path)?.asImageBitmap() else null
    }

    SettingsItem(
        text = { Text(stringResource(R.string.settings_custom_icon)) },
        secondaryText = {
            Text(stringResource(if (hasCustomIcon) R.string.settings_custom_icon_set else R.string.settings_custom_icon_none))
        },
        trailing = {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(parsedColor ?: android.graphics.Color.BLACK))
                )
            }
        }
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp)
    ) {
        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_custom_icon_pick))
        }
        if (hasCustomIcon) {
            OutlinedButton(
                onClick = {
                    prefs.customIconFile.delete()
                    iconVersion++
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.settings_custom_icon_reset))
            }
        }
    }
}
