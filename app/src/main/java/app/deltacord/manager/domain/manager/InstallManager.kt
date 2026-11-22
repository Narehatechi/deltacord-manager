package app.deltacord.manager.domain.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.deltacord.manager.BuildConfig
import app.deltacord.manager.installer.session.InstallService

class InstallManager(
    private val context: Context,
    private val prefs: PreferenceManager,
) {

    var current by mutableStateOf<PackageInfo?>(null)

    /** The module inside the installed Deltacord (its release number, like 1941), read from the app itself. */
    var moduleVersion by mutableStateOf<String?>(null)

    init {
        getInstalled()
    }

    fun getInstalled() {
        current = try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    context.packageManager.getPackageInfo(
                        prefs.packageName.ifBlank { BuildConfig.MODDED_APP_PACKAGE_NAME },
                        PackageManager.PackageInfoFlags.of(
                            0L
                        )
                    )
                }

                else -> {
                    context.packageManager.getPackageInfo(
                        prefs.packageName.ifBlank { BuildConfig.MODDED_APP_PACKAGE_NAME },
                        0
                    )
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        readModuleVersion()
    }

    /**
     * The installed Deltacord carries the module inside it (LSPatch keeps it in
     * assets/lspatch/modules/). Reading its version from there means the Manager always knows
     * which module you have, even for installs made before it started keeping track.
     */
    private fun readModuleVersion() {
        val app = current?.applicationInfo ?: run { moduleVersion = null; return }
        Thread {
            moduleVersion = try {
                java.util.zip.ZipFile(app.sourceDir).use { zip ->
                    val entry = zip.entries().asSequence().firstOrNull {
                        it.name.startsWith("assets/lspatch/modules/") && it.name.endsWith(".apk") && "deltacord" in it.name
                    } ?: return@use null
                    val copy = java.io.File(context.cacheDir, "installed-module.apk")
                    zip.getInputStream(entry).use { input -> copy.outputStream().use { input.copyTo(it) } }
                    @Suppress("DEPRECATION")
                    val info = context.packageManager.getPackageArchiveInfo(copy.absolutePath, 0)
                    copy.delete()
                    info?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toString() else @Suppress("DEPRECATION") it.versionCode.toString() }
                }
            } catch (e: Throwable) {
                null
            }
        }.start()
    }

    fun uninstall() {
        current?.let {
            val callbackIntent = Intent(context, InstallService::class.java).apply {
                action = "deltacord.actions.ACTION_UNINSTALL"
            }

            @SuppressLint("UnspecifiedImmutableFlag")
            val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getService(context, 0, callbackIntent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getService(context, 0, callbackIntent, 0)
            }

            context.packageManager.packageInstaller.uninstall(
                it.packageName,
                contentIntent.intentSender
            )
        }
    }
}