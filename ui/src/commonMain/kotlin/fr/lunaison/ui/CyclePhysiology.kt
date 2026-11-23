package fr.lunaison.ui

import kotlin.math.exp

/**
 * Modèle continu d'un cycle type de 28 jours, pour animer le schéma de l'utérus jour après jour sans saut.
 * Toutes les valeurs sont relatives (0 à 1) : c'est une illustration pédagogique, pas une mesure.
 *
 * Formes reprises des descriptions classiques du cycle (Endotext, « The Normal Menstrual Cycle and the Control
 * of Ovulation ») : œstradiol qui monte jusqu'à la veille de l'ovulation, petite remontée en phase lutéale,
 * pic bref de LH vers J14, progestérone produite par le corps jaune avec un maximum vers J21. L'endomètre
 * s'amincit pendant les règles (2 à 4 mm), s'épaissit en phase folliculaire et reste épais en phase lutéale
 * (de l'ordre de 7 à 16 mm), avant de se détacher si aucune grossesse ne commence.
 *
 * Le jour est un nombre décimal entre 1 et 28 : le schéma passe d'un jour à l'autre en glissant.
 */
internal object CyclePhysiology {

    const val FIRST_DAY = 1f
    const val LAST_DAY = 28f
    const val OVULATION_DAY = 14f

    private fun gauss(day: Float, center: Float, width: Float): Float {
        val d = (day - center) / width
        return exp(-0.5f * d * d)
    }

    /** Montée douce de 0 à 1 entre [from] et [to]. */
    fun smooth(from: Float, to: Float, day: Float): Float {
        val t = ((day - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun Float.unit() = coerceIn(0f, 1f)

    /** Œstradiol : bas pendant les règles, pic vers J13, petite remontée vers J21. */
    fun estradiol(day: Float): Float =
        (0.12f + 0.88f * gauss(day, 13f, 2.6f) + 0.4f * gauss(day, 21.5f, 3.2f)).unit()

    /** Progestérone : presque nulle avant l'ovulation, maximum vers J21 (corps jaune), chute avant les règles. */
    fun progesterone(day: Float): Float =
        (0.05f + 0.95f * gauss(day, 21.5f, 3.3f) * smooth(13.5f, 16f, day)).unit()

    /** Hormone lutéinisante : pic bref qui déclenche l'ovulation. */
    fun lh(day: Float): Float = (0.08f + 0.92f * gauss(day, OVULATION_DAY, 0.75f)).unit()

    /** Épaisseur relative de l'endomètre. */
    fun endometrium(day: Float): Float = when {
        day <= 5f -> 0.75f + (0.18f - 0.75f) * smooth(1f, 5f, day)
        day <= OVULATION_DAY -> 0.18f + 0.62f * smooth(5f, OVULATION_DAY, day)
        else -> 0.8f + 0.2f * smooth(OVULATION_DAY, 20f, day) - 0.12f * smooth(24f, LAST_DAY, day)
    }.unit()

    /** Part de la muqueuse qui se détache : règles, J1 à J5. */
    fun shedding(day: Float): Float = 1f - smooth(3f, 5.5f, day)

    /** Aspect sécrétoire (glandes sinueuses, artères spiralées), sous l'effet de la progestérone. */
    fun secretory(day: Float): Float = smooth(OVULATION_DAY, 19f, day) * (1f - 0.3f * smooth(25f, LAST_DAY, day))

    /** Taille relative du follicule dominant, jusqu'à sa rupture. */
    fun dominantFollicle(day: Float): Float =
        if (day < OVULATION_DAY + 0.3f) smooth(5f, 13.7f, day) else 0f

    /** Corps jaune : se forme après l'ovulation, régresse en fin de cycle. */
    fun corpusLuteum(day: Float): Float =
        if (day < OVULATION_DAY + 0.3f) 0f else smooth(OVULATION_DAY + 0.3f, 17f, day) * (1f - 0.75f * smooth(23f, LAST_DAY, day))

    /** Éclat bref de l'ovulation. */
    fun ovulationFlash(day: Float): Float = gauss(day, OVULATION_DAY, 0.55f)

    /** Avancée de l'ovule dans la trompe (0 au pavillon, 1 près de l'utérus), ou null hors de cette période. */
    fun eggProgress(day: Float): Float? =
        if (day in OVULATION_DAY..17.5f) ((day - OVULATION_DAY) / 3.5f).unit() else null

    /** Glaire fertile : claire et filante autour de l'ovulation. */
    fun fertileMucus(day: Float): Float = smooth(9f, 13f, day) * (1f - smooth(14.5f, 16.5f, day))

    /** Ouverture relative du canal du col (règles et période fertile). */
    fun cervixOpening(day: Float): Float = maxOf(0.8f * shedding(day), fertileMucus(day), 0.2f)
}
