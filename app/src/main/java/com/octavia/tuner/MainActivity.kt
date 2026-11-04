package com.octavia.tuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.octavia.tuner.model.TunerStatus
import com.octavia.tuner.model.TunerViewModel
import com.octavia.tuner.ui.TunerApp
import com.octavia.tuner.ui.TunerTheme

class MainActivity : ComponentActivity() {
    private val vm: TunerViewModel by viewModels()

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.start() else vm.markDenied()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TunerTheme {
                TunerApp(
                    vm = vm,
                    onRetry = { vm.resetStatus(); ensureListening() },
                    onOpenAppSettings = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        )
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureListening()
    }

    override fun onStop() {
        // Release the microphone whenever the app isn't visible.
        vm.stop()
        super.onStop()
    }

    private fun ensureListening() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            granted -> vm.start() // also covers returning from system Settings after enabling it
            vm.status != TunerStatus.Denied && vm.status != TunerStatus.Requesting -> {
                vm.markRequesting()
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
