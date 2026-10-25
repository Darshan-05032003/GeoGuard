package com.example.geoguard.ui.creategroup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onUseCurrentLocation: () -> Unit,
    onPermissionDenied: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onCreate: (name: String) -> Unit,
    onBack: () -> Unit,
    onCreated: (groupId: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            onUseCurrentLocation()
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId -> onCreated(groupId) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create group") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name (e.g. Home, Office)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Zone centre", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val lat = uiState.latitude
            val lng = uiState.longitude
            Text(
                text = if (lat != null && lng != null) "Lat %.5f, Lng %.5f".format(lat, lng) else "Not set yet",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (alreadyGranted) {
                        onUseCurrentLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !uiState.isLocating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isLocating) "Getting location..." else "Use my current location")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Zone radius: ${uiState.radiusMeters.roundToInt()} m",
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = uiState.radiusMeters,
                onValueChange = onRadiusChange,
                valueRange = MIN_RADIUS_METERS..MAX_RADIUS_METERS,
                steps = 16
            )
            Text(
                text = "Geofences smaller than 150 m are unreliable, so 150 m is the minimum.",
                style = MaterialTheme.typography.bodySmall
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onCreate(name) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create group")
                }
            }
        }
    }
}