/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.volo.compose.permission.internal

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Creates a [MutablePermissionState] that is remembered across compositions.
 * One of the callbacks will be called after [PermissionState.launchPermissionRequest] is invoked.
 *
 * @param permission the permission to control and observe
 * @param onGranted will be called if the user granted the permission
 * @param onDenied will be called if the user denied the permission
 * @param onDeniedPermanently will be called if the user denied the permission permanently
 */
@Composable
internal fun rememberMutablePermissionState(
    permission: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onDeniedPermanently: () -> Unit,
): MutablePermissionState {
    val context = LocalContext.current
    val launchTime = remember { mutableLongStateOf(0L) }
    val permissionState = remember(permission) {
        MutablePermissionState(permission, launchTime, context, context.findActivity())
    }

    // Refresh the permission status when the lifecycle is resumed
    PermissionLifecycleCheckerEffect(permissionState)

    // Remember RequestPermission launcher and assign it to permissionState
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
        permissionState.refreshPermissionStatus()
        if (isGranted) {
            onGranted()
        } else if (isDeniedPermanently(launchTime)) {
            onDeniedPermanently()
        } else {
            onDenied()
        }
    }

    DisposableEffect(permissionState, launcher) {
        permissionState.launcher = launcher
        onDispose {
            permissionState.launcher = null
        }
    }

    return permissionState
}

/**
 * A mutable state object that can be used to control and observe permission status changes.
 *
 * In most cases, this will be created via [rememberMutablePermissionState].
 *
 * @param permission the permission to control and observe.
 * @param context to check the status of the [permission].
 * @param activity to check if the user should be presented with a rationale for [permission].
 */
@Stable
internal class MutablePermissionState(
    override val permission: String,
    private val launchTime: MutableLongState,
    private val context: Context,
    private val activity: Activity
) : PermissionState {

    override var status: PermissionStatus by mutableStateOf(getPermissionStatus())

    override fun launchPermissionRequest() {
        launchTime.longValue = SystemClock.elapsedRealtime()
        launcher?.launch(
            permission
        ) ?: throw IllegalStateException("ActivityResultLauncher cannot be null")
    }

    internal var launcher: ActivityResultLauncher<String>? = null

    internal fun refreshPermissionStatus() {
        status = getPermissionStatus()
    }

    private fun getPermissionStatus(): PermissionStatus {
        val hasPermission = context.checkPermission(permission)
        return if (hasPermission) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied(activity.shouldShowRationale(permission))
        }
    }
}

private fun isDeniedPermanently(launchTime: MutableLongState) =
    SystemClock.elapsedRealtime() - launchTime.longValue < PERMISSION_PERMANENTLY_DENIED_THRESHOLD

private const val PERMISSION_PERMANENTLY_DENIED_THRESHOLD = 400L
