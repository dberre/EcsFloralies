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
        parseFilenameAndTestSubparts()
    }

    private fun parseFilename(file: File) {
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

    private fun parseFilenameAndTestSubparts() {
        var pattern = "([ABC])_ET([01234])_(.+_ASC)_(.+)\\."
        var matches = Regex(pattern).find("A_ET0_F2_FACE_ASC_VMC_CUI.jpg")
        assert(matches != null)
        assert(matches!!.groups.isNotEmpty())
        assert(matches.groups.count() == 5)
        assert(matches!!.groups[1]!!.value == "A")
        assert(matches!!.groups[2]!!.value == "0")
        assert(matches!!.groups[3]!!.value == "F2_FACE_ASC")
        assert(matches!!.groups[4]!!.value == "VMC_CUI")

        pattern = "([ABC])_ET([01234])_(.+_ASC)_(.+)\\."
        matches = Regex(pattern).find("A_ET0_F2_FACE_ASC_SDB.jpg")
        assert(matches != null)
        assert(matches!!.groups.isNotEmpty())
        assert(matches.groups.count() == 5)
        assert(matches!!.groups[1]!!.value == "A")
        assert(matches!!.groups[2]!!.value == "0")
        assert(matches!!.groups[3]!!.value == "F2_FACE_ASC")
        assert(matches!!.groups[4]!!.value == "SDB")
    }
}