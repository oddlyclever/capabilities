package com.oddlyclever.sundries.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString

@Composable
internal actual fun rememberClipboardWriter(): ClipboardWriter = remember {
    ClipboardWriter { text ->
        val board = NSPasteboard.generalPasteboard
        // A pasteboard keeps what it held until it is cleared, so the old text would stay beside the new.
        board.clearContents()
        board.setString(text, forType = NSPasteboardTypeString)
    }
}
