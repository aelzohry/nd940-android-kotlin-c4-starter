package com.udacity.project4.locationreminders.reminderslist

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: KoinTest {

    private lateinit var fakeRepository: ReminderDataSource

    // replace main coroutine dispatcher for all tests
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val reminder1 = ReminderDTO("title 1", "description 1", "location 1", 0.0, 0.0, "1")
    private val reminder2 = ReminderDTO("title 2", "description 2", "location 2", 0.0, 0.0, "2")
    private val reminder3 = ReminderDTO("title 3", "description 3", "location 3", 0.0, 0.0, "3")

    @Before
    fun init() {
        stopKoin()

        /**
         * use Koin Library as a service locator
         */
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            //Declare singleton definitions to be later injected using by inject()
            single { FakeDataSource() as ReminderDataSource }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(myModule))
        }

        // initialize fakeRepository using Koin
        fakeRepository = get()
    }

    @After
    fun clean() = mainCoroutineRule.runTest {
        fakeRepository.deleteAllReminders()
    }

    @Test
    fun tapAddReminder_navigatesToSaveReminderFragment() = mainCoroutineRule.runTest {
        // Given - RemindersListFragment
        val fragmentScenario = launchFragmentInContainer<ReminderListFragment>(bundleOf(), R.style.AppTheme)

        // mock NavController
        val navController = mock(NavController::class.java)

        // replace real navController with mocked one
        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        // When - tap add reminder fab button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Then - verify navigate to save reminder fragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun tapReminder_navigatesToReminderDescriptionActivity() = mainCoroutineRule.runTest {
        Intents.init()

        // Given - RemindersListFragment with reminders list
        fakeRepository.saveReminder(reminder1)
        launchFragmentInContainer<ReminderListFragment>(bundleOf(), R.style.AppTheme)

        // When - tap reminder1
        onView(withId(R.id.reminders_recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(reminder1.title)), click()))

        // Then - verify navigate to description activity
        intended(hasComponent(ReminderDescriptionActivity::class.java.name))

        Intents.release()
    }

    @Test
    fun reminders_displayedInUi() = mainCoroutineRule.runTest {
        // Given - some reminders
        fakeRepository.saveReminder(reminder1)
        fakeRepository.saveReminder(reminder2)
        fakeRepository.saveReminder(reminder3)

        // When - fragment launched
        launchFragmentInContainer<ReminderListFragment>(bundleOf(), R.style.AppTheme)

        // Then - verify reminders are displayed
        onView(withText(reminder1.title)).check(matches(isDisplayed()))
        onView(withText(reminder2.title)).check(matches(isDisplayed()))
        onView(withText(reminder3.title)).check(matches(isDisplayed()))

        // and no data text is hidden
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun reminders_emptyList_noDataDisplayed() = mainCoroutineRule.runTest {
        // Given - empty reminders list

        // When - fragment launched
        launchFragmentInContainer<ReminderListFragment>(bundleOf(), R.style.AppTheme)

        // Then - verify no data text is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(withText(R.string.no_data)))
    }

}