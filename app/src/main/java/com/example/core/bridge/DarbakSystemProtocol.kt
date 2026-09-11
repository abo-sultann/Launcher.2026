package com.example.core.bridge

/**
 * Versioned broadcast contract shared by the private Darbak head-unit applications.
 *
 * The protocol intentionally uses primitive Intent extras only so it stays cheap and compatible
 * with Android 7.1/API 25. Requests are always package-targeted; Darbak Maps is not a participant.
 */
object DarbakSystemProtocol {
    const val SCHEMA_VERSION = 1

    const val ACTION_STATUS_REQUEST = "com.abosultan.darbak.system.STATUS_REQUEST"
    const val ACTION_STATUS_RESPONSE = "com.abosultan.darbak.system.STATUS_RESPONSE"
    const val ACTION_COMMAND = "com.abosultan.darbak.system.COMMAND"

    const val EXTRA_SCHEMA_VERSION = "darbak_schema_version"
    const val EXTRA_REQUEST_ID = "darbak_request_id"
    const val EXTRA_MODULE_ID = "darbak_module_id"
    const val EXTRA_HEALTH = "darbak_health"
    const val EXTRA_PRIMARY_TEXT = "darbak_primary_text"
    const val EXTRA_SECONDARY_TEXT = "darbak_secondary_text"
    const val EXTRA_METRIC_VALUE = "darbak_metric_value"
    const val EXTRA_METRIC_UNIT = "darbak_metric_unit"
    const val EXTRA_TIMESTAMP_MS = "darbak_timestamp_ms"
    const val EXTRA_COMMAND = "darbak_command"

    const val HEALTH_READY = "ready"
    const val HEALTH_DEGRADED = "degraded"
    const val HEALTH_UNAVAILABLE = "unavailable"

    const val COMMAND_REFRESH = "refresh"
    const val COMMAND_PLAY_PAUSE = "play_pause"
    const val COMMAND_NEXT = "next"
    const val COMMAND_PREVIOUS = "previous"

    const val LAUNCHER_PACKAGE = "com.aistudio.carlauncher.lzrk26"
}

data class DarbakModuleSnapshot(
    val moduleId: DarbakModuleId,
    val health: String,
    val primaryText: String = "",
    val secondaryText: String = "",
    val metricValue: String = "",
    val metricUnit: String = "",
    val timestampMs: Long = 0L,
)
