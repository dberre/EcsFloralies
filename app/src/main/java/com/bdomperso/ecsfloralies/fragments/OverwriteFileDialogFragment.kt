package com.bdomperso.ecsfloralies.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bdomperso.ecsfloralies.R
import java.io.File

class OverwriteFileDialogFragment: DialogFragment() {

    private lateinit var file: File
    private lateinit var listener: NoticeDialogListener

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, file: File)
        fun onDialogNegativeClick(dialog: DialogFragment, file: File)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            file = requireArguments().getSerializable("file") as File
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(getString(R.string.file_exsists_alert, file.name))
                .setPositiveButton(R.string.yes) { _, _ ->
                    listener.onDialogPositiveClick(this, file)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    listener.onDialogNegativeClick(this, file)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            // This dialog must be called from SaveCaptureFragment which remains the current nav host
            listener = parentFragmentManager.findFragmentById(R.id.fragmentContainerView) as NoticeDialogListener

        } catch (e: ClassCastException) {
            throw ClassCastException("The  must implement NoticeDialogListener")
        }
    }
}