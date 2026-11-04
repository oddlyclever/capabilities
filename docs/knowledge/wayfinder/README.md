# wayfinder

The full documentation of [`wayfinder`](../../../wayfinder/README.md): maps for Compose Multiplatform on map-view, and the root of the wayfinder family. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [How the Wayfinder Modules Fit Together](DOC-0023-how-the-wayfinder-modules-fit-together.md): what each wayfinder module adds, the interfaces they meet at, what comes with the root, platforms and versions.
- [The Map and What Goes on It](DOC-0024-the-map-and-what-goes-on-it.md): `WayfinderMap`'s parameters, the drawing order, layers, polylines, the overlay, markers, taps, gestures, and writing a layer.
- [Marker Clustering](DOC-0025-marker-clustering.md): grouping markers that cover each other, how groups behave, and `clusterPoints` on its own.
- [Ornaments and Attribution](DOC-0026-ornaments-and-attribution.md): the compass, the orientation toggle, the scale bar, zoom buttons and the credits, over the map or in the screen's own layout.
- [The Map State and the Camera](DOC-0027-the-map-state-and-the-camera.md): `WayfinderMapState`, following the user, course-up, the visible bounds, fitting the camera to points, and camera limits.
- [Tile Sources and Tile Clients](DOC-0028-tile-sources-and-tile-clients.md): URL templates, TMS, what the client returns, why a map comes out blank, and caching.
- [Tracks and the User's Position](DOC-0029-tracks-and-the-users-position.md): recorded tracks and the location marker, on a live map and on a map frame, and how they are styled.
- [Maintaining wayfinder](DOC-0030-maintaining-wayfinder.md): the map-view pin, the copy of Web Mercator, and why the build and tests are shaped as they are.

## Why it is built this way

- [Shared Interfaces in the Root Module](ADR-0013-shared-interfaces-in-the-root-module.md): why the interfaces the wayfinder modules meet at live in the root, so no extension depends on another.
- [The App Fetches Every Tile](ADR-0014-the-app-fetches-every-tile.md): why wayfinder has no HTTP client or cache, and tiles come through the app's `TileClient`.
- [Camera Limits Corrected After the Move](ADR-0015-camera-limits-corrected-after-the-move.md): why the camera is brought back inside its limits rather than stopped at them.
- [Attribution That Collapses to an "i"](ADR-0016-attribution-that-collapses-to-an-i.md): why the credits are shown in full and then shrink, by default.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
