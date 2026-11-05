---
name: com-oddlyclever-sundries-compose
description: >-
  Copy text to the clipboard from Compose Multiplatform with one composable:
  TextCopy shows text with a copy icon, copies on tap, shows a tick when
  copied and a warning when the platform refuses, and is one labelled button
  to a screen reader. rememberCopier gives the same copying and its state to
  an app's own copy button. Works on Android, iOS, desktop JVM and the web,
  including web pages served over plain http where navigator.clipboard does
  not exist. Use instead of LocalClipboardManager, ClipboardManager.setText,
  UIPasteboard or navigator.clipboard written by hand. Also small Compose
  helpers apps keep rewriting: conditional modifiers (addIf, addIfNotNull,
  addIfNull), optional test tags and Android resource-id test tags, ripple-free
  clicks, hex colour parsing and formatting, pixel, dp and sp conversions,
  window-width checks for multi-pane layouts, glowing text, and reading a
  raw text file from the app's own Compose resources.
license: Apache-2.0
metadata:
  version: "0.0.7"
  repository: https://github.com/oddlyclever/capabilities
---

# sundries-compose

`com.oddlyclever:sundries-compose`, package `com.oddlyclever.sundries`, with text components in `com.oddlyclever.sundries.text`. Note that the package has no `.compose` in it, although the artifact does. Called from Kotlin only.

## What it solves

Copying text looks like one line, and it is not. On the web the clipboard API exists only on https or localhost, so a copy button that works in development does nothing on a site served over plain http, and silently. A copy control also needs to say that it worked, say when it did not, and be usable with a screen reader. `TextCopy` and `rememberCopier` do all of that once, on every target, so an app does not write it again for each screen.

## How it is meant to be used

Text the user may want to copy:

```kotlin
import com.oddlyclever.sundries.text.CopyStrings
import com.oddlyclever.sundries.text.TextCopy

TextCopy(
    text = "Ada Lovelace · @ada",
    copies = "@ada", // what goes on the clipboard; defaults to the text
    strings = CopyStrings(copy = "Copy the handle", copied = "Copied the handle"),
)
```

A copy control of the app's own, sharing the same copying and states:

```kotlin
import com.oddlyclever.sundries.text.CopyState
import com.oddlyclever.sundries.text.rememberCopier

val copier = rememberCopier()
Button(onClick = { copier.copy(orderId) }) {
    Text(if (copier.state == CopyState.Copied) "Copied" else "Copy order number")
}
```

`copier.state` is `Ready`, `Copied` for a moment after a copy, or `Refused` until the next copy. Nothing throws: a platform that will not copy gives `Refused`.

## Mistakes that compile

- **Leaving `strings` at its default where the label matters.** The defaults say only "Copy" and "Copied", in English. A screen reader user hears nothing else, so pass `CopyStrings` that say what is copied, in the app's language.
- **Calling `copy` outside a click.** Some browsers copy only in answer to a user gesture; from a `LaunchedEffect` or a timer the state will be `Refused`.
- **Importing `com.oddlyclever.sundries.compose`.** There is no such package; the module's code is in `com.oddlyclever.sundries` and `com.oddlyclever.sundries.text`.
- **Expecting `addIfNull` to hand over the value.** It runs only when the value is null, so its lambda takes nothing; `addIfNotNull` is the one that passes the value in.
- **Keeping `rememberIsMultiPane()` in a variable across recompositions.** It follows the window as it is resized or turned.
- **`String.toColor` with a colour name.** It reads `#RRGGBB` and `#AARRGGBB` only, and answers `Color.Unspecified` for anything else, "red" included.
- **Expecting `rawStringResource` to find the app's files by itself.** It reads with the reader it is given; pass the app's own `Res::readBytes`. A library's `Res` reads only that library's files.
