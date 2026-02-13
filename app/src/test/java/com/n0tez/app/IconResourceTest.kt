package com.n0tez.app

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class IconResourceTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    private val resDir = if (File(projectRoot, "app").exists()) {
        File(projectRoot, "app/src/main/res")
    } else {
        File(projectRoot, "src/main/res")
    }

    private fun getPngDimensions(file: File): Pair<Int, Int>? {
        if (!file.exists()) return null
        try {
            val bytes = file.inputStream().use { it.readBytes() }
            if (bytes.size < 24) return null
            
            // Check PNG signature
            val pngSignature = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
            for (i in pngSignature.indices) {
                if (bytes[i] != pngSignature[i]) return null
            }
            
            // Width: bytes 16-19 (Big Endian)
            val width = ((bytes[16].toInt() and 0xFF) shl 24) or
                        ((bytes[17].toInt() and 0xFF) shl 16) or
                        ((bytes[18].toInt() and 0xFF) shl 8) or
                        (bytes[19].toInt() and 0xFF)
            
            // Height: bytes 20-23 (Big Endian)
            val height = ((bytes[20].toInt() and 0xFF) shl 24) or
                         ((bytes[21].toInt() and 0xFF) shl 16) or
                         ((bytes[22].toInt() and 0xFF) shl 8) or
                         (bytes[23].toInt() and 0xFF)
                         
            return Pair(width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @Test
    fun testBubbleIconExistenceAndDimensions() {
        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        densities.forEach { (density, size) ->
            val file = File(resDir, "drawable-$density/ic_floating_bubble.png")
            assertTrue("File should exist: ${file.path}", file.exists())
            
            val dims = getPngDimensions(file)
            assertNotNull("Should be a valid PNG", dims)
            assertEquals("Width should match for $density", size, dims?.first)
            assertEquals("Height should match for $density", size, dims?.second)
        }
    }

    @Test
    fun testLauncherIconExistenceAndDimensions() {
        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        densities.forEach { (density, size) ->
            val file = File(resDir, "mipmap-$density/ic_launcher.png")
            assertTrue("File should exist: ${file.path}", file.exists())
            
            val dims = getPngDimensions(file)
            assertNotNull("Should be a valid PNG", dims)
            assertEquals("Width should match for $density", size, dims?.first)
            assertEquals("Height should match for $density", size, dims?.second)
        }
    }
    
    @Test
    fun testLargeBubbleIcon() {
        val file = File(resDir, "drawable-nodpi/ic_floating_bubble_large.png")
        assertTrue("File should exist: ${file.path}", file.exists())
        
        val dims = getPngDimensions(file)
        assertNotNull("Should be a valid PNG", dims)
        assertEquals("Width should be 512", 512, dims?.first)
        assertEquals("Height should be 512", 512, dims?.second)
    }

    @Test
    fun testMemoryLoadCheck() {
        // Verify that files are readable (simulated memory check)
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        var count = 0
        
        densities.forEach { density ->
            val bubbleFile = File(resDir, "drawable-$density/ic_floating_bubble.png")
            if (bubbleFile.exists()) {
                 val bytes = bubbleFile.readBytes()
                 assertTrue(bytes.isNotEmpty())
                 count++
            }
            
            val launcherFile = File(resDir, "mipmap-$density/ic_launcher.png")
            if (launcherFile.exists()) {
                val bytes = launcherFile.readBytes()
                assertTrue(bytes.isNotEmpty())
                count++
            }
        }
        assertTrue("Should verify files", count > 0)
    }
}
