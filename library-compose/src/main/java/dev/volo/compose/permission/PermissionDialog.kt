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
                is Execute -> onPermanentlyDenied.block()
                is ShowCustomDialog -> isCustomDialogVisible = true
                is OpenSystemSettings -> settingsLauncher.launch(
                    onPermanentlyDenied.actionIntent(appPackageName)
                )
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
        onPermanentlyDenied.dialog(customDialogState)
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
    public data class ShowCustomDialog(
        public val actionIntent: (packageName: String) -> Intent = ::openSystemSettingsIntent,
        public val dialog: @Composable (PermissionCustomDialogState) -> Unit,
    ) : OnPermissionPermanentlyDenied

    public data class OpenSystemSettings(
        val actionIntent: (packageName: String) -> Intent = ::openSystemSettingsIntent
    ) : OnPermissionPermanentlyDenied

    public data class Execute(val block: () -> Unit) : OnPermissionPermanentlyDenied
}

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
