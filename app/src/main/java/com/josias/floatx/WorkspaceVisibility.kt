package com.josias.floatx

/** Hiding a group never changes the user's individual minimized state. */
data class WorkspaceVisibility(val visible:Boolean,val touchable:Boolean,val focusable:Boolean) {
    companion object {
        fun forWindow(workspaceActive:Boolean,minimized:Boolean,focused:Boolean):WorkspaceVisibility {
            val shown=workspaceActive && !minimized
            return WorkspaceVisibility(shown,shown,shown && focused)
        }
    }
}

data class WorkspaceEntry(val key:String,val label:String,val component:String?,val box:WindowBox,val minimized:Boolean)
