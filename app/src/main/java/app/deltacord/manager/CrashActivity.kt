package app.deltacord.manager

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Shown after the installer crashes. Runs in its own process with plain Android views
 * (no Compose, no Koin), so it still works when the crash came from those.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = crashFile(this).takeIf { it.exists() }?.readText() ?: "No crash report found."

        val text = TextView(this).apply {
            this.text = report
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(32, 32, 32, 32)
        }
        val share = Button(this).apply {
            this.text = "Share crash report"
            setOnClickListener {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Deltacord Manager crash")
                    putExtra(Intent.EXTRA_TEXT, report)
                }, "Share crash report"))
            }
        }
        val restart = Button(this).apply {
            this.text = "Restart installer"
            setOnClickListener {
                crashFile(this@CrashActivity).delete()
                packageManager.getLaunchIntentForPackage(packageName)?.let {
                    startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }
                finish()
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            addView(TextView(context).apply {
                this.text = "Deltacord Manager crashed"
                textSize = 20f
                setPadding(8, 8, 8, 16)
            })
            addView(share, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(restart, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(ScrollView(context).apply { addView(text) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        })
    }

    companion object {
        fun crashFile(activity: android.content.Context) = File(activity.filesDir, "last_crash.txt")

        /** Saves crashes to a file and opens [CrashActivity] instead of silently closing. */
        fun install(app: android.app.Application) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                try {
                    val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
                    crashFile(app).writeText(
                        "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                            "Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n" +
                            "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                            "Thread: ${thread.name}\n\n$trace"
                    )
                    app.startActivity(
                        Intent(app, CrashActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(10)
                } catch (_: Throwable) {
                    previous?.uncaughtException(thread, error)
                }
            }
        }
    }
}
