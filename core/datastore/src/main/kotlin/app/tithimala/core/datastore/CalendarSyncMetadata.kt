package app.tithimala.core.datastore

/**
 * Persisted provenance for the most recent verified calendar dataset sync check.
 */
data class CalendarSyncMetadata(val activeDatasetVersion: String? = null, val lastSyncEpochMillis: Long? = null)
