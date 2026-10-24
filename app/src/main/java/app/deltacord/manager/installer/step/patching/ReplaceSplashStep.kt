package app.deltacord.manager.installer.step.patching

import app.deltacord.manager.R
import app.deltacord.manager.installer.step.Step
import app.deltacord.manager.installer.step.StepGroup
import app.deltacord.manager.installer.step.StepRunner
import app.deltacord.manager.installer.step.download.DownloadBaseStep
import app.deltacord.manager.installer.utils.ArscUtil
import app.deltacord.manager.installer.utils.ArscUtil.getPackageChunk
import com.github.diamondminer88.zip.ZipWriter
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import com.google.devrel.gmscore.tools.apk.arsc.PackageChunk

/**
 * Replaces Android's launch screen (Discord's logo on its background, shown before any of Deltacord's
 * code runs) with plain black, so opening Deltacord goes straight into Deltacord's own startup
 * animation, which is also black.
 *
 * Only the launch themes are changed: styles that set a splash icon or splash background. In those,
 * the icon becomes transparent and the backgrounds black (existing values only, nothing added).
 * If anything goes wrong, Discord's launch screen is kept.
 */
class ReplaceSplashStep : Step() {

    override val group = StepGroup.PATCHING
    override val nameRes = R.string.step_replace_splash

    override suspend fun run(runner: StepRunner) {
        val baseApk = runner.getCompletedStep<DownloadBaseStep>().workingCopy

        try {
            val arsc = ArscUtil.readArsc(baseApk)
            val pkg = arsc.getPackageChunk()

            // Discord uses AndroidX's splash screen, whose attributes live in Discord's own package too.
            val appAttrs = attrIds(pkg)
            val iconAttrs = setOf(
                android.R.attr.windowSplashScreenAnimatedIcon,
                android.R.attr.windowSplashScreenBrandingImage,
            ) + listOfNotNull(appAttrs["windowSplashScreenAnimatedIcon"])
            val backgroundAttrs = setOf(
                android.R.attr.windowSplashScreenBackground,
                android.R.attr.windowSplashScreenIconBackgroundColor,
            ) + listOfNotNull(appAttrs["windowSplashScreenBackground"], appAttrs["windowSplashScreenIconBackgroundColor"])
            val splashAttrs = iconAttrs + backgroundAttrs

            var changed = 0
            for (typeChunk in pkg.getTypeChunks("style")) {
                for (index in 0 until typeChunk.totalEntryCount) {
                    val entry = runCatching { typeChunk.getEntry(index) }.getOrNull() ?: continue
                    if (!entry.isComplex) continue

                    val values = entry.values()
                    val isLaunchTheme = (0 until values.size).any { values.get(it).key() in splashAttrs }
                    if (!isLaunchTheme) continue

                    for (i in 0 until values.size) {
                        val value = values.get(i)
                        when (value.key()) {
                            in iconAttrs -> value.setValue(BinaryResourceValue.Type.REFERENCE, android.R.color.transparent)
                            in backgroundAttrs, android.R.attr.windowBackground ->
                                value.setValue(BinaryResourceValue.Type.INT_COLOR_ARGB8, BLACK)
                        }
                    }
                    // Entries are read fresh from the table each time; store the edited one back.
                    typeChunk.overrideEntry(index, entry)
                    runner.logger.i("Launch screen blacked out in style ${entry.key()} (${typeChunk.configuration})")
                    changed++
                }
            }

            if (changed == 0) {
                runner.logger.i("No launch screen styles found; keeping Discord's launch screen")
                return
            }

            ZipWriter(baseApk, /* append = */ true).use {
                it.deleteEntry("resources.arsc")
                it.writeEntry("resources.arsc", arsc.toByteArray())
            }
        } catch (e: Throwable) {
            runner.logger.e("Couldn't replace the launch screen; keeping Discord's", e)
        }
    }

    /** Resource ids of Discord's own `attr` resources, by name. */
    private fun attrIds(pkg: PackageChunk): Map<String, Int> {
        val ids = HashMap<String, Int>()
        for (typeChunk in pkg.getTypeChunks("attr")) {
            for (index in 0 until typeChunk.totalEntryCount) {
                val entry = runCatching { typeChunk.getEntry(index) }.getOrNull() ?: continue
                val name = runCatching { entry.key() }.getOrNull() ?: continue
                ids.putIfAbsent(name, (pkg.id shl 24) or (typeChunk.id shl 16) or index)
            }
        }
        return ids
    }

    private companion object {
        const val BLACK = 0xFF000000.toInt()
    }
}
