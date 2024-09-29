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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Creates a [PermissionState] that is remembered across compositions.
 *
 * @param permission the permission to control and observe.
 * One of the callbacks will be called after [PermissionState.launchPermissionRequest] is invoked.
 *
 * @param permission the permission to control and observe.
 * @param onGranted will be called if the user granted the permission
 * @param onDenied will be called if the user denied the permission
 * @param onDeniedPermanently will be called if the user has previously denied the permission
 *  and doesn't want to be asked again.
 */
@Composable
internal fun rememberPermissionState(
    permission: String,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onDeniedPermanently: () -> Unit,
): PermissionState =
    rememberMutablePermissionState(
        permission = permission,
        onGranted = onGranted,
        onDenied = onDenied,
        onDeniedPermanently = onDeniedPermanently,
    )

/**
 * A state object that can be hoisted to control and observe [permission] status changes.
 *
 * In most cases, this will be created via [rememberPermissionState].
 *
 * It's recommended that apps exercise the permissions workflow as described in the
 * [documentation](https://developer.android.com/training/permissions/requesting#workflow_for_requesting_permissions).
 */
@Stable
internal interface PermissionState {

    /**
     * The permission to control and observe.
     */
    val permission: String

    /**
     * [permission]'s status
     */
    val status: PermissionStatus

    /**
     * Request the [permission] to the user.
     *
     * This should always be triggered from non-composable scope, for example, from a side-effect
     * or a non-composable callback. Otherwise, this will result in an IllegalStateException.
     *
     * This triggers a system dialog that asks the user to grant or revoke the permission.
     * Note that this dialog might not appear on the screen if the user doesn't want to be asked
     * again or has denied the permission multiple times.
     * This behavior varies depending on the Android level API.
     */
    fun launchPermissionRequest(): Unit
}
