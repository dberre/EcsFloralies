package com.bdomperso.ecsfloralies.fragments

import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bdomperso.ecsfloralies.GoogleDriveServices
import com.bdomperso.ecsfloralies.MainActivity
import com.bdomperso.ecsfloralies.R
import com.bdomperso.ecsfloralies.UploadWorker
import com.bdomperso.ecsfloralies.databinding.FragmentSaveCaptureBinding
import com.bdomperso.ecsfloralies.datamodel.DataModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import java.io.File
import java.util.*


/**
 * This fragment ensures the ....
 */
class SaveCaptureFragment : Fragment(), OverwriteFileDialogFragment.NoticeDialogListener {

    private val TAG = "SaveCaptureFragment"

    private var imageFile: File? = null

    private lateinit var gd: GoogleDriveServices

    // This property is only valid between onCreateView and onDestroyView.
    private var _binding: FragmentSaveCaptureBinding? = null

    private var _viewModel: DataModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageFile = if (arguments != null) {
            val imagePath = requireArguments().getString("photoPath")!!
            File(imagePath)
        } else {
            null
        }

        val jsonTxt = resources.openRawResource(R.raw.residence).bufferedReader().use { it.readText() }
        _viewModel = DataModel(jsonTxt)

        Log.i(TAG, "onCreate, arg=${imageFile ?: "none"}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Log.i(TAG, "onCreateView")

        try {
            _binding = FragmentSaveCaptureBinding.inflate(inflater, container, false)
            return _binding!!.root
        } catch (ex: Exception) {
            Log.e(TAG, "onCreateView: ${ex.message}")
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated")

        _binding?.apply {
            viewmodel = _viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        _binding?.saveImageButton?.setOnClickListener {
            saveImage()
        }

        _binding?.saveImageButton?.isEnabled = (imageFile != null)

        if (imageFile?.absolutePath != null) {
            _viewModel!!.image = imageFile?.absolutePath!!
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        _viewModel = null
    }

    override fun onStart() {
        super.onStart()

        // Check if the user is already signed in and all required scopes are granted
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(Scopes.DRIVE_FILE)
            )
        ) {
            gd = GoogleDriveServices(requireContext(), account)
            Log.i(TAG, "OnStart: logged in")
        } else {
            Log.i(TAG, "OnStart: logged out")
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, file: File) {
        if (renameAndSave(file, true)) {
            Toast.makeText(
                requireContext(),
                "Image ${file.name} créée dans Photos et sur Google Drive",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment, file: File) {
        // nothing to do here for now
    }

    private fun saveImage() {

        if (!imageFile!!.exists()) {
            Log.e(TAG, "${imageFile!!.absolutePath} file doesn't exist")
            Toast.makeText(requireContext(),
                "Photo ${imageFile!!.absolutePath} non trouvée",
                Toast.LENGTH_LONG).show()
            // TODO redirect to camera, Alert et Pop ?
            return
        }

        // full path of the destination file on Android local media storage
        val destFile = MainActivity.getOutputDirectory(requireContext()).resolve(
            _viewModel!!.filename.value ?: "BATX_ETX_FX_XX.jpg")

        if (destFile.exists()) {
            findNavController().navigate(
                SaveCaptureFragmentDirections.actionSaveCaptureFragmentToOverwriteFileDialogFragment(
                    destFile))
            return
        }

        if (renameAndSave(destFile, false)) {
            Toast.makeText(
                requireContext(),
                "Image ${destFile.name} créée dans Photos et sur Google Drive",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigate(SaveCaptureFragmentDirections.actionSaveCaptureFragmentToEntryFragment())
        } else {
            // TODO redirect to camera, Alert et Pop ?
        }
    }

    private fun renameAndSave(destFile: File, overwrite: Boolean): Boolean {
        return try {
            if (overwrite) {
                destFile.delete()
            }

            val photoSource: String
            if (imageFile!!.parent == MainActivity.getOutputDirectory(requireContext()).absolutePath) {
                // the photo is already in the expected folder, capture by internal camera, just rename it
                imageFile!!.renameTo(destFile)
                photoSource = "camera"
            } else {
                // move the photo to the expected folder (was captured by the external application in another folder)
                imageFile!!.copyTo(destFile)
                imageFile!!.delete()
                photoSource = "endoscope"
            }

            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(destFile.extension)

            // make the image available as a media content for other applications
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType)
            ) { _, mediaStoreUri ->
                Log.d(TAG, "Image capture scanned into media store: $mediaStoreUri")
            }

            val description = "Smartphone: ${getDeviceName()}\nPhoto: ${imageFile!!.name}\nSource: $photoSource"
            sendToGoogleDrive(destFile, destFile.name, description, overwrite)

            true
        } catch (ex: SecurityException) {
            Log.e(TAG, "saveImage: can't create local file ${destFile.name}: ${ex.message}")
            false
        }
    }

    private fun sendToGoogleDrive(srcFile: File, destFilename: String, description: String, overwrite: Boolean) {
        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                Pair("SrcFilePath", srcFile.absolutePath),
                Pair("DestFilename", destFilename),
                Pair("Description", description),
                Pair("Overwrite", overwrite)))
            .build()
        WorkManager.getInstance(requireContext()).enqueue(uploadWorkRequest)
    }

    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
            model
        } else {
            "${manufacturer.uppercase()} $model"
        }
    }
}