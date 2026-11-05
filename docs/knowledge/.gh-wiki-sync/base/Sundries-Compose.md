# sundries-compose

The full documentation of [`sundries-compose`](https://github.com/oddlyclever/capabilities/blob/HEAD/sundries-compose/README.md): the Compose Multiplatform parts every app ends up writing for itself. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

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

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
