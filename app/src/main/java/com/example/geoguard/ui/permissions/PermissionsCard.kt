package com.example.geoguard.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Walks the user through the three permissions the app needs, one step at a time:
 * 1. Precise location  2. "Allow all the time" (background)  3. Notifications (Android 13+).
 * Background location must be requested *after* foreground location, with an explanation first.
 */
@Composable
fun PermissionsCard(onPermissionsChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var askedBackgroundOnce by remember { mutableStateOf(false) }

    val hasFine = remember(refreshKey) { AppPermissions.hasFineLocation(context) }
    val hasBackground = remember(refreshKey) { AppPermissions.hasBackgroundLocation(context) }
    val hasNotifications = remember(refreshKey) { AppPermissions.hasNotifications(context) }

    // Tell the app to (re)register geofences whenever the location permissions change.
    LaunchedEffect(hasFine, hasBackground) { onPermissionsChanged() }

    // Re-check periodically while something is missing, so returning from the Settings app is picked up.
    if (!hasFine || !hasBackground || !hasNotifications) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000)
                refreshKey++
            }
        }
    } else {
        return
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshKey++ }

    val singleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }

    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            when {
                !hasFine -> {
                    Text("Step 1 of 3: Location", style = MaterialTheme.typography.titleSmall)
                    Text("GeoGuard needs your precise location to know when you leave a group's zone.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        foregroundLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) { Text("Allow precise location") }
                }
                !hasBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    Text("Step 2 of 3: Background location", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "To alert your group even when the app is closed, choose \"Allow all the time\" " +
                                "on the next screen. Your location is never uploaded; only zone exits are."
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        askedBackgroundOnce = true
                        singleLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }) { Text("Allow all the time") }
                    if (askedBackgroundOnce) {
                        TextButton(onClick = openAppSettings) { Text("Open app settings instead") }
                    }
                }
                !hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    Text("Step 3 of 3: Notifications", style = MaterialTheme.typography.titleSmall)
                    Text("Allow notifications so you're alerted when a member leaves the zone.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Allow notifications") }
                }
            }
        }
    }
}