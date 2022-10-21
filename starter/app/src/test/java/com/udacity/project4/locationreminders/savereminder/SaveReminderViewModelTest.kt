package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import com.udacity.project4.locationreminders.data.dto.Result
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // to be injected in view model
    private lateinit var fakeDataSource: FakeDataSource

    // sut (subject under test)
    private lateinit var viewModel: SaveReminderViewModel

    // reminderDataItem with all required data entered
    private val validReminderDataItem = ReminderDataItem(
        title = "title",
        description = "description",
        location = "location",
        latitude = 0.0,
        longitude = 0.0
    )

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
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun validateEnteredData_noTitle_returnFalseAndAlertUser() = mainCoroutineRule.runTest {
        // Given reminderDataItem with no title
        val reminderDataItem = ReminderDataItem(
            title = null,
            description = "description",
            location = "location",
            latitude = 0.0,
            longitude = 0.0
        )

        // When validating entered data
        val validationResult = viewModel.validateEnteredData(reminderDataItem)

        // Then - validation failed and snackBarInt set correctly
        assertThat(validationResult, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_noLocation_returnFalseAndAlertUser() = mainCoroutineRule.runTest {
        // Given reminderDataItem with empty location
        val reminderDataItem = ReminderDataItem(
            title = "title",
            description = "description",
            location = "",
            latitude = 0.0,
            longitude = 0.0
        )

        // When validating entered data
        val validationResult = viewModel.validateEnteredData(reminderDataItem)

        // Then - validation failed and snackBarInt set correctly
        assertThat(validationResult, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_allRequiredDataEntered_returnTrue() = mainCoroutineRule.runTest {
        // Given Valid ReminderDataItem

        // When validating it
        val validationResult = viewModel.validateEnteredData(validReminderDataItem)

        // Then - validation succeed
        assertThat(validationResult, `is`(true))
    }

    @Test
    fun saveReminder_showsAndHidesLoadingIndicator() = mainCoroutineRule.runTest {
        // pause dispatcher to verify loading initial value
        mainCoroutineRule.pauseDispatcher()

        // When saving reminder
        viewModel.validateAndSaveReminder(validReminderDataItem)

        // Then assert loading initial value is true
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // resume dispatcher to execute pending actions
        mainCoroutineRule.resumeDispatcher()

        // Then assert loading current value is false
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun saveReminder_savesToDataSource() = mainCoroutineRule.runTest {
        // Given valid ReminderDataItem

        // When saving the reminder
        viewModel.validateAndSaveReminder(validReminderDataItem)

        val savedItem = fakeDataSource.getReminder(validReminderDataItem.id)

        // Then assert savedItem is of type Result.Success
        assertThat(savedItem, `is`(instanceOf(Result.Success::class.java)))

        // retrieve data
        val data = (savedItem as Result.Success).data

        // Then assert data properties
        assertThat(data.id, `is`(validReminderDataItem.id))
        assertThat(data.title, `is`(validReminderDataItem.title))
        assertThat(data.description, `is`(validReminderDataItem.description))
        assertThat(data.location, `is`(validReminderDataItem.location))
        assertThat(data.latitude, `is`(validReminderDataItem.latitude))
        assertThat(data.longitude, `is`(validReminderDataItem.longitude))
    }

    @Test
    fun saveReminder_showsSuccessfulToast() = mainCoroutineRule.runTest {
        // Given valid ReminderDataItem

        // When saving the reminder
        viewModel.validateAndSaveReminder(validReminderDataItem)

        // Then assert toast value
        assertThat(viewModel.showToast.getOrAwaitValue(), `is`(viewModel.app.getString(R.string.reminder_saved)))
    }

}