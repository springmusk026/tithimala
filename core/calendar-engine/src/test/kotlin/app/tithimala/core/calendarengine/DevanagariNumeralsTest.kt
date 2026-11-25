package app.tithimala.core.calendarengine

import app.tithimala.core.model.BsDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class DevanagariNumeralsTest {
    @Test
    fun formatAllDigits() {
        assertEquals("०१२३४५६७८९", DevanagariNumerals.format("0123456789"))
    }

    @Test
    fun formatBikramSambatYear() {
        val date = BsDate(year = 2083, month = 8, day = 10)
        assertEquals("२०८३", DevanagariNumerals.format(date.year))
        assertEquals("२०८३-०८-१०", DevanagariNumerals.format("2083-08-10"))
    }

    @Test
    fun bsDateOrdering() {
        val earlier = BsDate(year = 2083, month = 1, day = 1)
        val later = BsDate(year = 2083, month = 1, day = 2)
        assertTrue(earlier < later)
    }
}
