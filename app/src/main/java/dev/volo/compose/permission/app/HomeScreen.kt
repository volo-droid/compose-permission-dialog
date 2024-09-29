package dev.volo.compose.permission.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.volo.compose.permission.OnPermissionPermanentlyDenied
import dev.volo.compose.permission.PermissionCustomDialogState
import dev.volo.compose.permission.PermissionDialog
import dev.volo.compose.permission.PermissionDialogState
import dev.volo.compose.permission.app.helper.rememberPermissionState
import dev.volo.compose.permission.rememberPermissionDialogState

@SuppressLint("InlinedApi")
@Composable
fun HomeScreen() {

    val allLocationPermissionsDialogState = rememberPermissionDialogState(isActive = false)
    val backgroundLocationPermissionDialogState = rememberPermissionDialogState(isActive = false)
    val cameraPermissionDialogState = rememberPermissionDialogState(isActive = false)
    val locationPermissionWithCustomDialogState = rememberPermissionDialogState(isActive = false)
    val coarseLocationPermissionDialogState = rememberPermissionDialogState(isActive = false)
    val fineLocationPermissionDialogState = rememberPermissionDialogState(isActive = false)


    Column(
        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PermissionStatus(Manifest.permission.CAMERA)
        PermissionStatus(Manifest.permission.ACCESS_COARSE_LOCATION)
        PermissionStatus(Manifest.permission.ACCESS_FINE_LOCATION)
        PermissionStatus(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        Spacer(Modifier.height(16.dp))

        RequestPermissionButton(
            state = cameraPermissionDialogState,
            text = "Request Camera permission",
        )

        RequestPermissionButton(
            state = coarseLocationPermissionDialogState,
            text = "Request Location permission (coarse)",
        )

        RequestPermissionButton(
            state = fineLocationPermissionDialogState,
            text = "Request Location permission (fine)",
        )

        RequestPermissionButton(
            state = locationPermissionWithCustomDialogState,
            text = "Request Location permission + custom dialog",
        )

        RequestPermissionButton(
            state = backgroundLocationPermissionDialogState,
            text = "Request Background Location permission",
        )

        RequestPermissionButton(
            state = allLocationPermissionsDialogState,
            text = "Request Location + Background permissions"
        )
    }

    val context = LocalContext.current

    PermissionDialog(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onGranted = {
            context.showToast("Location (fine) permission GRANTED")
            backgroundLocationPermissionDialogState.show()
        },
        onDenied = { context.showToast("Location (fine) permission DENIED") },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
        dialogState = allLocationPermissionsDialogState,
    )

    PermissionDialog(
        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        onGranted = { context.showToast("Location (coarse) permission GRANTED") },
        onDenied = { context.showToast("Location (coarse) permission DENIED") },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
        dialogState = coarseLocationPermissionDialogState,
    )

    PermissionDialog(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onGranted = { context.showToast("Location (fine) permission GRANTED") },
        onDenied = { context.showToast("Location (fine) permission DENIED") },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
        dialogState = fineLocationPermissionDialogState,
    )

    PermissionDialog(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        onGranted = { context.showToast("Background location permission GRANTED") },
        onDenied = { context.showToast("Background location permission DENIED") },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
        dialogState = backgroundLocationPermissionDialogState,
    )

    PermissionDialog(
        permission = Manifest.permission.CAMERA,
        onGranted = { context.showToast("Camera permission GRANTED") },
        onDenied = { context.showToast("Camera permission DENIED") },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
        dialogState = cameraPermissionDialogState,
    )

    PermissionDialog(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onGranted = {
            context.showToast("Location permission GRANTED")
        },
        onDenied = {
            context.showToast("Location permission DENIED")
        },
        onPermanentlyDenied = OnPermissionPermanentlyDenied.ShowCustomDialog { state ->
            PermissionExplanationDialog(state)
        },
        dialogState = locationPermissionWithCustomDialogState,
    )
}

@Composable
private fun PermissionExplanationDialog(dialogState: PermissionCustomDialogState) {
    AlertDialog(
        title = {
            Text("Location Permission required")
        },
        text = {
            Text(
                text = "The app requires Location Permission to function properly. " +
                        "Please enable the location permission in System Settings by " +
                        "selecting Permissions -> Location -> Allow all the time"
            )
        },
        onDismissRequest = dialogState::dismiss,
        confirmButton = {
            TextButton(onClick = dialogState::openSystemSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = dialogState::dismiss) {
                Text("Dismiss")
            }
        }
    )
}

private fun Context.showToast(text: String) {
    Toast.makeText(this, text, LENGTH_SHORT).show()
}

@Composable
private fun PermissionStatus(permission: String) {
    val permissionState = rememberPermissionState(permission)
    val shortName = remember(permission) { AnnotatedString(permission.substringAfterLast('.'), SpanStyle(fontWeight = FontWeight.Bold)) }
    val status =
        if (permissionState.isGranted) AnnotatedString("GRANTED", SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold))
        else AnnotatedString("DENIED", SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold))

    val text = AnnotatedString.Builder().apply {
        append(shortName)
        append(" permission is ")
        append(status)
    }.toAnnotatedString()

    Text(text)
}

@Composable
private fun RequestPermissionButton(
    state: PermissionDialogState,
    text: String,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = state::show
    ) {
        Text(text)
    }
}
