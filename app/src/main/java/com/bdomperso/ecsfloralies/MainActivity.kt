package com.bdomperso.ecsfloralies

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import java.io.File

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"

/** Milliseconds used for UI animations */
const val ANIMATION_FAST_MILLIS = 50L

class MainActivity : AppCompatActivity() {

    companion object {
        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }

//        var logText: String = ""
        var previousImagePath: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        logText += "\nonCreate ${savedInstanceState != null}"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

//        logText += "\nonNewIntent ${intent != null}"
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        // the previousImagePath is used so that the request to display the saveCaptureFragment is
        // sent only when the FolderObserver calls the startActivity when detecting a new image.
        // and not when onResume is called by the rotation or navigation buttons
        // onNewIntent() seems not called on Android7, it is why this processing is useful
        intent.extras?.getString("image_path").let { imagePath ->
//            logText += "\nonResume $imagePath"
            if ((imagePath != null) && (imagePath != previousImagePath)) {
                previousImagePath = imagePath!!
                var args = Bundle()
                args.putString("photoPath", imagePath)
                intent.extras?.putString("image_path", null)
                findNavController(R.id.fragmentContainerView).navigate(
                    R.id.action_global_saveCaptureFragment,
                    args
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
//                logText = ""
                val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
//            KeyEvent.KEYCODE_VOLUME_UP -> {
//                findViewById<TextView>(R.id.debug_text).text = logText
//                true
//            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    private fun breadcrumb() : String {
        return findNavController(R.id.fragmentContainerView).backQueue
            .map {
                it.destination
            }
            .joinToString(" > ") {
                it.displayName.split('/')[1]
            }
    }
}