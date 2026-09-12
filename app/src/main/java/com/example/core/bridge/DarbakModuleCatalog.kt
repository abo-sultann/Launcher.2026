package com.example.core.bridge

/** Stable registry for the core private Darbak applications managed by this head-unit system. */
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
    )
}
