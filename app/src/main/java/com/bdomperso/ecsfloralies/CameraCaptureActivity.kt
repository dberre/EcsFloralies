package com.bdomperso.ecsfloralies

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import java.io.File

class CameraCaptureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_capture)



        findViewById<Button>(R.id.buttonCloseCameraCapture).setOnClickListener {
            saveCapture()
        }
    }

    private fun saveCapture() {
        val file = File("")
        val intent = Intent(this, SaveCaptureActivity::class.java)
        intent.putExtra("image_path", file.absolutePath)
        this.startActivity(intent)
    }
}