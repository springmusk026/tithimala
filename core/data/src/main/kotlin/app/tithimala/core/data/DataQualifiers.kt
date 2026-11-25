package app.tithimala.core.data

import java.time.ZoneId
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CalendarUpdatesDir

internal data class CalendarUpdateEndpoint(val baseUrl: String)

internal fun interface EpochClock {
    fun nowMillis(): Long
}

internal fun interface ZoneIdProvider {
    fun currentZoneId(): ZoneId
}
