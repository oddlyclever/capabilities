package com.oddlyclever.platform

/**
 * The family of operating system an app runs on, for the few decisions that depend on it: which icon set, which
 * wording, whether a control exists at all.
 *
 * The members are upper case, against this repository's PascalCase rule for enum members. They predate the rule in
 * the apps that use them, and are kept as they are until those apps move with a rename. A member's name is not
 * meant to be stored or sent; compare members.
 */
enum class OsFamily {
    UNKNOWN,
    MACOSX,
    IOS,
    APPLE,
    LINUX,
    WINDOWS,
    ANDROID,
    WASM,
    WEB,
    TVOS,
    WATCHOS,
}

/** Whether this is one of [families]: for something shown only on them. */
fun OsFamily.isVisibleIf(vararg families: OsFamily): Boolean = families.contains(this)

/** Whether this is none of [families]: for something hidden on them. */
fun OsFamily.isVisibleUnless(vararg families: OsFamily): Boolean = !families.contains(this)
