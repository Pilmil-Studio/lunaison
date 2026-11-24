package fr.lunaison.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.lunaison.core.content.Article
import fr.lunaison.core.content.Articles
import fr.lunaison.core.content.RightsSheet
import fr.lunaison.core.content.RightsSheets
import fr.lunaison.core.content.Source
import fr.lunaison.core.content.Sources
import fr.lunaison.core.cycle.CycleState
import fr.lunaison.core.cycle.Phase
import fr.lunaison.core.designsystem.LunaCard
import fr.lunaison.core.designsystem.SectionTitle
import fr.lunaison.core.model.AgeGroup
import fr.lunaison.core.model.Country
import fr.lunaison.core.model.Pain
import fr.lunaison.core.model.tr
import kotlin.math.PI
import kotlin.math.roundToInt

private enum class LearnCategory {
    ALL,
    CYCLE,
    WELLBEING,
    LIFE,
    ;

    val label: String
        get() = when (this) {
            ALL -> tr("Tout", "All", pl = "Wszystko", uk = "Усе", es = "Todo", de = "Alle")
            CYCLE -> tr("Cycle & hormones", "Cycle & hormones", pl = "Cykl i hormony", uk = "Цикл і гормони", es = "Ciclo y hormonas", de = "Zyklus & Hormone")
            WELLBEING -> tr("Douleurs & bien-être", "Pain & well-being", pl = "Ból i ulga", uk = "Біль і полегшення", es = "Dolor y bienestar", de = "Schmerz & Wohl")
            LIFE -> tr("Santé & quotidien", "Health & daily life", pl = "Zdrowie i codzienność", uk = "Здоров'я та побут", es = "Salud y vida diaria", de = "Gesundheit & Alltag")
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearnScreen(
    onArticle: (String) -> Unit,
    state: AppState? = null,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(LearnCategory.ALL) }

    Page {
        PageTitle(
            tr("Comprendre", "Learn", pl = "Wiedza", uk = "Довідник", es = "Aprender", de = "Wissen"),
            subtitle = tr("Des fiches courtes, relues et sourcées pour mieux vivre son cycle.", "Short, reviewed and referenced guides to better understand your cycle.", pl = "Krótkie, sprawdzone i opatrzone źródłami artykuły, które pomagają lepiej przeżywać cykl.", uk = "Короткі, перевірені та підкріплені джерелами статті, щоб краще проживати свій цикл.", es = "Fichas breves, revisadas y con fuentes para vivir mejor tu ciclo.", de = "Kurze, geprüfte und mit Quellen belegte Artikel, um den eigenen Zyklus besser zu erleben."),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LearnCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label) },
                )
            }
        }

        if (selectedCategory == LearnCategory.ALL && state != null) {
            val contextualIds = buildList {
                val known = state.cycle as? CycleState.Known
                when (known?.phase) {
                    Phase.MENSTRUAL -> {
                        add("douleurs")
                        add("protections")
                    }
                    Phase.LUTEAL -> add("spm")
                    Phase.FOLLICULAR, Phase.OVULATION -> add(if (state.settings.trackOvulationSigns) "symptothermie" else "cycle")
                    Phase.LATE -> add("previsions")
                    null -> add("cycle")
                }
                if (state.settings.trackOvulationSigns) add("symptothermie")
                if (state.settings.pausePredictions || state.settings.baseline.ageGroup == AgeGroup.UNDER_18) add("transitions")
                if (state.settings.irregularCyclesMode) add("perimenopause")
                val hasStrongPain = state.logs.takeLast(14).any { it.pain == Pain.MODERATE || it.pain == Pain.SEVERE }
                if (hasStrongPain) add("endometriose")
            }.distinct().take(3)

            if (contextualIds.isNotEmpty()) {
                SectionTitle(
                    tr(
                        "Pour vous en ce moment",
                        "For you right now",
                        pl = "Dla Ciebie teraz",
                        uk = "Для вас зараз",
                        es = "Para ti ahora",
                        de = "Aktuell für dich",
                    ),
                )
                contextualIds.forEach { id ->
                    LearnEntryCard(id = id, onArticle = onArticle)
                }
            }
        }

        // Encart pédagogique mis en avant pour découvrir les schémas interactifs
        if (selectedCategory == LearnCategory.ALL || selectedCategory == LearnCategory.CYCLE) {
            LunaCard(
                modifier = Modifier.clickable(role = Role.Button) { onArticle("cycle") },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tr("Schémas interactifs : anatomie et cycle", "Interactive diagrams: anatomy and cycle", pl = "Interaktywne schematy: anatomia i cykl", uk = "Інтерактивні схеми: анатомія та цикл", es = "Esquemas interactivos: anatomía y ciclo", de = "Interaktive Diagramme: Anatomie und Zyklus"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            tr("Repères illustrés, organes et vagues hormonales expliqués simplement.", "Illustrated guide, organs and hormonal waves simply explained.", pl = "Ilustrowane wskazówki, narządy i fale hormonalne wyjaśnione prosto.", uk = "Ілюстровані орієнтири, органи та гормональні хвилі, пояснені просто.", es = "Esquemas ilustrados, órganos y oleadas hormonales explicados con sencillez.", de = "Illustrierte Orientierung, Organe und Hormonwellen einfach erklärt."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Catégorie 1 : Le cycle et les hormones
        if (selectedCategory == LearnCategory.ALL || selectedCategory == LearnCategory.CYCLE) {
            SectionTitle(tr("Le cycle et les hormones", "The cycle and hormones", pl = "Cykl i hormony", uk = "Цикл і гормони", es = "El ciclo y las hormonas", de = "Der Zyklus und die Hormone"))
        LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("contraception") }) {
            Text(tr("Contraception & règles : ce qui change", "Contraception & periods: what changes", pl = "Antykoncepcja a miesiączka: co się zmienia", uk = "Контрацепція та менструація: що змінюється", es = "Anticoncepción y regla: qué cambia", de = "Verhütung und Periode: was sich ändert"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Pilule, stérilet au cuivre ou hormonal, implant : pourquoi les saignements sont différents et comment ils fonctionnent.", "Pill, copper or hormonal IUD, implant: why bleeding differs and how it works.", pl = "Tabletka, wkładka miedziana lub hormonalna, implant: dlaczego krwawienia są inne i jak działają te metody.", uk = "Таблетка, мідна чи гормональна спіраль, імплант: чому кровотечі інші та як ці методи діють.", es = "Píldora, DIU de cobre u hormonal, implante: por qué los sangrados son distintos y cómo funcionan.", de = "Pille, Kupfer- oder Hormonspirale, Implantat: warum die Blutungen anders sind und wie sie funktionieren."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val cycleArticles = Articles.all.filter { it.id in listOf("cycle", "previsions", "symptothermie", "transitions") }
        cycleArticles.forEach { article ->
            ArticleCard(article, onArticle)
        }
        LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("sopk") }) {
            Text(tr("Le SOPK (ovaires polykystiques)", "PCOS (polycystic ovary syndrome)", pl = "PCOS (zespół policystycznych jajników)", uk = "СПКЯ (синдром полікістозних яєчників)", es = "El SOP (ovarios poliquísticos)", de = "PCOS (polyzystische Ovarien)"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(
                    fr = "Critères de Rotterdam, ovulations rares, hyperandrogénie et repères au quotidien.",
                    en = "Rotterdam criteria, infrequent ovulation, hyperandrogenism and everyday management.",
                    pl = "Kryteria rotterdamskie, rzadkie owulacje, hiperandrogenizm i wskazówki na co dzień.",
                    uk = "Роттердамські критерії, рідкісні овуляції, гіперандрогенія та орієнтири на щодень.",
                    es = "Criterios de Róterdam, ovulaciones infrecuentes, hiperandrogenismo y pautas del día a día.",
                    de = "Rotterdam-Kriterien, seltene Eisprünge, Hyperandrogenismus und Orientierung im Alltag.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("perimenopause") }) {
            Text(tr("La périménopause et ses changements", "Perimenopause and its changes", pl = "Perimenopauza i jej zmiany", uk = "Перименопауза та її зміни", es = "La perimenopausia y sus cambios", de = "Die Perimenopause und ihre Veränderungen"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(
                    fr = "Cycles irréguliers, bouffées de chaleur, sommeil et transition hormonale expliqués.",
                    en = "Irregular cycles, hot flashes, sleep and hormonal transition explained.",
                    pl = "Nieregularne cykle, uderzenia gorąca, sen i przejście hormonalne wyjaśnione.",
                    uk = "Нерегулярні цикли, припливи жару, сон і гормональний перехід: пояснення.",
                    es = "Ciclos irregulares, sofocos, sueño y transición hormonal explicados.",
                    de = "Unregelmäßige Zyklen, Hitzewallungen, Schlaf und hormoneller Übergang erklärt.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        }

        // Catégorie 2 : Soulagement et bien-être
        if (selectedCategory == LearnCategory.ALL || selectedCategory == LearnCategory.WELLBEING) {
            SectionTitle(tr("Soulagement et bien-être", "Relief and well-being", pl = "Ulga i dobre samopoczucie", uk = "Полегшення та самопочуття", es = "Alivio y bienestar", de = "Linderung und Wohlbefinden"))
        LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("endometriose") }) {
            Text(tr("Endométriose & douleurs pelviennes", "Endometriosis & pelvic pain", pl = "Endometrioza i ból miednicy", uk = "Ендометріоз і тазовий біль", es = "Endometriosis y dolor pélvico", de = "Endometriose und Beckenschmerzen"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(
                    fr = "Comprendre les signaux d'alerte (les 5 « D »), le diagnostic et les pistes de soulagement au quotidien.",
                    en = "Understand warning signs (the 5 'Ds'), diagnosis and everyday relief options.",
                    pl = "Zrozumieć sygnały alarmowe (5 „D”), diagnostykę i sposoby łagodzenia dolegliwości na co dzień.",
                    uk = "Розуміння тривожних сигналів (5 «Д»), діагностики та способів полегшення в повсякденному житті.",
                    es = "Comprender las señales de alerta (las 5 «D»), el diagnóstico y las vías de alivio en el día a día.",
                    de = "Warnzeichen (die 5 „D“), Diagnose und Wege zur Linderung im Alltag verstehen.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("adenomyose") }) {
            Text(tr("L'adénomyose : repères et saignements", "Adenomyosis: bleeding and pain", pl = "Adenomioza: wskazówki i krwawienia", uk = "Аденоміоз: орієнтири та кровотечі", es = "La adenomiosis: pautas y sangrados", de = "Adenomyose: Orientierung und Blutungen"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(
                    fr = "L'endométriose interne du muscle utérin, règles très abondantes, crampes et prise en charge.",
                    en = "Internal endometriosis in the uterine wall, heavy bleeding, cramps and management.",
                    pl = "Endometrioza wewnętrzna mięśnia macicy, bardzo obfite miesiączki, skurcze i postępowanie.",
                    uk = "Внутрішній ендометріоз м'яза матки, дуже рясні менструації, спазми та лікування.",
                    es = "Endometriosis interna del músculo uterino, reglas muy abundantes, calambres y tratamiento.",
                    de = "Innere Endometriose der Gebärmuttermuskulatur, sehr starke Perioden, Krämpfe und Behandlung.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val healthArticles = Articles.all.filter { it.id in listOf("douleurs", "spm", "alimentation") }
        healthArticles.forEach { article ->
            ArticleCard(article, onArticle)
        }

        }

        // Catégorie 3 : Santé, quotidien et entourage
        if (selectedCategory == LearnCategory.ALL || selectedCategory == LearnCategory.LIFE) {
            SectionTitle(tr("Santé, quotidien et entourage", "Health, daily life and loved ones", pl = "Zdrowie, codzienność i otoczenie", uk = "Здоров'я, повсякденне життя та оточення", es = "Salud, día a día y entorno", de = "Gesundheit, Alltag und Umfeld"))
            val lifeArticles = Articles.all.filter { it.id in listOf("consulter", "protections", "entourage") }
            lifeArticles.forEach { article ->
                ArticleCard(article, onArticle)
            }
        }

        // Droits et aides : propres à un pays, distincts de la langue.
        if (selectedCategory == LearnCategory.ALL || selectedCategory == LearnCategory.LIFE) {
            RightsSection(state, onArticle)
        }

        Muted(tr("Ces fiches informent et ne remplacent pas l'avis d'un professionnel de santé.", "These guides inform you and do not replace the advice of a health professional.", pl = "Te artykuły informują i nie zastępują porady osoby z personelu medycznego.", uk = "Ці статті інформують і не замінюють консультацію медичного працівника.", es = "Estas fichas informan y no sustituyen la opinión de un profesional sanitario.", de = "Diese Artikel informieren und ersetzen nicht die Einschätzung von medizinischem Fachpersonal."))
    }
}
@Composable
private fun LearnEntryCard(id: String, onArticle: (String) -> Unit) {
    Articles.byId(id)?.let { article ->
        ArticleCard(article, onArticle)
        return
    }
    when (id) {
        "endometriose" -> LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("endometriose") }) {
            Text(tr("Endométriose & douleurs pelviennes", "Endometriosis & pelvic pain", pl = "Endometrioza i ból miednicy", uk = "Ендометріоз і тазовий біль", es = "Endometriosis y dolor pélvico", de = "Endometriose und Beckenschmerzen"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Comprendre les signaux d'alerte (les 5 « D »), le diagnostic et les pistes de soulagement au quotidien.", "Understand warning signs (the 5 'Ds'), diagnosis and everyday relief options.", pl = "Zrozumieć sygnały alarmowe (5 „D”), diagnostykę i sposoby łagodzenia dolegliwości na co dzień.", uk = "Розуміння тривожних сигналів (5 «Д»), діагностики та способів полегшення в повсякденному житті.", es = "Comprender las señales de alerta (las 5 «D»), el diagnóstico y las vías de alivio en el día a día.", de = "Warnzeichen (die 5 „D“), Diagnose und Wege zur Linderung im Alltag verstehen."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        "perimenopause" -> LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle("perimenopause") }) {
            Text(tr("La périménopause et ses changements", "Perimenopause and its changes", pl = "Perimenopauza i jej zmiany", uk = "Перименопауза та її зміни", es = "La perimenopausia y sus cambios", de = "Die Perimenopause und ihre Veränderungen"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Cycles irréguliers, bouffées de chaleur, sommeil et transition hormonale expliqués.", "Irregular cycles, hot flashes, sleep and hormonal transition explained.", pl = "Nieregularne cykle, uderzenia gorąca, sen i przejście hormonalne wyjaśnione.", uk = "Нерегулярні цикли, припливи жару, сон і гормональний перехід: пояснення.", es = "Ciclos irregulares, sofocos, sueño y transición hormonal explicados.", de = "Unregelmäßige Zyklen, Hitzewallungen, Schlaf und hormoneller Übergang erklärt."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
/** Fiches de droits du pays choisi (ou de la région du téléphone), ou une phrase honnête s'il n'y en a pas. */
@Composable
private fun RightsSection(state: AppState?, onArticle: (String) -> Unit) {
    val country = Country.resolve(state?.settings?.country ?: Country.AUTO)
    val sheets = RightsSheets.forCountry(country)
    if (sheets.isNotEmpty() && country != null) {
        val name = Country.displayName(country)
        SectionTitle(tr("Vos droits ($name)", "Your rights ($name)", pl = "Twoje prawa ($name)", uk = "Ваші права ($name)", es = "Tus derechos ($name)", de = "Deine Rechte ($name)"))
        sheets.forEach { ArticleCard(it.article, onArticle) }
    } else {
        SectionTitle(tr("Vos droits", "Your rights", pl = "Twoje prawa", uk = "Ваші права", es = "Tus derechos", de = "Deine Rechte"))
        Muted(
            tr(
                "Les remboursements et l'accès gratuit à la contraception dépendent du pays. Renseignez-vous auprès de votre système de santé, d'une pharmacie ou d'un centre de santé sexuelle. Vous pouvez choisir un pays dans les réglages.",
                "Reimbursements and free access to contraception depend on the country. Ask your health system, a pharmacy or a sexual health centre. You can choose a country in the settings.",
                pl = "Refundacje i bezpłatny dostęp do antykoncepcji zależą od kraju. Zapytaj w swoim systemie opieki zdrowotnej, w aptece lub w ośrodku zdrowia seksualnego. Kraj możesz wybrać w ustawieniach.",
                uk = "Відшкодування та безкоштовний доступ до контрацепції залежать від країни. Дізнайтеся у своїй системі охорони здоров'я, в аптеці чи центрі сексуального здоров'я. Країну можна обрати в налаштуваннях.",
                es = "Los reembolsos y el acceso gratuito a la anticoncepción dependen del país. Infórmate en tu sistema de salud, en una farmacia o en un centro de salud sexual. Puedes elegir un país en los ajustes.",
                de = "Erstattungen und kostenloser Zugang zu Verhütung hängen vom Land ab. Frag bei deinem Gesundheitssystem, in einer Apotheke oder einem Zentrum für sexuelle Gesundheit nach. Ein Land kannst du in den Einstellungen wählen.",
            ),
        )
    }
}

/** Fiche de droits : comme une fiche, avec le pays, la date de vérification et les sources en liens directs. */
@Composable
private fun RightsSheetScreen(sheet: RightsSheet, onBack: () -> Unit) {
    val context = rememberPlatformContext()
    val article = sheet.article
    Page {
        PageTitle(article.title, onBack = onBack)
        Text(article.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val name = Country.displayName(sheet.country)
        val date = "${Format.dayMonth(sheet.verifiedOn)} ${sheet.verifiedOn.year}"
        Muted(tr("Valable en $name. Vérifié le $date.", "Applies in $name. Checked on $date.", pl = "Dotyczy: $name. Sprawdzono $date.", uk = "Діє: $name. Перевірено $date.", es = "Válido en $name. Comprobado el $date.", de = "Gilt in $name. Geprüft am $date."))
        article.sections.forEach { section ->
            SectionTitle(section.heading)
            section.paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
        SectionTitle(tr("Sources", "Sources", pl = "Źródła", uk = "Джерела", es = "Fuentes", de = "Quellen"))
        article.sources.forEach { source ->
            TextButton(onClick = { openLink(context, source.url) }) {
                Text(source.label, style = MaterialTheme.typography.bodySmall, textDecoration = TextDecoration.Underline)
            }
        }
        Muted(
            tr(
                "Les droits évoluent. En cas de doute, vérifiez sur les sites officiels ou demandez en pharmacie.",
                "Rights change. If in doubt, check the official websites or ask a pharmacist.",
                pl = "Przepisy się zmieniają. W razie wątpliwości sprawdź oficjalne strony lub zapytaj w aptece.",
                uk = "Права змінюються. Якщо маєте сумніви, перевірте на офіційних сайтах або запитайте в аптеці.",
                es = "Los derechos cambian. Si tienes dudas, consulta las webs oficiales o pregunta en la farmacia.",
                de = "Rechte ändern sich. Im Zweifel auf den offiziellen Seiten nachsehen oder in der Apotheke fragen.",
            ),
        )
    }
}

@Composable
private fun ArticleCard(article: Article, onArticle: (String) -> Unit) {
    LunaCard(modifier = Modifier.clickable(role = Role.Button) { onArticle(article.id) }) {
        Text(article.title, style = MaterialTheme.typography.titleMedium)
        Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
fun ArticleScreen(id: String, onBack: () -> Unit) {
    RightsSheets.byId(id)?.let { sheet ->
        RightsSheetScreen(sheet, onBack)
        return
    }
    if (id == "contraception") {
        ContraceptionGuideScreen(onBack = onBack)
        return
    }
    if (id == "endometriose") {
        EndometriosisGuideScreen(onBack = onBack)
        return
    }
    if (id == "sopk") {
        PcosGuideScreen(onBack = onBack)
        return
    }
    if (id == "adenomyose") {
        AdenomyosisGuideScreen(onBack = onBack)
        return
    }
    if (id == "perimenopause") {
        PerimenopauseGuideScreen(onBack = onBack)
        return
    }
    val article = Articles.byId(id)
    val context = rememberPlatformContext()
    Page {
        if (article == null) {
            PageTitle(tr("Fiche introuvable", "Guide not found", pl = "Nie znaleziono artykułu", uk = "Статтю не знайдено", es = "Ficha no encontrada", de = "Artikel nicht gefunden"), onBack = onBack)
            return@Page
        }
        PageTitle(article.title, onBack = onBack)
        Text(article.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Schémas visuels pédagogiques intégrés
        when (id) {
            "cycle" -> CycleVisualGuide()
            "protections" -> ProtectionsWearVisual()
            "douleurs" -> PainMechanismVisual()
            "spm" -> SpmWindowVisual()
            "symptothermie" -> SymptothermalVisual()
        }

        article.sections.forEach { section ->
            SectionTitle(section.heading)
            section.paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
        // Les références ne sont pas dans le texte : un lien discret mène à la page du site qui les détaille.
        if (article.sources.isNotEmpty()) {
            TextButton(onClick = { openLink(context, Links.sourcesOf(article.id)) }) {
                Text(
                    tr("Sources de cette fiche, sur le site", "Sources for this guide, on the website (in French)", pl = "Źródła tego artykułu na stronie", uk = "Джерела цієї статті на сайті", es = "Fuentes de esta ficha, en el sitio web", de = "Quellen zu diesem Artikel auf der Website"),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}
