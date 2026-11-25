package app.tithimala.core.calendarengine

import app.tithimala.core.model.BilingualText
import app.tithimala.core.model.MoonPhase
import java.util.Locale

/**
 * Pure visual helper mapping a tithi label to a crisp [MoonPhase] badge icon state.
 *
 * Upstream ` (K)` / `*` markers are ignored only inside this local classification lookup and are
 * never stripped from stored or displayed [BilingualText] values.
 */
internal object MoonPhaseClassifier {
    private val EARLY_TITHIS = setOf("pratipadha", "pratipada", "dwitiya", "tritiya")
    private val QUARTER_TITHIS = setOf("chaturthi", "panchami", "shashthi", "shashti", "saptami")
    private val LATE_TITHIS = setOf(
        "ashtami",
        "navami",
        "dashami",
        "ekadashi",
        "dwadashi",
        "trayodashi",
        "chaturdashi"
    )
    private val NEW_MOON_TITHIS = setOf("aunsi", "amavasya", "aushi")
    private val FULL_MOON_TITHIS = setOf("purnima", "poornima")

    private val NEPALI_ALIASES = mapOf(
        "औंसी" to "aunsi",
        "अमावस्या" to "aunsi",
        "पूर्णिमा" to "purnima",
        "प्रतिपदा" to "pratipadha",
        "द्वितीया" to "dwitiya",
        "तृतिया" to "tritiya",
        "तृतीया" to "tritiya",
        "चतुर्थी" to "chaturthi",
        "पञ्चमी" to "panchami",
        "पन्चमी" to "panchami",
        "षष्ठी" to "shashthi",
        "सप्तमी" to "saptami",
        "अष्टमी" to "ashtami",
        "नवमी" to "navami",
        "दशमी" to "dashami",
        "एकादशी" to "ekadashi",
        "द्वादशी" to "dwadashi",
        "त्रयोदशी" to "trayodashi",
        "चतुर्दशी" to "chaturdashi"
    )

    fun infer(tithi: BilingualText?): MoonPhase? {
        if (tithi == null) return null

        val normalizedEn = normalizeEnglish(tithi.en)
        val isExplicitKrishna = normalizedEn.startsWith("krishna ") || tithi.np.trim().startsWith("कृष्ण ")
        val coreToken = extractCoreToken(normalizedEn, tithi.np)

        return when {
            coreToken in FULL_MOON_TITHIS -> MoonPhase.FULL_MOON
            coreToken in NEW_MOON_TITHIS -> MoonPhase.NEW_MOON
            coreToken in EARLY_TITHIS ->
                if (isExplicitKrishna) MoonPhase.WANING_GIBBOUS else MoonPhase.WAXING_CRESCENT
            coreToken in QUARTER_TITHIS ->
                if (isExplicitKrishna) MoonPhase.LAST_QUARTER else MoonPhase.FIRST_QUARTER
            coreToken in LATE_TITHIS ->
                if (isExplicitKrishna) MoonPhase.WANING_CRESCENT else MoonPhase.WAXING_GIBBOUS
            else -> null
        }
    }

    private fun normalizeEnglish(raw: String): String = raw
        .trim()
        .removeSuffix("(K)")
        .trim()
        .lowercase(Locale.ROOT)

    private fun extractCoreToken(normalizedEn: String, rawNp: String): String {
        val enCandidate = normalizedEn
            .removePrefix("shukla ")
            .removePrefix("krishna ")
            .trim()
        if (enCandidate.isNotEmpty() && isKnownEnglishToken(enCandidate)) {
            return enCandidate
        }
        val npCandidate = rawNp
            .trim()
            .removeSuffix("*")
            .removePrefix("शुक्ल ")
            .removePrefix("कृष्ण ")
            .trim()
        return NEPALI_ALIASES[npCandidate] ?: enCandidate
    }

    private fun isKnownEnglishToken(token: String): Boolean = token in FULL_MOON_TITHIS ||
        token in NEW_MOON_TITHIS ||
        token in EARLY_TITHIS ||
        token in QUARTER_TITHIS ||
        token in LATE_TITHIS
}
