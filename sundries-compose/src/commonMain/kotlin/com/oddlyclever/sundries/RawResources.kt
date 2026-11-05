package com.oddlyclever.sundries

/**
 * The raw file at [path] as UTF-8 text, read with [read]: pass the app's own `Res::readBytes`, as in
 * `rawStringResource("files/legal/terms.md", Res::readBytes)`.
 *
 * The reader is a parameter because Compose generates a `Res` for each module and each one reads only its own
 * module's files, so a library can never reach an app's files by itself. Taking the reader also leaves room to look
 * for a localized copy of a file before the default one, which raw files do not get from Compose's qualifiers.
 *
 * A missing file fails as [read] fails it; with a generated `Res`, that is its `MissingResourceException`.
 */
suspend fun rawStringResource(path: String, read: suspend (path: String) -> ByteArray): String =
    read(path).decodeToString()
