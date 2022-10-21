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
import com.bdomperso.ecsfloralies.GoogleDriveServices
import com.bdomperso.ecsfloralies.OverwriteFileDialogFragment
import com.bdomperso.ecsfloralies.R
import com.bdomperso.ecsfloralies.databinding.FragmentSaveCaptureBinding
import com.bdomperso.ecsfloralies.datamodel.DataModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
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

    private lateinit var viewModel: DataModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageFile = if (arguments != null) {
            val imagePath = requireArguments().getString("photoPath")
            File(imagePath)
        } else {
            null
        }

        Log.i(TAG, "onCreate, arg=${imageFile ?: "none"}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val jsonTxt =
            resources.openRawResource(R.raw.residence).bufferedReader().use { it.readText() }
        try {
            viewModel = DataModel(jsonTxt)
            val binding =
                FragmentSaveCaptureBinding.inflate(inflater,container, false)
            binding.viewmodel = viewModel
            binding.lifecycleOwner = this
            return binding.root
        } catch (ex: Exception) {
            Log.e(TAG, "onCreateView: ${ex.message}")
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.saveImageButton).setOnClickListener {
            saveImage()
        }

        if (imageFile?.absolutePath != null) {
            viewModel.image = imageFile?.absolutePath!! // TODO
        }
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
            Log.i(TAG, "logged in")
        } else {
            // TODO disable GUI
            Log.i(TAG, "logged out")
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
            } catch (ex: SecurityException) {
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

        val destFilename = viewModel.filename.value ?: "BATX_ETX_FX_XX.jpg"
        val destFile = File(imageFile!!.parent).resolve(destFilename)

        if (destFile.exists()) {
            val dialog = OverwriteFileDialogFragment(destFile)
            dialog.show(childFragmentManager, "OverwriteFileDialogFragment")
            return
        }

        try {
            imageFile!!.renameTo(destFile)

            gd.uploadImageFile(destFile.absolutePath, destFilename, "ECS_2022")

            Toast.makeText(
                requireContext(),
                "$destFilename créée dans Photos et sur Google Drive",
                Toast.LENGTH_LONG
            ).show()

            val action = SaveCaptureFragmentDirections.actionSaveCaptureFragmentToEntryFragment()
            findNavController().navigate(action)
            return
        } catch (ex: ApiException) {
            Log.e(TAG, "saveImage: Api error: ${ex.message}")
        } catch (ex: IOException) {
            Log.e("ExternalStorage", "renamed failed", ex)
        } catch (ex: Exception) {
            Log.e(TAG, "saveImage: general error: ${ex.message}")
        }

        // TODO error popup here. Distinguish rename which must not fail and upload which can fail
    }

    private val defaultImageUri
        get() =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                requireContext().resources.getResourcePackageName(R.drawable.no_image_available) + '/' +
                requireContext().resources.getResourceTypeName(R.drawable.no_image_available) + '/' +
                requireContext().resources.getResourceEntryName(R.drawable.no_image_available))
}