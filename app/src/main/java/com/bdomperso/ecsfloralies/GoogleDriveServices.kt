package com.bdomperso.ecsfloralies

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
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
            NetHttpTransport(),
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

    @Throws(IOException::class, ApiException::class)
    fun uploadImageFile(filePath: String, title: String, description: String, parent: String? = null) {
        try {
            val body = com.google.api.services.drive.model.File()
            body.name = title
            if (parent != null) {
                body.parents = arrayListOf(safelyGetParentFolder(parent).id.toString())
            }
            body.description = description
            body.mimeType = "image/jpg"
            val fileContent = File(filePath)
            val mediaContent = FileContent("image/jpg", fileContent)

            val file = service!!.files().create(body, mediaContent).execute()
            Log.i(TAG, "remote file name: ${file.id}")
        } catch (ex: ApiException) {
            Log.e(TAG, "uploadImageFile: ${ex.message}")
            throw ex
        } catch (ex: IOException) {
            Log.e(TAG, "UploadImageFile: ${ex.message}")
            throw ex
        }
    }

    @Throws(IOException::class, ApiException::class, NoSuchElementException::class)
    fun safelyGetParentFolder(folderName: String): com.google.api.services.drive.model.File {
        return try {
            searchFolders(folderName).first()
        } catch (ex: NoSuchElementException) {
            Log.e(TAG, "safelyGetParentFolder: folder $folderName does not exist, try to create it")
            createFolder(folderName)
        }
    }

    @Throws(IOException::class, ApiException::class)
    fun createFolder(name: String): com.google.api.services.drive.model.File {
        try {
            val body = com.google.api.services.drive.model.File()
            body.name = name
            body.mimeType = "application/vnd.google-apps.folder"
            val folder = service!!.files().create(body).execute()
            Log.i(TAG, "remote folder id: ${folder.id}")
            return folder
        } catch (ex: ApiException) {
            Log.e(TAG, "createFolder: ${ex.message}")
            throw ex
        } catch (ex: IOException) {
            Log.e(TAG, "createFolder: ${ex.message}")
            throw ex
        }
    }

    /**
     * Retrieve a list of File resources.
     *
     * @param
     * @return List of File resources.
     * @author Google
     * @throws IOException
     */
    @Throws(IOException::class)
    fun retrieveAllFiles(): List<com.google.api.services.drive.model.File> {
        val result = ArrayList<com.google.api.services.drive.model.File>()
        val request = service!!.files().list()
        do {
            try {
                val files = request.execute()
                result.addAll(files.files)
                request.pageToken = files.nextPageToken
            } catch (e: IOException) {
                request.pageToken = null
            }
        } while (request.pageToken != null && request.pageToken.isNotEmpty())
        return result
    }

    @Throws(IOException::class)
    fun searchFolders(name: String): List<com.google.api.services.drive.model.File> {
            val result = ArrayList<com.google.api.services.drive.model.File>()
            val request = service!!.files().list()
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
            return result
    }

    @Throws(IOException::class)
    fun searchImages(name: String, parent: String? = null): List<com.google.api.services.drive.model.File> {
            val result = ArrayList<com.google.api.services.drive.model.File>()
            val request = service!!.files().list()
            do {
                try {
                    var query = "mimeType contains 'image/' and name = '$name'"
                    if (parent != null) {
                        val folderId = searchFolders(parent).first().id
                        query += " and '$folderId' in parents"
                    }
                    val files = request
                        .setQ(query)
                        .execute()
                    result.addAll(files.files)
                    request.pageToken = files.nextPageToken
                } catch (ex: IOException) {
                    Log.e(TAG, "searchImages: ${ex.message}")
                    throw ex
                } catch (ex: NoSuchElementException) {
                    Log.e(TAG, "searchImages: folder $parent not found")
                    // no rethrow here, just return the empty result
                    return result
                }
            } while (request.pageToken != null && request.pageToken.isNotEmpty())
            return result
    }

    fun deleteImage(fileName: String, parent: String? = null) {
        try {
            val files = searchImages(fileName, parent)
            files.forEach {
                service!!.files().delete(it.id).execute()
                Log.i(TAG, "image ${it.name} id:${it.id} deleted")
            }
        } catch (ex: IOException) {
            // not critical as most common reason is no communication
            Log.e(TAG, "deleteImage: ${ex.message}")
        }
    }
}