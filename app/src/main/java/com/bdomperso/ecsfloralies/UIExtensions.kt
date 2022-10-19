package com.bdomperso.ecsfloralies

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.databinding.BindingAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@BindingAdapter("entries")
fun Spinner.setSpinnerEntries(entries: List<Any>?) {
    if (entries != null) {
        val arrayAdapter = ArrayAdapter(context, R.layout.custom_spinner, entries)
        arrayAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
        adapter = arrayAdapter
    }
}

@BindingAdapter("onItemSelected")
fun Spinner.setItemSelectedListener(listener: ItemSelectedListener?) {
    if (listener == null) {
        onItemSelectedListener = null
    } else {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (tag != position) {
                    listener.onItemSelected(parent.getItemAtPosition(position))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}

@BindingAdapter("srcUri")
fun ImageView.setSrcURI(uri: Uri) {
    CoroutineScope(Dispatchers.IO).launch {
        BitmapFactory.decodeFile(uri.path).also { bitmap ->
            Log.e("BindingAdatper", "setSrcURI bmpsize: ${bitmap.width} ${bitmap.height}")
            withContext(Dispatchers.Main) {
                setImageBitmap(bitmap)
            }
        }
    }
}

interface ItemSelectedListener {
    fun onItemSelected(item: Any)
}
