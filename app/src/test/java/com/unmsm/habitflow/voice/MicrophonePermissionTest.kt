package com.unmsm.habitflow.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class MicrophonePermissionTest {
    @Test fun grantedPermissionIsRecognized() = assertEquals(
        MicrophonePermissionState.Granted,
        microphonePermissionState(true, requestedBefore = false, shouldShowRationale = false)
    )

    @Test fun deniedPermissionCanBeRequestedAgain() = assertEquals(
        MicrophonePermissionState.Denied,
        microphonePermissionState(false, requestedBefore = true, shouldShowRationale = true)
    )

    @Test fun permanentlyDeniedPermissionOpensSettingsPath() = assertEquals(
        MicrophonePermissionState.PermanentlyDenied,
        microphonePermissionState(false, requestedBefore = true, shouldShowRationale = false)
    )
}
