/*
 * Copyright 2024 Volodymyr Galandzij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.volo.compose.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.volo.compose.permission.OnPermissionPermanentlyDenied.Execute
import dev.volo.compose.permission.OnPermissionPermanentlyDenied.OpenSystemSettings
import dev.volo.compose.permission.OnPermissionPermanentlyDenied.ShowCustomDialog
import dev.volo.compose.permission.internal.isGranted
import dev.volo.compose.permission.internal.rememberPermissionState

/**
 * Composable that shows Android permission dialog, calling the respective callbacks when
 * the permission has been granted or denied, and allowing to perform a custom action when the
 * permission has been permanently denied by the user or the system (i.e. when the user
 * has selected the 'Never ask me again' option on older Android versions,
 * or denied the permission more than one time on newer Android versions).
 *
 * @param permission the permission to request
 *
 * @param onGranted callback that will be called if the permission has been granted
 *
 * @param onDenied callback that will be called if the permission has been denied
 *
 * @param onPermanentlyDenied optional action to execute when the permission was permanently denied.
 * This can be one of:
 * - [Execute] - call a specific lambda block, this is the default option which simply calls
 * the [onDenied] callback, if the permission has been permanently denied.
 *
 * - [OpenSystemSettings] - open the Android system settings, by default it opens the app settings,
 * if necessary that can be overridden with the [actionIntent][OpenSystemSettings.actionIntent]
 * parameter.
 *
 * - [ShowCustomDialog] - show a custom dialog composable, you can specify the composable using
 * the [content][ShowCustomDialog.content] slot parameter. The composable receives a
 * [PermissionCustomDialogState] which can be used to dismiss the composable or
 * to open the Android system settings (by default it opens the app settings, if
 * if necessary that can be overridden with the [actionIntent][ShowCustomDialog.actionIntent]
 * parameter).
 *
 * @param dialogState permission dialog state that determines whether this dialog is active,
 * by default the dialog is active, meaning it is shown to the user as soon as this composable
 * enters the composition. To control when the dialog should be shown create the state
 * with [rememberPermissionDialogState] setting [isActive][PermissionDialogState.isActive]
 * to `false`, then when it's time to show the dialog set it to `true` (for example, in an
 * `onClick` listener of a button that should trigger the permission request).
 * Note that `isActive` is automatically set to `false` before any of the callbacks is called:
 * [onGranted], [onDenied], and [Execute] lambda [block][Execute.block] (if [Execute] is used
 * as the [onPermanentlyDenied] action).
 */
@Composable
public fun PermissionDialog(
    permission: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: OnPermissionPermanentlyDenied = Execute(onDenied),
    dialogState: PermissionDialogState = rememberPermissionDialogState(),
) {
    var isCustomDialogVisible by remember(dialogState.isActive) { mutableStateOf(false) }
    var wasSystemSettingsShown by remember(dialogState.isActive) { mutableStateOf(false) }

    val settingsLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        wasSystemSettingsShown = true
    }

    val appPackageName = LocalContext.current.packageName

    val permissionState = rememberPermissionState(
        permission = permission,
        onGranted = {
            dialogState.hide()
            onGranted()
        },
        onDenied = {
            dialogState.hide()
            onDenied()
        },
        onDeniedPermanently = {
            when (onPermanentlyDenied) {
                is Execute -> {
                    dialogState.hide()
                    onPermanentlyDenied.block()
                }
                is OpenSystemSettings -> settingsLauncher.launch(
                    onPermanentlyDenied.actionIntent(appPackageName)
                )
                is ShowCustomDialog -> isCustomDialogVisible = true
            }
        },
    )

    if (!dialogState.isActive) return

    LaunchedEffect(wasSystemSettingsShown) {
        if (permissionState.status.isGranted) {
            dialogState.hide()
            onGranted()
        } else if (wasSystemSettingsShown) {
            dialogState.hide()
            onDenied()
        } else if (!isCustomDialogVisible) {
            permissionState.launchPermissionRequest()
        }
    }

    if (onPermanentlyDenied is ShowCustomDialog && isCustomDialogVisible) {
        val customDialogState = remember {
            PermissionCustomDialogState(
                onDismiss = {
                    isCustomDialogVisible = false
                    dialogState.hide()
                    onDenied()
                },
                onOpenSystemSettings = {
                    isCustomDialogVisible = false
                    settingsLauncher.launch(onPermanentlyDenied.actionIntent(appPackageName))
                },
            )
        }
        onPermanentlyDenied.content(customDialogState)
    }
}

@Stable
public class PermissionDialogState(isActive: Boolean) {
    public var isActive: Boolean by mutableStateOf(isActive)
        private set

    public fun show() {
        isActive = true
    }

    public fun hide() {
        isActive = false
    }
}

@Composable
public fun rememberPermissionDialogState(isActive: Boolean = true): PermissionDialogState =
    remember { PermissionDialogState(isActive = isActive) }

@Stable
public sealed interface OnPermissionPermanentlyDenied {

    public data class Execute(val block: () -> Unit) : OnPermissionPermanentlyDenied

    public data class OpenSystemSettings(
        val actionIntent: (packageName: String) -> Intent = ::openSystemSettingsIntent
    ) : OnPermissionPermanentlyDenied

    public data class ShowCustomDialog(
        public val actionIntent: (packageName: String) -> Intent = ::openSystemSettingsIntent,
        public val content: @Composable (PermissionCustomDialogState) -> Unit,
    ) : OnPermissionPermanentlyDenied
}

@Stable
public class PermissionCustomDialogState(
    private val onDismiss: () -> Unit,
    private val onOpenSystemSettings: () -> Unit,
) {
    public fun dismiss(): Unit = onDismiss()
    public fun openSystemSettings(): Unit = onOpenSystemSettings()
}

private fun openSystemSettingsIntent(packageName: String): Intent =
    Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts(SCHEME_SETTINGS_PACKAGE, packageName, null)
    }

private const val SCHEME_SETTINGS_PACKAGE = "package"
