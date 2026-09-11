package com.example.core.bridge

/**
 * Logical Darbak modules managed by Darbak Launcher.
 *
 * Darbak Maps is intentionally not part of this list. It is a separate public product and must
 * never become a hard dependency of the private in-car system.
 */
enum class DarbakModuleId {
    LAUNCHER,
    VEHICLE_HUB,
    MAINTENANCE,
    MEDIA,
    KIDS_TV,
    LAQQINNI,
    ADHKAR,
}

data class DarbakModuleSpec(
    val id: DarbakModuleId,
    val displayName: String,
    val packageName: String,
    val priority: Int,
    val showOnHome: Boolean = true,
)

data class DarbakModuleState(
    val spec: DarbakModuleSpec,
    val installed: Boolean,
    val enabled: Boolean,
    val launchable: Boolean,
    val versionName: String? = null,
    val versionCode: Long? = null,
)
