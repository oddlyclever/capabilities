package com.oddlyclever.wayfinder.frame

import kotlinx.coroutines.await
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko

// Skiko's own readiness callback is internal; this promise is the public way to wait for the same thing.
@OptIn(InternalSkikoApi::class)
actual suspend fun awaitSkia() {
    awaitSkiko.await()
}
