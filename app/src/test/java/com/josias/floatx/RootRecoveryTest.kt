package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class RootRecoveryTest {
    @Test fun transientEmptyFrameDoesNotRestartApp() {
        val policy=RootRecovery()
        policy.observe(AppPresence.VISIBLE)
        assertFalse(policy.observe(AppPresence.EMPTY))
        assertFalse(policy.observe(AppPresence.VISIBLE))
        assertFalse(policy.observe(AppPresence.EMPTY))
    }
    @Test fun sustainedEmptyDisplayRestoresRootOnce() {
        val policy=RootRecovery()
        policy.observe(AppPresence.VISIBLE)
        assertFalse(policy.observe(AppPresence.EMPTY))
        assertTrue(policy.observe(AppPresence.EMPTY))
        assertFalse(policy.observe(AppPresence.VISIBLE))
    }
    @Test fun emptyUnverifiedTaskListNeverTriggersRestartLoop() {
        val policy=RootRecovery()
        repeat(10) { assertFalse(policy.observe(AppPresence.EMPTY)) }
        policy.observe(AppPresence.VISIBLE)
        policy.observe(AppPresence.EMPTY)
        assertTrue(policy.observe(AppPresence.EMPTY))
        repeat(10) { assertFalse(policy.observe(AppPresence.EMPTY)) }
    }
    @Test fun unreadableTasksAndClosedSessionNeverTriggerRecovery() {
        val policy=RootRecovery()
        policy.observe(AppPresence.VISIBLE)
        policy.observe(AppPresence.EMPTY)
        assertFalse(policy.observe(AppPresence.UNKNOWN))
        assertFalse(policy.observe(AppPresence.EMPTY))
        policy.reset()
        assertFalse(policy.observe(AppPresence.EMPTY))
    }
}
