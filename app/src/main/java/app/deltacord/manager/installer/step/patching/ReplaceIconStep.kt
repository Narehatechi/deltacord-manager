package app.deltacord.manager.installer.step.patching

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.github.diamondminer88.zip.ZipWriter
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.installer.step.Step
import app.deltacord.manager.installer.step.StepGroup
import app.deltacord.manager.installer.step.StepRunner
import app.deltacord.manager.installer.step.download.DownloadBaseStep
import app.deltacord.manager.installer.utils.ArscUtil
import app.deltacord.manager.installer.utils.ArscUtil.addColorResource
import app.deltacord.manager.installer.utils.ArscUtil.addResource
import app.deltacord.manager.utils.CustomIcon
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceIdentifier
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import app.deltacord.manager.installer.utils.ArscUtil.getMainArscChunk
import app.deltacord.manager.installer.utils.ArscUtil.getPackageChunk
import app.deltacord.manager.installer.utils.ArscUtil.getResourceFileName
import app.deltacord.manager.installer.utils.AxmlUtil
import app.deltacord.manager.utils.DiscordVersion
import org.koin.core.component.inject

/**
 * Replaces the app icon: the chosen background colour, and in front of it Deltacord's logo,
 * or the user's own picture if they picked one in Customization settings.
 */
class ReplaceIconStep : Step() {

    private val preferences: PreferenceManager by inject()

    val context: Context by inject()

    override val group = StepGroup.PATCHING
    override val nameRes = R.string.step_change_icon

    override suspend fun run(runner: StepRunner) {
        val baseApk = runner.getCompletedStep<DownloadBaseStep>().workingCopy

        runner.logger.i("Reading resources.arsc")
        val arsc = ArscUtil.readArsc(baseApk)
        
        val iconRscIds = AxmlUtil.readManifestIconInfo(baseApk)
        val squareIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.squareIcon, "anydpi-v26")
        val roundIconFile = arsc.getMainArscChunk().getResourceFileName(iconRscIds.roundIcon, "anydpi-v26")

        runner.logger.i("Patching icon assets (squareIcon=$squareIconFile, roundIcon=$roundIconFile)")

        val colorArgb = CustomIcon.parseColor(preferences.iconColor) ?: BuildConfig.MODDED_APP_ICON
        val backgroundColor = arsc.getPackageChunk().addColorResource("brand", Color(colorArgb))

        // Your own picture, or else Deltacord's logo.
        val customIcon = preferences.customIconFile.takeIf { it.exists() }
        val iconBytes = customIcon?.readBytes()
            ?: runCatching { context.resources.openRawResource(R.raw.deltacord_icon_foreground).use { it.readBytes() } }
                .onFailure { runner.logger.e("Couldn't read Deltacord's logo; keeping Discord's", it) }
                .getOrNull()
        var foregroundIcon: BinaryResourceIdentifier? = null
        if (iconBytes != null) {
            runner.logger.i("Adding ${if (customIcon != null) "your picture" else "Deltacord's logo"} as the icon ($CUSTOM_ICON_PATH)")
            val pathIdx = arsc.getMainArscChunk().stringPool.addString(CUSTOM_ICON_PATH)
            foregroundIcon = arsc.getPackageChunk().addResource(
                typeName = "mipmap",
                resourceName = "deltacord_custom_icon",
                configurations = { it.toString().endsWith("dpi") }, // every density bucket except anydpi-v26
                valueType = BinaryResourceValue.Type.STRING,
                valueData = pathIdx,
            )
        }

        val postfix = when (preferences.channel) {
            DiscordVersion.Type.BETA -> "beta"
            DiscordVersion.Type.ALPHA -> "canary"
            else -> null
        }
        
        for (rscFile in setOf(squareIconFile, roundIconFile)) { // setOf to not possibly patch same file twice
            val referencePath = if (postfix == null) rscFile else {
                rscFile.replace("_$postfix.xml", ".xml")
            }

            runner.logger.i("Patching adaptive icon ($rscFile <- $referencePath)")

            AxmlUtil.patchAdaptiveIcon(
                apk = baseApk,
                resourcePath = rscFile,
                referencePath = referencePath,
                backgroundColor = backgroundColor,
                foregroundIcon = foregroundIcon,
            )
        }

        runner.logger.i("Writing and compiling resources.arsc")
        ZipWriter(baseApk, /* append = */ true).use {
            if (iconBytes != null) it.writeEntry(CUSTOM_ICON_PATH, iconBytes)
            it.deleteEntry("resources.arsc")
            it.writeEntry("resources.arsc", arsc.toByteArray())
        }
    }

    private companion object {
        const val CUSTOM_ICON_PATH = "res/deltacord_custom_icon.png"
    }
}
