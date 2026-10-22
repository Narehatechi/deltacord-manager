package app.undercord.manager.installer.step.download

import androidx.compose.runtime.Stable
import app.undercord.manager.R
import app.undercord.manager.installer.step.download.base.DownloadStep
import java.io.File

/**
 * Downloads the Undercord XPosed module
 *
 * https://github.com/Narehatechi/undercord-xposed
 */
@Stable
class DownloadModStep(
    workingDir: File
): DownloadStep() {

    override val nameRes = R.string.step_dl_mod

    override val downloadFullUrl: String = "https://github.com/Narehatechi/undercord-xposed/releases/latest/download/app-release.apk"
    override val destination = preferenceManager.moduleLocation
    override val workingCopy = workingDir.resolve("xposed.apk")

}
