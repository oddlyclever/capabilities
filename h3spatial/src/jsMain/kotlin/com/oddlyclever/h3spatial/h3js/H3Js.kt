@file:JsModule("h3-js")
@file:JsNonModule

package com.oddlyclever.h3spatial.h3js

// The h3-js functions H3Native calls, declared from h3-js's own typings (dist/types.d.ts). Indexes
// cross this boundary as canonical H3 strings and coordinates as [lat, lng] pairs in degrees. Kept in
// their own package so they do not shadow H3Native's members of the same name.

internal external fun latLngToCell(lat: Double, lng: Double, res: Int): String
internal external fun cellToLatLng(h3Index: String): Array<Double>
internal external fun cellToBoundary(h3Index: String): Array<Array<Double>>
internal external fun isValidCell(h3Index: String): Boolean
internal external fun isPentagon(h3Index: String): Boolean
internal external fun gridDisk(h3Index: String, ringSize: Int): Array<String>
internal external fun gridDistance(origin: String, destination: String): Double
internal external fun areNeighborCells(origin: String, destination: String): Boolean
internal external fun cellToParent(h3Index: String, res: Int): String
internal external fun cellToChildren(h3Index: String, res: Int): Array<String>
