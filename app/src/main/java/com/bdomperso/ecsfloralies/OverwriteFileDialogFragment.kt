package com.bdomperso.ecsfloralies

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.io.File

class OverwriteFileDialogFragment(file: File) : DialogFragment() {

    private val file: File
    private lateinit var listener: NoticeDialogListener

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, file: File)
        fun onDialogNegativeClick(dialog: DialogFragment, file: File)
    }

    init {
        this.file = file
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
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement NoticeDialogListener")
        }
    }
}