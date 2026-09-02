package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.data.TripComputer
import com.example.data.readUtf8TextLimited
import com.example.model.SavedTrip
import com.example.model.TripData
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], qualifiers = "w1024dp-h600dp-land-mdpi")
class LongUseStabilityTest {
    @Test
    fun `application offroad tracker survives activity recreation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE).edit().clear().commit()
        val manager = (context.applicationContext as CarLauncherApp).offroadTrackManager
        val trackFile = File(context.filesDir, "offroad_track_rolling.json")
        manager.clearTrack()
        awaitCondition { trackFile.readJsonLengthOrMinusOne() == 0 }
        val imported = manager.importBackupJson(
            JSONObject().put("track", JSONArray().put(
                JSONObject().put("lat", 24.7136).put("lon", 46.6753).put("time", 1L)
            )).toString()
        )
        assertEquals(1, imported)
        awaitCondition { trackFile.readJsonLengthOrMinusOne() == 1 }

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertFalse(controller.get().isFinishing)
        controller.pause().stop().destroy()

        manager.clearTrack()
        awaitCondition { trackFile.readJsonLengthOrMinusOne() == 0 }
    }

    @Test
    fun `saved trip routes stay lazy until opened`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = PreferencesManager(context)
        preferences.saveTripData(TripData())
        val trip = SavedTrip(
            id = "long-use-route", name = "اختبار طويل", startTimeStamp = 1L, endTimeStamp = 2L,
            distanceKm = 120f, movingTimeSec = 3600L, stopTimeSec = 0L,
            maxSpeedKmH = 80f, averageSpeedKmH = 45f,
            startLatitude = 24.0, startLongitude = 46.0,
            endLatitude = 25.0, endLongitude = 47.0
        )
        preferences.saveSavedTrips(listOf(trip))
        val route = JSONArray()
        repeat(2500) { index ->
            route.put(JSONObject()
                .put("lat", 24.0 + index / 100_000.0)
                .put("lon", 46.0 + index / 100_000.0)
                .put("t", index.toLong()))
        }
        context.getSharedPreferences("launcher_trip_routes_2026", Context.MODE_PRIVATE)
            .edit().putString("route_${trip.id}", route.toString()).commit()

        val computer = TripComputer(preferences)
        assertEquals(1, computer.history.value.size)
        assertTrue(computer.history.value.single().route.isEmpty())
        assertTrue(computer.hasSavedTripRoute(trip.id))
        val loaded = computer.loadSavedTripWithRoute(trip.id)
        assertNotNull(loaded)
        assertEquals(2500, loaded!!.route.size)

        computer.release()
        preferences.saveSavedTrips(emptyList())
        context.getSharedPreferences("launcher_trip_routes_2026", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `file picker rejects text above memory budget`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "oversized-import.json").apply { writeText("x".repeat(4096)) }
        val failure = runCatching {
            context.contentResolver.readUtf8TextLimited(Uri.fromFile(file), maxBytes = 1024L)
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        file.delete()
    }

    private fun awaitCondition(timeoutMs: Long = 3_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(25L)
        }
        assertTrue("Timed out waiting for persistence", condition())
    }

    private fun File.readJsonLengthOrMinusOne(): Int = runCatching {
        if (!exists()) -1 else JSONArray(readText()).length()
    }.getOrDefault(-1)
}
