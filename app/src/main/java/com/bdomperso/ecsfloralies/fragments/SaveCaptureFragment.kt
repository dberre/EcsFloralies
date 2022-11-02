package com.bdomperso.ecsfloralies.fragments

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bdomperso.ecsfloralies.GoogleDriveServices
import com.bdomperso.ecsfloralies.R
import com.bdomperso.ecsfloralies.UploadWorker
import com.bdomperso.ecsfloralies.databinding.FragmentSaveCaptureBinding
import com.bdomperso.ecsfloralies.datamodel.DataModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

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
            val imagePath = requireArguments().getString("photoPath")
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
        savedInstanceState: Bundle?
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

        view.findViewById<Button>(R.id.saveImageButton).setOnClickListener {
            saveImage()
        }

        if (imageFile?.absolutePath != null) {
            _viewModel!!.image = imageFile?.absolutePath!! // TODO
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
            // TODO disable GUI
            Log.i(TAG, "OnStart: logged out")
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment, file: File) {
        CoroutineScope(Dispatchers.IO).launch() {
            try {
                // delete the image on the telephone and on Google Drive

                file.delete()
                gd.deleteImage(file.name)

                withContext(Dispatchers.Main) {
                    saveImage()
                }

            } catch (ex: IOException) {
                Log.e(TAG, "delete image ${file.name} failed", ex)
                // TODO manage the fact that this will happen when the connection to drive is nok
            } catch (ex: SecurityException) {
                // local rename on the device fails (reasons ?)
                Log.e(TAG, "delete image ${file.name} not allowed", ex)
            }
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment, file: File) {
        // nothing to do here for now
    }

    private fun saveImage() {

        if (!imageFile!!.exists()) {
            Log.e(TAG, "${imageFile!!.absolutePath} file doesn't exist")
            Toast.makeText(requireContext(), "Photo ${imageFile!!.absolutePath} non trouvée", Toast.LENGTH_LONG).show()
            return
        }

        val destFilename = _viewModel!!.filename.value ?: "BATX_ETX_FX_XX.jpg"
        val destFile = File(imageFile!!.parent).resolve(destFilename)

        if (destFile.exists()) {
            val action = SaveCaptureFragmentDirections.actionSaveCaptureFragmentToOverwriteFileDialogFragment(destFile)
            findNavController().navigate(action)
            return
        }

        try {
            imageFile!!.renameTo(destFile)

            enqueueUpload(destFile, destFilename)

            Toast.makeText(
                requireContext(),
                "Image $destFilename créée dans Photos et sur Google Drive",
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigate(SaveCaptureFragmentDirections.actionSaveCaptureFragmentToEntryFragment())

        } catch (ex: SecurityException) {
            Log.e(TAG, "saveImage: rename failed: ${ex.message}")
        }
    }

    private fun enqueueUpload(srcFile: File, destFilename: String) {
        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                Pair("SrcFilePath", srcFile.absolutePath),
                Pair("DestFilename", destFilename)))
            .build()
        WorkManager.getInstance(requireContext()).enqueue(uploadWorkRequest)
    }

    private val defaultImageUri
        get() =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                requireContext().resources.getResourcePackageName(R.drawable.no_image_available) + '/' +
                requireContext().resources.getResourceTypeName(R.drawable.no_image_available) + '/' +
                requireContext().resources.getResourceEntryName(R.drawable.no_image_available))
}