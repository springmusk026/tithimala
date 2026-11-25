package app.tithimala.core.calendarengine.data

import java.time.LocalDate
import java.time.format.DateTimeParseException

class CalendarDataValidationException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Validates bundled and downloaded calendar manifests, BS month-length tables, and patro year JSON payloads.
 */
object CalendarDataValidator {
    private const val EXPECTED_SCHEMA_VERSION = 1
    private const val MONTHS_PER_YEAR = 12
    private const val MIN_MONTH_DAYS = 28
    private const val MAX_MONTH_DAYS = 32
    private const val DATE_PARTS_COUNT = 3

    private val SHA256_REGEX = Regex("^[0-9a-f]{64}$")
    private val COMMIT_SHA_REGEX = Regex("^[0-9a-f]{7,40}$")
    private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun validateManifest(manifest: CalendarManifest) {
        checkRule(manifest.schema == EXPECTED_SCHEMA_VERSION) {
            "Unsupported manifest schema: ${manifest.schema}"
        }
        checkRule(manifest.datasetVersion.isNotBlank()) { "dataset_version must not be blank" }
        checkRule(manifest.upstreamRepo.isNotBlank()) { "upstream_repo must not be blank" }
        checkRule(COMMIT_SHA_REGEX.matches(manifest.upstreamCommit)) {
            "Invalid upstream_commit: ${manifest.upstreamCommit}"
        }
        checkRule(manifest.license.isNotBlank()) { "license must not be blank" }
        checkRule(manifest.patroYears.isNotEmpty()) { "patro_years must not be empty" }
        checkRule(manifest.patroYears == manifest.patroYears.distinct().sorted()) {
            "patro_years must be sorted and unique"
        }

        val requiredPaths = buildList {
            add(manifest.calendarFile)
            add(manifest.noticeFile)
            manifest.patroYears.forEach { year -> add("patro/$year.json") }
        }
        for (path in requiredPaths) {
            val digest = manifest.sha256[path]
                ?: throw CalendarDataValidationException("Missing SHA-256 entry for $path")
            checkRule(SHA256_REGEX.matches(digest)) { "Invalid SHA-256 hex for $path" }
        }
    }

    fun validateBsCalendar(table: BsCalendarTable) {
        checkRule(table.schema == EXPECTED_SCHEMA_VERSION) {
            "Unsupported bs-calendar schema: ${table.schema}"
        }
        validateSource(table.source)
        checkRule(table.minYear <= table.maxYear) {
            "min_year (${table.minYear}) must be <= max_year (${table.maxYear})"
        }
        validateEpoch(table)
        validateMonthLengths(table)
    }

    fun validatePatroYear(patro: PatroYearData, calendarTable: BsCalendarTable) {
        checkRule(patro.schema == EXPECTED_SCHEMA_VERSION) {
            "Unsupported patro schema for ${patro.year}: ${patro.schema}"
        }
        validateSource(patro.source)
        checkRule(patro.year in calendarTable.minYear..calendarTable.maxYear) {
            "Patro year ${patro.year} outside BS calendar range ${calendarTable.minYear}..${calendarTable.maxYear}"
        }
        val yearMonthLengths = calendarTable.monthLengths[patro.year.toString()]
            ?: throw CalendarDataValidationException("Missing month lengths for patro year ${patro.year}")

        for ((dateKey, record) in patro.days) {
            validatePatroDayKey(dateKey, patro.year, yearMonthLengths)
            validatePatroDayRecord(dateKey, record)
        }
    }

    private fun validateSource(source: CalendarSourceMetadata) {
        checkRule(source.repo.isNotBlank()) { "source.repo must not be blank" }
        checkRule(COMMIT_SHA_REGEX.matches(source.commit)) { "Invalid source.commit: ${source.commit}" }
        checkRule(source.license.isNotBlank()) { "source.license must not be blank" }
        checkRule(ISO_DATE_REGEX.matches(source.imported)) { "Invalid source.imported date: ${source.imported}" }
    }

    private fun validateEpoch(table: BsCalendarTable) {
        checkRule(ISO_DATE_REGEX.matches(table.epoch.bs)) { "Invalid BS epoch: ${table.epoch.bs}" }
        checkRule(ISO_DATE_REGEX.matches(table.epoch.ad)) { "Invalid AD epoch: ${table.epoch.ad}" }
        val expectedBsEpoch = "${table.minYear}-01-01"
        checkRule(table.epoch.bs == expectedBsEpoch) {
            "Expected BS epoch $expectedBsEpoch, got ${table.epoch.bs}"
        }
        try {
            LocalDate.parse(table.epoch.ad)
        } catch (e: DateTimeParseException) {
            throw CalendarDataValidationException("Unparseable AD epoch: ${table.epoch.ad}", e)
        }
    }

    private fun validateMonthLengths(table: BsCalendarTable) {
        val expectedCount = table.maxYear - table.minYear + 1
        checkRule(table.monthLengths.size == expectedCount) {
            "Expected $expectedCount years in month_lengths, got ${table.monthLengths.size}"
        }
        for (year in table.minYear..table.maxYear) {
            val lengths = table.monthLengths[year.toString()]
                ?: throw CalendarDataValidationException("Missing month_lengths entry for BS year $year")
            checkRule(lengths.size == MONTHS_PER_YEAR) {
                "Year $year must have $MONTHS_PER_YEAR months, got ${lengths.size}"
            }
            for ((index, days) in lengths.withIndex()) {
                checkRule(days in MIN_MONTH_DAYS..MAX_MONTH_DAYS) {
                    "Year $year month ${index + 1} has invalid length $days"
                }
            }
        }
    }

    private fun validatePatroDayKey(dateKey: String, expectedYear: Int, monthLengths: List<Int>) {
        checkRule(ISO_DATE_REGEX.matches(dateKey)) { "Invalid patro day key: $dateKey" }
        val parts = dateKey.split("-")
        checkRule(parts.size == DATE_PARTS_COUNT) { "Malformed patro day key: $dateKey" }
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        checkRule(year == expectedYear) { "Day key $dateKey does not match patro year $expectedYear" }
        checkRule(month in 1..MONTHS_PER_YEAR) { "Invalid month in $dateKey" }
        val maxDay = monthLengths[month - 1]
        checkRule(day in 1..maxDay) { "Day $day out of range 1..$maxDay for $dateKey" }
    }

    private fun validatePatroDayRecord(dateKey: String, record: PatroDayRecord) {
        record.tithi?.let { label ->
            checkRule(label.np.isNotBlank() && label.en.isNotBlank()) {
                "Blank tithi label at $dateKey"
            }
        }
        for (event in record.events) {
            checkRule(event.np.isNotBlank() && event.en.isNotBlank()) {
                "Blank event label at $dateKey"
            }
        }
    }

    private inline fun checkRule(condition: Boolean, lazyMessage: () -> String) {
        if (!condition) {
            throw CalendarDataValidationException(lazyMessage())
        }
    }
}
