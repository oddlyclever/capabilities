# wayfinder

The full documentation of [`wayfinder`](https://github.com/oddlyclever/capabilities/blob/HEAD/wayfinder/README.md): maps for Compose Multiplatform on map-view, and the root of the wayfinder family. Its README covers installing it and a first use, and its KDoc is the API reference; this section covers the rest.

## Using it

- [How the Wayfinder Modules Fit Together](Wayfinder-How-The-Wayfinder-Modules-Fit-Together): what each wayfinder module adds, the interfaces they meet at, what comes with the root, platforms and versions.
- [The Map and What Goes on It](Wayfinder-The-Map-And-What-Goes-On-It): `WayfinderMap`'s parameters, the drawing order, layers, polylines, the overlay, markers, taps, gestures, and writing a layer.
- [Marker Clustering](Wayfinder-Marker-Clustering): grouping markers that cover each other, how groups behave, and `clusterPoints` on its own.
- [Ornaments and Attribution](Wayfinder-Ornaments-And-Attribution): the compass, the orientation toggle, the scale bar, zoom buttons and the credits, over the map or in the screen's own layout.
- [The Map State and the Camera](Wayfinder-The-Map-State-And-The-Camera): `WayfinderMapState`, following the user, course-up, the visible bounds, fitting the camera to points, and camera limits.
- [Tile Sources and Tile Clients](Wayfinder-Tile-Sources-And-Tile-Clients): URL templates, TMS, what the client returns, why a map comes out blank, and caching.
- [Tracks and the User's Position](Wayfinder-Tracks-And-The-Users-Position): recorded tracks and the location marker, on a live map and on a map frame, and how they are styled.
- [Maintaining wayfinder](Wayfinder-Maintaining-Wayfinder): the map-view pin, the copy of Web Mercator, and why the build and tests are shaped as they are.

## Why it is built this way

- [Shared Interfaces in the Root Module](Wayfinder-Shared-Interfaces-In-The-Root-Module): why the interfaces the wayfinder modules meet at live in the root, so no extension depends on another.
- [The App Fetches Every Tile](Wayfinder-The-App-Fetches-Every-Tile): why wayfinder has no HTTP client or cache, and tiles come through the app's `TileClient`.
- [Camera Limits Corrected After the Move](Wayfinder-Camera-Limits-Corrected-After-The-Move): why the camera is brought back inside its limits rather than stopped at them.
- [Attribution That Collapses to an "i"](Wayfinder-Attribution-That-Collapses-To-An-I): why the credits are shown in full and then shrink, by default.

A new page about this module alone goes here, numbered like every other document in the knowledge base. Knowledge that spans several modules goes in the shared sections instead.
