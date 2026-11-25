package app.tithimala.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-created Bikram Sambat reminder.
 */
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["bsYear", "bsMonth", "bsDay"]),
        Index(value = ["recurrenceType", "bsMonth", "bsDay"]),
        Index(value = ["isEnabled"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String,
    val bsYear: Int,
    val bsMonth: Int,
    val bsDay: Int,
    val hour: Int,
    val minute: Int,
    val recurrenceType: String,
    val isEnabled: Boolean,
    val createdAtEpochMillis: Long
) {
    companion object {
        const val RECURRENCE_ONE_TIME = "ONE_TIME"
        const val RECURRENCE_YEARLY_BS = "YEARLY_BS"
    }
}
