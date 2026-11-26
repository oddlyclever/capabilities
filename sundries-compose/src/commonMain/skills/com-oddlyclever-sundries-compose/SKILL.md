---
name: com-oddlyclever-sundries-compose
description: >-
  Copy text to the clipboard from Compose Multiplatform with one composable:
  TextCopy shows text with a copy icon, copies on tap, shows a tick when
  copied and a warning when the platform refuses, and is one labelled button
  to a screen reader. rememberCopier gives the same copying to an app's own
  copy button. Works on Android, iOS, macOS, desktop JVM and the web,
  including pages served over plain http where navigator.clipboard does not
  exist. Use instead of LocalClipboardManager, ClipboardManager.setText,
  UIPasteboard or navigator.clipboard written by hand. Also TextShimmer, a
  "Thinking…" or loading label with a band of colour sweeping across its
  letters, an indeterminate progress indicator, and Modifier.shimmer for the
  same on any content. Also Compose helpers apps keep rewriting: conditional
  modifiers (addIf, addIfNotNull, addIfNull), optional test tags,
  ripple-free clicks, hex colours, pixel, dp and sp conversions,
  window-width checks, glowing text, and raw text files read with the app's
  own resources.
license: Apache-2.0
metadata:
  version: "0.0.9"
  repository: https://github.com/oddlyclever/capabilities
---

# sundries-compose

`com.oddlyclever:sundries-compose`, package `com.oddlyclever.sundries`, with text components in `com.oddlyclever.sundries.text`. Note that the package has no `.compose` in it, although the artifact does. Called from Kotlin only.

## What it solves

Copying text looks like one line, and it is not. On the web the clipboard API exists only on https or localhost, so a copy button that works in development does nothing on a site served over plain http, and silently. A copy control also needs to say that it worked, say when it did not, and be usable with a screen reader. `TextCopy` and `rememberCopier` do all of that once, on every target, so an app does not write it again for each screen.

Showing that work is under way without a spinner is the other part people keep writing: a label such as "Thinking…" with a band of colour sweeping across its letters, the way agent tools show they are working. `TextShimmer` is that label, and an indeterminate progress indicator to a screen reader while it runs. `Modifier.shimmer` sweeps the same band across anything else drawn: an icon, a placeholder, a row.

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

A label for work under way, muted with a darker band by default, or rotating through the app's colours, one per pass:

```kotlin
import com.oddlyclever.sundries.ShimmerMotion
import com.oddlyclever.sundries.text.TextShimmer

TextShimmer("Thinking for ${seconds}s", active = working)

TextShimmer(
    text = "Searching",
    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
    motion = ShimmerMotion.Bounce, // back and forth, like a scanner; Sweep, the default, goes one way and pauses
)
```

The same band on other content, here a placeholder block, dim outside the band:

```kotlin
import com.oddlyclever.sundries.shimmer

Box(
    Modifier.size(120.dp, 16.dp)
        .shimmer(MaterialTheme.colorScheme.onSurface, base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        .background(Color.Black, RoundedCornerShape(4.dp)) // any opaque colour: the shimmer recolours it
)
```

`sweepMillis`, `pauseMillis` and `band` (a fraction of the width) tune the timing and the band. Only drawing changes as the band moves, so it costs no recomposition.

A live microphone is shown with `VoiceIndicator`, not with a hand-drawn animation: a small drawing for the inside of a button, in one colour. Pass it the loudness as a number from 0 to 1 and it rests almost still in silence and moves with the voice; pass `null` where the app has no measure of the sound and it moves by itself.

```kotlin
import com.oddlyclever.sundries.VoiceIndicator
import com.oddlyclever.sundries.VoicePattern

VoiceIndicator(level = loudness, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
VoiceIndicator(level = null, color = MaterialTheme.colorScheme.primary, pattern = VoicePattern.Bars, modifier = Modifier.size(20.dp))
```

`VoicePattern` is `Wave` (the default), `Bars`, `Dots`, `Rings` or `Breathing`. It carries no semantics, so the button around it says that it is listening, and an app that honours reduced motion passes `animated = false`.

## Mistakes that compile

- **Leaving `strings` at its default where the label matters.** The defaults say only "Copy" and "Copied", in English. A screen reader user hears nothing else, so pass `CopyStrings` that say what is copied, in the app's language.
- **Calling `copy` outside a click.** Some browsers copy only in answer to a user gesture; from a `LaunchedEffect` or a timer the state will be `Refused`.
- **Importing `com.oddlyclever.sundries.compose`.** There is no such package; the module's code is in `com.oddlyclever.sundries` and `com.oddlyclever.sundries.text`.
- **Expecting `addIfNull` to hand over the value.** It runs only when the value is null, so its lambda takes nothing; `addIfNotNull` is the one that passes the value in.
- **Keeping `rememberIsMultiPane()` in a variable across recompositions.** It follows the window as it is resized or turned.
- **`String.toColor` with a colour name.** It reads `#RRGGBB` and `#AARRGGBB` only, and answers `Color.Unspecified` for anything else, "red" included.
- **A `TextShimmer` whose band does not show.** The band recolours the letters to `colors`, so a `color` as bright as the band hides it. Keep `color` dimmer, as the default half-colour does, or pass a band colour that stands out from the text.
- **Dimming content with alpha under `Modifier.shimmer`.** The band keeps each pixel's opacity, so a half-transparent text stays half-transparent where the band crosses it. Draw the content opaque and pass the dim colour as `base`.
- **Leaving a shimmer running after the work is done.** It runs while it is on screen; pass `active = false`, or show plain text, once the work it stands for has finished.
- **Expecting `Modifier.shimmer` to draw over the background.** It recolours only what the content after it in the chain draws; put it before `background` to shimmer a block, after it to shimmer only the content.
- **Expecting `rawStringResource` to find the app's files by itself.** It reads with the reader it is given; pass the app's own `Res::readBytes`. A library's `Res` reads only that library's files.
- **Expecting colours from a `TextShimmer`'s style or spans.** Its letters are drawn in `color` and the band's colours only.
- **Writing a second "is loading" announcement next to a running `TextShimmer`.** It already tells a screen reader that it is in progress; another live region repeats it.
- **Passing `colors = emptyList()` to stop the shimmer.** It does stop the band, but `active = false` says so and also drops the progress semantics.

## What changed between versions

`sundries-compose` was first released in 0.0.7, and its API has only grown since.

- **Since 0.0.10, `TextShimmer`, `Modifier.shimmer` and `ShimmerMotion` exist.** Code written for 0.0.9 or earlier that drew its own animated gradient over a "loading" label can use them instead.
