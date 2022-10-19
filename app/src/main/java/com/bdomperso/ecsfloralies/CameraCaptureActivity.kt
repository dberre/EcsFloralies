package com.bdomperso.ecsfloralies

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.bdomperso.ecsfloralies.fragments.CameraFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraCaptureActivity : AppCompatActivity() {

    private lateinit var photoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_capture)

        photoFile = File(MainActivity.getOutputDirectory(this), SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg")

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                val bundle = Bundle()
                bundle.putSerializable("photoFile", photoFile)
                setReorderingAllowed(true)
                add<CameraFragment>(R.id.fragmentContainerView, "", bundle)
            }
        }

        findViewById<Button>(R.id.buttonCloseCameraCapture).setOnClickListener {
            saveCapture()
        }
    }

    private fun saveCapture() {
        val intent = Intent(this, SaveCaptureActivity::class.java)
        intent.putExtra("image_path", photoFile.absolutePath)
        this.startActivity(intent)
    }
}