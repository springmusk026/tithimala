package app.tithimala.core.data

import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.calendarengine.data.BundledCalendarDataLoader
import app.tithimala.core.calendarengine.data.CalendarDataValidationException
import app.tithimala.core.calendarengine.data.CalendarManifest
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class CalendarDatasetStorage @Inject constructor(@CalendarUpdatesDir private val updatesDir: File) {
    private val bundledManifestVersion: String by lazy { loadBundledManifestVersion() }

    fun loadCachedEngineOrNull(): BikramSambatEngine? {
        val manifestFile = File(updatesDir, BundledCalendarDataLoader.MANIFEST_FILE)
        if (!manifestFile.isFile) {
            return null
        }

        return try {
            val manifestBytes = manifestFile.readBytes()
            val dataset = BundledCalendarDataLoader.loadAndVerify(manifestBytes) { relativePath ->
                val targetFile = resolveSafeChildFile(updatesDir, relativePath)
                if (!targetFile.isFile) {
                    throw IOException("Missing cached calendar file: $relativePath")
                }
                targetFile.readBytes()
            }
            if (CalendarDatasetVersionComparator.compare(dataset.manifest.datasetVersion, bundledManifestVersion) > 0) {
                BikramSambatEngine(
                    dataset = dataset,
                    isRemoteUpdateActive = true,
                    lastSyncEpochMillis = manifestFile.lastModified().takeIf { it > 0L }
                )
            } else {
                null
            }
        } catch (_: CalendarDataValidationException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun commitVerifiedDataset(
        manifestBytes: ByteArray,
        relativeFiles: Map<String, ByteArray>,
        syncedAtEpochMillis: Long
    ) {
        val parentDir = updatesDir.parentFile
            ?: throw IOException("Updates directory has no parent: ${updatesDir.path}")
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw IOException("Unable to create parent directory: ${parentDir.path}")
        }

        val token = System.nanoTime()
        val stagingDir = File(parentDir, "${updatesDir.name}.staging.$token")
        val backupDir = File(parentDir, "${updatesDir.name}.backup.$token")

        try {
            writeStagedFiles(stagingDir, manifestBytes, relativeFiles, syncedAtEpochMillis)
            swapDirectoryAtomically(stagingDir, backupDir)
        } finally {
            if (stagingDir.exists()) {
                stagingDir.deleteRecursively()
            }
        }
    }

    private fun writeStagedFiles(
        stagingDir: File,
        manifestBytes: ByteArray,
        relativeFiles: Map<String, ByteArray>,
        syncedAtEpochMillis: Long
    ) {
        if (!stagingDir.mkdirs()) {
            throw IOException("Unable to create staging directory: ${stagingDir.path}")
        }
        val manifestFile = resolveSafeChildFile(stagingDir, BundledCalendarDataLoader.MANIFEST_FILE)
        manifestFile.writeBytes(manifestBytes)

        for ((relativePath, bytes) in relativeFiles) {
            val targetFile = resolveSafeChildFile(stagingDir, relativePath)
            val targetParent = targetFile.parentFile
            if (targetParent != null && !targetParent.exists() && !targetParent.mkdirs()) {
                throw IOException("Unable to create directory for $relativePath")
            }
            targetFile.writeBytes(bytes)
        }
        manifestFile.setLastModified(syncedAtEpochMillis)
    }

    private fun swapDirectoryAtomically(stagingDir: File, backupDir: File) {
        val hadExistingUpdates = updatesDir.exists()
        if (hadExistingUpdates) {
            moveDirectory(updatesDir, backupDir)
        }

        try {
            moveDirectory(stagingDir, updatesDir)
        } catch (moveFailure: IOException) {
            if (hadExistingUpdates && backupDir.exists() && !updatesDir.exists()) {
                try {
                    moveDirectory(backupDir, updatesDir)
                } catch (rollbackFailure: IOException) {
                    moveFailure.addSuppressed(rollbackFailure)
                }
            }
            throw moveFailure
        }

        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    }

    private fun moveDirectory(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath())
        }
    }

    private fun loadBundledManifestVersion(): String {
        val path = "${BundledCalendarDataLoader.BUNDLED_RESOURCE_ROOT}/${BundledCalendarDataLoader.MANIFEST_FILE}"
        val bytes = BundledCalendarDataLoader::class.java.classLoader
            ?.getResourceAsStream(path)
            ?.use { it.readBytes() }
        return if (bytes == null) {
            ""
        } else {
            try {
                STRICT_JSON.decodeFromString<CalendarManifest>(bytes.decodeToString()).datasetVersion
            } catch (_: SerializationException) {
                ""
            }
        }
    }

    companion object {
        private val STRICT_JSON = Json {
            ignoreUnknownKeys = false
            isLenient = false
            explicitNulls = false
        }

        fun validateSafeRelativePath(relativePath: String) {
            require(relativePath.isNotBlank()) { "Relative path must not be blank" }
            require(!relativePath.startsWith("/") && !relativePath.contains("\\")) {
                "Absolute or backslash path is not allowed: $relativePath"
            }
            require(!relativePath.contains("?") && !relativePath.contains("#")) {
                "Query or fragment is not allowed in path: $relativePath"
            }
            val segments = relativePath.split("/")
            require(segments.none { it.isEmpty() || it == "." || it == ".." }) {
                "Invalid path segment in relative path: $relativePath"
            }
        }

        internal fun resolveSafeChildFile(rootDir: File, relativePath: String): File {
            validateSafeRelativePath(relativePath)
            val rootCanonical = rootDir.canonicalFile
            val childCanonical = File(rootCanonical, relativePath).canonicalFile
            val expectedPrefix = rootCanonical.path + File.separator
            require(childCanonical.path.startsWith(expectedPrefix)) {
                "Path escapes target directory: $relativePath"
            }
            return childCanonical
        }
    }
}

internal object CalendarDatasetVersionComparator {
    fun compare(candidate: String, baseline: String): Int {
        if (candidate == baseline) return 0
        val candidateParts = candidate.split(".")
        val baselineParts = baseline.split(".")
        val firstDiff = candidateParts.zip(baselineParts)
            .map { (left, right) -> compareSegment(left, right) }
            .firstOrNull { it != 0 }
        return firstDiff ?: candidateParts.size.compareTo(baselineParts.size)
    }

    private fun compareSegment(left: String, right: String): Int {
        val leftInt = left.toIntOrNull()
        val rightInt = right.toIntOrNull()
        return if (leftInt != null && rightInt != null) {
            leftInt.compareTo(rightInt)
        } else {
            left.compareTo(right)
        }
    }
}
