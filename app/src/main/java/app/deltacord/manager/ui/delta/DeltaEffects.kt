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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Whether the flashy effects run (CONFIG › Flashy effects). Off: still backdrops, no motion. */
val LocalFlashy = staticCompositionLocalOf { true }

/** A fixed moment to draw the effects at (screenshots); null: they run. */
val LocalEffectTime = staticCompositionLocalOf<Float?> { null }

/** The dark world's colours. */
object DarkWorld {
    val Grid = Color(0xFF3A1462)
    val Deep = Color(0xFF07020D)
    /** The light that drifts over the dark world, cycling slowly through these. */
    val Lights = listOf(Color(0xFF7A2CFF), Color(0xFFFF2E9A), Color(0xFF14E0FF), Color(0xFF6A3BFF))
    val Sparkle = listOf(Color.White, Color(0xFFFFF27A), Color(0xFF7AF4FF), Color(0xFFFF8AD8))
}

/** A clock for effects, in seconds: running only when effects are on. */
@Composable
private fun effectTime(): Float {
    LocalEffectTime.current?.let { return it }
    val still = LocalStill.current || !LocalFlashy.current
    if (still) return 0f
    val t = rememberInfiniteTransition(label = "time")
    // Loops every 10 minutes, long enough not to notice.
    return t.animateFloat(0f, 600f, infiniteRepeatable(tween(600_000, easing = LinearEasing)), label = "t").value
}

/** Blends smoothly around a list of colours as [x] goes 0 → 1 (and wraps). */
private fun cycle(colors: List<Color>, x: Float): Color {
    val f = ((x % 1f) + 1f) % 1f * colors.size
    val i = floor(f).toInt()
    val a = colors[i % colors.size]
    val b = colors[(i + 1) % colors.size]
    val k = f - i
    return Color(a.red + (b.red - a.red) * k, a.green + (b.green - a.green) * k, a.blue + (b.blue - a.blue) * k, 1f)
}

/** A rainbow: hue [h] (0..1). */
private fun hue(h: Float, s: Float = 0.75f, v: Float = 1f): Color = Color.hsv(((h % 1f) + 1f) % 1f * 360f, s, v)

/**
 * The dark world behind everything: a scrolling diamond grid, coloured light drifting over it and
 * slowly changing colour, fading into black at the edges. [strong]: brighter (the main menu).
 */
@Composable
fun DarkWorldBackdrop(modifier: Modifier = Modifier, strong: Boolean = false) {
    val t = effectTime()
    Canvas(modifier.fillMaxSize()) {
        drawRect(Delta.Black)
        // Two coloured lights wandering slowly, the colours cycling.
        val a = cycle(DarkWorld.Lights, t * 0.03f)
        val b = cycle(DarkWorld.Lights, t * 0.03f + 0.5f)
        val power = if (strong) 0.32f else 0.18f
        drawRect(Brush.radialGradient(listOf(a.copy(alpha = power), Color.Transparent),
            center = Offset(size.width * (0.3f + 0.2f * sin(t * 0.21f)), size.height * (0.25f + 0.1f * cos(t * 0.17f))),
            radius = size.maxDimension * 0.6f))
        drawRect(Brush.radialGradient(listOf(b.copy(alpha = power * 0.8f), Color.Transparent),
            center = Offset(size.width * (0.75f + 0.15f * cos(t * 0.19f)), size.height * (0.7f + 0.12f * sin(t * 0.23f))),
            radius = size.maxDimension * 0.55f))

        // The grid, glowing brighter where a pulse passes down it.
        val cell = 46.dp.toPx()
        val shift = (t * 16.dp.toPx()) % cell
        val pulseY = (t * 0.25f % 1.4f - 0.2f) * size.height
        val w = 1.5.dp.toPx()
        var x = -size.height - cell + shift
        while (x < size.width + cell) {
            val grid = DarkWorld.Grid.copy(alpha = if (strong) 0.75f else 0.5f)
            drawLine(grid, Offset(x, 0f), Offset(x + size.height, size.height), w)
            drawLine(grid, Offset(x + size.height, 0f), Offset(x, size.height), w)
            x += cell
        }
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, a.copy(alpha = if (strong) 0.16f else 0.08f), Color.Transparent),
                startY = pulseY - 120f, endY = pulseY + 120f,
            )
        )
        // Black edges.
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Transparent, Delta.Black.copy(alpha = 0.95f)),
                center = Offset(size.width / 2, size.height * 0.4f),
                radius = size.maxDimension * 0.78f,
            )
        )
    }
}

/**
 * The Dark Fountain: a pulsing column of light pouring upward, edged with shifting colour, with
 * motes rising in it and little SOULs circling it.
 */
@Composable
fun DarkFountain(modifier: Modifier = Modifier) {
    val t = effectTime()
    val motes = remember { List(36) { Random(it * 7919).let { r -> Triple(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 0.8f) } } }
    Canvas(modifier) {
        val cx = size.width / 2
        val pulse = 1f + 0.12f * sin(t * 2.4f)
        val w = size.width * 0.17f * pulse
        val edge = hue(t * 0.08f)
        fun column(widthScale: Float, color: Color, alpha: Float) {
            val path = Path()
            val steps = 40
            for (i in 0..steps) {
                val y = size.height * i / steps
                val wave = sin(y / 37f + t * 2.6f) * w * 0.14f + sin(y / 13f - t * 3.4f) * w * 0.06f
                val half = w * widthScale * (0.5f + 0.5f * i / steps) + wave
                if (i == 0) path.moveTo(cx - half, y) else path.lineTo(cx - half, y)
            }
            for (i in steps downTo 0) {
                val y = size.height * i / steps
                val wave = sin(y / 41f - t * 2.2f) * w * 0.14f + sin(y / 11f + t * 3.0f) * w * 0.06f
                val half = w * widthScale * (0.5f + 0.5f * i / steps) + wave
                path.lineTo(cx + half, y)
            }
            path.close()
            drawPath(
                path,
                Brush.horizontalGradient(
                    listOf(Color.Transparent, color.copy(alpha = alpha * 0.4f), color.copy(alpha = alpha), color.copy(alpha = alpha * 0.4f), Color.Transparent),
                    startX = cx - w * widthScale * 1.2f, endX = cx + w * widthScale * 1.2f,
                ),
            )
        }
        // Coloured outer glow, then the white core.
        column(1.9f, edge, 0.35f)
        column(1.0f, Color.White, 0.85f)
        // The top fades into the dark.
        drawRect(Brush.verticalGradient(listOf(Delta.Black, Color.Transparent), endY = size.height * 0.4f))
        // Motes rising.
        for ((fx, fy, speed) in motes) {
            val y = size.height - ((fy * size.height + t * 70f * speed) % size.height)
            val x = cx + (fx - 0.5f) * w * 1.8f + sin(t * 1.5f + fy * 20f) * 7f
            val a = (0.4f + 0.6f * ((sin(t * 4f + fx * 30f) + 1) / 2)) * ((y / size.height - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val s = (1.5f + speed * 2.5f).dp.toPx()
            drawRect((if (fx > 0.8f) edge else Color.White).copy(alpha = a.coerceIn(0f, 1f)), Offset(x, y), Size(s, s))
        }
        // Three little SOULs circling the column (behind it on the far side).
        for (k in 0 until 3) {
            val ang = t * 1.1f + k * (2 * PI.toFloat() / 3)
            val ox = cx + cos(ang) * size.width * 0.28f
            val oy = size.height * 0.9f + sin(ang) * size.height * 0.07f
            val depth = (sin(ang) + 1) / 2
            pixelArt(HEART_ART, Offset(ox, oy), (1.6f + 1.4f * depth).dp.toPx(), mapOf('X' to Delta.Red.copy(alpha = 0.45f + 0.55f * depth)))
        }
    }
}

/** Sparkles in several colours, twinkling and drifting up the screen. */
@Composable
fun Sparkles(modifier: Modifier = Modifier, count: Int = 26) {
    val t = effectTime()
    val stars = remember(count) { List(count) { Random(it * 104729 + 3).let { r -> listOf(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    Canvas(modifier.fillMaxSize()) {
        for ((fx, fy, phase, speed) in stars) {
            val y = (fy * size.height - t * (10f + speed * 22f)).mod(size.height)
            val x = fx * size.width + sin(t * 0.6f + phase * 6f) * 12f
            val twinkle = ((sin(t * (1.5f + speed * 2.5f) + phase * 2 * PI.toFloat()) + 1) / 2)
            val c = DarkWorld.Sparkle[(phase * DarkWorld.Sparkle.size).toInt() % DarkWorld.Sparkle.size].copy(alpha = 0.12f + 0.55f * twinkle)
            val arm = (2.5f + speed * 3.5f).dp.toPx() * (0.5f + 0.5f * twinkle)
            val p = 1.5.dp.toPx()
            drawRect(c, Offset(x - arm, y - p / 2), Size(arm * 2, p))
            drawRect(c, Offset(x - p / 2, y - arm), Size(p, arm * 2))
        }
    }
}

// ------------------------------------------------------------------ pixel art (original)

private val HEART_ART = listOf(
    ".XXX...XXX.",
    "XXXXX.XXXXX",
    "XXXXXXXXXXX",
    "XXXXXXXXXXX",
    ".XXXXXXXXX.",
    "..XXXXXXX..",
    "...XXXXX...",
    "....XXX....",
    ".....X.....",
)

/**
 * Deltacord's own little mascot (an original design): a round horned creature with a lantern of
 * light. Two frames of hopping. B = body, H = horns, E = eyes, L = lantern light, F = feet.
 */
private val MASCOT = listOf(
    listOf(
        "..H......H..",
        "..HH....HH..",
        "...BBBBBB...",
        "..BBBBBBBB..",
        ".BBEEBBEEBB.",
        ".BBEEBBEEBB.",
        ".BBBBBBBBBB.",
        ".BBBBBBBBBBL",
        "..BBBBBBBB.L",
        "...BBBBBB...",
        "...F....F...",
    ),
    listOf(
        "............",
        "..H......H..",
        "..HH....HH..",
        "...BBBBBB...",
        "..BBBBBBBB..",
        ".BBEEBBEEBB.",
        ".BBEEBBEEBB.",
        ".BBBBBBBBBBL",
        "..BBBBBBBB.L",
        "...BBBBBB...",
        "..FF....FF..",
    ),
)

/** Draws pixel art from rows of characters, coloured by [palette] ('.' is empty). */
private fun DrawScope.pixelArt(art: List<String>, center: Offset, pixel: Float, palette: Map<Char, Color>, flip: Boolean = false) {
    val w = art[0].length * pixel
    val h = art.size * pixel
    art.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val color = palette[c] ?: return@forEachIndexed
            val px = if (flip) art[0].length - 1 - x else x
            drawRect(color, Offset(center.x - w / 2 + px * pixel, center.y - h / 2 + y * pixel), Size(pixel + 0.5f, pixel + 0.5f))
        }
    }
}

/**
 * The bottom of the main menu: a skyline of castle spires against the coloured dark, and the
 * mascot hopping back and forth along it with its lantern glowing.
 */
@Composable
fun MascotScene(modifier: Modifier = Modifier) {
    val t = effectTime()
    Canvas(modifier) {
        val ground = size.height - 6.dp.toPx()
        // Skyline: spires of different heights, in front of a coloured glow.
        val glow = cycle(DarkWorld.Lights, t * 0.03f + 0.25f)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.35f)), startY = size.height * 0.2f, endY = ground))
        val spires = listOf(0.06f to 0.45f, 0.16f to 0.7f, 0.24f to 0.5f, 0.38f to 0.85f, 0.47f to 0.6f, 0.6f to 0.75f, 0.72f to 0.5f, 0.83f to 0.9f, 0.93f to 0.55f)
        val px = 4.dp.toPx()
        for ((fx, fh) in spires) {
            val sx = fx * size.width
            val top = ground - fh * size.height * 0.8f
            val bw = 5 * px
            drawRect(Color(0xFF12061F), Offset(sx - bw / 2, top + 2 * px), Size(bw, ground - top))
            drawRect(Color(0xFF12061F), Offset(sx - px * 1.5f, top), Size(px * 3, 2 * px))
            drawRect(Color(0xFF12061F), Offset(sx - px / 2, top - 2 * px), Size(px, 2 * px))
            // A lit window.
            if ((fx * 100).toInt() % 2 == 0) drawRect(Color(0xFFFFE27A).copy(alpha = 0.5f + 0.5f * ((sin(t * 2 + fx * 9) + 1) / 2)), Offset(sx - px / 2, top + 4 * px), Size(px, px))
        }
        drawRect(Color(0xFF12061F), Offset(0f, ground), Size(size.width, size.height - ground))

        // The mascot walks across, turns around, and hops.
        val span = size.width - 80.dp.toPx()
        val phase = (t * 0.07f) % 2f
        val forward = phase < 1f
        val along = if (forward) phase else 2f - phase
        val x = 40.dp.toPx() + span * along
        val hop = abs(sin(t * 5f)) * 10.dp.toPx()
        val frame = if (sin(t * 5f) > 0) 0 else 1
        val p = 4.5.dp.toPx()
        val center = Offset(x, ground - MASCOT[0].size * p / 2 - hop)
        // Its lantern lights the ground.
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0A0).copy(alpha = 0.35f), Color.Transparent), center = Offset(x, ground), radius = 60.dp.toPx()), 60.dp.toPx(), Offset(x, ground))
        pixelArt(
            MASCOT[frame], center, p,
            mapOf('B' to Color(0xFF2B2B5E), 'H' to Color(0xFFE8E8FF), 'E' to Color.White, 'L' to Color(0xFFFFF0A0), 'F' to Color(0xFF16163A)),
            flip = !forward,
        )
    }
}

// ------------------------------------------------------------------ the logo

/**
 * The logo: it drops in letter by letter, then a rainbow shine sweeps across it, it glows and
 * breathes, and every few seconds it glitches: split into red and cyan and shaken.
 */
@Composable
fun FlashyLogo(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val moving = LocalFlashy.current && !LocalStill.current
    val showEffects = LocalFlashy.current
    val t = effectTime()
    val letters = text.toList()
    val shown = remember(text) { letters.map { Animatable(if (moving) 0f else 1f) } }
    LaunchedEffect(text, moving) {
        if (!moving) return@LaunchedEffect
        delay(200)
        for (a in shown) a.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }
    val glitch = showEffects && (t % 6f) in 0f..0.18f
    val shake = if (glitch) sin(t * 140f) * 7f else 0f
    val sweep = (t * 0.4f) % 1.8f - 0.4f
    val glow = 0.6f + 0.4f * ((sin(t * 2.2f) + 1) / 2)
    val shine = Brush.linearGradient(
        colors = listOf(Delta.White, Delta.White, hue(t * 0.2f), hue(t * 0.2f + 0.33f), hue(t * 0.2f + 0.66f), Delta.White, Delta.White),
        start = Offset(sweep * 1000f - 350f, 0f),
        end = Offset(sweep * 1000f + 350f, 160f),
        tileMode = TileMode.Clamp,
    )
    val glowShadow = Shadow(hue(t * 0.08f, 0.4f).copy(alpha = if (showEffects) 0.9f * glow else 0.35f), Offset.Zero, if (showEffects) 34f else 18f)
    val main = if (showEffects) style.merge(TextStyle(brush = shine, shadow = glowShadow)) else style.copy(shadow = glowShadow)
    val breathe = if (showEffects) 1f + 0.025f * sin(t * 1.6f) else 1f
    Box(
        modifier.fillMaxWidth().graphicsLayer { translationX = shake; scaleX = breathe; scaleY = breathe },
        contentAlignment = Alignment.Center,
    ) {
        if (glitch) {
            LogoLetters(letters, shown, style.copy(color = Color(0xFFFF2040)), Modifier.graphicsLayer { translationX = -9f; translationY = 2f })
            LogoLetters(letters, shown, style.copy(color = Color(0xFF20FFF0)), Modifier.graphicsLayer { translationX = 9f; translationY = -2f })
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
                        translationY = (1 - p) * -60f
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        rotationZ = (1 - p) * if (i % 2 == 0) -20f else 20f
                    },
            )
        }
    }
}

// ------------------------------------------------------------------ motion helpers

/** Fades, slides and pops a menu item in, a little after the one before it ([index]). */
@Composable
fun Modifier.entrance(index: Int): Modifier {
    val moving = LocalFlashy.current && !LocalStill.current
    val p = remember { Animatable(if (moving) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!moving) return@LaunchedEffect
        delay(150L + index * 80L)
        p.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    return this
        .alpha(p.value)
        .graphicsLayer {
            translationX = (1 - p.value) * 120f
            scaleX = 0.9f + 0.1f * p.value
            scaleY = 0.9f + 0.1f * p.value
        }
}

/** A screen's backdrop: the dark world, with sparkles. */
@Composable
fun FlashyBackdrop(strong: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        DarkWorldBackdrop(strong = strong)
        if (LocalFlashy.current) Sparkles(count = if (strong) 28 else 16)
        // The main menu: the skyline and the mascot along the bottom of the screen.
        if (strong && LocalFlashy.current) {
            MascotScene(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp).navigationBarsPadding())
        }
        content()
    }
}

/** A gentle up-and-down bob for the SOUL. */
@Composable
fun bob(): Float {
    val moving = LocalFlashy.current && !LocalStill.current
    if (!moving) return 0f
    val t = rememberInfiniteTransition(label = "bob")
    return t.animateFloat(-3f, 3f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y").value
}

/** A glow behind text that's selected. */
fun selectedGlow(color: Color) = Shadow(color.copy(alpha = 0.9f), Offset.Zero, 18f)
