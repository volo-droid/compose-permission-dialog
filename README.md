# Android Permission Dialog for Jetpack Compose

Composable that shows Android permission dialog, and allows to perform custom actions when the
permission has been permanently denied by the user or the system (i.e. when the user has 
selected the 'Never ask me again' option on older Android versions, or denied the permission more 
than once on newer Android versions).

### Usage

#### Simple use-case

```kotlin
PermissionDialog(
    permission = Manifest.permission.CAMERA,
    onGranted = { context.showToast("Camera permission was GRANTED") },
    onDenied = { context.showToast("Camera permission was DENIED") }
)
```

#### Open app settings if the permission has been permanently denied

```kotlin
PermissionDialog(
    permission = Manifest.permission.ACCESS_FINE_LOCATION,
    onGranted = { context.showToast("Location permission was GRANTED") },
    onDenied = { context.showToast("Location permission was DENIED") },
    onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings()
)
```

#### Show custom dialog if the permission has been permanently denied

```kotlin
PermissionDialog(
    permission = Manifest.permission.ACCESS_FINE_LOCATION,
    onGranted = { context.showToast("Location permission was GRANTED") },
    onDenied = { context.showToast("Location permission was DENIED") },
    onPermanentlyDenied = OnPermissionPermanentlyDenied.ShowCustomDialog { state ->
        PermissionExplanationDialog(state)
    }
)

@Composable
fun PermissionExplanationDialog(dialogState: PermissionCustomDialogState) {
    AlertDialog(
        title = {
            Text("Location Permission required")
        },
        text = {
            Text(
                 "The app requires Location Permission to function properly.\n" +
                 "Please enable the location permission in System Settings by " +
                 "selecting Permissions -> Location -> Allow all the time"
            )
        },
        confirmButton = {
            TextButton(onClick = dialogState::openSystemSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = dialogState::dismiss) {
                Text("Dismiss")
            }
        },
        onDismissRequest = dialogState::dismiss,
    )
}
```

#### Request Background Location permission after the regular Location permission has been granted

```kotlin
val locationPermissionsDialogState = rememberPermissionDialogState(isActive = false)
val backgroundLocationPermissionDialogState = rememberPermissionDialogState(isActive = false)

Button(onClick = locationPermissionsDialogState::show) {
    Text("Request all Location permissions")
}

PermissionDialog(
    permission = Manifest.permission.ACCESS_FINE_LOCATION,
    onGranted = { backgroundLocationPermissionDialogState.show() },
    onDenied = { context.showToast("Location permission has been DENIED") },
    onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
    dialogState = locationPermissionsDialogState,
)

PermissionDialog(
    permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    onGranted = { context.showToast("Background Location permission has been GRANTED") },
    onDenied = { context.showToast("Background location permission has been DENIED") },
    onPermanentlyDenied = OnPermissionPermanentlyDenied.OpenSystemSettings(),
    dialogState = backgroundLocationPermissionDialogState,
)
```

#### Other

Have a look at the sample app and its [HomeScreen](app/src/main/java/dev/volo/compose/permission/app/HomeScreen.kt) composable.
