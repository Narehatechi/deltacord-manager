package app.deltacord.manager.installer.step.download

import androidx.compose.runtime.Stable
import app.deltacord.manager.R
import app.deltacord.manager.installer.step.download.base.DownloadStep
import java.io.File

/**
 * Downloads the Deltacord XPosed module
 *
 * https://github.com/Narehatechi/deltacord-xposed
 */
@Stable
class DownloadModStep(
    workingDir: File
): DownloadStep() {

    override val nameRes = R.string.step_dl_mod

    override val downloadFullUrl: String = "https://github.com/Narehatechi/deltacord-xposed/releases/latest/download/app-release.apk"
    override val destination = preferenceManager.moduleLocation
    override val workingCopy = workingDir.resolve("xposed.apk")

}
