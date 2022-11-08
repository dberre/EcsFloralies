package com.bdomperso.ecsfloralies

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.*
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
    CoroutineScope(Dispatchers.IO).launch {
        try {
            BitmapFactory.decodeFile(uri.path).also { bitmap ->
                val resizedBitmap = getResizedBitmap(bitmap, 960)
                Log.i("BindingAdapter", "setSrcURI resized ${uri.path} size: ${resizedBitmap.width} ${resizedBitmap.height}")
                withContext(Dispatchers.Main) {
                    val matrix = Matrix()
                    matrix.postRotate(if (resizedBitmap.height > resizedBitmap.width)  90f else 0f)
                    setImageBitmap(Bitmap.createBitmap(resizedBitmap,
                        0, 0,
                        resizedBitmap.width, resizedBitmap.height, matrix, true))
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

/**
 * Simulate a button click, including a small delay while it is being pressed to trigger the
 * appropriate animations.
 */
fun ImageButton.simulateClick(delay: Long = ANIMATION_FAST_MILLIS) {
    performClick()
    isPressed = true
    invalidate()
    postDelayed({
        invalidate()
        isPressed = false
    }, delay)
}


fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 1) {
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, width, height,true)
}