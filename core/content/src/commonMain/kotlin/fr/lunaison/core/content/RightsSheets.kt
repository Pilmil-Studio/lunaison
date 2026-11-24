package fr.lunaison.core.content

import fr.lunaison.core.model.tr
import kotlinx.datetime.LocalDate

/**
 * Fiche de droits : ce qui est remboursé, gratuit ou accessible dans un pays donné. Contrairement aux fiches
 * « Comprendre », elle ne vaut que pour un pays et elle se périme : chaque fiche porte la date de sa dernière
 * vérification sur des sources officielles, et un test fait échouer la CI au-delà de [RightsSheets.REVIEW_MAX_DAYS].
 */
data class RightsSheet(
    /** Code ISO du pays (« FR »). */
    val country: String,
    val article: Article,
    val verifiedOn: LocalDate,
)

object RightsSheets {
    /** Une fiche de droits non revérifiée depuis plus d'un an fait échouer la CI. */
    const val REVIEW_MAX_DAYS = 365

    /** Préfixe des identifiants, pour distinguer ces fiches des autres dans la navigation. */
    const val ID_PREFIX = "droits-"

    /** Toutes les fiches, dans la langue de l'appli (lues à chaque appel). */
    val all: List<RightsSheet> get() = France.all

    fun forCountry(code: String?): List<RightsSheet> = if (code == null) emptyList() else all.filter { it.country == code }

    fun byId(id: String): RightsSheet? = all.firstOrNull { it.article.id == id }

    private val SP_PROTECTIONS = Source(
        "Service-Public.fr, « Les protections hygiéniques sont-elles remboursées par la Sécurité sociale ? »",
        "https://www.service-public.gouv.fr/particuliers/vosdroits/F37700",
    )
    private val SP_PROTECTIONS_NEWS = Source(
        "Service-Public.fr, « Les protections hygiéniques réutilisables remboursées à partir du 1er octobre »",
        "https://www.service-public.gouv.fr/particuliers/actualites/A18883",
    )
    private val SP_EMERGENCY = Source(
        "Service-Public.fr, « Contraception d'urgence »",
        "https://www.service-public.gouv.fr/particuliers/vosdroits/F1100",
    )
    private val ANSM_EMERGENCY = Source(
        "ANSM, « Pilule du lendemain »",
        "https://ansm.sante.fr/dossiers-thematiques/contraception-durgence/pilule-du-lendemain",
    )

    private object France {
        val all: List<RightsSheet> get() = listOf(protections(), emergencyContraception())

        fun protections() = RightsSheet(
            country = "FR",
            verifiedOn = LocalDate(2026, 11, 24),
            article = Article(
                id = "${ID_PREFIX}protections",
                title = tr(
                    "Coupes et culottes menstruelles remboursées",
                    "Reimbursed menstrual cups and period pants",
                    pl = "Refundowane kubeczki i majtki menstruacyjne",
                    uk = "Відшкодування менструальних чаш і трусиків",
                    es = "Copas y bragas menstruales reembolsadas",
                    de = "Erstattung von Menstruationstassen und Periodenunterwäsche",
                ),
                summary = tr(
                    "Depuis le 1er octobre 2026, l'Assurance maladie rembourse certaines protections réutilisables.",
                    "Since 1 October 2026, French national health insurance reimburses some reusable period products.",
                    pl = "Od 1 października 2026 r. francuskie ubezpieczenie zdrowotne zwraca koszt niektórych środków wielokrotnego użytku.",
                    uk = "З 1 жовтня 2026 року французьке медичне страхування відшкодовує деякі багаторазові засоби.",
                    es = "Desde el 1 de octubre de 2026, el seguro médico francés reembolsa algunas protecciones reutilizables.",
                    de = "Seit dem 1. Oktober 2026 erstattet die französische Krankenversicherung einige wiederverwendbare Periodenprodukte.",
                ),
                sections = listOf(
                    Section(
                        tr("Qui est concerné", "Who is eligible", pl = "Kogo to dotyczy", uk = "Кого це стосується", es = "Quién tiene derecho", de = "Wer Anspruch hat"),
                        listOf(
                            tr(
                                "Toutes les personnes assurées de moins de 26 ans, sans condition de ressources.",
                                "Everyone insured under 26, whatever their income.",
                                pl = "Wszystkie ubezpieczone osoby poniżej 26. roku życia, niezależnie od dochodów.",
                                uk = "Усі застраховані особи віком до 26 років, незалежно від доходів.",
                                es = "Todas las personas aseguradas menores de 26 años, sin condición de ingresos.",
                                de = "Alle Versicherten unter 26 Jahren, unabhängig vom Einkommen.",
                            ),
                            tr(
                                "Les personnes qui ont la Complémentaire santé solidaire (C2S), quel que soit leur âge.",
                                "People with the Complémentaire santé solidaire (C2S), whatever their age.",
                                pl = "Osoby objęte Complémentaire santé solidaire (C2S), bez względu na wiek.",
                                uk = "Особи, які мають Complémentaire santé solidaire (C2S), незалежно від віку.",
                                es = "Las personas con la Complémentaire santé solidaire (C2S), sea cual sea su edad.",
                                de = "Personen mit der Complémentaire santé solidaire (C2S), unabhängig vom Alter.",
                            ),
                        ),
                    ),
                    Section(
                        tr("Ce qui est remboursé", "What is covered", pl = "Co jest refundowane", uk = "Що відшкодовується", es = "Qué se reembolsa", de = "Was erstattet wird"),
                        listOf(
                            tr(
                                "Les coupes et les culottes menstruelles réutilisables vendues en pharmacie, pour les marques figurant sur la liste officielle. Les tampons, serviettes et protège-slips jetables ne sont pas remboursés.",
                                "Reusable menstrual cups and period pants sold in pharmacies, for brands on the official list. Disposable tampons, pads and panty liners are not covered.",
                                pl = "Kubeczki i majtki menstruacyjne wielokrotnego użytku sprzedawane w aptekach, z oficjalnej listy marek. Jednorazowe tampony, podpaski i wkładki nie są refundowane.",
                                uk = "Багаторазові менструальні чаші та трусики з аптек, марки яких є в офіційному списку. Одноразові тампони, прокладки та щоденки не відшкодовуються.",
                                es = "Las copas y bragas menstruales reutilizables vendidas en farmacia, de las marcas de la lista oficial. Los tampones, compresas y salvaslips desechables no se reembolsan.",
                                de = "Wiederverwendbare Menstruationstassen und Periodenunterwäsche aus der Apotheke, sofern die Marke auf der offiziellen Liste steht. Einweg-Tampons, -Binden und -Slipeinlagen werden nicht erstattet.",
                            ),
                            tr(
                                "Deux produits par an, par exemple une coupe et une culotte, ou deux culottes. L'année commence à la date du premier achat remboursé.",
                                "Two products a year, for example one cup and one pair of pants, or two pairs of pants. The year starts on the date of the first reimbursed purchase.",
                                pl = "Dwa produkty rocznie, np. jeden kubeczek i jedne majtki albo dwie pary majtek. Rok liczy się od pierwszego refundowanego zakupu.",
                                uk = "Два вироби на рік, наприклад одна чаша й одні трусики або двоє трусиків. Рік рахується від дати першої відшкодованої покупки.",
                                es = "Dos productos al año, por ejemplo una copa y una braga, o dos bragas. El año empieza en la fecha de la primera compra reembolsada.",
                                de = "Zwei Produkte pro Jahr, zum Beispiel eine Tasse und eine Unterhose oder zwei Unterhosen. Das Jahr beginnt mit dem ersten erstatteten Kauf.",
                            ),
                            tr(
                                "Moins de 26 ans : 60 % remboursés par l'Assurance maladie, le reste par la mutuelle si vous en avez une. C2S : 100 %.",
                                "Under 26: 60% reimbursed by national health insurance, the rest by your top-up insurance if you have one. C2S: 100%.",
                                pl = "Poniżej 26 lat: 60% zwraca ubezpieczenie zdrowotne, resztę ubezpieczenie dodatkowe, jeśli je masz. C2S: 100%.",
                                uk = "До 26 років: 60% відшкодовує медичне страхування, решту додаткове страхування, якщо воно у вас є. C2S: 100%.",
                                es = "Menores de 26 años: el seguro médico reembolsa el 60 %, y el resto la mutua si tienes una. C2S: 100 %.",
                                de = "Unter 26: 60 % erstattet die Krankenversicherung, den Rest die Zusatzversicherung, falls vorhanden. C2S: 100 %.",
                            ),
                        ),
                    ),
                    Section(
                        tr("Comment faire", "How to get it", pl = "Jak to zrobić", uk = "Як це отримати", es = "Cómo hacerlo", de = "So geht's"),
                        listOf(
                            tr(
                                "Achetez la protection en pharmacie, sans ordonnance, avec votre carte Vitale. Demandez au pharmacien si le modèle fait partie de la liste prise en charge.",
                                "Buy the product in a pharmacy, without a prescription, with your carte Vitale. Ask the pharmacist whether the model is on the covered list.",
                                pl = "Kup produkt w aptece, bez recepty, z kartą Vitale. Zapytaj farmaceutę, czy dany model jest na liście refundowanych.",
                                uk = "Купіть виріб в аптеці, без рецепта, з карткою Vitale. Запитайте фармацевта, чи є ця модель у списку.",
                                es = "Compra la protección en farmacia, sin receta, con tu tarjeta Vitale. Pregunta en la farmacia si el modelo está en la lista cubierta.",
                                de = "Kauf das Produkt in der Apotheke, ohne Rezept, mit deiner Carte Vitale. Frag in der Apotheke, ob das Modell auf der Liste steht.",
                            ),
                        ),
                    ),
                ),
                sources = listOf(SP_PROTECTIONS, SP_PROTECTIONS_NEWS),
            ),
        )

        fun emergencyContraception() = RightsSheet(
            country = "FR",
            verifiedOn = LocalDate(2026, 11, 24),
            article = Article(
                id = "${ID_PREFIX}contraception-urgence",
                title = tr(
                    "Contraception d'urgence : gratuite et sans ordonnance",
                    "Emergency contraception: free, no prescription",
                    pl = "Antykoncepcja awaryjna: bezpłatna i bez recepty",
                    uk = "Екстрена контрацепція: безкоштовно й без рецепта",
                    es = "Anticoncepción de emergencia: gratuita y sin receta",
                    de = "Notfallverhütung: kostenlos und ohne Rezept",
                ),
                summary = tr(
                    "Après un rapport non ou mal protégé, il y a des solutions. Plus tôt elles sont prises, mieux elles marchent.",
                    "After unprotected or poorly protected sex, there are options. The sooner, the better they work.",
                    pl = "Po stosunku bez zabezpieczenia lub ze słabym zabezpieczeniem są rozwiązania. Im wcześniej, tym lepiej działają.",
                    uk = "Після незахищеного або погано захищеного статевого акту є рішення. Що раніше, то краще вони діють.",
                    es = "Tras una relación sin protección o mal protegida, hay soluciones. Cuanto antes, mejor funcionan.",
                    de = "Nach ungeschütztem oder schlecht geschütztem Sex gibt es Möglichkeiten. Je früher, desto besser wirken sie.",
                ),
                sections = listOf(
                    Section(
                        tr("Les solutions", "The options", pl = "Możliwości", uk = "Варіанти", es = "Las opciones", de = "Die Möglichkeiten"),
                        listOf(
                            tr(
                                "La pilule d'urgence : un seul comprimé, à prendre le plus tôt possible. Selon le médicament, jusqu'à 3 jours (lévonorgestrel) ou 5 jours (ulipristal) après le rapport. Son efficacité n'est pas totale.",
                                "The emergency pill: a single tablet, to take as soon as possible. Depending on the medicine, up to 3 days (levonorgestrel) or 5 days (ulipristal) after sex. It is not 100% effective.",
                                pl = "Tabletka awaryjna: jedna tabletka, jak najszybciej. W zależności od leku do 3 dni (lewonorgestrel) lub 5 dni (ulipristal) po stosunku. Nie jest w 100% skuteczna.",
                                uk = "Екстрена таблетка: одна таблетка якомога раніше. Залежно від препарату до 3 днів (левоноргестрел) або 5 днів (уліпристал) після акту. Вона не діє на 100%.",
                                es = "La píldora de emergencia: un solo comprimido, lo antes posible. Según el medicamento, hasta 3 días (levonorgestrel) o 5 días (ulipristal) después de la relación. No es eficaz al 100 %.",
                                de = "Die Notfallpille: eine einzige Tablette, so früh wie möglich. Je nach Wirkstoff bis 3 Tage (Levonorgestrel) oder 5 Tage (Ulipristal) danach. Sie wirkt nicht zu 100 %.",
                            ),
                            tr(
                                "Le stérilet au cuivre : la méthode la plus efficace, posé par un médecin ou une sage-femme dans les 5 jours. Dites que c'est une urgence en prenant rendez-vous.",
                                "The copper IUD: the most effective method, fitted by a doctor or midwife within 5 days. Say it is an emergency when booking.",
                                pl = "Wkładka miedziana: najskuteczniejsza metoda, zakładana przez lekarza lub położną w ciągu 5 dni. Przy umawianiu wizyty powiedz, że to pilne.",
                                uk = "Мідна спіраль: найефективніший метод, її встановлює лікар або акушерка протягом 5 днів. Під час запису скажіть, що це терміново.",
                                es = "El DIU de cobre: el método más eficaz, colocado por un médico o una matrona en un plazo de 5 días. Di que es urgente al pedir cita.",
                                de = "Die Kupferspirale: die wirksamste Methode, eingesetzt von Ärztin, Arzt oder Hebamme innerhalb von 5 Tagen. Sag bei der Terminvergabe, dass es dringend ist.",
                            ),
                        ),
                    ),
                    Section(
                        tr("Où l'obtenir, gratuitement", "Where to get it, for free", pl = "Gdzie ją dostać bezpłatnie", uk = "Де отримати безкоштовно", es = "Dónde conseguirla gratis", de = "Wo es sie kostenlos gibt"),
                        listOf(
                            tr(
                                "En pharmacie, sans ordonnance. Avant 18 ans, c'est gratuit et anonyme : vous n'avez rien à présenter. À partir de 18 ans, c'est pris en charge à 100 % avec la carte Vitale, la carte AME ou une attestation de droits.",
                                "In a pharmacy, without a prescription. Under 18, it is free and anonymous: you don't need to show anything. From 18, it is fully covered with a carte Vitale, AME card or proof of insurance.",
                                pl = "W aptece, bez recepty. Poniżej 18 lat bezpłatnie i anonimowo: nie trzeba niczego okazywać. Od 18 lat w 100% refundowana z kartą Vitale, kartą AME lub zaświadczeniem o ubezpieczeniu.",
                                uk = "В аптеці, без рецепта. До 18 років безкоштовно й анонімно: нічого показувати не треба. З 18 років покривається на 100% з карткою Vitale, карткою AME або довідкою про страхування.",
                                es = "En farmacia, sin receta. Antes de los 18 años es gratuita y anónima: no hace falta enseñar nada. Desde los 18, está cubierta al 100 % con la tarjeta Vitale, la tarjeta AME o un certificado de derechos.",
                                de = "In der Apotheke, ohne Rezept. Unter 18 kostenlos und anonym: Du musst nichts vorzeigen. Ab 18 voll übernommen mit Carte Vitale, AME-Karte oder Versicherungsnachweis.",
                            ),
                            tr(
                                "Aussi auprès de l'infirmerie scolaire ou universitaire, dans les centres de santé sexuelle (dont le Planning familial) et dans les CeGIDD, y compris sans couverture sociale.",
                                "Also from the school or university nurse, sexual health centres (including Planning familial) and CeGIDD clinics, even without health insurance.",
                                pl = "Także u pielęgniarki szkolnej lub uczelnianej, w ośrodkach zdrowia seksualnego (w tym Planning familial) i w CeGIDD, również bez ubezpieczenia.",
                                uk = "Також у шкільному чи університетському медпункті, у центрах сексуального здоров'я (зокрема Planning familial) і в CeGIDD, навіть без страхування.",
                                es = "También en la enfermería escolar o universitaria, en los centros de salud sexual (como el Planning familial) y en los CeGIDD, incluso sin seguro médico.",
                                de = "Auch bei der Schul- oder Hochschulkrankenstation, in Zentren für sexuelle Gesundheit (etwa Planning familial) und in CeGIDD, auch ohne Krankenversicherung.",
                            ),
                        ),
                    ),
                    Section(
                        tr("Bon à savoir", "Good to know", pl = "Warto wiedzieć", uk = "Корисно знати", es = "Conviene saber", de = "Gut zu wissen"),
                        listOf(
                            tr(
                                "La contraception d'urgence ne protège pas des infections sexuellement transmissibles. En cas de doute, un dépistage est possible dans les mêmes centres.",
                                "Emergency contraception does not protect against sexually transmitted infections. If in doubt, testing is available at the same centres.",
                                pl = "Antykoncepcja awaryjna nie chroni przed chorobami przenoszonymi drogą płciową. W razie wątpliwości można zrobić badania w tych samych ośrodkach.",
                                uk = "Екстрена контрацепція не захищає від інфекцій, що передаються статевим шляхом. Якщо є сумніви, у тих самих центрах можна пройти тестування.",
                                es = "La anticoncepción de emergencia no protege de las infecciones de transmisión sexual. Si tienes dudas, puedes hacerte pruebas en los mismos centros.",
                                de = "Notfallverhütung schützt nicht vor sexuell übertragbaren Infektionen. Im Zweifel kannst du dich in denselben Zentren testen lassen.",
                            ),
                        ),
                    ),
                ),
                sources = listOf(SP_EMERGENCY, ANSM_EMERGENCY),
            ),
        )
    }
}
