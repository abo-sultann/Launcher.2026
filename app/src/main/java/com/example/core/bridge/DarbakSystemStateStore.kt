package com.example.core.bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory snapshot of the private Darbak apps installed on this head unit.
 *
 * UI code observes this store instead of querying PackageManager directly. Refreshing is cheap and
 * safe, and a missing companion app is represented as state rather than an exception.
 */
class DarbakSystemStateStore(
    private val bridge: DarbakAppBridge,
) {
    private val _modules = MutableStateFlow(bridge.refresh())
    val modules: StateFlow<List<DarbakModuleState>> = _modules.asStateFlow()

    fun refresh(): List<DarbakModuleState> {
        val snapshot = bridge.refresh()
        _modules.value = snapshot
        return snapshot
    }

    fun launch(id: DarbakModuleId): Boolean {
        val launched = bridge.launch(id)
        refresh()
        return launched
    }

    fun module(id: DarbakModuleId): DarbakModuleState? =
        _modules.value.firstOrNull { it.spec.id == id }
}
