package app.deltacord.manager.domain.link

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.deltacord.manager.domain.manager.InstallManager
import app.deltacord.manager.domain.manager.PreferenceManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** What Deltacord reports about itself (sent by Deltacord; see the bundle's managerLink.ts). */
@Serializable
data class LinkStatus(
    val v: Int = 1,
    val requestId: String? = null,
    val error: String? = null,
    val at: Long = 0,
    val bundle: String = "",
    val module: String = "",
    val discord: String = "",
    val safeMode: Boolean = false,
    val checks: List<LinkCheck> = emptyList(),
    val plugins: List<LinkPlugin> = emptyList(),
    val themes: List<LinkTheme> = emptyList(),
    val backups: LinkBackups = LinkBackups(),
)

@Serializable
data class LinkCheck(val group: String, val label: String, val value: String, val status: String, val hint: String? = null)

@Serializable
data class LinkPlugin(val id: String, val name: String, val on: Boolean, val core: Boolean = false, val running: Boolean = false)

@Serializable
data class LinkTheme(val id: String, val name: String, val on: Boolean)

@Serializable
data class LinkBackups(val newest: Long? = null, val count: Int = 0)

/** A report from the module, and when it arrived. */
data class SavedReport(val text: String, val at: Long)

/** A request to Deltacord. */
@Serializable
data class LinkRequest(val id: String, val action: String, val target: String? = null, val value: Boolean? = null)

/**
 * Talks to the installed Deltacord app (no root needed): requests go out as a broadcast only that
 * app receives, carrying a reply token Android ties to the Manager (Deltacord checks it, so other
 * apps can't send it requests). Deltacord answers through that token, which only it ever got, so
 * its answers can be trusted. If Deltacord isn't running, [open] starts it with the request.
 */
class DeltacordLink(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val installManager: InstallManager,
    private val json: Json,
) {
    private val file = context.filesDir.resolve("deltacord-status.json")
    private val _status = MutableStateFlow(load())
    val status: StateFlow<LinkStatus?> = _status.asStateFlow()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<LinkStatus>>()

    private val reportFile = context.filesDir.resolve("deltacord-report.txt")
    private val _report = MutableStateFlow<SavedReport?>(
        reportFile.takeIf { it.exists() }?.let { SavedReport(it.readText(), it.lastModified()) }
    )
    /** The newest report from the Deltacord module (works even when Deltacord is stuck). */
    val report: StateFlow<SavedReport?> = _report.asStateFlow()

    /** A report from the module (from [LinkReceiver]). */
    fun onReport(text: String) {
        val saved = SavedReport(text, System.currentTimeMillis())
        _report.value = saved
        runCatching { reportFile.writeText(text) }
    }

    /**
     * Asks the module for a report and waits for it (null: no answer, usually because Deltacord
     * isn't running; then [open] with "report" gets one once it has started).
     */
    suspend fun requestReport(timeoutMs: Long = 8000): SavedReport? {
        val before = _report.value?.at ?: 0
        request("report", timeoutMs = 1)
        return withTimeoutOrNull(timeoutMs) {
            report.first { it != null && it.at > before }
        }
    }

    private fun load(): LinkStatus? = runCatching { json.decodeFromString<LinkStatus>(file.readText()) }.getOrNull()

    /** A status from Deltacord (from [LinkReceiver]). */
    fun onStatus(text: String) {
        val status = runCatching { json.decodeFromString<LinkStatus>(text) }.getOrNull() ?: return
        // Keep the last full report when an answer only carries an error.
        if (status.error == null || _status.value == null) {
            _status.value = status
            runCatching { file.writeText(text) }
        }
        status.requestId?.let { waiting.remove(it)?.complete(status) }
    }

    private fun replyToken(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LinkReceiver::class.java).setAction(ACTION_STATUS),
        // Mutable: Deltacord fills in its answer.
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val installedPackage: String? get() = installManager.current?.packageName

    /**
     * Sends a request to a running Deltacord and waits for its answer (null: no answer, usually
     * because Deltacord isn't running).
     */
    suspend fun request(action: String, target: String? = null, value: Boolean? = null, timeoutMs: Long = 4000): LinkStatus? {
        val pkg = installedPackage ?: return null
        val request = LinkRequest(UUID.randomUUID().toString(), action, target, value)
        val answer = CompletableDeferred<LinkStatus>()
        waiting[request.id] = answer
        context.sendBroadcast(
            Intent(ACTION_REQUEST)
                .setPackage(pkg)
                .putExtra(EXTRA_REQUEST, json.encodeToString(request))
                .putExtra(EXTRA_REPLY, replyToken())
        )
        return withTimeoutOrNull(timeoutMs) { answer.await() }.also { waiting.remove(request.id) }
    }

    /** Opens Deltacord, handing it this request to carry out when it starts. */
    fun open(action: String = "status", target: String? = null, value: Boolean? = null) {
        val pkg = installedPackage ?: return
        val request = LinkRequest(UUID.randomUUID().toString(), action, target, value)
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        context.startActivity(
            launch
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REQUEST, json.encodeToString(request))
                .putExtra(EXTRA_REPLY, replyToken())
        )
    }

    companion object {
        const val ACTION_REQUEST = "app.deltacord.REQUEST"
        const val ACTION_STATUS = "app.deltacord.manager.STATUS"
        const val EXTRA_REQUEST = "deltacord.request"
        const val EXTRA_REPLY = "deltacord.reply"
        const val EXTRA_STATUS = "deltacord.status"
        const val EXTRA_REPORT = "deltacord.report"
    }
}

/** Receives Deltacord's answers (only through the reply token, so it isn't exported). */
class LinkReceiver : BroadcastReceiver(), KoinComponent {
    private val link: DeltacordLink by inject()

    override fun onReceive(context: Context, intent: Intent) {
        intent.getStringExtra(DeltacordLink.EXTRA_STATUS)?.let(link::onStatus)
        intent.getStringExtra(DeltacordLink.EXTRA_REPORT)?.let(link::onReport)
    }
}
