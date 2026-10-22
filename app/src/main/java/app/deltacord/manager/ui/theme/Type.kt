package app.deltacord.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.deltacord.manager.R

/** "Determination Mono" by Abacadaba, CC BY-NC-ND. See app/src/main/res/font/README. */
val Determination = FontFamily(Font(R.font.determination))

private fun TextStyle.determination() = copy(fontFamily = Determination)

// Every Material text style uses Determination, so all of the installer picks it up.
val Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.determination(),
        displayMedium = displayMedium.determination(),
        displaySmall = displaySmall.determination(),
        headlineLarge = headlineLarge.determination(),
        headlineMedium = headlineMedium.determination(),
        headlineSmall = headlineSmall.determination(),
        titleLarge = titleLarge.determination(),
        titleMedium = titleMedium.determination(),
        titleSmall = titleSmall.determination(),
        bodyLarge = bodyLarge.determination(),
        bodyMedium = bodyMedium.determination(),
        bodySmall = bodySmall.determination(),
        labelLarge = labelLarge.determination(),
        labelMedium = labelMedium.determination(),
        labelSmall = labelSmall.determination(),
    )
}
