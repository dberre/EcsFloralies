package com.bdomperso.ecsfloralies

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpResponse
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*

class GoogleDriveServices(context: Context, googleAccount: GoogleSignInAccount) {

    private var service: Drive? = null

    private val TAG = "GoogleDriveServices"

    /**
         * Initialize initials attributes.
         *
         * @param .
         */
        init {
            val credential = GoogleAccountCredential.usingOAuth2(context, setOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = googleAccount.account

            service = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(java.lang.String.valueOf(R.string.app_name))
                .build()

            if (service != null) {
                Log.i(TAG, "drive created")
            } else {
                Log.e(TAG, "failed to create a drive. check connection state")
            }
        }

        /**
         * Upload a text file.
         *
         * @param String path of the file.
         * @return String name of the file in google drive.
         */
        @Throws(IOException::class)
        fun uploadTextFile(filePath: String, title: String): String {
            val body = com.google.api.services.drive.model.File()
            body.name = title
            body.description = "A test document"
            body.mimeType = "text/plain"
            val fileContent = File(filePath)
            val mediaContent = FileContent("text/plain", fileContent)
            val file = service!!.files().create(body, mediaContent).execute()
            return file.id
        }


        @Throws(IOException::class)
        fun uploadImageFile(filePath: String, title: String, parent: String) {

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val parentFolder = searchFolders(parent).first()

                    val body = com.google.api.services.drive.model.File()
                    body.name = title
                    body.parents = arrayListOf(parentFolder.id.toString())
                    body.description = "ECS 2022 image"
                    body.mimeType = "image/jpg"
                    val fileContent = File(filePath)
                    val mediaContent = FileContent("image/jpg", fileContent)

                    val file = service!!.files().create(body, mediaContent).execute()
                    Log.i(TAG, "remote file name: ${file.id}")
                } catch (ex: ApiException) {
                    Log.e(TAG, "uploadImageFile: ${ex.message}")
                    throw ex
                } catch (ex: NoSuchElementException) {
                    Log.e(TAG, "UploadImageFile: remote folder $parent ${ex.message}")
                    throw ex
                } catch (ex: IOException) {
                    Log.e(TAG, "UploadImageFile: ${ex.message}")
                    throw ex
                } catch (ex: Exception) {
                    Log.e(TAG, "uploadImageFile: ${ex.message}")
                    throw ex
                }
            }
        }

        /**
         * Get the content of a file.
         *
         * @param File to get the content.
         * @return String content of the file.
         */
        @Throws(IOException::class)
        fun downloadTextFile(file: com.google.api.services.drive.model.File): String {
            val url = GenericUrl(file.webViewLink)
            val response: HttpResponse = service!!.requestFactory.buildGetRequest(url).execute()
            return try {
                Scanner(response.content).useDelimiter("\\A").next()
            } catch (e: NoSuchElementException) {
                ""
            }
        }

        /**
         * Get the content of a file.
         *
         * @param String the file ID.
         * @return String content of the file.
         */
        @Throws(IOException::class)
        fun downloadTextFile(fileID: String?): String {
            val file = service!!.files().get(fileID).execute()
            return downloadTextFile(file)
        }

        /**
         * Retrieve a list of File resources.
         *
         * @param service Drive API service instance.
         * @return List of File resources.
         * @author Google
         * @throws IOException
         */
        @Throws(IOException::class)
        fun retrieveAllFiles(): List<com.google.api.services.drive.model.File> {
            val result = ArrayList<com.google.api.services.drive.model.File>()
            CoroutineScope(Dispatchers.IO).launch {
                var request = service!!.files().list()
                do {
                    try {
                        val files = request.execute()
                        result.addAll(files.files)
                        Log.i(TAG, "retrieveAllFiles: ${files.count()}")
                        request.pageToken = files.nextPageToken
                    } catch (e: IOException) {
                        request.pageToken = null
                    }
                } while (request.pageToken != null && request.pageToken.isNotEmpty())
            }
            return result
        }

    @Throws(IOException::class)
    suspend fun searchFolders(name: String): List<com.google.api.services.drive.model.File> {

        val value = CoroutineScope(Dispatchers.IO).async {
            val result = ArrayList<com.google.api.services.drive.model.File>()
            var request = service!!.files().list()
            do {
                try {
                    val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$name'"
                    val files = request
                        .setQ(query)
                        .execute()
                    result.addAll(files.files)
                    request.pageToken = files.nextPageToken
                } catch (e: IOException) {
                    Log.e(TAG, "searchFolders: ${e.message}")
                    throw e
                }
            } while (request.pageToken != null && request.pageToken.isNotEmpty())
            result
        }
        return value.await()
    }

    @Throws(IOException::class)
    suspend fun searchImage(name: String): List<com.google.api.services.drive.model.File> {

        val value = CoroutineScope(Dispatchers.IO).async {
            val result = ArrayList<com.google.api.services.drive.model.File>()
            var request = service!!.files().list()
            do {
                try {
                    val query = "mimeType contains 'image/' and name = '$name'"
                    val files = request
                        .setQ(query)
                        .execute()
                    result.addAll(files.files)
                    request.pageToken = files.nextPageToken
                } catch (e: IOException) {
                    Log.e(TAG, "searchImage: ${e.message} $e.")
                    throw e
                }
            } while (request.pageToken != null && request.pageToken.isNotEmpty())
            result
        }
        return value.await()
    }

    // TODO specify the folder for securing the delete ?.
    @Throws(IOException::class)
    suspend fun deleteImage(fileName: String) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            val files = searchImage(fileName)
            files.forEach {
                service!!.files().delete(it.id).execute()
                Log.i(TAG, "image ${it.name} id:${it.id} deleted")
            }
        }
        job.join()
    }
}