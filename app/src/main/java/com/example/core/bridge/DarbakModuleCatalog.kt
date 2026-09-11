package com.example.core.bridge

/** Stable registry for private Darbak applications that belong to this head-unit system. */
object DarbakModuleCatalog {
    fun systemModules(launcherPackage: String): List<DarbakModuleSpec> = listOf(
        DarbakModuleSpec(
            id = DarbakModuleId.LAUNCHER,
            displayName = "Darbak Launcher",
            packageName = launcherPackage,
            priority = 0,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.VEHICLE_HUB,
            displayName = "Darbak Vehicle Hub",
            packageName = "com.abosultan.darbakvehiclehub",
            priority = 10,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.MAINTENANCE,
            displayName = "Darbak Maintenance",
            packageName = "com.abosultan.darbakmaintenance",
            priority = 20,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.MEDIA,
            displayName = "Darbak Media",
            packageName = "com.abosultan.darbakmedia",
            priority = 30,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.KIDS_TV,
            displayName = "Darbak Kids TV",
            packageName = "com.abosultan.darbakkidstv",
            priority = 40,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.LAQQINNI,
            displayName = "لقّني",
            packageName = "com.abosultan.laqqinni",
            priority = 50,
        ),
        DarbakModuleSpec(
            id = DarbakModuleId.ADHKAR,
            displayName = "Darbak Adhkar",
            packageName = "com.abosultan.darbakadhkar",
            priority = 60,
        ),
    )
}
