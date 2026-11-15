package fr.lunaison.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

/** Contraste WCAG 2 des paires de couleurs déclarées dans Theme.kt : 4,5:1 pour le texte, 3:1 pour bordures et états. */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun textPairs(c: ColorScheme) = listOf(
        "onBackground/background" to (c.onBackground to c.background),
        "onSurface/surface" to (c.onSurface to c.surface),
        "onPrimary/primary" to (c.onPrimary to c.primary),
        "onSecondary/secondary" to (c.onSecondary to c.secondary),
        "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
        "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
        "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
        "onSurfaceVariant/background" to (c.onSurfaceVariant to c.background),
        "primary/surface" to (c.primary to c.surface),
        "secondary/background" to (c.secondary to c.background),
    )

    private fun stateAndBorderPairs(c: ColorScheme) = listOf(
        "outline/surface" to (c.outline to c.surface),
        "outline/background" to (c.outline to c.background),
        "outline/surfaceVariant" to (c.outline to c.surfaceVariant),
        "primary/surface (bordure sélectionnée)" to (c.primary to c.surface),
        "primary/primaryContainer" to (c.primary to c.primaryContainer),
    )

    @Test
    fun textPairsReach4Point5() {
        val failures = AllColorSchemes.flatMap { (name, dark, scheme) ->
            textPairs(scheme).filter { contrast(it.second.first, it.second.second) < 4.5 }
                .map { "$name${if (dark) " sombre" else ""} ${it.first} = ${(kotlin.math.round(contrast(it.second.first, it.second.second) * 100) / 100).toString()}" }
        }
        assertTrue(failures.isEmpty(), "Texte sous 4,5:1 : $failures")
    }

    @Test
    fun bordersAndStatesReach3() {
        val failures = AllColorSchemes.flatMap { (name, dark, scheme) ->
            stateAndBorderPairs(scheme).filter { contrast(it.second.first, it.second.second) < 3.0 }
                .map { "$name${if (dark) " sombre" else ""} ${it.first} = ${(kotlin.math.round(contrast(it.second.first, it.second.second) * 100) / 100).toString()}" }
        }
        assertTrue(failures.isEmpty(), "Bordure ou état sous 3:1 : $failures")
    }
}
