package app.tithimala.core.model

import kotlinx.serialization.Serializable

/**
 * Provenance and sync status metadata for the active calendar dataset.
 */
@Serializable
data class DatasetMetadata(
    val version: String,
    val upstreamRepo: String,
    val upstreamCommit: String,
    val license: String,
    val patroYears: List<Int>,
    val isRemoteUpdateActive: Boolean = false,
    val lastSyncEpochMillis: Long? = null
)
