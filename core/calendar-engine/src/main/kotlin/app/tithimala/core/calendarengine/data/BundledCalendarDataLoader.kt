package app.tithimala.core.calendarengine.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Loads and verifies calendar datasets from bundled JVM classpath resources or externally supplied byte payloads.
 */
object BundledCalendarDataLoader {
    const val BUNDLED_RESOURCE_ROOT = "tithimala/nepdate"
    const val MANIFEST_FILE = "manifest.json"

    private val strictJson = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
    }

    fun loadBundled(
        classLoader: ClassLoader = BundledCalendarDataLoader::class.java.classLoader
    ): VerifiedCalendarDataset {
        val manifestBytes = readResourceBytes(classLoader, "$BUNDLED_RESOURCE_ROOT/$MANIFEST_FILE")
        return loadAndVerify(manifestBytes) { relativePath ->
            readResourceBytes(classLoader, "$BUNDLED_RESOURCE_ROOT/$relativePath")
        }
    }

    fun loadAndVerify(
        manifestBytes: ByteArray,
        readRelativeFile: (relativePath: String) -> ByteArray
    ): VerifiedCalendarDataset {
        val manifest = decodeStrict<CalendarManifest>(manifestBytes, MANIFEST_FILE)
        CalendarDataValidator.validateManifest(manifest)

        val calendarBytes = readAndVerifyFile(manifest, manifest.calendarFile, readRelativeFile)
        val calendarTable = decodeStrict<BsCalendarTable>(calendarBytes, manifest.calendarFile)
        CalendarDataValidator.validateBsCalendar(calendarTable)

        val noticeBytes = readAndVerifyFile(manifest, manifest.noticeFile, readRelativeFile)
        val noticeText = noticeBytes.decodeToString()
        if (noticeText.isBlank()) {
            throw CalendarDataValidationException("NOTICE file must not be blank")
        }

        val patroByYear = buildMap {
            for (year in manifest.patroYears) {
                val path = "patro/$year.json"
                val bytes = readAndVerifyFile(manifest, path, readRelativeFile)
                val patroYear = decodeStrict<PatroYearData>(bytes, path)
                if (patroYear.year != year) {
                    throw CalendarDataValidationException(
                        "File $path declared year ${patroYear.year}, expected $year"
                    )
                }
                CalendarDataValidator.validatePatroYear(patroYear, calendarTable)
                put(year, patroYear)
            }
        }

        return VerifiedCalendarDataset(
            manifest = manifest,
            calendarTable = calendarTable,
            patroByYear = patroByYear,
            noticeText = noticeText
        )
    }

    private fun readAndVerifyFile(
        manifest: CalendarManifest,
        relativePath: String,
        readRelativeFile: (String) -> ByteArray
    ): ByteArray {
        val expectedSha = manifest.sha256[relativePath]
            ?: throw CalendarDataValidationException("Missing SHA-256 digest for $relativePath")
        val bytes = readRelativeFile(relativePath)
        if (!Sha256Verifier.verify(bytes, expectedSha)) {
            throw CalendarDataValidationException(
                "SHA-256 mismatch for $relativePath: expected $expectedSha, got ${Sha256Verifier.sha256Hex(bytes)}"
            )
        }
        return bytes
    }

    private inline fun <reified T> decodeStrict(bytes: ByteArray, fileName: String): T = try {
        strictJson.decodeFromString<T>(bytes.decodeToString())
    } catch (e: SerializationException) {
        throw CalendarDataValidationException("Failed to decode $fileName: ${e.message}", e)
    }

    private fun readResourceBytes(classLoader: ClassLoader, path: String): ByteArray =
        classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: throw CalendarDataValidationException("Missing bundled calendar resource: $path")
}
