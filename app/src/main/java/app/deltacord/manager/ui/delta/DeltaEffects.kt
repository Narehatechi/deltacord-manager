package app.deltacord.manager.ui.delta

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Whether the flashy effects run (CONFIG › Flashy effects). Off: still backdrops, no motion. */
val LocalFlashy = staticCompositionLocalOf { true }

/** The dark world's colours. */
object DarkWorld {
    val Grid = Color(0xFF2A1046)
    val GridGlow = Color(0xFF5B2A8C)
    val Deep = Color(0xFF07020D)
    val Fountain = Color(0xFFFFFFFF)
}

/** A clock for effects: seconds since shown, running only when effects are on. */
@Composable
private fun effectTime(): Float {
    val still = LocalStill.current || !LocalFlashy.current
    if (still) return 0f
    val t = rememberInfiniteTransition(label = "time")
    // Loops every 10 minutes, long enough not to notice.
    return t.animateFloat(0f, 600f, infiniteRepeatable(tween(600_000, easing = LinearEasing)), label = "t").value
}

/**
 * The dark world behind everything: a slowly scrolling diamond grid in deep purple, fading into
 * black towards the edges. [strong] draws it brighter (the main menu).
 */
@Composable
fun DarkWorldBackdrop(modifier: Modifier = Modifier, strong: Boolean = false) {
    val t = effectTime()
    Canvas(modifier.fillMaxSize()) {
        drawRect(Delta.Black)
        val cell = 46.dp.toPx()
        val shift = (t * 14.dp.toPx()) % cell
        val grid = if (strong) DarkWorld.Grid.copy(alpha = 0.7f) else DarkWorld.Grid.copy(alpha = 0.45f)
        val stroke = Stroke(1.5.dp.toPx())
        // Diamonds: two sets of diagonal lines.
        var x = -size.height - cell + shift
        while (x < size.width + cell) {
            drawLine(grid, Offset(x, 0f), Offset(x + size.height, size.height), stroke.width)
            drawLine(grid, Offset(x + size.height, 0f), Offset(x, size.height), stroke.width)
            x += cell
        }
        // A soft purple glow in the middle, black at the edges (vignette).
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    DarkWorld.GridGlow.copy(alpha = if (strong) 0.14f else 0.07f),
                    Color.Transparent,
                    Delta.Black.copy(alpha = 0.92f),
                ),
                center = Offset(size.width / 2, size.height * 0.35f),
                radius = size.maxDimension * 0.75f,
            )
        )
    }
}

/**
 * The Dark Fountain: a column of white light pouring up from below, wavering, with motes rising
 * in it. Drawn behind the logo.
 */
@Composable
fun DarkFountain(modifier: Modifier = Modifier) {
    val t = effectTime()
    val motes = remember { List(26) { Random(it * 7919).let { r -> Triple(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f) } } }
    Canvas(modifier) {
        val cx = size.width / 2
        val w = size.width * 0.18f
        // The column: brightest in the middle, wavering edges.
        val path = Path()
        val steps = 40
        for (i in 0..steps) {
            val y = size.height * i / steps
            val wave = sin(y / 37f + t * 2.2f) * w * 0.12f + sin(y / 13f - t * 3.1f) * w * 0.05f
            val half = w * (0.55f + 0.45f * i / steps) + wave
            if (i == 0) path.moveTo(cx - half, y) else path.lineTo(cx - half, y)
        }
        for (i in steps downTo 0) {
            val y = size.height * i / steps
            val wave = sin(y / 41f - t * 2.0f) * w * 0.12f + sin(y / 11f + t * 2.7f) * w * 0.05f
            val half = w * (0.55f + 0.45f * i / steps) + wave
            path.lineTo(cx + half, y)
        }
        path.close()
        drawPath(
            path,
            Brush.horizontalGradient(
                listOf(Color.Transparent, DarkWorld.Fountain.copy(alpha = 0.18f), DarkWorld.Fountain.copy(alpha = 0.55f), DarkWorld.Fountain.copy(alpha = 0.18f), Color.Transparent),
                startX = cx - w * 1.2f, endX = cx + w * 1.2f,
            ),
        )
        // Fade the top out into the dark.
        drawRect(Brush.verticalGradient(listOf(Delta.Black, Color.Transparent), endY = size.height * 0.45f))
        // Motes rising up the column.
        for ((fx, fy, speed) in motes) {
            val y = size.height - ((fy * size.height + t * 60f * speed) % size.height)
            val x = cx + (fx - 0.5f) * w * 1.6f + sin(t * 1.5f + fy * 20f) * 6f
            val a = (0.35f + 0.65f * ((sin(t * 4f + fx * 30f) + 1) / 2)) * ((y / size.height - 0.35f) / 0.65f).coerceIn(0f, 1f)
            val s = (1.5f + speed * 2.5f).dp.toPx()
            drawRect(Color.White.copy(alpha = a.coerceIn(0f, 1f)), Offset(x, y), Size(s, s))
        }
    }
}

/** Little white sparkles twinkling and drifting up the whole screen. */
@Composable
fun Sparkles(modifier: Modifier = Modifier, count: Int = 26) {
    val t = effectTime()
    val stars = remember(count) { List(count) { Random(it * 104729 + 3).let { r -> listOf(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    Canvas(modifier.fillMaxSize()) {
        for ((fx, fy, phase, speed) in stars) {
            val y = (fy * size.height - t * (8f + speed * 18f)).mod(size.height)
            val x = fx * size.width + sin(t * 0.6f + phase * 6f) * 10f
            val twinkle = ((sin(t * (1.5f + speed * 2f) + phase * 2 * PI.toFloat()) + 1) / 2)
            val a = 0.1f + 0.45f * twinkle
            val arm = (2f + speed * 3f).dp.toPx() * (0.6f + 0.4f * twinkle)
            val c = Color.White.copy(alpha = a)
            // A pixel "+" sparkle.
            val p = 1.5.dp.toPx()
            drawRect(c, Offset(x - arm, y - p / 2), Size(arm * 2, p))
            drawRect(c, Offset(x - p / 2, y - arm), Size(p, arm * 2))
        }
    }
}

/**
 * A logo that comes in letter by letter (each dropping into place), then shimmers: a band of light
 * sweeps across it, it glows, and now and then it glitches into red and cyan for a moment.
 */
@Composable
fun FlashyLogo(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val flashy = LocalFlashy.current && !LocalStill.current
    val t = effectTime()
    // The letters' entrance.
    val letters = text.toList()
    val shown = remember(text) { letters.map { Animatable(if (flashy) 0f else 1f) } }
    LaunchedEffect(text, flashy) {
        if (!flashy) return@LaunchedEffect
        delay(250)
        for (a in shown) {
            a.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        }
    }
    // A glitch every ~7 seconds, for a moment.
    val glitch = flashy && (t % 7f) in 0f..0.12f
    val sweep = (t * 0.35f) % 1.6f - 0.3f
    val glow = 0.55f + 0.45f * ((sin(t * 2f) + 1) / 2)
    val shine = Brush.linearGradient(
        colors = listOf(Delta.White, Delta.White, Color(0xFFFFF6B0), Delta.White, Delta.White),
        start = Offset(sweep * 900f - 300f, 0f),
        end = Offset(sweep * 900f + 300f, 200f),
        tileMode = TileMode.Clamp,
    )
    val glowShadow = Shadow(Delta.White.copy(alpha = if (flashy) 0.55f * glow else 0.35f), Offset.Zero, 22f)
    val main = if (flashy) style.merge(TextStyle(brush = shine, shadow = glowShadow)) else style.copy(shadow = glowShadow)
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (glitch) {
            LogoLetters(letters, shown, style.copy(color = Color(0xFFFF2040)), Modifier.graphicsLayer { translationX = -6f })
            LogoLetters(letters, shown, style.copy(color = Color(0xFF20FFF0)), Modifier.graphicsLayer { translationX = 6f })
        }
        LogoLetters(letters, shown, main, Modifier)
    }
}

@Composable
private fun LogoLetters(letters: List<Char>, shown: List<Animatable<Float, *>>, style: TextStyle, modifier: Modifier) {
    Row(modifier) {
        letters.forEachIndexed { i, c ->
            val p = shown[i].value
            Text(
                c.toString(),
                style = style,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(p)
                    .graphicsLayer {
                        translationY = (1 - p) * -40f
                        scaleX = 0.7f + 0.3f * p
                        scaleY = 0.7f + 0.3f * p
                    },
            )
        }
    }
}

/** Fades and slides a menu item in, a little after the one before it ([index]). */
@Composable
fun Modifier.entrance(index: Int): Modifier {
    val flashy = LocalFlashy.current && !LocalStill.current
    val p = remember { Animatable(if (flashy) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!flashy) return@LaunchedEffect
        delay(120L + index * 70L)
        p.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    return this
        .alpha(p.value)
        .graphicsLayer { translationX = (1 - p.value) * 60f }
}

/** A screen's backdrop: the dark world, with sparkles; the main menu also gets the fountain. */
@Composable
fun FlashyBackdrop(strong: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        DarkWorldBackdrop(strong = strong)
        if (LocalFlashy.current) Sparkles(count = if (strong) 20 else 12)
        content()
    }
}

/** A gentle up-and-down bob for the SOUL. */
@Composable
fun bob(): Float {
    val flashy = LocalFlashy.current && !LocalStill.current
    if (!flashy) return 0f
    val t = rememberInfiniteTransition(label = "bob")
    return t.animateFloat(-2f, 2f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y").value
}

/** A glow behind text that's selected. */
fun selectedGlow(color: Color) = Shadow(color.copy(alpha = 0.8f), Offset.Zero, 14f)
