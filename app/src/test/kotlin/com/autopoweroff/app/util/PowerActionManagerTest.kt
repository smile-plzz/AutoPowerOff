package com.autopoweroff.app.util

import com.autopoweroff.app.data.PowerOffMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerActionManagerTest {

    @Test
    fun powerOffModes_haveValidTitlesAndDescriptions() {
        for (mode in PowerOffMode.entries) {
            assertTrue(mode.title.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }

    @Test
    fun powerActionManager_failureResultContainsInformativeReason() {
        val failure = PowerActionManager.ExecutionResult.Failure("Accessibility Service not active")
        assertEquals("Accessibility Service not active", failure.reason)
    }
}

