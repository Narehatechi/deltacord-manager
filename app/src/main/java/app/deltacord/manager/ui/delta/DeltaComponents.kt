package app.deltacord.manager.ui.delta

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ the SOUL

private val HEART = listOf(
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

/** The red pixel heart. [pixel] is the size of one of its 11×9 squares. */
@Composable
fun SoulHeart(modifier: Modifier = Modifier, pixel: Dp = 2.dp, color: Color = Delta.Red) {
    Canvas(modifier.size(pixel * HEART[0].length, pixel * HEART.size)) {
        val p = pixel.toPx()
        HEART.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                // Slight overlap so no seams show between squares.
                if (c == 'X') drawRect(color, Offset(x * p, y * p), Size(p + 0.5f, p + 0.5f))
            }
        }
    }
}

// ------------------------------------------------------------------ boxes

/** A white-bordered black box, like the game's text boxes and menus. */
@Composable
fun PixelBox(
    modifier: Modifier = Modifier,
    borderColor: Color = Delta.White,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(Delta.Box)
            .border(3.dp, borderColor, RectangleShape)
            .padding(3.dp)
            .padding(padding),
        content = content,
    )
}

/** A label above a group of options, like "CONFIG" in the game's menus. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = DeltaType.small.copy(color = Delta.Gray, letterSpacing = DeltaType.small.letterSpacing),
        modifier = modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp),
    )
}

// ------------------------------------------------------------------ menus

private val HEART_SLOT = 34.dp

/**
 * One menu option. The SOUL sits beside it while it's [selected] or being pressed, and the text
 * turns yellow, like choosing in the game. [trailing] is a value shown on the right (ON, STABLE…).
 */
@Composable
fun MenuOption(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    color: Color = Delta.White,
    style: TextStyle = DeltaType.menu,
    sub: String? = null,
    trailing: String? = null,
    trailingColor: Color? = null,
) {
    val blip = LocalBlip.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lit = enabled && (selected || pressed)
    val textColor = when {
        !enabled -> Delta.DarkGray
        lit -> Delta.Yellow
        else -> color
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                blip.blip(BlipKind.SELECT)
                onClick()
            }
            .padding(vertical = 8.dp),
    ) {
        Box(Modifier.width(HEART_SLOT), contentAlignment = Alignment.CenterStart) {
            if (lit) SoulHeart(pixel = 2.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = style.copy(color = textColor), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = DeltaType.small, modifier = Modifier.padding(top = 2.dp))
        }
        if (trailing != null) {
            Text(
                trailing,
                style = DeltaType.body.copy(color = trailingColor ?: if (lit) Delta.Yellow else Delta.White),
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp).widthIn(max = 160.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A switch, the game's way: the option with ON or OFF beside it. */
@Composable
fun ToggleOption(
    text: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    enabled: Boolean = true,
) {
    MenuOption(
        text = text,
        onClick = { onChange(!checked) },
        modifier = modifier,
        style = DeltaType.body,
        sub = sub,
        enabled = enabled,
        trailing = if (checked) "ON" else "OFF",
        trailingColor = if (checked) Delta.Yellow else Delta.Gray,
    )
}

/** A choice between a few values: tapping moves to the next one. */
@Composable
fun <T> CycleOption(
    text: String,
    value: T,
    values: List<T>,
    label: (T) -> String,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    MenuOption(
        text = text,
        onClick = { onChange(values[(values.indexOf(value) + 1) % values.size]) },
        modifier = modifier,
        style = DeltaType.body,
        sub = sub,
        trailing = label(value).uppercase(),
    )
}

/** A big boxed button, for the one main thing on a screen. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Delta.White,
) {
    val blip = LocalBlip.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val c = when {
        !enabled -> Delta.DarkGray
        pressed -> Delta.Yellow
        else -> color
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(3.dp, c, RectangleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                blip.blip(BlipKind.SELECT)
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
    ) {
        if (pressed && enabled) {
            SoulHeart(pixel = 2.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(text.uppercase(), style = DeltaType.menu.copy(color = c), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------------ dialogue

/** Frames (of 30 a second) to wait after each character, like the game's text. */
private fun waitAfter(c: Char, next: Char?): Int {
    val breaks = next == null || next == ' ' || next == '\n'
    return when {
        (c == '.' || c == '!' || c == '?') && breaks -> 8
        (c == ',' || c == ':' || c == ';') && breaks -> 4
        else -> 1
    }
}

/**
 * Text that types itself out in a box, one letter a frame with a blip, each line starting with
 * "* " like the game's dialogue. Tap it to show it all at once.
 */
@Composable
fun Dialogue(
    lines: List<String>,
    modifier: Modifier = Modifier,
    asterisk: Boolean = true,
    boxed: Boolean = true,
    onDone: () -> Unit = {},
) {
    val text = lines.joinToString("\n") { if (asterisk) "* $it" else it }
    val preview = LocalStill.current
    val blip = LocalBlip.current
    var shown by remember(text) { mutableIntStateOf(if (preview) text.length else 0) }

    LaunchedEffect(text) {
        while (shown < text.length) {
            val c = text[shown]
            shown++
            if (c.isLetterOrDigit()) blip.blip(BlipKind.TEXT)
            delay(33L * waitAfter(c, text.getOrNull(shown)))
        }
        onDone()
    }

    val tapToFinish = modifier
        .fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
            shown = text.length
        }
    val typed = @Composable {
        Box {
            // The full text, invisible, keeps the box its final size while it types.
            Text(text, style = DeltaType.body.copy(color = Color.Transparent))
            Text(text.take(shown), style = DeltaType.body)
        }
    }
    if (boxed) PixelBox(modifier = tapToFinish) { typed() } else Box(tapToFinish) { typed() }
}

// ------------------------------------------------------------------ progress

/** A progress bar like the HP bar: yellow on red. [progress] null means "working…". */
@Composable
fun HpBar(progress: Float?, modifier: Modifier = Modifier, label: String = "HP", height: Dp = 20.dp) {
    val sweep = if (progress == null && !LocalStill.current) {
        val t = rememberInfiniteTransition(label = "hp")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "sweep").value
    } else 0f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(label, style = DeltaType.small.copy(color = Delta.White), modifier = Modifier.padding(end = 10.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(height)
        ) {
            drawRect(Delta.Red)
            if (progress != null) {
                drawRect(Delta.Yellow, size = Size(size.width * progress.coerceIn(0f, 1f), size.height))
            } else {
                val w = size.width * 0.3f
                drawRect(Delta.Yellow, topLeft = Offset((size.width + w) * sweep - w, 0f), size = Size(w, size.height))
            }
        }
    }
}

// ------------------------------------------------------------------ text input

/** A text field: white box, Determination, a yellow caret. */
@Composable
fun DeltaTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: Boolean = false,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = DeltaType.body)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = DeltaType.body,
            cursorBrush = SolidColor(Delta.Yellow),
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .border(2.dp, if (error) Delta.Red else Delta.White, RectangleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        if (hint != null) {
            Text(hint, style = DeltaType.small.copy(color = if (error) Delta.Red else Delta.Gray), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ------------------------------------------------------------------ screens

/** Room for "BACK" (with the heart) on the left of a screen's title, and the same on the right. */
private val HEADER_SIDE = 100.dp

/**
 * A whole screen: black, the title in capitals, an optional "< BACK", and the content scrolling
 * underneath.
 */
@Composable
fun DeltaScreen(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Delta.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (onBack != null || title != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                if (onBack != null) {
                    MenuOption(
                        text = "BACK",
                        onClick = onBack,
                        style = DeltaType.body,
                        modifier = Modifier.width(HEADER_SIDE),
                    )
                } else {
                    Spacer(Modifier.width(HEADER_SIDE))
                }
                Text(
                    title?.uppercase() ?: "",
                    style = DeltaType.title.copy(fontSize = 22.sp, letterSpacing = 1.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(HEADER_SIDE))
            }
        }
        val body = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 20.dp)
        Column(if (scroll) body.verticalScroll(rememberScrollState()) else body) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * A question in a dialogue box over the screen, with the choices underneath (like the game's
 * yes/no prompts). [options] are label → action; the box closes before the action runs.
 */
@Composable
fun DeltaDialog(
    lines: List<String>,
    options: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        PixelBox(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            Dialogue(lines, boxed = false, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.height(8.dp))
            for ((label, action) in options) {
                MenuOption(text = label, onClick = { onDismiss(); action() }, style = DeltaType.body)
            }
        }
    }
}
