package app.deltacord.manager.ui.widgets.dialog

import app.deltacord.manager.ui.delta.DeltaDialog

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import app.deltacord.manager.R
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.utils.showToast
import org.koin.compose.koinInject

@Composable
fun BatteryOptimizationDialog() {
    val context = LocalContext.current
    val prefs: PreferenceManager = koinInject()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        checkBatteryOptimizations(context, prefs) {
            showDialog = true
        }
    }

    if (showDialog) {
        DeltaDialog(
            lines = listOf(
                "Let the Manager keep going in the background?",
                "Then downloads don't stop when the screen turns off.",
            ),
            options = listOf(
                "ALLOW" to { requestIgnoreBatteryOptimizations(context) },
                "NOT NOW" to {},
            ),
            onDismiss = {
                prefs.hasAskedForBatteryOpt = true
                showDialog = false
            },
            dismissible = false,
        )
    }
}

private fun checkBatteryOptimizations(
    context: Context,
    prefs: PreferenceManager,
    showDialog: () -> Unit
) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName) || prefs.hasAskedForBatteryOpt) {
        return
    }
    showDialog()
}

@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.showToast(R.string.battery_opt_error, short = false)
        Log.e("BatteryOptimization", "Failed to start activity for battery optimization settings", e)
    }
}