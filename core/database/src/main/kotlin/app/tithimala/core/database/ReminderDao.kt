package app.tithimala.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for one-time and yearly Bikram Sambat reminders.
 */
@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        ORDER BY bsYear ASC, bsMonth ASC, bsDay ASC, hour ASC, minute ASC, id ASC
        """
    )
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE (
            recurrenceType = '${ReminderEntity.RECURRENCE_ONE_TIME}'
            AND bsYear = :bsYear
            AND bsMonth = :bsMonth
            AND bsDay = :bsDay
        ) OR (
            recurrenceType = '${ReminderEntity.RECURRENCE_YEARLY_BS}'
            AND bsYear <= :bsYear
            AND bsMonth = :bsMonth
            AND bsDay = :bsDay
        )
        ORDER BY hour ASC, minute ASC, id ASC
        """
    )
    fun observeForDate(bsYear: Int, bsMonth: Int, bsDay: Int): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE (
            recurrenceType = '${ReminderEntity.RECURRENCE_ONE_TIME}'
            AND bsYear = :bsYear
            AND bsMonth = :bsMonth
        ) OR (
            recurrenceType = '${ReminderEntity.RECURRENCE_YEARLY_BS}'
            AND bsYear <= :bsYear
            AND bsMonth = :bsMonth
        )
        ORDER BY bsDay ASC, hour ASC, minute ASC, id ASC
        """
    )
    fun observeForMonth(bsYear: Int, bsMonth: Int): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE isEnabled = 1
        ORDER BY bsYear ASC, bsMonth ASC, bsDay ASC, hour ASC, minute ASC, id ASC
        """
    )
    suspend fun getEnabledReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    /**
     * Inserts a new reminder or replaces an existing reminder with the same primary key,
     * returning its persisted row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReminderEntity): Long

    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
