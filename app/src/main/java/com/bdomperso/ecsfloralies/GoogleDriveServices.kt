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
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import kotlin.NoSuchElementException

class GoogleDriveServices(context: Context, googleAccount: GoogleSignInAccount) {

    private var service: Drive? = null

    private val TAG = "SaveCaptureActivity"

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
                val parentFolder = searchFolders(parent).first()

                val body = com.google.api.services.drive.model.File()
                body.name = title
                body.parents = arrayListOf(parentFolder.id.toString())
                body.description = "ECS 2022 image"
                body.mimeType = "image/jpg"
                val fileContent = File(filePath)
                val mediaContent = FileContent("image/jpg", fileContent)
                try {
                    val file = service!!.files().create(body, mediaContent).execute()
                    println("remote file name: ${file.id}")
                } catch (ex: ApiException) {
                    println("uploadImageFile: ApiException ${ex.message}")
                } catch (ex: NoSuchElementException) {
                    println("UploadImageFile: remote folder $parent ${ex.message}")
                } catch (ex: IOException) {
                    println("UploadImageFile: I/O error: ${ex.message}")
                } catch (ex: Exception) {
                    println("uploadImageFile: Exception ${ex.message}")
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
            //var request: Files.List? = null
            CoroutineScope(Dispatchers.IO).launch {
                var request = service!!.files().list()
                do {
                    try {
                        val files = request.execute()
                        result.addAll(files.files)
                        println("retrieveAllFiles: ${files.count()}")
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
                    println(query)
                    val files = request
                        .setQ(query)
                        .execute()
                    files.files.forEach { println(it.id) }
                    result.addAll(files.files)
                    request.pageToken = files.nextPageToken
                } catch (e: IOException) {
                    println("IOException ${e.message}")
                    request.pageToken = null
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
                    files.files.forEach { println(it.id) }
                    result.addAll(files.files)
                    request.pageToken = files.nextPageToken
                } catch (e: IOException) {
                    println("IOException ${e.message}")
                    request.pageToken = null
                }
            } while (request.pageToken != null && request.pageToken.isNotEmpty())
            result
        }
        return value.await()
    }

    // TODO specify the folder for securing the delete ?.
    @Throws(IOException::class)
    suspend fun deleteImage(fileName: String) {
        val value = CoroutineScope(Dispatchers.IO).async {
            val files = searchImage(fileName)
            files.forEach {
                service!!.files().delete(it.id).execute()
                Log.i(TAG, "image ${it.name} id:${it.id} deleted")
            }
            0
        }
        value.await()
    }
}