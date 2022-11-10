package com.bdomperso.ecsfloralies

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TestGoogleDriveServices {
    private val TAG = "TestGoogleDriveServices"

    private fun getServices(): GoogleDriveServices {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bdomperso.ecsfloralies", appContext.packageName)

        val account = GoogleSignIn.getLastSignedInAccount(appContext)
        assert(account != null)

        assert(GoogleSignIn.hasPermissions(account, Scope(Scopes.DRIVE_FILE)))

        return GoogleDriveServices(appContext, account!!)
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bdomperso.ecsfloralies", appContext.packageName)
    }

    @Test
    fun listFiles() {
        val gd = getServices()
        val lst = gd.retrieveAllFiles()
        assert(lst.isNotEmpty())
        lst.forEach { Log.i(TAG, "listFiles: ${it.name} ${it.id}") }
        assert(lst.count { it.name == "ECS_2022" } > 0)
    }

    @Test
    fun searchImages(){
        val gd = getServices()
        val lst = gd.searchImages("A_ET0_F2_CUI.jpg", "ECS_2024")
        assert(lst.isNotEmpty())
        lst.forEach { Log.i(TAG, "searchImage: ${it.name} ${it.id}") }
    }

    @Test
    fun createFiles() {
        val gd = getServices()

        // ensure the file to create don't yet exit
        gd.deleteImage("ImageForTests1.jpg")
        gd.deleteImage("ImageForTests2.jpg", "ECS_202X")

        val filePath = "/storage/emulated/0/Android/media/com.bdomperso.ecsfloralies/EcsFloralies/A_ET0_F2_CUI.jpg"
        gd.uploadImageFile(filePath, "ImageForTests1.jpg", "For tests")
        gd.uploadImageFile(filePath, "ImageForTests2.jpg", "For tests","ECS_202X")

        assert(gd.searchImages("ImageForTests1.jpg").count() == 1)
        assert(gd.searchImages("ImageForTests1.jpg", "ECS_202X").isEmpty())
        assert(gd.searchImages("ImageForTests2.jpg", "ECS_202X-").count() == 1)
        assert(gd.searchImages("ImageForTests2.jpg").isEmpty())
    }

    @Test
    fun createFolder() {
        val gd = getServices()
        try {
            gd.createFolder("ECS_202X")
            assert(true)
        } catch (ex: Exception) {
            assert(false)
        }
    }

//    When using the drive.files scope your app will only be able to access files that you have created
//    with it or that the user is opening from the Drive UI. When the file is shared with another user,
//    he won't be able to see it using the drive.files scope. For this use case you should request access to the full Drive scope.
//    Check the documentation for more details on the available OAuth scopes: https://developers.google.com/drive/scopes
    fun testToCreate() {

    }
}