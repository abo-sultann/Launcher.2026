package com.example.core.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DarbakModuleCatalogTest {
    @Test
    fun privateSystemCatalog_hasUniqueIdsAndPackages() {
        val modules = DarbakModuleCatalog.systemModules("com.aistudio.carlauncher.lzrk26")

        assertEquals(modules.size, modules.map { it.id }.distinct().size)
        assertEquals(modules.size, modules.map { it.packageName }.distinct().size)
        assertTrue(modules.first().id == DarbakModuleId.LAUNCHER)
    }

    @Test
    fun activeCatalog_containsOnlyCoreCarModules() {
        val modules = DarbakModuleCatalog.systemModules("com.aistudio.carlauncher.lzrk26")
        val ids = modules.map { it.id }.toSet()

        assertEquals(
            setOf(
                DarbakModuleId.LAUNCHER,
                DarbakModuleId.VEHICLE_HUB,
                DarbakModuleId.MAINTENANCE,
                DarbakModuleId.MEDIA,
            ),
            ids,
        )
    }

    @Test
    fun mapsKidsQuranAndAdhkar_areOutsideLauncherSystem() {
        val modules = DarbakModuleCatalog.systemModules("com.aistudio.carlauncher.lzrk26")
        val packages = modules.map { it.packageName.lowercase() }

        assertFalse(packages.any { it.contains("darbakmaps") })
        assertFalse(packages.any { it.contains("darbakkidstv") })
        assertFalse(packages.any { it.contains("laqqinni") })
        assertFalse(packages.any { it.contains("darbakadhkar") })
    }
}
