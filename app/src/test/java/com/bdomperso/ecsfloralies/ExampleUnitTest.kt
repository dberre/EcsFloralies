package com.bdomperso.ecsfloralies

import org.junit.Test

import org.junit.Assert.*
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun filename_regex() {
        parseFilename(File("A_ET4_F2_FACE_ASC_SDB.jpg"))
        parseFilename(File("C_ET1_F2_MUR_ASC_CUI.jpg"))
    }

    private fun parseFilename(file: File) {
//        val pattern = "([ABC])_ET([01234])_(.*)_(.*)"
        val pattern = "([ABC])_ET([01234])_(.*)_(.*)\\."
        val matches = Regex(pattern).find(file.name)
        if (matches != null) {
            if (matches!!.groups.isNotEmpty()) {
                println("count: ${matches.groups.count()}")
                for(group in matches!!.groups) {
                    println(group)
                }
            } else {
                println("no matches")
            }
        } else {
            println("pattern no found")
        }
    }
}