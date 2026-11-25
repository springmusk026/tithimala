package app.tithimala.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderDaoTest {

    private lateinit var database: TithimalaDatabase
    private lateinit var reminderDao: ReminderDao

    private val baseReminder = ReminderEntity(
        id = 0L,
        title = "Reminder",
        notes = "",
        bsYear = 2082,
        bsMonth = 1,
        bsDay = 1,
        hour = 9,
        minute = 0,
        recurrenceType = ReminderEntity.RECURRENCE_ONE_TIME,
        isEnabled = true,
        createdAtEpochMillis = 1_744_617_600_000L
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TithimalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderDao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertInsertsAndUpdatesExistingReminderPreservingId() = runTest {
        val insertedId = reminderDao.upsert(
            baseReminder.copy(
                title = "New Year greetings",
                notes = "Call family",
                hour = 7,
                minute = 30
            )
        )

        val stored = reminderDao.getById(insertedId)
        assertNotNull(stored)
        assertEquals("New Year greetings", stored.title)
        assertEquals(insertedId, stored.id)

        val updatedId = reminderDao.upsert(
            stored.copy(
                title = "Bisket Jatra morning",
                notes = "Leave early",
                hour = 6,
                minute = 45
            )
        )

        assertEquals(insertedId, updatedId)
        val reloaded = reminderDao.getById(insertedId)
        assertNotNull(reloaded)
        assertEquals("Bisket Jatra morning", reloaded.title)
        assertEquals("Leave early", reloaded.notes)
        assertEquals(6, reloaded.hour)
        assertEquals(45, reloaded.minute)
        assertEquals(1, reminderDao.observeAll().first().size)
    }

    @Test
    fun observeAllReturnsRemindersOrderedByBsDateAndTime() = runTest {
        reminderDao.upsert(
            baseReminder.copy(title = "Late afternoon", bsYear = 2082, bsMonth = 5, bsDay = 10, hour = 16, minute = 0)
        )
        reminderDao.upsert(
            baseReminder.copy(title = "Earlier year", bsYear = 2081, bsMonth = 12, bsDay = 30, hour = 21, minute = 15)
        )
        reminderDao.upsert(
            baseReminder.copy(title = "Same day morning", bsYear = 2082, bsMonth = 5, bsDay = 10, hour = 8, minute = 30)
        )
        reminderDao.upsert(
            baseReminder.copy(title = "Same day early", bsYear = 2082, bsMonth = 5, bsDay = 10, hour = 8, minute = 5)
        )

        val titles = reminderDao.observeAll().first().map(ReminderEntity::title)

        assertEquals(
            listOf("Earlier year", "Same day early", "Same day morning", "Late afternoon"),
            titles
        )
    }

    @Test
    fun observeForDateReturnsExactOneTimeAndYearlyBsForStartAndLaterYears() = runTest {
        reminderDao.upsert(
            baseReminder.copy(
                title = "One-time dentist",
                bsYear = 2082,
                bsMonth = 6,
                bsDay = 15,
                hour = 11,
                minute = 0,
                recurrenceType = ReminderEntity.RECURRENCE_ONE_TIME
            )
        )
        reminderDao.upsert(
            baseReminder.copy(
                title = "Ama birthday",
                bsYear = 2082,
                bsMonth = 6,
                bsDay = 15,
                hour = 7,
                minute = 0,
                recurrenceType = ReminderEntity.RECURRENCE_YEARLY_BS
            )
        )
        reminderDao.upsert(
            baseReminder.copy(
                title = "Future yearly start",
                bsYear = 2084,
                bsMonth = 6,
                bsDay = 15,
                hour = 9,
                minute = 0,
                recurrenceType = ReminderEntity.RECURRENCE_YEARLY_BS
            )
        )
        reminderDao.upsert(
            baseReminder.copy(
                title = "Different day",
                bsYear = 2082,
                bsMonth = 6,
                bsDay = 16,
                hour = 8,
                minute = 0,
                recurrenceType = ReminderEntity.RECURRENCE_YEARLY_BS
            )
        )

        val priorYear = reminderDao.observeForDate(bsYear = 2081, bsMonth = 6, bsDay = 15).first()
        assertTrue(priorYear.isEmpty())

        val startYear = reminderDao.observeForDate(bsYear = 2082, bsMonth = 6, bsDay = 15).first()
        assertEquals(listOf("Ama birthday", "One-time dentist"), startYear.map(ReminderEntity::title))

        val nextYear = reminderDao.observeForDate(bsYear = 2083, bsMonth = 6, bsDay = 15).first()
        assertEquals(listOf("Ama birthday"), nextYear.map(ReminderEntity::title))

        val laterYear = reminderDao.observeForDate(bsYear = 2085, bsMonth = 6, bsDay = 15).first()
        assertEquals(listOf("Ama birthday", "Future yearly start"), laterYear.map(ReminderEntity::title))
    }

    @Test
    fun observeForMonthMatchesCurrentOneTimeAndActiveYearlyBsReminders() = runTest {
        val yearly = ReminderEntity.RECURRENCE_YEARLY_BS
        val oneTime = ReminderEntity.RECURRENCE_ONE_TIME
        listOf(
            baseReminder.copy(
                title = "Dashain shopping",
                bsYear = 2082,
                bsMonth = 6,
                bsDay = 20,
                hour = 10,
                recurrenceType = oneTime
            ),
            baseReminder.copy(
                title = "Annual family puja",
                bsYear = 2080,
                bsMonth = 6,
                bsDay = 5,
                hour = 6,
                minute = 30,
                recurrenceType = yearly
            ),
            baseReminder.copy(
                title = "Next year one-time",
                bsYear = 2083,
                bsMonth = 6,
                bsDay = 10,
                hour = 9,
                recurrenceType = oneTime
            ),
            baseReminder.copy(
                title = "Future yearly",
                bsYear = 2085,
                bsMonth = 6,
                bsDay = 12,
                hour = 9,
                recurrenceType = yearly
            ),
            baseReminder.copy(
                title = "Other month",
                bsYear = 2082,
                bsMonth = 7,
                bsDay = 5,
                hour = 6,
                minute = 30,
                recurrenceType = yearly
            )
        ).forEach { reminderDao.upsert(it) }

        val month2082 = reminderDao.observeForMonth(bsYear = 2082, bsMonth = 6).first()
        assertEquals(listOf("Annual family puja", "Dashain shopping"), month2082.map(ReminderEntity::title))

        val month2083 = reminderDao.observeForMonth(bsYear = 2083, bsMonth = 6).first()
        assertEquals(listOf("Annual family puja", "Next year one-time"), month2083.map(ReminderEntity::title))
    }

    @Test
    fun setEnabledTogglesStateAndFiltersEnabledReminders() = runTest {
        val firstId = reminderDao.upsert(baseReminder.copy(title = "First", isEnabled = true))
        val secondId = reminderDao.upsert(baseReminder.copy(title = "Second", bsDay = 2, isEnabled = true))

        reminderDao.setEnabled(firstId, enabled = false)

        val enabled = reminderDao.getEnabledReminders()
        assertEquals(listOf(secondId), enabled.map(ReminderEntity::id))
        assertEquals(false, reminderDao.getById(firstId)?.isEnabled)

        reminderDao.setEnabled(firstId, enabled = true)
        assertEquals(listOf(firstId, secondId), reminderDao.getEnabledReminders().map(ReminderEntity::id))
    }

    @Test
    fun deleteByIdRemovesTargetReminderOnly() = runTest {
        val keepId = reminderDao.upsert(baseReminder.copy(title = "Keep me"))
        val deleteId = reminderDao.upsert(baseReminder.copy(title = "Delete me", bsDay = 3))

        reminderDao.deleteById(deleteId)

        assertNull(reminderDao.getById(deleteId))
        assertNotNull(reminderDao.getById(keepId))
        assertEquals(listOf(keepId), reminderDao.observeAll().first().map(ReminderEntity::id))
    }
}
