package com.example.core.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Short-lived status client used only while Darbak System UI is visible.
 *
 * Requests are explicit to each companion package. Responses must echo a live request id and the
 * expected module id before they are accepted, which prevents stale/mismatched companion data from
 * appearing in the launcher.
 */
class DarbakStatusClient(context: Context) {
    private val appContext = context.applicationContext
    private val pendingRequests = ConcurrentHashMap<String, DarbakModuleId>()
    private val _snapshots = MutableStateFlow<Map<DarbakModuleId, DarbakModuleSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<DarbakModuleId, DarbakModuleSnapshot>> = _snapshots.asStateFlow()

    @Volatile private var started = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DarbakSystemProtocol.ACTION_STATUS_RESPONSE) return
            if (intent.getIntExtra(DarbakSystemProtocol.EXTRA_SCHEMA_VERSION, -1) != DarbakSystemProtocol.SCHEMA_VERSION) return

            val requestId = intent.getStringExtra(DarbakSystemProtocol.EXTRA_REQUEST_ID) ?: return
            val expectedModule = pendingRequests.remove(requestId) ?: return
            val moduleId = runCatching {
                DarbakModuleId.valueOf(intent.getStringExtra(DarbakSystemProtocol.EXTRA_MODULE_ID).orEmpty())
            }.getOrNull() ?: return
            if (moduleId != expectedModule) return

            val snapshot = DarbakModuleSnapshot(
                moduleId = moduleId,
                health = intent.getStringExtra(DarbakSystemProtocol.EXTRA_HEALTH)
                    ?: DarbakSystemProtocol.HEALTH_UNAVAILABLE,
                primaryText = intent.getStringExtra(DarbakSystemProtocol.EXTRA_PRIMARY_TEXT).orEmpty(),
                secondaryText = intent.getStringExtra(DarbakSystemProtocol.EXTRA_SECONDARY_TEXT).orEmpty(),
                metricValue = intent.getStringExtra(DarbakSystemProtocol.EXTRA_METRIC_VALUE).orEmpty(),
                metricUnit = intent.getStringExtra(DarbakSystemProtocol.EXTRA_METRIC_UNIT).orEmpty(),
                timestampMs = intent.getLongExtra(DarbakSystemProtocol.EXTRA_TIMESTAMP_MS, System.currentTimeMillis()),
            )
            _snapshots.value = _snapshots.value + (moduleId to snapshot)
        }
    }

    fun start() {
        if (started) return
        val filter = IntentFilter(DarbakSystemProtocol.ACTION_STATUS_RESPONSE)
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
        started = true
    }

    fun requestAll(modules: List<DarbakModuleState>) {
        modules.asSequence()
            .filter { it.spec.id != DarbakModuleId.LAUNCHER && it.installed && it.enabled }
            .forEach(::requestStatus)
    }

    fun requestStatus(module: DarbakModuleState) {
        if (!module.installed || !module.enabled || module.spec.id == DarbakModuleId.LAUNCHER) return
        val requestId = UUID.randomUUID().toString()
        pendingRequests[requestId] = module.spec.id
        val request = Intent(DarbakSystemProtocol.ACTION_STATUS_REQUEST).apply {
            setPackage(module.spec.packageName)
            putExtra(DarbakSystemProtocol.EXTRA_SCHEMA_VERSION, DarbakSystemProtocol.SCHEMA_VERSION)
            putExtra(DarbakSystemProtocol.EXTRA_REQUEST_ID, requestId)
            putExtra(DarbakSystemProtocol.EXTRA_MODULE_ID, module.spec.id.name)
        }
        appContext.sendBroadcast(request)
    }

    fun stop() {
        if (!started) return
        runCatching { appContext.unregisterReceiver(receiver) }
        pendingRequests.clear()
        started = false
    }
}
