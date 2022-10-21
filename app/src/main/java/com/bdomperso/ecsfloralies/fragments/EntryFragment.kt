package com.bdomperso.ecsfloralies.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bdomperso.ecsfloralies.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * This is the entry fragment of the app.
 */
class EntryFragment : Fragment() {

    private val PERMISSIONS_REQUEST_CODE = 9000
    private val RC_SIGN_IN = 9001
    private val TAG = "EntryFragment"

    private lateinit var signInButton: Button
    private lateinit var signOutButton: Button
    private lateinit var endoscopeButton: Button
    private lateinit var cameraButton: Button

    private var mGoogleSignInClient: GoogleSignInClient? = null

    private var observer: FileObserver? = null

    // This is the path where the Depstech-View app is saving the image (with Android 7, and for later ? TODO)
    private val pathToWatch = File("/sdcard/DCIM/DEPSTECH_View")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.DRIVE_FILE))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FolderObserver(requireContext(), pathToWatch)
            // TODO stop watching ?
        } else {
            FolderObserverLegacy(requireContext(), pathToWatch)
        }
        Log.i(TAG, "End of onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signInButton = view.findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            signIn()
        }

        signOutButton = view.findViewById(R.id.signOutButton)
        signOutButton.setOnClickListener {
            signOut()
        }

        cameraButton = view.findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            launchCameraCapture()
        }

        endoscopeButton = view.findViewById(R.id.endoscopeButton)
        endoscopeButton.setOnClickListener {
            launchEndoscopeCapture()
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if the user is already signed in and all required scopes are granted
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(Scopes.DRIVE_FILE))) {
            Log.i(TAG, "already logged in")
        } else {
            Log.i(TAG, "logged out")
        }
        updateUI(account)

        // TODO is this the best place ?
        checkStoragePermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        when (requestCode) {
            RC_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        mGoogleSignInClient!!.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUI(null)
            } else {
                // Task failed with an exception
                val exception = task.exception
            }
        }
    }

    private fun launchCameraCapture() {
        val photoFile = File(
            MainActivity.getOutputDirectory(requireContext()), SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg")

        val action = EntryFragmentDirections.actionEntryFragmentToCameraFragment(photoFile)
        this.findNavController().navigate(action)
    }

    private fun launchEndoscopeCapture() {
//        val packageName = "com.ipotensic.depstech"
        val packageName = "org.o7planning.simulatedepstech"

        try {
            val pm = requireContext().packageManager
            // the next call will throw a specific exception if the app not installed
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            startActivity(pm.getLaunchIntentForPackage(packageName))
            observer!!.startWatching()

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "isAppInstalled: ${e.message}")
            Toast.makeText(
                context,
                "App Depstech-View not installed",
                Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>?) {
        Log.d(TAG, "handleSignInResult:" + completedTask!!.isSuccessful)
        try {
            // Signed in successfully, show authenticated U
            val googleAccount = completedTask.getResult(ApiException::class.java)
            updateUI(googleAccount)
        } catch (e: ApiException) {
            // Signed out, show unauthenticated UI.
            Log.w(TAG, "handleSignInResult:error", e)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
        }
    }

    private fun checkStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) return true

            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
            return false
        }
        return checkStoragePermissionsLegacy()
    }

    private fun checkStoragePermissionsLegacy(): Boolean {
        val readPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val writePermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!(readPermission && writePermission)) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), PERMISSIONS_REQUEST_CODE
            )
            return false
        }

        val cameraPermission = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraPermission) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                ), PERMISSIONS_REQUEST_CODE
            )
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(requireContext(), "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }
}