# Missing and Broken Tiles

DOC-0039 · 2026-11-04
Keywords: blank map, blurry map, tiles never load, tile server error page, 404 tile, 502 Bad Gateway, HTML instead of image, truncated image, corrupt tile, WebP tiles not showing, JPEG tiles, PNG tiles, decodeRasterTile returns null, no error reported, exception in TileClient swallowed, retry tiles, nearest ancestor, over-zoom blur

A raster layer never fails because of a tile. Whatever goes wrong with one, the cell shows the nearest ancestor it has, or nothing, and no error reaches the app. This page covers what counts as a missing tile, what the map does about it, and how to find out why a map is blank.

## What counts as a missing tile

Every tile's bytes go through `decodeRasterTile`, which returns null, a missing tile, for:

- no bytes: the client returned null;
- an empty byte array;
- bytes no decoder on the platform can read, such as a server's HTML or JSON error page;
- a truncated image;
- an image that decodes with no width or height.

An exception thrown by the `TileClient` is caught by map-view's tile queue and counts as a missing tile too.

## Formats

PNG and JPEG decode on every target. WebP decodes only where the platform's image decoder reads it; where it does not, every WebP tile is a missing tile, and the map is blank with no error. A tile decodes at its own size.

## What the map does

- **It draws the nearest ancestor**, magnified, from up to five levels up. With none, the cell is empty and whatever is under the layer shows.
- **It asks again after ten seconds**, for as long as the tile is wanted on screen. That retry is map-view's tile queue, not something the module adds, and it means a tile that can never succeed is requested every ten seconds.
- **Past the last level of the source's `zoomRange`** it never asks: the deepest tile is magnified instead, and blurs.

## When the map is blank or blurry

Nothing is reported, so the cause is found in the client. Log inside it.

| What you see | Likely cause |
|---|---|
| Nothing at all | Every tile is missing: a wrong URL, a missing key or header, an error page for every request, or WebP on a platform that cannot decode it. |
| A scramble of the right tiles in the wrong places | A TMS source without `tms = true`. |
| A blurry map at close zooms | The camera is past the source's last level, or the close levels fail and their ancestors are shown. |
| The same tile requested every ten seconds | That tile fails each time, and is retried while it is on screen. |
| Every tile fetched again at every recomposition | The client, or another argument, is new each time. See [Raster Basemaps](Wayfinder-Raster-Raster-Basemaps). |

Why failure is never an error is in [Decode Raster Tiles Upstream and Treat Failure as a Missing Tile](Wayfinder-Raster-Decode-Raster-Tiles-Upstream-And-Treat-Failure-As-A-Missing-Tile).
