package fr.lunaison.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
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
internal fun CycleVisualGuide() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(tr("Schéma des 4 temps du cycle", "The 4 cycle phases diagram", pl = "Schemat 4 etapów cyklu", uk = "Схема 4 етапів циклу", es = "Esquema de los 4 tiempos del ciclo", de = "Schema der 4 Zyklusphasen"))
        LunaCard {
            Text(
                tr("Le cycle menstruel en 4 étapes clés :", "The menstrual cycle in 4 key stages:", pl = "Cykl miesiączkowy w 4 kluczowych etapach:", uk = "Менструальний цикл у 4 ключових етапи:", es = "El ciclo menstrual en 4 etapas clave:", de = "Der Menstruationszyklus in 4 Schlüsselphasen:"),
                style = MaterialTheme.typography.titleSmall,
            )

            // Frise visuelle horizontale avec 4 segments proportionnels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .height(16.dp),
            ) {
                // Règles J1-J5
                Box(
                    modifier = Modifier
                        .weight(5f)
                        .fillMaxHeight()
                        .background(Color(0xFFE8998D)),
                )
                // Folliculaire J6-J13
                Box(
                    modifier = Modifier
                        .weight(8f)
                        .fillMaxHeight()
                        .background(Color(0xFFB4D6C4)),
                )
                // Ovulation J14
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .background(Color(0xFFF9D162)),
                )
                // Lutéale J15-J28
                Box(
                    modifier = Modifier
                        .weight(13f)
                        .fillMaxHeight()
                        .background(Color(0xFFD3BBD9)),
                )
            }

            PhaseExplainerRow(
                color = Color(0xFFE8998D),
                phase = tr("1. Les règles (J1 à J5)", "1. Period (Days 1 to 5)", pl = "1. Miesiączka (dni 1 do 5)", uk = "1. Менструація (дні 1-5)", es = "1. La regla (días 1 a 5)", de = "1. Die Periode (Tage 1 bis 5)"),
                detail = tr("La muqueuse intérieure de l'utérus (l'endomètre) se détache et s'élimine. Les taux d'hormones sont au plus bas.", "The inner uterine lining (endometrium) sheds. Hormone levels are at their lowest.", pl = "Wewnętrzna błona śluzowa macicy (endometrium) się złuszcza i jest usuwana. Poziom hormonów jest najniższy.", uk = "Внутрішня слизова оболонка матки (ендометрій) відшаровується та виводиться. Рівень гормонів найнижчий.", es = "La mucosa interior del útero (el endometrio) se desprende y se elimina. Los niveles hormonales están en su punto más bajo.", de = "Die innere Schleimhaut der Gebärmutter (das Endometrium) löst sich ab und wird ausgeschieden. Die Hormonspiegel sind am niedrigsten."),
            )
            PhaseExplainerRow(
                color = Color(0xFFB4D6C4),
                phase = tr("2. Phase folliculaire (J6 à J13)", "2. Follicular phase (Days 6 to 13)", pl = "2. Faza folikularna (dni 6 do 13)", uk = "2. Фолікулярна фаза (дні 6-13)", es = "2. Fase folicular (días 6 a 13)", de = "2. Follikelphase (Tage 6 bis 13)"),
                detail = tr("Un follicule mûrit dans l'ovaire. Les œstrogènes montent pour reconstruire la muqueuse et préparer une éventuelle fécondation.", "An ovarian follicle matures. Estrogens rise to rebuild the uterine lining.", pl = "W jajniku dojrzewa pęcherzyk. Estrogeny rosną, aby odbudować błonę śluzową i przygotować ją na ewentualne zapłodnienie.", uk = "У яєчнику дозріває фолікул. Естрогени зростають, щоб відновити слизову оболонку та підготувати можливе запліднення.", es = "Un folículo madura en el ovario. Los estrógenos suben para reconstruir la mucosa y preparar una posible fecundación.", de = "Ein Follikel reift im Eierstock heran. Das Östrogen steigt an, um die Schleimhaut wieder aufzubauen und eine mögliche Befruchtung vorzubereiten."),
            )
            PhaseExplainerRow(
                color = Color(0xFFF9D162),
                phase = tr("3. L'ovulation (~ J14)", "3. Ovulation (~ Day 14)", pl = "3. Owulacja (~ dzień 14)", uk = "3. Овуляція (~ 14-й день)", es = "3. La ovulación (~ día 14)", de = "3. Der Eisprung (~ Tag 14)"),
                detail = tr("L'ovaire libère l'ovule mûr. Il ne survit qu'entre 12 et 24 heures.", "The ovary releases a mature egg. It survives for 12 to 24 hours.", pl = "Jajnik uwalnia dojrzałą komórkę jajową. Przeżywa tylko od 12 do 24 godzin.", uk = "Яєчник вивільняє зрілу яйцеклітину. Вона живе лише від 12 до 24 годин.", es = "El ovario libera el óvulo maduro. Solo sobrevive entre 12 y 24 horas.", de = "Der Eierstock gibt die reife Eizelle frei. Sie überlebt nur 12 bis 24 Stunden."),
            )
            PhaseExplainerRow(
                color = Color(0xFFD3BBD9),
                phase = tr("4. Phase lutéale (J15 à J28)", "4. Luteal phase (Days 15 to 28)", pl = "4. Faza lutealna (dni 15 do 28)", uk = "4. Лютеїнова фаза (дні 15-28)", es = "4. Fase lútea (días 15 a 28)", de = "4. Lutealphase (Tage 15 bis 28)"),
                detail = tr("La progestérone domine pour maintenir la muqueuse. Sans grossesse, elle s'effondre et déclenche les règles suivantes.", "Progesterone dominates to maintain the lining. Without pregnancy, it drops, triggering the next period.", pl = "Dominuje progesteron, który podtrzymuje błonę śluzową. Bez ciąży jego poziom gwałtownie spada i wywołuje kolejną miesiączkę.", uk = "Переважає прогестерон, що підтримує слизову оболонку. Без вагітності його рівень різко падає й запускає наступну менструацію.", es = "La progesterona predomina para mantener la mucosa. Sin embarazo, se desploma y desencadena la regla siguiente.", de = "Progesteron dominiert, um die Schleimhaut zu erhalten. Ohne Schwangerschaft fällt es stark ab und löst die nächste Periode aus."),
            )
        }

        HormoneCurvesDiagram()

        InteractiveUterusDiagram()

        VulvaDiagram()

        SectionTitle(tr("L'anatomie interne sans tabou", "Internal anatomy explained", pl = "Anatomia wewnętrzna bez tabu", uk = "Внутрішня анатомія без табу", es = "La anatomía interna sin tabúes", de = "Die innere Anatomie ohne Tabus"))
        LunaCard {
            Text(
                tr("Les repères clés du corps féminin :", "Key body reference points:", pl = "Kluczowe punkty orientacyjne kobiecego ciała:", uk = "Ключові орієнтири жіночого тіла:", es = "Las referencias clave del cuerpo femenino:", de = "Die wichtigsten Orientierungspunkte des weiblichen Körpers:"),
                style = MaterialTheme.typography.titleSmall,
            )
            AnatomyItemRow(
                title = tr("La vulve (externe)", "The vulva (external)", pl = "Srom (zewnętrzny)", uk = "Вульва (зовнішня)", es = "La vulva (externa)", de = "Die Vulva (außen)"),
                description = tr("Tous les organes visibles à l'extérieur : lèvres, clitoris, méat urinaire et entrée du vagin. Souvent confondue avec le vagin !", "All visible external parts: labia, clitoris, urethra and vaginal opening. Often confused with the vagina!", pl = "Wszystkie narządy widoczne na zewnątrz: wargi, łechtaczka, ujście cewki moczowej i wejście do pochwy. Często mylony z pochwą!", uk = "Усі органи, видимі ззовні: губи, клітор, отвір сечівника та вхід до піхви. Її часто плутають із піхвою!", es = "Todos los órganos visibles por fuera: labios, clítoris, meato urinario y entrada de la vagina. ¡A menudo se confunde con la vagina!", de = "Alle außen sichtbaren Organe: Schamlippen, Klitoris, Harnröhrenöffnung und Scheideneingang. Wird oft mit der Vagina verwechselt!"),
            )
            AnatomyItemRow(
                title = tr("Le vagin (interne)", "The vagina (internal)", pl = "Pochwa (wewnętrzna)", uk = "Піхва (внутрішня)", es = "La vagina (interna)", de = "Die Vagina (innen)"),
                description = tr("Le conduit musculaire souple reliant la vulve au col de l'utérus. C'est par lui que s'écoulent les règles.", "The flexible muscular canal connecting the vulva to the cervix. It is where menstrual blood flows.", pl = "Elastyczny kanał mięśniowy łączący srom z szyjką macicy. To przez niego wypływa krew menstruacyjna.", uk = "Гнучкий м'язовий канал, що з'єднує вульву з шийкою матки. Саме ним виходить менструальна кров.", es = "El conducto muscular flexible que une la vulva con el cuello del útero. Por él sale la sangre de la regla.", de = "Der flexible Muskelschlauch, der die Vulva mit dem Gebärmutterhals verbindet. Durch ihn fließt die Periodenblutung ab."),
            )
            AnatomyItemRow(
                title = tr("Le col de l'utérus (cervix)", "The cervix", pl = "Szyjka macicy (cervix)", uk = "Шийка матки (цервікс)", es = "El cuello del útero (cérvix)", de = "Der Gebärmutterhals (Zervix)"),
                description = tr("La partie basse de l'utérus. Il produit la glaire cervicale qui change au fil du mois et s'entrouvre légèrement pendant les règles.", "The lower part of the uterus. It secretes cervical mucus that changes texture throughout the month and opens slightly during periods.", pl = "Dolna część macicy. Wytwarza śluz szyjkowy, który zmienia się w ciągu miesiąca, i lekko się otwiera podczas miesiączki.", uk = "Нижня частина матки. Він виробляє цервікальний слиз, що змінюється впродовж місяця, і трохи розкривається під час менструації.", es = "La parte baja del útero. Produce el moco cervical, que cambia a lo largo del mes, y se entreabre ligeramente durante la regla.", de = "Der untere Teil der Gebärmutter. Er bildet den Zervixschleim, der sich im Lauf des Monats verändert, und öffnet sich während der Periode leicht."),
            )
            AnatomyItemRow(
                title = tr("L'utérus et le myomètre (muscle)", "The uterus & myometrium (muscle)", pl = "Macica i miometrium (mięsień)", uk = "Матка та міометрій (м'яз)", es = "El útero y el miometrio (músculo)", de = "Die Gebärmutter und das Myometrium (Muskel)"),
                description = tr("Un muscle creux puissant de la taille d'une petite poire. Sa paroi musculaire (le myomètre) se contracte lors des règles sous l'effet des prostaglandines.", "A powerful hollow pear-sized muscle. Its muscular wall (myometrium) contracts during periods under the effect of prostaglandins.", pl = "Silny, wydrążony mięsień wielkości małej gruszki. Jego ściana mięśniowa (miometrium) kurczy się podczas miesiączki pod wpływem prostaglandyn.", uk = "Потужний порожнистий м'яз завбільшки з невелику грушу. Його м'язова стінка (міометрій) скорочується під час менструації під дією простагландинів.", es = "Un músculo hueco y potente del tamaño de una pera pequeña. Su pared muscular (el miometrio) se contrae durante la regla por efecto de las prostaglandinas.", de = "Ein kräftiger Hohlmuskel von der Größe einer kleinen Birne. Seine Muskelwand (das Myometrium) zieht sich während der Periode unter dem Einfluss der Prostaglandine zusammen."),
            )
            AnatomyItemRow(
                title = tr("L'endomètre (muqueuse interne)", "The endometrium (inner lining)", pl = "Endometrium (błona śluzowa wewnętrzna)", uk = "Ендометрій (внутрішня слизова оболонка)", es = "El endometrio (mucosa interna)", de = "Das Endometrium (innere Schleimhaut)"),
                description = tr("La muqueuse vivante qui tapisse l'intérieur de l'utérus. Elle s'épaissit chaque mois, s'enrichit en vaisseaux, puis desquame si aucune grossesse ne débute : ce sont les règles.", "The living tissue lining the inside of the uterus. It thickens each month, vascularizes, then sheds if no pregnancy occurs: this is menstruation.", pl = "Żywa błona śluzowa wyściełająca wnętrze macicy. Co miesiąc pogrubia się, wzbogaca w naczynia krwionośne, a następnie złuszcza się, jeśli nie rozpocznie się ciąża: to właśnie miesiączka.", uk = "Жива слизова оболонка, що вистилає внутрішню поверхню матки. Щомісяця вона потовщується, збагачується судинами, а потім відшаровується, якщо вагітність не настала: це і є менструація.", es = "La mucosa viva que reviste el interior del útero. Cada mes se engrosa, se enriquece de vasos y luego se descama si no empieza un embarazo: es la regla.", de = "Die lebendige Schleimhaut, die die Gebärmutter innen auskleidet. Sie baut sich jeden Monat auf, bekommt mehr Gefäße und wird dann abgestoßen, wenn keine Schwangerschaft beginnt: das ist die Periode."),
            )
            AnatomyItemRow(
                title = tr("Les trompes de Fallope", "The fallopian tubes", pl = "Jajowody", uk = "Маткові труби", es = "Las trompas de Falopio", de = "Die Eileiter"),
                description = tr("Deux conduits fins reliant les ovaires à l'utérus, où a lieu la fécondation éventuelle d'un ovocyte.", "Two slender ducts connecting ovaries to uterus, where potential egg fertilization takes place.", pl = "Dwa cienkie przewody łączące jajniki z macicą, w których może dojść do zapłodnienia komórki jajowej.", uk = "Дві тонкі труби, що з'єднують яєчники з маткою, де може відбутися запліднення яйцеклітини.", es = "Dos conductos finos que unen los ovarios con el útero, donde puede producirse la fecundación de un óvulo.", de = "Zwei dünne Gänge, die die Eierstöcke mit der Gebärmutter verbinden und in denen eine Eizelle befruchtet werden kann."),
            )
            AnatomyItemRow(
                title = tr("Les ovaires (maturation & hormones)", "The ovaries (maturation & hormones)", pl = "Jajniki (dojrzewanie i hormony)", uk = "Яєчники (дозрівання та гормони)", es = "Los ovarios (maduración y hormonas)", de = "Die Eierstöcke (Reifung und Hormone)"),
                description = tr("Deux glandes qui abritent les ovocytes dès la naissance et fabriquent les œstrogènes et la progestérone au fil du cycle.", "Two glands containing egg cells from birth and producing estrogen and progesterone throughout the cycle.", pl = "Dwa gruczoły, które od urodzenia zawierają komórki jajowe i wytwarzają estrogeny oraz progesteron w ciągu cyklu.", uk = "Дві залози, що з народження містять яйцеклітини та виробляють естрогени й прогестерон протягом циклу.", es = "Dos glándulas que albergan los óvulos desde el nacimiento y fabrican los estrógenos y la progesterona a lo largo del ciclo.", de = "Zwei Drüsen, die seit der Geburt die Eizellen enthalten und im Lauf des Zyklus Östrogen und Progesteron bilden."),
            )
        }

        SectionTitle(tr("Signes visibles : la glaire cervicale", "Visible signs: cervical mucus", pl = "Widoczne oznaki: śluz szyjkowy", uk = "Видимі ознаки: цервікальний слиз", es = "Signos visibles: el moco cervical", de = "Sichtbare Zeichen: der Zervixschleim"))
        LunaCard {
            Text(
                tr("Comment la glaire change au fil du mois :", "How cervical mucus changes during the cycle:", pl = "Jak śluz zmienia się w ciągu miesiąca:", uk = "Як слиз змінюється впродовж місяця:", es = "Cómo cambia el moco a lo largo del mes:", de = "So verändert sich der Zervixschleim im Lauf des Monats:"),
                style = MaterialTheme.typography.titleSmall,
            )
            MucusStageRow(
                stage = tr("Après les règles", "After period", pl = "Po miesiączce", uk = "Після менструації", es = "Después de la regla", de = "Nach der Periode"),
                texture = tr("Sensation plutôt sèche, peu ou pas de glaire visible.", "Rather dry feeling, little to no visible mucus.", pl = "Raczej sucho, mało śluzu lub brak widocznego śluzu.", uk = "Відчуття радше сухості, слизу мало або його не видно.", es = "Sensación más bien seca, poco o nada de moco visible.", de = "Eher trockenes Gefühl, wenig oder kein sichtbarer Schleim."),
            )
            MucusStageRow(
                stage = tr("Avant l'ovulation", "Before ovulation", pl = "Przed owulacją", uk = "Перед овуляцією", es = "Antes de la ovulación", de = "Vor dem Eisprung"),
                texture = tr("Glaire crémeuse, blanchâtre ou collante.", "Creamy, whitish or sticky mucus.", pl = "Śluz kremowy, białawy lub lepki.", uk = "Слиз кремоподібний, білуватий або липкий.", es = "Moco cremoso, blanquecino o pegajoso.", de = "Cremiger, weißlicher oder klebriger Schleim."),
            )
            MucusStageRow(
                stage = tr("Autour de l'ovulation", "Around ovulation", pl = "Wokół owulacji", uk = "Навколо овуляції", es = "Alrededor de la ovulación", de = "Rund um den Eisprung"),
                texture = tr("Glaire abondante, transparente et étirable entre deux doigts (comme du blanc d'œuf cru). Elle aide les spermatozoïdes.", "Abundant, clear and stretchy between two fingers (like raw egg white). It helps sperm.", pl = "Śluz obfity, przezroczysty i ciągnący się między dwoma palcami (jak surowe białko jaja). Pomaga plemnikom.", uk = "Слиз рясний, прозорий і тягучий між двома пальцями (як сирий яєчний білок). Він допомагає сперматозоїдам.", es = "Moco abundante, transparente y elástico entre dos dedos (como clara de huevo cruda). Ayuda a los espermatozoides.", de = "Reichlicher, durchsichtiger Schleim, der sich zwischen zwei Fingern ziehen lässt (wie rohes Eiweiß). Er unterstützt die Spermien."),
            )
            MucusStageRow(
                stage = tr("Après l'ovulation", "After ovulation", pl = "Po owulacji", uk = "Після овуляції", es = "Después de la ovulación", de = "Nach dem Eisprung"),
                texture = tr("Retour rapide à une glaire opaque, pâteuse ou sensation sèche.", "Quick return to opaque, pasty mucus or dry feeling.", pl = "Szybki powrót do śluzu nieprzezroczystego, gęstego lub uczucia suchości.", uk = "Швидке повернення до непрозорого, пастоподібного слизу або відчуття сухості.", es = "Vuelta rápida a un moco opaco, pastoso o a una sensación seca.", de = "Schnelle Rückkehr zu undurchsichtigem, pastösem Schleim oder zu einem trockenen Gefühl."),
            )
        }
    }
}

@Composable
private fun PhaseExplainerRow(color: Color, phase: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(12.dp)
                .background(color, CircleShape),
        )
        Column {
            Text(phase, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnatomyItemRow(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MucusStageRow(stage: String, texture: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(stage, style = MaterialTheme.typography.titleSmall)
        Text(texture, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HormoneCurvesDiagram() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val estrogenColor = if (isDark) Color(0xFF66C79F) else Color(0xFF2E7D5E)
    val progesteroneColor = if (isDark) Color(0xFFF08A78) else Color(0xFFC04E3A)
    val lhColor = if (isDark) Color(0xFFF4C554) else Color(0xFFD49B24)
    val baselineColor = MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(tr("La courbe des hormones (cycle de 28 jours)", "Hormone level curves (28-day cycle)", pl = "Krzywe hormonów (cykl 28-dniowy)", uk = "Криві гормонів (28-денний цикл)", es = "La curva de las hormonas (ciclo de 28 días)", de = "Die Hormonkurven (28-Tage-Zyklus)"))
        LunaCard {
            Text(
                tr("Évolution des 3 hormones clés sur un cycle naturel :", "Evolution of the 3 key hormones over a natural cycle:", pl = "Zmiany 3 kluczowych hormonów w cyklu naturalnym:", uk = "Зміна 3 ключових гормонів протягом природного циклу:", es = "Evolución de las 3 hormonas clave en un ciclo natural:", de = "Verlauf der 3 wichtigsten Hormone in einem natürlichen Zyklus:"),
                style = MaterialTheme.typography.titleSmall,
            )

            // Canvas graphique de la dynamique hormonale
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                val padTop = 16.dp.toPx()
                val padBottom = 16.dp.toPx()
                val h = size.height - padTop - padBottom
                val w = size.width

                fun x(day: Float): Float = ((day - 1f) / 27f).coerceIn(0f, 1f) * w
                fun y(level: Float): Float = padTop + (1f - level.coerceIn(0f, 1f)) * h

                // Bandes pastel des 4 phases
                // Règles J1-J5
                drawRect(
                    color = Color(0xFFE8998D).copy(alpha = 0.16f),
                    topLeft = Offset(x(1f), 0f),
                    size = Size(x(5f) - x(1f), size.height),
                )
                // Folliculaire J5-J13
                drawRect(
                    color = Color(0xFFB4D6C4).copy(alpha = 0.16f),
                    topLeft = Offset(x(5f), 0f),
                    size = Size(x(13f) - x(5f), size.height),
                )
                // Ovulation J13-J15
                drawRect(
                    color = Color(0xFFF9D162).copy(alpha = 0.25f),
                    topLeft = Offset(x(13f), 0f),
                    size = Size(x(15f) - x(13f), size.height),
                )
                // Lutéale J15-J28
                drawRect(
                    color = Color(0xFFD3BBD9).copy(alpha = 0.16f),
                    topLeft = Offset(x(15f), 0f),
                    size = Size(x(28f) - x(15f), size.height),
                )

                // Ligne de base
                drawLine(
                    color = baselineColor,
                    start = Offset(0f, y(0f)),
                    end = Offset(w, y(0f)),
                    strokeWidth = 1.dp.toPx(),
                )

                // 1. Œstrogènes (Vert sauge / émeraude - Trait continu)
                val estrogenPoints = listOf(
                    Offset(x(1f), y(0.12f)),
                    Offset(x(5f), y(0.16f)),
                    Offset(x(9f), y(0.38f)),
                    Offset(x(12f), y(0.78f)),
                    Offset(x(13f), y(0.92f)),
                    Offset(x(14f), y(0.48f)),
                    Offset(x(17f), y(0.42f)),
                    Offset(x(21f), y(0.62f)),
                    Offset(x(25f), y(0.32f)),
                    Offset(x(28f), y(0.12f)),
                )
                drawSmoothCurve(estrogenPoints, estrogenColor, 3.dp.toPx())

                // 2. Progestérone (Terracotta chaud - Trait tireté pour accessibilité daltonisme WCAG 1.4.1)
                val progesteronePoints = listOf(
                    Offset(x(1f), y(0.06f)),
                    Offset(x(12f), y(0.06f)),
                    Offset(x(14f), y(0.12f)),
                    Offset(x(16f), y(0.35f)),
                    Offset(x(19f), y(0.78f)),
                    Offset(x(21.5f), y(0.88f)),
                    Offset(x(24f), y(0.65f)),
                    Offset(x(26.5f), y(0.24f)),
                    Offset(x(28f), y(0.06f)),
                )
                val progDash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                drawSmoothCurve(progesteronePoints, progesteroneColor, 3.dp.toPx(), pathEffect = progDash)

                // 3. Pic de LH (Ambre doré - Trait pointillé)
                val lhPoints = listOf(
                    Offset(x(1f), y(0.08f)),
                    Offset(x(11f), y(0.08f)),
                    Offset(x(12.5f), y(0.28f)),
                    Offset(x(13.5f), y(0.96f)),
                    Offset(x(14.5f), y(0.26f)),
                    Offset(x(16f), y(0.08f)),
                    Offset(x(28f), y(0.08f)),
                )
                val lhDots = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 6f), 0f)
                drawSmoothCurve(lhPoints, lhColor, 2.5.dp.toPx(), pathEffect = lhDots)
            }

            // Repères temporels horizontaux
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(tr("J1 (Règles)", "D1 (Period)", pl = "D1 (Miesiączka)", uk = "Д1 (Менструація)", es = "D1 (Regla)", de = "T1 (Periode)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tr("J7", "D7", pl = "D7", uk = "Д7", es = "D7", de = "T7"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tr("J14 (Ovulation)", "D14 (Ovulation)", pl = "D14 (Owulacja)", uk = "Д14 (Овуляція)", es = "D14 (Ovulación)", de = "T14 (Eisprung)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(tr("J21", "D21", pl = "D21", uk = "Д21", es = "D21", de = "T21"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tr("J28", "D28", pl = "D28", uk = "Д28", es = "D28", de = "T28"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Légende descriptive avec repère visuel tactile
            HormoneLegendItem(
                color = estrogenColor,
                name = tr("Œstrogènes (Ligne continue)", "Estrogen (Solid line)", pl = "Estrogeny (linia ciągła)", uk = "Естрогени (суцільна лінія)", es = "Estrógenos (línea continua)", de = "Östrogen (durchgezogene Linie)"),
                role = tr("Pic pré-ovulatoire. Stimule le renouvellement de l'endomètre, fluidifie la glaire cervicale et soutient l'énergie.", "Peaks before ovulation. Stimulates endometrial renewal, thins cervical mucus and supports energy.", pl = "Szczyt przedowulacyjny. Pobudza odnowę endometrium, rozrzedza śluz szyjkowy i wspiera energię.", uk = "Передовуляторний пік. Стимулює оновлення ендометрія, розріджує цервікальний слиз і підтримує енергію.", es = "Pico preovulatorio. Estimula la renovación del endometrio, fluidifica el moco cervical y sostiene la energía.", de = "Höhepunkt vor dem Eisprung. Regt die Erneuerung der Gebärmutterschleimhaut an, macht den Zervixschleim flüssiger und stützt die Energie."),
            )
            HormoneLegendItem(
                color = progesteroneColor,
                name = tr("Progestérone (Ligne tiretée)", "Progesterone (Dashed line)", pl = "Progesteron (linia przerywana)", uk = "Прогестерон (штрихова лінія)", es = "Progesterona (línea discontinua)", de = "Progesteron (gestrichelte Linie)"),
                role = tr("Pic en milieu de phase lutéale (sécrétée par le corps jaune). Maintient l'endomètre réceptif pour une éventuelle nidation. Sa chute déclenche les règles.", "Peaks in mid-luteal phase (secreted by corpus luteum). Maintains endometrial readiness for implantation. Its drop triggers menstruation.", pl = "Szczyt w środku fazy lutealnej (wydzielany przez ciałko żółte). Utrzymuje endometrium gotowe na ewentualne zagnieżdżenie. Jego spadek wywołuje miesiączkę.", uk = "Пік у середині лютеїнової фази (виробляється жовтим тілом). Підтримує ендометрій готовим до можливої імплантації. Його падіння запускає менструацію.", es = "Pico a mitad de la fase lútea (secretada por el cuerpo lúteo). Mantiene el endometrio receptivo para una posible implantación. Su descenso desencadena la regla.", de = "Höhepunkt in der Mitte der Lutealphase (vom Gelbkörper gebildet). Hält die Gebärmutterschleimhaut für eine mögliche Einnistung aufnahmebereit. Sein Abfall löst die Periode aus."),
            )
            HormoneLegendItem(
                color = lhColor,
                name = tr("Pic de LH (Ligne pointillée)", "LH surge (Dotted line)", pl = "Szczyt LH (linia kropkowana)", uk = "Пік ЛГ (пунктирна лінія)", es = "Pico de LH (línea de puntos)", de = "LH-Gipfel (gepunktete Linie)"),
                role = tr("Décharge brève et intense à J13-J14 qui déclenche l'expulsion de l'ovule par le follicule en 24 à 36 heures.", "Brief, intense surge at Days 13-14 that triggers egg release within 24 to 36 hours.", pl = "Krótki, intensywny wyrzut w dniach 13-14, który w ciągu 24 do 36 godzin powoduje uwolnienie komórki jajowej z pęcherzyka.", uk = "Короткий інтенсивний викид на 13-14-й день, що протягом 24-36 годин спричиняє вивільнення яйцеклітини з фолікула.", es = "Descarga breve e intensa en los días 13-14 que desencadena la expulsión del óvulo por el folículo en 24 a 36 horas.", de = "Kurze, intensive Ausschüttung an den Tagen 13 und 14, die innerhalb von 24 bis 36 Stunden die Freisetzung der Eizelle aus dem Follikel auslöst."),
            )
        }
    }
}

private fun DrawScope.drawSmoothCurve(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
    pathEffect: androidx.compose.ui.graphics.PathEffect? = null,
) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val cx = (p0.x + p1.x) / 2f
        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = pathEffect,
        ),
    )
}

@Composable
private fun HormoneLegendItem(color: Color, name: String, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .background(color, CircleShape),
        )
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class UterusPart {
    ENDOMETRIUM,
    MYOMETRIUM,
    CERVIX,
    TUBES,
    OVARIES,
    VAGINA,
}

private fun uterusPartTitle(part: UterusPart): String = when (part) {
    UterusPart.ENDOMETRIUM -> tr("L'endomètre (muqueuse utérine)", "The endometrium (uterine lining)", pl = "Endometrium (błona śluzowa macicy)", uk = "Ендометрій (слизова оболонка матки)", es = "El endometrio (mucosa uterina)", de = "Das Endometrium (Gebärmutterschleimhaut)")
    UterusPart.MYOMETRIUM -> tr("Le myomètre (paroi musculaire)", "The myometrium (muscular wall)", pl = "Miometrium (ściana mięśniowa)", uk = "Міометрій (м'язова стінка)", es = "El miometrio (pared muscular)", de = "Das Myometrium (Muskelwand)")
    UterusPart.CERVIX -> tr("Le col de l'utérus (zone de passage)", "The cervix (cervical canal)", pl = "Szyjka macicy (strefa przejścia)", uk = "Шийка матки (зона проходу)", es = "El cuello del útero (zona de paso)", de = "Der Gebärmutterhals (Durchgangszone)")
    UterusPart.TUBES -> tr("Les trompes de Fallope (conduits utéro-ovariens)", "The fallopian tubes (uterine tubes)", pl = "Jajowody (przewody macicy i jajników)", uk = "Маткові труби (канали між маткою та яєчниками)", es = "Las trompas de Falopio (conductos útero-ováricos)", de = "Die Eileiter (Verbindungsgänge zwischen Gebärmutter und Eierstöcken)")
    UterusPart.OVARIES -> tr("Les ovaires (glandes hormonales et reproductrices)", "The ovaries (hormonal and reproductive glands)", pl = "Jajniki (gruczoły hormonalne i rozrodcze)", uk = "Яєчники (гормональні та репродуктивні залози)", es = "Los ovarios (glándulas hormonales y reproductoras)", de = "Die Eierstöcke (Hormon- und Keimdrüsen)")
    UterusPart.VAGINA -> tr("Le vagin (canal musculaire élastique)", "The vagina (elastic muscular canal)", pl = "Pochwa (elastyczny kanał mięśniowy)", uk = "Піхва (еластичний м'язовий канал)", es = "La vagina (conducto muscular elástico)", de = "Die Vagina (elastischer Muskelkanal)")
}

internal fun uterusPartChipLabel(part: UterusPart): String = when (part) {
    UterusPart.ENDOMETRIUM -> tr("Endomètre (muqueuse)", "Endometrium (lining)", pl = "Endometrium (błona śluzowa)", uk = "Ендометрій (слизова)", es = "Endometrio (mucosa)", de = "Endometrium (Schleimhaut)")
    UterusPart.MYOMETRIUM -> tr("Myomètre (muscle)", "Myometrium (muscle)", pl = "Miometrium (mięsień)", uk = "Міометрій (м'яз)", es = "Miometrio (músculo)", de = "Myometrium (Muskel)")
    UterusPart.CERVIX -> tr("Col de l'utérus", "Cervix", pl = "Szyjka macicy", uk = "Шийка матки", es = "Cuello del útero", de = "Gebärmutterhals")
    UterusPart.TUBES -> tr("Trompes de Fallope", "Fallopian tubes", pl = "Jajowody", uk = "Маткові труби", es = "Trompas de Falopio", de = "Eileiter")
    UterusPart.OVARIES -> tr("Ovaires (hormones)", "Ovaries (hormones)", pl = "Jajniki (hormony)", uk = "Яєчники (гормони)", es = "Ovarios (hormonas)", de = "Eierstöcke (Hormone)")
    UterusPart.VAGINA -> tr("Vagin (interne)", "Vagina (internal)", pl = "Pochwa (wewnętrzna)", uk = "Піхва (внутрішня)", es = "Vagina (interna)", de = "Vagina (innen)")
}

private fun uterusPartSummary(part: UterusPart): String = when (part) {
    UterusPart.ENDOMETRIUM -> tr(
        fr = "La muqueuse vivante qui tapisse la cavité interne de l'utérus. Chaque mois, sous l'effet des œstrogènes puis de la progestérone, elle s'épaissit et développe de fins vaisseaux sanguins pour préparer une éventuelle implantation embryonnaire (nidation). En l'absence de fécondation, la chute hormonale entraîne son décollement et son élimination : c'est le flux menstruel des règles. Il s'agit d'un processus naturel de renouvellement tissulaire.",
        en = "The mucous membrane lining the inner uterine cavity. Each month, influenced by estrogen and progesterone, it thickens and develops micro-vessels in preparation for potential embryo implantation. If pregnancy does not occur, hormone decline causes this layer to shed and flow outward: this is menstruation. It is a natural process of regular tissue renewal.",
        pl = "Żywa błona śluzowa wyściełająca wnętrze jamy macicy. Co miesiąc, pod wpływem estrogenów, a następnie progesteronu, pogrubia się i wytwarza drobne naczynia krwionośne, aby przygotować się na ewentualne zagnieżdżenie zarodka (implantację). Gdy nie dojdzie do zapłodnienia, spadek poziomu hormonów powoduje jej oddzielenie i usunięcie: to krwawienie menstruacyjne. Jest to naturalny proces odnowy tkanki.",
        uk = "Жива слизова оболонка, що вистилає внутрішню порожнину матки. Щомісяця під дією естрогенів, а потім прогестерону вона потовщується й утворює дрібні кровоносні судини, щоб підготуватися до можливої імплантації ембріона. Якщо запліднення не відбулося, зниження рівня гормонів спричиняє її відшарування й виведення: це менструальна кровотеча. Це природний процес оновлення тканини.",
        es = "La mucosa viva que reviste la cavidad interna del útero. Cada mes, por efecto de los estrógenos y después de la progesterona, se engrosa y desarrolla pequeños vasos sanguíneos para preparar una posible implantación embrionaria (anidación). Si no hay fecundación, la caída hormonal provoca su desprendimiento y su eliminación: es el flujo menstrual de la regla. Se trata de un proceso natural de renovación del tejido.",
        de = "Die lebendige Schleimhaut, die die Gebärmutterhöhle auskleidet. Jeden Monat baut sie sich unter dem Einfluss von Östrogen und dann Progesteron auf und bildet feine Blutgefäße, um eine mögliche Einnistung des Embryos vorzubereiten. Bleibt die Befruchtung aus, führt der Hormonabfall zu ihrer Ablösung und Ausscheidung: das ist die Menstruationsblutung. Es handelt sich um einen natürlichen Prozess der Gewebeerneuerung.",
    )
    UterusPart.MYOMETRIUM -> tr(
        fr = "L'épaisse paroi musculaire qui compose le corps de l'utérus. Pendant les règles, il effectue des contractions rythmées, déclenchées par les prostaglandines, pour faciliter l'évacuation de la muqueuse utérine. Lorsque ces contractions sont intenses, la compression temporaire des petits vaisseaux sanguins réduit brièvement l'apport en oxygène des tissus, provoquant la sensation de crampe. La chaleur (bouillotte, bain tiède) soulage en détendant les fibres musculaires. En revanche, des douleurs invalidantes qui empêchent les activités quotidiennes ne sont pas normales et justifient un avis médical (suspicion d'endométriose ou d'adénomyose).",
        en = "The thick muscular wall that forms the body of the uterus. During menstruation, it contracts in rhythmic waves triggered by prostaglandins to help shed the uterine lining. Strong contractions can temporarily compress local micro-vessels, briefly reducing oxygen delivery and causing cramp pain. Applying gentle heat relaxes the muscle fibers. However, debilitating pain that disrupts daily activities is not normal and warrants medical evaluation for conditions such as endometriosis or adenomyosis.",
        pl = "Gruba ściana mięśniowa tworząca trzon macicy. Podczas miesiączki wykonuje rytmiczne skurcze, wywoływane przez prostaglandyny, aby ułatwić usunięcie błony śluzowej macicy. Gdy te skurcze są silne, chwilowe uciskanie drobnych naczyń krwionośnych krótko zmniejsza dopływ tlenu do tkanek, co wywołuje uczucie skurczu. Ciepło (termofor, ciepła kąpiel) przynosi ulgę, rozluźniając włókna mięśniowe. Natomiast bóle uniemożliwiające codzienne czynności nie są normalne i uzasadniają poradę lekarską (podejrzenie endometriozy lub adenomiozy).",
        uk = "Товста м'язова стінка, що утворює тіло матки. Під час менструації вона виконує ритмічні скорочення, спричинені простагландинами, щоб полегшити виведення слизової оболонки матки. Коли ці скорочення інтенсивні, тимчасове стискання дрібних кровоносних судин ненадовго зменшує надходження кисню до тканин, що викликає відчуття спазму. Тепло (грілка, тепла ванна) полегшує стан, розслабляючи м'язові волокна. Натомість біль, що унеможливлює повсякденну діяльність, не є нормою і потребує консультації лікаря (підозра на ендометріоз чи аденоміоз).",
        es = "La gruesa pared muscular que forma el cuerpo del útero. Durante la regla realiza contracciones rítmicas, desencadenadas por las prostaglandinas, para facilitar la evacuación de la mucosa uterina. Cuando estas contracciones son intensas, la compresión temporal de los pequeños vasos sanguíneos reduce brevemente el aporte de oxígeno a los tejidos y provoca la sensación de calambre. El calor (bolsa de agua caliente, baño templado) alivia al relajar las fibras musculares. En cambio, los dolores incapacitantes que impiden las actividades cotidianas no son normales y justifican una consulta médica (sospecha de endometriosis o de adenomiosis).",
        de = "Die dicke Muskelwand, die den Gebärmutterkörper bildet. Während der Periode zieht sie sich rhythmisch zusammen, ausgelöst durch Prostaglandine, um das Ausstoßen der Gebärmutterschleimhaut zu erleichtern. Sind diese Kontraktionen stark, drückt sie vorübergehend kleine Blutgefäße ab, was die Sauerstoffversorgung des Gewebes kurz verringert und das Krampfgefühl auslöst. Wärme (Wärmflasche, warmes Bad) lindert, indem sie die Muskelfasern entspannt. Schmerzen, die den Alltag unmöglich machen, sind dagegen nicht normal und sollten ärztlich abgeklärt werden (Verdacht auf Endometriose oder Adenomyose).",
    )
    UterusPart.CERVIX -> tr(
        fr = "La portion cylindrique inférieure reliant la cavité utérine au fond du vagin. Il assure un rôle protecteur et fonctionnel : la majeure partie du cycle, son canal est scellé par un bouchon muqueux dense et acide qui fait obstacle aux bactéries extérieures. À l'approche de l'ovulation, il s'assouplit et produit une glaire limpide et fluide facilitant le passage des spermatozoïdes. Pendant les règles, son orifice s'entrouvre légèrement pour laisser s'écouler le flux menstruel.",
        en = "The lower cylindrical portion of the uterus connecting the uterine cavity to the upper vagina. It provides both protection and guidance: during most of the cycle, its canal is closed by thick, acidic mucus blocking bacterial entry. Near ovulation, it softens and secretes clear, fluid mucus facilitating sperm passage. During menstruation, it slightly relaxes to allow menstrual flow to pass.",
        pl = "Dolna, cylindryczna część łącząca jamę macicy z głębią pochwy. Pełni funkcję ochronną i czynnościową: przez większą część cyklu jego kanał jest zamknięty gęstym, kwaśnym czopem śluzowym, który stanowi barierę dla bakterii z zewnątrz. W okolicach owulacji staje się bardziej miękki i wytwarza przejrzysty, płynny śluz ułatwiający przechodzenie plemników. Podczas miesiączki jego ujście lekko się otwiera, by umożliwić odpływ krwi menstruacyjnej.",
        uk = "Нижня циліндрична частина, що з'єднує порожнину матки з глибиною піхви. Вона виконує захисну та функціональну роль: більшу частину циклу її канал закритий щільною кислою слизовою пробкою, яка перешкоджає проникненню бактерій ззовні. Наближаючись до овуляції, вона стає м'якшою й виробляє прозорий рідкий слиз, що полегшує проходження сперматозоїдів. Під час менструації її отвір трохи розкривається, щоб пропустити менструальні виділення.",
        es = "La porción cilíndrica inferior que une la cavidad uterina con el fondo de la vagina. Cumple una función protectora y funcional: durante la mayor parte del ciclo, su canal está sellado por un tapón mucoso denso y ácido que impide el paso de bacterias del exterior. Al acercarse la ovulación, se ablanda y produce un moco límpido y fluido que facilita el paso de los espermatozoides. Durante la regla, su orificio se entreabre ligeramente para dejar salir el flujo menstrual.",
        de = "Der untere, zylinderförmige Abschnitt, der die Gebärmutterhöhle mit dem Scheidengewölbe verbindet. Er hat eine schützende und funktionelle Aufgabe: Den größten Teil des Zyklus ist sein Kanal von einem dichten, sauren Schleimpfropf verschlossen, der Bakterien von außen abhält. Vor dem Eisprung wird er weicher und bildet einen klaren, flüssigen Schleim, der den Spermien den Durchtritt erleichtert. Während der Periode öffnet sich seine Öffnung leicht, damit die Menstruationsblutung abfließen kann.",
    )
    UterusPart.TUBES -> tr(
        fr = "Deux fins conduits musculo-membraneux d'environ 10 à 12 cm reliant les ovaires à l'utérus. Leurs pavillons bordés de franges mobiles (les fimbriae) captent l'ovocyte dès son expulsion de l'ovaire. C'est dans le tiers externe de la trompe qu'a lieu la fécondation si un spermatozoïde rencontre l'ovule. Des micro-cils vibratiles et des mouvements péristaltiques acheminent ensuite l'ovocyte ou l'embryon vers la cavité utérine.",
        en = "Two delicate channels (10 to 12 cm long) linking each ovary to the uterus. Their flared outer ends, fringed with mobile fingers (fimbriae), capture the released egg at ovulation. Fertilization occurs in the outer portion of the tube when sperm is present. Microscopic cilia and gentle muscular contractions then transport the egg or embryo toward the uterine cavity.",
        pl = "Dwa cienkie przewody mięśniowo-błoniaste o długości około 10 do 12 cm, łączące jajniki z macicą. Ich lejkowate końce, obrzeżone ruchomymi frędzlami (strzępkami), wychwytują komórkę jajową zaraz po jej uwolnieniu z jajnika. W zewnętrznej jednej trzeciej jajowodu dochodzi do zapłodnienia, jeśli plemnik spotka komórkę jajową. Mikroskopijne rzęski i ruchy perystaltyczne przenoszą następnie komórkę jajową lub zarodek do jamy macicy.",
        uk = "Дві тонкі м'язово-перетинчасті труби завдовжки близько 10-12 см, що з'єднують яєчники з маткою. Їхні лійкоподібні кінці з рухомою бахромою (фімбріями) вловлюють яйцеклітину одразу після виходу з яєчника. У зовнішній третині труби відбувається запліднення, якщо сперматозоїд зустрічає яйцеклітину. Мікроскопічні війки та перистальтичні рухи потім просувають яйцеклітину або ембріон до порожнини матки.",
        es = "Dos conductos finos, musculomembranosos, de unos 10 a 12 cm, que unen los ovarios con el útero. Sus pabellones, bordeados de franjas móviles (las fimbrias), recogen el óvulo en cuanto sale del ovario. La fecundación tiene lugar en el tercio externo de la trompa si un espermatozoide encuentra el óvulo. Unas pestañas vibrátiles microscópicas y los movimientos peristálticos llevan después el óvulo o el embrión hacia la cavidad uterina.",
        de = "Zwei dünne Muskel-Schleimhaut-Gänge von etwa 10 bis 12 cm Länge, die die Eierstöcke mit der Gebärmutter verbinden. Ihre trichterförmigen Enden mit beweglichen Fransen (den Fimbrien) fangen die Eizelle auf, sobald sie den Eierstock verlässt. Im äußeren Drittel des Eileiters findet die Befruchtung statt, wenn ein Spermium auf die Eizelle trifft. Winzige Flimmerhärchen und Muskelbewegungen befördern die Eizelle oder den Embryo anschließend in die Gebärmutterhöhle.",
    )
    UterusPart.OVARIES -> tr(
        fr = "Deux glandes en forme d'amande situées de chaque côté de l'utérus. Dès la naissance, elles renferment la réserve complète d'ovocytes sous forme de follicules au repos. À chaque cycle, une cohorte de follicules entre en phase de maturation sous contrôle hormonal, et le plus développé libère un ovule (ovulation). Les ovaires sont aussi les principaux producteurs d'hormones sexuelles : les œstrogènes (croissance des tissus et vitalité) et la progestérone (stabilisation de la muqueuse utérine).",
        en = "Two almond-shaped glands nestled on either side of the uterus. From birth, they contain the complete lifetime reserve of egg cells in dormant follicles. Each cycle, a cohort of follicles matures under hormonal cues, and the leading follicle releases an egg (ovulation). The ovaries also produce primary sex hormones: estrogen (supporting tissue growth and vitality) and progesterone (stabilizing the uterine lining).",
        pl = "Dwa gruczoły w kształcie migdałów, położone po obu stronach macicy. Od urodzenia zawierają pełną rezerwę komórek jajowych w postaci uśpionych pęcherzyków. W każdym cyklu grupa pęcherzyków wchodzi w fazę dojrzewania pod kontrolą hormonów, a najbardziej rozwinięty uwalnia komórkę jajową (owulacja). Jajniki są też głównym źródłem hormonów płciowych: estrogenów (wzrost tkanek i witalność) oraz progesteronu (stabilizacja błony śluzowej macicy).",
        uk = "Дві залози мигдалеподібної форми, розташовані по боках від матки. З народження вони містять повний запас яйцеклітин у вигляді фолікулів у стані спокою. У кожному циклі група фолікулів вступає у фазу дозрівання під гормональним контролем, а найрозвиненіший вивільняє яйцеклітину (овуляція). Яєчники також є основними виробниками статевих гормонів: естрогенів (ріст тканин і життєві сили) та прогестерону (стабілізація слизової оболонки матки).",
        es = "Dos glándulas en forma de almendra situadas a cada lado del útero. Desde el nacimiento contienen toda la reserva de óvulos en forma de folículos en reposo. En cada ciclo, un grupo de folículos entra en fase de maduración bajo control hormonal, y el más desarrollado libera un óvulo (ovulación). Los ovarios son también los principales productores de hormonas sexuales: los estrógenos (crecimiento de los tejidos y vitalidad) y la progesterona (estabilización de la mucosa uterina).",
        de = "Zwei mandelförmige Drüsen zu beiden Seiten der Gebärmutter. Sie enthalten seit der Geburt den gesamten Vorrat an Eizellen in Form ruhender Follikel. In jedem Zyklus tritt eine Gruppe von Follikeln unter hormoneller Steuerung in die Reifung ein, und der am weitesten entwickelte gibt eine Eizelle frei (Eisprung). Die Eierstöcke sind außerdem die wichtigsten Bildungsorte der Geschlechtshormone: Östrogen (Gewebewachstum und Vitalität) und Progesteron (Stabilisierung der Gebärmutterschleimhaut).",
    )
    UterusPart.VAGINA -> tr(
        fr = "Le canal fibro-musculaire souple reliant la vulve au col de l'utérus. Au repos, ses parois antérieures et postérieures sont naturellement en contact. Sa muqueuse pourvue de plis transversaux lui confère une grande souplesse et une capacité d'extension indolore, lui permettant d'accueillir tampons, coupes menstruelles ou rapports sexuels, ainsi que le passage du bébé lors de l'accouchement, avant de retrouver ses dimensions de repos. Son milieu acide protecteur est préservé par une flore bactérienne équilibrée (lactobacilles), sans nécessiter de lavage interne.",
        en = "The flexible muscular canal connecting the vulva to the cervix. At rest, its anterior and posterior walls naturally rest against each other. Transverse mucosal folds provide great elasticity, allowing it to stretch comfortably to accommodate tampons, menstrual cups, or intimacy, as well as childbirth, before returning to resting dimensions. Its protective acidic ecosystem is maintained by balanced flora (lactobacilli), requiring no internal washing.",
        pl = "Elastyczny kanał włóknisto-mięśniowy łączący srom z szyjką macicy. W spoczynku jego ściany przednia i tylna naturalnie przylegają do siebie. Błona śluzowa z poprzecznymi fałdami nadaje mu dużą elastyczność i zdolność bezbolesnego rozciągania, dzięki czemu może pomieścić tampony, kubeczki menstruacyjne lub współżycie, a także przejście dziecka podczas porodu, po czym wraca do wymiarów spoczynkowych. Jego ochronne, kwaśne środowisko utrzymuje zrównoważona flora bakteryjna (pałeczki kwasu mlekowego), bez potrzeby płukania wewnętrznego.",
        uk = "Гнучкий волокнисто-м'язовий канал, що з'єднує вульву з шийкою матки. У спокої його передня й задня стінки природно прилягають одна до одної. Слизова оболонка з поперечними складками надає йому великої еластичності та здатності безболісно розтягуватися, що дає змогу приймати тампони, менструальні чаші чи статевий акт, а також пропускати дитину під час пологів, після чого він повертається до звичайних розмірів. Його захисне кисле середовище підтримується збалансованою бактеріальною флорою (лактобактерії), без потреби внутрішнього промивання.",
        es = "El conducto fibromuscular flexible que une la vulva con el cuello del útero. En reposo, sus paredes anterior y posterior están naturalmente en contacto. Su mucosa, provista de pliegues transversales, le da una gran flexibilidad y una capacidad de distensión indolora, lo que le permite acoger tampones, copas menstruales o relaciones sexuales, así como el paso del bebé durante el parto, antes de recuperar sus dimensiones de reposo. Su medio ácido protector se mantiene gracias a una flora bacteriana equilibrada (lactobacilos), sin necesidad de lavados internos.",
        de = "Der flexible, fibromuskuläre Kanal, der die Vulva mit dem Gebärmutterhals verbindet. In Ruhe liegen seine vordere und hintere Wand natürlich aneinander. Seine Schleimhaut mit Querfalten verleiht ihm große Elastizität und die Fähigkeit, sich schmerzlos zu dehnen, sodass er Tampons, Menstruationstassen oder Geschlechtsverkehr aufnehmen kann, ebenso wie den Durchtritt des Babys bei der Geburt, bevor er wieder seine Ruhemaße annimmt. Sein schützendes saures Milieu wird durch eine ausgewogene Bakterienflora (Laktobazillen) aufrechterhalten, ganz ohne Spülungen im Inneren.",
    )
}

internal fun cyclePhaseName(day: Int): String = when (day) {
    in 1..5 -> tr("1. Phase menstruelle (Règles)", "1. Menstrual phase (Period)", pl = "1. Faza menstruacyjna (miesiączka)", uk = "1. Менструальна фаза (менструація)", es = "1. Fase menstrual (regla)", de = "1. Menstruationsphase (Periode)")
    in 6..13 -> tr("2. Phase folliculaire (Pré-ovulation)", "2. Follicular phase (Pre-ovulation)", pl = "2. Faza folikularna (przed owulacją)", uk = "2. Фолікулярна фаза (до овуляції)", es = "2. Fase folicular (preovulación)", de = "2. Follikelphase (vor dem Eisprung)")
    14 -> tr("3. Phase ovulatoire (Ovulation)", "3. Ovulatory phase (Ovulation)", pl = "3. Faza owulacyjna (owulacja)", uk = "3. Овуляторна фаза (овуляція)", es = "3. Fase ovulatoria (ovulación)", de = "3. Ovulationsphase (Eisprung)")
    else -> tr("4. Phase lutéale (Post-ovulation)", "4. Luteal phase (Post-ovulation)", pl = "4. Faza lutealna (po owulacji)", uk = "4. Лютеїнова фаза (після овуляції)", es = "4. Fase lútea (posovulación)", de = "4. Lutealphase (nach dem Eisprung)")
}

private fun cyclePhaseDescription(day: Int): String = when (day) {
    in 1..5 -> tr(
        "J1 à J5 : Le premier jour de saignement rouge vif marque le Jour 1 du cycle. En l'absence de nidation, les taux d'hormones chutent : la couche superficielle de la muqueuse utérine se détache et s'évacue. Les contractions régulières du myomètre favorisent cette élimination. Ce renouvellement marque le départ d'un nouveau cycle physiologique.",
        "D1 to D5: The first day of bright red flow marks Day 1 of the cycle. When no implantation occurs, hormone levels drop: the superficial uterine lining sheds and flows outward. Regular contractions of the uterine muscle facilitate this shedding, initiating a fresh cycle of tissue renewal.",
        pl = "D1 do D5: pierwszy dzień jasnoczerwonego krwawienia to 1. dzień cyklu. Gdy nie dojdzie do zagnieżdżenia, poziom hormonów spada: powierzchowna warstwa błony śluzowej macicy odrywa się i zostaje usunięta. Regularne skurcze miometrium sprzyjają temu oczyszczaniu. To odnowienie zaczyna nowy cykl fizjologiczny.",
        uk = "Д1-Д5: перший день яскраво-червоної кровотечі є 1-м днем циклу. Якщо імплантації не було, рівень гормонів знижується: поверхневий шар слизової оболонки матки відшаровується й виводиться. Регулярні скорочення міометрія сприяють цьому виведенню. Таке оновлення дає початок новому фізіологічному циклу.",
        es = "D1 a D5: el primer día de sangrado rojo vivo marca el día 1 del ciclo. Si no hay anidación, los niveles hormonales caen: la capa superficial de la mucosa uterina se desprende y se elimina. Las contracciones regulares del miometrio favorecen esta eliminación. Esta renovación marca el inicio de un nuevo ciclo fisiológico.",
        de = "T1 bis T5: Der erste Tag mit hellroter Blutung ist Tag 1 des Zyklus. Bleibt eine Einnistung aus, sinken die Hormonspiegel: Die oberste Schicht der Gebärmutterschleimhaut löst sich ab und wird ausgeschieden. Regelmäßige Kontraktionen des Myometriums fördern diese Ausscheidung. Diese Erneuerung markiert den Beginn eines neuen physiologischen Zyklus.",
    )
    in 6..13 -> tr(
        "J6 à J13 : Un follicule grandit dans l'un des ovaires pour mener un ovocyte à maturité. Il sécrète des œstrogènes en quantité progressive, stimulant l'épaississement de la muqueuse utérine et accompagnant souvent une période d'énergie et de vitalité.",
        "D6 to D13: A follicle develops inside one ovary to bring an egg to maturity. It secretes increasing amounts of estrogen, rebuilding the uterine lining while often supporting energy and overall vitality.",
        pl = "D6 do D13: w jednym z jajników rośnie pęcherzyk, który doprowadza komórkę jajową do dojrzałości. Wydziela stopniowo coraz więcej estrogenów, pobudzając pogrubienie błony śluzowej macicy i często towarzysząc okresowi energii i witalności.",
        uk = "Д6-Д13: в одному з яєчників росте фолікул, щоб довести яйцеклітину до зрілості. Він виробляє дедалі більше естрогенів, стимулюючи потовщення слизової оболонки матки та нерідко супроводжуючи період енергії та життєвих сил.",
        es = "D6 a D13: un folículo crece en uno de los ovarios para llevar un óvulo a la madurez. Secreta estrógenos en cantidad progresiva, estimulando el engrosamiento de la mucosa uterina y acompañando a menudo un periodo de energía y vitalidad.",
        de = "T6 bis T13: In einem der Eierstöcke wächst ein Follikel heran, um eine Eizelle zur Reife zu bringen. Er bildet zunehmend Östrogen, regt den Aufbau der Gebärmutterschleimhaut an und begleitet oft eine Phase voller Energie und Vitalität.",
    )
    14 -> tr(
        "J14 : Sous l'effet d'un pic soudain d'hormone lutéinisante (LH), le follicule dominant libère son ovule mûr, capté par la trompe. L'ovule est fécondable pendant 12 à 24 heures. Pour optimiser le passage des spermatozoïdes, le col sécrète une glaire cervicale limpide, fluide et étirable.",
        "D14: Triggered by a rapid surge in luteinizing hormone (LH), the mature follicle releases its egg, captured by the fallopian tube. The egg remains viable for fertilization for 12 to 24 hours. The cervix produces clear, slippery, and stretchy mucus to facilitate sperm transit.",
        pl = "D14: pod wpływem nagłego szczytu hormonu luteinizującego (LH) dominujący pęcherzyk uwalnia dojrzałą komórkę jajową, wychwytywaną przez jajowód. Komórka jajowa może zostać zapłodniona przez 12 do 24 godzin. Aby ułatwić przejście plemników, szyjka macicy wytwarza przejrzysty, płynny i rozciągliwy śluz.",
        uk = "Д14: під впливом різкого піка лютеїнізуючого гормону (ЛГ) домінантний фолікул вивільняє зрілу яйцеклітину, яку вловлює маткова труба. Яйцеклітина здатна до запліднення впродовж 12-24 годин. Щоб полегшити проходження сперматозоїдів, шийка матки виробляє прозорий, рідкий і тягучий слиз.",
        es = "D14: bajo el efecto de un pico repentino de hormona luteinizante (LH), el folículo dominante libera su óvulo maduro, que recoge la trompa. El óvulo es fecundable durante 12 a 24 horas. Para facilitar el paso de los espermatozoides, el cuello del útero secreta un moco cervical límpido, fluido y elástico.",
        de = "T14: Durch einen plötzlichen Anstieg des luteinisierenden Hormons (LH) gibt der dominante Follikel seine reife Eizelle frei, die vom Eileiter aufgenommen wird. Die Eizelle ist 12 bis 24 Stunden befruchtungsfähig. Um den Spermien den Durchtritt zu erleichtern, bildet der Gebärmutterhals einen klaren, flüssigen und dehnbaren Zervixschleim.",
    )
    else -> tr(
        "J15 à J28 : Le follicule libéré s'est transformé en corps jaune et sécrète de la progestérone. Cette hormone enrichit la muqueuse utérine en glycogène et en vaisseaux sanguins, la rendant propice à une éventuelle nidation. En l'absence de fécondation, le corps jaune régresse, les hormones diminuent, ce qui peut influencer l'humeur ou l'énergie (syndrome prémenstruel), avant l'arrivée du cycle suivant.",
        "D15 to D28: The follicle transforms into the corpus luteum, secreting progesterone. This hormone enriches the uterine lining with nutrients and blood vessels for potential embryo implantation. Without fertilization, the corpus luteum regresses, hormone levels decline, sometimes prompting premenstrual symptoms (PMS), before the next cycle begins.",
        pl = "D15 do D28: uwolniony pęcherzyk przekształcił się w ciałko żółte i wydziela progesteron. Ten hormon wzbogaca błonę śluzową macicy w glikogen i naczynia krwionośne, czyniąc ją sprzyjającą ewentualnemu zagnieżdżeniu. Gdy nie dojdzie do zapłodnienia, ciałko żółte zanika, poziom hormonów spada, co może wpływać na nastrój lub energię (zespół napięcia przedmiesiączkowego), przed rozpoczęciem kolejnego cyklu.",
        uk = "Д15-Д28: фолікул, що вивільнив яйцеклітину, перетворився на жовте тіло й виробляє прогестерон. Цей гормон збагачує слизову оболонку матки глікогеном і кровоносними судинами, роблячи її сприятливою для можливої імплантації. Якщо запліднення не відбулося, жовте тіло регресує, рівень гормонів знижується, що може впливати на настрій чи енергію (передменструальний синдром), перед початком наступного циклу.",
        es = "D15 a D28: el folículo liberado se ha transformado en cuerpo lúteo y secreta progesterona. Esta hormona enriquece la mucosa uterina en glucógeno y en vasos sanguíneos, haciéndola propicia para una posible anidación. Si no hay fecundación, el cuerpo lúteo involuciona y las hormonas descienden, lo que puede influir en el ánimo o la energía (síndrome premenstrual), antes de que empiece el ciclo siguiente.",
        de = "T15 bis T28: Der freigesetzte Follikel hat sich in den Gelbkörper verwandelt und bildet Progesteron. Dieses Hormon reichert die Gebärmutterschleimhaut mit Glykogen und Blutgefäßen an und macht sie für eine mögliche Einnistung geeignet. Bleibt die Befruchtung aus, bildet sich der Gelbkörper zurück und die Hormone sinken, was Stimmung oder Energie beeinflussen kann (prämenstruelles Syndrom), bevor der nächste Zyklus beginnt.",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveUterusDiagram(
    initialPart: UterusPart = UterusPart.ENDOMETRIUM,
    initialDay: Int = 14,
) {
    var selectedPart by remember { mutableStateOf(initialPart) }
    var cycleDay by remember { mutableFloatStateOf(initialDay.toFloat()) }
    val sliderDay = cycleDay.roundToInt().coerceIn(1, 28)
    val colors = MaterialTheme.colorScheme
    val isDarkScheme = colors.background.luminance() < 0.5f

    SectionTitle(tr("Voyage interactif dans votre cycle", "Interactive journey inside your cycle", pl = "Interaktywna podróż przez Twój cykl", uk = "Інтерактивна подорож вашим циклом", es = "Viaje interactivo por tu ciclo", de = "Interaktive Reise durch deinen Zyklus"))
    LunaCard {
        // 1. Schéma : le jour affiché glisse vers celui du curseur, sans saut ; voir UterusIllustration.
        val shownDay by animateFloatAsState(
            targetValue = cycleDay,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "jour-schema",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            UterusIllustration(
                day = shownDay,
                selected = selectedPart,
                onSelect = { selectedPart = it },
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )
        }

        // 2. Curseur temporel du cycle (J1 à J28), sous le schéma : le doigt ne cache pas ce qui change
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        tr("Jour du cycle : J$sliderDay", "Cycle day: D$sliderDay", pl = "Dzień cyklu: D$sliderDay", uk = "День циклу: Д$sliderDay", es = "Día del ciclo: D$sliderDay", de = "Zyklustag: T$sliderDay"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (sliderDay) {
                            in 1..5 -> Color(0xFFEF5350).copy(alpha = 0.2f)
                            in 6..13 -> Color(0xFF42A5F5).copy(alpha = 0.2f)
                            14 -> Color(0xFFFFB300).copy(alpha = 0.25f)
                            else -> Color(0xFFAB47BC).copy(alpha = 0.2f)
                        },
                    ) {
                        Text(
                            cyclePhaseName(sliderDay),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkScheme) {
                                when (sliderDay) {
                                    in 1..5 -> Color(0xFFFF8A80)
                                    in 6..13 -> Color(0xFF90CAF9)
                                    14 -> Color(0xFFFFCC80)
                                    else -> Color(0xFFE1BEE7)
                                }
                            } else {
                                when (sliderDay) {
                                    in 1..5 -> Color(0xFFC62828)
                                    in 6..13 -> Color(0xFF1565C0)
                                    14 -> Color(0xFFE65100)
                                    else -> Color(0xFF6A1B9A)
                                }
                            },
                        )
                    }
                }

                Slider(
                    value = cycleDay,
                    onValueChange = { cycleDay = it },
                    valueRange = 1f..28f,
                    steps = 26,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Raccourcis de sélection rapide de phase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        Triple(1f, tr("1. Règles", "1. Period", pl = "1. Miesiączka", uk = "1. Менструація", es = "1. Regla", de = "1. Periode"), 1..5),
                        Triple(8f, tr("2. Folliculaire", "2. Follicular", pl = "2. Folikularna", uk = "2. Фолікулярна", es = "2. Folicular", de = "2. Follikulär"), 6..13),
                        Triple(14f, tr("3. Ovulation", "3. Ovulation", pl = "3. Owulacja", uk = "3. Овуляція", es = "3. Ovulación", de = "3. Eisprung"), 14..14),
                        Triple(21f, tr("4. Lutéale", "4. Luteal", pl = "4. Lutealna", uk = "4. Лютеїнова", es = "4. Lútea", de = "4. Luteal"), 15..28),
                    ).forEach { (targetDay, label, range) ->
                        val isCurrent = sliderDay in range
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) colors.primaryContainer else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) colors.primary else colors.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { cycleDay = targetDay },
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Synthèse physiologique de la phase active, sous le curseur
        AnimatedContent(
            targetState = sliderDay,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            contentKey = { cyclePhaseName(it) },
            label = "description-phase",
        ) { day ->
            Text(
                cyclePhaseDescription(day),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        // 3. Boutons sélecteurs (Chips)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            UterusPart.entries.forEach { part ->
                FilterChip(
                    selected = part == selectedPart,
                    onClick = { selectedPart = part },
                    label = { Text(uterusPartChipLabel(part), style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                    ),
                )
            }
        }

        // 4. Fiche détaillée de l'organe sélectionné
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.secondaryContainer.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (selectedPart) {
                                    UterusPart.ENDOMETRIUM -> Color(0xFFD32F2F)
                                    UterusPart.MYOMETRIUM -> Color(0xFFE57373)
                                    UterusPart.CERVIX -> Color(0xFFD81B60)
                                    UterusPart.TUBES -> Color(0xFFFF7043)
                                    UterusPart.OVARIES -> Color(0xFFFFB300)
                                    UterusPart.VAGINA -> Color(0xFFAB47BC)
                                }
                            )
                    )
                    Text(
                        uterusPartTitle(selectedPart),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                }
                Text(
                    uterusPartSummary(selectedPart),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
