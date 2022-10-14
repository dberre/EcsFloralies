package com.bdomperso.ecsfloralies

import android.R
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.databinding.BindingAdapter

@BindingAdapter("entries")
fun Spinner.setSpinnerEntries(entries: List<Any>?) {
    if (entries != null) {
        val arrayAdapter = ArrayAdapter(context, R.layout.simple_spinner_item, entries)
        arrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
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

interface ItemSelectedListener {
    fun onItemSelected(item: Any)
}
