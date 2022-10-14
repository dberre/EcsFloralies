package com.bdomperso.ecsfloralies

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

// adb shell to browse the device files

@RequiresApi(Build.VERSION_CODES.Q)
class FolderObserver(context: Context, pathToWatch: File) : FileObserver(pathToWatch, mask) {

    private var mainThreadHandler: Handler

    private var context: Context
    private var pathToWatch: File

    companion object {
        const val TAG = "FolderObserver"
        const val mask = CREATE
    }

    init {
        this.context = context
        this.pathToWatch = pathToWatch
        mainThreadHandler = Handler(Looper.getMainLooper())
        Log.i(TAG, "-> FolderObserver: ${pathToWatch.absolutePath}")
    }

    override fun onEvent(event: Int, filename: String?) {
        Log.i(TAG, "-> event=$event")
        // .probe condition is there to filter event when camera is launched
        if (event == CREATE && !filename.equals(".probe")) {
            val file = File(pathToWatch, filename!!)
            Log.i("TAG", "new file detected: ${file.absolutePath}")

            mainThreadHandler.post(Runnable() {
                val intent = Intent(context, SaveCaptureActivity::class.java)
                intent.putExtra("image_path", file.absolutePath)
                context.startActivity(intent)
            })
        }
    }
}

class FolderObserverLegacy(context: Context, pathToWatch: File) : FileObserver(pathToWatch.absolutePath, mask) {

    private var mainThreadHandler: Handler

    private var context: Context
    private var pathToWatch: File

    companion object {
        const val TAG = "FolderObserver"
        const val mask = CREATE
    }

    init {
        this.context = context
        this.pathToWatch = pathToWatch
        mainThreadHandler = Handler(Looper.getMainLooper())
        Log.i(TAG, "-> FolderObserver: ${pathToWatch.absolutePath}")
    }

    override fun onEvent(event: Int, filename: String?) {
        Log.i(TAG, "-> event=$event")
        // .probe condition is there to filter event when camera is launched
        if (event == CREATE && !filename.equals(".probe")) {
            val file = File(pathToWatch, filename!!)
            Log.i("TAG", "new file detected: ${file.absolutePath}")

            mainThreadHandler.post(Runnable() {
                val intent = Intent(context, SaveCaptureActivity::class.java)
                intent.putExtra("image_path", file.absolutePath)
                context.startActivity(intent)
            })
        }
    }
}


