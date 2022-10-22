package com.udacity.project4.util

import android.app.Activity
import android.view.View
import androidx.test.core.app.ActivityScenario

fun <T : Activity> ActivityScenario<T>.decorView()
        : View? {
    var decorView: View? = null
    onActivity {
        decorView =
            it.window.decorView
    }
    return decorView
}

fun <T : Activity> ActivityScenario<T>.activity()
        : Activity? {
    var activity: Activity? = null
    onActivity {
        activity = it
    }
    return activity
}