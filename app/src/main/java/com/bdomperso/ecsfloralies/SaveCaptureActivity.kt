package com.bdomperso.ecsfloralies

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bdomperso.ecsfloralies.databinding.ActivitySaveCaptureBinding
import com.bdomperso.ecsfloralies.datamodel.DataModel
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

class SaveCaptureActivity : AppCompatActivity(), OverwriteFileDialogFragment.NoticeDialogListener {

    private val RESULT_GALLERY = 9002
    private val TAG = "SaveCaptureActivity"

    private var imageFile: File? = null

    private lateinit var gd: GoogleDriveServices

    private lateinit var viewModel: DataModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val jsonTxt =
            resources.openRawResource(R.raw.residence).bufferedReader().use { it.readText() }
        try {
            viewModel = DataModel(jsonTxt)
            val binding =
                DataBindingUtil.setContentView<ActivitySaveCaptureBinding>(this, R.layout.activity_save_capture)
            binding.lifecycleOwner = this
            binding.viewmodel = viewModel
        } catch (ex: Exception) {
            println("onCreate: ${ex.message}")
        }

        findViewById<Button>(R.id.saveImageButton).setOnClickListener {
            saveImage()
        }

        val argument = intent.extras?.getString("image_path")
        imageFile = if (argument != null) File(argument) else null

        Log.i("ExternalStorage", "onCreate, arg=${imageFile ?: "none"}")
    }

    override fun onStart() {
        super.onStart()

        // Check if the user is already signed in and all required scopes are granted
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(Scopes.DRIVE_FILE)
            )
        ) {
            gd = GoogleDriveServices(this, account)
            Log.i(TAG, "logged in")
        } else {
            // TODO disable GUI
            Log.i(TAG, "logged out")
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, file: File) {
        println("positive response")
        try {
            file.delete()
            saveImage()
        } catch (ex: IOException) {
            Log.e("ExternalStorage", "delete file failed", ex)
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment, file: File) {
        println("negative response")
    }

    private fun saveImage() {

        if (!imageFile!!.exists()) {
            Log.e(TAG,"${imageFile!!.absolutePath} file doesn't exist")
            Toast.makeText(this, "Photo ${imageFile!!.absolutePath} non trouvée", Toast.LENGTH_LONG)
            return
        }

        val destFilename = viewModel.filename.value ?: "BATX_ETX_FX_XX.jpg"
        val destFile = File(imageFile!!.parent).resolve(destFilename)

        if (destFile.exists()) {
            val dialog = OverwriteFileDialogFragment(destFile)
            dialog.show(supportFragmentManager, "OverwriteFileDialogFragment")
            return
        }

        try {
            imageFile!!.renameTo(destFile)

            gd.uploadImageFile(destFile.absolutePath, destFilename)

            Toast.makeText(this, "$destFilename créée dans Photos", Toast.LENGTH_LONG)

            return
        } catch (ex: ApiException) {
            Log.e(TAG, "saveImage: Api error: ${ex.message}")
        } catch (ex: IOException) {
            Log.e("ExternalStorage", "renamed failed", ex)
        } catch (ex: Exception) {
            Log.e(TAG, "saveImage: general error: ${ex.message}")
        }
    }
}