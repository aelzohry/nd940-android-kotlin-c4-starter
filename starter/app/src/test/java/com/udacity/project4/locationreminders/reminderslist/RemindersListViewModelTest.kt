package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // to be injected in view model
    private lateinit var fakeDataSource: FakeDataSource

    // sut (subject under test)
    private lateinit var viewModel: RemindersListViewModel

    // executes each task synchronously using architecture components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // replace main coroutine dispatcher for all tests
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun loadReminders_emptyList_showsNoData() = mainCoroutineRule.runTest {
        // When loading reminders with empty list
        viewModel.loadReminders()

        // Then - no data shown
        assertThat(viewModel.remindersList.getOrAwaitValue().size, `is`(0))
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_loadingIndicators() = mainCoroutineRule.runTest {
        // pause dispatcher to verify loading initial value
        mainCoroutineRule.pauseDispatcher()

        // When loading reminders
        viewModel.loadReminders()

        // Then assert loading initial value is true
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // resume dispatcher to execute pending actions
        mainCoroutineRule.resumeDispatcher()

        // Then assert loading current value is false
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_error_showsSnackBar() = mainCoroutineRule.runTest {
        // Given error
        fakeDataSource.shouldReturnError = true

        // When loading reminders
        viewModel.loadReminders()

        // Then - snack bar shows with error message
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Error loading data"))
    }

    @Test
    fun loadReminders_updatesRemindersList() = mainCoroutineRule.runTest {
        // Given few reminders
        val reminders = arrayListOf(
            ReminderDTO("Reminder 1", "Description 1",
                "Location 1", 1.0, 1.0, "1"),
            ReminderDTO("Reminder 2", "Description 2",
                "Location 2", 1.0, 1.0, "2"),
            ReminderDTO("Reminder 3", "Description 3",
                "Location 3", 1.0, 1.0, "3")
        )
        reminders.forEach {
            fakeDataSource.saveReminder(it)
        }

        // When loading reminders
        viewModel.loadReminders()

        // Then - reminders list size should be 3
        assertThat(viewModel.remindersList.getOrAwaitValue().size, `is`(reminders.size))
    }

}