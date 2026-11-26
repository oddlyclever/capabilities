# sundries-compose

The full documentation of [`sundries-compose`](../../../sundries-compose/README.md): the Compose Multiplatform parts every app ends up writing for itself. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Copying text

`TextCopy` is text with a copy icon at its end. A tap puts `copies`, which defaults to the shown text, on the clipboard; the icon turns to a tick for a moment, or to a warning in the error colour when the platform refuses. To a screen reader it is one button, labelled with the app's own words from `CopyStrings`, and the outcome is announced politely.

`rememberCopier` is the part under it, for an app's own copy controls. Its `state` is `Ready`, `Copied` for a moment after a copy, or `Refused` until the next copy. Every control that uses one copies the same way and reports the same states, while each looks like itself.

Each platform's clipboard:

| Target | How it copies | When it refuses |
|---|---|---|
| Android | the system `ClipboardManager` | the clipboard service is missing |
| iOS | `UIPasteboard.generalPasteboard` | never |
| JVM | AWT's system clipboard | headless, or the clipboard is held by another program |
| JS, wasmJs | `navigator.clipboard`, then a selected text area | both fail |

**Plain http on the web.** Browsers give the clipboard API only to a secure context, https or localhost, so a page served over plain http, such as an app on a private network, has no `navigator.clipboard`. The copier then copies from a hidden, selected text area, the older way that browsers still allow in answer to a click, and puts focus back afterwards, so a canvas app keeps the keyboard. The same fallback covers a secure page whose write is refused. Call `copy` from a click: some browsers copy only in answer to one.

**Touch targets.** The icon is sized to the text so `TextCopy` fits a table cell without making the row taller, which means its target is only as big as the text. Where it stands alone on a touch screen, give it room with its modifier.

## Modifiers and test tags

- `testTagIfNotNull` tags an element only when a tag is given, joining suffixes and an index with underscores, so a reusable component takes an optional tag and is tagged only where a screen asked for it.
- `exposeTestTagsAsResourceIds`, Android only, turns test tags into resource ids for tools that drive the app from outside. Apply it once near the root, enabled only in builds that are not for release.
- `addIf`, `addIfNotNull` and `addIfNull` add one link of a modifier chain on a condition.
- `noRippleClickable` is a click without the ripple, for an element that shows its own response; it is still announced and focusable.
- `alternatingBackground` stripes rows, and `iconSizeByTextStyle` sizes an icon to a line of text.

## Colours, sizes and windows

- `String.toColor` reads `#RRGGBB` or `#AARRGGBB`, with or without the `#`, and answers `Color.Unspecified` for anything else; `toColorInt` throws instead. `Color.toColorString` writes `#AARRGGBB`, which reads back as the same colour. `disabled` applies Material's disabled alpha.
- `toDp` and `toTextUnit` convert between pixels, dp and text sizes at the current density.
- `rememberIsMultiPane` and `rememberIsExpanded` say whether the window is at least Material's medium or expanded width. They are live: do not keep the answer across recompositions.
- `rawStringResource` reads a raw file as UTF-8 text with a reader the app passes, its own `Res::readBytes`. Compose generates a `Res` for each module, and each reads only its own module's files, so a library cannot reach an app's files without being handed the reader. A missing file fails as the reader fails it. Taking the reader also leaves room for a localized lookup, a copy of the file for the person's language tried before the default, which raw files do not get from Compose's qualifiers.

## Text effects

`TextAmbientGlow` and `TextNeonGlow` are Material's `Text` with a glow behind it: a soft, wide one, or a tight one around each letter. The glow is a shadow with no offset, so it replaces any shadow in the style.

`TextShimmer` is Material's `Text` with a soft band of colour sweeping across its letters, the way an agent's "Thinking…" line shows that work is under way. With nothing set it is the muted look: the text at half its usual colour, and the band bringing it back to full colour as it crosses. `colors` gives the band its colour, the app's highlight colour for a colour shimmer; with several, each pass takes the next and the colours rotate. `color` is the text outside the band, and must be dimmer than the band's colours for the band to show. It is an indeterminate progress indicator that is its own label, and says so: while active, its semantics are an indeterminate progress bar's, so a screen reader reads the text and that it is in progress.

`Modifier.shimmer` is the same band on anything drawn: an icon, a placeholder, a whole row. The band recolours only the pixels the content drew, so it follows the letters or the icon's shape, never the box around them. A band never changes how opaque a pixel is, so content dimmed with its own alpha would stay faint inside the band; `base` is the way to dim it instead, recolouring everything the band does not reach, which is how `TextShimmer` gets its muted look. It is half the content's width by default (`band`), crosses in `sweepMillis` and then leaves the content plain for `pauseMillis`. `ShimmerMotion.Sweep` enters at the start edge and leaves at the end edge, right to left in a right-to-left layout; `ShimmerMotion.Bounce` goes and returns, slowing into each edge like a scanner. Only drawing changes as the band moves, so nothing recomposes or measures. Compose Multiplatform has no reduced-motion setting on every target, so an app that honours one passes `active = false` there, which also ends the shimmer when the work is done.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.

## Voice indicator

`VoiceIndicator` is a small drawn sign that a microphone is live, for the inside of a button or beside one. Given a loudness level from 0 to 1 it rests almost still in silence and moves with the voice, rising quickly and falling slowly; given `null` it moves by itself, for when an app has no measure of the sound. `VoicePattern` chooses what it draws: a `Wave` line pinned at both ends, `Bars`, `Dots`, `Rings` or a `Breathing` circle. It draws in one colour and nothing else, carries no semantics, and takes `animated = false` for an app that honours reduced motion.

```kotlin
VoiceIndicator(level = loudness, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
VoiceIndicator(level = null, color = accent, pattern = VoicePattern.Rings, modifier = Modifier.size(24.dp))
```

It knows nothing of where the level comes from: an app passes a number as often as it has one, a dozen times a second is plenty, and the indicator smooths it. A level under `gate` counts as silence, so a room's own noise does not move it. Whether the microphone is live should not rest on the indicator alone: the button around it says so in its own state and to a screen reader, and with `animated = false` the pattern is drawn once, at rest.
