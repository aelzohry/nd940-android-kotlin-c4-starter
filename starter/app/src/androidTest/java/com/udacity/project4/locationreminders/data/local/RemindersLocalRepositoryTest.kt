package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // sut
    private lateinit var repository: RemindersLocalRepository

    private lateinit var database: RemindersDatabase

    // executes each task synchronously using architecture components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // replace main coroutine dispatcher for all tests
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 0.0, 0.0, "1")
    private val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 0.0, 0.0, "2")
    private val reminder3 = ReminderDTO("title 3", "description 3", "location 3", 0.0, 0.0, "3")

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        repository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_savesUsingDAO() = mainCoroutineRule.runTest {
        // Given - insert reminder
        repository.saveReminder(reminder1)

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
    fun getReminders_returnSuccessResult() = mainCoroutineRule.runTest {
        // Given - insert reminders
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // When - getting reminders result
        val result = repository.getReminders()

        // Then - asserting the loaded reminders against the  expected data
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val list = (result as Result.Success).data

        assertThat(list.size, `is`(3))
        assertThat(list[0].id, `is`(reminder1.id))
        assertThat(list[1].id, `is`(reminder2.id))
        assertThat(list[2].id, `is`(reminder3.id))
    }

    @Test
    fun getReminderByInvalidId_returnsError() = mainCoroutineRule.runTest {
        // Given - empty reminders list

        // When - getting reminder by id
        val result = repository.getReminder("1")

        // Then - asserting the result is error with the correct message
        assertThat(result, `is`(instanceOf(Result.Error::class.java)))

        val errorMessage = (result as Result.Error).message
        assertThat(errorMessage, `is`("Reminder not found!"))
    }

    @Test
    fun getReminderById_returnsCorrectReminder() = mainCoroutineRule.runTest {
        // Given - insert some reminders
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // When - getting reminder by id
        val result = repository.getReminder(reminder1.id)

        // Then - asserting the result is success with the correct reminder
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val loadedReminder = (result as Result.Success).data
        assertThat(loadedReminder.id, `is`(loadedReminder.id))
    }

    @Test
    fun deleteAllReminders_gettingReminders_returnEmptyList() = mainCoroutineRule.runTest {
        // Given - insert some reminders
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // When - deleting all reminders
        repository.deleteAllReminders()

        // Then - asserting the result is success with empty list
        val result = repository.getReminders()
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))
        val list = (result as Result.Success).data
        assertThat(list.size, `is`(0))
    }

}