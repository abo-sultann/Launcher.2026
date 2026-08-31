package com.example

import com.example.model.WidgetStyle
import com.example.model.WidgetTone
import com.example.model.WidgetType
import com.example.model.modernWidgetStyle
import com.example.model.preferredWidgetStylesFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelsTest {

    @Test
    fun `rebuilt library exposes three distinct styles for every widget type`() {
        WidgetType.values().forEach { type ->
            val styles = preferredWidgetStylesFor(type)
            assertEquals(type.name, 3, styles.size)
            assertEquals(type.name, 3, styles.distinct().size)
            assertTrue(type.name, styles.all { it.type == type })
        }
    }

    @Test
    fun `every legacy style migrates into its rebuilt library`() {
        WidgetStyle.values().forEach { legacy ->
            val migrated = modernWidgetStyle(legacy)
            assertEquals(legacy.type, migrated.type)
            assertTrue(migrated in preferredWidgetStylesFor(legacy.type))
        }
    }

    @Test
    fun `custom colours collapse to white while black stays black`() {
        assertEquals(WidgetTone.BLACK, WidgetTone.fromArgb(WidgetTone.BLACK.argb))
        assertEquals(WidgetTone.WHITE, WidgetTone.fromArgb(WidgetTone.WHITE.argb))
        assertEquals(WidgetTone.WHITE, WidgetTone.fromArgb(0xFF00FFFF.toInt()))
        assertEquals(WidgetTone.WHITE, WidgetTone.fromArgb(null))
    }
}
