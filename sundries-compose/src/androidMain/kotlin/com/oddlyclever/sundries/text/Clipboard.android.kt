package com.oddlyclever.sundries.text

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberClipboardWriter(): ClipboardWriter {
    val context = LocalContext.current
    return remember(context) {
        ClipboardWriter { text ->
            val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return@ClipboardWriter false
            clipboard.setPrimaryClip(ClipData.newPlainText("", text))
            true
        }
    }
}
