package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private val remindersList: MutableList<ReminderDTO> = mutableListOf(),
) : ReminderDataSource {

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
            return Result.Error("Error loading data")

        return Result.Success(remindersList.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
            return Result.Error("Error loading data")

        val reminder = remindersList.firstOrNull { id == it.id }

        return reminder?.let { Result.Success(it) } ?: Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }

}