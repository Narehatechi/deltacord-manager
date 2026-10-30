package app.deltacord.manager.ui.delta

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import app.deltacord.manager.R
import app.deltacord.manager.ui.theme.DeltaruneColors
import app.deltacord.manager.ui.theme.Determination
import app.deltacord.manager.ui.theme.Typography

/** Deltacord Manager's colours: the game's dark world. */
object Delta {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    /** Whatever is selected or being pressed. */
    val Yellow = Color(0xFFFFFF00)
    /** The SOUL, danger, failures. */
    val Red = Color(0xFFFF0000)
    /** Healing, success. */
    val Green = Color(0xFF00FF00)
    /** Secondary text and disabled things. */
    val Gray = Color(0xFF8C8C8C)
    val DarkGray = Color(0xFF3A3A3A)
    /** The battle box behind a dialogue. */
    val Box = Color(0xFF000000)
}

/** Text styles, all in Determination. */
object DeltaType {
    val logo = TextStyle(fontFamily = Determination, fontSize = 40.sp, letterSpacing = 4.sp, color = Delta.White)
    val title = TextStyle(fontFamily = Determination, fontSize = 26.sp, letterSpacing = 2.sp, color = Delta.White)
    val menu = TextStyle(fontFamily = Determination, fontSize = 24.sp, letterSpacing = 1.sp, color = Delta.White)
    val body = TextStyle(fontFamily = Determination, fontSize = 19.sp, lineHeight = 26.sp, color = Delta.White)
    val small = TextStyle(fontFamily = Determination, fontSize = 15.sp, lineHeight = 20.sp, color = Delta.Gray)
}

/** Plays the text blip (once per letter while dialogue types, and on taps). */
fun interface Blipper {
    fun blip(kind: BlipKind)
}

enum class BlipKind { TEXT, SELECT, SAVE }

val LocalBlip = staticCompositionLocalOf<Blipper> { Blipper { } }

/** Drawing a still picture (previews, screenshots): text is shown whole instead of typing. */
val LocalStill = staticCompositionLocalOf { false }

/** The sounds, loaded once. Quiet; nothing plays in previews. */
private object Sounds {
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<BlipKind, Int>()
    private var last = 0L

    fun init(context: Context) {
        if (pool != null) return
        // Sounds are a nicety: never let them break a screen.
        pool = runCatching { build(context) }.getOrNull()
    }

    private fun build(context: Context): SoundPool =
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
            .also { p ->
                ids[BlipKind.TEXT] = p.load(context, R.raw.blip, 1)
                ids[BlipKind.SELECT] = ids[BlipKind.TEXT]!!
                ids[BlipKind.SAVE] = p.load(context, R.raw.save, 1)
            }

    fun play(kind: BlipKind) {
        val now = System.currentTimeMillis()
        // A blip at most every 30ms, like the game's text voice.
        if (kind == BlipKind.TEXT && now - last < 30) return
        last = now
        val id = ids[kind] ?: return
        val rate = if (kind == BlipKind.SELECT) 1.25f else 1f
        pool?.play(id, 0.6f, 0.6f, 1, 0, rate)
    }
}

/**
 * The Manager's look: black, white boxes, Determination, the SOUL. [sounds] says whether blips
 * play right now (they never play in previews).
 */
@Composable
fun DeltaTheme(sounds: () -> Boolean = { true }, still: Boolean = false, flashy: Boolean = true, content: @Composable () -> Unit) {
    val quiet = still || LocalInspectionMode.current
    val context = LocalContext.current
    val blipper = if (quiet) Blipper { } else {
        Sounds.init(context.applicationContext)
        Blipper { if (sounds()) runCatching { Sounds.play(it) } }
    }
    MaterialTheme(colorScheme = DeltaruneColors, typography = Typography) {
        CompositionLocalProvider(LocalBlip provides blipper, LocalStill provides quiet, LocalFlashy provides flashy, content = content)
    }
}
