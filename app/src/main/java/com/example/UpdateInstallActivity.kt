package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity

class UpdateInstallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as? CarLauncherApp)?.updateManager?.installDownloadedUpdate()
        finish()
    }
}
