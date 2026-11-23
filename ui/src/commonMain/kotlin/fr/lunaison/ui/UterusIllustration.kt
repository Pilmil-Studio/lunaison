package fr.lunaison.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import fr.lunaison.core.model.tr
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Schéma de l'appareil génital interne, vue de face (coupe frontale), dessiné à la main dans un Canvas comme les
 * scènes de Sillon : du fond vers l'avant, avec des dégradés doux et sans effet GPU, pour un rendu identique sur
 * le téléphone et dans les captures. Les teintes sont des teintes de chair adoucies : réaliste, sans rouge vif.
 * Tout ce qui change au fil du cycle vient de [CyclePhysiology], avec un jour décimal : rien ne saute.
 */

// Teintes de tissus, choisies douces et désaturées.
private val MyoLight = Color(0xFFF4C4B8)
private val MyoDeep = Color(0xFFDC968C)
private val MyoRim = Color(0xFFB9726C)
private val EndoProliferative = Color(0xFFEFA5A2)
private val EndoSecretory = Color(0xFFD9798A)
private val EndoShedding = Color(0xFFB3566A)
private val GlandTint = Color(0xFFFBE0DA)
private val ArteryTint = Color(0xFFB45A6C)
private val CervixLight = Color(0xFFF1C2C4)
private val CervixDeep = Color(0xFFDB9CA4)
private val MucusOpaque = Color(0xFFEFE4D6)
private val MucusClear = Color(0xFFE6F3F6)
private val VaginaLight = Color(0xFFF0C9CF)
private val VaginaDeep = Color(0xFFD9A3AE)
private val TubeOuter = Color(0xFFD99595)
private val TubeInner = Color(0xFFF2C0B8)
private val OvaryLight = Color(0xFFF5DECF)
private val OvaryDeep = Color(0xFFDDB5A2)
private val OvaryRim = Color(0xFFBA907E)
private val FollicleTint = Color(0xFFFFF6EA)
private val LuteumYoung = Color(0xFFF2C96E)
private val LuteumOld = Color(0xFFF0E5D2)
private val EggTint = Color(0xFFFFF8EC)
private val LigamentTint = Color(0xFFE6BDB1)
private val NightWash = Color(0xFF2B2026)

/** Emplacements des organes, calculés une fois par taille de Canvas ; sert au dessin et à la détection du toucher. */
private class UterusLayout(w: Float, val h: Float) {
    val s = min(w, h * 1.25f)
    val cx = w / 2f
    val topY = h * 0.2f
    val fundusW = s * 0.19f
    val isthY = h * 0.56f
    val isthW = s * 0.07f
    val cervW = s * 0.078f
    val cervBottom = h * 0.68f
    val vagW = s * 0.078f
    val vagBottom = h * 0.92f
    val cavTop = topY + h * 0.07f
    val cavW = fundusW * 0.62f
    val cavApex = isthY - h * 0.015f
    val ovaryW = s * 0.15f
    val ovaryH = s * 0.09f
    val ovaryY = topY + h * 0.29f

    fun ovaryX(side: Float) = cx + side * s * 0.33f

    fun cornu(side: Float) = Offset(cx + side * fundusW * 0.92f, topY + h * 0.045f)
    fun tubeEnd(side: Float) = Offset(cx + side * s * 0.41f, topY + h * 0.2f)
    fun tubeC1(side: Float) = Offset(cx + side * s * 0.27f, topY - h * 0.07f)
    fun tubeC2(side: Float) = Offset(cx + side * s * 0.47f, topY + h * 0.02f)

    /** Point de la trompe, de la corne utérine (0) au pavillon (1). */
    fun tubePoint(side: Float, t: Float): Offset {
        val p0 = cornu(side)
        val p1 = tubeC1(side)
        val p2 = tubeC2(side)
        val p3 = tubeEnd(side)
        val u = 1f - t
        return p0 * (u * u * u) + p1 * (3f * u * u * t) + p2 * (3f * u * t * t) + p3 * (t * t * t)
    }

    /** Organe touché à cet endroit, ou null. */
    fun hit(p: Offset, liningPx: Float): UterusPart? {
        val dx = p.x - cx
        for (side in listOf(-1f, 1f)) {
            val ex = (p.x - ovaryX(side)) / (ovaryW * 0.62f)
            val ey = (p.y - ovaryY) / (ovaryH * 0.65f)
            if (ex * ex + ey * ey <= 1f) return UterusPart.OVARIES
        }
        for (side in listOf(-1f, 1f)) {
            for (i in 1..14) {
                val q = tubePoint(side, i / 14f)
                if (hypot(p.x - q.x, p.y - q.y) < s * 0.035f) return UterusPart.TUBES
            }
        }
        if (p.y > cervBottom - h * 0.005f && abs(dx) < vagW * 1.15f && p.y < vagBottom) return UterusPart.VAGINA
        if (p.y in (isthY + h * 0.015f)..(cervBottom + h * 0.01f) && abs(dx) < cervW * 1.25f) return UterusPart.CERVIX
        if (p.y in (cavTop - liningPx)..(cavApex + liningPx)) {
            val k = ((p.y - cavTop) / (cavApex - cavTop)).coerceIn(0f, 1f)
            if (abs(dx) <= cavW * (1f - k) + liningPx) return UterusPart.ENDOMETRIUM
        }
        if (p.y in (topY - h * 0.05f)..(isthY + h * 0.015f) && abs(dx) < fundusW * 1.05f) return UterusPart.MYOMETRIUM
        return null
    }
}

/** Teinte adoucie vers un beige de même clarté, pour estomper les organes non sélectionnés sans les éteindre. */
private fun Color.muted(amount: Float): Color {
    if (amount <= 0f) return this
    val l = 0.3f * red + 0.59f * green + 0.11f * blue
    val warmNeutral = Color((l * 1.04f).coerceAtMost(1f), l, (l * 0.95f), alpha)
    return lerp(this, warmNeutral, amount)
}

/**
 * Le schéma lui-même. [day] est décimal (1 à 28) : il suit le curseur en glissant. Toucher un organe le
 * sélectionne ; les autres s'estompent en douceur.
 */
@Composable
internal fun UterusIllustration(
    day: Float,
    selected: UterusPart,
    onSelect: (UterusPart) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val measurer = rememberTextMeasurer()
    val currentOnSelect by rememberUpdatedState(onSelect)
    val focus = UterusPart.entries.associateWith { part ->
        animateFloatAsState(if (part == selected) 1f else 0f, tween(durationMillis = 280), label = "focus-${part.name}").value
    }
    val label = uterusPartChipLabel(selected)
    val roundedDay = day.roundToInt().coerceIn(1, 28)
    val description = tr(
        "Schéma de l'utérus au jour $roundedDay, ${cyclePhaseName(roundedDay)}. Organe sélectionné : $label.",
        "Diagram of the uterus on day $roundedDay, ${cyclePhaseName(roundedDay)}. Selected: $label.",
        pl = "Schemat macicy w dniu $roundedDay, ${cyclePhaseName(roundedDay)}. Wybrano: $label.",
        uk = "Схема матки в день $roundedDay, ${cyclePhaseName(roundedDay)}. Вибрано: $label.",
        es = "Esquema del útero el día $roundedDay, ${cyclePhaseName(roundedDay)}. Seleccionado: $label.",
        de = "Schema der Gebärmutter an Tag $roundedDay, ${cyclePhaseName(roundedDay)}. Ausgewählt: $label.",
    )

    Canvas(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    val layout = UterusLayout(size.width.toFloat(), size.height.toFloat())
                    val lining = layout.s * 0.06f
                    layout.hit(p, lining)?.let(currentOnSelect)
                }
            },
    ) {
        val layout = UterusLayout(size.width, size.height)
        // En thème sombre, les tissus s'assombrissent un peu pour ne pas éblouir.
        fun tone(c: Color) = if (dark) lerp(c, NightWash, 0.18f) else c
        fun tissue(part: UterusPart, c: Color) = tone(c).muted(0.38f * (1f - (focus[part] ?: 0f)))

        drawBackdrop(layout, colors.primary.copy(alpha = if (dark) 0.10f else 0.07f))
        drawVagina(layout, day, ::tissue)
        drawLigamentsAndOvaries(layout, day, ::tissue)
        drawTubes(layout, day, ::tissue)
        drawUterus(layout, day, ::tissue)
        drawFocusRing(layout, selected, focus[selected] ?: 0f, colors.primary)

        // Étiquette de l'organe sélectionné, en haut à gauche pour ne jamais masquer le schéma.
        val text = measurer.measure(label, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onPrimary))
        val padH = 8f.dpx(this)
        val padV = 4f.dpx(this)
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(padH, padH),
            size = Size(text.size.width + 2 * padH, text.size.height + 2 * padV),
            cornerRadius = CornerRadius(10f.dpx(this)),
        )
        drawText(text, topLeft = Offset(2 * padH, padH + padV))
    }
}

private fun Float.dpx(scope: DrawScope): Float = this * scope.density

/** Halo chaud derrière l'utérus, pour détacher le schéma du fond de la carte. */
private fun DrawScope.drawBackdrop(l: UterusLayout, glow: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(l.cx, l.topY + size.height * 0.25f), radius = l.s * 0.62f),
        radius = l.s * 0.62f,
        center = Offset(l.cx, l.topY + size.height * 0.25f),
    )
}

private fun DrawScope.drawVagina(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    val h = size.height
    val top = l.cervBottom - h * 0.06f
    val path = Path().apply {
        moveTo(l.cx - l.vagW, top)
        cubicTo(l.cx - l.vagW * 1.15f, top + h * 0.08f, l.cx - l.vagW * 0.95f, l.vagBottom - h * 0.08f, l.cx - l.vagW * 0.9f, l.vagBottom)
        lineTo(l.cx + l.vagW * 0.9f, l.vagBottom)
        cubicTo(l.cx + l.vagW * 0.95f, l.vagBottom - h * 0.08f, l.cx + l.vagW * 1.15f, top + h * 0.08f, l.cx + l.vagW, top)
        cubicTo(l.cx + l.vagW * 0.6f, top - h * 0.025f, l.cx - l.vagW * 0.6f, top - h * 0.025f, l.cx - l.vagW, top)
        close()
    }
    val light = tissue(UterusPart.VAGINA, VaginaLight)
    val deep = tissue(UterusPart.VAGINA, VaginaDeep)
    drawPath(path, Brush.horizontalGradient(listOf(deep, light, deep), startX = l.cx - l.vagW, endX = l.cx + l.vagW))
    // Lumière du canal et plis transversaux, très discrets.
    clipPath(path) {
        drawRect(
            Brush.verticalGradient(listOf(light.copy(alpha = 0.9f), light.copy(alpha = 0f)), startY = l.cervBottom, endY = l.vagBottom),
            topLeft = Offset(l.cx - l.vagW * 0.35f, l.cervBottom),
            size = Size(l.vagW * 0.7f, l.vagBottom - l.cervBottom),
        )
        for (i in 1..5) {
            val y = l.cervBottom + (l.vagBottom - l.cervBottom) * i / 6f
            val rw = l.vagW * 0.7f
            val fold = Path().apply {
                moveTo(l.cx - rw, y)
                cubicTo(l.cx - rw * 0.3f, y - 1.6f.dpx(this@drawVagina), l.cx + rw * 0.3f, y + 1.6f.dpx(this@drawVagina), l.cx + rw, y)
            }
            drawPath(fold, deep.copy(alpha = 0.35f), style = Stroke(1f.dpx(this), cap = StrokeCap.Round))
        }
        // Règles : un voile qui s'estompe vers le bas, sans gouttes.
        val flow = CyclePhysiology.shedding(day)
        if (flow > 0.02f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(tissue(UterusPart.VAGINA, EndoShedding).copy(alpha = 0.55f * flow), Color.Transparent),
                    startY = l.cervBottom,
                    endY = l.cervBottom + (l.vagBottom - l.cervBottom) * 0.6f,
                ),
                topLeft = Offset(l.cx - l.vagW * 0.22f, l.cervBottom),
                size = Size(l.vagW * 0.44f, (l.vagBottom - l.cervBottom) * 0.6f),
            )
        }
    }
    drawPath(path, deep.copy(alpha = 0.8f), style = Stroke(1.2f.dpx(this), join = StrokeJoin.Round))
}

private fun DrawScope.drawLigamentsAndOvaries(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    for (side in listOf(-1f, 1f)) {
        val ox = l.ovaryX(side)
        // Ligament de l'ovaire, qui le relie à l'utérus.
        drawLine(
            tissue(UterusPart.OVARIES, LigamentTint),
            start = Offset(ox - side * l.ovaryW * 0.4f, l.ovaryY - l.ovaryH * 0.1f),
            end = Offset(l.cx + side * l.fundusW * 0.82f, l.topY + size.height * 0.12f),
            strokeWidth = l.s * 0.014f,
            cap = StrokeCap.Round,
        )
        val topLeft = Offset(ox - l.ovaryW / 2f, l.ovaryY - l.ovaryH / 2f)
        val ovarySize = Size(l.ovaryW, l.ovaryH)
        drawOval(
            Brush.radialGradient(
                listOf(tissue(UterusPart.OVARIES, OvaryLight), tissue(UterusPart.OVARIES, OvaryDeep)),
                center = Offset(ox - l.ovaryW * 0.12f, l.ovaryY - l.ovaryH * 0.15f),
                radius = l.ovaryW * 0.62f,
            ),
            topLeft,
            ovarySize,
        )
        drawOval(tissue(UterusPart.OVARIES, OvaryRim), topLeft, ovarySize, style = Stroke(1.2f.dpx(this)))

        // Petits follicles au repos, en vésicules translucides.
        val follicle = tissue(UterusPart.OVARIES, FollicleTint)
        listOf(Offset(-0.28f, 0.1f), Offset(0.05f, -0.22f), Offset(0.3f, 0.15f), Offset(-0.05f, 0.25f)).forEach { o ->
            val c = Offset(ox + side * o.x * l.ovaryW, l.ovaryY + o.y * l.ovaryH)
            drawCircle(follicle.copy(alpha = 0.85f), radius = l.s * 0.009f, center = c)
            drawCircle(tissue(UterusPart.OVARIES, OvaryRim).copy(alpha = 0.5f), radius = l.s * 0.009f, center = c, style = Stroke(0.8f.dpx(this)))
        }
    }

    // L'ovaire de gauche porte le follicule dominant de ce cycle, puis le corps jaune.
    val ox = l.ovaryX(-1f)
    val spot = Offset(ox - l.ovaryW * 0.1f, l.ovaryY - l.ovaryH * 0.05f)
    val dominant = CyclePhysiology.dominantFollicle(day)
    if (dominant > 0.01f) {
        val r = l.s * (0.012f + 0.03f * dominant)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), tissue(UterusPart.OVARIES, FollicleTint)), center = spot - Offset(r * 0.3f, r * 0.3f), radius = r),
            radius = r,
            center = spot,
        )
        drawCircle(tissue(UterusPart.OVARIES, OvaryRim), radius = r, center = spot, style = Stroke(1f.dpx(this)))
    }
    val flash = CyclePhysiology.ovulationFlash(day)
    if (flash > 0.02f) {
        drawCircle(
            Brush.radialGradient(listOf(EggTint.copy(alpha = 0.8f * flash), Color.Transparent), center = spot, radius = l.s * 0.07f),
            radius = l.s * 0.07f,
            center = spot,
        )
    }
    val luteum = CyclePhysiology.corpusLuteum(day)
    if (luteum > 0.01f) {
        val r = l.s * 0.032f * luteum.coerceAtLeast(0.45f)
        val fade = CyclePhysiology.smooth(23f, 28f, day)
        val c = tissue(UterusPart.OVARIES, lerp(LuteumYoung, LuteumOld, fade))
        drawCircle(Brush.radialGradient(listOf(c, lerp(c, OvaryRim, 0.25f)), center = spot, radius = r), radius = r, center = spot)
    }
}

private fun DrawScope.drawTubes(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    val outer = tissue(UterusPart.TUBES, TubeOuter)
    val inner = tissue(UterusPart.TUBES, TubeInner)
    for (side in listOf(-1f, 1f)) {
        val c0 = l.cornu(side)
        val c1 = l.tubeC1(side)
        val c2 = l.tubeC2(side)
        val end = l.tubeEnd(side)
        val path = Path().apply {
            moveTo(c0.x, c0.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
        }
        // Deux traits superposés donnent le volume d'un conduit.
        drawPath(path, outer, style = Stroke(l.s * 0.03f, cap = StrokeCap.Round))
        drawPath(path, inner, style = Stroke(l.s * 0.012f, cap = StrokeCap.Round))
        // Pavillon : franges souples tournées vers l'ovaire.
        for (i in 0 until 7) {
            val a = -0.9f + i * 0.3f
            val tip = end + Offset(side * l.s * 0.035f * sin(a + 1.2f), l.s * (0.03f + 0.012f * abs(sin(i * 1.7f))))
            val fringe = Path().apply {
                moveTo(end.x, end.y)
                quadraticTo(end.x + side * l.s * 0.02f * (i - 3) / 3f, end.y + l.s * 0.01f, tip.x, tip.y)
            }
            drawPath(fringe, outer, style = Stroke(l.s * 0.011f, cap = StrokeCap.Round))
            drawPath(fringe, inner, style = Stroke(l.s * 0.005f, cap = StrokeCap.Round))
        }
    }
    // L'ovule descend la trompe de gauche dans les jours qui suivent l'ovulation.
    CyclePhysiology.eggProgress(day)?.let { progress ->
        val p = l.tubePoint(-1f, 1f - progress)
        drawCircle(Brush.radialGradient(listOf(EggTint.copy(alpha = 0.7f), Color.Transparent), center = p, radius = l.s * 0.03f), radius = l.s * 0.03f, center = p)
        drawCircle(EggTint, radius = l.s * 0.011f, center = p)
        drawCircle(OvaryRim.copy(alpha = 0.6f), radius = l.s * 0.011f, center = p, style = Stroke(0.8f.dpx(this)))
    }
}

private fun DrawScope.drawUterus(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    val h = size.height
    val body = Path().apply {
        moveTo(l.cx - l.isthW, l.isthY)
        cubicTo(l.cx - l.isthW * 1.4f, l.isthY - h * 0.1f, l.cx - l.fundusW, l.topY + h * 0.17f, l.cx - l.fundusW, l.topY + h * 0.05f)
        cubicTo(l.cx - l.fundusW, l.topY - h * 0.05f, l.cx + l.fundusW, l.topY - h * 0.05f, l.cx + l.fundusW, l.topY + h * 0.05f)
        cubicTo(l.cx + l.fundusW, l.topY + h * 0.17f, l.cx + l.isthW * 1.4f, l.isthY - h * 0.1f, l.cx + l.isthW, l.isthY)
        cubicTo(l.cx + l.isthW, l.isthY + h * 0.04f, l.cx + l.cervW, l.cervBottom - h * 0.07f, l.cx + l.cervW, l.cervBottom - h * 0.02f)
        cubicTo(l.cx + l.cervW, l.cervBottom + h * 0.015f, l.cx - l.cervW, l.cervBottom + h * 0.015f, l.cx - l.cervW, l.cervBottom - h * 0.02f)
        cubicTo(l.cx - l.cervW, l.cervBottom - h * 0.07f, l.cx - l.isthW, l.isthY + h * 0.04f, l.cx - l.isthW, l.isthY)
        close()
    }

    // Myomètre : un volume charnu, plus clair au centre.
    drawPath(
        body,
        Brush.radialGradient(
            listOf(tissue(UterusPart.MYOMETRIUM, MyoLight), tissue(UterusPart.MYOMETRIUM, MyoDeep)),
            center = Offset(l.cx - l.fundusW * 0.2f, l.topY + h * 0.1f),
            radius = l.fundusW * 1.6f,
        ),
    )
    // Col : même contour, teinte propre, du rétrécissement (isthme) jusqu'à l'orifice.
    clipPath(body) {
        clipRect(top = l.isthY + h * 0.015f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(tissue(UterusPart.CERVIX, CervixLight), tissue(UterusPart.CERVIX, CervixDeep)),
                    startY = l.isthY,
                    endY = l.cervBottom,
                ),
            )
        }
        // Fibres musculaires, à peine visibles.
        val fiber = tissue(UterusPart.MYOMETRIUM, MyoRim).copy(alpha = 0.14f)
        for (i in 0 until 5) {
            val y = l.topY + h * (0.0f + i * 0.06f)
            val arc = Path().apply {
                moveTo(l.cx - l.fundusW * 0.95f, y + h * 0.03f)
                cubicTo(l.cx - l.fundusW * 0.4f, y - h * 0.02f, l.cx + l.fundusW * 0.4f, y - h * 0.02f, l.cx + l.fundusW * 0.95f, y + h * 0.03f)
            }
            drawPath(arc, fiber, style = Stroke(1.2f.dpx(this@drawUterus), cap = StrokeCap.Round))
        }
    }

    drawEndometrium(l, day, tissue)
    drawCervicalCanal(l, day, tissue)

    drawPath(body, tissue(UterusPart.MYOMETRIUM, MyoRim), style = Stroke(1.4f.dpx(this), join = StrokeJoin.Round))
}

private fun DrawScope.drawEndometrium(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    val thickness = CyclePhysiology.endometrium(day)
    val secretory = CyclePhysiology.secretory(day)
    val shedding = CyclePhysiology.shedding(day)
    // La cavité est une fente triangulaire ; la muqueuse la tapisse. Fine après les règles, épaisse en phase lutéale.
    val lining = l.s * (0.006f + 0.032f * thickness)
    val w = l.cavW * (0.5f + 0.5f * thickness)
    val top = l.cavTop + (1f - thickness) * (l.cavApex - l.cavTop) * 0.1f
    fun triangle(width: Float, bow: Float) = Path().apply {
        moveTo(l.cx - width, top)
        quadraticTo(l.cx, top + (l.cavApex - top) * bow, l.cx + width, top)
        quadraticTo(l.cx + width * 0.25f, (top + l.cavApex) / 2f, l.cx, l.cavApex)
        quadraticTo(l.cx - width * 0.25f, (top + l.cavApex) / 2f, l.cx - width, top)
        close()
    }
    val cavity = triangle(w, 0.08f)
    val lumen = triangle(w * (0.42f - 0.22f * thickness), 0.04f)
    val base = lerp(lerp(EndoProliferative, EndoSecretory, secretory), EndoShedding, shedding * 0.8f)
    val color = tissue(UterusPart.ENDOMETRIUM, base)
    // La muqueuse : un liseré épais autour de la cavité, plein à l'intérieur.
    drawPath(cavity, color, style = Stroke(lining * 2f, join = StrokeJoin.Round))
    drawPath(cavity, Brush.radialGradient(listOf(lerp(color, Color.White, 0.12f), color), center = Offset(l.cx, top + (l.cavApex - top) * 0.3f), radius = w * 1.2f))

    // Glandes : droites en phase folliculaire, sinueuses en phase lutéale.
    val gland = tissue(UterusPart.ENDOMETRIUM, GlandTint).copy(alpha = 0.35f + 0.25f * thickness)
    val count = 5 + (thickness * 4).roundToInt()
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until count) {
            val t = (i + 0.5f) / count
            val start = Offset(l.cx + side * (w * (1f - t) + lining * 0.85f), top + (l.cavApex - top) * t - lining * 0.2f)
            val end = Offset(l.cx + side * (w * (1f - t) * 0.55f), top + (l.cavApex - top) * t + lining * 0.1f)
            val wiggle = secretory * lining * 0.22f
            val g = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    start.x + (end.x - start.x) * 0.33f, start.y + wiggle,
                    start.x + (end.x - start.x) * 0.66f, end.y - wiggle,
                    end.x, end.y,
                )
            }
            drawPath(g, gland, style = Stroke(0.9f.dpx(this), cap = StrokeCap.Round))
        }
    }
    // Artères spiralées, visibles quand la muqueuse se prépare (phase lutéale).
    if (secretory > 0.05f) {
        val artery = tissue(UterusPart.ENDOMETRIUM, ArteryTint).copy(alpha = 0.45f * secretory)
        for (side in listOf(-1f, 1f)) {
            for (i in 1..3) {
                val t = i / 4f
                val x = l.cx + side * (w * (1f - t) + lining * 0.6f)
                val y = top + (l.cavApex - top) * t
                val spiral = Path().apply {
                    moveTo(x, y)
                    for (k in 1..6) {
                        val f = k / 6f
                        lineTo(x - side * lining * 0.6f * f, y + sin(f * 9f) * lining * 0.12f)
                    }
                }
                drawPath(spiral, artery, style = Stroke(0.9f.dpx(this), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
    // La cavité elle-même : une fente claire, teintée pendant les règles.
    val slit = lerp(lerp(GlandTint, color, 0.3f), tissue(UterusPart.ENDOMETRIUM, EndoShedding), shedding)
    drawPath(lumen, slit)
    drawPath(lumen, lerp(color, MyoRim, 0.3f).copy(alpha = 0.6f), style = Stroke(0.8f.dpx(this), join = StrokeJoin.Round))
}

private fun DrawScope.drawCervicalCanal(l: UterusLayout, day: Float, tissue: (UterusPart, Color) -> Color) {
    val h = size.height
    val opening = CyclePhysiology.cervixOpening(day)
    val fertile = CyclePhysiology.fertileMucus(day)
    val shedding = CyclePhysiology.shedding(day)
    val half = l.s * (0.004f + 0.011f * opening)
    val top = l.cavApex - h * 0.005f
    val bottom = l.cervBottom + h * 0.008f
    val canal = Path().apply {
        moveTo(l.cx - half * 0.5f, top)
        cubicTo(l.cx - half * 1.3f, (top + bottom) / 2f, l.cx - half * 1.1f, bottom - h * 0.02f, l.cx - half * 0.8f, bottom)
        lineTo(l.cx + half * 0.8f, bottom)
        cubicTo(l.cx + half * 1.1f, bottom - h * 0.02f, l.cx + half * 1.3f, (top + bottom) / 2f, l.cx + half * 0.5f, top)
        close()
    }
    val mucus = lerp(lerp(MucusOpaque, MucusClear, fertile), EndoShedding, shedding * 0.7f)
    drawPath(canal, tissue(UterusPart.CERVIX, mucus))
    // Reflet de la glaire claire et filante autour de l'ovulation.
    if (fertile > 0.05f) {
        drawLine(
            Color.White.copy(alpha = 0.7f * fertile),
            start = Offset(l.cx - half * 0.25f, top + (bottom - top) * 0.2f),
            end = Offset(l.cx - half * 0.25f, top + (bottom - top) * 0.8f),
            strokeWidth = 1.2f.dpx(this),
            cap = StrokeCap.Round,
        )
    }
    drawPath(canal, tissue(UterusPart.CERVIX, CervixDeep), style = Stroke(0.9f.dpx(this)))
}

/** Liseré doux autour de l'organe sélectionné. */
private fun DrawScope.drawFocusRing(l: UterusLayout, part: UterusPart, focus: Float, color: Color) {
    if (focus <= 0.01f) return
    val h = size.height
    val c = color.copy(alpha = 0.55f * focus)
    val stroke = Stroke(2f.dpx(this), cap = StrokeCap.Round)
    when (part) {
        UterusPart.OVARIES -> for (side in listOf(-1f, 1f)) {
            val pad = 4f.dpx(this)
            drawOval(c, Offset(l.ovaryX(side) - l.ovaryW / 2f - pad, l.ovaryY - l.ovaryH / 2f - pad), Size(l.ovaryW + 2 * pad, l.ovaryH + 2 * pad), style = stroke)
        }
        UterusPart.CERVIX -> drawRoundRect(
            c,
            topLeft = Offset(l.cx - l.cervW * 1.35f, l.isthY + h * 0.01f),
            size = Size(l.cervW * 2.7f, l.cervBottom - l.isthY + h * 0.02f),
            cornerRadius = CornerRadius(l.cervW),
            style = stroke,
        )
        UterusPart.VAGINA -> drawRoundRect(
            c,
            topLeft = Offset(l.cx - l.vagW * 1.25f, l.cervBottom - h * 0.07f),
            size = Size(l.vagW * 2.5f, l.vagBottom - l.cervBottom + h * 0.08f),
            cornerRadius = CornerRadius(l.vagW * 0.6f),
            style = stroke,
        )
        // Pour l'utérus, les trompes et l'endomètre, l'estompage des autres organes suffit à montrer la sélection.
        else -> Unit
    }
}
