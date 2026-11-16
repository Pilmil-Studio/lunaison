package fr.lunaison.core.content

import fr.lunaison.core.cycle.Phase
import fr.lunaison.core.model.tr

/** Niveau relatif d'une hormone à un stade du cycle. */
enum class HormoneLevel(
    private val fr: String,
    private val en: String,
    private val pl: String,
    private val uk: String,
    private val es: String,
    private val de: String,
) {
    LOW("Bas", "Low", "Niski", "Низький", "Bajo", "Niedrig"),
    RISING("En hausse", "Rising", "Wzrastający", "Зростає", "En ascenso", "Steigend"),
    PEAK("Pic maximal", "Peak", "Szczyt", "Пік", "Pico máximo", "Spitze"),
    DOMINANT("Dominant", "Dominant", "Dominujący", "Домінує", "Dominante", "Dominant"),
    FALLING("En baisse", "Falling", "Opadający", "Спадає", "En descenso", "Fallend"),
    VARIABLE("Variable", "Variable", "Zmienny", "Змінний", "Variable", "Variabel"),
    HIGH_THEN_FALLING("Élevé puis en baisse", "High, then falling", "Wysoki, potem spada", "Високий, потім спадає", "Alto, luego en descenso", "Hoch, dann fallend"),
    ;

    val label: String get() = tr(fr, en, pl, uk, es, de)
}

/** Vue d'ensemble des 4 hormones clés du cycle ovarien. */
data class HormoneSnapshot(
    val estrogen: HormoneLevel,
    val progesterone: HormoneLevel,
    val lh: HormoneLevel,
    val fsh: HormoneLevel,
    val summary: String,
)

/** Profil physiologique, atouts et défis d'une phase du cycle. */
data class PhaseProfile(
    val phase: Phase,
    val title: String,
    val hormones: HormoneSnapshot,
    val strengths: List<String>,
    val challenges: List<String>,
    val supportAdvice: List<String>,
)

object PhaseHormoneInfo {

    fun profile(phase: Phase): PhaseProfile = when (phase) {
        Phase.MENSTRUAL -> PhaseProfile(
            phase = Phase.MENSTRUAL,
            title = tr("Règles", "Period", pl = "Miesiączka", uk = "Менструація", es = "Regla", de = "Periode"),
            hormones = HormoneSnapshot(
                estrogen = HormoneLevel.LOW,
                progesterone = HormoneLevel.LOW,
                lh = HormoneLevel.LOW,
                fsh = HormoneLevel.RISING,
                summary = tr(
                    fr = "La baisse de la progestérone en fin de cycle entraîne le détachement de la muqueuse utérine.",
                    en = "The drop in progesterone at the end of the cycle leads to the shedding of the uterine lining.",
                    pl = "Spadek progesteronu pod koniec cyklu prowadzi do złuszczania błony śluzowej macicy.",
                    uk = "Падіння рівня прогестерону наприкінці циклу призводить до відторгнення слизової оболонки матки.",
                    es = "La bajada de la progesterona al final del ciclo provoca el desprendimiento de la mucosa uterina.",
                    de = "Der Abfall des Progesterons am Ende des Zyklus führt zur Ablösung der Gebärmutterschleimhaut.",
                ),
            ),
            strengths = listOf(
                tr("Certaines personnes apprécient de ralentir pendant leurs règles ; d'autres ne ressentent aucun besoin de le faire.", "Some people enjoy slowing down during their period; others feel no need to.", pl = "Niektóre osoby lubią zwolnić podczas miesiączki, inne nie odczuwają takiej potrzeby.", uk = "Дехто любить сповільнитися під час менструації, а дехто не відчуває такої потреби.", es = "Algunas personas disfrutan de bajar el ritmo durante la regla; otras no sienten ninguna necesidad de hacerlo.", de = "Manche Menschen genießen es, während der Periode langsamer zu machen; andere verspüren dieses Bedürfnis überhaupt nicht."),
                tr("Le premier jour des règles marque le début d'un nouveau cycle : de nouveaux follicules commencent à être recrutés dans les ovaires.", "The first day of the period marks the start of a new cycle: new follicles begin to be recruited in the ovaries.", pl = "Pierwszy dzień miesiączki oznacza początek nowego cyklu: w jajnikach zaczynają być rekrutowane nowe pęcherzyki.", uk = "Перший день менструації позначає початок нового циклу: у яєчниках починають залучатися нові фолікули.", es = "El primer día de la regla marca el inicio de un nuevo ciclo: empiezan a reclutarse nuevos folículos en los ovarios.", de = "Der erste Tag der Periode markiert den Beginn eines neuen Zyklus: In den Eierstöcken werden neue Follikel rekrutiert."),
            ),
            challenges = listOf(
                tr("Les crampes viennent de contractions de l'utérus, que l'on pense liées aux prostaglandines.", "Cramps come from contractions of the uterus, which are thought to be linked to prostaglandins.", pl = "Bóle skurczowe wynikają z kurczenia się macicy, które według przypuszczeń jest związane z prostaglandynami.", uk = "Спазми виникають через скорочення матки, які, як вважають, пов'язані з простагландинами.", es = "Los calambres vienen de contracciones del útero, que se cree que están relacionadas con las prostaglandinas.", de = "Die Krämpfe entstehen durch Kontraktionen der Gebärmutter, die vermutlich mit Prostaglandinen zusammenhängen."),
                tr("La fatigue est fréquente chez certaines personnes, surtout en cas de règles douloureuses ou abondantes. Les saignements entraînent une perte de fer.", "Fatigue is common in some people, especially with painful or heavy periods. Bleeding causes a loss of iron.", pl = "U niektórych osób częste jest zmęczenie, zwłaszcza przy bolesnych lub obfitych miesiączkach. Krwawienie wiąże się z utratą żelaza.", uk = "У деяких людей часто буває втома, особливо при болісних або рясних менструаціях. Кровотеча призводить до втрати заліза.", es = "El cansancio es frecuente en algunas personas, sobre todo si la regla es dolorosa o abundante. El sangrado supone una pérdida de hierro.", de = "Müdigkeit ist bei manchen Menschen häufig, vor allem bei schmerzhaften oder starken Perioden. Die Blutung führt zu einem Eisenverlust."),
            ),
            supportAdvice = listOf(
                tr("Une bouillotte sur le bas du ventre peut soulager la douleur.", "A heating pad on the lower abdomen can relieve pain.", pl = "Termofor na podbrzuszu może łagodzić ból.", uk = "Грілка на низ живота може полегшити біль.", es = "Una bolsa de agua caliente en la parte baja del vientre puede aliviar el dolor.", de = "Eine Wärmflasche auf dem Unterbauch kann den Schmerz lindern."),
                tr("Privilégier les aliments riches en fer.", "Favor iron-rich foods.", pl = "Warto wybierać produkty bogate w żelazo.", uk = "Віддавати перевагу продуктам, багатим на залізо.", es = "Dar preferencia a los alimentos ricos en hierro.", de = "Eisenreiche Lebensmittel bevorzugen."),
                tr("Ne pas hésiter à alléger ses journées sans culpabilité.", "Lighten your schedule when possible without guilt.", pl = "Warto bez poczucia winy odciążyć swoje dni.", uk = "Не вагатися полегшувати свої дні без почуття провини.", es = "No dudar en aligerar los días sin sentir culpa.", de = "Ohne schlechtes Gewissen den Tag ruhig entlasten."),
            ),
        )

        Phase.FOLLICULAR -> PhaseProfile(
            phase = Phase.FOLLICULAR,
            title = tr("Phase folliculaire", "Follicular phase", pl = "Faza folikularna", uk = "Фолікулярна фаза", es = "Fase folicular", de = "Follikelphase"),
            hormones = HormoneSnapshot(
                estrogen = HormoneLevel.RISING,
                progesterone = HormoneLevel.LOW,
                lh = HormoneLevel.LOW,
                fsh = HormoneLevel.HIGH_THEN_FALLING,
                summary = tr(
                    fr = "La FSH stimule les follicules ovariens ; l'un d'eux mûrit et produit des œstrogènes en hausse.",
                    en = "FSH stimulates ovarian follicles; one matures and produces rising estrogen.",
                    pl = "FSH pobudza pęcherzyki jajnikowe; jeden z nich dojrzewa i wytwarza coraz więcej estrogenów.",
                    uk = "ФСГ стимулює фолікули яєчників; один із них дозріває й виробляє дедалі більше естрогенів.",
                    es = "La FSH estimula los folículos ováricos; uno de ellos madura y produce cada vez más estrógenos.",
                    de = "FSH stimuliert die Follikel der Eierstöcke; einer reift heran und bildet zunehmend Östrogen.",
                ),
            ),
            strengths = listOf(
                tr("Certaines personnes se sentent plus en forme dans cette phase, d'autres ne remarquent aucune différence. Les études ne montrent pas de différence nette de performance.", "Some people feel more energetic in this phase, others notice no difference. Studies do not show a clear difference in performance.", pl = "Niektóre osoby czują się w tej fazie lepiej, inne nie zauważają żadnej różnicy. Badania nie wykazują wyraźnej różnicy w wydolności.", uk = "Дехто почувається в цій фазі краще, дехто не помічає жодної різниці. Дослідження не показують чіткої різниці в працездатності.", es = "Algunas personas se sienten con más energía en esta fase; otras no notan ninguna diferencia. Los estudios no muestran una diferencia clara de rendimiento.", de = "Manche Menschen fühlen sich in dieser Phase fitter, andere bemerken keinen Unterschied. Studien zeigen keinen klaren Unterschied bei der Leistung."),
            ),
            challenges = listOf(
                tr("Les ressentis varient beaucoup d'une personne à l'autre : vos propres notes sont plus parlantes que des généralités.", "Experiences vary a lot from one person to another: your own notes say more than generalities.", pl = "Odczucia bardzo różnią się między osobami: własne notatki mówią więcej niż ogólniki.", uk = "Відчуття дуже різняться від людини до людини: ваші власні нотатки говорять більше, ніж загальні твердження.", es = "Las sensaciones varían mucho de una persona a otra: tus propias notas dicen más que las generalidades.", de = "Das Empfinden ist von Mensch zu Mensch sehr verschieden: Deine eigenen Notizen sagen mehr als Verallgemeinerungen."),
            ),
            supportAdvice = listOf(
                tr("Si cela vous intéresse, notez comment vous vous sentez à l'effort au fil de vos cycles : les études ne permettent pas de recommandation valable pour tout le monde.", "If you are interested, note how you feel during exercise over your cycles: studies do not allow a recommendation that holds for everyone.", pl = "Jeśli to Cię interesuje, zapisuj, jak czujesz się podczas wysiłku w kolejnych cyklach: badania nie pozwalają na zalecenie dobre dla wszystkich.", uk = "Якщо вам цікаво, занотовуйте, як ви почуваєтеся під час навантаження протягом циклів: дослідження не дозволяють дати рекомендацію, що підходить усім.", es = "Si te interesa, anota cómo te sientes al hacer esfuerzo a lo largo de tus ciclos: los estudios no permiten una recomendación válida para todo el mundo.", de = "Wenn es dich interessiert, notiere, wie du dich bei Belastung im Verlauf deiner Zyklen fühlst: Die Studien erlauben keine Empfehlung, die für alle gilt."),
            ),
        )

        Phase.OVULATION -> PhaseProfile(
            phase = Phase.OVULATION,
            title = tr("Autour de l'ovulation", "Around ovulation", pl = "Wokół owulacji", uk = "Навколо овуляції", es = "En torno a la ovulación", de = "Rund um den Eisprung"),
            hormones = HormoneSnapshot(
                estrogen = HormoneLevel.PEAK,
                progesterone = HormoneLevel.RISING,
                lh = HormoneLevel.PEAK,
                fsh = HormoneLevel.PEAK,
                summary = tr(
                    fr = "Le pic massif de LH provoque la rupture du follicule et libère l'ovocyte dans la trompe.",
                    en = "A surge in LH triggers follicle rupture, releasing the egg into the fallopian tube.",
                    pl = "Gwałtowny szczyt LH powoduje pęknięcie pęcherzyka i uwolnienie komórki jajowej do jajowodu.",
                    uk = "Різкий пік ЛГ спричиняє розрив фолікула й вивільняє яйцеклітину в маткову трубу.",
                    es = "El pico masivo de LH provoca la rotura del folículo y libera el óvulo en la trompa.",
                    de = "Der massive LH-Anstieg lässt den Follikel platzen und setzt die Eizelle in den Eileiter frei.",
                ),
            ),
            strengths = listOf(
                tr("En moyenne, le désir sexuel déclaré est un peu plus élevé autour de l'ovulation, mais beaucoup de personnes ne le constatent pas.", "On average, reported sexual desire is slightly higher around ovulation, but many people do not notice it.", pl = "Średnio deklarowane pożądanie seksualne jest nieco wyższe w okolicy owulacji, ale wiele osób tego nie zauważa.", uk = "У середньому заявлене сексуальне бажання трохи вище навколо овуляції, але багато людей цього не помічають.", es = "En promedio, el deseo sexual declarado es algo más alto en torno a la ovulación, pero muchas personas no lo notan.", de = "Im Durchschnitt ist das angegebene sexuelle Verlangen rund um den Eisprung etwas höher, aber viele Menschen bemerken das nicht."),
            ),
            challenges = listOf(
                tr("Une gêne d'un côté du bas-ventre à mi-cycle (douleur d'ovulation) touche jusqu'à 40 % des personnes qui ovulent, en général pendant quelques heures.", "Discomfort on one side of the lower abdomen at mid-cycle (ovulation pain) affects up to 40% of people who ovulate, usually for a few hours.", pl = "Dyskomfort po jednej stronie podbrzusza w połowie cyklu (ból owulacyjny) dotyczy do 40 % osób, które mają owulację, zwykle przez kilka godzin.", uk = "Дискомфорт з одного боку внизу живота в середині циклу (овуляторний біль) стосується до 40 % людей, у яких відбувається овуляція, зазвичай упродовж кількох годин.", es = "Una molestia en un lado del bajo vientre a mitad del ciclo (dolor de ovulación) afecta hasta al 40 % de las personas que ovulan, por lo general durante unas horas.", de = "Ein Unwohlsein auf einer Seite des Unterbauchs in der Zyklusmitte (Eisprungschmerz) betrifft bis zu 40 % der Menschen, die einen Eisprung haben, meist für einige Stunden."),
                tr("Certaines personnes remarquent des pertes plus fluides et filantes.", "Some people notice clearer, stretchier discharge.", pl = "Niektóre osoby zauważają bardziej przejrzysty, rozciągliwy śluz.", uk = "Дехто помічає прозоріші та тягучіші виділення.", es = "Algunas personas notan un flujo más fluido y elástico.", de = "Manche bemerken klareren, spinnbaren Zervixschleim."),
                tr("Si vous ne souhaitez pas de grossesse, une contraception fiable reste nécessaire tout au long du cycle : Lunaison ne peut pas vous dire quand vous êtes fertile.", "If you do not want a pregnancy, reliable contraception is still needed throughout the cycle: Lunaison cannot tell you when you are fertile.", pl = "Jeśli nie planujesz ciąży, niezawodna antykoncepcja jest potrzebna przez cały cykl: Lunaison nie jest w stanie wskazać, kiedy możliwe jest zajście w ciążę.", uk = "Якщо ви не плануєте вагітність, надійна контрацепція потрібна протягом усього циклу: Lunaison не може підказати, коли ви можете завагітніти.", es = "Si no deseas un embarazo, sigue siendo necesaria una anticoncepción fiable durante todo el ciclo: Lunaison no puede decirte cuándo eres fértil.", de = "Wenn du keine Schwangerschaft möchtest, ist während des ganzen Zyklus weiterhin eine zuverlässige Verhütung nötig: Lunaison kann dir nicht sagen, wann du fruchtbar bist."),
            ),
            supportAdvice = listOf(
                tr("Noter la glaire cervicale pour repérer précisément votre propre schéma corporel.", "Log cervical mucus observations to map your personal body patterns.", pl = "Warto zapisywać śluz szyjkowy, aby dokładnie rozpoznać własny wzorzec swojego ciała.", uk = "Записувати цервікальний слиз, щоб точно визначити власну закономірність свого тіла.", es = "Anotar el moco cervical para identificar con precisión tu propio patrón corporal.", de = "Den Zervixschleim eintragen, um dein eigenes körperliches Muster genau zu erkennen."),
                tr("Un cycle varie d'un mois à l'autre : une date estimée n'est jamais une garantie.", "Cycles vary from month to month: an estimated date is never a guarantee.", pl = "Cykl zmienia się z miesiąca na miesiąc: szacowana data nigdy nie jest gwarancją.", uk = "Цикл змінюється від місяця до місяця: орієнтовна дата ніколи не є гарантією.", es = "El ciclo varía de un mes a otro: una fecha estimada nunca es una garantía.", de = "Der Zyklus schwankt von Monat zu Monat: Ein geschätztes Datum ist nie eine Garantie."),
            ),
        )

        Phase.LUTEAL -> PhaseProfile(
            phase = Phase.LUTEAL,
            title = tr("Phase lutéale", "Luteal phase", pl = "Faza lutealna", uk = "Лютеїнова фаза", es = "Fase lútea", de = "Lutealphase"),
            hormones = HormoneSnapshot(
                estrogen = HormoneLevel.VARIABLE,
                progesterone = HormoneLevel.DOMINANT,
                lh = HormoneLevel.LOW,
                fsh = HormoneLevel.LOW,
                summary = tr(
                    fr = "Le corps jaune sécrète la progestérone qui épaissit la muqueuse utérine, avant de chuter.",
                    en = "The corpus luteum secretes progesterone to nourish the uterine lining, then drops.",
                    pl = "Ciałko żółte wydziela progesteron, który pogrubia błonę śluzową macicy, po czym jego poziom spada.",
                    uk = "Жовте тіло виділяє прогестерон, що потовщує слизову оболонку матки, перш ніж його рівень падає.",
                    es = "El cuerpo lúteo secreta progesterona, que engrosa la mucosa uterina, antes de que baje.",
                    de = "Der Gelbkörper bildet Progesteron, das die Gebärmutterschleimhaut verdickt, bevor es abfällt.",
                ),
            ),
            strengths = listOf(
                tr("Certaines études mesurent une dépense d'énergie au repos un peu plus élevée en phase lutéale. L'écart est petit et incertain.", "Some studies measure slightly higher resting energy expenditure in the luteal phase. The difference is small and uncertain.", pl = "Niektóre badania mierzą nieco wyższe zużycie energii w spoczynku w fazie lutealnej. Różnica jest niewielka i niepewna.", uk = "Деякі дослідження фіксують трохи вищі витрати енергії у спокої в лютеїновій фазі. Різниця невелика й невизначена.", es = "Algunos estudios miden un gasto de energía en reposo algo mayor en la fase lútea. La diferencia es pequeña e incierta.", de = "Einige Studien messen in der Lutealphase einen etwas höheren Energieverbrauch in Ruhe. Der Unterschied ist klein und unsicher."),
            ),
            challenges = listOf(
                tr("Une minorité de personnes ressent des changements d'humeur marqués avant les règles ; beaucoup n'en ressentent pas. Si cela vous pèse, parlez-en à un professionnel de santé.", "A minority of people experience marked mood changes before their period; many do not. If it weighs on you, talk to a health professional.", pl = "Mniejszość osób odczuwa wyraźne zmiany nastroju przed miesiączką, wiele osób ich nie odczuwa. Jeśli Cię to obciąża, porozmawiaj z pracownikiem ochrony zdrowia.", uk = "Меншість людей відчуває виразні зміни настрою перед менструацією, багато хто їх не відчуває. Якщо це вам дошкуляє, поговоріть із медичним працівником.", es = "Una minoría de personas nota cambios de humor marcados antes de la regla; muchas no los notan. Si te pesa, habla con un profesional de la salud.", de = "Eine Minderheit der Menschen erlebt vor der Periode deutliche Stimmungsschwankungen; viele nicht. Wenn es dich belastet, sprich mit einer medizinischen Fachperson."),
                tr("L'appétit est parfois plus marqué en phase lutéale : une méta-analyse trouve un apport énergétique un peu plus élevé, avec des preuves limitées.", "Appetite is sometimes stronger in the luteal phase: a meta-analysis finds slightly higher energy intake, with limited evidence.", pl = "Apetyt bywa większy w fazie lutealnej: metaanaliza wskazuje na nieco wyższe spożycie energii, przy ograniczonych dowodach.", uk = "Апетит іноді сильніший у лютеїновій фазі: метааналіз виявляє дещо вище споживання енергії, за обмежених доказів.", es = "El apetito a veces es mayor en la fase lútea: un metaanálisis encuentra una ingesta de energía algo mayor, con evidencia limitada.", de = "Der Appetit ist in der Lutealphase manchmal stärker: Eine Metaanalyse findet eine etwas höhere Energiezufuhr, bei begrenzter Evidenz."),
            ),
            supportAdvice = listOf(
                tr("Un apport suffisant en calcium peut atténuer les symptômes prémenstruels chez certaines personnes (preuves limitées).", "An adequate calcium intake may reduce premenstrual symptoms in some people (limited evidence).", pl = "Wystarczająca podaż wapnia może łagodzić objawy przedmiesiączkowe u niektórych osób (ograniczone dowody).", uk = "Достатнє надходження кальцію може пом'якшувати передменструальні симптоми в деяких людей (докази обмежені).", es = "Un aporte suficiente de calcio puede atenuar los síntomas premenstruales en algunas personas (evidencia limitada).", de = "Eine ausreichende Calciumzufuhr kann prämenstruelle Beschwerden bei manchen Menschen lindern (begrenzte Evidenz)."),
                tr("Une activité physique régulière et modérée peut aider (preuves limitées).", "Regular, moderate physical activity may help (limited evidence).", pl = "Regularna, umiarkowana aktywność fizyczna może pomagać (ograniczone dowody).", uk = "Регулярна помірна фізична активність може допомагати (докази обмежені).", es = "Una actividad física regular y moderada puede ayudar (evidencia limitada).", de = "Regelmäßige, moderate körperliche Aktivität kann helfen (begrenzte Evidenz)."),
            ),
        )

        Phase.LATE -> PhaseProfile(
            phase = Phase.LATE,
            title = tr("Retard de règles : écoute et bienveillance", "Late period: gentle observation", pl = "Opóźniająca się miesiączka: uważność i życzliwość", uk = "Затримка менструації: прислухання до себе й дбайливість", es = "Regla con retraso: escucha y amabilidad", de = "Verspätete Periode: Achtsamkeit und Wohlwollen"),
            hormones = HormoneSnapshot(
                estrogen = HormoneLevel.VARIABLE,
                progesterone = HormoneLevel.VARIABLE,
                lh = HormoneLevel.VARIABLE,
                fsh = HormoneLevel.VARIABLE,
                summary = tr(
                    fr = "Plusieurs causes sont possibles : grossesse, stress, changement de poids, sport intense, périménopause, contraception hormonale, allaitement ou variation naturelle du cycle.",
                    en = "Several causes are possible: pregnancy, stress, weight change, intense exercise, perimenopause, hormonal contraception, breastfeeding, or natural cycle variation.",
                    pl = "Możliwych przyczyn jest kilka: ciąża, stres, zmiana masy ciała, intensywny wysiłek fizyczny, perimenopauza, antykoncepcja hormonalna, karmienie piersią lub naturalna zmienność cyklu.",
                    uk = "Можливих причин кілька: вагітність, стрес, зміна ваги, інтенсивні заняття спортом, перименопауза, гормональна контрацепція, грудне вигодовування або природна мінливість циклу.",
                    es = "Hay varias causas posibles: embarazo, estrés, cambio de peso, deporte intenso, perimenopausia, anticoncepción hormonal, lactancia o variación natural del ciclo.",
                    de = "Es gibt mehrere mögliche Ursachen: Schwangerschaft, Stress, Gewichtsveränderung, intensiver Sport, Perimenopause, hormonelle Verhütung, Stillzeit oder natürliche Zyklusschwankungen.",
                ),
            ),
            strengths = listOf(
                tr("Stress, changement de poids ou sport intense peuvent retarder les règles.", "Stress, weight change, or intense exercise can delay a period.", pl = "Stres, zmiana masy ciała lub intensywny wysiłek mogą opóźnić miesiączkę.", uk = "Стрес, зміна ваги або інтенсивні заняття спортом можуть затримати менструацію.", es = "El estrés, un cambio de peso o el deporte intenso pueden retrasar la regla.", de = "Stress, Gewichtsveränderungen oder intensiver Sport können die Periode verzögern."),
            ),
            challenges = listOf(
                tr("Inquiétude ou incertitude liée à l'attente des règles.", "Uncertainty or concern while waiting for bleeding to start.", pl = "Niepokój lub niepewność związane z oczekiwaniem na miesiączkę.", uk = "Тривога чи невизначеність, пов'язані з очікуванням менструації.", es = "Inquietud o incertidumbre ligadas a la espera de la regla.", de = "Sorge oder Unsicherheit beim Warten auf die Periode."),
            ),
            supportAdvice = listOf(
                tr("Un test urinaire peut être fait dès le premier jour de retard. Un résultat positif est presque certain ; un résultat négatif est moins fiable, refaites le test après quelques jours.", "A urine test can be taken from the first day of a late period. A positive result is almost certain; a negative result is less reliable, so repeat the test after a few days.", pl = "Test moczowy można wykonać już od pierwszego dnia opóźnienia. Wynik dodatni jest niemal pewny; wynik ujemny jest mniej wiarygodny, powtórz test po kilku dniach.", uk = "Сечовий тест можна зробити вже з першого дня затримки. Позитивний результат майже напевно правильний; негативний менш надійний, повторіть тест через кілька днів.", es = "Se puede hacer una prueba de orina desde el primer día de retraso. Un resultado positivo es casi seguro; un resultado negativo es menos fiable, repite la prueba pasados unos días.", de = "Ein Urintest kann ab dem ersten Tag der Verspätung gemacht werden. Ein positives Ergebnis ist nahezu sicher; ein negatives ist weniger zuverlässig, wiederhole den Test nach einigen Tagen."),
                tr("Si une pause est souhaitée (grossesse ou contraception), le mode pause est disponible.", "If a pause is wanted (pregnancy or contraception), the pause mode is available.", pl = "Jeśli pożądana jest przerwa (ciąża lub antykoncepcja), dostępny jest tryb pauzy.", uk = "Якщо потрібна пауза (вагітність або контрацепція), доступний режим паузи.", es = "Si se desea una pausa (embarazo o anticoncepción), está disponible el modo pausa.", de = "Wenn eine Pause gewünscht ist (Schwangerschaft oder Verhütung), steht der Pausenmodus zur Verfügung."),
                tr("Sans règles depuis trois mois, demandez un avis médical.", "If you have had no period for three months, seek medical advice.", pl = "Jeśli nie masz miesiączki od trzech miesięcy, skonsultuj się z lekarzem.", uk = "Якщо менструації немає вже три місяці, зверніться до лікаря.", es = "Si llevas tres meses sin regla, pide consejo médico.", de = "Wenn deine Periode seit drei Monaten ausbleibt, hole ärztlichen Rat ein."),
            ),
        )
    }
}
