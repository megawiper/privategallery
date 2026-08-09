package com.privategallery.app.ui.editor

/**
 * STEP 39 — UNDO / REDO
 *
 * A simple linear history over EditState snapshots. Since EditState
 * (STEP 34) is an immutable data class, undo/redo is just moving a
 * cursor through a list of snapshots — no diffing or inverse-operation
 * logic needed, and the source image is never touched by any of this.
 */
class EditHistory(initial: EditState) {

    private val states = mutableListOf(initial)
    private var cursor = 0

    val current: EditState get() = states[cursor]
    val canUndo: Boolean get() = cursor > 0
    val canRedo: Boolean get() = cursor < states.lastIndex

    fun push(newState: EditState) {
        while (states.lastIndex > cursor) {
            states.removeAt(states.lastIndex)
        }
        states.add(newState)
        cursor = states.lastIndex
    }

    fun undo(): EditState {
        if (canUndo) cursor--
        return current
    }

    fun redo(): EditState {
        if (canRedo) cursor++
        return current
    }
}
