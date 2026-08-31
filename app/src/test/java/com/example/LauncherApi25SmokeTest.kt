package com.example

import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.example.data.RecommendedMapStatus
import com.example.ui.components.CarScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25], qualifiers = "w1024dp-h600dp-land-mdpi")
class LauncherApi25SmokeTest {

    @Test
    fun `launcher reaches home on Android 7 without safe mode`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(900, TimeUnit.MILLISECONDS)

        assertFalse(activity.isFinishing)
        val viewModel = ViewModelProvider(activity)[com.example.ui.viewmodel.MainViewModel::class.java]
        assertEquals(CarScreen.HOME, viewModel.currentScreen.value)
        assertEquals(RecommendedMapStatus.IDLE, viewModel.recommendedMapDownloadState.value.status)

        controller.pause().stop().destroy()
    }
}
