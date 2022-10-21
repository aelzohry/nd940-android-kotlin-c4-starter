package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // sut
    private lateinit var database: RemindersDatabase

    // executes each task synchronously using architecture components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // replace main coroutine dispatcher for all tests
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val reminder1 = ReminderDTO("title 1",  "description 1", "location 1", 0.0, 0.0, "1")
    private val reminder2 = ReminderDTO("title 2",  "description 2", "location 2", 0.0, 0.0, "2")
    private val reminder3 = ReminderDTO("title 3",  "description 3", "location 3", 0.0, 0.0, "3")

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = mainCoroutineRule.runTest {
        // Given - insert reminder
        database.reminderDao().saveReminder(reminder1)

        // When - getting the reminder by id
        val loadedReminder = database.reminderDao().getReminderById(reminder1.id)

        // Then - asserting the loaded reminder against the  expected data
        assertThat(loadedReminder, `is`(notNullValue()))
        assertThat(loadedReminder?.id, `is`(reminder1.id))
        assertThat(loadedReminder?.title, `is`(reminder1.title))
        assertThat(loadedReminder?.description, `is`(reminder1.description))
        assertThat(loadedReminder?.location, `is`(reminder1.location))
        assertThat(loadedReminder?.latitude, `is`(reminder1.latitude))
        assertThat(loadedReminder?.longitude, `is`(reminder1.longitude))
    }

    @Test
    fun insertRemindersAndGetAll() = mainCoroutineRule.runTest {
        // Given - insert reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // When - getting all reminders
        val loadedReminders = database.reminderDao().getReminders()

        // Then - asserting the loaded reminders result
        assertThat(loadedReminders.size, `is`(3))
        assertThat(loadedReminders[0].id, `is`(reminder1.id))
        assertThat(loadedReminders[1].id, `is`(reminder2.id))
        assertThat(loadedReminders[2].id, `is`(reminder3.id))
    }

    @Test
    fun deleteAllReminders() = mainCoroutineRule.runTest {
        // Given - insert reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // When - deleting the reminders and getting all reminders
        database.reminderDao().deleteAllReminders()

        val loadedReminders = database.reminderDao().getReminders()

        // Then - asserting the loaded reminders result
        assertThat(loadedReminders.size, `is`(0))
    }

    @Test
    fun updateReminder() = mainCoroutineRule.runTest {
        // Given - insert reminder
        database.reminderDao().saveReminder(reminder1)

        val updatedReminder = reminder1.copy(
            "updated title",
            "updated description",
            "updated location",
            1.0,
            1.0
        )

        // insert reminder with the same id should replace the old one
        database.reminderDao().saveReminder(updatedReminder)

        // When - getting reminder by id
        val loadedReminder = database.reminderDao().getReminderById(reminder1.id)

        // Then - asserting the loaded reminder against updatedReminder
        assertThat(loadedReminder, `is`(notNullValue()))
        assertThat(loadedReminder?.id, `is`(updatedReminder.id))
        assertThat(loadedReminder?.title, `is`(updatedReminder.title))
        assertThat(loadedReminder?.description, `is`(updatedReminder.description))
        assertThat(loadedReminder?.location, `is`(updatedReminder.location))
        assertThat(loadedReminder?.latitude, `is`(updatedReminder.latitude))
        assertThat(loadedReminder?.longitude, `is`(updatedReminder.longitude))
    }

    @Test
    fun getReminder_invalidId_returnsNull() = mainCoroutineRule.runTest {
        // Given - empty db

        // When - getting reminder by invalid id
        val loadedReminder = database.reminderDao().getReminderById("123")

        // Then - asserting the loaded reminder is null
        assertThat(loadedReminder, `is`(nullValue()))
    }

}