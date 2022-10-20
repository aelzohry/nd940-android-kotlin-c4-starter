package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import timber.log.Timber

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding

    val viewModel: AuthenticationViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.loginButton.setOnClickListener { onLogin() }

        // observe auth state and go to reminders activity if user is authenticated
        viewModel.authState.observe(this) {
            when (it) {
                AuthenticationViewModel.AuthState.AUTHENTICATED -> {
                    Timber.i("User is authenticated")

                    // Open Reminders Activity
                    startActivity(Intent(this, RemindersActivity::class.java))

                    // finish auth activity
                    finish()
                }
                else -> {
                    Timber.i("User is unauthenticated")
                }
            }
        }
    }

    /**
     * login button handler
     */
    private fun onLogin() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.map)
            .build()
        signInLauncher.launch(signInIntent)
    }

    /**
     * firebase auth result handler
     */
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse

        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            Timber.i("Successfully signed in: user ${user?.email}")
        } else {
            // Failed to sign in
            if (response == null) {
                // user canceled the sign-in flow
                Timber.i("Failed to sign in: user canceled the process")
            } else {
                Timber.i("Failed to sign in: ${response.error}")

                // alert the user with the error
                Toast.makeText(this,
                    response.error?.localizedMessage ?: getString(R.string.error_happened),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

}
