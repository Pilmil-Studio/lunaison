package fr.lunaison.ui

import kotlin.math.PI
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import fr.lunaison.core.content.Article
import fr.lunaison.core.content.Articles
import fr.lunaison.core.content.Source
import fr.lunaison.core.content.Sources
import fr.lunaison.core.cycle.CycleState
import fr.lunaison.core.cycle.Phase
import fr.lunaison.core.designsystem.LunaCard
import fr.lunaison.core.designsystem.SectionTitle
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.tr


@Composable
internal fun SymptothermalVisual() {
    val colors = MaterialTheme.colorScheme
    LunaCard {
        Text(
            tr(
                "Le tandem glaire + température autour de l'ovulation",
                "Mucus + temperature tandem around ovulation",
                pl = "Śluz i temperatura wokół owulacji",
                uk = "Слиз і температура навколо овуляції",
                es = "Moco y temperatura en torno a la ovulación",
                de = "Schleim und Temperatur rund um den Eisprung",
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MechanismStep(
                modifier = Modifier.weight(1f),
                title = tr("Avant ovulation", "Before ovulation", pl = "Przed owulacją", uk = "До овуляції", es = "Antes de ovulación", de = "Vor Eisprung"),
                subtitle = tr("Glaire claire filante\nTempérature basse", "Clear stretchy mucus\nBaseline temp", pl = "Przejrzysty śluz\nNiższa temp.", uk = "Прозорий слиз\nНижча темп.", es = "Moco elástico\nTemp. basal baja", de = "Klarer Schleim\nTiefe Temp."),
                bg = colors.primaryContainer,
                fg = colors.onPrimaryContainer,
            )
            MechanismStep(
                modifier = Modifier.weight(1f),
                title = tr("Après ovulation", "After ovulation", pl = "Po owulacji", uk = "Після овуляції", es = "Tras ovulación", de = "Nach Eisprung"),
                subtitle = tr("Glaire sèche/épaisse\n+0,2 à +0,5 °C", "Dry/thick mucus\n+0.2 to +0.5 °C", pl = "Śluz suchy/gęsty\n+0,2 do +0,5 °C", uk = "Сухий/густий слиз\n+0,2…+0,5 °C", es = "Moco seco/espeso\n+0,2 a +0,5 °C", de = "Trockener Schleim\n+0,2 bis +0,5 °C"),
                bg = colors.tertiaryContainer,
                fg = colors.onTertiaryContainer,
            )
        }
    }
}

@Composable

internal fun ProtectionsWearVisual() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    LunaCard {
        Text(
            tr("Repère visuel : durée de port max", "Visual guide: max wear time", pl = "Wskazówka wizualna: maks. czas noszenia", uk = "Візуальний орієнтир: макс. час носіння", es = "Guía visual: tiempo máx. de uso", de = "Visuelle Orientierung: max. Tragedauer"),
            style = MaterialTheme.typography.titleMedium,
        )
        WearBar(
            label = tr("Tampon", "Tampon", pl = "Tampon", uk = "Тампон", es = "Tampón", de = "Tampon"),
            duration = "4–6 h",
            fraction = 6f / 12f,
            color = primary,
            trackColor = track,
        )
        WearBar(
            label = tr("Coupe menstruelle", "Menstrual cup", pl = "Kubeczek menstruacyjny", uk = "Менструальна чаша", es = "Copa menstrual", de = "Menstruationstasse"),
            duration = "6–8 h",
            fraction = 8f / 12f,
            color = secondary,
            trackColor = track,
        )
        WearBar(
            label = tr("Serviette / Culotte", "Pad / Period underwear", pl = "Podpaska / Majtki", uk = "Прокладка / Білизна", es = "Compresa / Bragas", de = "Binde / Periodenunterwäsche"),
            duration = "4–8 h",
            fraction = 8f / 12f,
            color = tertiary,
            trackColor = track,
        )
        Text(
            tr("Changer régulièrement réduit le risque de syndrome du choc toxique.", "Changing regularly lowers the risk of toxic shock syndrome.", pl = "Regularna zmiana zmniejsza ryzyko zespołu wstrząsu toksycznego.", uk = "Регулярна заміна знижує ризик синдрому токсичного шоку.", es = "Cambiarla con regularidad reduce el riesgo de síndrome de shock tóxico.", de = "Regelmäßiges Wechseln senkt das Risiko für ein toxisches Schocksyndrom."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WearBar(
    label: String,
    duration: String,
    fraction: Float,
    color: Color,
    trackColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(duration, style = MaterialTheme.typography.labelLarge, color = color)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            )
        }
    }
}

@Composable
internal fun PainMechanismVisual() {
    val primary = MaterialTheme.colorScheme.primaryContainer
    val onPrimary = MaterialTheme.colorScheme.onPrimaryContainer
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    val onSecondary = MaterialTheme.colorScheme.onSecondaryContainer
    LunaCard {
        Text(
            tr("Mécanisme et leviers d'apaisement", "Mechanism and relief levers", pl = "Mechanizm i sposoby łagodzenia", uk = "Механізм і способи полегшення", es = "Mecanismo y formas de alivio", de = "Mechanismus und Linderung"),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MechanismStep(
                modifier = Modifier.weight(1f),
                title = tr("1. Prostaglandines", "1. Prostaglandins", pl = "1. Prostaglandyny", uk = "1. Простагландини", es = "1. Prostaglandinas", de = "1. Prostaglandine"),
                subtitle = tr("Pic au début des règles", "Peak as bleeding starts", pl = "Szczyt na początku", uk = "Пік на початку", es = "Pico al inicio", de = "Spitze zu Beginn"),
                bg = primary,
                fg = onPrimary,
            )
            MechanismStep(
                modifier = Modifier.weight(1f),
                title = tr("2. Contractions", "2. Contractions", pl = "2. Skurcze", uk = "2. Скорочення", es = "2. Contracciones", de = "2. Kontraktionen"),
                subtitle = tr("Moins d'oxygène local", "Less local blood flow", pl = "Mniej tlenu w mięśniu", uk = "Менше кисню", es = "Menor flujo local", de = "Weniger Durchblutung"),
                bg = primary,
                fg = onPrimary,
            )
            MechanismStep(
                modifier = Modifier.weight(1f),
                title = tr("3. Leviers", "3. Relief", pl = "3. Ulga", uk = "3. Полегшення", es = "3. Alivio", de = "3. Linderung"),
                subtitle = tr("Chaleur · Mouvement · 4-6 s", "Heat · Movement · 4-6 s", pl = "Ciepło · Ruch · 4-6 s", uk = "Тепло · Рух · 4-6 с", es = "Calor · Movimiento · 4-6 s", de = "Wärme · Bewegung · 4-6 s"),
                bg = secondary,
                fg = onSecondary,
            )
        }
    }
}

@Composable
private fun MechanismStep(
    modifier: Modifier,
    title: String,
    subtitle: String,
    bg: Color,
    fg: Color,
) {
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = fg)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

@Composable
internal fun SpmWindowVisual() {
    val menstrualColor = MaterialTheme.colorScheme.primary
    val lutealColor = MaterialTheme.colorScheme.secondary
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    LunaCard {
        Text(
            tr("Où se situe le SPM dans un cycle (exemple 28 j)", "Where PMS sits in a cycle (28-day example)", pl = "Kiedy pojawia się PMS w cyklu (przykład 28 dni)", uk = "Коли виникає ПМС у циклі (приклад 28 днів)", es = "Dónde se sitúa el SPM (ejemplo 28 días)", de = "Wann PMS im Zyklus auftritt (28-Tage-Beispiel)"),
            style = MaterialTheme.typography.titleMedium,
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            drawRoundRect(color = baseColor, cornerRadius = corner)
            // J1-J5 : règles
            drawRoundRect(
                color = menstrualColor,
                size = Size(size.width * (5f / 28f), size.height),
                cornerRadius = corner,
            )
            // J21-J28 : fenêtre SPM (avant les règles)
            val spmStart = size.width * (20f / 28f)
            drawRoundRect(
                color = lutealColor,
                topLeft = Offset(spmStart, 0f),
                size = Size(size.width - spmStart, size.height),
                cornerRadius = corner,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                tr("J1–J5 : règles", "D1–D5: period", pl = "D1–D5: okres", uk = "Д1–Д5: менструація", es = "D1–D5: regla", de = "T1–T5: Periode"),
                style = MaterialTheme.typography.bodySmall,
                color = menstrualColor,
            )
            Text(
                tr("J21–J28 : fenêtre SPM", "D21–D28: PMS window", pl = "D21–D28: okno PMS", uk = "Д21–Д28: вікно ПМС", es = "D21–D28: ventana SPM", de = "T21–T28: PMS-Fenster"),
                style = MaterialTheme.typography.bodySmall,
                color = lutealColor,
            )
        }
    }
}
