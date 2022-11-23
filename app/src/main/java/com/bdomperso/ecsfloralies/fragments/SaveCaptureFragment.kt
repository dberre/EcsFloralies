package com.bdomperso.ecsfloralies.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import com.bdomperso.ecsfloralies.*
import com.bdomperso.ecsfloralies.databinding.FragmentSaveCaptureBinding
import com.bdomperso.ecsfloralies.datamodel.DataModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import java.io.File
import java.io.FileOutputStream
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

            // resize the bitmap do have an image size 960x540 in 16/9
            val resizedBitmap = processImage(imageFile!!.absolutePath!!, 960)
            if (resizedBitmap != null) {
                persistImage(resizedBitmap, destFile.absolutePath)
            } else {
                // if the image can't be resized for whatever reason, then keep the original
                imageFile!!.copyTo(destFile)
            }
            imageFile!!.delete()

            // not perfect, but reasonable way to determine if the image comes from the internal
            // camera or the external DEPSTECH USB endoscope
            val photoSource = if (imageFile!!.parent == MainActivity.getOutputDirectory(requireContext()).absolutePath)
                "camera"  else "endoscope"

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

    private fun processImage(path: String, maxSize: Int): Bitmap? {
        BitmapFactory.decodeFile(path).also { bitmap ->
            var destWidth = bitmap.width
            var destHeight = bitmap.height
            val bitmapRatio = destWidth.toFloat() / destHeight.toFloat()
            if (bitmapRatio > 1) {
                destWidth = maxSize
                destHeight = (destWidth / bitmapRatio).toInt()
            } else {
                destHeight = maxSize
                destWidth = (destHeight * bitmapRatio).toInt()
            }

            val matrix = Matrix()
            // only scaling down is wanted. If the image resolution is below, keep it unchanged.
            if (destWidth < bitmap.width || destHeight < bitmap.height) {
                val sx: Float = destWidth.toFloat() / bitmap.width.toFloat()
                val sy: Float = destHeight.toFloat() / bitmap.height.toFloat()
                matrix.setScale(sx, sy)
            }

            if (destHeight > destWidth) {
                matrix.postRotate(90f)
            }

            return Bitmap.createBitmap(bitmap,0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return null
    }

    private fun persistImage(bitmap: Bitmap, name: String) {
        try {
            val os = FileOutputStream(name)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "persistImage: Error writing bitmap", e)
            throw e
        }
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