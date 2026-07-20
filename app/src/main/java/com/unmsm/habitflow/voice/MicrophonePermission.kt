package com.unmsm.habitflow.voice

enum class MicrophonePermissionState { Granted, Denied, PermanentlyDenied }

fun microphonePermissionState(
    granted: Boolean,
    requestedBefore: Boolean,
    shouldShowRationale: Boolean
): MicrophonePermissionState =
    when {
        granted -> MicrophonePermissionState.Granted
        requestedBefore && !shouldShowRationale -> MicrophonePermissionState.PermanentlyDenied
        else -> MicrophonePermissionState.Denied
    }
