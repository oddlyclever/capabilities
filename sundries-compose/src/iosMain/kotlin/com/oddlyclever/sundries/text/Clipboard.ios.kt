package com.oddlyclever.sundries.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberClipboardWriter(): ClipboardWriter = remember {
    ClipboardWriter { text ->
        UIPasteboard.generalPasteboard.string = text
        true
    }
}
