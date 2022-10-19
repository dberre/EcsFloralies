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


// various way to get a storage directory
// - When requesting the directory to the Environment
//   Environment.getExternalStorageDirectory() -> /storage/emulated/0
//   Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) -> /storage/emulated/0/Pictures
// - When requesting the directory to the Context
//   getExternalFilesDir(Environment.DIRECTORY_PICTURES) -> /storage/emulated/0/Android/data/com.bdomperso.ecsfloralies/files/Pictures

// Le chemin ou Depstech-view place les photos est vue par Android Details comme:
// '/Espace de stockage interne/DCIM/DEPSTECH_View'
// la vue physique dans le device manager Android Studio est:
// '/sdcard/DCIM/DEPSTECH_View'

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
            // only one file is expected, so stop watching now
            stopWatching()
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
            // only one file is expected, so stop watching now
            stopWatching()
        }
    }
}


