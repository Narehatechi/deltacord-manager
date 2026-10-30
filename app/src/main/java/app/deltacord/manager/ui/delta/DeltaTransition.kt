package app.deltacord.manager.ui.delta

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition

/**
 * Moving between screens, the game's way: a white flash, the new screen zooming in from slightly
 * big as the old one shrinks away. The app opening gets a bigger flash. Plain fades with Flashy
 * effects off.
 */
@Composable
fun FlashTransition(navigator: Navigator) {
    val flashy = LocalFlashy.current
    val flash = remember { Animatable(if (flashy) 1f else 0f) }
    val key = navigator.lastItem.key
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (!flashy) return@LaunchedEffect
        if (first[0]) {
            first[0] = false
            // Opening: from white.
            flash.snapTo(1f)
            flash.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
        } else {
            flash.snapTo(0.55f)
            flash.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.fillMaxSize()) {
        ScreenTransition(
            navigator = navigator,
            transition = {
                if (flashy) {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(260)) + scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 1.12f),
                        initialContentExit = fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.9f),
                    )
                } else {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                }
            },
        )
        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().alpha(flash.value).background(Delta.White))
        }
    }
}
