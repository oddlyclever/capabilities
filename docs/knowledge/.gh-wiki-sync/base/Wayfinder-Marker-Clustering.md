# Marker Clustering

DOC-0025 · 2026-11-04
Keywords: marker clustering, cluster markers, group nearby pins, overlapping markers, markers cover each other, too many markers, MarkerClustering, clusterPoints, PointCluster, isCluster, DefaultClusterMarker, cluster count, 99+, onClusterClick, tap a cluster, cluster radius, markers at the same spot never separate, clusters flicker, supercluster, cluster a list beside the map, IllegalArgumentException radius

Markers close together cover each other, so only the top one can be read or tapped. With clustering on, markers that would cover each other at the current zoom are drawn as one marker for the group, and as the map zooms in the groups part.

## Turning it on

Clustering is off unless the map is given a `MarkerClustering`. A map of a few markers, or of markers the app has spaced itself, has nothing to group.

```kotlin
WayfinderMap(
    state = state,
    markers = markers,
    clustering = MarkerClustering(
        description = { "${it.size} places" },
        content = { group -> DefaultClusterMarker(group.size) },
    ),
)
```

| `MarkerClustering` field | Default | What it is |
|---|---|---|
| `radius` | 40 dp | How close two markers' positions must be, at the current zoom, to be drawn as one. About a marker and its label across. |
| `step` | 0.5 | How much the zoom must change before the groups are worked out again |
| `zoomStep` | 2 | How much closer the map zooms when a group is tapped and there is no `onClusterClick` |
| `description` | null | What a screen reader says for a group |
| `content` | `DefaultClusterMarker(count)` | How a group is drawn, centred on its position |

A `radius`, `step` or `zoomStep` that is not positive is refused with `IllegalArgumentException` when the `MarkerClustering` is made.

## How a group behaves

- **A group is drawn at the middle of its markers.**
- **Tapping a group** moves the map `zoomStep` levels closer, centred on it, no closer than the limits allow. Give `onClusterClick` to do something else, such as listing the group's markers. `onMarkerClick` is only for single markers.
- **The groups are worked out again at every `step` of zoom**, not every frame of a pinch. A map of hundreds of markers stays smooth, and groups do not flicker apart and back together while the zoom passes a boundary.
- **At the closest zoom the limits allow, nothing is grouped.** Zooming no further cannot part markers at the same spot, so there they are drawn on top of each other, as they would be without clustering.
- **A group keeps its composition state while it gains or loses members**, because it is keyed on its first marker.

## What a group looks like and says

A group's look and words are the app's. `content` draws it, and `DefaultClusterMarker(count, color, size = 28.dp)` is the default: a white-ringed blue dot with the count in it, shown as "99+" above 99. `description` is what a screen reader says, and is null unless the app gives it, since a count of "markers" means nothing to the app's users.

## The grouping on its own

`clusterPoints(items, zoom, radius) { position }` is the same grouping for anything that is not a `MapMarker`: a list beside the map, a count in a header. It returns a `PointCluster<T>` for each group, with its `items`, its `position`, its `size`, and `isCluster`, which is true when it holds more than one.

- **Every item is in exactly one group.** An item with nothing near it is a group of one.
- **It is greedy and in order.** Each item not yet in a group starts one and takes every later item within `radius` of it. The same items at the same zoom always group the same way, and a group is never wider than twice `radius`.
- **It measures on the plane map-view draws**, Web Mercator with a tile 256 dp across at zoom 0, so `radius` is in the same units as a marker's size and the answer does not depend on the screen's density.
- **It measures the short way round the antimeridian**, and a group across it is centred on it.
- **It is quadratic** in the number of items. It is meant for the hundreds of markers a screen shows, not for a dataset.
