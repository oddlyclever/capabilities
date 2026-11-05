package com.oddlyclever.sundries.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

// The system clipboard through AWT. Headless, or with the clipboard held by another program, AWT throws, and the
// copier reports that as refused.
@Composable
internal actual fun rememberClipboardWriter(): ClipboardWriter = remember {
    ClipboardWriter { text ->
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    }
}
