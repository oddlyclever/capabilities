# OsFamily

DOC-0022 · 2026-11-04
Keywords: OsFamily, operating system family, detect platform, current platform, OsFamily.current does not exist, getPlatform, Platform interface, isVisibleIf, isVisibleUnless, show on iOS only, hide on web, Apple icons, upper case enum members, OsFamily.Ios does not compile, PascalCase exception, enum name stored, ordinal, APPLE not matching IOS, WEB and WASM

`OsFamily` names the family of operating system an app runs on, for the few decisions that depend on it: which icon set, which wording, whether a control exists at all. It is in common code, on every target.

## The members

`UNKNOWN`, `MACOSX`, `IOS`, `APPLE`, `LINUX`, `WINDOWS`, `ANDROID`, `WASM`, `WEB`, `TVOS`, `WATCHOS`.

They are exact, and members that overlap in meaning are not related. `OsFamily.IOS` is not an `APPLE`, and `WEB` and `WASM` are as separate as `IOS` and `ANDROID`.

## The two checks

```kotlin
fun showsShareButton(family: OsFamily) = family.isVisibleUnless(OsFamily.WEB, OsFamily.WASM)
fun usesAppleIcons(family: OsFamily) = family.isVisibleIf(OsFamily.IOS, OsFamily.MACOSX)
```

- **`isVisibleIf(…)`** is whether this member is among those listed: for something shown only on them.
- **`isVisibleUnless(…)`** is whether it is none of them: for something hidden on them.

Neither knows about overlap, so list every member that applies: `OsFamily.IOS.isVisibleIf(OsFamily.APPLE)` is `false`. With no arguments, `isVisibleIf()` is `false` and `isVisibleUnless()` is `true`.

## There is no current OsFamily

The module does not detect the platform. `OsFamily.current`, `currentOsFamily()`, a `Platform` interface and `getPlatform()`, as a new multiplatform project template generates, do not exist. The app decides which family it is, typically with an `expect val` of its own, and passes it in.

## Why the members are upper case

They break this repository's rule that enum members are PascalCase. The members predate the rule in the apps that use them, and are kept as they are until those apps move with a rename. It is `OsFamily.IOS`; `OsFamily.Ios` does not compile.

Because the names are expected to change, never store or send a member's `name`, and never its `ordinal`. Compare members, and map them to stored values of your own.
