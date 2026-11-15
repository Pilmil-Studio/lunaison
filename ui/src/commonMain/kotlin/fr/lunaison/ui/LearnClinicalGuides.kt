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
private fun ClinicalGuideSourcesCard(sources: List<Source>) {
    if (sources.isEmpty()) return
    val context = rememberPlatformContext()
    SectionTitle(
        tr(
            "Références scientifiques",
            "Scientific references",
            pl = "Źródła naukowe",
            uk = "Наукові джерела",
            es = "Referencias científicas",
            de = "Wissenschaftliche Quellen",
        ),
    )
    LunaCard {
        sources.forEach { source ->
            TextButton(onClick = { openLink(context, source.url) }) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

/** Types de contraception avec leur logique physiologique. */
enum class ContraceptiveMethod {
    COMBINED_PILL,
    COPPER_IUD,
    IMPLANT_OR_PROGESTIN,
    HORMONAL_IUD,
    NATURAL,
    ;

    val label: String get() = when (this) {
        COMBINED_PILL -> tr("Pilule combinée", "Combined pill", pl = "Pigułka złożona", uk = "Комбінована таблетка", es = "Píldora combinada", de = "Kombinationspille")
        COPPER_IUD -> tr("Stérilet cuivre", "Copper IUD", pl = "Wkładka miedziana", uk = "Мідна спіраль", es = "DIU de cobre", de = "Kupferspirale")
        IMPLANT_OR_PROGESTIN -> tr("Implant / Pilule continue", "Implant / Mini-pill", pl = "Implant / Tabletka ciągła", uk = "Імплантат / Безперервна таблетка", es = "Implante / Píldora continua", de = "Implantat / Dauerpille")
        HORMONAL_IUD -> tr("Stérilet hormonal", "Hormonal IUD", pl = "Wkładka hormonalna", uk = "Гормональна спіраль", es = "DIU hormonal", de = "Hormonspirale")
        NATURAL -> tr("Cycle naturel / Préservatif", "Natural cycle / Condoms", pl = "Cykl naturalny / Prezerwatywa", uk = "Природний цикл / Презерватив", es = "Ciclo natural / Preservativo", de = "Natürlicher Zyklus / Kondom")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContraceptionGuideScreen(onBack: () -> Unit) {
    var selectedMethod by rememberSaveable { mutableStateOf(ContraceptiveMethod.COMBINED_PILL) }
    val context = rememberPlatformContext()

    Page {
        PageTitle(
            tr("Contraception & règles", "Contraception & periods", pl = "Antykoncepcja i miesiączka", uk = "Контрацепція та менструація", es = "Anticoncepción y regla", de = "Verhütung und Periode"),
            onBack = onBack,
            subtitle = tr("Comprendre vos saignements selon votre méthode.", "Understand your bleeding depending on your method.", pl = "Zrozum swoje krwawienia w zależności od metody.", uk = "Зрозумійте свої кровотечі залежно від вашого методу.", es = "Entiende tus sangrados según tu método.", de = "Deine Blutungen je nach Methode verstehen."),
        )

        LunaCard {
            Text(
                tr("Sélectionnez votre moyen de contraception pour voir son fonctionnement :", "Select your contraceptive method to see how it works:", pl = "Wybierz swoją metodę antykoncepcji, aby zobaczyć, jak działa:", uk = "Оберіть свій спосіб контрацепції, щоб побачити, як він діє:", es = "Selecciona tu método anticonceptivo para ver cómo funciona:", de = "Wähle deine Verhütungsmethode, um zu sehen, wie sie funktioniert:"),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ContraceptiveMethod.entries.forEach { method ->
                    val isSelected = method == selectedMethod
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMethod = method },
                        label = {
                            Text(
                                method.label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        // Fiche détaillée mise en valeur pour la méthode sélectionnée
        SectionTitle(tr("Votre méthode : ${selectedMethod.label}", "Your method: ${selectedMethod.label}", pl = "Twoja metoda: ${selectedMethod.label}", uk = "Ваш метод: ${selectedMethod.label}", es = "Tu método: ${selectedMethod.label}", de = "Deine Methode: ${selectedMethod.label}"))
        when (selectedMethod) {
            ContraceptiveMethod.COMBINED_PILL -> CombinedPillDetailsCard()
            ContraceptiveMethod.COPPER_IUD -> CopperIudDetailsCard()
            ContraceptiveMethod.IMPLANT_OR_PROGESTIN -> ImplantDetailsCard()
            ContraceptiveMethod.HORMONAL_IUD -> HormonalIudDetailsCard()
            ContraceptiveMethod.NATURAL -> NaturalDetailsCard()
        }

        SectionTitle(tr("Toutes les méthodes en résumé", "All methods overview", pl = "Wszystkie metody w skrócie", uk = "Усі методи коротко", es = "Todos los métodos en resumen", de = "Alle Methoden im Überblick"))
        ContraceptionComparisonCard()

        SectionTitle(
            tr(
                "En cas d'oubli ou de doute",
                "If you miss a pill or are unsure",
                pl = "W razie pominięcia tabletki lub wątpliwości",
                uk = "У разі пропуску таблетки або сумнівів",
                es = "En caso de olvido o duda",
                de = "Bei vergessener Einnahme oder Unsicherheit",
            ),
        )
        LunaCard {
            Bullet(
                tr(
                    "Pilule combinée : le délai de tolérance est souvent de 12 heures (vérifiez la notice). Au-delà, prenez le comprimé oublié dès que possible et utilisez une protection mécanique pendant 7 jours.",
                    "Combined pill: the usual grace window is up to 12 hours (check your leaflet). Beyond that, take the missed pill as soon as possible and use a barrier method for 7 days.",
                    pl = "Tabletka dwuskładnikowa: dopuszczalne opóźnienie wynosi zwykle do 12 godzin (sprawdź ulotkę). Po tym czasie przyjmij pominiętą tabletkę jak najszybciej i stosuj zabezpieczenie mechaniczne przez 7 dni.",
                    uk = "Комбінована таблетка: допустиме запізнення зазвичай до 12 годин (перевірте інструкцію). Якщо минуло більше часу, прийміть пропущену таблетку якомога швидше та використовуйте бар'єрний метод протягом 7 днів.",
                    es = "Píldora combinada: el margen habitual es de hasta 12 horas (revisa el prospecto). Si pasa más tiempo, toma la píldora olvidada cuanto antes y usa un método de barrera durante 7 días.",
                    de = "Kombinationspille: Das übliche Zeitfenster beträgt bis zu 12 Stunden (Packungsbeilage prüfen). Danach die vergessene Tablette rasch nachnehmen und 7 Tage lang zusätzlich verhüten.",
                ),
            )
            Bullet(
                tr(
                    "Pilule progestative seule : le délai de tolérance peut être très court (3 heures ou 12 heures selon la molécule). Vérifiez la notice de votre plaquette.",
                    "Progestin-only pill: the grace window can be very short (3 hours or 12 hours depending on the formulation). Always check your leaflet.",
                    pl = "Tabletka jednoskładnikowa: okno tolerancji bywa bardzo krótkie (3 lub 12 godzin zależnie od substancji). Sprawdź ulotkę swojego leku.",
                    uk = "Чисто прогестинова таблетка: допустимий інтервал може бути дуже коротким (3 або 12 годин залежно від препарату). Перевірте інструкцію.",
                    es = "Píldora de solo progestágeno: el margen puede ser muy corto (3 o 12 horas según el principio activo). Revisa el prospecto.",
                    de = "Minipille (Gestagen-Monopräparat): Das Zeitfenster kann sehr kurz sein (3 oder 12 Stunden je nach Wirkstoff). Beachte die Packungsbeilage.",
                ),
            )
            Bullet(
                tr(
                    "Contraception d'urgence : en cas de rapport à risque, une contraception d'urgence (en pharmacie ou centre de santé sexuelle) reste possible jusqu'à 3 à 5 jours selon la méthode, et est d'autant plus efficace qu'elle est prise tôt.",
                    "Emergency contraception: after unprotected sex or a high-risk missed pill, emergency contraception (from a pharmacy or sexual health clinic) can be used up to 3 to 5 days depending on the method, and works best the sooner it is taken.",
                    pl = "Antykoncepcja awaryjna: po ryzykownym stosunku lub pominięciu tabletki można zastosować antykoncepcję awaryjną do 3–5 dni zależnie od metody (w aptece lub po konsultacji, zgodnie z lokalnymi przepisami); im szybciej, tym większa skuteczność.",
                    uk = "Екстрена контрацепція: після ризикованого контакту її можна застосувати протягом 3-5 днів залежно від методу (в аптеці чи центрі сексуального здоров'я); чим раніше, тим вища ефективність.",
                    es = "Anticoncepción de urgencia: tras una relación de riesgo o un olvido, puede utilizarse hasta 3 o 5 días después según el método (en farmacia o centro de salud sexual); es más eficaz cuanto antes se tome.",
                    de = "Notfallverhütung: Nach ungeschütztem Verkehr oder einem Einnahmefehler ist sie je nach Methode bis zu 3 bis 5 Tage möglich (Apotheke oder Beratungsstelle) und wirkt umso besser, je früher sie erfolgt.",
                ),
            )
        }

        TextButton(onClick = {
            openLink(
                context,
                tr(
                    fr = "https://www.questionsexualite.fr/choisir-sa-contraception",
                    en = "https://www.nhs.uk/contraception/",
                    pl = "https://ponton.org.pl/antykoncepcja/",
                    uk = "https://www.nhs.uk/contraception/",
                    es = "https://sedra-fpfe.org/",
                    de = "https://www.familienplanung.de/verhuetung/",
                ),
            )
        }) {
            Text(
                tr(
                    fr = "Guides officiels de Santé publique France (QuestionSexualité.fr)",
                    en = "Official NHS Contraception guide (nhs.uk)",
                    pl = "Grupa Ponton : Edukacja seksualna i antykoncepcja (ponton.org.pl)",
                    uk = "Керівництво з контрацепції NHS (nhs.uk)",
                    es = "Guía de anticoncepción SEDRA - Planificación Familiar (sedra-fpfe.org)",
                    de = "Offizielle Verhütungsinformationen der BZgA (familienplanung.de)",
                ),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
private fun CombinedPillDetailsCard() {
    LunaCard {
        Text(
            tr("Ce que sont vos saignements : des hémorragies de privation", "What your bleeding is: withdrawal bleeding", pl = "Czym są Twoje krwawienia: krwawienia z odstawienia", uk = "Що таке ваші кровотечі: кровотечі відміни", es = "Qué son tus sangrados: sangrados por deprivación", de = "Was deine Blutungen sind: Abbruchblutungen"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            tr(
                fr = "Sous pilule combinée (œstrogène + progestatif), vous n'avez pas de véritables règles. La pilule bloque l'ovulation. Les saignements lors de la semaine de pause (ou des comprimés placebo) sont déclenchés artificiellement par l'arrêt temporaire des hormones de synthèse.",
                en = "Under a combined pill, you do not have genuine menstrual periods. The pill stops ovulation. Bleeding during the pill-free week (or the placebo pills) is artificially triggered by the temporary stop of synthetic hormones.",
                pl = "W przypadku złożonej tabletki antykoncepcyjnej (estrogen + gestagen) nie masz prawdziwej miesiączki. Tabletka blokuje owulację. Krwawienia w tygodniu przerwy (lub podczas przyjmowania tabletek placebo) są wywoływane sztucznie przez czasowe odstawienie hormonów syntetycznych.",
                uk = "Під час прийому комбінованої таблетки справжніх менструацій немає. Таблетка блокує овуляцію. Кровотечі в тижневу перерву (або під час прийому плацебо) штучно викликає тимчасова відміна синтетичних гормонів.",
                es = "Con la píldora combinada (estrógeno + progestágeno) no tienes una regla verdadera. La píldora bloquea la ovulación. El sangrado de la semana de descanso (o de las pastillas de placebo) lo provoca artificialmente la interrupción temporal de las hormonas sintéticas.",
                de = "Unter der kombinierten Pille (Östrogen + Gestagen) hast du keine echte Periode. Die Pille verhindert den Eisprung. Die Blutung in der Pillenpause (oder bei den Placebo-Tabletten) wird künstlich durch das vorübergehende Absetzen der synthetischen Hormone ausgelöst.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Enchaîner sans pause :", "Taking packs back-to-back:", pl = "Przyjmowanie bez przerwy:", uk = "Приймання без перерви:", es = "Encadenar sin pausa:", de = "Ohne Pause durchnehmen:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Enchaîner deux plaquettes sans pause de 7 jours supprime tout saignement. C'est médicalement sans danger et validé par les sociétés savantes (OMS, HAS) : le sang ne s'accumule nulle part.",
                en = "Taking two pill packs back-to-back without the 7-day break suppresses bleeding entirely. It is medically safe and endorsed by medical societies (WHO, HAS): blood does not build up anywhere.",
                pl = "Przyjmowanie dwóch opakowań jedno po drugim, bez 7-dniowej przerwy, eliminuje wszelkie krwawienia. Jest to medycznie bezpieczne i zatwierdzone przez towarzystwa naukowe (WHO, HAS): krew nigdzie się nie gromadzi.",
                uk = "Приймання двох блістерів поспіль без 7-денної перерви усуває будь-які кровотечі. Це медично безпечно й підтверджено науковими товариствами (ВООЗ, HAS): кров ніде не накопичується.",
                es = "Encadenar dos blísteres sin la pausa de 7 días suprime cualquier sangrado. Es médicamente seguro y está respaldado por las sociedades científicas (OMS, HAS): la sangre no se acumula en ningún sitio.",
                de = "Zwei Packungen ohne die 7-tägige Pause hintereinander einzunehmen unterdrückt jede Blutung. Das ist medizinisch unbedenklich und von Fachgesellschaften (WHO, HAS) bestätigt: Das Blut sammelt sich nirgendwo an.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Dans Lunaison :", "In Lunaison:", pl = "W Lunaison:", uk = "У Lunaison:", es = "En Lunaison:", de = "In Lunaison:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Vous pouvez noter vos prises quotidiennes et vos saignements. Les prévisions de cycle naturel (ovulation estimée) n'ont pas de valeur sous pilule combinée puisque c'est le calendrier de la plaquette qui fait foi.",
                en = "You can log daily intake and bleeding. Natural cycle forecasts (estimated ovulation) are not biologically applicable under the combined pill as the pack schedule dictates bleeding.",
                pl = "Możesz zapisywać codzienne przyjmowanie tabletek i krwawienia. Prognozy cyklu naturalnego (szacowana owulacja) nie mają wartości przy złożonej tabletce antykoncepcyjnej, ponieważ decyduje harmonogram opakowania.",
                uk = "Ви можете записувати щоденний прийом і кровотечі. Прогнози природного циклу (орієнтовна овуляція) не мають значення при комбінованій таблетці, оскільки вирішальним є графік блістера.",
                es = "Puedes anotar las tomas diarias y los sangrados. Las previsiones del ciclo natural (ovulación estimada) no tienen valor con la píldora combinada, ya que lo que manda es el calendario del blíster.",
                de = "Du kannst deine täglichen Einnahmen und Blutungen eintragen. Die Vorhersagen des natürlichen Zyklus (geschätzter Eisprung) sind unter der kombinierten Pille nicht aussagekräftig, da der Kalender der Packung maßgeblich ist.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CopperIudDetailsCard() {
    LunaCard {
        Text(
            tr("Ce que sont vos saignements : de vraies règles naturelles", "What your bleeding is: genuine natural periods", pl = "Czym są Twoje krwawienia: prawdziwe, naturalne miesiączki", uk = "Що таке ваші кровотечі: справжні природні менструації", es = "Qué son tus sangrados: reglas naturales de verdad", de = "Was deine Blutungen sind: echte, natürliche Perioden"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            tr(
                fr = "Le stérilet au cuivre (DIU cuivre) est 100 % sans hormone. Votre cycle biologique naturel, vos variations hormonales et votre ovulation se poursuivent exactement comme d'habitude.",
                en = "The copper IUD is 100% hormone-free. Your natural cycle, hormonal variations and ovulation proceed normally.",
                pl = "Miedziana wkładka domaciczna jest w 100 % wolna od hormonów. Twój naturalny cykl biologiczny, wahania hormonalne i owulacja przebiegają dokładnie tak jak zwykle.",
                uk = "Мідна спіраль (мідна ВМС) на 100 % не містить гормонів. Ваш природний біологічний цикл, гормональні коливання та овуляція тривають так само, як і раніше.",
                es = "El DIU de cobre es 100 % libre de hormonas. Tu ciclo biológico natural, tus variaciones hormonales y tu ovulación continúan exactamente como siempre.",
                de = "Die Kupferspirale ist zu 100 % hormonfrei. Dein natürlicher biologischer Zyklus, deine hormonellen Schwankungen und dein Eisprung laufen genau wie gewohnt weiter.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("L'inflammation bénigne et les flux abondants :", "Benign inflammation and heavy bleeding:", pl = "Łagodny stan zapalny i obfite krwawienia:", uk = "Доброякісне запалення та рясні виділення:", es = "La inflamación benigna y los flujos abundantes:", de = "Die gutartige Entzündung und starke Blutungen:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Le cuivre diffuse de petites quantités d'ions cuivre qui provoquent une réaction inflammatoire locale bénigne de l'endomètre (spermicide). C'est pour cela que les règles sont souvent plus abondantes, plus longues et plus douloureuses les 3 à 6 premiers mois.",
                en = "Copper releases ions causing a localized, benign inflammatory response in the endometrium (spermicidal). This is why periods are frequently heavier, longer and more crampy during the first 3 to 6 months.",
                pl = "Miedź uwalnia niewielkie ilości jonów miedzi, które wywołują łagodną miejscową reakcję zapalną endometrium (działanie plemnikobójcze). Dlatego miesiączki są często obfitsze, dłuższe i bardziej bolesne przez pierwsze 3 do 6 miesięcy.",
                uk = "Мідь вивільняє невеликі кількості іонів міді, що спричиняють доброякісну локальну запальну реакцію ендометрія (сперміцидна дія). Саме тому менструації часто бувають ряснішими, довшими та болючішими в перші 3-6 місяців.",
                es = "El cobre libera pequeñas cantidades de iones de cobre que provocan una reacción inflamatoria local benigna del endometrio (espermicida). Por eso las reglas suelen ser más abundantes, más largas y más dolorosas durante los 3 a 6 primeros meses.",
                de = "Das Kupfer gibt kleine Mengen Kupferionen ab, die eine lokale, gutartige Entzündungsreaktion der Gebärmutterschleimhaut hervorrufen (spermienabtötend). Deshalb sind die Perioden in den ersten 3 bis 6 Monaten oft stärker, länger und schmerzhafter.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Vigilance fer et anémie :", "Iron monitoring and fatigue:", pl = "Czujność w sprawie żelaza i niedokrwistości:", uk = "Увага до заліза та анемії:", es = "Vigilancia del hierro y la anemia:", de = "Eisenmangel und Blutarmut im Blick:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Des règles très abondantes entraînent une perte de fer. Privilégiez les aliments riches en fer et parlez-en à un soignant pour un bilan sanguin (ferritine) si vous ressentez une grande fatigue ou des vertiges.",
                en = "Heavier periods increase iron loss. Favor iron-rich foods and ask your clinician for a ferritin blood test if you experience unusual fatigue or dizziness.",
                pl = "Bardzo obfite miesiączki powodują utratę żelaza. Wybieraj produkty bogate w żelazo i porozmawiaj z osobą z personelu medycznego o badaniu krwi (ferrytyna), jeśli odczuwasz duże zmęczenie lub zawroty głowy.",
                uk = "Дуже рясні менструації призводять до втрати заліза. Віддавайте перевагу продуктам, багатим на залізо, і поговоріть із медичним працівником про аналіз крові (феритин), якщо відчуваєте сильну втому або запаморочення.",
                es = "Las reglas muy abundantes provocan una pérdida de hierro. Da preferencia a los alimentos ricos en hierro y habla con un profesional sanitario para hacerte un análisis de sangre (ferritina) si sientes mucho cansancio o mareos.",
                de = "Sehr starke Perioden führen zu Eisenverlust. Bevorzuge eisenreiche Lebensmittel und sprich mit medizinischem Fachpersonal über eine Blutuntersuchung (Ferritin), wenn du dich sehr müde fühlst oder dir schwindlig ist.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ImplantDetailsCard() {
    LunaCard {
        Text(
            tr("Ce que sont vos saignements : un endomètre au repos complet", "What your bleeding is: resting endometrium", pl = "Czym są Twoje krwawienia: endometrium w całkowitym spoczynku", uk = "Що таке ваші кровотечі: ендометрій у повному спокої", es = "Qué son tus sangrados: un endometrio en reposo completo", de = "Was deine Blutungen sind: eine Gebärmutterschleimhaut in völliger Ruhe"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            tr(
                fr = "L'implant et la pilule progestative continue (ex. désogestrel) diffusent un progestatif en continu, sans aucune interruption. L'ovulation est le plus souvent bloquée et la muqueuse utérine reste très fine.",
                en = "The implant and progestin-only pill continuously release progestin without breaks. Ovulation is mostly suppressed and the uterine lining remains very thin.",
                pl = "Implant i ciągła tabletka gestagenowa (np. dezogestrel) uwalniają gestagen w sposób ciągły, bez żadnej przerwy. Owulacja jest najczęściej blokowana, a błona śluzowa macicy pozostaje bardzo cienka.",
                uk = "Імплант і безперервна прогестинова таблетка (напр. дезогестрел) постійно вивільняють прогестаген без жодних перерв. Овуляція здебільшого блокується, а слизова оболонка матки залишається дуже тонкою.",
                es = "El implante y la píldora de progestágeno continua (p. ej., desogestrel) liberan un progestágeno de forma continua, sin ninguna interrupción. La ovulación suele quedar bloqueada y la mucosa uterina se mantiene muy fina.",
                de = "Das Implantat und die durchgehend eingenommene Gestagen-Pille (z. B. Desogestrel) geben ohne jede Unterbrechung kontinuierlich ein Gestagen ab. Der Eisprung wird meist unterdrückt, und die Gebärmutterschleimhaut bleibt sehr dünn.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Disparition des règles ou saignements irréguliers :", "No periods or irregular spotting:", pl = "Brak miesiączek lub nieregularne krwawienia:", uk = "Зникнення менструацій або нерегулярні кровотечі:", es = "Desaparición de la regla o sangrados irregulares:", de = "Ausbleibende Periode oder unregelmäßige Blutungen:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Environ une femme sur trois n'a plus du tout de saignements (aménorrhée). C'est totalement normal et sans danger pour la fertilité future. D'autres personnes connaissent de petits saignements légers et anarchiques (spotting).",
                en = "About one in three women stops having periods entirely. This is completely normal and safe for future fertility. Others experience light, irregular spotting.",
                pl = "Około co trzecia kobieta nie ma już żadnych krwawień (brak miesiączki, amenorrhea). To całkowicie normalne i bezpieczne dla przyszłej płodności. Inne osoby mają niewielkie, nieregularne krwawienia (plamienia).",
                uk = "Приблизно в кожної третьої жінки кровотеч більше немає зовсім (аменорея). Це абсолютно нормально та безпечно для майбутньої фертильності. В інших бувають невеликі нерегулярні кров'янисті виділення (спотинг).",
                es = "Aproximadamente una de cada tres mujeres deja de tener sangrados por completo (amenorrea). Es totalmente normal y no supone ningún riesgo para la fertilidad futura. Otras personas tienen pequeños sangrados ligeros e irregulares (manchado).",
                de = "Etwa jede dritte Frau hat gar keine Blutungen mehr (Amenorrhoe). Das ist völlig normal und für die spätere Fruchtbarkeit unbedenklich. Andere haben leichte, unregelmäßige Schmierblutungen (Spotting).",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Dans Lunaison :", "In Lunaison:", pl = "W Lunaison:", uk = "У Lunaison:", es = "En Lunaison:", de = "In Lunaison:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Le mode « Mettre les prévisions en pause » dans les réglages est idéal si vos règles ont disparu, pour continuer à noter vos symptômes sans alertes de retard inutiles.",
                en = "The \"Pause predictions\" mode in settings is ideal if your periods have stopped, allowing you to log symptoms without unneeded late period alerts.",
                pl = "Tryb „Wstrzymaj prognozy” w ustawieniach jest idealny, jeśli miesiączki ustały, aby nadal zapisywać objawy bez niepotrzebnych alertów o opóźnieniu.",
                uk = "Режим «Призупинити прогнози» в налаштуваннях ідеально підходить, якщо менструації зникли: ви продовжуєте записувати симптоми без зайвих сповіщень про затримку.",
                es = "El modo «Pausar las previsiones» de los ajustes es ideal si tu regla ha desaparecido, para seguir anotando tus síntomas sin alertas de retraso innecesarias.",
                de = "Der Modus „Vorhersagen pausieren“ in den Einstellungen ist ideal, wenn deine Periode ausgeblieben ist: So kannst du weiter Symptome eintragen, ohne unnötige Verspätungswarnungen.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HormonalIudDetailsCard() {
    LunaCard {
        Text(
            tr("Ce que sont vos saignements : des flux fortement diminués", "What your bleeding is: significantly reduced bleeding", pl = "Czym są Twoje krwawienia: znacznie zmniejszone krwawienia", uk = "Що таке ваші кровотечі: значно зменшені виділення", es = "Qué son tus sangrados: flujos muy disminuidos", de = "Was deine Blutungen sind: stark verminderte Blutungen"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            tr(
                fr = "Le stérilet hormonal (DIU au lévonorgestrel) agit localement dans l'utérus. L'endomètre ne s'épaissit quasiment plus chaque mois. Les règles deviennent très légères, courtes, et disparaissent souvent complètement après 6 à 12 mois.",
                en = "The hormonal IUD acts locally inside the uterus. The endometrium barely thickens. Periods become very light, short, and frequently disappear entirely after 6 to 12 months.",
                pl = "Hormonalna wkładka domaciczna (z lewonorgestrelem) działa miejscowo w macicy. Endometrium prawie już nie pogrubia się co miesiąc. Miesiączki stają się bardzo skąpe i krótkie, a często zanikają całkowicie po 6 do 12 miesiącach.",
                uk = "Гормональна спіраль (ВМС із левоноргестрелом) діє локально в матці. Ендометрій майже перестає потовщуватися щомісяця. Менструації стають дуже скупими й короткими та часто повністю зникають через 6-12 місяців.",
                es = "El DIU hormonal (con levonorgestrel) actúa localmente en el útero. El endometrio casi deja de engrosarse cada mes. Las reglas se vuelven muy ligeras y cortas, y con frecuencia desaparecen por completo tras 6 a 12 meses.",
                de = "Die Hormonspirale (mit Levonorgestrel) wirkt lokal in der Gebärmutter. Die Gebärmutterschleimhaut baut sich kaum noch jeden Monat auf. Die Perioden werden sehr schwach und kurz und bleiben nach 6 bis 12 Monaten oft ganz aus.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Les premiers mois :", "The first few months:", pl = "Pierwsze miesiące:", uk = "Перші місяці:", es = "Los primeros meses:", de = "Die ersten Monate:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Des petits saignements de repérage (spotting) fréquents sont courants au cours des premières semaines, le temps que la muqueuse s'adapte à l'action locale du progestatif.",
                en = "Frequent light spotting is common in the first weeks while the uterine lining adjusts to the localized progestin.",
                pl = "W pierwszych tygodniach często występują niewielkie plamienia (spotting), gdy błona śluzowa dostosowuje się do miejscowego działania gestagenu.",
                uk = "У перші тижні часто бувають невеликі кров'янисті виділення (спотинг), поки слизова оболонка пристосовується до місцевої дії прогестагену.",
                es = "Es habitual tener pequeños sangrados frecuentes (manchado) durante las primeras semanas, mientras la mucosa se adapta a la acción local del progestágeno.",
                de = "In den ersten Wochen sind häufige leichte Schmierblutungen (Spotting) üblich, bis sich die Schleimhaut an die lokale Wirkung des Gestagens angepasst hat.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NaturalDetailsCard() {
    LunaCard {
        Text(
            tr("Ce que sont vos saignements : un cycle naturel et spontané", "What your bleeding is: a spontaneous natural cycle", pl = "Czym są Twoje krwawienia: spontaniczny cykl naturalny", uk = "Що таке ваші кровотечі: природний спонтанний цикл", es = "Qué son tus sangrados: un ciclo natural y espontáneo", de = "Was deine Blutungen sind: ein natürlicher, spontaner Zyklus"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            tr(
                fr = "Sans contraception hormonale ni DIU (préservatifs, méthode barrière, symptothermie ou pas de contraception), votre cycle est gouverné par vos propres hormones. Les règles surviennent environ 14 jours après une ovulation naturelle.",
                en = "Without hormonal contraception or an IUD (condoms, barrier methods, symptothermy, or no contraception), your cycle is governed by your own hormones. Periods arrive ~14 days after natural ovulation.",
                pl = "Bez antykoncepcji hormonalnej ani wkładki domacicznej (prezerwatywy, metody barierowe, metoda objawowo-termiczna lub brak antykoncepcji) Twój cykl jest sterowany przez Twoje własne hormony. Miesiączka pojawia się około 14 dni po naturalnej owulacji.",
                uk = "Без гормональної контрацепції та ВМС (презервативи, бар'єрні методи, симптотермальний метод або відсутність контрацепції) ваш цикл керується вашими власними гормонами. Менструація настає приблизно через 14 днів після природної овуляції.",
                es = "Sin anticoncepción hormonal ni DIU (preservativos, método de barrera, sintotérmico o sin anticoncepción), tu ciclo está gobernado por tus propias hormonas. La regla llega unos 14 días después de una ovulación natural.",
                de = "Ohne hormonelle Verhütung und ohne Spirale (Kondome, Barrieremethoden, symptothermale Methode oder keine Verhütung) wird dein Zyklus von deinen eigenen Hormonen gesteuert. Die Periode setzt etwa 14 Tage nach einem natürlichen Eisprung ein.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            tr("Dans Lunaison :", "In Lunaison:", pl = "W Lunaison:", uk = "У Lunaison:", es = "En Lunaison:", de = "In Lunaison:"),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            tr(
                fr = "Les calculs de durée habituelle, la fenêtre fertile estimée, les analyses de phases et le suivi de la glaire cervicale ou de la température s'appliquent pleinement à votre profil.",
                en = "Usual duration calculations, estimated fertile windows, phase patterns and mucus/temperature tracking fully apply to your biological profile.",
                pl = "Obliczenia typowej długości, szacowane dni płodne, analizy faz oraz śledzenie śluzu szyjkowego lub temperatury w pełni dotyczą Twojego profilu.",
                uk = "Розрахунки звичної тривалості, орієнтовне фертильне вікно, аналіз фаз і відстеження цервікального слизу чи температури повною мірою стосуються вашого профілю.",
                es = "Los cálculos de duración habitual, la ventana fértil estimada, los análisis de fases y el seguimiento del moco cervical o de la temperatura se aplican plenamente a tu perfil.",
                de = "Die Berechnung der üblichen Dauer, das geschätzte fruchtbare Fenster, die Phasenanalysen und die Beobachtung von Zervixschleim oder Temperatur gelten uneingeschränkt für dein Profil.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ContraceptionComparisonCard() {
    LunaCard {
        ComparisonRow(
            title = tr("Pilule combinée", "Combined pill", pl = "Złożona tabletka antykoncepcyjna", uk = "Комбінована таблетка", es = "Píldora combinada", de = "Kombinierte Pille"),
            summary = tr("Hémorragie de privation (pas de vraie règle). Pas d'ovulation. Possibilité d'enchaîner.", "Withdrawal bleeding (not real period). No ovulation. Can take back-to-back.", pl = "Krwawienie z odstawienia (nie prawdziwa miesiączka). Brak owulacji. Można przyjmować bez przerwy.", uk = "Кровотеча відміни (не справжня менструація). Овуляції немає. Можна приймати без перерви.", es = "Sangrado por deprivación (no es una regla verdadera). Sin ovulación. Se puede encadenar.", de = "Abbruchblutung (keine echte Periode). Kein Eisprung. Durchnehmen ist möglich."),
        )
        ComparisonRow(
            title = tr("Stérilet cuivre", "Copper IUD", pl = "Miedziana wkładka", uk = "Мідна спіраль", es = "DIU de cobre", de = "Kupferspirale"),
            summary = tr("Vraies règles naturelles. Règles souvent plus abondantes et crampes au début (inflammation locale bénigne). Surveiller le fer.", "Natural periods. Frequently heavier flow & cramps initially (benign local inflammation). Watch iron.", pl = "Prawdziwe, naturalne miesiączki. Na początku miesiączki często obfitsze i skurcze (łagodny miejscowy stan zapalny). Obserwować poziom żelaza.", uk = "Справжні природні менструації. На початку менструації часто рясніші та зі спазмами (доброякісне локальне запалення). Стежити за залізом.", es = "Reglas naturales de verdad. Reglas a menudo más abundantes y calambres al principio (inflamación local benigna). Vigilar el hierro.", de = "Echte, natürliche Perioden. Anfangs oft stärkere Perioden und Krämpfe (gutartige lokale Entzündung). Auf das Eisen achten."),
        )
        ComparisonRow(
            title = tr("Implant / Pilule continue", "Implant / Mini-pill", pl = "Implant / Tabletka ciągła", uk = "Імплант / Безперервна таблетка", es = "Implante / Píldora continua", de = "Implantat / Minipille"),
            summary = tr("Endomètre au repos. Règles souvent absentes (aménorrhée sans danger) ou spotting irrégulier.", "Resting endometrium. Often no periods (safe amenorrhea) or irregular spotting.", pl = "Endometrium w spoczynku. Miesiączki często nie występują (niegroźny brak miesiączki) lub występuje nieregularne plamienie.", uk = "Ендометрій у спокої. Менструацій часто немає (безпечна аменорея) або є нерегулярний спотинг.", es = "Endometrio en reposo. A menudo no hay regla (amenorrea sin peligro) o hay manchado irregular.", de = "Gebärmutterschleimhaut in Ruhe. Periode oft ausbleibend (harmlose Amenorrhoe) oder unregelmäßige Schmierblutungen."),
        )
        ComparisonRow(
            title = tr("Stérilet hormonal", "Hormonal IUD", pl = "Hormonalna wkładka", uk = "Гормональна спіраль", es = "DIU hormonal", de = "Hormonspirale"),
            summary = tr("Action locale. Diminution drastique des règles, disparition fréquente au bout d'un an.", "Local action. Drastic flow reduction, often complete cessation after one year.", pl = "Działanie miejscowe. Drastyczne zmniejszenie miesiączek, często całkowity zanik po roku.", uk = "Локальна дія. Різке зменшення менструацій, часто повне зникнення за рік.", es = "Acción local. Disminución drástica de las reglas, desaparición frecuente al cabo de un año.", de = "Lokale Wirkung. Drastische Abnahme der Periode, nach etwa einem Jahr häufig ganz ausbleibend."),
        )
    }
}

@Composable
private fun ComparisonRow(title: String, summary: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable

fun EndometriosisGuideScreen(onBack: () -> Unit) {
    val context = rememberPlatformContext()

    Page {
        PageTitle(
            tr("Endométriose & douleurs pelviennes", "Endometriosis & pelvic pain", pl = "Endometrioza i ból miednicy", uk = "Ендометріоз і тазовий біль", es = "Endometriosis y dolor pélvico", de = "Endometriose und Beckenschmerzen"),
            onBack = onBack,
            subtitle = tr("Repères, signaux d'alerte et pistes de soulagement.", "Indicators, warning signs and everyday relief.", pl = "Wskazówki, sygnały alarmowe i sposoby łagodzenia dolegliwości.", uk = "Орієнтири, тривожні сигнали та способи полегшення.", es = "Pautas, señales de alerta y vías de alivio.", de = "Orientierung, Warnzeichen und Wege zur Linderung."),
        )

        SectionTitle(tr("Qu'est-ce que l'endométriose ?", "What is endometriosis?", pl = "Czym jest endometrioza?", uk = "Що таке ендометріоз?", es = "¿Qué es la endometriosis?", de = "Was ist Endometriose?"))
        LunaCard {
            Text(
                tr(
                    fr = "L'endométriose est une affection gynécologique chronique dans laquelle du tissu semblable à la muqueuse utérine (l'endomètre) se développe hors de l'utérus (sur les ovaires, les trompes, les ligaments utérins, le péritoine, la vessie ou les intestins).",
                    en = "Endometriosis is a chronic condition in which tissue similar to the lining of the uterus (the endometrium) grows outside the uterus (on ovaries, tubes, ligaments, peritoneum, bladder or bowels).",
                    pl = "Endometrioza to przewlekła choroba ginekologiczna, w której tkanka podobna do błony śluzowej macicy (endometrium) rozwija się poza macicą (na jajnikach, jajowodach, więzadłach macicy, otrzewnej, pęcherzu lub jelitach).",
                    uk = "Ендометріоз є хронічним гінекологічним захворюванням, за якого тканина, подібна до слизової оболонки матки (ендометрія), розростається поза маткою (на яєчниках, маткових трубах, зв'язках матки, очеревині, сечовому міхурі або кишківнику).",
                    es = "La endometriosis es una afección ginecológica crónica en la que un tejido semejante a la mucosa uterina (el endometrio) crece fuera del útero (en los ovarios, las trompas, los ligamentos uterinos, el peritoneo, la vejiga o los intestinos).",
                    de = "Endometriose ist eine chronische gynäkologische Erkrankung, bei der gebärmutterschleimhautähnliches Gewebe (Endometrium) außerhalb der Gebärmutter wächst (an Eierstöcken, Eileitern, Gebärmutterbändern, Bauchfell, Blase oder Darm).",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Sous l'influence des hormones du cycle, ces lésions s'épaississent et saignent à chaque cycle, provoquant une inflammation locale, des adhérences et des douleurs.",
                    en = "Influenced by hormonal variations, these lesions thicken and bleed each cycle, causing local inflammation, adhesions and pain.",
                    pl = "Pod wpływem hormonów cyklu zmiany te pogrubiają się i krwawią w każdym cyklu, powodując miejscowy stan zapalny, zrosty i ból.",
                    uk = "Під впливом гормонів циклу ці осередки потовщуються й кровоточать у кожному циклі, спричиняючи локальне запалення, спайки та біль.",
                    es = "Bajo la influencia de las hormonas del ciclo, estas lesiones se engrosan y sangran en cada ciclo, provocando inflamación local, adherencias y dolor.",
                    de = "Unter dem Einfluss der Zyklushormone verdicken sich diese Herde und bluten in jedem Zyklus, was lokale Entzündungen, Verwachsungen und Schmerzen verursacht.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet(tr("Fréquence : elle concerne environ 1 personne sur 10 en âge de procréer (OMS / Inserm).", "Prevalence: affects approximately 1 in 10 individuals of reproductive age (WHO / Inserm).", pl = "Częstość: dotyczy około 1 osoby na 10 w wieku rozrodczym (WHO / Inserm).", uk = "Поширеність: стосується приблизно 1 людини з 10 дітородного віку (ВООЗ / Inserm).", es = "Frecuencia: afecta a aproximadamente 1 de cada 10 personas en edad reproductiva (OMS / Inserm).", de = "Häufigkeit: Betroffen ist etwa 1 von 10 Personen im gebärfähigen Alter (WHO / Inserm)."))
            Bullet(tr("Une règle essentielle : une douleur de règles qui empêche d'aller en cours, au travail ou de vivre normalement n'est pas normale.", "Key takeaway: menstrual pain that prevents you from going to work, school or daily activities is not normal.", pl = "Podstawowa zasada: ból miesiączkowy, który uniemożliwia chodzenie na zajęcia, do pracy lub normalne życie, nie jest normalny.", uk = "Головне правило: менструальний біль, через який неможливо піти на навчання чи роботу або жити звичайним життям, не є нормою.", es = "Una regla esencial: un dolor menstrual que impide ir a clase, al trabajo o llevar una vida normal no es normal.", de = "Eine wichtige Regel: Regelschmerzen, die verhindern, zur Schule oder zur Arbeit zu gehen oder normal zu leben, sind nicht normal."))
        }

        SectionTitle(tr("Les 5 « D » : les principaux signaux d'alerte", "The 5 'Ds': primary warning signs", pl = "5 „D”: główne sygnały alarmowe", uk = "5 «Д»: основні тривожні сигнали", es = "Las 5 «D»: las principales señales de alerta", de = "Die 5 „D“: die wichtigsten Warnzeichen"))
        LunaCard {
            Text(
                tr(
                    fr = "Les sociétés savantes (HAS, ESHRE) retiennent 5 manifestations douloureuses caractéristiques à surveiller :",
                    en = "Medical societies (HAS, ESHRE) highlight 5 characteristic pain symptoms to monitor:",
                    pl = "Towarzystwa naukowe (HAS, ESHRE) wskazują 5 charakterystycznych objawów bólowych, na które warto zwracać uwagę:",
                    uk = "Наукові товариства (HAS, ESHRE) виділяють 5 характерних больових проявів, за якими варто стежити:",
                    es = "Las sociedades científicas (HAS, ESHRE) señalan 5 manifestaciones dolorosas características que conviene vigilar:",
                    de = "Die Fachgesellschaften (HAS, ESHRE) nennen 5 charakteristische Schmerzzeichen, auf die man achten sollte:",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Bullet(tr("Dysménorrhée : règles très douloureuses qui résistent aux antalgiques usuels (paracétamol, anti-inflammatoires).", "Dysmenorrhea: severe period pain that resists standard pain relievers.", pl = "Dysmenorrhea: bardzo bolesne miesiączki, które nie ustępują po zwykłych lekach przeciwbólowych (paracetamol, leki przeciwzapalne).", uk = "Дисменорея: дуже болісні менструації, що не піддаються звичайним знеболювальним (парацетамол, протизапальні засоби).", es = "Dismenorrea: reglas muy dolorosas que no ceden con los analgésicos habituales (paracetamol, antiinflamatorios).", de = "Dysmenorrhoe: sehr schmerzhafte Perioden, die auf übliche Schmerzmittel nicht ansprechen (Paracetamol, Entzündungshemmer)."))
            Bullet(tr("Dyspareunie : douleurs profondes pendant ou après les rapports sexuels.", "Dyspareunia: deep pain during or after sexual intercourse.", pl = "Dyspareunia: głębokie bóle podczas lub po stosunku płciowym.", uk = "Диспареунія: глибокий біль під час або після статевого акту.", es = "Dispareunia: dolor profundo durante o después de las relaciones sexuales.", de = "Dyspareunie: tiefe Schmerzen während oder nach dem Geschlechtsverkehr."))
            Bullet(tr("Dyschésie : douleurs lors du transit ou de la défécation, accentuées pendant les règles.", "Dyschezia: painful bowel movements, especially during periods.", pl = "Dyschezja: ból podczas pasażu jelitowego lub wypróżniania, nasilający się podczas miesiączki.", uk = "Дисхезія: біль під час кишкового транзиту чи дефекації, що посилюється під час менструації.", es = "Disquecia: dolor durante el tránsito intestinal o la defecación, que empeora durante la regla.", de = "Dyschezie: Schmerzen bei der Darmpassage oder beim Stuhlgang, die sich während der Periode verstärken."))
            Bullet(tr("Dysurie : douleurs, brûlures ou difficultés à uriner pendant les règles, sans infection bactérienne.", "Dysuria: painful or difficult urination during periods, without bacterial infection.", pl = "Dyzuria: ból, pieczenie lub trudności w oddawaniu moczu podczas miesiączki, bez zakażenia bakteryjnego.", uk = "Дизурія: біль, печіння або утруднене сечовипускання під час менструації без бактеріальної інфекції.", es = "Disuria: dolor, ardor o dificultad para orinar durante la regla, sin infección bacteriana.", de = "Dysurie: Schmerzen, Brennen oder Schwierigkeiten beim Wasserlassen während der Periode, ohne bakterielle Infektion."))
            Bullet(tr("Douleurs pelviennes chroniques : douleurs continues dans le bas-ventre, tiraillements ou irradiations vers le dos, les lombaires et les cuisses.", "Chronic pelvic pain: persistent lower abdominal pain, pulling or radiating pain down the back and legs.", pl = "Przewlekły ból miednicy: ciągły ból w podbrzuszu, ciągnięcie lub promieniowanie do pleców, okolicy lędźwiowej i ud.", uk = "Хронічний тазовий біль: постійний біль унизу живота, тягнучі відчуття або іррадіація в спину, поперек і стегна.", es = "Dolor pélvico crónico: dolor continuo en el bajo vientre, tirones o irradiación hacia la espalda, la zona lumbar y los muslos.", de = "Chronische Beckenschmerzen: anhaltende Schmerzen im Unterbauch, Ziehen oder Ausstrahlen in den Rücken, den Lendenbereich und die Oberschenkel."))
        }

        SectionTitle(tr("Le parcours de diagnostic", "Diagnostic journey", pl = "Droga do diagnozy", uk = "Шлях до діагнозу", es = "El camino hacia el diagnóstico", de = "Der Weg zur Diagnose"))
        LunaCard {
            Text(
                tr(
                    fr = "Le diagnostic met encore souvent plusieurs années à être posé. Noter précisément vos symptômes dans Lunaison et utiliser le « Bilan de santé » permet d'apporter un historique clair à votre soignant.",
                    en = "Diagnosis still takes several years on average. Tracking symptoms in Lunaison and using the 'Health report' helps provide clear history to your clinician.",
                    pl = "Diagnoza nadal często zajmuje kilka lat. Dokładne zapisywanie objawów w Lunaison i korzystanie z „Raportu medycznego” pozwala przedstawić osobie z personelu medycznego czytelną historię.",
                    uk = "Діагноз і досі часто ставлять лише через кілька років. Точний запис симптомів у Lunaison та використання «Медичного звіту» дають змогу надати вашому медичному працівнику чітку історію.",
                    es = "El diagnóstico todavía tarda con frecuencia varios años en llegar. Anotar con precisión tus síntomas en Lunaison y usar el «Informe médico» permite llevar un historial claro a tu profesional sanitario.",
                    de = "Bis zur Diagnose vergehen oft noch mehrere Jahre. Wenn du deine Symptome in Lunaison genau einträgst und den „Arztbericht“ nutzt, kannst du deinem medizinischen Fachpersonal eine klare Vorgeschichte mitbringen.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet(tr("Qui consulter en premier : médecin généraliste, sage-femme ou gynécologue.", "Who to see first: GP, midwife or gynecologist.", pl = "Do kogo zgłosić się najpierw: lekarz rodzinny, położna lub ginekolog.", uk = "До кого звернутися насамперед: лікар загальної практики, акушерка або гінеколог.", es = "A quién consultar primero: médico de cabecera, matrona o ginecólogo.", de = "Wen man zuerst aufsucht: Hausärztin oder Hausarzt, Hebamme oder Gynäkologe."))
            Bullet(tr("Examens clés : échographie pelvienne par voie endovaginale et IRM pelvienne. Il est recommandé de les faire réaliser par un radiologue référent formé à l'endométriose (une imagerie normale n'exclut pas une forme superficielle).", "Key exams: transvaginal pelvic ultrasound and pelvic MRI, ideally performed by a specialized radiologist.", pl = "Kluczowe badania: USG miednicy przezpochwowe i rezonans magnetyczny miednicy. Zaleca się, aby wykonywał je radiolog referencyjny przeszkolony w zakresie endometriozy (prawidłowy obraz nie wyklucza postaci powierzchownej).", uk = "Ключові обстеження: трансвагінальне УЗД органів малого тазу та МРТ малого тазу. Рекомендується проводити їх у референтного радіолога, навченого діагностувати ендометріоз (нормальна візуалізація не виключає поверхневу форму).", es = "Pruebas clave: ecografía pélvica endovaginal y resonancia magnética pélvica. Se recomienda que las realice un radiólogo de referencia formado en endometriosis (una imagen normal no descarta una forma superficial).", de = "Wichtige Untersuchungen: transvaginaler Ultraschall des Beckens und MRT des Beckens. Empfohlen wird, sie von einer auf Endometriose spezialisierten Radiologin oder einem Radiologen durchführen zu lassen (eine unauffällige Bildgebung schließt eine oberflächliche Form nicht aus)."))
            Bullet(tr("Filières régionales : des réseaux de soins spécialisés labellisés (ARS en France) regroupent soignants, radiologues et centres experts pour faciliter l'orientation.", "Regional networks: specialized care networks help patients access experienced clinicians and reference centers.", pl = "Sieci regionalne: wyspecjalizowane sieci opieki zrzeszają personel medyczny, radiologów i ośrodki eksperckie, aby ułatwić skierowanie do odpowiedniej opieki.", uk = "Регіональні мережі: спеціалізовані мережі допомоги об'єднують медичних працівників, радіологів і експертні центри, щоб полегшити маршрутизацію пацієнток.", es = "Redes regionales: redes de atención especializadas reúnen a profesionales sanitarios, radiólogos y centros expertos para facilitar la orientación.", de = "Regionale Netzwerke: spezialisierte Versorgungsnetze bündeln medizinisches Fachpersonal, Radiologie und Expertenzentren, um die Orientierung zu erleichtern."))
        }

        SectionTitle(tr("Pistes de soulagement au quotidien", "Everyday relief options", pl = "Sposoby łagodzenia dolegliwości na co dzień", uk = "Способи полегшення в повсякденному житті", es = "Vías de alivio en el día a día", de = "Linderung im Alltag"))
        LunaCard {
            Bullet(tr("Chaleur : bouillotte ou patchs chauffants sur le ventre ou les lombaires pour détendre les muscles contractés.", "Heat: hot water bottle or heat patches on the abdomen or back to relax tight muscles.", pl = "Ciepło: termofor lub plastry rozgrzewające na brzuch lub okolicę lędźwiową, aby rozluźnić napięte mięśnie.", uk = "Тепло: грілка або зігрівальні пластирі на живіт чи поперек, щоб розслабити напружені м'язи.", es = "Calor: bolsa de agua caliente o parches térmicos sobre el vientre o la zona lumbar para relajar los músculos contraídos.", de = "Wärme: Wärmflasche oder Wärmepflaster auf Bauch oder Lendenbereich, um verkrampfte Muskeln zu entspannen."))
            Bullet(tr("Électrostimulation TENS : ces appareils de neurostimulation transcutanée envoient de légers influx non douloureux qui court-circuitent le message de douleur. Ils peuvent être prescrits et pris en charge.", "TENS neurostimulation: wearable devices sending gentle pulses that block pain signals to the brain. Often available on prescription.", pl = "Elektrostymulacja TENS: te urządzenia do przezskórnej neurostymulacji wysyłają lekkie, niebolesne impulsy, które zakłócają przekazywanie sygnału bólowego. Mogą być przepisane i refundowane.", uk = "Електростимуляція TENS: ці пристрої черезшкірної нейростимуляції надсилають легкі безболісні імпульси, які перебивають больовий сигнал. Їх можуть призначати лікарі, і їх вартість може відшкодовуватися.", es = "Electroestimulación TENS: estos aparatos de neuroestimulación transcutánea envían impulsos leves e indoloros que interfieren con el mensaje del dolor. Pueden prescribirse y estar cubiertos por el sistema sanitario.", de = "TENS-Elektrostimulation: Diese Geräte zur transkutanen Nervenstimulation senden leichte, schmerzfreie Impulse, die die Schmerzsignale überlagern. Sie können verordnet werden und werden unter Umständen erstattet."))
            Bullet(tr("Kinésithérapie et ostéopathie pelvienne : exercices de respiration, étirements du psoas et travail doux du plancher pelvien pour soulager les tensions réflexes.", "Pelvic physiotherapy: breathing exercises, psoas stretches and pelvic floor release to ease reflex tension.", pl = "Fizjoterapia i osteopatia miednicy: ćwiczenia oddechowe, rozciąganie mięśnia lędźwiowego i delikatna praca z dnem miednicy, aby złagodzić napięcia odruchowe.", uk = "Кінезітерапія та остеопатія таза: дихальні вправи, розтягування клубово-поперекового м'яза й м'яка робота з тазовим дном, щоб зняти рефлекторну напругу.", es = "Kinesiterapia y osteopatía pélvica: ejercicios de respiración, estiramientos del psoas y trabajo suave del suelo pélvico para aliviar las tensiones reflejas.", de = "Physiotherapie und Beckenosteopathie: Atemübungen, Dehnung des Psoas und sanfte Arbeit am Beckenboden, um reflektorische Verspannungen zu lösen."))
            Bullet(tr("Cohérence cardiaque : le module « Respirer » de Lunaison aide à apaiser le système nerveux autonome lors des pics douloureux.", "Cardiac coherence: Lunaison's 'Breathe' exercise helps calm the nervous system during pain flares.", pl = "Koherencja serca: moduł „Oddychaj” w Lunaison pomaga uspokoić autonomiczny układ nerwowy podczas nasilenia bólu.", uk = "Серцева когерентність: модуль «Дихати» в Lunaison допомагає заспокоїти вегетативну нервову систему під час піків болю.", es = "Coherencia cardíaca: el módulo «Respirar» de Lunaison ayuda a calmar el sistema nervioso autónomo durante los picos de dolor.", de = "Herzkohärenz: Das Modul „Atmen“ von Lunaison hilft, das autonome Nervensystem bei Schmerzspitzen zu beruhigen."))
            Bullet(tr("Traitements médicaux : pilule en continu ou DIU hormonal pour mettre les règles au repos, antalgiques adaptés ou chirurgie conservatrice si nécessaire, selon un projet de soins personnalisé.", "Medical management: continuous hormonal contraception to suppress periods, tailored medications or surgery when indicated.", pl = "Leczenie medyczne: tabletki w trybie ciągłym lub hormonalna wkładka domaciczna, aby wyciszyć miesiączki, odpowiednie leki przeciwbólowe lub w razie potrzeby zachowawcza operacja, zgodnie z indywidualnym planem opieki.", uk = "Медикаментозне лікування: безперервний прийом таблеток або гормональна ВМС, щоб «заспокоїти» менструації, відповідні знеболювальні або за потреби органозберігальна операція відповідно до персоналізованого плану лікування.", es = "Tratamientos médicos: píldora en continuo o DIU hormonal para poner la regla en reposo, analgésicos adaptados o cirugía conservadora si es necesario, según un plan de cuidados personalizado.", de = "Medizinische Behandlungen: Pille im Langzyklus oder Hormonspirale, um die Periode ruhigzustellen, passende Schmerzmittel oder bei Bedarf eine organerhaltende Operation, nach einem individuellen Behandlungsplan."))
        }

        SectionTitle(
            tr(
                fr = "Ressources et associations de patientes",
                en = "Patient resources & associations",
                pl = "Wsparcie i stowarzyszenia pacjentek",
                uk = "Підтримка та асоціації пацієнток",
                es = "Recursos y asociaciones de pacientes",
                de = "Hilfe & Patientenorganisationen",
            )
        )
        LunaCard {
            Text(
                tr(
                    fr = "Des associations reconnues informent, accompagnent et orientent les personnes touchées vers des soignants formés :",
                    en = "Recognized associations provide support, guidance and specialized clinician directories:",
                    pl = "Uznane organizacje wspierają, informują i pomagają znaleźć doświadczonych specjalistów:",
                    uk = "Організації підтримки надають інформацію та допомагають знайти профільних фахівців:",
                    es = "Las asociaciones de pacientes ofrecen apoyo, información y orientación hacia especialistas formados:",
                    de = "Anerkannte Organisationen bieten Beratung, Informationen und Hilfe bei der Suche nach Fachkräften:",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.endofrance.org",
                        en = "https://www.endometriosis-uk.org",
                        pl = "https://pokonacendometrioze.pl",
                        uk = "https://www.endometriosis-uk.org",
                        es = "https://adaec.org",
                        de = "https://www.endometriose-vereinigung.de",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "EndoFrance : association de lutte contre l'endométriose (endofrance.org)",
                        en = "Endometriosis UK: patient support and information (endometriosis-uk.org)",
                        pl = "Fundacja Pokonać Endometriozę : wsparcie pacjentek (pokonacendometrioze.pl)",
                        uk = "Endometriosis UK: підтримка пацієнток (endometriosis-uk.org)",
                        es = "ADAEC : Asociación de Afectadas de Endometriosis Crónica (adaec.org)",
                        de = "Endometriose-Vereinigung Deutschland e.V. (endometriose-vereinigung.de)",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.has-sante.fr/jcms/c_2819733/fr/prise-en-charge-de-l-endometriose",
                        en = "https://www.nhs.uk/conditions/endometriosis/",
                        pl = "https://www.mp.pl/pacjent/ginekologia/choroby/260759,endometrioza",
                        uk = "https://www.nhs.uk/conditions/endometriosis/",
                        es = "https://medlineplus.gov/spanish/endometriosis.html",
                        de = "https://www.frauenaerzte-im-netz.de/erkrankungen/endometriose/",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "Recommandations officielles de la Haute Autorité de Santé (HAS)",
                        en = "Official clinical guidelines & care (nhs.uk)",
                        pl = "Medycyna Praktyczna : diagnostyka i leczenie endometriozy (mp.pl)",
                        uk = "Клінічні рекомендації та діагностика (nhs.uk)",
                        es = "Guía clínica sobre endometriosis (MedlinePlus)",
                        de = "Frauenärzte im Netz : Diagnose und Therapie der Endometriose",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        ClinicalGuideSourcesCard(
            listOf(
                Sources.ESHRE_ENDOMETRIOSIS_2022,
                Sources.ENDOMETRIOSIS_2024,
                Sources.ACOG_DYSMENORRHEA_2018,
            ),
        )

        Muted(tr("Cette fiche est informative et ne remplace pas une consultation médicale avec un professionnel de santé.", "This guide is for informational purposes and does not replace consultation with a healthcare professional.", pl = "Ten artykuł ma charakter informacyjny i nie zastępuje konsultacji medycznej z osobą z personelu medycznego.", uk = "Ця стаття має інформаційний характер і не замінює медичну консультацію з медичним працівником.", es = "Esta ficha es informativa y no sustituye una consulta médica con un profesional sanitario.", de = "Dieser Artikel dient der Information und ersetzt keine ärztliche Beratung durch medizinisches Fachpersonal."))
    }
}

@Composable
fun PcosGuideScreen(onBack: () -> Unit) {
    val context = rememberPlatformContext()

    Page {
        PageTitle(
            tr("Le SOPK (ovaires polykystiques)", "PCOS (polycystic ovary syndrome)", pl = "PCOS (zespół policystycznych jajników)", uk = "СПКЯ (синдром полікістозних яєчників)", es = "El SOP (ovarios poliquísticos)", de = "PCOS (polyzystische Ovarien)"),
            onBack = onBack,
            subtitle = tr("Repères cliniques, critères de Rotterdam et pistes d'action.", "Clinical signs, Rotterdam criteria and management options.", pl = "Wskazówki kliniczne, kryteria rotterdamskie i możliwe działania.", uk = "Клінічні орієнтири, Роттердамські критерії та можливі кроки.", es = "Pautas clínicas, criterios de Róterdam y vías de actuación.", de = "Klinische Orientierung, Rotterdam-Kriterien und Handlungsmöglichkeiten."),
        )

        SectionTitle(tr("Qu'est-ce que le SOPK ?", "What is PCOS?", pl = "Czym jest PCOS?", uk = "Що таке СПКЯ?", es = "¿Qué es el SOP?", de = "Was ist PCOS?"))
        LunaCard {
            Text(
                tr(
                    fr = "Le syndrome des ovaires polykystiques (SOPK) est la première cause d'infertilité anovulatoire et le dérèglement hormonal le plus fréquent chez les femmes en âge de procréer (environ 1 personne sur 10 selon l'Inserm et l'OMS).",
                    en = "Polycystic ovary syndrome (PCOS) is the leading cause of anovulatory infertility and the most common hormonal disorder in women of reproductive age (approx. 1 in 10 per WHO/Inserm).",
                    pl = "Zespół policystycznych jajników (PCOS) jest główną przyczyną niepłodności z powodu braku owulacji i najczęstszym zaburzeniem hormonalnym u kobiet w wieku rozrodczym (około 1 osoba na 10 według Inserm i WHO).",
                    uk = "Синдром полікістозних яєчників (СПКЯ) є основною причиною ановуляторного безпліддя та найпоширенішим гормональним порушенням у жінок дітородного віку (приблизно 1 особа з 10 за даними Inserm і ВООЗ).",
                    es = "El síndrome de ovario poliquístico (SOP) es la primera causa de infertilidad por anovulación y el trastorno hormonal más frecuente en mujeres en edad reproductiva (aproximadamente 1 de cada 10 personas según Inserm y la OMS).",
                    de = "Das polyzystische Ovarialsyndrom (PCOS) ist die häufigste Ursache anovulatorischer Unfruchtbarkeit und die häufigste Hormonstörung bei Frauen im gebärfähigen Alter (etwa 1 von 10 Personen laut Inserm und WHO).",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Malgré son nom historique, il ne s'agit pas de kystes organiques mais d'une multitude de petits follicules immatures bloqués au stade de maturation dans l'ovaire, liés à un déséquilibre entre hormones hypophysaires (LH/FSH) et androgènes.",
                    en = "Despite its name, it does not involve ovarian cysts, but rather multiple small immature follicles paused in their growth, driven by an imbalance in pituitary hormones and androgens.",
                    pl = "Mimo historycznej nazwy nie chodzi o torbiele organiczne, lecz o wiele małych niedojrzałych pęcherzyków zatrzymanych na etapie dojrzewania w jajniku, związanych z zaburzeniem równowagi między hormonami przysadki (LH/FSH) a androgenami.",
                    uk = "Попри історичну назву, йдеться не про органічні кісти, а про безліч дрібних незрілих фолікулів, що застрягли на стадії дозрівання в яєчнику, пов'язаних із дисбалансом між гормонами гіпофіза (ЛГ/ФСГ) та андрогенами.",
                    es = "A pesar de su nombre histórico, no se trata de quistes orgánicos, sino de una multitud de pequeños folículos inmaduros bloqueados en la fase de maduración en el ovario, relacionados con un desequilibrio entre las hormonas hipofisarias (LH/FSH) y los andrógenos.",
                    de = "Trotz des historischen Namens handelt es sich nicht um organische Zysten, sondern um eine Vielzahl kleiner, unreifer Follikel, die im Eierstock in der Reifung stecken geblieben sind, bedingt durch ein Ungleichgewicht zwischen den Hormonen der Hirnanhangdrüse (LH/FSH) und den Androgenen.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionTitle(tr("Les critères de Rotterdam (diagnostic)", "Rotterdam diagnostic criteria", pl = "Kryteria rotterdamskie (diagnostyka)", uk = "Роттердамські критерії (діагностика)", es = "Los criterios de Róterdam (diagnóstico)", de = "Die Rotterdam-Kriterien (Diagnose)"))
        LunaCard {
            Text(
                tr(
                    fr = "Selon le consensus international de Rotterdam, le diagnostic est posé en présence d'au moins 2 des 3 critères suivants (après exclusion d'autres causes) :",
                    en = "Under international Rotterdam criteria, diagnosis requires at least 2 of the following 3 signs:",
                    pl = "Zgodnie z międzynarodowym konsensusem z Rotterdamu diagnozę stawia się przy obecności co najmniej 2 z 3 poniższych kryteriów (po wykluczeniu innych przyczyn):",
                    uk = "За міжнародним Роттердамським консенсусом діагноз ставлять за наявності щонайменше 2 з 3 наведених критеріїв (після виключення інших причин):",
                    es = "Según el consenso internacional de Róterdam, el diagnóstico se establece cuando hay al menos 2 de los 3 criterios siguientes (tras descartar otras causas):",
                    de = "Nach dem internationalen Konsens von Rotterdam wird die Diagnose gestellt, wenn mindestens 2 der folgenden 3 Kriterien erfüllt sind (nach Ausschluss anderer Ursachen):",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Bullet(tr("1. Ovulations rares ou absentes : cycles très longs (> 35 jours, spanioménorrhée), imprévisibles ou absence de règles pendant plus de 3 mois (aménorrhée).", "1. Infrequent or absent ovulation: very long cycles (> 35 days), unpredictable bleeding or absence of periods for 3+ months.", pl = "1. Rzadkie lub brakujące owulacje: bardzo długie cykle (> 35 dni, spaniomenorea), nieprzewidywalne lub brak miesiączki przez ponad 3 miesiące (amenorrhea).", uk = "1. Рідкісні або відсутні овуляції: дуже довгі цикли (> 35 днів, спаніоменорея), непередбачувані або відсутність менструацій понад 3 місяці (аменорея).", es = "1. Ovulaciones infrecuentes o ausentes: ciclos muy largos (> 35 días, espaniomenorrea), impredecibles o ausencia de regla durante más de 3 meses (amenorrea).", de = "1. Seltene oder ausbleibende Eisprünge: sehr lange Zyklen (> 35 Tage, Spanomenorrhoe), unvorhersehbar oder Ausbleiben der Periode über mehr als 3 Monate (Amenorrhoe)."))
            Bullet(tr("2. Hyperandrogénie : excès d'hormones androgènes visible (acné d'adulte tenace, pilosité marquée sur le menton, le thorax ou la ligne ombilicale, chute de cheveux) ou biologique (taux de testostérone libre élevé à la prise de sang).", "2. Hyperandrogenism: excess androgens manifesting as persistent acne, hirsutism, hair loss or elevated blood levels.", pl = "2. Hiperandrogenizm: nadmiar hormonów androgenowych widoczny (uporczywy trądzik u dorosłych, nasilone owłosienie na brodzie, klatce piersiowej lub linii pępka, wypadanie włosów) lub wykazany w badaniach (podwyższony poziom wolnego testosteronu we krwi).", uk = "2. Гіперандрогенія: надлишок андрогенних гормонів, видимий (стійкі акне в дорослому віці, виражене оволосіння на підборідді, грудях чи по лінії пупка, випадіння волосся) або біохімічний (підвищений рівень вільного тестостерону в аналізі крові).", es = "2. Hiperandrogenismo: exceso de hormonas andrógenas visible (acné persistente en la edad adulta, vello marcado en el mentón, el tórax o la línea umbilical, caída del cabello) o biológico (nivel elevado de testosterona libre en el análisis de sangre).", de = "2. Hyperandrogenismus: Überschuss an männlichen Hormonen, sichtbar (hartnäckige Akne im Erwachsenenalter, ausgeprägte Behaarung an Kinn, Brust oder Nabellinie, Haarausfall) oder laborchemisch (erhöhter freier Testosteronspiegel im Bluttest)."))
            Bullet(tr("3. Morphologie ovarienne polykystique : présence de 20 follicules ou plus par ovaire ou volume ovarien supérieur à 10 ml à l'échographie endovaginale.", "3. Polycystic ovarian morphology: 20+ follicles per ovary or ovarian volume > 10 ml on pelvic ultrasound.", pl = "3. Policystyczna morfologia jajników: obecność 20 lub więcej pęcherzyków w jajniku albo objętość jajnika powyżej 10 ml w USG przezpochwowym.", uk = "3. Полікістозна морфологія яєчників: наявність 20 або більше фолікулів в яєчнику чи об'єм яєчника понад 10 мл при трансвагінальному УЗД.", es = "3. Morfología ovárica poliquística: presencia de 20 o más folículos por ovario o volumen ovárico superior a 10 ml en la ecografía endovaginal.", de = "3. Polyzystische Eierstockmorphologie: 20 oder mehr Follikel pro Eierstock oder ein Eierstockvolumen von mehr als 10 ml im transvaginalen Ultraschall."))
        }

        SectionTitle(tr("Santé métabolique & globale", "Metabolic & general health", pl = "Zdrowie metaboliczne i ogólne", uk = "Метаболічне та загальне здоров'я", es = "Salud metabólica y global", de = "Stoffwechsel und allgemeine Gesundheit"))
        LunaCard {
            Bullet(tr("Résistance à l'insuline : jusqu'à 70 % des femmes avec SOPK présentent une insulinorésistance qui stimule la production d'androgènes par les ovaires et favorise la prise de poids abdominale et la fatigue.", "Insulin resistance: present in up to 70% of individuals with PCOS, stimulating androgen production and abdominal weight gain.", pl = "Insulinooporność: do 70 % kobiet z PCOS ma insulinooporność, która pobudza produkcję androgenów w jajnikach i sprzyja przyrostowi masy ciała w okolicy brzucha oraz zmęczeniu.", uk = "Інсулінорезистентність: до 70 % жінок із СПКЯ мають інсулінорезистентність, яка стимулює вироблення андрогенів яєчниками та сприяє збільшенню ваги в ділянці живота й втомі.", es = "Resistencia a la insulina: hasta el 70 % de las mujeres con SOP presentan insulinorresistencia, que estimula la producción de andrógenos por los ovarios y favorece el aumento de peso abdominal y el cansancio.", de = "Insulinresistenz: Bis zu 70 % der Frauen mit PCOS haben eine Insulinresistenz, die die Androgenbildung in den Eierstöcken anregt und Gewichtszunahme am Bauch sowie Müdigkeit begünstigt."))
            Bullet(tr("Risques à long terme : surveillance préventive recommandée pour le diabète de type 2, l'hypertension et la santé cardiovasculaire.", "Long-term monitoring: recommended for type 2 diabetes risk and cardiovascular health.", pl = "Ryzyka długoterminowe: zalecany jest profilaktyczny nadzór w kierunku cukrzycy typu 2, nadciśnienia i zdrowia sercowo-naczyniowego.", uk = "Довгострокові ризики: рекомендовано профілактичний нагляд щодо цукрового діабету 2 типу, артеріальної гіпертензії та здоров'я серцево-судинної системи.", es = "Riesgos a largo plazo: se recomienda una vigilancia preventiva de la diabetes de tipo 2, la hipertensión y la salud cardiovascular.", de = "Langfristige Risiken: Eine vorbeugende Überwachung auf Typ-2-Diabetes, Bluthochdruck und Herz-Kreislauf-Gesundheit wird empfohlen."))
            Bullet(tr("Bien-être émotionnel : l'impact sur l'image corporelle, l'anxiété et le moral est réel et mérite un accompagnement bienveillant et sans jugement.", "Emotional well-being: the psychological impact on self-image and anxiety warrants compassionate care.", pl = "Dobrostan emocjonalny: wpływ na obraz ciała, lęk i nastrój jest realny i zasługuje na życzliwe wsparcie bez oceniania.", uk = "Емоційне благополуччя: вплив на образ тіла, тривожність і настрій реальний і заслуговує на доброзичливу підтримку без осуду.", es = "Bienestar emocional: el impacto sobre la imagen corporal, la ansiedad y el ánimo es real y merece un acompañamiento respetuoso y sin juicios.", de = "Emotionales Wohlbefinden: Die Auswirkungen auf das Körperbild, die Angst und die Stimmung sind real und verdienen eine wohlwollende Begleitung ohne Wertung."))
        }

        SectionTitle(tr("Pistes de prise en charge", "Management options", pl = "Możliwe kierunki postępowania", uk = "Напрями лікування та підтримки", es = "Vías de tratamiento y acompañamiento", de = "Behandlungs- und Unterstützungsmöglichkeiten"))
        LunaCard {
            Bullet(tr("Alimentation à index glycémique modéré : privilégier les légumes, les légumineuses, les protéines et les bonnes graisses pour éviter les pics d'insuline.", "Low-glycemic nutrition: emphasizing vegetables, legumes, protein and fiber to smooth insulin spikes.", pl = "Dieta o umiarkowanym indeksie glikemicznym: wybierać warzywa, rośliny strączkowe, białko i zdrowe tłuszcze, aby unikać skoków insuliny.", uk = "Харчування з помірним глікемічним індексом: віддавати перевагу овочам, бобовим, білкам і корисним жирам, щоб уникати стрибків інсуліну.", es = "Alimentación con índice glucémico moderado: dar preferencia a las verduras, las legumbres, las proteínas y las grasas buenas para evitar los picos de insulina.", de = "Ernährung mit moderatem glykämischem Index: Gemüse, Hülsenfrüchte, Proteine und gesunde Fette bevorzugen, um Insulinspitzen zu vermeiden."))
            Bullet(tr("Activité physique régulière : la marche, la natation et le renforcement musculaire augmentent directement la sensibilité cellulaire à l'insuline.", "Regular physical activity: strength training and aerobic exercise directly improve cellular insulin sensitivity.", pl = "Regularna aktywność fizyczna: spacery, pływanie i trening siłowy bezpośrednio zwiększają wrażliwość komórek na insulinę.", uk = "Регулярна фізична активність: ходьба, плавання та силові вправи безпосередньо підвищують чутливість клітин до інсуліну.", es = "Actividad física regular: caminar, nadar y el fortalecimiento muscular aumentan directamente la sensibilidad de las células a la insulina.", de = "Regelmäßige Bewegung: Gehen, Schwimmen und Krafttraining erhöhen direkt die Insulinempfindlichkeit der Zellen."))
            Bullet(tr("Protéger l'endomètre : en l'absence de règles régulières, un traitement progestatif séquentiel permet d'évacuer régulièrement la muqueuse utérine.", "Endometrial protection: sequential progestins ensure regular shedding when periods are absent.", pl = "Ochrona endometrium: przy braku regularnych miesiączek sekwencyjne leczenie gestagenem pozwala regularnie usuwać błonę śluzową macicy.", uk = "Захист ендометрія: за відсутності регулярних менструацій послідовне лікування прогестагеном дає змогу регулярно виводити слизову оболонку матки.", es = "Proteger el endometrio: en ausencia de reglas regulares, un tratamiento secuencial con progestágeno permite eliminar con regularidad la mucosa uterina.", de = "Die Gebärmutterschleimhaut schützen: Wenn die Periode nicht regelmäßig kommt, ermöglicht eine sequenzielle Gestagenbehandlung, die Gebärmutterschleimhaut regelmäßig abzustoßen."))
            Bullet(tr("Traitements médicaux : pilule oestroprogestative anti-androgène, metformine en cas d'insulinorésistance ou inducteurs d'ovulation en projet de grossesse.", "Medical options: anti-androgenic pill, metformin or ovulation induction when seeking pregnancy.", pl = "Leczenie medyczne: złożona tabletka antykoncepcyjna o działaniu antyandrogennym, metformina w przypadku insulinooporności lub leki indukujące owulację przy planowaniu ciąży.", uk = "Медикаментозне лікування: естроген-прогестагенна таблетка з антиандрогенною дією, метформін за інсулінорезистентності або стимулятори овуляції при плануванні вагітності.", es = "Tratamientos médicos: píldora estroprogestágena antiandrógena, metformina en caso de insulinorresistencia o inductores de la ovulación si hay un proyecto de embarazo.", de = "Medizinische Behandlungen: antiandrogene Östrogen-Gestagen-Pille, Metformin bei Insulinresistenz oder Ovulationsauslöser bei Kinderwunsch."))
            Bullet(tr("Dans Lunaison : activez l'option « Cycles naturellement irréguliers » dans les Réglages pour bénéficier de prévisions adaptées sans alertes anxiogènes.", "In Lunaison: enable 'Naturally irregular cycles' in Settings for adapted prediction intervals.", pl = "W Lunaison: włącz opcję „Naturalnie nieregularne cykle” w Ustawieniach, aby korzystać z dopasowanych prognoz bez niepokojących alertów.", uk = "У Lunaison: увімкніть опцію «Природно нерегулярні цикли» в Налаштуваннях, щоб отримувати адаптовані прогнози без тривожних сповіщень.", es = "En Lunaison: activa la opción «Ciclos naturalmente irregulares» en Ajustes para obtener previsiones adaptadas sin alertas que generen ansiedad.", de = "In Lunaison: Aktiviere in den Einstellungen die Option „Natürlich unregelmäßige Zyklen“, um angepasste Vorhersagen ohne beunruhigende Warnungen zu erhalten."))
        }

        SectionTitle(
            tr(
                fr = "Ressources et associations",
                en = "Resources & associations",
                pl = "Wsparcie i organizacje",
                uk = "Підтримка та ресурси",
                es = "Recursos y asociaciones",
                de = "Hilfe & Organisationen",
            )
        )
        LunaCard {
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.espopk.org",
                        en = "https://www.verity-pcos.org.uk",
                        pl = "https://pacjent.gov.pl/zapobiegaj/zespol-policystycznych-jajnikow-pcos",
                        uk = "https://www.verity-pcos.org.uk",
                        es = "https://aesopspain.org",
                        de = "https://www.pcos-selbsthilfe.org",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "Association Esp'OPK : association francophone dédiée au SOPK (espopk.org)",
                        en = "Verity PCOS UK: patient support charity (verity-pcos.org.uk)",
                        pl = "Pacjent.gov.pl : Zespół policystycznych jajników (PCOS)",
                        uk = "Verity PCOS: підтримка пацієнток (verity-pcos.org.uk)",
                        es = "AESOP : Asociación Española de Síndrome de Ovario Poliquístico (aesopspain.org)",
                        de = "Selbsthilfegruppe PCO-Syndrom e.V. (pcos-selbsthilfe.org)",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.inserm.fr/dossier/syndrome-ovaires-polykystiques-sopk/",
                        en = "https://www.nhs.uk/conditions/polycystic-ovary-syndrome-pcos/",
                        pl = "https://www.mp.pl/pacjent/ginekologia/choroby/260757,zespol-policystycznych-jajnikow-pcos",
                        uk = "https://www.nhs.uk/conditions/polycystic-ovary-syndrome-pcos/",
                        es = "https://medlineplus.gov/spanish/polycysticoverydisease.html",
                        de = "https://www.frauenaerzte-im-netz.de/erkrankungen/pcos-polyzystisches-ovarialsyndrom/",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "Dossier scientifique de l'Inserm sur le SOPK",
                        en = "NHS clinical guide to PCOS (nhs.uk)",
                        pl = "Medycyna Praktyczna : objawy i postępowanie w PCOS (mp.pl)",
                        uk = "Клінічне керівництво щодо СПКЯ (nhs.uk)",
                        es = "Información médica sobre el SOP (MedlinePlus)",
                        de = "Frauenärzte im Netz : PCO-Syndrom Ursachen und Therapie",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        ClinicalGuideSourcesCard(listOf(Sources.FIGO_2018, Sources.BULL_2019))

        Muted(tr("Cette fiche est informative et ne remplace pas une consultation médicale avec un professionnel de santé.", "This guide is for informational purposes and does not replace consultation with a healthcare professional.", pl = "Ten artykuł ma charakter informacyjny i nie zastępuje konsultacji medycznej z osobą z personelu medycznego.", uk = "Ця стаття має інформаційний характер і не замінює медичну консультацію з медичним працівником.", es = "Esta ficha es informativa y no sustituye una consulta médica con un profesional sanitario.", de = "Dieser Artikel dient der Information und ersetzt keine ärztliche Beratung durch medizinisches Fachpersonal."))
    }
}

@Composable
fun AdenomyosisGuideScreen(onBack: () -> Unit) {
    val context = rememberPlatformContext()

    Page {
        PageTitle(
            tr("L'adénomyose", "Adenomyosis", pl = "Adenomioza", uk = "Аденоміоз", es = "La adenomiosis", de = "Adenomyose"),
            onBack = onBack,
            subtitle = tr("Repères, règles très abondantes et options de soulagement.", "Indicators, heavy menstrual bleeding and relief options.", pl = "Wskazówki, bardzo obfite miesiączki i możliwości łagodzenia dolegliwości.", uk = "Орієнтири, дуже рясні менструації та варіанти полегшення.", es = "Pautas, reglas muy abundantes y opciones de alivio.", de = "Orientierung, sehr starke Perioden und Möglichkeiten der Linderung."),
        )

        SectionTitle(tr("Qu'est-ce que l'adénomyose ?", "What is adenomyosis?", pl = "Czym jest adenomioza?", uk = "Що таке аденоміоз?", es = "¿Qué es la adenomiosis?", de = "Was ist Adenomyose?"))
        LunaCard {
            Text(
                tr(
                    fr = "L'adénomyose se caractérise par l'infiltration de cellules de l'endomètre (la muqueuse qui tapisse l'intérieur de l'utérus) dans l'épaisseur du muscle utérin (le myomètre). On la décrit couramment comme une « endométriose interne » de l'utérus.",
                    en = "Adenomyosis occurs when endometrial tissue (the lining inside the uterus) infiltrates the muscular uterine wall (myometrium). It is often called 'internal endometriosis'.",
                    pl = "Adenomiozę cechuje naciekanie komórek endometrium (błony śluzowej wyścielającej wnętrze macicy) w głąb mięśnia macicy (miometrium). Powszechnie określa się ją jako „endometriozę wewnętrzną” macicy.",
                    uk = "Аденоміоз характеризується проникненням клітин ендометрія (слизової оболонки, що вистилає матку зсередини) у товщу м'яза матки (міометрій). Його зазвичай описують як «внутрішній ендометріоз» матки.",
                    es = "La adenomiosis se caracteriza por la infiltración de células del endometrio (la mucosa que reviste el interior del útero) en el espesor del músculo uterino (el miometrio). Se describe con frecuencia como una «endometriosis interna» del útero.",
                    de = "Bei der Adenomyose wachsen Zellen des Endometriums (der Schleimhaut, die die Gebärmutter innen auskleidet) in die Dicke der Gebärmuttermuskulatur (das Myometrium) ein. Sie wird häufig als „innere Endometriose“ der Gebärmutter bezeichnet.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Ces foyers de muqueuse réagissent aux hormones de chaque cycle : ils saignent dans le muscle, provoquant une inflammation locale, des crampes douloureuses et une augmentation du volume de l'utérus.",
                    en = "These tissue patches respond to hormonal cycles by bleeding inside the uterine muscle, causing inflammation, intense cramps and an enlarged uterus.",
                    pl = "Te ogniska błony śluzowej reagują na hormony każdego cyklu: krwawią w obrębie mięśnia, powodując miejscowy stan zapalny, bolesne skurcze i powiększenie macicy.",
                    uk = "Ці осередки слизової оболонки реагують на гормони кожного циклу: вони кровоточать у м'язі, спричиняючи локальне запалення, болісні спазми та збільшення об'єму матки.",
                    es = "Estos focos de mucosa reaccionan a las hormonas de cada ciclo: sangran dentro del músculo, provocando inflamación local, calambres dolorosos y un aumento del volumen del útero.",
                    de = "Diese Schleimhautherde reagieren auf die Hormone jedes Zyklus: Sie bluten im Muskel und verursachen lokale Entzündungen, schmerzhafte Krämpfe und eine Vergrößerung der Gebärmutter.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Bullet(tr("Fréquence : elle touche 15 à 20 % des femmes et coexiste souvent avec l'endométriose pelvienne.", "Prevalence: affects 15 to 20% of women and frequently coexists with pelvic endometriosis.", pl = "Częstość: dotyczy 15 do 20 % kobiet i często współwystępuje z endometriozą miednicy.", uk = "Поширеність: стосується 15-20 % жінок і часто поєднується з ендометріозом органів малого тазу.", es = "Frecuencia: afecta al 15 a 20 % de las mujeres y coexiste con frecuencia con la endometriosis pélvica.", de = "Häufigkeit: Sie betrifft 15 bis 20 % der Frauen und tritt oft zusammen mit der Beckenendometriose auf."))
        }

        SectionTitle(tr("Les symptômes principaux", "Main symptoms", pl = "Główne objawy", uk = "Основні симптоми", es = "Los síntomas principales", de = "Die wichtigsten Symptome"))
        LunaCard {
            Bullet(tr("Règles très abondantes (ménorragies) : saignements prolongés (> 7 jours), abondants avec émission fréquente de caillots sanguins nécessitant de doubler les protections ou de se lever la nuit.", "Heavy menstrual bleeding (menorrhagia): prolonged bleeding (> 7 days), large blood clots and frequent protection changes.", pl = "Bardzo obfite miesiączki (menorrhagia): przedłużone krwawienia (> 7 dni), obfite, z częstym oddawaniem skrzepów, wymagające podwójnych zabezpieczeń lub wstawania w nocy.", uk = "Дуже рясні менструації (меноррагії): тривалі кровотечі (> 7 днів), рясні з частим виходом згустків крові, що вимагають подвійного захисту або нічного вставання.", es = "Reglas muy abundantes (menorragias): sangrados prolongados (> 7 días), abundantes, con expulsión frecuente de coágulos que obligan a doblar las protecciones o a levantarse por la noche.", de = "Sehr starke Perioden (Menorrhagien): verlängerte Blutungen (> 7 Tage), stark, mit häufigem Abgang von Blutgerinnseln, sodass man Hygieneartikel doppelt benötigt oder nachts aufstehen muss."))
            Bullet(tr("Crampes utérines violentes : contractions douloureuses avant et pendant les règles, résistant souvent aux antalgiques courants.", "Severe menstrual cramps: intense pain before and during periods that resists standard analgesics.", pl = "Silne skurcze macicy: bolesne skurcze przed miesiączką i w jej trakcie, często oporne na zwykłe leki przeciwbólowe.", uk = "Сильні маткові спазми: болісні скорочення перед менструацією та під час неї, які часто не піддаються звичайним знеболювальним.", es = "Calambres uterinos violentos: contracciones dolorosas antes y durante la regla, que a menudo no ceden con los analgésicos habituales.", de = "Heftige Gebärmutterkrämpfe: schmerzhafte Kontraktionen vor und während der Periode, die oft auf gängige Schmerzmittel nicht ansprechen."))
            Bullet(tr("Pesanteur pelvienne : sensation d'inconfort continu ou de bas-ventre lourd et gonflé.", "Pelvic heaviness: continuous lower abdominal discomfort or feelings of pressure.", pl = "Ciężkość w miednicy: uczucie ciągłego dyskomfortu lub ciężkiego, wzdętego podbrzusza.", uk = "Тяжкість у тазі: відчуття постійного дискомфорту або важкого, роздутого низу живота.", es = "Pesadez pélvica: sensación de molestia continua o de bajo vientre pesado e hinchado.", de = "Beckenschwere: Gefühl von anhaltendem Unbehagen oder einem schweren, geblähten Unterbauch."))
            Bullet(tr("Fatigue et carence en fer : les saignements abondants répétés épuisent les réserves de fer (anémie ferriprive), causant épuisement et essoufflement.", "Fatigue and iron deficiency: heavy blood loss depletes iron stores, causing persistent fatigue and shortness of breath.", pl = "Zmęczenie i niedobór żelaza: powtarzające się obfite krwawienia wyczerpują zapasy żelaza (niedokrwistość z niedoboru żelaza), powodując wyczerpanie i duszność.", uk = "Втома та дефіцит заліза: повторювані рясні кровотечі виснажують запаси заліза (залізодефіцитна анемія), спричиняючи виснаження й задишку.", es = "Cansancio y falta de hierro: los sangrados abundantes repetidos agotan las reservas de hierro (anemia ferropénica), lo que causa agotamiento y falta de aire.", de = "Müdigkeit und Eisenmangel: Wiederholte starke Blutungen erschöpfen die Eisenreserven (Eisenmangelanämie) und verursachen Erschöpfung und Atemnot."))
        }

        SectionTitle(tr("Le diagnostic", "Diagnosis", pl = "Diagnoza", uk = "Діагностика", es = "El diagnóstico", de = "Die Diagnose"))
        LunaCard {
            Text(
                tr(
                    fr = "Le diagnostic est posé grâce à des examens d'imagerie ciblés réalisés par des praticiens formés :",
                    en = "Diagnosis relies on specialized pelvic imaging performed by experienced practitioners:",
                    pl = "Diagnozę stawia się na podstawie ukierunkowanych badań obrazowych wykonywanych przez przeszkolonych specjalistów:",
                    uk = "Діагноз ставлять за допомогою цілеспрямованих візуалізаційних обстежень, які проводять підготовлені фахівці:",
                    es = "El diagnóstico se establece mediante pruebas de imagen dirigidas, realizadas por profesionales formados:",
                    de = "Die Diagnose wird durch gezielte bildgebende Untersuchungen gestellt, die von geschulten Fachleuten durchgeführt werden:",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Bullet(tr("Échographie pelvienne endovaginale : visualise l'asymétrie des parois utérines, l'aspect hétérogène du muscle et les micro-kystes myométriaux.", "Transvaginal ultrasound: shows wall asymmetry, heterogeneous muscle texture and myometrial microcysts.", pl = "Przezpochwowe USG miednicy: uwidacznia asymetrię ścian macicy, niejednorodny wygląd mięśnia i mikrotorbiele w miometrium.", uk = "Трансвагінальне УЗД малого тазу: виявляє асиметрію стінок матки, неоднорідний вигляд м'яза та мікрокісти в міометрії.", es = "Ecografía pélvica endovaginal: visualiza la asimetría de las paredes uterinas, el aspecto heterogéneo del músculo y los microquistes miometriales.", de = "Transvaginaler Beckenultraschall: zeigt die Asymmetrie der Gebärmutterwände, das ungleichmäßige Aussehen des Muskels und Mikrozysten im Myometrium."))
            Bullet(tr("IRM pelvienne : examen de référence mesurant l'épaississement de la « zone jonctionnelle » (> 12 mm) entre l'endomètre et le myomètre.", "Pelvic MRI: gold standard measuring the thickening of the junctional zone (> 12 mm).", pl = "Rezonans magnetyczny miednicy: badanie referencyjne mierzące pogrubienie „strefy połączenia” (> 12 mm) między endometrium a miometrium.", uk = "МРТ малого тазу: референтне обстеження, що вимірює потовщення «перехідної зони» (> 12 мм) між ендометрієм і міометрієм.", es = "RM pélvica: prueba de referencia que mide el engrosamiento de la «zona de unión» (> 12 mm) entre el endometrio y el miometrio.", de = "MRT des Beckens: Referenzuntersuchung, die die Verdickung der „Junktionalzone“ (> 12 mm) zwischen Endometrium und Myometrium misst."))
        }

        SectionTitle(tr("Pistes de soulagement", "Relief and treatments", pl = "Sposoby łagodzenia dolegliwości", uk = "Способи полегшення", es = "Vías de alivio", de = "Möglichkeiten der Linderung"))
        LunaCard {
            Bullet(tr("Anti-hémorragiques (acide tranexamique) : pris uniquement pendant les règles, il diminue d'environ 50 % l'abondance des saignements.", "Antifibrinolytics (tranexamic acid): taken during flow days to reduce blood loss volume by approx. 50%.", pl = "Leki przeciwkrwotoczne (kwas traneksamowy): przyjmowany wyłącznie w czasie miesiączki zmniejsza obfitość krwawienia o około 50 %.", uk = "Протикровотечні засоби (транексамова кислота): її приймають лише під час менструації, і вона зменшує рясність кровотечі приблизно на 50 %.", es = "Antihemorrágicos (ácido tranexámico): tomado solo durante la regla, reduce en torno a un 50 % la abundancia del sangrado.", de = "Blutstillende Mittel (Tranexamsäure): nur während der Periode eingenommen, verringert sie die Stärke der Blutung um etwa 50 %."))
            Bullet(tr("Contraception hormonale : stérilet hormonal au lévonorgestrel ou pilule progestative en continu pour atrophier la muqueuse et stopper les hémorragies.", "Hormonal therapy: levonorgestrel IUD or continuous progestin pill to thin the lining and stop heavy bleeding.", pl = "Antykoncepcja hormonalna: hormonalna wkładka z lewonorgestrelem lub ciągła tabletka gestagenowa, aby spowodować zanik błony śluzowej i zatrzymać krwawienia.", uk = "Гормональна контрацепція: гормональна спіраль із левоноргестрелом або безперервна прогестинова таблетка, щоб спричинити атрофію слизової оболонки та зупинити кровотечі.", es = "Anticoncepción hormonal: DIU hormonal con levonorgestrel o píldora de progestágeno continua para atrofiar la mucosa y detener las hemorragias.", de = "Hormonelle Verhütung: Hormonspirale mit Levonorgestrel oder durchgehend eingenommene Gestagen-Pille, um die Schleimhaut zurückzubilden und die Blutungen zu stoppen."))
            Bullet(tr("Recharger le fer : dosage de la ferritine et complémentation en fer (orale ou injectable si sévère) pour restaurer l'énergie.", "Iron repletion: ferritin checks and oral or IV iron supplements to resolve anemia.", pl = "Uzupełnianie żelaza: oznaczenie ferrytyny i suplementacja żelaza (doustna lub w iniekcjach w ciężkich przypadkach), aby przywrócić energię.", uk = "Поповнення заліза: визначення феритину та додавання заліза (перорально або ін'єкційно за тяжкого дефіциту), щоб відновити енергію.", es = "Recargar el hierro: dosificación de ferritina y suplementos de hierro (orales o inyectables si es grave) para recuperar la energía.", de = "Eisen auffüllen: Bestimmung des Ferritins und Eisenergänzung (oral, bei schwerem Mangel als Injektion), um die Energie wiederherzustellen."))
            Bullet(tr("Chaleur et antispasmodiques : bouillotte chaude sur le bas-ventre et repos pour relâcher les spasmes myométriaux.", "Heat and antispasmodics: heating pad on the lower abdomen to ease myometrial spasms.", pl = "Ciepło i leki rozkurczowe: ciepły termofor na podbrzuszu i odpoczynek, aby rozluźnić skurcze miometrium.", uk = "Тепло та спазмолітики: тепла грілка на низ живота й відпочинок, щоб зняти спазми міометрія.", es = "Calor y antiespasmódicos: bolsa de agua caliente sobre el bajo vientre y reposo para relajar los espasmos miometriales.", de = "Wärme und krampflösende Mittel: warme Wärmflasche auf dem Unterbauch und Ruhe, um die Spasmen des Myometriums zu lösen."))
            Bullet(tr("Gestes interventionnels : embolisation des artères utérines ou chirurgie discutées au cas par cas en cas d'échec des traitements médicaux.", "Interventional options: uterine artery embolization or surgery evaluated in severe cases.", pl = "Zabiegi interwencyjne: embolizacja tętnic macicznych lub operacja, omawiane indywidualnie, gdy leczenie farmakologiczne zawiedzie.", uk = "Інтервенційні втручання: емболізація маткових артерій або операція, які обговорюють в індивідуальному порядку у разі неефективності медикаментозного лікування.", es = "Procedimientos intervencionistas: la embolización de las arterias uterinas o la cirugía se valoran caso por caso si fracasan los tratamientos médicos.", de = "Interventionelle Maßnahmen: Embolisation der Gebärmutterarterien oder Operation werden im Einzelfall besprochen, wenn die medikamentösen Behandlungen nicht wirken."))
        }

        SectionTitle(
            tr(
                fr = "Ressources et accompagnement",
                en = "Resources & support",
                pl = "Wsparcie i źródła informacji",
                uk = "Підтримка та ресурси",
                es = "Recursos y apoyo",
                de = "Hilfe & Ressourcen",
            )
        )
        LunaCard {
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.endofrance.org/la-maladie-endometriose/adenomyose/",
                        en = "https://www.nhs.uk/conditions/adenomyosis/",
                        pl = "https://pokonacendometrioze.pl",
                        uk = "https://www.nhs.uk/conditions/adenomyosis/",
                        es = "https://adaec.org",
                        de = "https://www.endometriose-vereinigung.de",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "EndoFrance : dossier complet sur l'adénomyose (endofrance.org)",
                        en = "NHS guide to adenomyosis symptoms & care (nhs.uk)",
                        pl = "Fundacja Pokonać Endometriozę : informacje o adenomiozie (pokonacendometrioze.pl)",
                        uk = "Керівництво NHS щодо аденоміозу (nhs.uk)",
                        es = "ADAEC : información y apoyo sobre adenomiosis (adaec.org)",
                        de = "Endometriose-Vereinigung : Informationen zu Adenomyose",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.has-sante.fr/jcms/c_2819733/fr/prise-en-charge-de-l-endometriose",
                        en = "https://www.adenomyosisadviceassociation.org",
                        pl = "https://www.mp.pl/pacjent/ginekologia/choroby/260761,adenomioza",
                        uk = "https://www.adenomyosisadviceassociation.org",
                        es = "https://medlineplus.gov/spanish/ency/article/001513.htm",
                        de = "https://www.frauenaerzte-im-netz.de/erkrankungen/endometriose/formen-symptome/",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "Recommandations de prise en charge (HAS)",
                        en = "Adenomyosis Advice Association (adenomyosisadviceassociation.org)",
                        pl = "Medycyna Praktyczna : adenomioza macicy (mp.pl)",
                        uk = "Adenomyosis Advice Association (adenomyosisadviceassociation.org)",
                        es = "MedlinePlus : Adenomiosis uterina y diagnóstico",
                        de = "Frauenärzte im Netz : Adenomyose verstehen und behandeln",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        ClinicalGuideSourcesCard(
            listOf(
                Sources.ESHRE_ENDOMETRIOSIS_2022,
                Sources.NHS_HEAVY_PERIODS,
                Sources.FIGO_2018,
            ),
        )

        Muted(tr("Cette fiche est informative et ne remplace pas une consultation médicale avec un professionnel de santé.", "This guide is for informational purposes and does not replace consultation with a healthcare professional.", pl = "Ten artykuł ma charakter informacyjny i nie zastępuje konsultacji medycznej z osobą z personelu medycznego.", uk = "Ця стаття має інформаційний характер і не замінює медичну консультацію з медичним працівником.", es = "Esta ficha es informativa y no sustituye una consulta médica con un profesional sanitario.", de = "Dieser Artikel dient der Information und ersetzt keine ärztliche Beratung durch medizinisches Fachpersonal."))
    }
}

@Composable
fun PerimenopauseGuideScreen(onBack: () -> Unit) {
    val context = rememberPlatformContext()

    Page {
        PageTitle(
            tr("La périménopause", "Perimenopause", pl = "Perimenopauza", uk = "Перименопауза", es = "La perimenopausia", de = "Die Perimenopause"),
            onBack = onBack,
            subtitle = tr("Comprendre les variations hormonales et vivre sereinement la transition.", "Understanding hormonal fluctuations and navigating the transition.", pl = "Zrozumieć wahania hormonalne i spokojnie przejść przez tę zmianę.", uk = "Зрозуміти гормональні коливання та спокійно пройти цей перехід.", es = "Comprender las variaciones hormonales y vivir la transición con serenidad.", de = "Hormonelle Schwankungen verstehen und den Übergang gelassen erleben."),
        )

        SectionTitle(tr("Qu'est-ce que la périménopause ?", "What is perimenopause?", pl = "Czym jest perimenopauza?", uk = "Що таке перименопауза?", es = "¿Qué es la perimenopausia?", de = "Was ist die Perimenopause?"))
        LunaCard {
            Text(
                tr(
                    fr = "La périménopause est la phase naturelle de transition hormonale qui précède la ménopause (l'arrêt définitif des règles pendant 12 mois consécutifs).",
                    en = "Perimenopause is the natural transition period preceding menopause (defined as 12 consecutive months without periods).",
                    pl = "Perimenopauza to naturalna faza przejścia hormonalnego poprzedzająca menopauzę (trwałe ustanie miesiączek przez 12 kolejnych miesięcy).",
                    uk = "Перименопауза є природною фазою гормонального переходу, що передує менопаузі (остаточному припиненню менструацій упродовж 12 місяців поспіль).",
                    es = "La perimenopausia es la fase natural de transición hormonal que precede a la menopausia (el cese definitivo de la regla durante 12 meses consecutivos).",
                    de = "Die Perimenopause ist die natürliche Phase des hormonellen Übergangs vor der Menopause (dem endgültigen Ausbleiben der Periode über 12 aufeinanderfolgende Monate).",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                tr(
                    fr = "Elle débute le plus souvent entre 40 et 48 ans et dure en moyenne entre 2 et 8 ans. La réserve de follicules ovariens diminue et la production d'œstrogènes et de progestérone commence à fluctuer de manière imprévisible.",
                    en = "It typically begins between ages 40 and 48 and lasts 2 to 8 years on average. Ovarian follicle reserve declines, causing estrogen and progesterone to fluctuate.",
                    pl = "Najczęściej zaczyna się między 40. a 48. rokiem życia i trwa średnio od 2 do 8 lat. Rezerwa pęcherzyków jajnikowych maleje, a produkcja estrogenów i progesteronu zaczyna nieprzewidywalnie się zmieniać.",
                    uk = "Найчастіше вона починається між 40 і 48 роками та триває в середньому від 2 до 8 років. Запас яєчникових фолікулів зменшується, а вироблення естрогенів і прогестерону починає непередбачувано коливатися.",
                    es = "Suele comenzar entre los 40 y los 48 años y dura de media entre 2 y 8 años. La reserva de folículos ováricos disminuye y la producción de estrógenos y de progesterona empieza a fluctuar de forma impredecible.",
                    de = "Sie beginnt meist zwischen 40 und 48 Jahren und dauert durchschnittlich 2 bis 8 Jahre. Der Vorrat an Eierstockfollikeln nimmt ab, und die Bildung von Östrogen und Progesteron beginnt unvorhersehbar zu schwanken.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SectionTitle(tr("Les deux phases de la transition", "The two phases of transition", pl = "Dwie fazy przejścia", uk = "Дві фази переходу", es = "Las dos fases de la transición", de = "Die zwei Phasen des Übergangs"))
        LunaCard {
            Bullet(tr("1. Chute précoce de la progestérone : les ovulations deviennent irrégulières. Les œstrogènes ne sont plus contrebalancés : les cycles se raccourcissent (21 à 25 jours), les seins deviennent très sensibles, l'irritabilité et le syndrome prémenstruel s'accentuent.", "1. Early progesterone decline: ovulation becomes irregular. Estrogen dominates unopposed: cycles shorten (21-25 days), breast tenderness increases and PMS intensifies.", pl = "1. Wczesny spadek progesteronu: owulacje stają się nieregularne. Estrogeny nie są już równoważone: cykle się skracają (21 do 25 dni), piersi stają się bardzo tkliwe, nasilają się drażliwość i zespół napięcia przedmiesiączkowego.", uk = "1. Ранній спад прогестерону: овуляції стають нерегулярними. Естрогени більше не врівноважуються: цикли скорочуються (21-25 днів), груди стають дуже чутливими, посилюються дратівливість і передменструальний синдром.", es = "1. Descenso precoz de la progesterona: las ovulaciones se vuelven irregulares. Los estrógenos ya no están contrarrestados: los ciclos se acortan (21 a 25 días), los pechos se vuelven muy sensibles y se acentúan la irritabilidad y el síndrome premenstrual.", de = "1. Früher Abfall des Progesterons: Die Eisprünge werden unregelmäßig. Das Östrogen wird nicht mehr ausgeglichen: Die Zyklen verkürzen sich (21 bis 25 Tage), die Brüste werden sehr empfindlich, Reizbarkeit und prämenstruelles Syndrom nehmen zu."))
            Bullet(tr("2. Chute tardive des œstrogènes : les cycles s'allongent considérablement (parfois 2 à 4 mois sans règles), les flux deviennent imprévisibles, et les symptômes vasomoteurs apparaissent.", "2. Late estrogen decline: cycles lengthen significantly (2-4 months between periods), flow becomes unpredictable, and vasomotor symptoms emerge.", pl = "2. Późny spadek estrogenów: cykle znacznie się wydłużają (czasem 2 do 4 miesięcy bez miesiączki), krwawienia stają się nieprzewidywalne i pojawiają się objawy naczynioruchowe.", uk = "2. Пізній спад естрогенів: цикли значно подовжуються (іноді 2-4 місяці без менструації), кровотечі стають непередбачуваними, з'являються вазомоторні симптоми.", es = "2. Descenso tardío de los estrógenos: los ciclos se alargan considerablemente (a veces 2 a 4 meses sin regla), los flujos se vuelven impredecibles y aparecen los síntomas vasomotores.", de = "2. Später Abfall des Östrogens: Die Zyklen werden deutlich länger (manchmal 2 bis 4 Monate ohne Periode), die Blutungen werden unvorhersehbar, und vasomotorische Symptome treten auf."))
        }

        SectionTitle(tr("Les manifestations fréquentes", "Common symptoms", pl = "Częste objawy", uk = "Часті прояви", es = "Las manifestaciones frecuentes", de = "Häufige Beschwerden"))
        LunaCard {
            Bullet(tr("Bouffées de chaleur et sueurs nocturnes : sensation soudaine de chaleur intense remontant au cou et au visage, parfois suivie de frissons.", "Hot flashes & night sweats: sudden waves of intense heat over the face and neck, sometimes followed by chills.", pl = "Uderzenia gorąca i nocne poty: nagłe uczucie intensywnego gorąca, które wędruje ku szyi i twarzy, czasem po którym pojawiają się dreszcze.", uk = "Припливи жару та нічне пітніння: раптове відчуття сильного жару, що піднімається до шиї та обличчя, інколи з наступним ознобом.", es = "Sofocos y sudores nocturnos: sensación repentina de calor intenso que sube hacia el cuello y la cara, a veces seguida de escalofríos.", de = "Hitzewallungen und nächtliches Schwitzen: plötzliches Gefühl intensiver Hitze, die zu Hals und Gesicht aufsteigt und manchmal von Schüttelfrost gefolgt wird."))
            Bullet(tr("Troubles du sommeil : difficultés d'endormissement et réveils nocturnes fréquents (souvent vers 3h-4h).", "Sleep disruption: difficulty falling asleep and frequent night awakenings (often around 3-4 a.m.).", pl = "Zaburzenia snu: trudności z zasypianiem i częste nocne przebudzenia (często około 3-4 w nocy).", uk = "Порушення сну: труднощі із засинанням і часті нічні пробудження (нерідко близько 3-4 години ранку).", es = "Trastornos del sueño: dificultad para conciliar el sueño y despertares nocturnos frecuentes (a menudo hacia las 3-4 h).", de = "Schlafstörungen: Einschlafschwierigkeiten und häufiges nächtliches Erwachen (oft gegen 3 bis 4 Uhr)."))
            Bullet(tr("Brouillard mental (« brain fog ») : sensation de tête confuse, trous de mémoire ponctuels et baisse de concentration passagère.", "Brain fog: temporary concentration lapses and occasional memory blanks.", pl = "Mgła mózgowa („brain fog”): uczucie mętlika w głowie, okresowe luki w pamięci i przemijające pogorszenie koncentracji.", uk = "Туман у голові («brain fog»): відчуття сплутаності думок, окремі провали в пам'яті та тимчасове зниження концентрації.", es = "Niebla mental («brain fog»): sensación de cabeza confusa, lapsus de memoria puntuales y bajada pasajera de la concentración.", de = "Gedankennebel („Brain Fog“): Gefühl eines benebelten Kopfes, gelegentliche Gedächtnislücken und vorübergehend nachlassende Konzentration."))
            Bullet(tr("Variations de l'humeur : anxiété nouvelle, irritabilité ou moral fluctuant liés aux vagues d'œstrogènes.", "Mood changes: heightened anxiety or irritability linked to shifting hormone levels.", pl = "Wahania nastroju: nowo pojawiający się lęk, drażliwość lub zmienny nastrój związane z falami estrogenów.", uk = "Коливання настрою: нова тривожність, дратівливість або мінливий настрій, пов'язані з хвилями естрогенів.", es = "Variaciones del estado de ánimo: ansiedad nueva, irritabilidad o ánimo fluctuante vinculados a las oleadas de estrógenos.", de = "Stimmungsschwankungen: neu auftretende Angst, Reizbarkeit oder schwankende Stimmung durch die Östrogenwellen."))
            Bullet(tr("Sécheresse intime : inconfort vaginal lors des rapports, attribuable à la baisse progressive d'imprégnation œstrogénique.", "Vaginal dryness: intimate discomfort during sex due to lower estrogen support.", pl = "Suchość intymna: dyskomfort w pochwie podczas współżycia, wynikający ze stopniowego spadku działania estrogenów.", uk = "Інтимна сухість: вагінальний дискомфорт під час статевого акту, зумовлений поступовим зниженням естрогенної насиченості.", es = "Sequedad íntima: molestias vaginales durante las relaciones, atribuibles a la disminución progresiva del efecto de los estrógenos.", de = "Intime Trockenheit: Unbehagen in der Scheide beim Geschlechtsverkehr, bedingt durch die allmählich sinkende Östrogenwirkung."))
        }

        SectionTitle(tr("Pistes d'accompagnement et bien-être", "Support and well-being options", pl = "Wsparcie i dobre samopoczucie", uk = "Підтримка та самопочуття", es = "Vías de acompañamiento y bienestar", de = "Begleitung und Wohlbefinden"))
        LunaCard {
            Bullet(tr("Activité physique régulière : le renforcement musculaire et les exercices avec impact (marche active, danse) protègent la densité osseuse et la masse musculaire.", "Regular physical activity: strength training and weight-bearing exercise protect bone density and muscle mass.", pl = "Regularna aktywność fizyczna: trening siłowy i ćwiczenia z obciążeniem (szybki marsz, taniec) chronią gęstość kości i masę mięśniową.", uk = "Регулярна фізична активність: силові вправи та вправи з навантаженням (жвава ходьба, танці) захищають щільність кісток і м'язову масу.", es = "Actividad física regular: el fortalecimiento muscular y los ejercicios con impacto (caminata activa, baile) protegen la densidad ósea y la masa muscular.", de = "Regelmäßige Bewegung: Krafttraining und Übungen mit Belastung (zügiges Gehen, Tanzen) schützen die Knochendichte und die Muskelmasse."))
            Bullet(tr("Alimentation protectrice : apport suffisant en protéines, calcium (produits laitiers, eaux minérales calciques) et vitamine D.", "Bone & metabolic nutrition: adequate protein, calcium and vitamin D intake.", pl = "Dieta ochronna: wystarczająca podaż białka, wapnia (produkty mleczne, wody mineralne bogate w wapń) i witaminy D.", uk = "Захисне харчування: достатнє надходження білка, кальцію (молочні продукти, кальцієві мінеральні води) і вітаміну D.", es = "Alimentación protectora: aporte suficiente de proteínas, calcio (lácteos, aguas minerales ricas en calcio) y vitamina D.", de = "Schützende Ernährung: ausreichende Zufuhr von Eiweiß, Calcium (Milchprodukte, calciumreiche Mineralwässer) und Vitamin D."))
            Bullet(tr("Modérer les déclencheurs de bouffées : l'alcool, le café, les repas très épicés et le stress aigu accentuent la fréquence des bouffées de chaleur.", "Moderate triggers: alcohol, caffeine, spicy foods and acute stress commonly trigger hot flashes.", pl = "Ograniczać czynniki wyzwalające uderzenia gorąca: alkohol, kawa, bardzo ostre potrawy i silny stres zwiększają częstość uderzeń gorąca.", uk = "Помірковано ставитися до тригерів припливів: алкоголь, кава, дуже гостра їжа та гострий стрес збільшують частоту припливів жару.", es = "Moderar los desencadenantes de los sofocos: el alcohol, el café, las comidas muy picantes y el estrés agudo aumentan la frecuencia de los sofocos.", de = "Auslöser von Hitzewallungen mäßigen: Alkohol, Kaffee, sehr scharfe Mahlzeiten und akuter Stress erhöhen die Häufigkeit der Hitzewallungen."))
            Bullet(tr("Traitement hormonal de la ménopause (THM) : peut être discuté avec un soignant si les symptômes altèrent la qualité de vie, avec un bilan bénéfice/risque personnalisé.", "Menopausal hormone therapy (MHT): can be evaluated with a doctor if symptoms substantially affect daily quality of life.", pl = "Menopauzalna terapia hormonalna (MTH): można ją omówić z osobą z personelu medycznego, jeśli objawy pogarszają jakość życia, z indywidualną oceną korzyści i ryzyka.", uk = "Менопаузальна гормональна терапія (МГТ): її можна обговорити з медичним працівником, якщо симптоми погіршують якість життя, з індивідуальною оцінкою користі та ризику.", es = "Tratamiento hormonal de la menopausia (THM): puede plantearse con un profesional sanitario si los síntomas afectan a la calidad de vida, con una valoración personalizada del beneficio y el riesgo.", de = "Menopausale Hormontherapie (MHT): Sie kann mit medizinischem Fachpersonal besprochen werden, wenn die Beschwerden die Lebensqualität beeinträchtigen, mit einer individuellen Nutzen-Risiko-Abwägung."))
            Bullet(tr("Dans Lunaison : notez vos bouffées et troubles du sommeil dans votre carnet, et activez le mode « Cycles irréguliers » dans les Réglages.", "In Lunaison: track hot flashes and sleep in your log, and enable 'Irregular cycles' mode in Settings.", pl = "W Lunaison: zapisuj uderzenia gorąca i zaburzenia snu w dzienniku oraz włącz tryb „Naturalnie nieregularne cykle” w Ustawieniach.", uk = "У Lunaison: записуйте припливи жару та порушення сну у своєму щоденнику й увімкніть режим «Природно нерегулярні цикли» в Налаштуваннях.", es = "En Lunaison: anota tus sofocos y trastornos del sueño en tu registro y activa el modo «Ciclos naturalmente irregulares» en Ajustes.", de = "In Lunaison: Trage deine Hitzewallungen und Schlafstörungen in dein Tagebuch ein und aktiviere in den Einstellungen den Modus „Natürlich unregelmäßige Zyklen“."))
            Bullet(tr("Contraception : tant que la ménopause n'est pas confirmée (12 mois consécutifs sans règles), une grossesse reste possible malgré des cycles irréguliers.", "Contraception: until menopause is confirmed (12 consecutive months without a period), pregnancy remains possible despite irregular cycles.", pl = "Antykoncepcja: dopóki menopauza nie jest potwierdzona (12 kolejnych miesięcy bez miesiączki), ciąża nadal jest możliwa mimo nieregularnych cyklów.", uk = "Контрацепція: поки менопауза не підтверджена (12 місяців поспіль без менструації), вагітність усе ще можлива попри нерегулярні цикли.", es = "Anticoncepción: mientras no se confirme la menopausia (12 meses consecutivos sin regla), sigue siendo posible un embarazo aunque los ciclos sean irregulares.", de = "Verhütung: Solange die Menopause nicht bestätigt ist (12 aufeinanderfolgende Monate ohne Periode), bleibt eine Schwangerschaft trotz unregelmäßiger Zyklen möglich."))
            Bullet(tr("Saignements à signaler : des règles très abondantes ou prolongées, ou tout saignement survenant après 12 mois sans règles, doivent amener à consulter.", "Bleeding to report: very heavy or prolonged periods, or any bleeding occurring after 12 months without a period, should be evaluated by a clinician.", pl = "Krwawienia wymagające konsultacji: bardzo obfite lub przedłużone miesiączki albo każde krwawienie po 12 miesiącach bez miesiączki wymagają wizyty lekarskiej.", uk = "Кровотечі, про які варто повідомити: дуже рясні чи тривалі менструації або будь-яка кровотеча після 12 місяців без менструації потребують консультації лікаря.", es = "Sangrados que conviene consultar: reglas muy abundantes o prolongadas, o cualquier sangrado tras 12 meses sin regla, requieren valoración médica.", de = "Abzuklärende Blutungen: Sehr starke oder verlängerte Perioden sowie jede Blutung nach 12 Monaten ohne Periode sollten ärztlich abgeklärt werden."))
        }

        SectionTitle(
            tr(
                fr = "Ressources et accompagnement",
                en = "Resources & support",
                pl = "Wsparcie i źródła informacji",
                uk = "Підтримка та ресурси",
                es = "Recursos y apoyo",
                de = "Hilfe & Ressourcen",
            )
        )
        LunaCard {
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.gemvi.org",
                        en = "https://www.themenopausecharity.org",
                        pl = "https://pacjent.gov.pl/aktualnosc/klimakterium-okres-przekwitania",
                        uk = "https://www.themenopausecharity.org",
                        es = "https://aeem.es",
                        de = "https://www.menopause-gesellschaft.de",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "GEMVI : Société savante sur la ménopause (gemvi.org)",
                        en = "The Menopause Charity: evidence-based information (themenopausecharity.org)",
                        pl = "Pacjent.gov.pl : Klimakterium i menopauza",
                        uk = "The Menopause Charity: науково підтверджена інформація (themenopausecharity.org)",
                        es = "AEEM : Asociación Española para el Estudio de la Menopausia (aeem.es)",
                        de = "Deutsche Menopause Gesellschaft e.V. (menopause-gesellschaft.de)",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
            TextButton(onClick = {
                openLink(
                    context,
                    tr(
                        fr = "https://www.inserm.fr/dossier/menopause/",
                        en = "https://www.nhs.uk/conditions/menopause/",
                        pl = "https://www.mp.pl/pacjent/ginekologia/choroby/260751,klimakterium-i-menopauza",
                        uk = "https://www.nhs.uk/conditions/menopause/",
                        es = "https://medlineplus.gov/spanish/menopause.html",
                        de = "https://www.frauenaerzte-im-netz.de/koerper-sexualitaet/wechseljahre-klimakterium/",
                    ),
                )
            }) {
                Text(
                    tr(
                        fr = "Dossier scientifique de l'Inserm sur la ménopause",
                        en = "NHS guide to perimenopause and menopause (nhs.uk)",
                        pl = "Medycyna Praktyczna : objawy i terapia (mp.pl)",
                        uk = "Керівництво NHS щодо менопаузи (nhs.uk)",
                        es = "MedlinePlus : Información médica sobre perimenopausia",
                        de = "Frauenärzte im Netz : Wechseljahre und Perimenopause",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        ClinicalGuideSourcesCard(listOf(Sources.FIGO_2018, Sources.NHS_HEAVY_PERIODS))

        Muted(tr("Cette fiche est informative et ne remplace pas une consultation médicale avec un professionnel de santé.", "This guide is for informational purposes and does not replace consultation with a healthcare professional.", pl = "Ten artykuł ma charakter informacyjny i nie zastępuje konsultacji medycznej z osobą z personelu medycznego.", uk = "Ця стаття має інформаційний характер і не замінює медичну консультацію з медичним працівником.", es = "Esta ficha es informativa y no sustituye una consulta médica con un profesional sanitario.", de = "Dieser Artikel dient der Information und ersetzt keine ärztliche Beratung durch medizinisches Fachpersonal."))
    }
}
