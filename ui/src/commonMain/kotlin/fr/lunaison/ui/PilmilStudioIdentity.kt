package fr.lunaison.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.lunaison.core.model.tr

/**
 * Emblème « L'Îlot Hors-Ligne » de Pilmil Studio dessiné en vectoriel pur.
 */
@Composable
fun PilmilStudioMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val bgColor = if (darkTheme) Color(0xFF1E1D1A) else Color(0xFFFAF8F4)
    val inkColor = if (darkTheme) Color(0xFFF3EFE6) else Color(0xFF1E1D1A)
    val archColor = if (darkTheme) Color(0xFF4D9A82) else Color(0xFF285D4D)
    val amberColor = if (darkTheme) Color(0xFFF59E0B) else Color(0xFFD97706)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val s = w / 256f
        val center = Offset(w / 2f, w / 2f)

        drawCircle(color = bgColor, radius = 114f * s, center = center)
        drawCircle(
            color = inkColor,
            radius = 114f * s,
            center = center,
            style = Stroke(width = 10f * s),
        )
        drawArc(
            color = archColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(62f * s, 80f * s),
            size = Size(132f * s, 132f * s),
            style = Stroke(width = 22f * s, cap = StrokeCap.Round),
        )
        drawLine(
            color = inkColor,
            start = Offset(56f * s, 176f * s),
            end = Offset(200f * s, 176f * s),
            strokeWidth = 14f * s,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = amberColor,
            radius = 22f * s,
            center = Offset(128f * s, 134f * s),
        )
    }
}

@Composable
fun PilmilLegalBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PilmilStudioMark(size = 40.dp)
            Column {
                Text(
                    text = tr(
                        "Pilmil Studio · Mentions légales",
                        "Pilmil Studio · Legal notice",
                        pl = "Pilmil Studio · Informacje prawne",
                        uk = "Pilmil Studio · Юридична інформація",
                        es = "Pilmil Studio · Aviso legal",
                        de = "Pilmil Studio · Impressum",
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Alfortville, France",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = tr(
                "Éditeur : Pilmil Studio (Alfortville, France).\n" +
                    "Confidentialité : 100 % hors-ligne, aucune permission réseau, aucun traceur, aucun compte.\n" +
                    "Licence : logiciel libre MPL-2.0 · Contact : pilmil.studio@proton.me",
                "Publisher: Pilmil Studio (Alfortville, France).\n" +
                    "Privacy: 100% offline, no network permission, no trackers, no account.\n" +
                    "License: open-source MPL-2.0 · Contact: pilmil.studio@proton.me",
                pl = "Wydawca: Pilmil Studio (Alfortville, Francja).\n" +
                    "Prywatność: 100% offline, brak dostępu do sieci, brak trackerów, brak konta.\n" +
                    "Licencja: otwarte oprogramowanie MPL-2.0 · Kontakt: pilmil.studio@proton.me",
                uk = "Видавець: Pilmil Studio (Альфорвіль, Франція).\n" +
                    "Конфіденційність: 100% офлайн, без доступу до мережі, без трекерів і облікового запису.\n" +
                    "Ліцензія: вільне ПЗ MPL-2.0 · Контакт: pilmil.studio@proton.me",
                es = "Editor: Pilmil Studio (Alfortville, Francia).\n" +
                    "Privacidad: 100 % sin conexión, sin permiso de red, sin rastreadores, sin cuenta.\n" +
                    "Licencia: software libre MPL-2.0 · Contacto: pilmil.studio@proton.me",
                de = "Herausgeber: Pilmil Studio (Alfortville, Frankreich).\n" +
                    "Datenschutz: 100 % offline, keine Netzwerkberechtigung, keine Tracker, kein Konto.\n" +
                    "Lizenz: Freie Software MPL-2.0 · Kontakt: pilmil.studio@proton.me",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
