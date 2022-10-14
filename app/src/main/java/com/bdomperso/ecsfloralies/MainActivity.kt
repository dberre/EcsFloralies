package com.bdomperso.ecsfloralies

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L


class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 9000
    private val RC_SIGN_IN = 9001
    private val REQUEST_IMAGE_CAPTURE = 9002
    private val TAG = "ecsfloralies.mainactivity"

    private lateinit var signInButton: Button
    private lateinit var signOutButton: Button
    private lateinit var endoscopeButton: Button
    private lateinit var cameraButton: Button

    private var mGoogleSignInClient: GoogleSignInClient? = null

    private var observer: FileObserver? = null

    companion object {
        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signInButton = findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            signIn()
        }

        signOutButton = findViewById(R.id.signOutButton)
        signOutButton.setOnClickListener {
            signOut()
        }

        cameraButton = findViewById(R.id.cameraButton)
        cameraButton.setOnClickListener {
            launchCameraCapture()
        }

        endoscopeButton = findViewById(R.id.endoscopeButton)
        endoscopeButton.setOnClickListener {
            launchEndoscopeCapture()
        }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.DRIVE_FILE))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val pathToWatch = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            observer = FolderObserver(this, pathToWatch)
            // TODO stop watching ?
        } else {
            observer = FolderObserverLegacy(this, pathToWatch)
        }
    }

    override fun onStart() {
        super.onStart()

        observer!!.stopWatching()

        // Check if the user is already signed in and all required scopes are granted
        val account = GoogleSignIn.getLastSignedInAccount(this)
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
            REQUEST_IMAGE_CAPTURE -> {
                println("REQUEST_IMAGE_CAPTURE")
            }
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        mGoogleSignInClient!!.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun launchCameraCapture() {
        val intent = Intent(this, CameraCaptureActivity::class.java)
        this.startActivity(intent)
    }

    private fun launchEndoscopeCapture() {
        val packageName = "com.ipotensic.depstech"
//        val packageName = "org.o7planning.simulatedepstech"
        observer!!.startWatching()
        if (isAppInstalled(this, packageName))
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
        else Toast.makeText(
            this,
            "App Depstech-View not installed",
            Toast.LENGTH_SHORT)
            .show()
    }

    private fun isAppInstalled(activity: Activity, packageName: String?): Boolean {
        val pm = activity.packageManager
        try {
            pm.getPackageInfo(packageName!!, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
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
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val writePermission = ActivityCompat.checkSelfPermission(
            this,
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
            this,
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
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

}