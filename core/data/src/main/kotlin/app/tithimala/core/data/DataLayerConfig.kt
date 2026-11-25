package app.tithimala.core.data

internal object DataLayerConfig {
    const val CALENDAR_SYNC_WORK_NAME = "tithimala_calendar_sync"
    const val REMINDER_RESCHEDULE_WORK_NAME = "tithimala_reminder_reschedule"
    const val CALENDAR_UPDATES_DIR_NAME = "calendar_updates"
    const val DEFAULT_CALENDAR_UPDATE_BASE_URL =
        "https://raw.githubusercontent.com/springmusk026/tithimala/main/" +
            "core/calendar-engine/src/main/resources/tithimala/nepdate"

    const val CALENDAR_SYNC_INTERVAL_DAYS = 7L
    const val HTTP_CONNECT_TIMEOUT_MS = 10_000
    const val HTTP_READ_TIMEOUT_MS = 10_000
    const val MAX_REMOTE_FILE_BYTES = 512 * 1024
}
