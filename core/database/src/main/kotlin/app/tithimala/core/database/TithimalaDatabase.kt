package app.tithimala.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local Room database for Tithimala offline persistence.
 */
@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TithimalaDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}
