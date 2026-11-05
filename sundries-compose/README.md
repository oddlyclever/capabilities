# sundries-compose

The Compose Multiplatform parts every app ends up writing for itself: text that copies itself when tapped, modifier and test-tag helpers, colour and size conversions, window-size checks, text effects, and raw text files read with the app's own resources.

**Targets:** JVM · Android · iOS (arm64, simulator) · JS · wasmJs. `exposeTestTagsAsResourceIds` is Android only.

**Package:** `com.oddlyclever.sundries`, with text components in `com.oddlyclever.sundries.text`. That departs from this repository's rule of a package named after the module, which would give `com.oddlyclever.sundries.compose`: the shorter package was chosen because every import in an app would otherwise carry the extra word. A Compose-free `sundries` module, if one is ever added, gets a package of its own.

## Install

```kotlin
implementation("com.oddlyclever:sundries-compose:0.0.7")
```

There is nothing to set up and no dependency-injection container.

## Use

```kotlin
// Text that copies itself: a copy icon at its end, a tick once copied, a warning when the platform refuses.
TextCopy(
    text = "Ada Lovelace · @ada",
    copies = "@ada",
    strings = CopyStrings(copy = "Copy the handle", copied = "Copied the handle"),
)

// The same copying for a control of the app's own.
val copier = rememberCopier()
Button(onClick = { copier.copy(orderId) }) {
    Text(if (copier.state == CopyState.Copied) "Copied" else "Copy order number")
}

// Tags a component for UI tests only where the screen gave it one.
Modifier.testTagIfNotNull(testTag, "title")

// One link of a modifier chain, on a condition.
Modifier.addIf(enabled) { clickable(onClick = onOpen) }

// A raw text file of the app's, read with the app's own generated Res.
val terms = rawStringResource("files/legal/terms.md", Res::readBytes)
```

On the web, `TextCopy` and `rememberCopier` also copy on a page served over plain http, where browsers have no clipboard API, by falling back to the older way a click may still copy. Where nothing works, the state is `CopyState.Refused` and the control says so; nothing is thrown.

`TextCopy`'s wording is the app's: `CopyStrings` has English defaults, and an app passes its own for another language or to say what is copied, which is what a screen reader user hears.

## Documentation

The full documentation is in the module's section of the [knowledge base](../docs/knowledge/sundries-compose/README.md), which is mirrored to the repository's wiki.

## Third-party material

The three icons `TextCopy` draws, copy, check and warning, are the path data of Material icons from `androidx.compose.material:material-icons-core` and `material-icons-extended` 1.7.6, by The Android Open Source Project, under the Apache License 2.0. They are copied into [CopyIcons.kt](src/commonMain/kotlin/com/oddlyclever/sundries/text/CopyIcons.kt), which keeps the original notice and says what was changed, so the module needs neither icon library.

## For coding agents

The module ships an [Agent Skill](https://agentskills.io/specification), `com-oddlyclever-sundries-compose`, for the agents of projects that depend on it: what the library is for, how it's meant to be called, and the mistakes that compile. It is written at [src/commonMain/skills/com-oddlyclever-sundries-compose/SKILL.md](src/commonMain/skills/com-oddlyclever-sundries-compose/SKILL.md) and published inside every sources jar, at `commonMain/skills/com-oddlyclever-sundries-compose/`.
