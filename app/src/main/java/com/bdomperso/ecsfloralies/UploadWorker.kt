package com.bdomperso.ecsfloralies

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.io.IOException


class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val TAG = "UploadWorker"

    private lateinit var gd: GoogleDriveServices

    init {
        // Check if the user is already signed in and all required scopes are granted
        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        if (account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(Scopes.DRIVE_FILE)
            )
        ) {
            gd = GoogleDriveServices(appContext, account)
            Log.i(TAG, "OnStart: logged in")
        } else {
            Log.i(TAG, "OnStart: logged out")
        }
    }

    override fun doWork(): Result {

        val srcFilePath = inputData.getString("SrcFilePath")!!
        val destFilename = inputData.getString("DestFilename")!!
        val description = inputData.getString("Description")!!
        val overwrite = inputData.getBoolean("Overwrite", false)

        return try {
            if (overwrite) {
                gd.deleteImage(destFilename)
            }
            gd.uploadImageFile(srcFilePath, destFilename, "ECS_2022", description)

            Result.success()
        } catch (ex: ApiException) {
            Log.e(TAG, "saveImage: Api error: ${ex.message}")
            Result.failure()
        } catch (ex: IOException) {
            Log.e(TAG, "saveImage: IOException ${ex.message}")
            // most common reason for this error is missing signal which could be temporary
            // Retry later after a delay of 30s (default for WorkRequest)
            Result.retry()
        } catch (ex: Exception) {
            Log.e(TAG, "saveImage: general error: ${ex.message}")
            Result.failure()
        }
    }
}
