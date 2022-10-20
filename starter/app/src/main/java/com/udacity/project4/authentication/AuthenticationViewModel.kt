package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel : ViewModel() {

    enum class AuthState {
        AUTHENTICATED,
        UNAUTHENTICATED
    }

    val authState = FirebaseUserLiveData().map { user ->
        if (user == null)
            AuthState.UNAUTHENTICATED
        else
            AuthState.AUTHENTICATED
    }

}