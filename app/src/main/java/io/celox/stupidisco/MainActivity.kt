package io.celox.stupidisco

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import io.celox.stupidisco.data.ApiKeyStore
import io.celox.stupidisco.ui.ApiKeyDialog
import io.celox.stupidisco.ui.AppColors
import io.celox.stupidisco.ui.StupidiscoTheme

class MainActivity : ComponentActivity() {
    private lateinit var apiKeyStore: ApiKeyStore
    private var showApiKeyDialog by mutableStateOf(false)
    private var pendingOverlayPermission = false

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "Mikrofon-Berechtigung wird benötigt", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiKeyStore = ApiKeyStore(this)

        setContent {
            StupidiscoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.Background)
                ) {
                    if (showApiKeyDialog) {
                        ApiKeyDialog(
                            initialDeepgramKey = apiKeyStore.getDeepgramKey(),
                            initialAnthropicKey = apiKeyStore.getAnthropicKey(),
                            onSave = { deepgramKey, anthropicKey ->
                                apiKeyStore.saveKeys(deepgramKey, anthropicKey)
                                showApiKeyDialog = false
                                checkAudioPermission()
                            }
                        )
                    }
                }
            }
        }

        if (!apiKeyStore.hasKeys()) {
            showApiKeyDialog = true
        } else {
            checkAudioPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingOverlayPermission && Settings.canDrawOverlays(this)) {
            pendingOverlayPermission = false
            startOverlayAndFinish()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            checkOverlayPermission()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startOverlayAndFinish()
        } else {
            pendingOverlayPermission = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Bitte Overlay-Berechtigung aktivieren", Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlayAndFinish() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        finish()
    }
}
