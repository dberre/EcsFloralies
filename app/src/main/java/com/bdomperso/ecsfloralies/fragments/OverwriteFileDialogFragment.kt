package com.bdomperso.ecsfloralies.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
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
            builder.setMessage(formattedMessage(file.name))
                .setPositiveButton(R.string.yes) { _, _ ->
                    listener.onDialogPositiveClick(this, file)
                    findNavController().navigate(OverwriteFileDialogFragmentDirections.actionOverwriteFileDialogFragmentToEntryFragment())
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

    private fun formattedMessage(filename: String): String {
        val matches = Regex("([ABC])_ET([01234])_(.+_ASC)_(.+)\\.").find(filename)
        if ((matches != null) && (matches!!.groups.count() == 5)) {
            return getString(
                R.string.file_exsists_alert_full,
                filename,
                matches.groups[1]!!.value,
                matches.groups[2]!!.value,
                matches.groups[3]!!.value,
                if (matches.groups[4]!!.value.contains("VMC_")) getString(R.string.vmc_exhaust) else getString(
                                    R.string.ecs_counter),
                matches.groups[4]!!.value
            )
        }
        return getString(R.string.file_exsists_alert, filename)
    }
}