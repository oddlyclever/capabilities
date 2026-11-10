# platform

The full documentation of [`platform`](https://github.com/oddlyclever/capabilities/blob/HEAD/platform/README.md): what must exist exactly once in a process, which on Android is the application context and the activity in front, and `OsFamily` on every target. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [The Application Context and the Activity in Front](Platform-The-Application-Context-And-The-Activity-In-Front): which targets get what, `AppContext` and `ActivityProvider` in detail, what `current` reports, and binding them in a container.
- [Start-Up and the Manual Install](Platform-Start-Up-And-The-Manual-Install): how the initializer arrives and runs, installing by hand when App Startup's provider is removed, what each failure looks like, and the device tests.
- [OsFamily](Platform-Osfamily): the members, the two visibility checks, why there is no current family, and why the members are upper case.

## Why it is built this way

- [One Holder per Process](Platform-One-Holder-Per-Process): why the context and the activity are held once, filled by App Startup, and why nothing else belongs here.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
