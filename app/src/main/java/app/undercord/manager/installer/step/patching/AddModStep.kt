package app.undercord.manager.installer.step.patching

import app.undercord.manager.BuildConfig
import app.undercord.manager.R
import app.undercord.manager.installer.step.Step
import app.undercord.manager.installer.step.StepGroup
import app.undercord.manager.installer.step.StepRunner
import app.undercord.manager.installer.step.download.DownloadModStep
import java.io.File

/**
 * Uses LSPatch to inject the Undercord XPosed module into Discord
 *
 * @param signedDir The signed apks to patch
 * @param lspatchedDir Output directory for LSPatch
 */
class AddModStep(
    private val signedDir: File,
    private val lspatchedDir: File
) : Step() {

    override val group = StepGroup.PATCHING
    override val nameRes = R.string.step_add_mod

    override suspend fun run(runner: StepRunner) {
        val mod = runner.getCompletedStep<DownloadModStep>().workingCopy

        runner.logger.i("Adding ${BuildConfig.MOD_NAME}Xposed module with LSPatch")
        val files = signedDir.listFiles()
            ?.takeIf { it.isNotEmpty() }
            ?: throw Error("Missing APKs from signing step")

        app.undercord.manager.installer.util.Patcher.patch(
            runner.logger,
            outputDir = lspatchedDir,
            apkPaths = files.map { it.absolutePath },
            embeddedModules = listOf(mod.absolutePath)
        )
    }

}