package com.n0tez.app

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.Robolectric

class OverlayIconPipelineTest {

    @Test
    fun decodeOverlayIcon_scalesTo96() {
        val service = Robolectric.setupService(FloatingWidgetService::class.java)
        val method = service::class.java.getDeclaredMethod("decodeOverlayIcon", Int::class.javaPrimitiveType)
        method.isAccessible = true
        val resId = com.n0tez.app.R.drawable.ic_floating_bubble_original
        val bitmap = method.invoke(service, resId) as Bitmap?
        assertNotNull(bitmap)
        assertEquals(96, bitmap!!.width)
        assertEquals(96, bitmap.height)
    }

    @Test
    fun decodeOverlayIcon_fallbackWorks() {
        val service = Robolectric.setupService(FloatingWidgetService::class.java)
        val method = service::class.java.getDeclaredMethod("decodeOverlayIcon", Int::class.javaPrimitiveType)
        method.isAccessible = true
        val invalid = -1
        val nullBm = method.invoke(service, invalid) as Bitmap?
        assertNull(nullBm)
        val fbRes = com.n0tez.app.R.drawable.ic_floating_bubble_large
        val fbBitmap = method.invoke(service, fbRes) as Bitmap?
        assertNotNull(fbBitmap)
        assertEquals(96, fbBitmap!!.width)
        assertEquals(96, fbBitmap.height)
    }
}
