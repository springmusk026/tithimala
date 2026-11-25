package app.tithimala.core.calendarengine

/**
 * Formats ASCII numerals into Nepali Devanagari digits.
 */
object DevanagariNumerals {
    private val digits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')

    fun format(value: Int): String = format(value.toString())

    fun format(input: String): String = buildString(input.length) {
        for (ch in input) {
            if (ch in '0'..'9') {
                append(digits[ch - '0'])
            } else {
                append(ch)
            }
        }
    }
}
