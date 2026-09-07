package com.example

import com.example.data.MaintenanceMileageBridge
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceMileageBridgeTest {
    @Test
    fun acceptsNormalDrivingGpsSample() {
        assertTrue(MaintenanceMileageBridge.shouldCountSample(60f, 1f, 8f))
    }

    @Test
    fun rejectsStationaryOrWeakGpsSamples() {
        assertFalse(MaintenanceMileageBridge.shouldCountSample(0f, 1f, 8f))
        assertFalse(MaintenanceMileageBridge.shouldCountSample(60f, 1f, 80f))
        assertFalse(MaintenanceMileageBridge.shouldCountSample(60f, 10f, 8f))
    }
}
