package com.example.core.bridge

/**
 * Logical Darbak modules known to Darbak Launcher.
 *
 * Darbak Maps is intentionally not part of this list. It is a separate public product and must
 * never become a hard dependency of the private in-car system.
 *
 * Legacy identifiers for apps that were previously integrated remain in the enum for source and
 * backup compatibility, but the active catalog decides what Launcher actually manages.
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
    /** Optional companions never make Darbak System unhealthy when missing. */
    val requiredForSystemHealth: Boolean = true,
)

data class DarbakModuleState(
    val spec: DarbakModuleSpec,
    val installed: Boolean,
    val enabled: Boolean,
    val launchable: Boolean,
    val versionName: String? = null,
    val versionCode: Long? = null,
)
