package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    // for temporarily usage
    private var tempReminderData: ReminderDataItem? = null

    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                // All permissions are granted
                checkDeviceLocationSettings()
            } else {
                _viewModel.showSnackBar.value =
                    getString(R.string.permission_denied_explanation)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener { onSelectLocation() }
        binding.saveReminder.setOnClickListener { onSave() }
    }

    /**
     * select location button handler
     */
    private fun onSelectLocation() {
        // Navigate to another fragment to get the user location
        _viewModel.navigationCommand.value =
            NavigationCommand.To(
                SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment()
            )
    }

    /**
     * save button handler
     */
    private fun onSave() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        // initialize data item
        val reminderData = ReminderDataItem(
            title = title,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude
        )

        // validate input
        if (!_viewModel.validateEnteredData(reminderData))
            return

        tempReminderData = reminderData

        // 1) add a geofencing request
        // First check permissions
        checkPermissionsAndStartGeofence()

        // 2) save the reminder to the local db
    }

    private fun checkPermissionsAndStartGeofence() {
        if (isLocationPermissionGranted()) {
            checkDeviceLocationSettings()
        } else {
            // request location permissions from the user
            requestPermissionLauncher.launch(getPermissionsArray())
        }
    }

    private fun getPermissionsArray(): Array<String> {
        // Foreground location permissions
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Background location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return permissions.toTypedArray()
    }

    private fun isLocationPermissionGranted(): Boolean {
        val permissionsToCheck = getPermissionsArray()
        permissionsToCheck.forEach {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    it) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    private fun checkDeviceLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val request = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        settingsClient.checkLocationSettings(request)
            .addOnFailureListener {
                Timber.e("checkDeviceLocationSettings failed: $it")
                Snackbar
                    .make(
                        binding.root,
                        getString(R.string.location_required_error),
                        Snackbar.LENGTH_INDEFINITE
                    )
                    .setAction(R.string.settings) {
                        // open location settings
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }.show()
            }
            .addOnSuccessListener {
                Timber.i("checkDeviceLocationSettings succeed")
                // finally :) add geofence
                addGeofence()
            }

    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        val reminderData = tempReminderData ?: return

        // 1. Create geofence object
        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(reminderData.id)

            // Set the circular region of this geofence.
            .setCircularRegion(
                reminderData.latitude ?: 0.0,
                reminderData.longitude ?: 0.0,
                Constants.GEOFENCE_RADIUS_IN_METERS
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry only.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

            // Create the geofence.
            .build()

        // 2. Create geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            // Add the geofence to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        // 3. add geofence
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnFailureListener {
                Timber.e("addGeofence failed: $it")
                // Alert user with the error
                _viewModel.showToast.value = getString(R.string.error_adding_geofence)
            }
            .addOnSuccessListener {
                Timber.i("addGeofence succeed")
                _viewModel.validateAndSaveReminder(reminderData)
            }
    }

    override fun onDestroy() {
        // make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        super.onDestroy()
    }
}
