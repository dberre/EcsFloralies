package com.bdomperso.ecsfloralies

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import java.io.FileNotFoundException


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
    onItemSelectedListener = if (listener == null) {
        null
    } else {
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
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
    // TODO more smart structure ?
    CoroutineScope(Dispatchers.IO).launch {
        try {
            BitmapFactory.decodeFile(uri.path).also { bitmap ->
                Log.e("BindingAdapter", "setSrcURI ${uri.path} size: ${bitmap.width} ${bitmap.height}")
                withContext(Dispatchers.Main) {
                    var matrix = Matrix()
                    matrix.postRotate(if (bitmap.height > bitmap.width)  90f else 0f)
                    setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true))
                }
            }
        } catch (ex: Exception) {
            when(ex) {
                is FileNotFoundException, is NullPointerException -> {
                    BitmapFactory.decodeResource(resources, R.drawable.no_image_available)
                    .also { bitmap ->
                        Log.w("BindingAdapter",
                            "setSrcURI default image size: ${bitmap.width} ${bitmap.height}")
                        withContext(Dispatchers.Main) {
                            setImageBitmap(bitmap)
                        }
                    }
                } else -> throw ex
            }
        }
    }
}

interface ItemSelectedListener {
    fun onItemSelected(item: Any)
}
