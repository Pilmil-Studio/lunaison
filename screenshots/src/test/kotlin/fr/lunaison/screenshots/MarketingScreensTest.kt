package fr.lunaison.screenshots

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import fr.lunaison.core.designsystem.LunaisonTheme
import fr.lunaison.ui.ArticleScreen
import fr.lunaison.ui.CalendarScreen
import fr.lunaison.ui.HomeScreen
import fr.lunaison.ui.LearnScreen
import fr.lunaison.ui.LogScreen
import fr.lunaison.ui.OnboardingScreen
import fr.lunaison.ui.SettingsScreen
import fr.lunaison.ui.ShareScreen
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Captures pour les visuels marketing (docs/marketing/captures, via pilmil-core/marketing/capture.sh) :
 * mêmes écrans et mêmes données fabriquées que [LocalizedScreens], mais à la résolution réelle de l'appareil
 * (1080 x 2340) au lieu d'une réduction à 1000 px de haut.
 */
abstract class MarketingScreens(private val locale: Locale) {

    init {
        Locale.setDefault(locale)
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(locale = locale.language),
        maxPercentDifference = 0.1,
        useDeviceResolution = true,
    )

    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
        }
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                LunaisonTheme(dark = dark) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                }
            }
        }
    }

    @Test
    fun captures() {
        val state = Sample.state()
        shot("bienvenue") { OnboardingScreen(today = Sample.today, onFinish = {}) }
        shot("accueil") { HomeScreen(state, NoActions, { _, _ -> }, {}, {}, {}) }
        shot("accueil_sombre", dark = true) { HomeScreen(state, NoActions, { _, _ -> }, {}, {}, {}) }
        shot("journee") { LogScreen(state, Sample.today, periodStart = false, onSave = {}, onBack = {}) }
        shot("calendrier") { CalendarScreen(state, onDay = {}) }
        shot("comprendre") { LearnScreen(onArticle = {}) }
        shot("fiche_cycle") { ArticleScreen("cycle", onBack = {}) }
        shot("fiche_douleurs") { ArticleScreen("douleurs", onBack = {}) }
        shot("partage") { ShareScreen(state, NoActions, onBack = {}, onArticle = {}) }
        shot("reglages") { SettingsScreen(state, NoActions, versionName = "0.1.0", onArticle = {}) }
    }
}

class MarketingScreensFrTest : MarketingScreens(Locale.FRENCH)

class MarketingScreensEnTest : MarketingScreens(Locale.ENGLISH)
