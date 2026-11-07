package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class WorkspaceVisibilityTest {
    @Test fun leavingTheGroupHidesEveryWindowAndDisablesInput() {
        for(minimized in listOf(false,true)) for(focused in listOf(false,true)) {
            assertEquals(WorkspaceVisibility(false,false,false),WorkspaceVisibility.forWindow(false,minimized,focused))
        }
    }
    @Test fun returningRestoresVisibleWindowsButNotMinimizedWindows() {
        assertEquals(WorkspaceVisibility(true,true,false),WorkspaceVisibility.forWindow(true,false,false))
        assertEquals(WorkspaceVisibility(false,false,false),WorkspaceVisibility.forWindow(true,true,true))
    }
    @Test fun onlyAVisibleFocusedWindowCanOwnTheKeyboard() {
        assertEquals(WorkspaceVisibility(true,true,true),WorkspaceVisibility.forWindow(true,false,true))
        assertFalse(WorkspaceVisibility.forWindow(false,false,true).focusable)
    }
}
